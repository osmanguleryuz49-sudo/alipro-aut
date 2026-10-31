package com.example.ui.components

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.ThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    onDismiss: () -> Unit,
    isBiometricEnabled: Boolean,
    onToggleBiometric: (Boolean) -> Unit,
    isSecureScreenEnabled: Boolean,
    onToggleSecureScreen: (Boolean) -> Unit,
    isHideCodesEnabled: Boolean,
    onToggleHideCodes: (Boolean) -> Unit,
    isAutoClearClipboardEnabled: Boolean,
    onToggleAutoClearClipboard: (Boolean) -> Unit,
    themeMode: ThemeMode,
    onChangeThemeMode: (ThemeMode) -> Unit,
    isDynamicColorEnabled: Boolean,
    onToggleDynamicColor: (Boolean) -> Unit,
    onOpenPasskeyVault: () -> Unit,
    onExportRequested: (password: CharArray, uri: Uri) -> Unit,
    onImportRequested: (password: CharArray, uri: Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var pendingExportUri by remember { mutableStateOf<Uri?>(null) }

    var showImportPasswordDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    // SAF Document Launchers
    val exportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            pendingExportUri = uri
            showExportPasswordDialog = true
        }
    }

    val importDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingImportUri = uri
            showImportPasswordDialog = true
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("settings_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 18.dp)
            )

            // Section: Güvenlik
            SettingsSectionHeader(title = stringResource(R.string.security))

            SettingsSwitchTile(
                icon = Icons.Default.Fingerprint,
                title = stringResource(R.string.biometric_lock),
                subtitle = stringResource(R.string.biometric_lock_desc),
                checked = isBiometricEnabled,
                onCheckedChange = onToggleBiometric,
                testTag = "switch_biometric"
            )

            SettingsSwitchTile(
                icon = Icons.Default.Security,
                title = stringResource(R.string.secure_screen),
                subtitle = stringResource(R.string.secure_screen_desc),
                checked = isSecureScreenEnabled,
                onCheckedChange = onToggleSecureScreen,
                testTag = "switch_secure_screen"
            )

            SettingsSwitchTile(
                icon = Icons.Default.VisibilityOff,
                title = stringResource(R.string.hide_codes),
                subtitle = stringResource(R.string.hide_codes_desc),
                checked = isHideCodesEnabled,
                onCheckedChange = onToggleHideCodes,
                testTag = "switch_hide_codes"
            )

            SettingsSwitchTile(
                icon = Icons.Default.AutoDelete,
                title = stringResource(R.string.auto_clear_clipboard),
                subtitle = stringResource(R.string.auto_clear_clipboard_desc),
                checked = isAutoClearClipboardEnabled,
                onCheckedChange = onToggleAutoClearClipboard,
                testTag = "switch_auto_clear"
            )

            // Passkey Vault Entry
            SettingsActionTile(
                icon = Icons.Default.VpnKey,
                title = stringResource(R.string.passkey_vault),
                subtitle = stringResource(R.string.passkey_vault_desc),
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onOpenPasskeyVault() }
                },
                testTag = "tile_passkey_vault"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Section: Görünüm
            SettingsSectionHeader(title = stringResource(R.string.appearance))

            // Theme Mode Segment (including Liquid Glass!)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.theme),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        FilterChip(
                            selected = themeMode == ThemeMode.SYSTEM,
                            onClick = { onChangeThemeMode(ThemeMode.SYSTEM) },
                            label = { Text(stringResource(R.string.theme_system), fontSize = 11.sp) },
                            modifier = Modifier.testTag("theme_chip_system")
                        )
                        FilterChip(
                            selected = themeMode == ThemeMode.LIGHT,
                            onClick = { onChangeThemeMode(ThemeMode.LIGHT) },
                            label = { Text(stringResource(R.string.theme_light), fontSize = 11.sp) },
                            modifier = Modifier.testTag("theme_chip_light")
                        )
                        FilterChip(
                            selected = themeMode == ThemeMode.DARK,
                            onClick = { onChangeThemeMode(ThemeMode.DARK) },
                            label = { Text(stringResource(R.string.theme_dark), fontSize = 11.sp) },
                            modifier = Modifier.testTag("theme_chip_dark")
                        )
                        FilterChip(
                            selected = themeMode == ThemeMode.LIQUID_GLASS,
                            onClick = { onChangeThemeMode(ThemeMode.LIQUID_GLASS) },
                            label = { Text(stringResource(R.string.theme_liquid_glass), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("theme_chip_liquid_glass")
                        )
                    }
                }
            }

            // Android 12+ Dynamic Color
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && themeMode != ThemeMode.LIQUID_GLASS) {
                SettingsSwitchTile(
                    icon = Icons.Default.Palette,
                    title = stringResource(R.string.dynamic_color),
                    subtitle = stringResource(R.string.dynamic_color_desc),
                    checked = isDynamicColorEnabled,
                    onCheckedChange = onToggleDynamicColor,
                    testTag = "switch_dynamic_color"
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section: Yedekleme ve Geri Yükleme
            SettingsSectionHeader(title = stringResource(R.string.backup_restore))

            SettingsActionTile(
                icon = Icons.Default.Upload,
                title = stringResource(R.string.export_backup),
                subtitle = stringResource(R.string.export_backup_desc),
                onClick = {
                    val defaultName = "dogrula_backup_${System.currentTimeMillis() / 1000L}.json"
                    exportDocumentLauncher.launch(defaultName)
                },
                testTag = "tile_export_backup"
            )

            SettingsActionTile(
                icon = Icons.Default.Download,
                title = stringResource(R.string.import_backup),
                subtitle = stringResource(R.string.import_backup_desc),
                onClick = {
                    importDocumentLauncher.launch(arrayOf("application/json", "*/*"))
                },
                testTag = "tile_import_backup"
            )
        }
    }

    // Export Password Prompt Dialog
    if (showExportPasswordDialog && pendingExportUri != null) {
        ExportPasswordDialog(
            onDismiss = {
                showExportPasswordDialog = false
                pendingExportUri = null
            },
            onConfirm = { password ->
                showExportPasswordDialog = false
                pendingExportUri?.let { uri ->
                    onExportRequested(password, uri)
                }
                pendingExportUri = null
            }
        )
    }

    // Import Password Prompt Dialog
    if (showImportPasswordDialog && pendingImportUri != null) {
        ImportPasswordDialog(
            onDismiss = {
                showImportPasswordDialog = false
                pendingImportUri = null
            },
            onConfirm = { password ->
                showImportPasswordDialog = false
                pendingImportUri?.let { uri ->
                    onImportRequested(password, uri)
                }
                pendingImportUri = null
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

@Composable
private fun SettingsSwitchTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}

@Composable
private fun SettingsActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ExportPasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (CharArray) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.export_password_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.export_password_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_password_input")
                )

                OutlinedTextField(
                    value = passwordConfirm,
                    onValueChange = {
                        passwordConfirm = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.password_confirm)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_password_confirm_input")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (password.length < 6) {
                        errorMessage = "Parola en az 6 karakter olmalıdır"
                        return@Button
                    }
                    if (password != passwordConfirm) {
                        errorMessage = "Parolalar eşleşmiyor"
                        return@Button
                    }
                    onConfirm(password.toCharArray())
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_export_button")
            ) {
                Text(stringResource(R.string.export_backup))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun ImportPasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (CharArray) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.import_password_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.import_password_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_password_input")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (password.isEmpty()) {
                        errorMessage = "Lütfen parolayı girin"
                        return@Button
                    }
                    onConfirm(password.toCharArray())
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_import_button")
            ) {
                Text(stringResource(R.string.import_backup))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
