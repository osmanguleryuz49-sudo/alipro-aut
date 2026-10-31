package com.example.data.repository

import com.example.crypto.BackupAccountItem
import com.example.crypto.BackupCrypto
import com.example.crypto.Base32
import com.example.crypto.KeyStoreManager
import com.example.data.db.AccountDao
import com.example.data.db.AccountEntity
import com.example.data.db.PasskeyEntity
import com.example.parser.ParsedOtpAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class AccountRepository(private val accountDao: AccountDao) {

    val accounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()
    val passkeys: Flow<List<PasskeyEntity>> = accountDao.getAllPasskeys()

    suspend fun addAccount(
        serviceName: String,
        accountName: String,
        secretBase32: String,
        algorithm: String = "SHA1",
        digits: Int = 6,
        period: Int = 30,
        type: String = "TOTP",
        category: String = "Genel"
    ): Long = withContext(Dispatchers.IO) {
        val cleanSecret = Base32.clean(secretBase32)
        val secretBytes = Base32.decode(cleanSecret)
        val encrypted = KeyStoreManager.encrypt(secretBytes)

        val entity = AccountEntity(
            serviceName = serviceName.trim(),
            accountName = accountName.trim(),
            encryptedSecret = encrypted,
            algorithm = algorithm,
            digits = digits,
            period = period,
            type = type,
            category = category
        )
        accountDao.insert(entity)
    }

    suspend fun addAccounts(parsedList: List<ParsedOtpAccount>): Int = withContext(Dispatchers.IO) {
        var count = 0
        for (item in parsedList) {
            val cleanSecret = Base32.clean(item.secretBase32)
            if (Base32.isValid(cleanSecret)) {
                val secretBytes = Base32.decode(cleanSecret)
                val encrypted = KeyStoreManager.encrypt(secretBytes)
                val entity = AccountEntity(
                    serviceName = item.serviceName.trim().ifEmpty { "Hesap" },
                    accountName = item.accountName.trim(),
                    encryptedSecret = encrypted,
                    algorithm = item.algorithm,
                    digits = item.digits,
                    period = item.period
                )
                accountDao.insert(entity)
                count++
            }
        }
        count
    }

    suspend fun updateAccountNames(id: Long, newServiceName: String, newAccountName: String, newCategory: String) =
        withContext(Dispatchers.IO) {
            val existing = accountDao.getAccountById(id) ?: return@withContext
            val updated = existing.copy(
                serviceName = newServiceName.trim(),
                accountName = newAccountName.trim(),
                category = newCategory
            )
            accountDao.update(updated)
        }

    suspend fun togglePin(id: Long, currentPin: Boolean) = withContext(Dispatchers.IO) {
        accountDao.setPinned(id, !currentPin)
    }

    suspend fun updateOrderIndex(id: Long, newIndex: Int) = withContext(Dispatchers.IO) {
        accountDao.updateOrderIndex(id, newIndex)
    }

    suspend fun incrementHotpCounter(id: Long) = withContext(Dispatchers.IO) {
        accountDao.incrementCounter(id)
    }

    suspend fun deleteAccount(account: AccountEntity) = withContext(Dispatchers.IO) {
        accountDao.delete(account)
    }

    suspend fun getDecryptedSecret(account: AccountEntity): ByteArray = withContext(Dispatchers.Default) {
        KeyStoreManager.decrypt(account.encryptedSecret)
    }

    suspend fun addPasskey(relyingParty: String, userName: String, note: String): Long = withContext(Dispatchers.IO) {
        val passkey = PasskeyEntity(
            relyingParty = relyingParty,
            userName = userName,
            credentialId = Base32.encode(relyingParty.toByteArray()),
            note = note
        )
        accountDao.insertPasskey(passkey)
    }

    suspend fun deletePasskey(passkey: PasskeyEntity) = withContext(Dispatchers.IO) {
        accountDao.deletePasskey(passkey)
    }

    /**
     * Exports all accounts to an encrypted JSON backup using user-supplied password.
     */
    suspend fun exportBackup(password: CharArray): String = withContext(Dispatchers.IO) {
        val list = accounts.first()
        val backupItems = list.map { acc ->
            val secretBytes = KeyStoreManager.decrypt(acc.encryptedSecret)
            val secretBase32 = Base32.encode(secretBytes)
            BackupAccountItem(
                serviceName = acc.serviceName,
                accountName = acc.accountName,
                secretBase32 = secretBase32,
                algorithm = acc.algorithm,
                digits = acc.digits,
                period = acc.period
            )
        }
        BackupCrypto.encryptBackup(backupItems, password)
    }

    /**
     * Imports accounts from an encrypted JSON backup using user-supplied password.
     */
    suspend fun importBackup(backupJson: String, password: CharArray): Int = withContext(Dispatchers.IO) {
        val items = BackupCrypto.decryptBackup(backupJson, password)
        var count = 0
        for (item in items) {
            val cleanSecret = Base32.clean(item.secretBase32)
            if (Base32.isValid(cleanSecret)) {
                val secretBytes = Base32.decode(cleanSecret)
                val encrypted = KeyStoreManager.encrypt(secretBytes)
                val entity = AccountEntity(
                    serviceName = item.serviceName.trim().ifEmpty { "Hesap" },
                    accountName = item.accountName.trim(),
                    encryptedSecret = encrypted,
                    algorithm = item.algorithm,
                    digits = item.digits,
                    period = item.period
                )
                accountDao.insert(entity)
                count++
            }
        }
        count
    }
}
