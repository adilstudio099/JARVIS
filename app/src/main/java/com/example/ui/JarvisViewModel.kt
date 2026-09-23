package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SpeechManager
import com.example.data.JarvisRepository
import com.example.data.local.ChatMessageEntity
import com.example.data.local.JarvisDatabase
import com.example.data.local.NoteEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.TodoEntity
import com.example.data.remote.GeminiResponse
import com.example.data.remote.GeminiService
import com.example.domain.TaskExecutionResult
import com.example.domain.TaskExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val db = JarvisDatabase.getDatabase(application)
    private val repository = JarvisRepository(db.chatDao(), db.noteDao(), db.todoDao(), db.reminderDao())
    private val geminiService = GeminiService()
    private val taskExecutor = TaskExecutor(repository)
    val speechManager = SpeechManager(application)

    private val prefs = application.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)

    // Data streams
    val messages: StateFlow<List<ChatMessageEntity>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<NoteEntity>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todos: StateFlow<List<TodoEntity>> = repository.allTodos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<ReminderEntity>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Audio & Interaction State
    val isListening: StateFlow<Boolean> = speechManager.isListening
    val isSpeaking: StateFlow<Boolean> = speechManager.isSpeaking
    val rmsLevel: StateFlow<Float> = speechManager.rmsLevel
    val speechError: StateFlow<String?> = speechManager.speechError

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _statusMessage = MutableStateFlow("SYSTEM READY")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _userError = MutableStateFlow<String?>(null)
    val userError: StateFlow<String?> = _userError.asStateFlow()

    // Settings
    private val _customApiKey = MutableStateFlow(prefs.getString("custom_gemini_key", "") ?: "")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _isTtsEnabled = MutableStateFlow(prefs.getBoolean("tts_enabled", true))
    val isTtsEnabled: StateFlow<Boolean> = _isTtsEnabled.asStateFlow()

    private val _speechRate = MutableStateFlow(prefs.getFloat("speech_rate", 1.0f))
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _speechPitch = MutableStateFlow(prefs.getFloat("speech_pitch", 0.95f))
    val speechPitch: StateFlow<Float> = _speechPitch.asStateFlow()

    init {
        speechManager.isTtsEnabled = _isTtsEnabled.value
        speechManager.speechRate = _speechRate.value
        speechManager.speechPitch = _speechPitch.value

        // Seed initial welcoming message if database is brand new
        viewModelScope.launch {
            val existing = repository.getRecentMessages(1)
            if (existing.isEmpty()) {
                val welcome = ChatMessageEntity(
                    role = "jarvis",
                    content = "Good day, sir. J.A.R.V.I.S. is online and at your service. All sub-systems are operating within nominal parameters. How may I assist you today?",
                    toolName = null
                )
                repository.insertMessage(welcome)
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _inputText.value = text
    }

    fun clearError() {
        _userError.value = null
        speechManager.clearError()
    }

    fun toggleVoiceListening() {
        if (speechManager.isListening.value) {
            speechManager.stopListening()
            _statusMessage.value = "SYSTEM READY"
            _liveTranscript.value = ""
        } else {
            _liveTranscript.value = ""
            _statusMessage.value = "LISTENING FOR DIRECTIVE..."
            speechManager.startListening(
                onResult = { text ->
                    _liveTranscript.value = ""
                    if (text.isNotBlank()) {
                        processUserPrompt(text)
                    }
                },
                onPartial = { partial ->
                    _liveTranscript.value = partial
                }
            )
        }
    }

    fun stopSpeaking() {
        speechManager.stopSpeaking()
        _statusMessage.value = "SYSTEM READY"
    }

    fun submitTextPrompt() {
        val text = _inputText.value.trim()
        if (text.isNotBlank()) {
            _inputText.value = ""
            processUserPrompt(text)
        }
    }

    fun processUserPrompt(promptText: String) {
        viewModelScope.launch {
            speechManager.stopSpeaking()
            _isProcessing.value = true
            _statusMessage.value = "PROCESSING DIRECTIVE..."

            // 1. Record user turn
            val userMsg = ChatMessageEntity(
                role = "user",
                content = promptText
            )
            repository.insertMessage(userMsg)

            // 2. Prepare conversation memory
            val recentEntities = repository.getRecentMessages(6)
            val history = recentEntities.map { entity ->
                (if (entity.role == "user") "user" else "model") to entity.content
            }

            // 3. Try Gemini API first
            val geminiResult = geminiService.generateWithTools(
                prompt = promptText,
                history = history,
                customApiKey = _customApiKey.value.ifBlank { null }
            )

            when (geminiResult) {
                is GeminiResponse.FunctionCallResponse -> {
                    // Tool call received from Gemini
                    _statusMessage.value = "EXECUTING MODULE [${geminiResult.functionName.uppercase()}]..."
                    val execResult = taskExecutor.executeTool(
                        geminiResult.functionName,
                        geminiResult.arguments
                    )
                    handleExecutionResult(execResult)
                }

                is GeminiResponse.TextResponse -> {
                    // Direct text from Gemini
                    val jarvisMsg = ChatMessageEntity(
                        role = "jarvis",
                        content = geminiResult.text
                    )
                    repository.insertMessage(jarvisMsg)
                    speechManager.speak(geminiResult.text)
                    _statusMessage.value = "RESPONSE TRANSMITTED"
                }

                is GeminiResponse.ErrorResponse -> {
                    // Fallback to local task parser if API key is missing or offline
                    val localResult = taskExecutor.executeLocalFallback(promptText)
                    if (localResult != null) {
                        _statusMessage.value = "EXECUTING OFFLINE MODULE..."
                        handleExecutionResult(localResult)
                    } else {
                        val fallbackSpoken = if (geminiResult.error == "API_KEY_MISSING") {
                            "Sir, I require an active Gemini API key in Settings to activate global neural processing, but my local command modules for notes, to-dos, reminders, math, and live weather are fully operational."
                        } else {
                            "Directive received. I encountered a communication issue: ${geminiResult.error}. Operating in local redundancy mode."
                        }
                        val jarvisMsg = ChatMessageEntity(
                            role = "jarvis",
                            content = fallbackSpoken
                        )
                        repository.insertMessage(jarvisMsg)
                        speechManager.speak(fallbackSpoken)
                        _statusMessage.value = "SYSTEM STANDBY"
                    }
                }
            }

            _isProcessing.value = false
        }
    }

    private suspend fun handleExecutionResult(result: TaskExecutionResult) {
        val jarvisMsg = ChatMessageEntity(
            role = "jarvis",
            content = result.displayContent,
            toolName = result.moduleName,
            toolDataJson = result.rawJson
        )
        repository.insertMessage(jarvisMsg)
        speechManager.speak(result.spokenResponse)
        _statusMessage.value = "MODULE COMPLETE: ${result.moduleName}"
    }

    // Direct Database Actions (for Task Center UI)
    fun addTodo(title: String, priority: String = "Normal") {
        viewModelScope.launch {
            repository.insertTodo(TodoEntity(title = title, priority = priority))
        }
    }

    fun toggleTodo(todo: TodoEntity) {
        viewModelScope.launch {
            repository.toggleTodoCompletion(todo.id, !todo.isCompleted)
        }
    }

    fun deleteTodo(id: Long) {
        viewModelScope.launch {
            repository.deleteTodoById(id)
        }
    }

    fun addNote(title: String, content: String, category: String = "General") {
        viewModelScope.launch {
            repository.insertNote(NoteEntity(title = title, content = content, category = category))
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            repository.deleteNoteById(id)
        }
    }

    fun addReminder(task: String, timeText: String) {
        viewModelScope.launch {
            val epoch = System.currentTimeMillis() + 30 * 60 * 1000
            repository.insertReminder(ReminderEntity(task = task, timeText = timeText, targetEpochMs = epoch))
        }
    }

    fun toggleReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.toggleReminderCompletion(reminder.id, !reminder.isCompleted)
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            repository.deleteReminderById(id)
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            val resetMsg = ChatMessageEntity(
                role = "jarvis",
                content = "Conversation cache purged. J.A.R.V.I.S. is ready for fresh commands, sir."
            )
            repository.insertMessage(resetMsg)
        }
    }

    // Settings actions
    fun setCustomApiKey(key: String) {
        _customApiKey.value = key.trim()
        prefs.edit().putString("custom_gemini_key", key.trim()).apply()
    }

    fun setTtsEnabled(enabled: Boolean) {
        _isTtsEnabled.value = enabled
        speechManager.isTtsEnabled = enabled
        prefs.edit().putBoolean("tts_enabled", enabled).apply()
        if (!enabled) speechManager.stopSpeaking()
    }

    fun setSpeechRate(rate: Float) {
        _speechRate.value = rate
        speechManager.speechRate = rate
        prefs.edit().putFloat("speech_rate", rate).apply()
    }

    fun setSpeechPitch(pitch: Float) {
        _speechPitch.value = pitch
        speechManager.speechPitch = pitch
        prefs.edit().putFloat("speech_pitch", pitch).apply()
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.release()
    }
}
