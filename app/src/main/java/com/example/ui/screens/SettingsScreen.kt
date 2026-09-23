package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.ui.JarvisViewModel
import com.example.ui.components.CyberDecryptedText
import com.example.ui.components.HudCard
import com.example.ui.components.HudScanOverlay
import com.example.ui.components.HudTechnicalGridBackground
import com.example.ui.theme.JarvisBorderCyan
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisOrange
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisSpaceBlack
import com.example.ui.theme.JarvisSurfaceElevated
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@Composable
fun SettingsScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val customApiKey by viewModel.customApiKey.collectAsStateWithLifecycle()
    val isTtsEnabled by viewModel.isTtsEnabled.collectAsStateWithLifecycle()
    val speechRate by viewModel.speechRate.collectAsStateWithLifecycle()
    val speechPitch by viewModel.speechPitch.collectAsStateWithLifecycle()
    val isTestingConnection by viewModel.isTestingConnection.collectAsStateWithLifecycle()
    val testConnectionResult by viewModel.testConnectionResult.collectAsStateWithLifecycle()

    var keyInput by remember(customApiKey) { mutableStateOf(customApiKey) }
    var keySavedNotice by remember { mutableStateOf(false) }

    val isBuiltInKeyPresent = remember {
        BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
    }

    HudTechnicalGridBackground(
        modifier = modifier.fillMaxSize()
    ) {
        HudScanOverlay(
            modifier = Modifier.fillMaxSize(),
            laserColor = JarvisCyan,
            scanDurationMillis = 5400
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column {
                CyberDecryptedText(
                    text = "SYSTEM PARAMETERS // CONFIGURATION",
                    color = JarvisCyanBright,
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = "Jarvis Settings & Telemetry",
                    color = JarvisTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Section 1: AI Core & Neural Connection
            HudCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = JarvisCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GEMINI AI NEURAL NETWORK (3.5 FLASH)",
                            color = JarvisCyanBright,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val statusText = when {
                        customApiKey.isNotBlank() -> "Custom API Key Active"
                        isBuiltInKeyPresent -> "Connected via Secrets Panel"
                        else -> "API Key Missing (Local Redundancy Active)"
                    }
                    val statusColor = when {
                        customApiKey.isNotBlank() || isBuiltInKeyPresent -> JarvisGreen
                        else -> JarvisOrange
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusColor.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Gemini API Key:",
                        color = JarvisTextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = {
                            keyInput = it
                            keySavedNotice = false
                        },
                        placeholder = { Text("Paste your Gemini API key here...", color = JarvisTextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = JarvisBorderCyan,
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary,
                            cursorColor = JarvisCyan
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                viewModel.setCustomApiKey(keyInput)
                                keySavedNotice = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisCyan,
                                contentColor = JarvisSpaceBlack
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("save_api_key_button")
                        ) {
                            Text("Save Key", fontWeight = FontWeight.Bold)
                        }

                        if (keySavedNotice) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = JarvisGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Saved!", color = JarvisGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // TEST CONNECTION BUTTON
                    Button(
                        onClick = { viewModel.testGeminiConnection() },
                        enabled = !isTestingConnection,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JarvisCyan.copy(alpha = 0.2f),
                            contentColor = JarvisCyanBright
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_connection_button")
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = JarvisCyan,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testing Connection...", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        } else {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Gemini API Connection", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Test Connection Result Display
                    testConnectionResult?.let { (success, message) ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background((if (success) JarvisGreen else JarvisRed).copy(alpha = 0.12f))
                                .border(1.dp, if (success) JarvisGreen else JarvisRed, RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (success) JarvisGreen else JarvisRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = message,
                                    color = if (success) JarvisGreen else JarvisRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Voice & Audio Pipeline
            HudCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = JarvisCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GEMINI-NATIVE AUDIO PIPELINE",
                            color = JarvisCyanBright,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "• Voice Input: AudioRecord (16kHz PCM / WAV) sent directly to Gemini multimodal understanding.",
                        color = JarvisTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Voice Output: Gemini native vocal playback with audio player.",
                        color = JarvisTextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // TTS Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Audio Playback", color = JarvisTextPrimary, fontSize = 14.sp)
                            Text(text = "Play spoken response automatically", color = JarvisTextMuted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = isTtsEnabled,
                            onCheckedChange = { viewModel.setTtsEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = JarvisSpaceBlack,
                                checkedTrackColor = JarvisCyan,
                                uncheckedThumbColor = JarvisTextMuted,
                                uncheckedTrackColor = JarvisSurfaceElevated
                            ),
                            modifier = Modifier.testTag("tts_toggle")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Speech Rate Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Speech Rate (Pace)", color = JarvisTextPrimary, fontSize = 13.sp)
                            Text(text = "${"%.2f".format(speechRate)}x", color = JarvisCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = speechRate,
                            onValueChange = { viewModel.setSpeechRate(it) },
                            valueRange = 0.6f..1.6f,
                            colors = SliderDefaults.colors(thumbColor = JarvisCyan, activeTrackColor = JarvisCyan, inactiveTrackColor = JarvisSurfaceElevated),
                            modifier = Modifier.testTag("speech_rate_slider")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.speechManager.speak("سسٹم نارمل ہے۔ تمام فنکشنز کامیابی سے کام کر رہے ہیں۔")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JarvisCyan.copy(alpha = 0.15f),
                            contentColor = JarvisCyanBright
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Urdu Voice Readout", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // Section 3: Diagnostic Telemetry
            HudCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "STARK PROTOCOL TELEMETRY",
                            color = JarvisCyanBright,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TelemetryItem("MODEL DESIGNATION", "gemini-3.5-flash (Free Tier)")
                    TelemetryItem("VOICE PIPELINE", "Gemini Live Bidirectional Streaming")
                    TelemetryItem("AUDIO INPUT", "AudioRecord 16kHz PCM (Real-Time)")
                    TelemetryItem("AUDIO OUTPUT", "Gemini 24kHz PCM Streaming Track")
                    TelemetryItem("INTERRUPTION / BARGE-IN", "Active Hardware VAD")
                    TelemetryItem("DEVICE ACTIONS", "Android Intents (Dial, SMS, Maps, Alarms)")
                    TelemetryItem("SEARCH GROUNDING", "Google Search Grounding (Live Web)")
                    TelemetryItem("PERSISTENCE", "Room SQLite Database v1")
                    TelemetryItem("LANGUAGE & SCRIPT", "Urdu Nastaliq RTL & English")

                    Spacer(modifier = Modifier.height(14.dp))

                    // Clear Chat History
                    Button(
                        onClick = { viewModel.clearChatHistory() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JarvisRed.copy(alpha = 0.15f),
                            contentColor = JarvisRed
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear Conversation History", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetryItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = JarvisTextMuted,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = JarvisTextPrimary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}
