package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.Base32
import com.example.crypto.KeyStoreManager
import com.example.crypto.TotpGenerator
import com.example.data.db.AccountEntity
import com.example.data.db.AppDatabase
import com.example.data.db.PasskeyEntity
import com.example.data.repository.AccountRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.ThemeMode
import com.example.parser.ParsedOtpAccount
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

data class AccountUiModel(
    val entity: AccountEntity,
    val currentCode: String,
    val formattedCode: String,
    val nextCode: String,
    val remainingSeconds: Int,
    val progress: Float,
    val isNearExpiry: Boolean,
    val isCodeRevealed: Boolean,
    val isNextCodePeeked: Boolean
)

sealed interface UiMessage {
    data class Success(val message: String) : UiMessage
    data class Error(val message: String) : UiMessage
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    val accountRepository = AccountRepository(database.accountDao())
    val settingsRepository = SettingsRepository(application)

    // Decrypted secrets in memory cache for swift code calculation
    private val secretCache = ConcurrentHashMap<Long, ByteArray>()

    // Search query & category filter
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Tümü")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Reveal timer map: accountId -> expirationEpochMs
    private val _revealedMap = MutableStateFlow<Map<Long, Long>>(emptyMap())

    // Peek next code map: accountId -> expirationEpochMs
    private val _peekedMap = MutableStateFlow<Map<Long, Long>>(emptyMap())

    // Clock tick state to trigger recomputations
    private val _currentEpochMs = MutableStateFlow(System.currentTimeMillis())

    // Settings
    val isBiometricEnabled: StateFlow<Boolean> = settingsRepository.isBiometricEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isSecureScreenEnabled: StateFlow<Boolean> = settingsRepository.isSecureScreenEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isHideCodesEnabled: StateFlow<Boolean> = settingsRepository.isHideCodesEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isAutoClearClipboardEnabled: StateFlow<Boolean> = settingsRepository.isAutoClearClipboardEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val isDynamicColorEnabled: StateFlow<Boolean> = settingsRepository.isDynamicColorEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    // App Lock state
    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    // Passkeys flow
    val passkeys: StateFlow<List<PasskeyEntity>> = accountRepository.passkeys
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Snackbar / Toast events
    private val _uiMessage = MutableStateFlow<UiMessage?>(null)
    val uiMessage: StateFlow<UiMessage?> = _uiMessage.asStateFlow()

    private var clipboardJob: Job? = null

    init {
        // Synchronized 1-second clock tick for fluid, lag-free UI updates and optimal battery life
        viewModelScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                _currentEpochMs.value = now
                val msUntilNextSecond = 1000L - (now % 1000L)
                delay(msUntilNextSecond.coerceIn(50L, 1000L))
            }
        }
    }

    // Accounts UI Flow
    val accountList: StateFlow<List<AccountUiModel>> = combine(
        accountRepository.accounts,
        _searchQuery,
        _selectedCategory,
        _currentEpochMs,
        combine(isHideCodesEnabled, _revealedMap, _peekedMap) { hide, rev, peek ->
            Triple(hide, rev, peek)
        }
    ) { rawAccounts, query, category, nowMs, (hideCodes, revealedMap, peekedMap) ->
        val epochSec = nowMs / 1000L

        val categoryFiltered = if (category == "Tümü") {
            rawAccounts
        } else {
            rawAccounts.filter { it.category.equals(category, ignoreCase = true) }
        }

        val filtered = if (query.isBlank()) {
            categoryFiltered
        } else {
            val q = query.trim().lowercase()
            categoryFiltered.filter {
                it.serviceName.lowercase().contains(q) || it.accountName.lowercase().contains(q)
            }
        }

        filtered.map { account ->
            val secretBytes = secretCache.getOrPut(account.id) {
                try {
                    KeyStoreManager.decrypt(account.encryptedSecret)
                } catch (_: Exception) {
                    ByteArray(0)
                }
            }

            val code = TotpGenerator.generateCode(
                secretBytes = secretBytes,
                algorithm = account.algorithm,
                digits = account.digits,
                period = account.period,
                type = account.type,
                counter = account.counter,
                timeSeconds = epochSec
            )

            val nextCode = TotpGenerator.generateNextCode(
                secretBytes = secretBytes,
                algorithm = account.algorithm,
                digits = account.digits,
                period = account.period,
                type = account.type,
                counter = account.counter,
                timeSeconds = epochSec
            )

            val remainingSec = if (account.type.equals("HOTP", ignoreCase = true)) {
                0
            } else {
                TotpGenerator.getRemainingSeconds(account.period, epochSec)
            }

            // Accurate progress based on remaining seconds, smoothly animated in UI
            val smoothProgress = if (account.type.equals("HOTP", ignoreCase = true)) {
                1.0f
            } else {
                remainingSec.toFloat() / account.period.toFloat()
            }

            val isRevealed = if (!hideCodes) {
                true
            } else {
                val expiry = revealedMap[account.id] ?: 0L
                nowMs < expiry
            }

            val isPeeked = (peekedMap[account.id] ?: 0L) > nowMs

            AccountUiModel(
                entity = account,
                currentCode = code,
                formattedCode = TotpGenerator.formatCode(code),
                nextCode = TotpGenerator.formatCode(nextCode),
                remainingSeconds = remainingSec,
                progress = smoothProgress.coerceIn(0.0f, 1.0f),
                isNearExpiry = !account.type.equals("HOTP", ignoreCase = true) && remainingSec <= 5,
                isCodeRevealed = isRevealed,
                isNextCodePeeked = isPeeked
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun revealCode(accountId: Long) {
        val now = System.currentTimeMillis()
        val updated = _revealedMap.value.toMutableMap()
        updated[accountId] = now + 5000L // reveal for 5 seconds
        _revealedMap.value = updated
    }

    fun togglePeekNextCode(accountId: Long) {
        val now = System.currentTimeMillis()
        val updated = _peekedMap.value.toMutableMap()
        val currentExpiry = updated[accountId] ?: 0L
        if (currentExpiry > now) {
            updated.remove(accountId)
        } else {
            updated[accountId] = now + 8000L // peek for 8 seconds
        }
        _peekedMap.value = updated
    }

    fun handleCopiedCode(code: String) {
        if (!isAutoClearClipboardEnabled.value) return

        clipboardJob?.cancel()
        clipboardJob = viewModelScope.launch {
            delay(30_000L) // 30 seconds
            try {
                val clipboard = getApplication<Application>()
                    .getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val primaryClip = clipboard?.primaryClip
                if (primaryClip != null && primaryClip.itemCount > 0) {
                    val currentClipText = primaryClip.getItemAt(0).text?.toString()
                    if (currentClipText == code) {
                        clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                        _uiMessage.value = UiMessage.Success("Pano güvenlik için temizlendi")
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun togglePin(account: AccountEntity) {
        viewModelScope.launch {
            accountRepository.togglePin(account.id, account.isPinned)
        }
    }

    fun moveAccount(account: AccountEntity, direction: Int) {
        viewModelScope.launch {
            val newIndex = (account.orderIndex + direction).coerceAtLeast(0)
            accountRepository.updateOrderIndex(account.id, newIndex)
        }
    }

    fun incrementHotp(account: AccountEntity) {
        viewModelScope.launch {
            accountRepository.incrementHotpCounter(account.id)
        }
    }

    suspend fun getDecryptedSecretBase32(account: AccountEntity): String {
        return try {
            val bytes = KeyStoreManager.decrypt(account.encryptedSecret)
            Base32.encode(bytes)
        } catch (_: Exception) {
            ""
        }
    }

    fun setAppLocked(locked: Boolean) {
        _isAppLocked.value = locked
    }

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    fun showMessage(message: UiMessage) {
        _uiMessage.value = message
    }

    fun addManualAccount(
        serviceName: String,
        accountName: String,
        secret: String,
        algorithm: String,
        digits: Int,
        period: Int,
        type: String,
        category: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                accountRepository.addAccount(
                    serviceName = serviceName,
                    accountName = accountName,
                    secretBase32 = secret,
                    algorithm = algorithm,
                    digits = digits,
                    period = period,
                    type = type,
                    category = category
                )
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Hesap eklenirken bir hata oluştu")
            }
        }
    }

    fun addParsedAccounts(accounts: List<ParsedOtpAccount>, onSuccess: (Int) -> Unit) {
        viewModelScope.launch {
            val count = accountRepository.addAccounts(accounts)
            onSuccess(count)
        }
    }

    fun updateAccount(id: Long, serviceName: String, accountName: String, category: String) {
        viewModelScope.launch {
            accountRepository.updateAccountNames(id, serviceName, accountName, category)
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            secretCache.remove(account.id)
            accountRepository.deleteAccount(account)
        }
    }

    fun addPasskey(relyingParty: String, userName: String, note: String) {
        viewModelScope.launch {
            accountRepository.addPasskey(relyingParty, userName, note)
            _uiMessage.value = UiMessage.Success("Geçiş anahtarı kaydedildi")
        }
    }

    fun deletePasskey(passkey: PasskeyEntity) {
        viewModelScope.launch {
            accountRepository.deletePasskey(passkey)
            _uiMessage.value = UiMessage.Success("Anahtar silindi")
        }
    }

    suspend fun exportEncryptedBackup(password: CharArray): Result<String> {
        return try {
            val json = accountRepository.exportBackup(password)
            Result.success(json)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importEncryptedBackup(json: String, password: CharArray): Result<Int> {
        return try {
            val count = accountRepository.importBackup(json, password)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBiometricEnabled(enabled)
            if (!enabled) {
                _isAppLocked.value = false
            }
        }
    }

    fun setSecureScreenEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSecureScreenEnabled(enabled)
        }
    }

    fun setHideCodesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHideCodesEnabled(enabled)
        }
    }

    fun setAutoClearClipboardEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoClearClipboardEnabled(enabled)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicColorEnabled(enabled)
        }
    }
}
