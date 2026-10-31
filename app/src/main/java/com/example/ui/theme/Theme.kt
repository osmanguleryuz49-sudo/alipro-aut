package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.data.repository.ThemeMode

private val DogrulaDarkColorScheme = darkColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = IndigoPrimaryVariant,
    onPrimaryContainer = Color.White,
    secondary = IndigoSecondary,
    onSecondary = Color.White,
    tertiary = IndigoTertiary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1),
    error = ExpiryAlert,
    onError = Color.White
)

private val DogrulaLightColorScheme = lightColorScheme(
    primary = IndigoPrimaryVariant,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = IndigoPrimaryVariant,
    secondary = IndigoSecondary,
    onSecondary = Color.White,
    tertiary = IndigoTertiary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF64748B),
    error = ExpiryAlert,
    onError = Color.White
)

private val DogrulaLiquidGlassColorScheme = darkColorScheme(
    primary = Color(0xFF67E8F9), // Luminous Neon Cyan / Sky
    onPrimary = Color(0xFF03101C),
    primaryContainer = Color(0x3300E5FF),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = Color(0xFFA78BFA), // Electric Violet
    onSecondary = Color(0xFF1E0E38),
    tertiary = Color(0xFFF472B6), // Neon Fuchsia
    background = Color(0xFF070B14), // Ultra deep midnight
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xF20B111E), // Crisp, high-contrast dark frosted obsidian for sheets, menus, dialogs
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFFCBD5E1),
    error = ExpiryAlert,
    onError = Color.White
)

@Composable
fun DogrulaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.LIQUID_GLASS -> true
    }

    val context = LocalContext.current
    val colorScheme = when {
        themeMode == ThemeMode.LIQUID_GLASS -> DogrulaLiquidGlassColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DogrulaDarkColorScheme
        else -> DogrulaLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
