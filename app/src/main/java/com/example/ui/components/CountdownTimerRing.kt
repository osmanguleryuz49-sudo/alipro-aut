package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ExpiryAlert

@Composable
fun CountdownTimerRing(
    progress: Float,
    remainingSeconds: Int,
    isNearExpiry: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    strokeWidth: Dp = 4.dp
) {
    val context = LocalContext.current
    val animationsDisabled = AnimationUtils.areAnimationsDisabled(context)

    // Pulse animation only when expiring (last 5 seconds) to avoid background CPU churn
    val pulseScale by if (isNearExpiry && !animationsDisabled) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    // Smooth sub-second sweep animation between second updates
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (animationsDisabled || progress >= 0.95f) {
            tween(0)
        } else {
            tween(durationMillis = 1000, easing = LinearEasing)
        },
        label = "ring_progress"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    val ringColor by animateColorAsState(
        targetValue = if (isNearExpiry) ExpiryAlert else primaryColor,
        animationSpec = if (animationsDisabled) tween(0) else tween(durationMillis = 300),
        label = "ring_color"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = if (!animationsDisabled) pulseScale else 1f
                scaleY = if (!animationsDisabled) pulseScale else 1f
            }
            .testTag("timer_ring")
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val diameter = this.size.minDimension - strokePx
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)
            val arcSize = Size(diameter, diameter)

            // Background Track
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Progress Arc (sweeps counter-clockwise as time ticks down, or clockwise based on progress)
            val sweep = 360f * animatedProgress.coerceIn(0f, 1f)
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Text(
            text = remainingSeconds.toString(),
            color = if (isNearExpiry) ExpiryAlert else MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
