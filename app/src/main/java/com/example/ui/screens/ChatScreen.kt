package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.LiveSessionState
import com.example.data.local.ChatMessageEntity
import com.example.ui.JarvisViewModel
import com.example.ui.components.ArcReactor
import com.example.ui.components.CyberDecryptedText
import com.example.ui.components.CyberGlowingMicButton
import com.example.ui.components.HudCard
import com.example.ui.components.HudScanOverlay
import com.example.ui.components.HudTechnicalGridBackground
import com.example.ui.components.HudWaveform
import com.example.ui.components.SystemTelemetryHeader
import com.example.ui.components.cyberCornerReticles
import com.example.ui.theme.JarvisBorderCyan
import com.example.ui.theme.JarvisBorderGreen
import com.example.ui.theme.JarvisBorderGlow
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisCyanDark
import com.example.ui.theme.JarvisCyanDeep
import com.example.ui.theme.JarvisCyanGlow
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisGreenBright
import com.example.ui.theme.JarvisGreenDark
import com.example.ui.theme.JarvisGreenDeep
import com.example.ui.theme.JarvisGreenGlow
import com.example.ui.theme.JarvisOrange
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisSpaceBlack
import com.example.ui.theme.JarvisSurfaceCard
import com.example.ui.theme.JarvisSurfaceDark
import com.example.ui.theme.JarvisSurfaceElevated
import com.example.ui.theme.JarvisTextGlow
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@Composable
fun ChatScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val isPlayingAudio by viewModel.isPlayingAudio.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val rmsLevel by viewModel.rmsLevel.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val speechError by viewModel.speechError.collectAsStateWithLifecycle()
    val userError by viewModel.userError.collectAsStateWithLifecycle()
    val apiKeyMissingAlert by viewModel.apiKeyMissingAlert.collectAsStateWithLifecycle()

    // Gemini Live Bidirectional Session States
    val liveSessionState by viewModel.liveSessionState.collectAsStateWithLifecycle()
    val liveTranscript by viewModel.liveTranscript.collectAsStateWithLifecycle()
    val liveRmsLevel by viewModel.liveRmsLevel.collectAsStateWithLifecycle()
    val liveSessionError by viewModel.liveSessionError.collectAsStateWithLifecycle()

    val isLiveActive = liveSessionState != LiveSessionState.DISCONNECTED && liveSessionState != LiveSessionState.ERROR
    val isLiveListening = liveSessionState == LiveSessionState.LISTENING
    val isLiveSpeaking = liveSessionState == LiveSessionState.SPEAKING
    val isLiveThinking = liveSessionState == LiveSessionState.THINKING || liveSessionState == LiveSessionState.CONNECTING

    val effectiveIsListening = if (isLiveActive) isLiveListening else isRecording
    val effectiveIsSpeaking = if (isLiveActive) isLiveSpeaking else isPlayingAudio
    val effectiveIsProcessing = if (isLiveActive) isLiveThinking else isProcessing
    val effectiveRmsLevel = if (isLiveActive) liveRmsLevel else rmsLevel

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    var showPermissionRationaleDialog by remember { mutableStateOf(false) }

    // Runtime Permission Launcher for Microphone
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleLiveVoiceSession()
        } else {
            viewModel.onRecordPermissionDenied()
            showPermissionRationaleDialog = true
        }
    }

    fun handleLiveVoiceToggle() {
        if (isLiveActive) {
            viewModel.stopLiveVoiceSession()
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                viewModel.toggleLiveVoiceSession()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    // Scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Show error snackbar
    LaunchedEffect(speechError, userError, liveSessionError) {
        val error = userError ?: liveSessionError ?: speechError
        if (error != null) {
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    val quickCommands = listOf(
        "🌤 اسلام آباد میں موسم کا حال",
        "⚡ آج کی اہم ترین تازہ خبریں",
        "📞 03001234567 پر کال ملاؤ",
        "⏰ صبح 7 بجے کا الارم لگاؤ",
        "🗺 لاہور مال روڈ کا نقشہ دکھاؤ",
        "📝 کلائنٹ میٹنگ کا ایجنڈا محفوظ کرو",
        "🔢 15% discount on 12500"
    )

    HudTechnicalGridBackground(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // High-Tech Cyan Laser Scanline Overlay
        HudScanOverlay(
            modifier = Modifier.fillMaxSize(),
            laserColor = JarvisCyanGlow,
            scanDurationMillis = 4600
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // HUD Telemetry Top Bar
            SystemTelemetryHeader(
                statusText = if (isLiveActive) {
                    when (liveSessionState) {
                        LiveSessionState.CONNECTING -> "LIVE // CONNECTING..."
                        LiveSessionState.LISTENING -> "LIVE // LISTENING (URDU/ENG)"
                        LiveSessionState.THINKING -> "LIVE // THINKING..."
                        LiveSessionState.SPEAKING -> "LIVE // VOCAL TRANSMISSION"
                        else -> "LIVE STREAM ACTIVE"
                    }
                } else statusMessage,
                isOnline = !effectiveIsProcessing
            )

            // API Key Missing Warning Banner
            if (apiKeyMissingAlert) {
                Surface(
                    color = JarvisOrange.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, JarvisOrange),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("api_key_missing_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = JarvisOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "⚠️ Gemini API Key درکار ہے۔ براہ کرم سیٹنگز میں چیک کریں۔",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Button(
                            onClick = onNavigateToSettings,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisOrange,
                                contentColor = JarvisSpaceBlack
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("سیٹنگز", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Centered Arc Reactor Hero Visualizer (Iron Man Green HUD Arc Reactor)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Arc Reactor with reactive pulse, radar cone and glowing coils
                    ArcReactor(
                        size = 132.dp,
                        isListening = effectiveIsListening,
                        isSpeaking = effectiveIsSpeaking,
                        isProcessing = effectiveIsProcessing,
                        rmsLevel = effectiveRmsLevel,
                        onClick = { handleLiveVoiceToggle() }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Animated Voice Waveform & Dynamic Cyber Decrypted Status
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        HudWaveform(
                            isEmitting = isLiveActive || effectiveIsListening || effectiveIsSpeaking || effectiveIsProcessing,
                            tint = if (effectiveIsListening) JarvisCyanGlow
                            else if (effectiveIsSpeaking) JarvisCyanBright
                            else if (effectiveIsProcessing) JarvisOrange
                            else JarvisCyanDeep
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        CyberDecryptedText(
                            text = when {
                                liveSessionState == LiveSessionState.CONNECTING -> "INITIALIZING GEMINI LIVE PROTOCOL..."
                                liveSessionState == LiveSessionState.SPEAKING -> "TRANSMITTING LIVE VOCAL AUDIO..."
                                liveSessionState == LiveSessionState.THINKING -> "PROCESSING INTENT // THINKING..."
                                liveSessionState == LiveSessionState.LISTENING -> "LIVE LISTENING // SPEAK NATURALLY (بولیں)"
                                isLiveActive -> "LIVE CONVERSATION OPEN (OPEN MIC)"
                                effectiveIsListening -> "LISTENING // صوتی حکم وصول ہو رہا ہے"
                                effectiveIsProcessing -> "SYNTHESIZING PROTOCOL..."
                                effectiveIsSpeaking -> "TRANSMITTING VOCAL AUDIO..."
                                else -> "TAP INSTRUMENT DIAL FOR LIVE VOICE"
                            },
                            color = if (isLiveActive) JarvisCyanGlow else JarvisTextSecondary,
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }

                    // Live Session Badge and Active Transcript HUD Display
                    if (isLiveActive) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(JarvisCyanDeep.copy(alpha = 0.4f))
                                .border(1.dp, JarvisBorderCyan, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(JarvisCyanGlow)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIVE SCI-FI VOICE // GEMINI-3.5-FLASH",
                                color = JarvisCyanGlow,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TAP TO END",
                                color = JarvisTextMuted,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.clickable { viewModel.stopLiveVoiceSession() }
                            )
                        }

                        // Live RTL Transcript Card
                        if (liveTranscript.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth(0.92f)
                                    .testTag("live_transcript_card"),
                                color = JarvisSurfaceCard.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderCyan)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "LIVE SPEECH TRANSCRIPTION",
                                        color = JarvisCyanBright,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                        Text(
                                            text = liveTranscript,
                                            color = JarvisTextPrimary,
                                            fontSize = 13.sp,
                                            lineHeight = 20.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
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
                            .testTag("quick_command_${cmd.take(8)}")
                            .clip(RoundedCornerShape(16.dp))
                            .background(JarvisSurfaceCard)
                            .border(0.8.dp, JarvisBorderGreen, RoundedCornerShape(16.dp))
                            .cyberCornerReticles(bracketColor = JarvisGreenGlow, bracketLength = 6.dp, strokeWidth = 1.dp, glowAlpha = 0.6f)
                            .clickable {
                                viewModel.processUserPrompt(cmd.substringAfter(" "))
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cmd,
                            color = JarvisGreenBright,
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

                // Processing Indicator
                if (effectiveIsProcessing && !isLiveActive) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            HudCard(modifier = Modifier.widthIn(max = 280.dp)) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = JarvisGreenGlow,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "پروسیسنگ جاری ہے (Querying Gemini)...",
                                        color = JarvisGreenBright,
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
                    Brush.verticalGradient(listOf(JarvisBorderGreen, Color.Transparent))
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Voice Mic Button (One tap starts/stops continuous live voice session)
                    CyberGlowingMicButton(
                        isListening = isLiveActive || effectiveIsListening,
                        isSpeaking = effectiveIsSpeaking,
                        rmsLevel = effectiveRmsLevel,
                        onClick = { handleLiveVoiceToggle() }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Typed Text Field (Keeps text input 100% available at all times)
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { viewModel.onInputTextChanged(it) },
                        placeholder = {
                            Text(
                                text = "حکم لکھیں یا بولیں (Enter command)...",
                                color = JarvisTextMuted,
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_text_input"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisGreenGlow,
                            unfocusedBorderColor = JarvisBorderGreen,
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary,
                            cursorColor = JarvisGreenGlow,
                            focusedContainerColor = JarvisSurfaceElevated,
                            unfocusedContainerColor = JarvisSurfaceCard
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { viewModel.submitTextPrompt() })
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (effectiveIsSpeaking || isLiveActive) {
                        IconButton(
                            onClick = { viewModel.stopAudioPlayback() },
                            modifier = Modifier
                                .testTag("stop_speaking_button")
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(JarvisOrange.copy(alpha = 0.2f))
                                .border(1.dp, JarvisOrange, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop Audio",
                                tint = JarvisOrange
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { viewModel.submitTextPrompt() },
                            modifier = Modifier
                                .testTag("send_button")
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (inputText.isNotBlank()) JarvisGreenGlow else JarvisSurfaceElevated)
                                .border(
                                    if (inputText.isNotBlank()) 1.5.dp else 1.dp,
                                    if (inputText.isNotBlank()) Color.White else JarvisBorderGreen,
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
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )

        // Permission Rationale Dialog
        if (showPermissionRationaleDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionRationaleDialog = false },
                title = {
                    Text(
                        text = "مائیکروفون کی اجازت درکار ہے",
                        color = JarvisGreenBright,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "J.A.R.V.I.S. کو لائیو صوتی گفتگو اور احکامات سننے کے لیے مائیکروفون کی اجازت درکار ہے۔ براہ کرم سیٹنگز میں جا کر اجازت فراہم کریں۔\n\n(Microphone access is required for real-time live speech conversation)",
                        color = JarvisTextPrimary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showPermissionRationaleDialog = false
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisGreenGlow, contentColor = JarvisSpaceBlack)
                    ) {
                        Text("ایپ سیٹنگز کھولیں", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPermissionRationaleDialog = false }) {
                        Text("منسوخ کریں", color = JarvisTextMuted)
                    }
                },
                containerColor = JarvisSurfaceDark,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessageEntity,
    onReplayAudio: () -> Unit = {}
) {
    val isUser = message.role == "user"
    val isUrdu = message.content.any {
        it in '\u0600'..'\u06FF' || it in '\u0750'..'\u077F' || it in '\uFB50'..'\uFDFF' || it in '\uFE70'..'\uFEFF'
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) JarvisGreenDeep.copy(alpha = 0.35f) else JarvisSurfaceCard,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isUser) JarvisGreenGlow.copy(alpha = 0.6f) else JarvisBorderGreen
            ),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "OPERATOR" else "J.A.R.V.I.S.",
                        color = if (isUser) JarvisGreenBright else JarvisGreenGlow,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    if (!isUser) {
                        IconButton(
                            onClick = onReplayAudio,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Replay audio",
                                tint = JarvisGreenGlow,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                CompositionLocalProvider(
                    LocalLayoutDirection provides if (isUrdu) LayoutDirection.Rtl else LayoutDirection.Ltr
                ) {
                    Text(
                        text = message.content,
                        color = JarvisTextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                if (message.toolName != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = JarvisGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, JarvisGreenGlow)
                    ) {
                        Text(
                            text = "⚡ Directive Executed: ${message.toolName}",
                            color = JarvisGreenBright,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
