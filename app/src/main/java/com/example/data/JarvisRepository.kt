package com.example.data

import com.example.data.local.ChatDao
import com.example.data.local.ChatMessageEntity
import com.example.data.local.DirectiveDao
import com.example.data.local.DirectiveItemEntity
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
    private val reminderDao: ReminderDao,
    private val directiveDao: DirectiveDao
) {
    // General Directives & Dynamic Vault
    val allDirectives: Flow<List<DirectiveItemEntity>> = directiveDao.getAllItems()
    suspend fun insertDirective(item: DirectiveItemEntity): Long = directiveDao.insertItem(item)
    suspend fun updateDirective(item: DirectiveItemEntity) = directiveDao.updateItem(item)
    suspend fun deleteDirective(item: DirectiveItemEntity) = directiveDao.deleteItem(item)
    suspend fun deleteDirectiveById(id: Long) = directiveDao.deleteById(id)
    suspend fun toggleDirectiveCompletion(id: Long, completed: Boolean) = directiveDao.setCompleted(id, completed)
    suspend fun searchDirectives(query: String): List<DirectiveItemEntity> = directiveDao.searchItems(query)
    suspend fun getDirectivesByType(type: String): List<DirectiveItemEntity> = directiveDao.getDirectivesByType(type)
    suspend fun getRecentDirectives(limit: Int = 10): List<DirectiveItemEntity> = directiveDao.getRecentItems(limit)

    // Chat & Conversation Memory
    val allMessages: Flow<List<ChatMessageEntity>> = chatDao.getAllMessages()
    suspend fun insertMessage(message: ChatMessageEntity): Long = chatDao.insertMessage(message)
    suspend fun clearHistory() = chatDao.clearHistory()
    suspend fun clearChatHistory() = chatDao.clearHistory()
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
