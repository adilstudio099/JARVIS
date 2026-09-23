package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisNeonTeal
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Full-screen or container overlay that casts an authentic Iron Man holographic
 * helmet visor scanline and subtle traveling laser beam.
 */
@Composable
fun HudScanOverlay(
    modifier: Modifier = Modifier,
    laserColor: Color = JarvisCyan,
    scanDurationMillis: Int = 4000,
    enableScanlines: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hud_scan_anim")

    // Laser beam vertical travel from 0% to 100%
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = scanDurationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "laser_scan_progress"
    )

    // Breathing shimmer for ambient visor glow
    val ambientGlow by infiniteTransition.animateFloat(
        initialValue = 0.03f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_glow_pulse"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 1. Subtle horizontal holographic raster scanlines
        if (enableScanlines) {
            val lineSpacing = 6.dp.toPx()
            val totalLines = (height / lineSpacing).toInt()
            for (i in 0..totalLines) {
                val y = i * lineSpacing
                drawLine(
                    color = laserColor.copy(alpha = 0.025f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        // 2. Sweeping glowing laser beam
        val beamY = scanProgress * height
        val beamHeight = 45.dp.toPx()

        val beamBrush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                laserColor.copy(alpha = 0.04f),
                laserColor.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.40f),
                laserColor.copy(alpha = 0.22f),
                laserColor.copy(alpha = 0.04f),
                Color.Transparent
            ),
            startY = beamY - (beamHeight / 2),
            endY = beamY + (beamHeight / 2)
        )

        drawRect(
            brush = beamBrush,
            topLeft = Offset(0f, beamY - (beamHeight / 2)),
            size = Size(width, beamHeight)
        )

        // Crisp central laser core line
        drawLine(
            color = Color.White.copy(alpha = 0.65f),
            start = Offset(0f, beamY),
            end = Offset(width, beamY),
            strokeWidth = 1.2.dp.toPx()
        )

        // Edge beacon ticks along the beam
        drawCircle(
            color = JarvisCyanBright,
            radius = 2.5.dp.toPx(),
            center = Offset(8.dp.toPx(), beamY)
        )
        drawCircle(
            color = JarvisCyanBright,
            radius = 2.5.dp.toPx(),
            center = Offset(width - 8.dp.toPx(), beamY)
        )
    }
}

/**
 * Subtle, continuous scan-line and holographic beam animation overlay
 * designed specifically for the main Compose screen to enhance the 'Iron Man HUD' aesthetic.
 *
 * Features:
 * - Continuous rolling micro-scanlines across the screen.
 * - Smooth vertical sweeping laser beam with neon gradient halo.
 * - Stark helmet HUD corner targeting brackets.
 * - Completely non-intrusive: touch events pass through to child Composables unimpeded.
 */
@Composable
fun ContinuousHudScanlinesOverlay(
    modifier: Modifier = Modifier,
    scanColor: Color = JarvisCyan,
    beamColor: Color = JarvisCyanBright,
    scanDurationMillis: Int = 4800,
    lineSpacing: Dp = 4.dp,
    showCornerBrackets: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "continuous_hud_scan_anim")

    // Vertical sweep progress from 0f to 1f
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = scanDurationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "continuous_scan_progress"
    )

    // Continuous subtle roll for scanlines (rolling phosphor effect)
    val rollPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanline_roll_phase"
    )

    // Breathing glow intensity
    val ambientPulse by infiniteTransition.animateFloat(
        initialValue = 0.025f,
        targetValue = 0.055f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hud_ambient_pulse"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val spacingPx = lineSpacing.toPx()
        val phaseOffset = rollPhase * spacingPx
        val totalLines = (height / spacingPx).toInt() + 1

        // 1. Continuous subtle horizontal scanlines
        for (i in 0..totalLines) {
            val y = (i * spacingPx + phaseOffset) % height
            drawLine(
                color = scanColor.copy(alpha = 0.035f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // 2. Sweeping glowing laser beam
        val beamY = scanProgress * height
        val beamHeight = 50.dp.toPx()

        val beamBrush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                scanColor.copy(alpha = 0.03f),
                scanColor.copy(alpha = 0.16f),
                Color.White.copy(alpha = 0.35f),
                scanColor.copy(alpha = 0.16f),
                scanColor.copy(alpha = 0.03f),
                Color.Transparent
            ),
            startY = beamY - (beamHeight / 2),
            endY = beamY + (beamHeight / 2)
        )

        drawRect(
            brush = beamBrush,
            topLeft = Offset(0f, beamY - (beamHeight / 2)),
            size = Size(width, beamHeight)
        )

        // Core bright laser line
        drawLine(
            color = Color.White.copy(alpha = 0.55f),
            start = Offset(0f, beamY),
            end = Offset(width, beamY),
            strokeWidth = 1.dp.toPx()
        )

        // Subtle side alignment ticks along the sweeping beam
        drawCircle(
            color = beamColor.copy(alpha = 0.8f),
            radius = 2.dp.toPx(),
            center = Offset(10.dp.toPx(), beamY)
        )
        drawCircle(
            color = beamColor.copy(alpha = 0.8f),
            radius = 2.dp.toPx(),
            center = Offset(width - 10.dp.toPx(), beamY)
        )

        // 3. Corner targeting brackets for authentic Iron Man HUD feel
        if (showCornerBrackets) {
            val bracketLen = 16.dp.toPx()
            val bracketStroke = 1.2.dp.toPx()
            val bracketColor = scanColor.copy(alpha = ambientPulse * 12f)
            val padding = 8.dp.toPx()

            // Top-Left ┌
            drawLine(bracketColor, Offset(padding, padding), Offset(padding + bracketLen, padding), bracketStroke)
            drawLine(bracketColor, Offset(padding, padding), Offset(padding, padding + bracketLen), bracketStroke)

            // Top-Right ┐
            drawLine(bracketColor, Offset(width - padding, padding), Offset(width - padding - bracketLen, padding), bracketStroke)
            drawLine(bracketColor, Offset(width - padding, padding), Offset(width - padding, padding + bracketLen), bracketStroke)

            // Bottom-Left └
            drawLine(bracketColor, Offset(padding, height - padding), Offset(padding + bracketLen, height - padding), bracketStroke)
            drawLine(bracketColor, Offset(padding, height - padding), Offset(padding, height - padding - bracketLen), bracketStroke)

            // Bottom-Right ┘
            drawLine(bracketColor, Offset(width - padding, height - padding), Offset(width - padding - bracketLen, height - padding), bracketStroke)
            drawLine(bracketColor, Offset(width - padding, height - padding), Offset(width - padding, padding + bracketLen), bracketStroke)
        }
    }
}

/**
 * Modifier to draw Stark-style cybernetic targeting brackets ([  ]) on the four corners of a component.
 */
fun Modifier.cyberCornerReticles(
    bracketColor: Color = JarvisCyan,
    bracketLength: Dp = 10.dp,
    strokeWidth: Dp = 1.5.dp,
    glowAlpha: Float = 0.85f
): Modifier = this.drawBehind {
    val len = bracketLength.toPx()
    val sw = strokeWidth.toPx()
    val color = bracketColor.copy(alpha = glowAlpha)

    // Top-Left corner ┌
    drawLine(color = color, start = Offset(0f, 0f), end = Offset(len, 0f), strokeWidth = sw)
    drawLine(color = color, start = Offset(0f, 0f), end = Offset(0f, len), strokeWidth = sw)

    // Top-Right corner ┐
    drawLine(color = color, start = Offset(size.width, 0f), end = Offset(size.width - len, 0f), strokeWidth = sw)
    drawLine(color = color, start = Offset(size.width, 0f), end = Offset(size.width, len), strokeWidth = sw)

    // Bottom-Left corner └
    drawLine(color = color, start = Offset(0f, size.height), end = Offset(len, size.height), strokeWidth = sw)
    drawLine(color = color, start = Offset(0f, size.height), end = Offset(0f, size.height - len), strokeWidth = sw)

    // Bottom-Right corner ┘
    drawLine(color = color, start = Offset(size.width, size.height), end = Offset(size.width - len, size.height), strokeWidth = sw)
    drawLine(color = color, start = Offset(size.width, size.height), end = Offset(size.width, size.height - len), strokeWidth = sw)
}

/**
 * Concentric energy pulse rings that radiate outward from a central button or point.
 */
@Composable
fun ConcentricPulseWaves(
    modifier: Modifier = Modifier,
    baseSize: Dp = 56.dp,
    pulseColor: Color = JarvisCyan,
    waveCount: Int = 3,
    isActive: Boolean = true
) {
    if (!isActive) return

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_waves_transition")

    // Staggered waves
    val wave1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_1"
    )

    val wave2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_2"
    )

    val wave3 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_3"
    )

    Box(
        modifier = modifier.size(baseSize * 2.3f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = (baseSize.toPx() / 2)

            listOf(wave1, wave2, wave3).forEach { progress ->
                val radius = baseRadius * progress
                val alpha = ((2.1f - progress) / 1.1f).coerceIn(0f, 0.75f)
                if (alpha > 0.02f) {
                    drawCircle(
                        color = pulseColor.copy(alpha = alpha),
                        radius = radius,
                        center = center,
                        style = Stroke(
                            width = (2.dp * (2.1f - progress + 0.2f)).toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                }
            }
        }
    }
}

/**
 * Text component that scrambles and simulates cybernetic decryption
 * whenever the incoming text string changes.
 */
@Composable
fun CyberDecryptedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = JarvisCyanBright,
    scrambleDurationMs: Long = 400
) {
    var displayedText by remember { mutableStateOf(text) }
    val cyberGlitchChars = remember { "!<>-_\\/[]{}—=+*^?#________0123456789ABCDEF" }

    LaunchedEffect(text) {
        val target = text
        val len = target.length
        if (len == 0) {
            displayedText = ""
            return@LaunchedEffect
        }

        val stepTime = (scrambleDurationMs / (len.coerceAtLeast(1) + 4)).coerceIn(15, 60)
        var revealedChars = 0

        while (revealedChars <= len) {
            val sb = java.lang.StringBuilder()
            for (i in 0 until len) {
                if (i < revealedChars) {
                    sb.append(target[i])
                } else if (target[i] == ' ') {
                    sb.append(' ')
                } else {
                    sb.append(cyberGlitchChars[Random.nextInt(cyberGlitchChars.length)])
                }
            }
            displayedText = sb.toString()
            revealedChars += 2
            delay(stepTime)
        }
        displayedText = target
    }

    Text(
        text = displayedText,
        modifier = modifier,
        style = style,
        color = color
    )
}

/**
 * Compact radar sweep animation with angular scanning wedge and reticle crosshairs.
 */
@Composable
fun HolographicScannerRadar(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tint: Color = JarvisCyan
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_anim")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_sweep_angle"
    )

    Canvas(modifier = modifier.size(size)) {
        val center = Offset(this.size.width / 2, this.size.height / 2)
        val radius = this.size.minDimension / 2.2f

        // Outer reticle circle
        drawCircle(
            color = tint.copy(alpha = 0.4f),
            radius = radius,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        // Inner reference circle
        drawCircle(
            color = tint.copy(alpha = 0.2f),
            radius = radius * 0.55f,
            center = center,
            style = Stroke(width = 0.8.dp.toPx())
        )

        // Axis crosshairs
        drawLine(
            color = tint.copy(alpha = 0.35f),
            start = Offset(center.x - radius, center.y),
            end = Offset(center.x + radius, center.y),
            strokeWidth = 0.75.dp.toPx()
        )
        drawLine(
            color = tint.copy(alpha = 0.35f),
            start = Offset(center.x, center.y - radius),
            end = Offset(center.x, center.y + radius),
            strokeWidth = 0.75.dp.toPx()
        )

        // Sweeping radar beam with trailing gradient arc
        rotate(sweepAngle, pivot = center) {
            val path = Path().apply {
                moveTo(center.x, center.y)
                lineTo(center.x + radius, center.y)
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = -45f,
                    forceMoveTo = false
                )
                close()
            }

            drawPath(
                path = path,
                brush = Brush.radialGradient(
                    colors = listOf(tint.copy(alpha = 0.45f), tint.copy(alpha = 0.05f), Color.Transparent),
                    center = center,
                    radius = radius
                )
            )

            // Sharp leading line
            drawLine(
                color = Color.White.copy(alpha = 0.9f),
                start = center,
                end = Offset(center.x + radius, center.y),
                strokeWidth = 1.5.dp.toPx()
            )
        }
    }
}
