package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisNeonTeal
import com.example.ui.theme.JarvisOrange
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactor(
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    isListening: Boolean = false,
    isSpeaking: Boolean = false,
    isProcessing: Boolean = false,
    rmsLevel: Float = 0f,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "reactor_anim")

    // Slow continuous rotation
    val rotationFast by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 3500 else if (isSpeaking) 5000 else 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_fast"
    )

    val rotationSlow by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 5000 else 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_slow"
    )

    // Sweeping Radar Hologram beam
    val radarSweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 1800 else 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_sweep_angle"
    )

    // Breathing glow pulse
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening || isSpeaking) 700 else 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "reactor_pulse"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
            .testTag("arc_reactor_visualizer")
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = size / 2, color = JarvisCyan),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Concentric pulse waves in listening/speaking mode
        ConcentricPulseWaves(
            baseSize = size * 0.7f,
            pulseColor = if (isListening) JarvisCyanBright else if (isProcessing) JarvisNeonTeal else JarvisCyan,
            isActive = isListening || isSpeaking || isProcessing
        )

        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val baseRadius = this.size.minDimension / 2.3f

            // Dynamic expansion based on speech audio or pulse
            val audioBoost = if (isListening) rmsLevel * 14.dp.toPx() else if (isSpeaking) (pulse - 1f) * 12.dp.toPx() else 0f
            val effectiveRadius = (baseRadius * (if (isListening || isSpeaking) pulse else 1f)) + audioBoost

            val primaryCyan = if (isListening) JarvisCyanBright else JarvisCyan
            val secondaryColor = if (isProcessing) JarvisNeonTeal else Color(0xFF0284C7)

            // Outer Radial Energy Aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryCyan.copy(alpha = if (isListening || isSpeaking) 0.38f else 0.16f),
                        secondaryColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = effectiveRadius * 1.35f
                ),
                radius = effectiveRadius * 1.35f,
                center = center
            )

            // Outer ring with slow reverse rotation and dash pattern
            rotate(rotationSlow, pivot = center) {
                drawCircle(
                    color = primaryCyan.copy(alpha = 0.35f),
                    radius = effectiveRadius * 0.96f,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f), 0f)
                    )
                )

                // 4 Orbiting Photon Nodes
                val nodeCount = 4
                val orbitR = effectiveRadius * 0.96f
                for (i in 0 until nodeCount) {
                    val angle = (i * 90.0) * (Math.PI / 180.0)
                    val nx = (center.x + orbitR * cos(angle)).toFloat()
                    val ny = (center.y + orbitR * sin(angle)).toFloat()
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = Offset(nx, ny)
                    )
                    drawCircle(
                        color = primaryCyan,
                        radius = 5.dp.toPx(),
                        center = Offset(nx, ny),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }

            // Sweeping Holographic Radar Cone
            rotate(radarSweepAngle, pivot = center) {
                val coneRadius = effectiveRadius * 0.88f
                val radarPath = Path().apply {
                    moveTo(center.x, center.y)
                    lineTo(center.x + coneRadius, center.y)
                    arcTo(
                        rect = Rect(center.x - coneRadius, center.y - coneRadius, center.x + coneRadius, center.y + coneRadius),
                        startAngleDegrees = 0f,
                        sweepAngleDegrees = -45f,
                        forceMoveTo = false
                    )
                    close()
                }
                drawPath(
                    path = radarPath,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryCyan.copy(alpha = if (isListening) 0.35f else 0.18f),
                            primaryCyan.copy(alpha = 0.04f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = coneRadius
                    )
                )
                // Leading scanning sweep line
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = center,
                    end = Offset(center.x + coneRadius, center.y),
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            // Middle segment ring with fast forward rotation
            rotate(rotationFast, pivot = center) {
                drawCircle(
                    color = primaryCyan.copy(alpha = 0.8f),
                    radius = effectiveRadius * 0.78f,
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 20f), 0f)
                    )
                )

                // 8 Arc Coils (Iron Man Reactor Coil Segments)
                val coilCount = 8
                val innerR = effectiveRadius * 0.52f
                val outerR = effectiveRadius * 0.76f
                for (i in 0 until coilCount) {
                    val angle = (i * (360f / coilCount)) * (Math.PI / 180.0)
                    val startX = (center.x + innerR * cos(angle)).toFloat()
                    val startY = (center.y + innerR * sin(angle)).toFloat()
                    val endX = (center.x + outerR * cos(angle)).toFloat()
                    val endY = (center.y + outerR * sin(angle)).toFloat()

                    drawLine(
                        color = primaryCyan,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // Inner solid glowing ring
            drawCircle(
                color = primaryCyan,
                radius = effectiveRadius * 0.50f,
                style = Stroke(width = 2.5.dp.toPx())
            )

            // Inner translucent reactor chamber
            drawCircle(
                color = Color(0x3300F0FF),
                radius = effectiveRadius * 0.48f
            )

            // Central Glowing Core (Pure Cyan & White)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        primaryCyan,
                        primaryCyan.copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = effectiveRadius * 0.38f
                ),
                radius = effectiveRadius * 0.36f,
                center = center
            )

            // Triangular / hexagonal cybernetic crosshairs in center
            val crosshairLen = effectiveRadius * 0.22f
            drawLine(
                color = Color.White.copy(alpha = 0.9f),
                start = Offset(center.x - crosshairLen, center.y),
                end = Offset(center.x + crosshairLen, center.y),
                strokeWidth = 1.5.dp.toPx()
            )
            drawLine(
                color = Color.White.copy(alpha = 0.9f),
                start = Offset(center.x, center.y - crosshairLen),
                end = Offset(center.x, center.y + crosshairLen),
                strokeWidth = 1.5.dp.toPx()
            )
        }
    }
}
