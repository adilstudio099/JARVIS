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
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.NoteEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.TodoEntity
import com.example.ui.JarvisViewModel
import com.example.ui.components.CyberDecryptedText
import com.example.ui.components.CyberPulseFab
import com.example.ui.components.HudCard
import com.example.ui.components.HudScanOverlay
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
    val todos by viewModel.todos.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("To-Dos (${todos.size})", "Notes (${notes.size})", "Reminders (${reminders.size})")

    var showAddDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisSpaceBlack)
    ) {
        // Holographic HUD Scan Overlay
        HudScanOverlay(
            modifier = Modifier.fillMaxSize(),
            laserColor = JarvisCyan,
            scanDurationMillis = 5200
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
                        text = "TASK MODULES // OPERATIONS",
                        color = JarvisCyanBright,
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "System Directive Management",
                        color = JarvisTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // High-tech Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = JarvisSurfaceDark,
                contentColor = JarvisCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = JarvisCyan
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                color = if (selectedTab == index) JarvisCyanBright else JarvisTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> TodoList(
                    todos = todos,
                    onToggle = { viewModel.toggleTodo(it) },
                    onDelete = { viewModel.deleteTodo(it) }
                )
                1 -> NotesList(
                    notes = notes,
                    onDelete = { viewModel.deleteNote(it) }
                )
                2 -> RemindersList(
                    reminders = reminders,
                    onToggle = { viewModel.toggleReminder(it) },
                    onDelete = { viewModel.deleteReminder(it) }
                )
            }
        }

        // Cybernetic Pulsing Floating Action Button to add item manually
        CyberPulseFab(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            icon = Icons.Default.Add,
            contentDescription = "Add New Entry"
        )

        // Dialog for adding task/note/reminder
        if (showAddDialog) {
            AddItemDialog(
                currentTab = selectedTab,
                onDismiss = { showAddDialog = false },
                onAddTodo = { title, prio ->
                    viewModel.addTodo(title, prio)
                    showAddDialog = false
                },
                onAddNote = { title, content, cat ->
                    viewModel.addNote(title, content, cat)
                    showAddDialog = false
                },
                onAddReminder = { task, time ->
                    viewModel.addReminder(task, time)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun TodoList(
    todos: List<TodoEntity>,
    onToggle: (TodoEntity) -> Unit,
    onDelete: (Long) -> Unit
) {
    if (todos.isEmpty()) {
        EmptyHudState("No active to-dos. Tell Jarvis: 'Add task...'")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(todos, key = { it.id }) { todo ->
                HudCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onToggle(todo) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (todo.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = if (todo.isCompleted) "Completed" else "Mark Complete",
                                tint = if (todo.isCompleted) JarvisGreen else JarvisCyan
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = todo.title,
                                color = if (todo.isCompleted) JarvisTextMuted else JarvisTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            )
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (todo.priority == "High") JarvisRed.copy(alpha = 0.2f) else JarvisCyan.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = todo.priority.uppercase(),
                                        color = if (todo.priority == "High") JarvisRed else JarvisCyan,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { onDelete(todo.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete To-Do",
                                tint = JarvisTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotesList(
    notes: List<NoteEntity>,
    onDelete: (Long) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()) }

    if (notes.isEmpty()) {
        EmptyHudState("No saved notes. Tell Jarvis: 'Note down...'")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(notes, key = { it.id }) { note ->
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
                                text = note.title,
                                color = JarvisCyanBright,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            IconButton(
                                onClick = { onDelete(note.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Note",
                                    tint = JarvisTextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (note.content.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = note.content,
                                color = JarvisTextPrimary,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(JarvisCyan.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = note.category.uppercase(),
                                    color = JarvisCyan,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Text(
                                text = dateFormat.format(Date(note.timestamp)),
                                color = JarvisTextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RemindersList(
    reminders: List<ReminderEntity>,
    onToggle: (ReminderEntity) -> Unit,
    onDelete: (Long) -> Unit
) {
    if (reminders.isEmpty()) {
        EmptyHudState("No scheduled reminders. Tell Jarvis: 'Remind me in 10 mins...'")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(reminders, key = { it.id }) { reminder ->
                HudCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = "Reminder Icon",
                            tint = if (reminder.isCompleted) JarvisTextMuted else JarvisOrange,
                            modifier = Modifier.size(26.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = reminder.task,
                                color = if (reminder.isCompleted) JarvisTextMuted else JarvisTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            )
                            Text(
                                text = "Scheduled: ${reminder.timeText}",
                                color = if (reminder.isCompleted) JarvisTextMuted else JarvisOrange,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        IconButton(
                            onClick = { onToggle(reminder) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (reminder.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Toggle Done",
                                tint = if (reminder.isCompleted) JarvisGreen else JarvisCyan
                            )
                        }

                        IconButton(
                            onClick = { onDelete(reminder.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Reminder",
                                tint = JarvisTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyHudState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(JarvisSurfaceElevated)
                    .border(1.dp, JarvisBorderCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = null,
                    tint = JarvisCyan,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "NO ACTIVE DIRECTIVES",
                color = JarvisCyanBright,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                color = JarvisTextSecondary,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun AddItemDialog(
    currentTab: Int,
    onDismiss: () -> Unit,
    onAddTodo: (String, String) -> Unit,
    onAddNote: (String, String, String) -> Unit,
    onAddReminder: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var extra by remember { mutableStateOf(if (currentTab == 0) "Normal" else if (currentTab == 1) "General" else "In 30 minutes") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = JarvisSurfaceCard,
        title = {
            Text(
                text = when (currentTab) {
                    0 -> "NEW TO-DO ITEM"
                    1 -> "NEW NOTE"
                    else -> "NEW REMINDER"
                },
                color = JarvisCyanBright,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (currentTab == 1) "Note Title" else "Task Description") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisBorderCyan,
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (currentTab == 1) {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Note Content") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = JarvisBorderCyan,
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }

                OutlinedTextField(
                    value = extra,
                    onValueChange = { extra = it },
                    label = {
                        Text(
                            when (currentTab) {
                                0 -> "Priority (High, Normal, Low)"
                                1 -> "Category"
                                else -> "Time / Due (e.g. 5:00 PM, 30 mins)"
                            }
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisBorderCyan,
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        when (currentTab) {
                            0 -> onAddTodo(title, extra)
                            1 -> onAddNote(title, content, extra)
                            else -> onAddReminder(title, extra)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = JarvisSpaceBlack)
            ) {
                Text("Execute")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = JarvisTextSecondary)
            }
        }
    )
}
