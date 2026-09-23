package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.DirectiveItemEntity
import com.example.ui.JarvisViewModel
import com.example.ui.components.ConcentricPulseWaves
import com.example.ui.components.CyberDecryptedText
import com.example.ui.components.HudCard
import com.example.ui.components.HudScanOverlay
import androidx.compose.material3.FloatingActionButton
import androidx.compose.ui.text.TextStyle
import com.example.ui.theme.JarvisBorderCyan
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisOrange
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisSpaceBlack
import com.example.ui.theme.JarvisSurfaceCard
import com.example.ui.theme.JarvisSurfaceDark
import com.example.ui.theme.JarvisSurfaceElevated
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TaskCenterScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val directives by viewModel.directives.collectAsStateWithLifecycle()
    val todos by viewModel.todos.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }

    // Aggregate dynamic directives with legacy records into a unified list
    val unifiedDirectives = remember(directives, todos, notes, reminders) {
        val list = mutableListOf<DirectiveItemEntity>()
        list.addAll(directives)

        // Merge todos not already in directives
        for (t in todos) {
            if (list.none { it.title.equals(t.title, ignoreCase = true) }) {
                list.add(
                    DirectiveItemEntity(
                        id = t.id * 1000 + 1,
                        type = "task",
                        title = t.title,
                        content = "Priority: ${t.priority}",
                        isCompleted = t.isCompleted,
                        timestamp = t.timestamp
                    )
                )
            }
        }

        // Merge notes not already in directives
        for (n in notes) {
            if (list.none { it.title.equals(n.title, ignoreCase = true) }) {
                list.add(
                    DirectiveItemEntity(
                        id = n.id * 1000 + 2,
                        type = "note",
                        title = n.title,
                        content = n.content,
                        tags = n.category,
                        timestamp = n.timestamp
                    )
                )
            }
        }

        // Merge reminders not already in directives
        for (r in reminders) {
            if (list.none { it.title.equals(r.task, ignoreCase = true) }) {
                list.add(
                    DirectiveItemEntity(
                        id = r.id * 1000 + 3,
                        type = "reminder",
                        title = r.task,
                        content = "Time: ${r.timeText}",
                        isCompleted = r.isCompleted,
                        timestamp = r.timestamp
                    )
                )
            }
        }

        list.sortedByDescending { it.timestamp }
    }

    val filteredList = unifiedDirectives.filter { item ->
        val matchesFilter = when (selectedFilter) {
            "ALL" -> true
            "TASKS" -> item.type.contains("task", ignoreCase = true) || item.type.contains("todo", ignoreCase = true)
            "NOTES" -> item.type.contains("note", ignoreCase = true) || item.type.contains("memo", ignoreCase = true)
            "REMINDERS" -> item.type.contains("reminder", ignoreCase = true) || item.type.contains("alarm", ignoreCase = true)
            "MEMORIES" -> item.type.contains("memory", ignoreCase = true) || item.type.contains("fact", ignoreCase = true)
            else -> item.type.contains(selectedFilter, ignoreCase = true)
        }
        val matchesSearch = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.content.contains(searchQuery, ignoreCase = true) ||
                item.tags.contains(searchQuery, ignoreCase = true)

        matchesFilter && matchesSearch
    }

    val filters = listOf(
        "ALL" to unifiedDirectives.size,
        "TASKS" to unifiedDirectives.count { it.type.contains("task", true) || it.type.contains("todo", true) },
        "NOTES" to unifiedDirectives.count { it.type.contains("note", true) || it.type.contains("memo", true) },
        "REMINDERS" to unifiedDirectives.count { it.type.contains("reminder", true) },
        "MEMORIES" to unifiedDirectives.count { it.type.contains("memory", true) || it.type.contains("fact", true) }
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisSpaceBlack)
    ) {
        // Holographic Reticle Scan
        HudScanOverlay(
            modifier = Modifier.fillMaxSize(),
            laserColor = JarvisCyan,
            scanDurationMillis = 5200
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    CyberDecryptedText(
                        text = "COMMAND VAULT // DIRECTIVES",
                        modifier = Modifier.testTag("vault_header_title"),
                        color = JarvisCyanBright,
                        style = TextStyle(
                            fontSize = 17.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "Dynamic Data, Reminders & AI Memories",
                        color = JarvisTextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(JarvisSurfaceElevated)
                        .border(1.dp, JarvisBorderCyan, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${filteredList.size} ITEMS",
                        color = JarvisCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search vault directives, tasks, notes (Urdu / English)...",
                        color = JarvisTextMuted,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = JarvisCyan,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vault_search_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JarvisCyan,
                    unfocusedBorderColor = JarvisBorderCyan,
                    focusedTextColor = JarvisTextPrimary,
                    unfocusedTextColor = JarvisTextPrimary,
                    cursorColor = JarvisCyan,
                    focusedContainerColor = JarvisSurfaceElevated,
                    unfocusedContainerColor = JarvisSurfaceDark
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { (name, count) ->
                    val isSelected = selectedFilter == name
                    Box(
                        modifier = Modifier
                            .testTag("filter_chip_$name")
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) JarvisCyan else JarvisSurfaceCard)
                            .border(
                                1.dp,
                                if (isSelected) JarvisCyanBright else JarvisBorderCyan,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedFilter = name }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$name ($count)",
                            color = if (isSelected) JarvisSpaceBlack else JarvisTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Directives List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Empty",
                            tint = JarvisCyan.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (searchQuery.isBlank()) "NO DIRECTIVES SAVED YET" else "NO MATCHING RECORDS FOUND",
                            color = JarvisCyan,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Ask J.A.R.V.I.S. in chat to remember or save anything, or tap '+' below.",
                            color = JarvisTextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 76.dp)
                ) {
                    items(filteredList, key = { "${it.type}_${it.id}" }) { item ->
                        DirectiveCard(
                            directive = item,
                            onToggle = {
                                viewModel.toggleDirective(item)
                            },
                            onDelete = {
                                viewModel.deleteDirective(item.id)
                            }
                        )
                    }
                }
            }
        }

        // Cybernetic Pulsing FAB for manual directive addition
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            ConcentricPulseWaves(
                baseSize = 56.dp,
                pulseColor = JarvisCyan,
                waveCount = 2,
                isActive = true
            )
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_directive_fab"),
                containerColor = JarvisCyan,
                contentColor = JarvisSpaceBlack,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Directive")
            }
        }

        // Dialog for adding any directive
        if (showAddDialog) {
            AddDirectiveDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { title, content, type, tags ->
                    viewModel.addDirective(title, content, type, tags)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun DirectiveCard(
    directive: DirectiveItemEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val isUrdu = isUrduText(directive.title) || isUrduText(directive.content)
    val typeColor = when (directive.type.lowercase()) {
        "task", "todo" -> JarvisCyanBright
        "note", "memo" -> JarvisOrange
        "reminder", "alarm" -> JarvisGreen
        "memory", "fact" -> JarvisRed
        else -> JarvisCyan
    }

    val typeIcon = when (directive.type.lowercase()) {
        "task", "todo" -> Icons.Default.TaskAlt
        "note", "memo" -> Icons.Default.EditNote
        "reminder", "alarm" -> Icons.Default.Alarm
        else -> Icons.Default.Memory
    }

    HudCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("directive_card_${directive.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Type badge & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(typeColor.copy(alpha = 0.15f))
                            .border(0.8.dp, typeColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = directive.type.uppercase(),
                            color = typeColor,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (directive.tags.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "#${directive.tags}",
                            color = JarvisTextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Checkbox toggle for tasks/reminders
                    if (directive.type.contains("task", true) || directive.type.contains("todo", true) || directive.type.contains("reminder", true)) {
                        IconButton(
                            onClick = onToggle,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (directive.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Toggle Complete",
                                tint = if (directive.isCompleted) JarvisGreen else JarvisTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = JarvisRed.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Body content with RTL support for Urdu
            CompositionLocalProvider(
                LocalLayoutDirection provides if (isUrdu) LayoutDirection.Rtl else LayoutDirection.Ltr
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = directive.title,
                        color = if (directive.isCompleted) JarvisTextMuted else JarvisTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (directive.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (directive.content.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = directive.content,
                            color = JarvisTextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val timeStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(directive.timestamp))
            Text(
                text = "LOGGED: $timeStr",
                color = JarvisTextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun AddDirectiveDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, content: String, type: String, tags: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("task") }
    var tags by remember { mutableStateOf("") }

    val types = listOf("task", "note", "reminder", "memory", "custom")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = JarvisSurfaceDark,
        title = {
            Text(
                text = "ADD NEW DIRECTIVE",
                color = JarvisCyanBright,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Type selector
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(types) { t ->
                        val isSel = selectedType == t
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) JarvisCyan else JarvisSurfaceCard)
                                .clickable { selectedType = t }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = t.uppercase(),
                                color = if (isSel) JarvisSpaceBlack else JarvisTextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title / Headline", color = JarvisTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisBorderCyan,
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Details / Content / Time", color = JarvisTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisBorderCyan,
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary
                    ),
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tag / Category (optional)", color = JarvisTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisBorderCyan,
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onAdd(title.trim(), content.trim(), selectedType, tags.trim())
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan)
            ) {
                Text("SAVE", color = JarvisSpaceBlack, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = JarvisTextMuted)
            }
        }
    )
}

private fun isUrduText(text: String): Boolean {
    return text.any { it in '\u0600'..'\u06FF' || it in '\u0750'..'\u077F' || it in '\uFB50'..'\uFDFF' || it in '\uFE70'..'\uFEFF' }
}
