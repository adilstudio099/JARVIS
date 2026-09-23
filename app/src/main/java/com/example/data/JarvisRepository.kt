package com.example.data

import com.example.data.local.ChatDao
import com.example.data.local.ChatMessageEntity
import com.example.data.local.NoteDao
import com.example.data.local.NoteEntity
import com.example.data.local.ReminderDao
import com.example.data.local.ReminderEntity
import com.example.data.local.TodoDao
import com.example.data.local.TodoEntity
import kotlinx.coroutines.flow.Flow

class JarvisRepository(
    private val chatDao: ChatDao,
    private val noteDao: NoteDao,
    private val todoDao: TodoDao,
    private val reminderDao: ReminderDao
) {
    // Chat & Conversation Memory
    val allMessages: Flow<List<ChatMessageEntity>> = chatDao.getAllMessages()
    suspend fun insertMessage(message: ChatMessageEntity): Long = chatDao.insertMessage(message)
    suspend fun clearHistory() = chatDao.clearHistory()
    suspend fun getRecentMessages(limit: Int = 10): List<ChatMessageEntity> = chatDao.getRecentMessages(limit)

    // Notes
    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()
    suspend fun insertNote(note: NoteEntity): Long = noteDao.insertNote(note)
    suspend fun deleteNote(note: NoteEntity) = noteDao.deleteNote(note)
    suspend fun deleteNoteById(id: Long) = noteDao.deleteNoteById(id)
    suspend fun searchNotes(query: String) = noteDao.searchNotes(query)

    // Todos
    val allTodos: Flow<List<TodoEntity>> = todoDao.getAllTodos()
    suspend fun insertTodo(todo: TodoEntity): Long = todoDao.insertTodo(todo)
    suspend fun updateTodo(todo: TodoEntity) = todoDao.updateTodo(todo)
    suspend fun deleteTodo(todo: TodoEntity) = todoDao.deleteTodo(todo)
    suspend fun deleteTodoById(id: Long) = todoDao.deleteTodoById(id)
    suspend fun toggleTodoCompletion(id: Long, completed: Boolean) = todoDao.setCompleted(id, completed)

    // Reminders
    val allReminders: Flow<List<ReminderEntity>> = reminderDao.getAllReminders()
    suspend fun insertReminder(reminder: ReminderEntity): Long = reminderDao.insertReminder(reminder)
    suspend fun updateReminder(reminder: ReminderEntity) = reminderDao.updateReminder(reminder)
    suspend fun deleteReminder(reminder: ReminderEntity) = reminderDao.deleteReminder(reminder)
    suspend fun deleteReminderById(id: Long) = reminderDao.deleteReminderById(id)
    suspend fun toggleReminderCompletion(id: Long, completed: Boolean) = reminderDao.setCompleted(id, completed)
}
