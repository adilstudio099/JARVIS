package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JarvisBorderCyan
import com.example.ui.theme.JarvisBorderGlow
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisNeonTeal
import com.example.ui.theme.JarvisOrange
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisSpaceBlack
import com.example.ui.theme.JarvisSurfaceCard
import com.example.ui.theme.JarvisSurfaceDark
import com.example.ui.theme.JarvisSurfaceElevated
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@Composable
fun HudCard(
    modifier: Modifier = Modifier,
    borderColor: Color = JarvisBorderCyan,
    showCornerReticles: Boolean = true,
    showScanShimmer: Boolean = true,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hud_card_anim")

    // Slow traveling cyber shimmer across the card
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    // Corner reticle breathing glow
    val cornerPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corner_pulse"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(borderColor, borderColor.copy(alpha = 0.25f), borderColor)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .background(JarvisSurfaceCard.copy(alpha = 0.88f))
    ) {
        // Holographic glass diagonal scanbeam across the card
        if (showScanShimmer) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val startX = shimmerOffset * (w + h)
                val beamBrush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        borderColor.copy(alpha = 0.02f),
                        borderColor.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.15f),
                        borderColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    start = Offset(startX, 0f),
                    end = Offset(startX + 120.dp.toPx(), h)
                )
                drawRect(brush = beamBrush)
            }
        }

        // Cybernetic Corner Brackets
        if (showCornerReticles) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val len = 10.dp.toPx()
                val sw = 1.5.dp.toPx()
                val c = JarvisCyanBright.copy(alpha = cornerPulse * 0.9f)

                // Top-Left ┌
                drawLine(c, Offset(0f, 0f), Offset(len, 0f), sw)
                drawLine(c, Offset(0f, 0f), Offset(0f, len), sw)

                // Top-Right ┐
                drawLine(c, Offset(size.width, 0f), Offset(size.width - len, 0f), sw)
                drawLine(c, Offset(size.width, 0f), Offset(size.width, len), sw)

                // Bottom-Left └
                drawLine(c, Offset(0f, size.height), Offset(len, size.height), sw)
                drawLine(c, Offset(0f, size.height), Offset(0f, size.height - len), sw)

                // Bottom-Right ┘
                drawLine(c, Offset(size.width, size.height), Offset(size.width - len, size.height), sw)
                drawLine(c, Offset(size.width, size.height), Offset(size.width, size.height - len), sw)
            }
        }

        content()
    }
}

/**
 * Cybernetic glowing Voice Command button with multi-layer expanding pulse waves,
 * rotating outer dashed reticle rings, and reactive audio scale.
 */
@Composable
fun CyberGlowingMicButton(
    isListening: Boolean,
    isSpeaking: Boolean,
    rmsLevel: Float = 0f,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse_anim")

    // Slow rotation of cyber orbital ring
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 3000 else 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mic_ring_rotation"
    )

    // Breathing pulse scale
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 700 else 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_breathing_pulse"
    )

    // Glowing halo alpha
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 600 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_halo_alpha"
    )

    val activeColor = if (isListening) JarvisRed else JarvisCyan
    val dynamicAudioScale = if (isListening) (1f + rmsLevel * 0.15f) else breathingPulse

    Box(
        modifier = modifier
            .size(76.dp)
            .testTag("voice_input_button"),
        contentAlignment = Alignment.Center
    ) {
        // Concentric Expanding Energy Waves when active or in standby
        ConcentricPulseWaves(
            baseSize = 48.dp,
            pulseColor = activeColor,
            isActive = isListening || isSpeaking
        )

        // Outer Rotating Reticle Ring
        Canvas(modifier = Modifier.size(68.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2.15f

            rotate(ringRotation, pivot = center) {
                // Dashed tech arc
                drawCircle(
                    color = activeColor.copy(alpha = haloAlpha),
                    radius = radius,
                    center = center,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f),
                        cap = StrokeCap.Round
                    )
                )

                // 4 Cyber tick nodes
                val nodeCount = 4
                for (i in 0 until nodeCount) {
                    val angle = (i * 90.0) * (Math.PI / 180.0)
                    val nx = (center.x + radius * Math.cos(angle)).toFloat()
                    val ny = (center.y + radius * Math.sin(angle)).toFloat()
                    drawCircle(
                        color = if (isListening) Color.White else JarvisCyanBright,
                        radius = 2.dp.toPx(),
                        center = Offset(nx, ny)
                    )
                }
            }
        }

        // Inner Glowing Core Button
        Box(
            modifier = Modifier
                .size(50.dp)
                .scale(dynamicAudioScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = if (isListening) listOf(Color(0xFFFF4D4D), JarvisRed, Color(0xFF990000))
                        else listOf(JarvisCyanBright, JarvisCyan, JarvisSurfaceElevated)
                    )
                )
                .border(
                    BorderStroke(1.5.dp, if (isListening) Color.White else JarvisCyanBright),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 32.dp, color = activeColor),
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = if (isListening) "Stop Listening" else "Start Voice Command",
                tint = if (isListening) Color.White else JarvisSpaceBlack,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Cybernetic glowing Floating Action Button with pulsing halo and tech reticles.
 */
@Composable
fun CyberPulseFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Alarm,
    contentDescription: String = "Action"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fab_pulse_anim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fab_scale"
    )

    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fab_halo_alpha"
    )

    Box(
        modifier = modifier.size(70.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer halo aura
        Canvas(modifier = Modifier.size(66.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        JarvisCyan.copy(alpha = haloAlpha),
                        JarvisCyan.copy(alpha = 0.08f),
                        Color.Transparent
                    )
                ),
                radius = size.minDimension / 2f
            )
            // Tech bracket ring
            drawCircle(
                color = JarvisCyan.copy(alpha = haloAlpha * 0.8f),
                radius = size.minDimension / 2.2f,
                style = Stroke(
                    width = 1.2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
            )
        }

        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier
                .scale(pulseScale)
                .size(52.dp)
                .testTag("add_task_fab"),
            containerColor = JarvisCyan,
            contentColor = JarvisSpaceBlack,
            shape = CircleShape
        ) {
            Icon(imageVector = icon, contentDescription = contentDescription)
        }
    }
}

@Composable
fun ToolBadge(
    toolName: String,
    modifier: Modifier = Modifier
) {
    val (icon: ImageVector, label: String, color: Color) = when (toolName.uppercase()) {
        "REMINDER", "SET_REMINDER" -> Triple(Icons.Default.Alarm, "REMINDER SET", JarvisOrange)
        "TODO", "MANAGE_TODO" -> Triple(Icons.Default.CheckCircle, "TO-DO DISPATCH", JarvisGreen)
        "NOTE", "CREATE_NOTE" -> Triple(Icons.Default.EditNote, "NOTE RECORDED", JarvisCyan)
        "CALCULATOR", "CALCULATE" -> Triple(Icons.Default.Calculate, "CALCULATION", JarvisGold)
        "WEATHER", "GET_WEATHER" -> Triple(Icons.Default.Cloud, "ATMOSPHERIC INTEL", JarvisCyanBright)
        "DATE_TIME", "GET_DATE_TIME" -> Triple(Icons.Default.Schedule, "TEMPORAL TELEMETRY", JarvisNeonTeal)
        else -> Triple(Icons.Default.Info, "TOOL: $toolName", JarvisCyan)
    }

    Row(
        modifier = modifier
            .testTag("tool_badge_${toolName.lowercase()}")
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.75.dp, color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
fun HudWaveform(
    isEmitting: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 6,
    tint: Color = JarvisCyan
) {
    val transition = rememberInfiniteTransition(label = "waveform_transition")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val animHeight by transition.animateFloat(
                initialValue = 4f,
                targetValue = if (isEmitting) (14 + ((i * 3) % 4) * 5).toFloat() else 4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 260 + (i * 65), easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_anim_$i"
            )

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(animHeight.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(
                        if (isEmitting) Brush.verticalGradient(
                            listOf(Color.White, tint, tint.copy(alpha = 0.4f))
                        ) else Brush.verticalGradient(
                            listOf(JarvisTextMuted, JarvisTextMuted.copy(alpha = 0.4f))
                        )
                    )
            )
        }
    }
}

@Composable
fun SystemTelemetryHeader(
    statusText: String = "JARVIS PROTOCOL ACTIVE",
    isOnline: Boolean = true,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "telemetry_beacon")
    val beaconPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_pulse"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Animated Radar Ping or Pulsing Beacon
            HolographicScannerRadar(size = 18.dp, tint = if (isOnline) JarvisCyan else JarvisOrange)
            Spacer(modifier = Modifier.width(8.dp))

            CyberDecryptedText(
                text = statusText.uppercase(),
                color = if (isOnline) JarvisTextSecondary else JarvisOrange,
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background((if (isOnline) JarvisCyan else JarvisOrange).copy(alpha = beaconPulse))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "SYS: 100% NOMINAL",
                color = JarvisTextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
