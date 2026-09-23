package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.local.ChatMessageEntity
import com.example.ui.JarvisViewModel
import com.example.ui.components.CyberDecryptedText
import com.example.ui.components.HudCard
import com.example.ui.components.HudScanOverlay
import com.example.ui.components.ToolBadge
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisSpaceBlack
import com.example.ui.theme.JarvisSurfaceCard
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: JarvisViewModel,
    onNavigateToChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val userCommands = remember(messages) { messages.filter { it.role == "user" }.reversed() }
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy · HH:mm:ss", Locale.getDefault()) }

    var showClearConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisSpaceBlack)
    ) {
        // Holographic HUD Scan Overlay
        HudScanOverlay(
            modifier = Modifier.fillMaxSize(),
            laserColor = JarvisCyan,
            scanDurationMillis = 5000
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    CyberDecryptedText(
                        text = "HISTORICAL DIRECTIVES // ARCHIVE",
                        color = JarvisCyanBright,
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "Voice & Text Command Log",
                        color = JarvisTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (userCommands.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearConfirm = true },
                        modifier = Modifier.testTag("clear_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear History",
                            tint = JarvisRed
                        )
                    }
                }
            }

            if (userCommands.isEmpty()) {
                EmptyHudState("No historical commands logged in current session.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(userCommands, key = { it.id }) { cmd ->
                        // Find matching Jarvis reply if any
                        val reply = messages.firstOrNull { it.timestamp > cmd.timestamp && it.role == "jarvis" }

                        HudCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = dateFormat.format(Date(cmd.timestamp)),
                                        color = JarvisTextMuted,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    reply?.toolName?.let { tool ->
                                        ToolBadge(toolName = tool)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "\"${cmd.content}\"",
                                    color = JarvisTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                if (reply != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "↳ ${reply.content.take(120)}${if (reply.content.length > 120) "..." else ""}",
                                        color = JarvisTextSecondary,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.processUserPrompt(cmd.content)
                                            onNavigateToChat()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = JarvisCyan.copy(alpha = 0.2f),
                                            contentColor = JarvisCyanBright
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Rerun",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Re-Execute", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                containerColor = JarvisSurfaceCard,
                title = {
                    Text(
                        text = "PURGE COMMAND MEMORY",
                        color = JarvisRed,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Are you certain you wish to purge all conversation and command logs? This operation cannot be reversed.",
                        color = JarvisTextPrimary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearChatHistory()
                            showClearConfirm = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisRed, contentColor = Color.White)
                    ) {
                        Text("Purge Memory")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirm = false }) {
                        Text("Cancel", color = JarvisTextSecondary)
                    }
                }
            )
        }
    }
}
