package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ChatMessageEntity
import com.example.ui.JarvisViewModel
import com.example.ui.components.ArcReactor
import com.example.ui.components.CyberDecryptedText
import com.example.ui.components.CyberGlowingMicButton
import com.example.ui.components.HudCard
import com.example.ui.components.HudScanOverlay
import com.example.ui.components.HudWaveform
import com.example.ui.components.SystemTelemetryHeader
import com.example.ui.components.ToolBadge
import com.example.ui.components.cyberCornerReticles
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.scale
import com.example.ui.theme.JarvisBlue
import com.example.ui.theme.JarvisBorderCyan
import com.example.ui.theme.JarvisBorderGlow
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
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
fun ChatScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val rmsLevel by viewModel.rmsLevel.collectAsStateWithLifecycle()
    val liveTranscript by viewModel.liveTranscript.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val speechError by viewModel.speechError.collectAsStateWithLifecycle()
    val userError by viewModel.userError.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    // Permission launcher for microphone
    var hasRecordPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasRecordPermission = isGranted
        if (isGranted) {
            viewModel.toggleVoiceListening()
        }
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, liveTranscript) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Show error in snackbar
    LaunchedEffect(speechError, userError) {
        val error = speechError ?: userError
        if (error != null) {
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    val quickCommands = listOf(
        "🌤 Weather in London",
        "⏰ Remind me to hydrate in 15 mins",
        "✅ Add review suit diagnostic to todo",
        "📝 Note: Arc reactor efficiency up 12%",
        "🔢 Calculate 45 * 128",
        "⏱ Current time and date"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisSpaceBlack)
            .imePadding()
    ) {
        // Futuristic Holographic Visor Scan Overlay
        HudScanOverlay(
            modifier = Modifier.fillMaxSize(),
            laserColor = JarvisCyan,
            scanDurationMillis = 4600
        )

        val sendPulseTransition = rememberInfiniteTransition(label = "send_pulse_trans")
        val sendScale by sendPulseTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(750, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "send_button_pulse"
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // HUD Telemetry Top Bar
            SystemTelemetryHeader(
                statusText = statusMessage,
                isOnline = !isProcessing
            )

            // Centered Arc Reactor Hero Visualizer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ArcReactor(
                        size = 140.dp,
                        isListening = isListening,
                        isSpeaking = isSpeaking,
                        isProcessing = isProcessing,
                        rmsLevel = rmsLevel,
                        onClick = {
                            if (hasRecordPermission) {
                                viewModel.toggleVoiceListening()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HudWaveform(
                            isEmitting = isListening || isSpeaking || isProcessing,
                            tint = if (isListening) JarvisCyanBright else if (isSpeaking) JarvisCyan else JarvisBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        CyberDecryptedText(
                            text = when {
                                isListening -> "LISTENING // SPEAK NOW"
                                isProcessing -> "SYNTHESIZING PROTOCOL..."
                                isSpeaking -> "TRANSMITTING VOCAL AUDIO..."
                                else -> "TAP TO ENGAGE VOICE DIRECTIVE"
                            },
                            color = if (isListening) JarvisCyanBright else JarvisTextSecondary,
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }

            // Quick suggestion chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickCommands) { cmd ->
                    Box(
                        modifier = Modifier
                            .testTag("quick_command_${cmd.take(10)}")
                            .clip(RoundedCornerShape(16.dp))
                            .background(JarvisSurfaceCard)
                            .border(0.8.dp, JarvisBorderCyan, RoundedCornerShape(16.dp))
                            .cyberCornerReticles(bracketColor = JarvisCyan, bracketLength = 6.dp, strokeWidth = 1.dp, glowAlpha = 0.6f)
                            .clickable {
                                viewModel.processUserPrompt(cmd.substringAfter(" "))
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cmd,
                            color = JarvisCyanBright,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Chat Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubble(
                        message = msg,
                        onReplayAudio = {
                            viewModel.speechManager.speak(msg.content)
                        }
                    )
                }

                // Live Transcript Bubble (while user is actively speaking)
                if (isListening && liveTranscript.isNotBlank()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = JarvisBlue.copy(alpha = 0.25f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.6f)),
                                modifier = Modifier.widthIn(max = 300.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = JarvisCyan,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "$liveTranscript...",
                                        color = JarvisTextPrimary,
                                        fontSize = 14.sp,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                            }
                        }
                    }
                }

                // Processing Indicator
                if (isProcessing) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            HudCard(
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = JarvisCyan,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Querying Stark Quantum Core...",
                                        color = JarvisCyanBright,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Input HUD Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = JarvisSurfaceDark,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.verticalGradient(listOf(JarvisBorderCyan, Color.Transparent))
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Futuristic Cybernetic Voice Mic Button with concentric energy waves
                    CyberGlowingMicButton(
                        isListening = isListening,
                        isSpeaking = isSpeaking,
                        rmsLevel = rmsLevel,
                        onClick = {
                            if (hasRecordPermission) {
                                viewModel.toggleVoiceListening()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Text Field
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { viewModel.onInputTextChanged(it) },
                        placeholder = {
                            Text(
                                text = "Enter command or speak...",
                                color = JarvisTextMuted,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_text_input"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = JarvisBorderCyan,
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary,
                            cursorColor = JarvisCyan,
                            focusedContainerColor = JarvisSurfaceElevated,
                            unfocusedContainerColor = JarvisSurfaceCard
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { viewModel.submitTextPrompt() })
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Stop Speaking Button (when TTS is active) OR Send Button
                    if (isSpeaking) {
                        IconButton(
                            onClick = { viewModel.stopSpeaking() },
                            modifier = Modifier
                                .testTag("stop_speaking_button")
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(JarvisOrange.copy(alpha = 0.2f))
                                .border(1.dp, JarvisOrange, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop Speech",
                                tint = JarvisOrange
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { viewModel.submitTextPrompt() },
                            modifier = Modifier
                                .testTag("send_button")
                                .size(44.dp)
                                .scale(if (inputText.isNotBlank()) sendScale else 1f)
                                .clip(CircleShape)
                                .background(if (inputText.isNotBlank()) JarvisCyan else JarvisSurfaceElevated)
                                .border(
                                    if (inputText.isNotBlank()) 1.5.dp else 1.dp,
                                    if (inputText.isNotBlank()) Color.White else JarvisBorderCyan,
                                    CircleShape
                                ),
                            enabled = inputText.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Command",
                                tint = if (inputText.isNotBlank()) JarvisSpaceBlack else JarvisTextMuted
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun ChatBubble(
    message: ChatMessageEntity,
    onReplayAudio: () -> Unit
) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(if (isUser) "user_message_${message.id}" else "jarvis_message_${message.id}"),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (isUser) {
            Surface(
                shape = RoundedCornerShape(16.dp).copy(bottomEnd = androidx.compose.foundation.shape.CornerSize(2.dp)),
                color = JarvisSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderCyan),
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Text(
                    text = message.content,
                    color = JarvisTextPrimary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        } else {
            HudCard(
                modifier = Modifier.widthIn(max = 320.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    // Header with Jarvis tag and Tool Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(JarvisCyan)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "J.A.R.V.I.S.",
                                color = JarvisCyanBright,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }

                        IconButton(
                            onClick = onReplayAudio,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Read Aloud",
                                tint = JarvisCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Tool executed badge if any
                    message.toolName?.let { tool ->
                        Spacer(modifier = Modifier.height(6.dp))
                        ToolBadge(toolName = tool)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = message.content,
                        color = JarvisTextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
