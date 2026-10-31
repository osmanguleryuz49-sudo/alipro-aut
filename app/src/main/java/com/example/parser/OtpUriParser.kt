package com.example.parser

import com.example.crypto.Base32
import com.example.crypto.Base64Util
import java.io.ByteArrayInputStream
import java.net.URLDecoder

data class ParsedOtpAccount(
    val serviceName: String,
    val accountName: String,
    val secretBase32: String,
    val algorithm: String = "SHA1",
    val digits: Int = 6,
    val period: Int = 30
)

object OtpUriParser {

    /**
     * Parses an input string which may be a standard `otpauth://totp/...` URI
     * or a Google Authenticator migration URI `otpauth-migration://offline?data=...`.
     * Returns a list of parsed accounts (usually 1, but migration QR codes can contain multiple).
     */
    fun parse(rawUriString: String): List<ParsedOtpAccount> {
        val trimmed = rawUriString.trim()
        if (trimmed.startsWith("otpauth-migration://", ignoreCase = true)) {
            return parseGoogleMigration(trimmed)
        }

        if (trimmed.startsWith("otpauth://", ignoreCase = true)) {
            val single = parseStandardTotp(trimmed)
            return if (single != null) listOf(single) else emptyList()
        }

        // Check if raw Base32 secret was pasted directly
        if (Base32.isValid(trimmed)) {
            return listOf(
                ParsedOtpAccount(
                    serviceName = "Doğrulama",
                    accountName = "",
                    secretBase32 = Base32.clean(trimmed),
                    algorithm = "SHA1",
                    digits = 6,
                    period = 30
                )
            )
        }

        return emptyList()
    }

    private fun parseQueryParams(queryString: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        if (queryString.isEmpty()) return params

        for (part in queryString.split("&")) {
            val idx = part.indexOf('=')
            if (idx != -1) {
                val key = URLDecoder.decode(part.substring(0, idx), "UTF-8").lowercase()
                val value = URLDecoder.decode(part.substring(idx + 1), "UTF-8")
                params[key] = value
            } else if (part.isNotEmpty()) {
                val key = URLDecoder.decode(part, "UTF-8").lowercase()
                params[key] = ""
            }
        }
        return params
    }

    private fun parseStandardTotp(uriString: String): ParsedOtpAccount? {
        return try {
            val questionIdx = uriString.indexOf('?')
            val basePart = if (questionIdx != -1) uriString.substring(0, questionIdx) else uriString
            val queryPart = if (questionIdx != -1) uriString.substring(questionIdx + 1) else ""

            val lowerBase = basePart.lowercase()
            if (!lowerBase.startsWith("otpauth://totp") && !lowerBase.startsWith("otpauth://hotp")) {
                return null
            }

            val queryParams = parseQueryParams(queryPart)
            val secretParam = queryParams["secret"] ?: return null
            val cleanedSecret = Base32.clean(secretParam)
            if (!Base32.isValid(cleanedSecret)) return null

            var issuer = queryParams["issuer"] ?: ""
            var accountName = ""

            // Extract path after otpauth://totp/
            val prefix = if (lowerBase.startsWith("otpauth://totp/")) "otpauth://totp/"
            else if (lowerBase.startsWith("otpauth://hotp/")) "otpauth://hotp/"
            else if (lowerBase.startsWith("otpauth://totp")) "otpauth://totp"
            else "otpauth://hotp"

            val rawPath = basePart.substring(prefix.length).trimStart('/')
            val decodedPath = URLDecoder.decode(rawPath, "UTF-8")

            if (decodedPath.contains(":")) {
                val parts = decodedPath.split(":", limit = 2)
                if (issuer.isEmpty()) {
                    issuer = parts[0].trim()
                }
                accountName = parts[1].trim()
            } else {
                accountName = decodedPath.trim()
            }

            if (issuer.isEmpty()) {
                issuer = if (accountName.isNotEmpty()) accountName else "Hesap"
            }

            val algorithm = when (queryParams["algorithm"]?.uppercase()) {
                "SHA256" -> "SHA256"
                "SHA512" -> "SHA512"
                else -> "SHA1"
            }

            val digits = when (queryParams["digits"]) {
                "8" -> 8
                else -> 6
            }

            val period = queryParams["period"]?.toIntOrNull() ?: 30

            ParsedOtpAccount(
                serviceName = issuer,
                accountName = accountName,
                secretBase32 = cleanedSecret,
                algorithm = algorithm,
                digits = digits,
                period = if (period > 0) period else 30
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Parses Google Authenticator export QR code (otpauth-migration://offline?data=...)
     * using a lightweight binary protobuf decoder.
     */
    private fun parseGoogleMigration(uriString: String): List<ParsedOtpAccount> {
        val result = mutableListOf<ParsedOtpAccount>()
        try {
            val questionIdx = uriString.indexOf('?')
            val queryPart = if (questionIdx != -1) uriString.substring(questionIdx + 1) else ""
            val queryParams = parseQueryParams(queryPart)
            val data = queryParams["data"] ?: return emptyList()

            val decodedBytes = Base64Util.decode(data)
            val stream = ByteArrayInputStream(decodedBytes)

            while (stream.available() > 0) {
                val tag = readVarint(stream) ?: break
                val fieldNumber = (tag ushr 3).toInt()
                val wireType = (tag and 0x07).toInt()

                if (fieldNumber == 1 && wireType == 2) { // 1: repeated OtpParameters
                    val length = readVarint(stream)?.toInt() ?: break
                    val subBytes = ByteArray(length)
                    var readTotal = 0
                    while (readTotal < length) {
                        val r = stream.read(subBytes, readTotal, length - readTotal)
                        if (r <= 0) break
                        readTotal += r
                    }
                    val account = parseOtpParameters(subBytes)
                    if (account != null) {
                        result.add(account)
                    }
                } else {
                    skipField(wireType, stream)
                }
            }
        } catch (_: Exception) {
            // Ignore corrupted migration payload
        }
        return result
    }

    private fun parseOtpParameters(bytes: ByteArray): ParsedOtpAccount? {
        val stream = ByteArrayInputStream(bytes)
        var secretBytes: ByteArray? = null
        var name = ""
        var issuer = ""
        var algorithm = "SHA1"
        var digits = 6
        var period = 30

        while (stream.available() > 0) {
            val tag = readVarint(stream) ?: break
            val fieldNumber = (tag ushr 3).toInt()
            val wireType = (tag and 0x07).toInt()

            when (fieldNumber) {
                1 -> { // secret (bytes)
                    val len = readVarint(stream)?.toInt() ?: break
                    val b = ByteArray(len)
                    stream.read(b, 0, len)
                    secretBytes = b
                }
                2 -> { // name (string)
                    val len = readVarint(stream)?.toInt() ?: break
                    val b = ByteArray(len)
                    stream.read(b, 0, len)
                    name = String(b, Charsets.UTF_8)
                }
                3 -> { // issuer (string)
                    val len = readVarint(stream)?.toInt() ?: break
                    val b = ByteArray(len)
                    stream.read(b, 0, len)
                    issuer = String(b, Charsets.UTF_8)
                }
                4 -> { // algorithm: 1=SHA1, 2=SHA256, 3=SHA512
                    val algVal = readVarint(stream)?.toInt() ?: 1
                    algorithm = when (algVal) {
                        2 -> "SHA256"
                        3 -> "SHA512"
                        else -> "SHA1"
                    }
                }
                5 -> { // digits: 1=6, 2=8
                    val digVal = readVarint(stream)?.toInt() ?: 1
                    digits = if (digVal == 2) 8 else 6
                }
                else -> skipField(wireType, stream)
            }
        }

        if (secretBytes == null || secretBytes.isEmpty()) return null
        val secretBase32 = Base32.encode(secretBytes)

        var finalIssuer = issuer
        var finalAccount = name
        if (finalIssuer.isEmpty() && name.contains(":")) {
            val parts = name.split(":", limit = 2)
            finalIssuer = parts[0].trim()
            finalAccount = parts[1].trim()
        }
        if (finalIssuer.isEmpty()) finalIssuer = "Hesap"

        return ParsedOtpAccount(
            serviceName = finalIssuer,
            accountName = finalAccount,
            secretBase32 = secretBase32,
            algorithm = algorithm,
            digits = digits,
            period = period
        )
    }

    private fun readVarint(stream: ByteArrayInputStream): Long? {
        var value = 0L
        var shift = 0
        while (true) {
            val b = stream.read()
            if (b == -1) return null
            value = value or ((b.toLong() and 0x7F) shl shift)
            if ((b and 0x80) == 0) break
            shift += 7
            if (shift > 63) return null
        }
        return value
    }

    private fun skipField(wireType: Int, stream: ByteArrayInputStream) {
        when (wireType) {
            0 -> readVarint(stream) // Varint
            1 -> stream.skip(8)     // 64-bit
            2 -> {                  // Length-delimited
                val length = readVarint(stream) ?: 0L
                stream.skip(length)
            }
            5 -> stream.skip(4)     // 32-bit
        }
    }
}
