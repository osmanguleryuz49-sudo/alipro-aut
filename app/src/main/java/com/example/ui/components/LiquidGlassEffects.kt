package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Refined, high-performance animated fluid liquid background for Liquid Glass theme.
 * Compact, elegant glowing orbs drift smoothly without overwhelming the interface or causing lag.
 */
@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_orbs")

    // Smooth continuous phase (10s cycle for gentle, relaxing ambient motion)
    val t by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time_phase"
    )

    // Pre-allocated brush color palettes to eliminate GC allocation churn in draw phase
    val violetColors = remember {
        listOf(
            Color(0x887C3AED), // Soft Electric Violet
            Color(0x356366F1), // Translucent Indigo
            Color(0x104F46E5),
            Color.Transparent
        )
    }
    val cyanColors = remember {
        listOf(
            Color(0x9900F0FF), // Soft Neon Cyan
            Color(0x400284C7), // Sky Blue
            Color(0x100369A1),
            Color.Transparent
        )
    }
    val coralColors = remember {
        listOf(
            Color(0x77F43F5E), // Delicate Coral Rose
            Color(0x28EC4899),
            Color.Transparent
        )
    }
    val sapphireColors = remember {
        listOf(
            Color(0x773B82F6), // Royal Sapphire
            Color(0x251D4ED8),
            Color.Transparent
        )
    }
    val causticColors = remember {
        listOf(
            Color.Transparent,
            Color(0x15FFFFFF),
            Color(0x2567E8F9),
            Color(0x15FFFFFF),
            Color.Transparent
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF080D1A))
            .graphicsLayer() // GPU hardware layer isolation
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Violet Fluid Orb (Top Right / Center) - Compact size (w * 0.36f)
            val orb1X = w * (0.62f + 0.16f * sin(t.toDouble()).toFloat())
            val orb1Y = h * (0.18f + 0.10f * cos(t.toDouble()).toFloat())
            val orb1Radius = w * 0.36f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = violetColors,
                    center = Offset(orb1X, orb1Y),
                    radius = orb1Radius
                ),
                radius = orb1Radius,
                center = Offset(orb1X, orb1Y)
            )

            // 2. Cyan Fluid Orb (Bottom Left / Center) - Compact size (w * 0.38f)
            val orb2X = w * (0.28f + 0.14f * cos((t * 0.8f).toDouble()).toFloat())
            val orb2Y = h * (0.65f + 0.12f * sin((t * 0.8f).toDouble()).toFloat())
            val orb2Radius = w * 0.38f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = cyanColors,
                    center = Offset(orb2X, orb2Y),
                    radius = orb2Radius
                ),
                radius = orb2Radius,
                center = Offset(orb2X, orb2Y)
            )

            // 3. Coral Accent Orb (Bottom Right) - Smaller subtle light pocket (w * 0.28f)
            val orb3X = w * (0.78f + 0.10f * cos((t * 1.1f + 1f).toDouble()).toFloat())
            val orb3Y = h * (0.82f + 0.08f * sin((t * 1.1f).toDouble()).toFloat())
            val orb3Radius = w * 0.28f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = coralColors,
                    center = Offset(orb3X, orb3Y),
                    radius = orb3Radius
                ),
                radius = orb3Radius,
                center = Offset(orb3X, orb3Y)
            )

            // 4. Sapphire Deep Accent Orb (Top Left) - Compact (w * 0.32f)
            val orb4X = w * (0.20f + 0.10f * sin((t * 0.7f + 2f).toDouble()).toFloat())
            val orb4Y = h * (0.12f + 0.08f * cos((t * 0.7f).toDouble()).toFloat())
            val orb4Radius = w * 0.32f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = sapphireColors,
                    center = Offset(orb4X, orb4Y),
                    radius = orb4Radius
                ),
                radius = orb4Radius,
                center = Offset(orb4X, orb4Y)
            )

            // 5. Delicate, subtle glass sheen line (strokeWidth = 36f instead of 140f)
            val sheenProgress = (sin(t.toDouble() * 0.4) * 0.5 + 0.5).toFloat()
            val sheenY = h * (0.20f + 0.60f * sheenProgress)
            drawLine(
                brush = Brush.linearGradient(
                    colors = causticColors,
                    start = Offset(0f, sheenY - 50f),
                    end = Offset(w, sheenY + 50f)
                ),
                start = Offset(0f, sheenY - 50f),
                end = Offset(w, sheenY + 50f),
                strokeWidth = 36f
            )
        }

        // Frosted Dark Obsidian Tint Overlay for crisp text contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x40070B14),
                            Color(0x20070B14),
                            Color(0x55070B14)
                        )
                    )
                )
        )
    }
}

/**
 * Modifier extension to apply authentic frosted smoked glassmorphic card styling with specular prism border.
 * Features an opaque-enough obsidian core (88-94%) to guarantee zero bleed-through while preserving
 * gorgeous refractive edge highlights and cyan/violet light catching.
 */
fun Modifier.glassmorphicCard(
    cornerRadius: Dp = 22.dp,
    isLiquidGlass: Boolean = false
): Modifier {
    if (!isLiquidGlass) return this

    val shape = RoundedCornerShape(cornerRadius)

    val glassBorderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.50f),
            Color(0x4567E8F9), // Cyan specular highlight
            Color.White.copy(alpha = 0.12f),
            Color(0x45A78BFA), // Violet specular refraction
            Color.White.copy(alpha = 0.08f)
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    // Rich smoked obsidian frosted glass backing: prevents any behind-element bleed-through
    val glassBackgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xEB111827), // 92% smoked glass at top with subtle light catch
            Color(0xEE0F172A), // 93% deep slate core
            Color(0xF5080E1A)  // 96% obsidian base for maximum contrast
        )
    )

    return this
        .clip(shape)
        .background(glassBackgroundBrush, shape)
        .border(1.2.dp, glassBorderBrush, shape)
}
