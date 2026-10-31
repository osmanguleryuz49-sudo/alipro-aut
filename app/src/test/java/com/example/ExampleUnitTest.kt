package com.example

import com.example.crypto.BackupAccountItem
import com.example.crypto.BackupCrypto
import com.example.crypto.Base32
import com.example.crypto.TotpGenerator
import com.example.parser.OtpUriParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun base32_decoding_and_encoding() {
        val originalText = "Hello 2FA World!"
        val bytes = originalText.toByteArray(Charsets.UTF_8)
        val encoded = Base32.encode(bytes)
        assertTrue(Base32.isValid(encoded))

        val decoded = Base32.decode(encoded)
        assertEquals(originalText, String(decoded, Charsets.UTF_8))
    }

    @Test
    fun base32_tolerance_handles_spaces_dashes_and_lowercase() {
        val original = "Doğrula Secure Secret 2026"
        val encoded = Base32.encode(original.toByteArray(Charsets.UTF_8))
        val messy = "  " + encoded.lowercase().chunked(4).joinToString(" - ") + " ==  "
        assertTrue(Base32.isValid(messy))

        val decoded = Base32.decode(messy)
        assertEquals(original, String(decoded, Charsets.UTF_8))
    }

    @Test
    fun totp_generation_standard_vector() {
        // RFC 6238 / RFC 4226 test vector
        val secretBytes = "12345678901234567890".toByteArray(Charsets.US_ASCII)

        // Time 59s -> counter 1
        val code1 = TotpGenerator.generateCode(
            secretBytes = secretBytes,
            algorithm = "SHA1",
            digits = 8,
            period = 30,
            timeSeconds = 59L
        )
        assertEquals("94287082", code1)

        // 6 digits formatting
        val formatted = TotpGenerator.formatCode("123456")
        assertEquals("123 456", formatted)

        val formatted8 = TotpGenerator.formatCode("12345678")
        assertEquals("1234 5678", formatted8)
    }

    @Test
    fun steam_guard_code_generation() {
        val secretBytes = Base32.decode("JBSWY3DPEHPK3PXP")
        val steamCode = TotpGenerator.generateCode(
            secretBytes = secretBytes,
            type = "STEAM",
            timeSeconds = 1000L
        )
        assertEquals(5, steamCode.length)
        // Verify all characters belong to Steam alphabet (23456789BCDFGHJKMNPQRTVWXY)
        val steamAlphabet = "23456789BCDFGHJKMNPQRTVWXY".toSet()
        assertTrue(steamCode.all { it in steamAlphabet })
    }

    @Test
    fun hotp_counter_based_generation() {
        val secretBytes = "12345678901234567890".toByteArray(Charsets.US_ASCII)
        val codeCounter0 = TotpGenerator.generateCode(
            secretBytes = secretBytes,
            type = "HOTP",
            counter = 0L,
            digits = 6
        )
        val codeCounter1 = TotpGenerator.generateCode(
            secretBytes = secretBytes,
            type = "HOTP",
            counter = 1L,
            digits = 6
        )
        assertEquals(6, codeCounter0.length)
        assertEquals(6, codeCounter1.length)
        assertNotEquals(codeCounter0, codeCounter1)
    }

    @Test
    fun next_code_peek_generation() {
        val secretBytes = "12345678901234567890".toByteArray(Charsets.US_ASCII)
        val currentCode = TotpGenerator.generateCode(secretBytes, timeSeconds = 30L)
        val nextCode = TotpGenerator.generateNextCode(secretBytes, timeSeconds = 30L)
        assertNotEquals(currentCode, nextCode)
    }

    @Test
    fun totp_remaining_seconds_and_progress() {
        val remaining = TotpGenerator.getRemainingSeconds(period = 30, timeSeconds = 25L)
        assertEquals(5, remaining)

        val progress = TotpGenerator.getProgress(period = 30, timeSeconds = 25L)
        assertEquals(5f / 30f, progress, 0.001f)
    }

    @Test
    fun otp_uri_parser_standard_uri() {
        val uri = "otpauth://totp/Google:john.doe@gmail.com?secret=JBSWY3DPEHPK3PXP&issuer=Google&algorithm=SHA256&digits=8&period=30"
        val accounts = OtpUriParser.parse(uri)
        assertEquals(1, accounts.size)

        val account = accounts[0]
        assertEquals("Google", account.serviceName)
        assertEquals("john.doe@gmail.com", account.accountName)
        assertEquals("JBSWY3DPEHPK3PXP", account.secretBase32)
        assertEquals("SHA256", account.algorithm)
        assertEquals(8, account.digits)
        assertEquals(30, account.period)
    }

    @Test
    fun backup_crypto_encrypt_and_decrypt() {
        val items = listOf(
            BackupAccountItem("GitHub", "octocat", "JBSWY3DPEHPK3PXP", "SHA1", 6, 30),
            BackupAccountItem("Google", "me@gmail.com", "MFRGGZDFMZTWQ2LK", "SHA256", 8, 30)
        )
        val password = "MySecretMasterPassword123!".toCharArray()

        val encryptedJson = BackupCrypto.encryptBackup(items, password)
        assertTrue(encryptedJson.contains("Dogrula"))
        assertTrue(encryptedJson.contains("PBKDF2WithHmacSHA256"))

        val restored = BackupCrypto.decryptBackup(encryptedJson, password)
        assertEquals(2, restored.size)
        assertEquals("GitHub", restored[0].serviceName)
        assertEquals("Google", restored[1].serviceName)
    }
}
