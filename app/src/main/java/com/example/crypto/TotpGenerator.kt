package com.example.crypto

import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

object TotpGenerator {

    private const val STEAM_ALPHABET = "23456789BCDFGHJKMNPQRTVWXY"

    /**
     * Generates an OTP code for a given secret key and parameters.
     * Supports TOTP (time-based), HOTP (counter-based), and Steam Guard (alphanumeric 5-char).
     */
    fun generateCode(
        secretBytes: ByteArray,
        algorithm: String = "SHA1",
        digits: Int = 6,
        period: Int = 30,
        type: String = "TOTP",
        counter: Long = 0L,
        timeSeconds: Long = System.currentTimeMillis() / 1000L
    ): String {
        if (secretBytes.isEmpty()) return "0".repeat(digits)

        val actualCounter = when (type.uppercase()) {
            "HOTP" -> counter
            else -> timeSeconds / period
        }

        val counterBytes = ByteBuffer.allocate(8).putLong(actualCounter).array()

        val hmacAlgorithm = when (algorithm.uppercase().replace("-", "")) {
            "SHA256" -> "HmacSHA256"
            "SHA512" -> "HmacSHA512"
            else -> "HmacSHA1"
        }

        val mac = Mac.getInstance(hmacAlgorithm)
        val keySpec = SecretKeySpec(secretBytes, hmacAlgorithm)
        mac.init(keySpec)
        val hash = mac.doFinal(counterBytes)

        // Dynamic truncation (RFC 4226 / RFC 6238)
        val offset = (hash[hash.size - 1].toInt() and 0x0F)
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)

        // Special Steam Guard 5-character alphanumeric formatting
        if (type.equals("STEAM", ignoreCase = true)) {
            var fullCode = binary
            val sb = StringBuilder(5)
            for (i in 0 until 5) {
                val index = (fullCode % STEAM_ALPHABET.length).toInt()
                sb.append(STEAM_ALPHABET[index])
                fullCode /= STEAM_ALPHABET.length
            }
            return sb.toString()
        }

        val modulo = 10.0.pow(digits.toDouble()).toInt()
        val otp = binary % modulo

        return String.format("%0${digits}d", otp)
    }

    /**
     * Generates the code for the upcoming interval (Next Code Peek).
     */
    fun generateNextCode(
        secretBytes: ByteArray,
        algorithm: String = "SHA1",
        digits: Int = 6,
        period: Int = 30,
        type: String = "TOTP",
        counter: Long = 0L,
        timeSeconds: Long = System.currentTimeMillis() / 1000L
    ): String {
        return if (type.equals("HOTP", ignoreCase = true)) {
            generateCode(secretBytes, algorithm, digits, period, type, counter + 1, timeSeconds)
        } else {
            generateCode(secretBytes, algorithm, digits, period, type, counter, timeSeconds + period)
        }
    }

    /**
     * Formats an OTP code (e.g., "123456" -> "123 456", "12345678" -> "1234 5678", Steam code stays 5 chars).
     */
    fun formatCode(code: String): String {
        return when (code.length) {
            6 -> "${code.substring(0, 3)} ${code.substring(3)}"
            8 -> "${code.substring(0, 4)} ${code.substring(4)}"
            else -> code
        }
    }

    /**
     * Calculates remaining seconds in the current TOTP interval.
     */
    fun getRemainingSeconds(
        period: Int = 30,
        timeSeconds: Long = System.currentTimeMillis() / 1000L
    ): Int {
        val remainder = (timeSeconds % period).toInt()
        val remaining = period - remainder
        return if (remaining == 0) period else remaining
    }

    /**
     * Calculates progress [0.0f, 1.0f] of the current period.
     */
    fun getProgress(
        period: Int = 30,
        timeSeconds: Long = System.currentTimeMillis() / 1000L
    ): Float {
        val remaining = getRemainingSeconds(period, timeSeconds)
        return remaining.toFloat() / period.toFloat()
    }
}
