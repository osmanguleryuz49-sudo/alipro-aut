package com.example.crypto

/**
 * RFC 4648 compliant Base32 decoder and encoder with tolerant input handling.
 * Strips whitespace, hyphens, and handles lowercase characters gracefully.
 */
object Base32 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private val CHAR_MAP = IntArray(128) { -1 }.apply {
        for (i in ALPHABET.indices) {
            val char = ALPHABET[i]
            this[char.code] = i
            // Allow lowercase equivalent
            if (char in 'A'..'Z') {
                this[char.lowercaseChar().code] = i
            }
        }
    }

    /**
     * Cleans an input Base32 string by removing spaces, dashes, equals signs, and normalizing case.
     */
    fun clean(input: String): String {
        return input.replace("[\\s\\-=]".toRegex(), "").uppercase()
    }

    /**
     * Checks if the cleaned string is valid Base32 format.
     */
    fun isValid(input: String): Boolean {
        val cleaned = clean(input)
        if (cleaned.isEmpty()) return false
        for (ch in cleaned) {
            if (ch.code >= CHAR_MAP.size || CHAR_MAP[ch.code] == -1) {
                return false
            }
        }
        return true
    }

    /**
     * Decodes a Base32 string into a byte array.
     * Throws [IllegalArgumentException] if an invalid character is encountered.
     */
    fun decode(input: String): ByteArray {
        val cleaned = clean(input)
        if (cleaned.isEmpty()) return ByteArray(0)

        val output = ByteArray((cleaned.length * 5) / 8)
        var buffer = 0
        var bitsLeft = 0
        var count = 0

        for (ch in cleaned) {
            val code = ch.code
            if (code >= CHAR_MAP.size || CHAR_MAP[code] == -1) {
                throw IllegalArgumentException("Geçersiz Base32 karakteri: '$ch'")
            }
            val value = CHAR_MAP[code]
            buffer = (buffer shl 5) or value
            bitsLeft += 5

            if (bitsLeft >= 8) {
                if (count < output.size) {
                    output[count++] = ((buffer shr (bitsLeft - 8)) and 0xFF).toByte()
                }
                bitsLeft -= 8
            }
        }

        return if (count == output.size) output else output.copyOf(count)
    }

    /**
     * Encodes a byte array into a canonical Base32 string.
     */
    fun encode(data: ByteArray): String {
        if (data.isEmpty()) return ""
        val result = StringBuilder((data.size * 8 + 4) / 5)
        var buffer = 0
        var bitsLeft = 0

        for (b in data) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                val index = (buffer shr (bitsLeft - 5)) and 0x1F
                result.append(ALPHABET[index])
                bitsLeft -= 5
            }
        }

        if (bitsLeft > 0) {
            val index = (buffer shl (5 - bitsLeft)) and 0x1F
            result.append(ALPHABET[index])
        }

        return result.toString()
    }
}
