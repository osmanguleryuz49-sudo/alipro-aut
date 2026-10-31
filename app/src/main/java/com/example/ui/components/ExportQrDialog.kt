package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.db.AccountEntity

@Composable
fun ExportQrDialog(
    account: AccountEntity,
    secretBase32: String,
    onDismiss: () -> Unit
) {
    // Generate standard otpauth:// URI for the account
    val uriString = remember(account, secretBase32) {
        val issuer = account.serviceName.ifEmpty { "Hesap" }
        val accountName = account.accountName
        val label = if (accountName.isNotEmpty()) "$issuer:$accountName" else issuer
        val type = if (account.type.equals("HOTP", ignoreCase = true)) "hotp" else "totp"
        val counterParam = if (type == "hotp") "&counter=${account.counter}" else ""
        "otpauth://$type/$label?secret=$secretBase32&issuer=$issuer&algorithm=${account.algorithm}&digits=${account.digits}&period=${account.period}$counterParam"
    }

    val qrBitmap: Bitmap? = remember(uriString) {
        QrCodeBitmapGenerator.createQrBitmap(uriString, size = 600)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.export_qr),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${account.serviceName} (${account.accountName})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("export_qr_content")
            ) {
                if (qrBitmap != null) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(12.dp)
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Transfer QR Kodu",
                            modifier = Modifier.size(216.dp)
                        )
                    }
                } else {
                    Text("QR Kod oluşturulamadı.")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.export_qr_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Tamam")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
