package com.example.crypto

/**
 * Pure Kotlin Base64 encoder and decoder.
 * Compatible with all Android API levels and local JVM unit tests without framework dependencies.
 */
object Base64Util {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    private val DECODE_TABLE = IntArray(128) { -1 }.apply {
        for (i in ALPHABET.indices) {
            this[ALPHABET[i].code] = i
        }
    }

    fun encode(data: ByteArray): String {
        val result = StringBuilder((data.size * 4 + 2) / 3)
        var i = 0
        while (i < data.size) {
            val b0 = data[i++].toInt() and 0xFF
            val b1 = if (i < data.size) data[i++].toInt() and 0xFF else -1
            val b2 = if (i < data.size) data[i++].toInt() and 0xFF else -1

            val c0 = b0 shr 2
            val c1 = ((b0 and 0x03) shl 4) or (if (b1 != -1) (b1 shr 4) else 0)
            val c2 = if (b1 != -1) ((b1 and 0x0F) shl 2) or (if (b2 != -1) (b2 shr 6) else 0) else -1
            val c3 = if (b2 != -1) (b2 and 0x3F) else -1

            result.append(ALPHABET[c0])
            result.append(ALPHABET[c1])
            result.append(if (c2 != -1) ALPHABET[c2] else '=')
            result.append(if (c3 != -1) ALPHABET[c3] else '=')
        }
        return result.toString()
    }

    fun decode(input: String): ByteArray {
        val cleaned = input.replace("[\\s\\r\\n]".toRegex(), "")
        if (cleaned.isEmpty()) return ByteArray(0)

        val padding = when {
            cleaned.endsWith("==") -> 2
            cleaned.endsWith("=") -> 1
            else -> 0
        }
        val outputLength = (cleaned.length * 3 / 4) - padding
        val output = ByteArray(outputLength)
        var outIndex = 0

        var i = 0
        while (i < cleaned.length) {
            val c0 = cleaned[i++].code
            val c1 = cleaned[i++].code
            val c2 = if (i < cleaned.length) cleaned[i++].code else '='.code
            val c3 = if (i < cleaned.length) cleaned[i++].code else '='.code

            val v0 = if (c0 < 128) DECODE_TABLE[c0] else -1
            val v1 = if (c1 < 128) DECODE_TABLE[c1] else -1
            val v2 = if (c2 in 0..127 && c2 != '='.code) DECODE_TABLE[c2] else 0
            val v3 = if (c3 in 0..127 && c3 != '='.code) DECODE_TABLE[c3] else 0

            if (v0 == -1 || v1 == -1) break

            if (outIndex < outputLength) {
                output[outIndex++] = ((v0 shl 2) or (v1 shr 4)).toByte()
            }
            if (c2 != '='.code && outIndex < outputLength) {
                output[outIndex++] = (((v1 and 0x0F) shl 4) or (v2 shr 2)).toByte()
            }
            if (c3 != '='.code && outIndex < outputLength) {
                output[outIndex++] = (((v2 and 0x03) shl 6) or v3).toByte()
            }
        }

        return output
    }
}
