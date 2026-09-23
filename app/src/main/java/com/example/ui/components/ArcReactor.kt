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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JarvisBorderGlow
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisCyanDark
import com.example.ui.theme.JarvisCyanDeep
import com.example.ui.theme.JarvisCyanGlow
import com.example.ui.theme.JarvisOrange
import com.example.ui.theme.JarvisSpaceBlack
import kotlin.math.cos
import kotlin.math.sin

/**
 * Sci-Fi HUD Central Circular Instrument Dial:
 * - Concentric glowing cyan rings rotating in alternating directions.
 * - Precision tick marks and cardinal indices around the circumference.
 * - Luminous pulsing core that reacts to live speech audio amplitude.
 * - Idle state: slow, subtle ambient rotation & breathing glow.
 * - Active state (Listening / Responding): faster rotation, energetic radial aura, and high-luminance pulse.
 */
@Composable
fun ArcReactor(
    modifier: Modifier = Modifier,
    size: Dp = 156.dp,
    isListening: Boolean = false,
    isSpeaking: Boolean = false,
    isProcessing: Boolean = false,
    rmsLevel: Float = 0f,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "instrument_dial_anim")

    // Clockwise ring rotation: subtle during idle, accelerated when listening or speaking
    val rotationFast by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isListening) 3200 else if (isSpeaking) 4200 else 14000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "dial_rot_cw"
    )

    // Counter-clockwise orbital rotation
    val rotationSlow by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isListening) 4800 else if (isSpeaking) 6000 else 18000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "dial_rot_ccw"
    )

    // Breathing glow pulse
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isListening || isSpeaking) 650 else 2200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dial_pulse"
    )

    // Radar hologram beam sweep when active
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isListening) 2000 else 3800,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "dial_sweep"
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
        // Multi-layer concentric expanding energy waves when active
        ConcentricPulseWaves(
            baseSize = size * 0.72f,
            pulseColor = if (isListening) JarvisCyanGlow else if (isProcessing) JarvisOrange else JarvisCyan,
            isActive = isListening || isSpeaking || isProcessing
        )

        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerRadius = this.size.minDimension / 2.15f
            val audioScale = if (isListening) (rmsLevel * 14.dp.toPx()) else if (isSpeaking) ((pulse - 1f) * 10.dp.toPx()) else 0f
            val activeRadius = (outerRadius * (if (isListening || isSpeaking) pulse else 1f)) + audioScale

            val cyanColor = if (isListening) JarvisCyanGlow else JarvisCyan
            val highlightColor = if (isListening) Color(0xFFE0F2FE) else JarvisCyanBright

            // 1. Outer Radial Energy Halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        cyanColor.copy(alpha = if (isListening || isSpeaking) 0.35f else 0.12f),
                        JarvisCyanDark.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = activeRadius * 1.35f
                )
            )

            // 2. Circumference Instrument Calibration Dial (72 Tick Marks: 5° and 15° intervals)
            val totalTicks = 72
            for (i in 0 until totalTicks) {
                val tickAngleDeg = i * (360f / totalTicks)
                val isMajorTick = (i % 3 == 0) // Every 15 degrees
                val isCardinal = (i % 18 == 0) // Cardinal 0, 90, 180, 270 degrees

                val angleRad = Math.toRadians(tickAngleDeg.toDouble())
                val cosA = cos(angleRad).toFloat()
                val sinA = sin(angleRad).toFloat()

                val tickLen = when {
                    isCardinal -> 9.dp.toPx()
                    isMajorTick -> 6.dp.toPx()
                    else -> 3.5.dp.toPx()
                }

                val tickStroke = when {
                    isCardinal -> 2.2.dp.toPx()
                    isMajorTick -> 1.4.dp.toPx()
                    else -> 0.9.dp.toPx()
                }

                val tickColor = when {
                    isCardinal -> highlightColor
                    isMajorTick -> cyanColor.copy(alpha = 0.85f)
                    else -> cyanColor.copy(alpha = 0.40f)
                }

                val startX = center.x + (outerRadius - tickLen) * cosA
                val startY = center.y + (outerRadius - tickLen) * sinA
                val endX = center.x + outerRadius * cosA
                val endY = center.y + outerRadius * sinA

                drawLine(
                    color = tickColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = tickStroke,
                    cap = StrokeCap.Round
                )
            }

            // 3. Outer Continuous Glowing Cyan Calibration Ring
            drawCircle(
                color = cyanColor.copy(alpha = if (isListening) 0.85f else 0.50f),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 1.4.dp.toPx())
            )

            // 4. Rotating Segmented Reticle Ring (Clockwise)
            rotate(rotationFast, pivot = center) {
                val segRadius = outerRadius * 0.84f
                // Dashed tech arc
                drawCircle(
                    color = cyanColor.copy(alpha = if (isListening) 0.9f else 0.6f),
                    radius = segRadius,
                    center = center,
                    style = Stroke(
                        width = 1.6.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 16f, 8f, 16f), 0f),
                        cap = StrokeCap.Round
                    )
                )

                // 4 Cyber Node Ticks at 90° intervals
                for (j in 0 until 4) {
                    val a = Math.toRadians((j * 90.0))
                    val nx = center.x + segRadius * cos(a).toFloat()
                    val ny = center.y + segRadius * sin(a).toFloat()
                    drawCircle(
                        color = Color.White,
                        radius = 2.2.dp.toPx(),
                        center = Offset(nx, ny)
                    )
                }
            }

            // 5. Counter-Rotating Intermediate Ring (Counter-Clockwise)
            rotate(rotationSlow, pivot = center) {
                val midRadius = outerRadius * 0.66f
                drawCircle(
                    color = JarvisCyanBright.copy(alpha = if (isListening || isSpeaking) 0.75f else 0.45f),
                    radius = midRadius,
                    center = center,
                    style = Stroke(
                        width = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 14f), 0f)
                    )
                )

                // 6 Concentric coil blocks / radial spokes
                val spokeCount = 6
                for (k in 0 until spokeCount) {
                    val sa = Math.toRadians((k * (360.0 / spokeCount)))
                    val sx1 = center.x + (midRadius - 5.dp.toPx()) * cos(sa).toFloat()
                    val sy1 = center.y + (midRadius - 5.dp.toPx()) * sin(sa).toFloat()
                    val sx2 = center.x + (midRadius + 5.dp.toPx()) * cos(sa).toFloat()
                    val sy2 = center.y + (midRadius + 5.dp.toPx()) * sin(sa).toFloat()
                    drawLine(
                        color = cyanColor,
                        start = Offset(sx1, sy1),
                        end = Offset(sx2, sy2),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // 6. Sweeping Holographic Radar Cone (Active/Processing)
            if (isListening || isSpeaking || isProcessing) {
                rotate(sweepAngle, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color.Transparent,
                                cyanColor.copy(alpha = 0.05f),
                                cyanColor.copy(alpha = 0.28f),
                                Color.Transparent
                            ),
                            center = center
                        ),
                        startAngle = 0f,
                        sweepAngle = 45f,
                        useCenter = true,
                        topLeft = Offset(center.x - outerRadius * 0.82f, center.y - outerRadius * 0.82f),
                        size = androidx.compose.ui.geometry.Size(outerRadius * 1.64f, outerRadius * 1.64f)
                    )
                }
            }

            // 7. Inner Core Reticle Bracket Ring
            val innerRingRadius = outerRadius * 0.44f
            drawCircle(
                color = cyanColor.copy(alpha = 0.7f),
                radius = innerRingRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Subtle Crosshairs within core ring
            drawLine(
                color = cyanColor.copy(alpha = 0.25f),
                start = Offset(center.x - innerRingRadius, center.y),
                end = Offset(center.x + innerRingRadius, center.y),
                strokeWidth = 0.8.dp.toPx()
            )
            drawLine(
                color = cyanColor.copy(alpha = 0.25f),
                start = Offset(center.x, center.y - innerRingRadius),
                end = Offset(center.x, center.y + innerRingRadius),
                strokeWidth = 0.8.dp.toPx()
            )

            // 8. Bright Glowing Luminous Core (Voice Reactor)
            val coreBaseRadius = outerRadius * 0.26f
            val coreDynamicRadius = (coreBaseRadius * (if (isListening || isSpeaking) pulse else 1f)) + (audioScale * 0.5f)

            // Core radial gradient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = if (isListening) listOf(
                        Color.White,
                        JarvisCyanGlow,
                        JarvisCyan,
                        JarvisSpaceBlack
                    ) else if (isSpeaking) listOf(
                        Color.White,
                        JarvisCyanBright,
                        JarvisCyanDark,
                        JarvisSpaceBlack
                    ) else listOf(
                        highlightColor,
                        cyanColor,
                        JarvisCyanDeep,
                        JarvisSpaceBlack
                    ),
                    center = center,
                    radius = coreDynamicRadius * 1.3f
                ),
                radius = coreDynamicRadius,
                center = center
            )

            // Ultra-bright specular white spark in the center
            drawCircle(
                color = Color.White,
                radius = (3.dp.toPx() * (if (isListening || isSpeaking) pulse else 1f)),
                center = center
            )
        }
    }
}
