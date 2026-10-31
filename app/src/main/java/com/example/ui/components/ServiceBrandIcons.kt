package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

enum class BrandService(
    val brandName: String,
    val brandColor: Color,
    val icon: ImageVector?
) {
    GOOGLE("Google", Color(0xFFEA4335), Icons.Default.Mail),
    GITHUB("GitHub", Color(0xFF24292E), Icons.Default.Code),
    MICROSOFT("Microsoft", Color(0xFF00A4EF), Icons.Default.Public),
    DISCORD("Discord", Color(0xFF5865F2), Icons.Default.SportsEsports),
    STEAM("Steam", Color(0xFF171A21), Icons.Default.Gamepad),
    BINANCE("Binance", Color(0xFFF3BA2F), Icons.Default.Lock),
    AWS("AWS", Color(0xFFFF9900), Icons.Default.Storage),
    PROTON("Proton", Color(0xFF6D4AFF), Icons.Default.Shield),
    GITLAB("GitLab", Color(0xFFFC6D26), Icons.Default.Terminal)
}

object ServiceBrandResolver {
    fun resolve(serviceName: String): BrandService? {
        val s = serviceName.trim().lowercase()
        return when {
            s.contains("google") || s.contains("gmail") -> BrandService.GOOGLE
            s.contains("github") -> BrandService.GITHUB
            s.contains("microsoft") || s.contains("outlook") || s.contains("azure") -> BrandService.MICROSOFT
            s.contains("discord") -> BrandService.DISCORD
            s.contains("steam") -> BrandService.STEAM
            s.contains("binance") || s.contains("crypto") -> BrandService.BINANCE
            s.contains("aws") || s.contains("amazon") -> BrandService.AWS
            s.contains("proton") -> BrandService.PROTON
            s.contains("gitlab") -> BrandService.GITLAB
            else -> null
        }
    }

    fun getServiceColor(serviceName: String): Color {
        val brand = resolve(serviceName)
        if (brand != null) return brand.brandColor

        val palette = listOf(
            Color(0xFF4F46E5), // Indigo
            Color(0xFF0284C7), // Sky
            Color(0xFF059669), // Emerald
            Color(0xFFD97706), // Amber
            Color(0xFFDC2626), // Rose
            Color(0xFF7C3AED), // Violet
            Color(0xFF0D9488), // Teal
            Color(0xFFEA580C)  // Orange
        )
        val index = abs(serviceName.lowercase().hashCode()) % palette.size
        return palette[index]
    }
}

@Composable
fun ServiceBrandBadge(
    serviceName: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val brand = ServiceBrandResolver.resolve(serviceName)
    val color = ServiceBrandResolver.getServiceColor(serviceName)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(color)
    ) {
        if (brand?.icon != null) {
            Icon(
                imageVector = brand.icon,
                contentDescription = serviceName,
                tint = if (brand == BrandService.BINANCE) Color.Black else Color.White,
                modifier = Modifier.size(size * 0.55f)
            )
        } else {
            Text(
                text = serviceName.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.42f).sp
            )
        }
    }
}
