package com.example.ui.components

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.AccountUiModel
import com.example.ui.theme.ExpiryAlert
import com.example.ui.theme.OtpCodeTextStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AccountCard(
    account: AccountUiModel,
    isLiquidGlass: Boolean,
    onCopyCode: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReveal: () -> Unit,
    onTogglePin: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onExportQr: () -> Unit,
    onTogglePeekNext: () -> Unit,
    onIncrementHotp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val animationsDisabled = AnimationUtils.areAnimationsDisabled(context)

    // Micro-interaction: Instant copy confirmation state
    var justCopied by remember { mutableStateOf(false) }

    // Press scale interaction feedback
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !animationsDisabled) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "card_scale"
    )

    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLiquidGlass) Color.Transparent else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isLiquidGlass) 0.dp else 2.dp,
            pressedElevation = if (isLiquidGlass) 0.dp else 6.dp
        ),
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .glassmorphicCard(cornerRadius = 22.dp, isLiquidGlass = isLiquidGlass)
            .testTag("account_card_${account.entity.id}")
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (!account.isCodeRevealed) {
                    onReveal()
                } else {
                    copyToClipboard(context, account.currentCode)
                    onCopyCode(account.currentCode)
                    justCopied = true
                    scope.launch {
                        delay(1600)
                        justCopied = false
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Branded Avatar, Service Name, Account Name, Pin Badge, Menu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Branded Service Icon
                ServiceBrandBadge(
                    serviceName = account.entity.serviceName,
                    size = 44.dp
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = account.entity.serviceName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        // Pinned Indicator Badge
                        if (account.entity.isPinned) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Sabitlendi",
                                tint = Color(0xFFF59E0B), // Gold
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Category Badge
                        if (account.entity.category.isNotBlank() && account.entity.category != "Genel") {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = account.entity.category,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (account.entity.accountName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = account.entity.accountName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Dropdown Menu Button
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("account_menu_${account.entity.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.edit),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.copy)) },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                copyToClipboard(context, account.currentCode)
                                onCopyCode(account.currentCode)
                                justCopied = true
                                scope.launch {
                                    delay(1600)
                                    justCopied = false
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (account.entity.isPinned) stringResource(R.string.unpin_account)
                                    else stringResource(R.string.pin_account)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.PushPin,
                                    contentDescription = null,
                                    tint = if (account.entity.isPinned) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                showMenu = false
                                onTogglePin()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.export_qr)) },
                            leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onExportQr()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.move_up)) },
                            leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onMoveUp()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.move_down)) },
                            leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onMoveDown()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit)) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Body Row: Formatted Code + Copy Badge + Countdown Ring / HOTP Refresh Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (account.isCodeRevealed) {
                        AnimatedContent(
                            targetState = account.formattedCode,
                            transitionSpec = {
                                if (animationsDisabled) {
                                    (fadeIn(tween(0)) togetherWith fadeOut(tween(0)))
                                } else {
                                    (slideInVertically(
                                        animationSpec = tween(durationMillis = 300),
                                        initialOffsetY = { fullHeight -> fullHeight / 2 }
                                    ) + fadeIn(tween(durationMillis = 300))).togetherWith(
                                        slideOutVertically(
                                            animationSpec = tween(durationMillis = 300),
                                            targetOffsetY = { fullHeight -> -fullHeight / 2 }
                                        ) + fadeOut(tween(durationMillis = 200))
                                    )
                                }
                            },
                            label = "otp_code_transition"
                        ) { codeText ->
                            Text(
                                text = codeText,
                                style = OtpCodeTextStyle,
                                color = if (account.isNearExpiry) ExpiryAlert
                                else if (isLiquidGlass) Color(0xFF67E8F9)
                                else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("otp_code_text")
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onReveal() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = stringResource(R.string.tap_to_reveal),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.tap_to_reveal),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Micro-interaction Detail 1: Instant floating "Kopyalandı!" pill badge
                    AnimatedVisibility(
                        visible = justCopied,
                        enter = fadeIn(tween(150)) + scaleIn(spring(dampingRatio = 0.6f)),
                        exit = fadeOut(tween(300)) + scaleOut(tween(200))
                    ) {
                        Surface(
                            color = if (isLiquidGlass) Color(0x3310B981) else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isLiquidGlass) Color(0xFF34D399) else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            modifier = Modifier.padding(start = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (isLiquidGlass) Color(0xFF34D399) else MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.copied_badge),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLiquidGlass) Color(0xFF34D399) else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // Timer Ring (for TOTP/Steam) OR Counter Refresh (for HOTP)
                if (account.entity.type.equals("HOTP", ignoreCase = true)) {
                    FilledTonalButton(
                        onClick = onIncrementHotp,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.refresh_hotp), fontSize = 12.sp)
                    }
                } else {
                    CountdownTimerRing(
                        progress = account.progress,
                        remainingSeconds = account.remainingSeconds,
                        isNearExpiry = account.isNearExpiry
                    )
                }
            }

            // Next Code Peek Row + Expiry Alert Hint (Micro-interaction Detail 2)
            if (!account.entity.type.equals("HOTP", ignoreCase = true)) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onTogglePeekNext() }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (account.isNextCodePeeked) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (account.isNextCodePeeked) stringResource(R.string.next_code_preview, account.nextCode)
                                else stringResource(R.string.next_code_peek),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (account.isNextCodePeeked) FontWeight.Bold else FontWeight.Normal,
                                color = if (account.isNextCodePeeked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Expiry alert indicator if near expiration
                    if (account.isNearExpiry) {
                        Surface(
                            color = ExpiryAlert.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(ExpiryAlert)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.expiring_soon_hint),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ExpiryAlert
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Copies the OTP code to system clipboard, marking it as sensitive on Android 13+ (API 33).
 */
private fun copyToClipboard(context: Context, code: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    val clip = ClipData.newPlainText("TOTP Code", code)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val bundle = PersistableBundle().apply {
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        }
        clip.description.extras = bundle
    }
    clipboard.setPrimaryClip(clip)
}
