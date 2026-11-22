package com.example.ui.island

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.RingerMode
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentViolet

@Composable
fun IslandMediaVisualizer(
    isPlaying: Boolean,
    barColor: Color = AccentViolet,
    barCount: Int = 5,
    maxBarHeight: Dp = 14.dp,
    modifier: Modifier = Modifier,
) {
    if (!isPlaying) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(barCount) { idx ->
                Box(
                    modifier = Modifier
                        .width(2.5.dp)
                        .height(if (idx % 2 == 0) 4.dp else 6.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(barColor.copy(alpha = 0.55f)),
                )
            }
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "eq_wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "eq_phase",
    )

    Row(
        modifier = modifier.height(maxBarHeight),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0 until barCount) {
            val scale = 0.35f + 0.65f * kotlin.math.abs(kotlin.math.sin(phase + i * 0.9f))
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(maxBarHeight)
                    .graphicsLayer {
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor),
            )
        }
    }
}

@Composable
fun RadialProgressRing(
    progress: Float,
    ringColor: Color = AccentCyan,
    trackColor: Color = Color.White.copy(alpha = 0.16f),
    size: Dp = 20.dp,
    strokeWidth: Dp = 2.5.dp,
    modifier: Modifier = Modifier,
    centerContent: @Composable () -> Unit = {},
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 320f),
        label = "ring_progress",
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokePx = strokeWidth.toPx()
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
        }
        centerContent()
    }
}

@Composable
fun HotspotTetheringIcon(
    tint: Color = AccentEmerald,
    size: Dp = 20.dp,
    pulse: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val scale = if (pulse) {
        val transition = rememberInfiniteTransition(label = "hotspot_pulse")
        val s by transition.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "hotspot_scale",
        )
        s
    } else {
        1f
    }

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        val stroke = this.size.minDimension * 0.13f
        val r = this.size.minDimension * 0.25f
        val cy = this.size.height * 0.5f
        val leftCx = this.size.width * 0.36f
        val rightCx = this.size.width * 0.64f

        drawCircle(
            color = tint,
            radius = r,
            center = Offset(leftCx, cy),
            style = Stroke(width = stroke),
        )
        drawCircle(
            color = tint,
            radius = r,
            center = Offset(rightCx, cy),
            style = Stroke(width = stroke),
        )
    }
}

@Composable
fun RingerBellBadge(
    mode: RingerMode,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "bell_shake")
    val angle by transition.animateFloat(
        initialValue = -14f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(140),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bell_angle",
    )

    val (icon, tint) = when (mode) {
        RingerMode.SILENT -> Icons.Filled.NotificationsOff to AccentCoral
        RingerMode.VIBRATE -> Icons.Filled.Vibration to AccentAmber
        RingerMode.NORMAL -> Icons.Filled.Notifications to AccentEmerald
    }

    Icon(
        imageVector = icon,
        contentDescription = mode.name,
        tint = tint,
        modifier = modifier
            .size(20.dp)
            .graphicsLayer {
                rotationZ = if (mode != RingerMode.NORMAL) angle else 0f
            },
    )
}

fun formatSecondsMmSs(totalSeconds: Int): String {
    val clamped = totalSeconds.coerceAtLeast(0)
    val mins = clamped / 60
    val secs = clamped % 60
    return String.format("%d:%02d", mins, secs)
}

fun formatMillisStopwatch(millis: Long): String {
    val safe = millis.coerceAtLeast(0L)
    val totalSec = safe / 1000L
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    val tenths = (safe % 1000L) / 100L
    return String.format("%02d:%02d.%d", mins, secs, tenths)
}
