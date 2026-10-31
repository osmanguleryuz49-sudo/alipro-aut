package com.example.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.db.AccountEntity
import com.example.data.repository.ThemeMode
import com.example.ui.components.AccountCard
import com.example.ui.components.AddAccountBottomSheet
import com.example.ui.components.AnimationUtils
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.EditAccountDialog
import com.example.ui.components.EmptyState
import com.example.ui.components.ExportQrDialog
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.PasskeyVaultDialog
import com.example.ui.components.SettingsSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onTriggerBiometricPrompt: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val animationsDisabled = AnimationUtils.areAnimationsDisabled(context)

    val accounts by viewModel.accountList.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()
    val passkeys by viewModel.passkeys.collectAsStateWithLifecycle()

    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
    val isSecureScreenEnabled by viewModel.isSecureScreenEnabled.collectAsStateWithLifecycle()
    val isHideCodesEnabled by viewModel.isHideCodesEnabled.collectAsStateWithLifecycle()
    val isAutoClearClipboardEnabled by viewModel.isAutoClearClipboardEnabled.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isDynamicColorEnabled by viewModel.isDynamicColorEnabled.collectAsStateWithLifecycle()

    val isLiquidGlass = themeMode == ThemeMode.LIQUID_GLASS

    var showSearchRow by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showPasskeyVault by remember { mutableStateOf(false) }

    var accountToEdit by remember { mutableStateOf<AccountEntity?>(null) }
    var accountToDelete by remember { mutableStateOf<AccountEntity?>(null) }

    // Account to export as QR
    var accountToExportQr by remember { mutableStateOf<AccountEntity?>(null) }
    var exportSecretBase32 by remember { mutableStateOf("") }

    // Listen for UiMessage events
    LaunchedEffect(uiMessage) {
        uiMessage?.let { msg ->
            when (msg) {
                is UiMessage.Success -> snackbarHostState.showSnackbar(msg.message)
                is UiMessage.Error -> snackbarHostState.showSnackbar(msg.message)
            }
            viewModel.clearUiMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Liquid Glass morphing fluid animation background
        if (isLiquidGlass) {
            LiquidGlassBackground()
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = if (isLiquidGlass) Color.Transparent else MaterialTheme.colorScheme.background,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isLiquidGlass) Color(0x75070B14) else MaterialTheme.colorScheme.background)
                ) {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White)
                                        .padding(2.dp)
                                ) {
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(R.drawable.ic_alipro_icon),
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (accounts.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = stringResource(R.string.total_accounts, accounts.size),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            // Passkey Vault Quick Access
                            IconButton(
                                onClick = { showPasskeyVault = true },
                                modifier = Modifier.testTag("passkey_vault_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = "Passkey Kasası",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(
                                onClick = {
                                    showSearchRow = !showSearchRow
                                    if (!showSearchRow) viewModel.setSearchQuery("")
                                },
                                modifier = Modifier.testTag("search_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (showSearchRow) Icons.Default.Clear else Icons.Default.Search,
                                    contentDescription = "Ara",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(
                                onClick = { showSettingsSheet = true },
                                modifier = Modifier.testTag("settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = stringResource(R.string.settings),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )

                    // Search Bar animated expansion
                    AnimatedVisibility(
                        visible = showSearchRow,
                        enter = if (animationsDisabled) fadeIn() else expandVertically() + fadeIn(),
                        exit = if (animationsDisabled) fadeOut() else shrinkVertically() + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text(stringResource(R.string.search_hint)) },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Temizle")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_text_field")
                            )
                        }
                    }

                    // Category Filter Chips Row (Small Feature #3)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        listOf("Tümü", "İş", "Kişisel", "Kripto", "Oyun").forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { viewModel.setSelectedCategory(category) },
                                label = { Text(category, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                shape = RoundedCornerShape(12.dp),
                                colors = if (isLiquidGlass) {
                                    androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0x4067E8F9),
                                        selectedLabelColor = Color(0xFF67E8F9),
                                        containerColor = Color(0x18FFFFFF),
                                        labelColor = Color(0xFFCBD5E1)
                                    )
                                } else {
                                    androidx.compose.material3.FilterChipDefaults.filterChipColors()
                                }
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                // FAB with scale press feedback
                val fabInteractionSource = remember { MutableInteractionSource() }
                val isFabPressed by fabInteractionSource.collectIsPressedAsState()
                val fabScale by animateFloatAsState(
                    targetValue = if (isFabPressed && !animationsDisabled) 0.90f else 1f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                    label = "fab_scale"
                )

                FloatingActionButton(
                    onClick = { showAddSheet = true },
                    interactionSource = fabInteractionSource,
                    shape = RoundedCornerShape(18.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier
                        .scale(fabScale)
                        .testTag("add_account_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_account),
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (accounts.isEmpty()) {
                    EmptyState(
                        onAddClick = { showAddSheet = true },
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("accounts_list")
                    ) {
                        items(
                            items = accounts,
                            key = { it.entity.id }
                        ) { account ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { dismissValue ->
                                    if (dismissValue == SwipeToDismissBoxValue.EndToStart ||
                                        dismissValue == SwipeToDismissBoxValue.StartToEnd
                                    ) {
                                        accountToDelete = account.entity
                                        false
                                    } else {
                                        false
                                    }
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = false,
                                backgroundContent = {
                                    val isSwiping = dismissState.targetValue != SwipeToDismissBoxValue.Settled ||
                                        dismissState.currentValue != SwipeToDismissBoxValue.Settled
                                    if (isSwiping) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(22.dp))
                                                .background(MaterialTheme.colorScheme.errorContainer)
                                                .padding(horizontal = 20.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = stringResource(R.string.delete),
                                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = stringResource(R.string.delete),
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.animateItem()
                            ) {
                                AccountCard(
                                    account = account,
                                    isLiquidGlass = isLiquidGlass,
                                    onCopyCode = { code ->
                                        viewModel.handleCopiedCode(code)
                                        scope.launch {
                                            snackbarHostState.showSnackbar(context.getString(R.string.code_copied))
                                        }
                                    },
                                    onEdit = { accountToEdit = account.entity },
                                    onDelete = { accountToDelete = account.entity },
                                    onReveal = { viewModel.revealCode(account.entity.id) },
                                    onTogglePin = { viewModel.togglePin(account.entity) },
                                    onMoveUp = { viewModel.moveAccount(account.entity, -1) },
                                    onMoveDown = { viewModel.moveAccount(account.entity, 1) },
                                    onExportQr = {
                                        scope.launch {
                                            val secret = viewModel.getDecryptedSecretBase32(account.entity)
                                            exportSecretBase32 = secret
                                            accountToExportQr = account.entity
                                        }
                                    },
                                    onTogglePeekNext = { viewModel.togglePeekNextCode(account.entity.id) },
                                    onIncrementHotp = { viewModel.incrementHotp(account.entity) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Account BottomSheet
    if (showAddSheet) {
        AddAccountBottomSheet(
            onDismiss = { showAddSheet = false },
            onAccountsParsed = { parsedList ->
                showAddSheet = false
                viewModel.addParsedAccounts(parsedList) { count ->
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.migration_success, count))
                    }
                }
            },
            onManualSave = { service, account, secret, alg, digits, period, type, category ->
                showAddSheet = false
                viewModel.addManualAccount(
                    serviceName = service,
                    accountName = account,
                    secret = secret,
                    algorithm = alg,
                    digits = digits,
                    period = period,
                    type = type,
                    category = category,
                    onSuccess = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Hesap başarıyla eklendi")
                        }
                    },
                    onError = { err ->
                        scope.launch {
                            snackbarHostState.showSnackbar(err)
                        }
                    }
                )
            }
        )
    }

    // Edit Account Dialog
    accountToEdit?.let { entity ->
        EditAccountDialog(
            account = entity,
            onDismiss = { accountToEdit = null },
            onSave = { service, account, category ->
                viewModel.updateAccount(entity.id, service, account, category)
                accountToEdit = null
                scope.launch {
                    snackbarHostState.showSnackbar("Hesap güncellendi")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    accountToDelete?.let { entity ->
        DeleteConfirmDialog(
            account = entity,
            onDismiss = { accountToDelete = null },
            onConfirm = {
                viewModel.deleteAccount(entity)
                accountToDelete = null
                scope.launch {
                    snackbarHostState.showSnackbar("Hesap silindi")
                }
            }
        )
    }

    // Export QR Modal for Device-to-Device transfer
    accountToExportQr?.let { entity ->
        ExportQrDialog(
            account = entity,
            secretBase32 = exportSecretBase32,
            onDismiss = {
                accountToExportQr = null
                exportSecretBase32 = ""
            }
        )
    }

    // Passkey Vault Dialog
    if (showPasskeyVault) {
        PasskeyVaultDialog(
            passkeys = passkeys,
            onAddPasskey = { relyingParty, user, note ->
                viewModel.addPasskey(relyingParty, user, note)
            },
            onDeletePasskey = { passkey ->
                viewModel.deletePasskey(passkey)
            },
            onDismiss = { showPasskeyVault = false }
        )
    }

    // Settings BottomSheet
    if (showSettingsSheet) {
        SettingsSheet(
            onDismiss = { showSettingsSheet = false },
            isBiometricEnabled = isBiometricEnabled,
            onToggleBiometric = { enabled ->
                if (enabled) {
                    onTriggerBiometricPrompt()
                } else {
                    viewModel.setBiometricEnabled(false)
                }
            },
            isSecureScreenEnabled = isSecureScreenEnabled,
            onToggleSecureScreen = { viewModel.setSecureScreenEnabled(it) },
            isHideCodesEnabled = isHideCodesEnabled,
            onToggleHideCodes = { viewModel.setHideCodesEnabled(it) },
            isAutoClearClipboardEnabled = isAutoClearClipboardEnabled,
            onToggleAutoClearClipboard = { viewModel.setAutoClearClipboardEnabled(it) },
            themeMode = themeMode,
            onChangeThemeMode = { viewModel.setThemeMode(it) },
            isDynamicColorEnabled = isDynamicColorEnabled,
            onToggleDynamicColor = { viewModel.setDynamicColorEnabled(it) },
            onOpenPasskeyVault = { showPasskeyVault = true },
            onExportRequested = { password, uri ->
                scope.launch {
                    val result = viewModel.exportEncryptedBackup(password)
                    result.onSuccess { jsonString ->
                        try {
                            context.contentResolver.openOutputStream(uri)?.use { os ->
                                os.write(jsonString.toByteArray(Charsets.UTF_8))
                            }
                            snackbarHostState.showSnackbar(context.getString(R.string.export_success))
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Yedek yazılamadı: ${e.localizedMessage}")
                        }
                    }.onFailure { e ->
                        snackbarHostState.showSnackbar("Yedekleme hatası: ${e.localizedMessage}")
                    }
                }
            },
            onImportRequested = { password, uri ->
                scope.launch {
                    try {
                        val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader().use {
                            it?.readText()
                        }
                        if (jsonString.isNullOrBlank()) {
                            snackbarHostState.showSnackbar("Seçilen dosya boş veya okunamadı")
                            return@launch
                        }

                        val result = viewModel.importEncryptedBackup(jsonString, password)
                        result.onSuccess { count ->
                            snackbarHostState.showSnackbar(context.getString(R.string.import_success, count))
                        }.onFailure {
                            snackbarHostState.showSnackbar(context.getString(R.string.import_failed))
                        }
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Dosya açılırken hata: ${e.localizedMessage}")
                    }
                }
            }
        )
    }
}
