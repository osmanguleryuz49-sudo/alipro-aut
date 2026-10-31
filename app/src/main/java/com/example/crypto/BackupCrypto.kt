package com.example.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class BackupAccountItem(
    val serviceName: String,
    val accountName: String,
    val secretBase32: String,
    val algorithm: String = "SHA1",
    val digits: Int = 6,
    val period: Int = 30
)

/**
 * Encrypted backup and restore engine using PBKDF2WithHmacSHA256 + AES/GCM/NoPadding.
 * Uses pure Kotlin JSON serialization for seamless execution without framework stubs.
 */
object BackupCrypto {
    private const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 100_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val TAG_LENGTH_BITS = 128

    private val secureRandom = SecureRandom()

    private fun escapeJson(str: String): String {
        return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\u000C", "\\f")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun unescapeJson(str: String): String {
        val sb = StringBuilder(str.length)
        var i = 0
        while (i < str.length) {
            val ch = str[i]
            if (ch == '\\' && i + 1 < str.length) {
                when (val next = str[i + 1]) {
                    '\\' -> sb.append('\\')
                    '"' -> sb.append('"')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000C')
                    else -> sb.append(next)
                }
                i += 2
            } else {
                sb.append(ch)
                i++
            }
        }
        return sb.toString()
    }

    /**
     * Encrypts a list of [BackupAccountItem] using PBKDF2 + AES-GCM with the user's password.
     * Returns an encrypted JSON envelope string.
     */
    fun encryptBackup(accounts: List<BackupAccountItem>, password: CharArray): String {
        // Serialize accounts list to JSON
        val payloadBuilder = StringBuilder("[")
        for (idx in accounts.indices) {
            val acc = accounts[idx]
            payloadBuilder.append("{")
            payloadBuilder.append("\"serviceName\":\"").append(escapeJson(acc.serviceName)).append("\",")
            payloadBuilder.append("\"accountName\":\"").append(escapeJson(acc.accountName)).append("\",")
            payloadBuilder.append("\"secret\":\"").append(escapeJson(acc.secretBase32)).append("\",")
            payloadBuilder.append("\"algorithm\":\"").append(escapeJson(acc.algorithm)).append("\",")
            payloadBuilder.append("\"digits\":").append(acc.digits).append(",")
            payloadBuilder.append("\"period\":").append(acc.period)
            payloadBuilder.append("}")
            if (idx < accounts.size - 1) {
                payloadBuilder.append(",")
            }
        }
        payloadBuilder.append("]")
        val plainBytes = payloadBuilder.toString().toByteArray(Charsets.UTF_8)

        // Generate salt & IV
        val salt = ByteArray(SALT_LENGTH).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { secureRandom.nextBytes(it) }

        // Derive AES key
        val keySpec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(KDF_ALGORITHM)
        val derivedKeyBytes = factory.generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(derivedKeyBytes, "AES")

        // AES-GCM Encrypt
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        val cipherText = cipher.doFinal(plainBytes)

        val saltStr = Base64Util.encode(salt)
        val ivStr = Base64Util.encode(iv)
        val payloadStr = Base64Util.encode(cipherText)

        return """
        {
          "version": 1,
          "app": "Dogrula",
          "kdf": "$KDF_ALGORITHM",
          "iterations": $ITERATIONS,
          "salt": "$saltStr",
          "iv": "$ivStr",
          "payload": "$payloadStr"
        }
        """.trimIndent()
    }

    /**
     * Decrypts an encrypted JSON envelope string using the user's password.
     * Returns the list of [BackupAccountItem].
     * Throws an exception if the password is wrong or format is invalid.
     */
    fun decryptBackup(envelopeString: String, password: CharArray): List<BackupAccountItem> {
        val version = extractInt(envelopeString, "version") ?: 1
        if (version != 1) {
            throw IllegalArgumentException("Desteklenmeyen yedek sürümü: $version")
        }

        val iterations = extractInt(envelopeString, "iterations") ?: ITERATIONS
        val saltStr = extractString(envelopeString, "salt")
            ?: throw IllegalArgumentException("Yedek tuz (salt) bilgisi eksik")
        val ivStr = extractString(envelopeString, "iv")
            ?: throw IllegalArgumentException("Yedek iv bilgisi eksik")
        val payloadStr = extractString(envelopeString, "payload")
            ?: throw IllegalArgumentException("Yedek şifreli yük (payload) eksik")

        val salt = Base64Util.decode(saltStr)
        val iv = Base64Util.decode(ivStr)
        val payload = Base64Util.decode(payloadStr)

        // Derive AES key
        val keySpec = PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(KDF_ALGORITHM)
        val derivedKeyBytes = factory.generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(derivedKeyBytes, "AES")

        // Decrypt
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
        val plainBytes = cipher.doFinal(payload)

        val plainJson = String(plainBytes, Charsets.UTF_8).trim()
        return parseAccountsList(plainJson)
    }

    private fun extractString(json: String, key: String): String? {
        val regex = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
        return regex.find(json)?.groupValues?.get(1)?.let { unescapeJson(it) }
    }

    private fun extractInt(json: String, key: String): Int? {
        val regex = Regex("\"$key\"\\s*:\\s*(\\d+)")
        return regex.find(json)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun parseAccountsList(json: String): List<BackupAccountItem> {
        val list = mutableListOf<BackupAccountItem>()
        // Match each account JSON object inside the array
        val objectRegex = Regex("\\{([^}]+)\\}")
        for (match in objectRegex.findAll(json)) {
            val objBody = match.groupValues[1]
            val serviceName = extractString(objBody, "serviceName") ?: "Hesap"
            val accountName = extractString(objBody, "accountName") ?: ""
            val secret = extractString(objBody, "secret") ?: continue
            val algorithm = extractString(objBody, "algorithm") ?: "SHA1"
            val digits = extractInt(objBody, "digits") ?: 6
            val period = extractInt(objBody, "period") ?: 30

            list.add(
                BackupAccountItem(
                    serviceName = serviceName,
                    accountName = accountName,
                    secretBase32 = secret,
                    algorithm = algorithm,
                    digits = digits,
                    period = period
                )
            )
        }
        return list
    }
}
