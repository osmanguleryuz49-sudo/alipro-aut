package com.example.ui.components

import android.Manifest
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.example.R
import com.example.crypto.Base32
import com.example.parser.OtpUriParser
import com.example.parser.ParsedOtpAccount
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountBottomSheet(
    onDismiss: () -> Unit,
    onAccountsParsed: (List<ParsedOtpAccount>) -> Unit,
    onManualSave: (service: String, account: String, secret: String, alg: String, digits: Int, period: Int, type: String, category: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: QR Camera, 1: Gallery, 2: Manual

    // Camera permission check
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PermissionChecker.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            processQrFromImageUri(context, uri) { parsed ->
                if (parsed.isNotEmpty()) {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onAccountsParsed(parsed) }
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("add_account_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.add_account),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Tabs: Kamera, Galeri, Manuel
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.scan_qr), fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                    modifier = Modifier.testTag("tab_qr")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.scan_from_image), fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Image, contentDescription = null) },
                    modifier = Modifier.testTag("tab_gallery")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(stringResource(R.string.manual_entry), fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    modifier = Modifier.testTag("tab_manual")
                )
            }

            when (selectedTab) {
                0 -> {
                    // QR Camera Scanner Tab
                    if (hasCameraPermission) {
                        QrCameraScanner(
                            onQrCodeDetected = { rawUri ->
                                val accounts = OtpUriParser.parse(rawUri)
                                if (accounts.isNotEmpty()) {
                                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                                        onAccountsParsed(accounts)
                                    }
                                }
                            }
                        )
                    } else {
                        CameraPermissionRationaleCard(
                            onRequestPermission = {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            onSwitchToGallery = { selectedTab = 1 }
                        )
                    }
                }
                1 -> {
                    // Gallery Picker Tab
                    GalleryPickerContent(
                        onPickClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                }
                2 -> {
                    // Manual Entry Tab
                    ManualEntryForm(
                        onSave = { service, account, secret, alg, digits, period, type, category ->
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                onManualSave(service, account, secret, alg, digits, period, type, category)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionRationaleCard(
    onRequestPermission: () -> Unit,
    onSwitchToGallery: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("camera_permission_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.camera_permission_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.camera_permission_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onSwitchToGallery,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.scan_from_image))
                }
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(R.string.camera_permission_grant))
                }
            }
        }
    }
}

@Composable
private fun GalleryPickerContent(
    onPickClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
            .testTag("gallery_picker_section")
    ) {
        Icon(
            imageVector = Icons.Default.Image,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Galeriden QR Kod Görseli Seçin",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Ekran görüntüsü veya fotoğraflardaki 2FA QR kodları taranarak hesabınız eklenir.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onPickClick,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.testTag("pick_image_button")
        ) {
            Icon(Icons.Default.Image, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.pick_image), fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualEntryForm(
    onSave: (service: String, account: String, secret: String, alg: String, digits: Int, period: Int, type: String, category: String) -> Unit
) {
    var serviceName by remember { mutableStateOf("") }
    var accountName by remember { mutableStateOf("") }
    var secretKey by remember { mutableStateOf("") }
    var selectedAlgorithm by remember { mutableStateOf("SHA1") }
    var selectedDigits by remember { mutableIntStateOf(6) }
    var periodString by remember { mutableStateOf("30") }
    var selectedType by remember { mutableStateOf("TOTP") } // TOTP, HOTP, STEAM
    var selectedCategory by remember { mutableStateOf("Genel") } // Genel, İş, Kişisel, Kripto, Oyun

    var serviceError by remember { mutableStateOf<String?>(null) }
    var secretError by remember { mutableStateOf<String?>(null) }

    var algDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("manual_entry_form")
    ) {
        // Type Selector: TOTP, HOTP, Steam Guard
        Text(
            text = stringResource(R.string.otp_type),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = selectedType == "TOTP",
                onClick = { selectedType = "TOTP" },
                label = { Text("TOTP (Zaman)") }
            )
            FilterChip(
                selected = selectedType == "HOTP",
                onClick = { selectedType = "HOTP" },
                label = { Text("HOTP (Sayaç)") }
            )
            FilterChip(
                selected = selectedType == "STEAM",
                onClick = {
                    selectedType = "STEAM"
                    serviceName = "Steam"
                    selectedDigits = 5
                },
                label = { Text("Steam Guard") }
            )
        }

        // Service Name Field
        OutlinedTextField(
            value = serviceName,
            onValueChange = {
                serviceName = it
                if (serviceError != null) serviceError = null
            },
            label = { Text(stringResource(R.string.service_name)) },
            placeholder = { Text(stringResource(R.string.service_name_hint)) },
            isError = serviceError != null,
            supportingText = serviceError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("manual_service_input")
        )

        // Account Name Field
        OutlinedTextField(
            value = accountName,
            onValueChange = { accountName = it },
            label = { Text(stringResource(R.string.account_name)) },
            placeholder = { Text(stringResource(R.string.account_name_hint)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("manual_account_input")
        )

        // Category Selector Chips
        Text(
            text = stringResource(R.string.category),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("Genel", "İş", "Kişisel", "Kripto", "Oyun").forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontSize = 12.sp) }
                )
            }
        }

        // Secret Key Field with real-time validation
        val cleanedSecret = Base32.clean(secretKey)
        val isSecretValid = cleanedSecret.isNotEmpty() && Base32.isValid(cleanedSecret)

        OutlinedTextField(
            value = secretKey,
            onValueChange = {
                secretKey = it
                if (secretError != null) secretError = null
            },
            label = { Text(stringResource(R.string.secret_key)) },
            placeholder = { Text(stringResource(R.string.secret_key_hint)) },
            isError = secretError != null || (secretKey.isNotBlank() && !isSecretValid),
            supportingText = {
                if (secretError != null) {
                    Text(secretError!!, color = MaterialTheme.colorScheme.error)
                } else if (secretKey.isNotBlank() && !isSecretValid) {
                    Text(stringResource(R.string.invalid_secret), color = MaterialTheme.colorScheme.error)
                }
            },
            trailingIcon = {
                if (isSecretValid) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Geçerli",
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else if (secretKey.isNotBlank()) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Geçersiz",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("manual_secret_input")
        )

        // Algorithm & Digits
        if (selectedType != "STEAM") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Algorithm Dropdown
                ExposedDropdownMenuBox(
                    expanded = algDropdownExpanded,
                    onExpandedChange = { algDropdownExpanded = !algDropdownExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedAlgorithm,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.algorithm)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = algDropdownExpanded) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = algDropdownExpanded,
                        onDismissRequest = { algDropdownExpanded = false }
                    ) {
                        listOf("SHA1", "SHA256", "SHA512").forEach { alg ->
                            DropdownMenuItem(
                                text = { Text(alg) },
                                onClick = {
                                    selectedAlgorithm = alg
                                    algDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Digits Selector
                OutlinedTextField(
                    value = "$selectedDigits hane",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.digits)) },
                    shape = RoundedCornerShape(14.dp),
                    trailingIcon = {
                        OutlinedButton(
                            onClick = { selectedDigits = if (selectedDigits == 6) 8 else 6 },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = ButtonDefaults.TextButtonContentPadding,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(if (selectedDigits == 6) "8'e Geç" else "6'ya Geç", fontSize = 11.sp)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Save Button
        Button(
            onClick = {
                val clean = Base32.clean(secretKey)
                if (serviceName.isBlank()) {
                    serviceError = "Lütfen servis adını girin"
                    return@Button
                }
                if (clean.isBlank()) {
                    secretError = "Lütfen gizli anahtarı girin"
                    return@Button
                }
                if (!Base32.isValid(clean)) {
                    secretError = "Geçersiz Base32 gizli anahtar"
                    return@Button
                }

                val period = periodString.toIntOrNull() ?: 30
                val digits = if (selectedType == "STEAM") 5 else selectedDigits
                onSave(serviceName.trim(), accountName.trim(), clean, selectedAlgorithm, digits, period, selectedType, selectedCategory)
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("manual_save_button")
        ) {
            Text(stringResource(R.string.save), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

/**
 * Reads a bitmap from the given image URI and passes it through ML Kit BarcodeScanner.
 */
private fun processQrFromImageUri(
    context: Context,
    uri: Uri,
    onResult: (List<ParsedOtpAccount>) -> Unit
) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        if (bitmap == null) return

        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val scannerOptions = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        val scanner = BarcodeScanning.getClient(scannerOptions)

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                for (b in barcodes) {
                    val raw = b.rawValue
                    if (!raw.isNullOrBlank()) {
                        val parsed = OtpUriParser.parse(raw)
                        if (parsed.isNotEmpty()) {
                            onResult(parsed)
                            return@addOnSuccessListener
                        }
                    }
                }
                onResult(emptyList())
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    } catch (_: Exception) {
        onResult(emptyList())
    }
}
