package com.example.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioRecorderManager
import com.example.audio.GeminiAudioPlayer
import com.example.audio.SpeechManager
import com.example.data.JarvisRepository
import com.example.data.local.ChatMessageEntity
import com.example.data.local.DirectiveItemEntity
import com.example.data.local.JarvisDatabase
import com.example.data.local.NoteEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.TodoEntity
import com.example.data.remote.GeminiResponse
import com.example.data.remote.GeminiService
import com.example.device.DeviceActionHandler
import com.example.domain.TaskExecutionResult
import com.example.domain.TaskExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "JarvisViewModel"
    }

    private val db = JarvisDatabase.getDatabase(application)
    private val repository = JarvisRepository(
        chatDao = db.chatDao(),
        noteDao = db.noteDao(),
        todoDao = db.todoDao(),
        reminderDao = db.reminderDao(),
        directiveDao = db.directiveDao()
    )
    val geminiService = GeminiService()
    val deviceActionHandler = DeviceActionHandler(application)
    val taskExecutor = TaskExecutor(repository, geminiService, deviceActionHandler)

    // Gemini-native audio pipeline
    val audioRecorderManager = AudioRecorderManager(application)
    val speechManager = SpeechManager(application)
    val geminiAudioPlayer = GeminiAudioPlayer(application)

    // Gemini Live real-time conversational session manager
    val liveSessionManager = com.example.audio.GeminiLiveSessionManager(
        context = application,
        audioRecorder = audioRecorderManager,
        audioPlayer = geminiAudioPlayer,
        geminiService = geminiService,
        taskExecutor = taskExecutor,
        speechManager = speechManager,
        repository = repository
    )

    val liveSessionState = liveSessionManager.sessionState
    val liveTranscript = liveSessionManager.liveTranscript
    val liveRmsLevel = liveSessionManager.rmsLevel
    val liveSessionError = liveSessionManager.errorMessage

    private val prefs = application.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)

    // Data streams
    val messages: StateFlow<List<ChatMessageEntity>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val directives: StateFlow<List<DirectiveItemEntity>> = repository.allDirectives
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<NoteEntity>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todos: StateFlow<List<TodoEntity>> = repository.allTodos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<ReminderEntity>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Audio & Interaction State
    val isRecording: StateFlow<Boolean> = audioRecorderManager.isRecording
    val isPlayingAudio: StateFlow<Boolean> = geminiAudioPlayer.isPlaying
    val rmsLevel: StateFlow<Float> = audioRecorderManager.rmsLevel
    val speechError: StateFlow<String?> = audioRecorderManager.recorderError

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _statusMessage = MutableStateFlow("SYSTEM READY")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _userError = MutableStateFlow<String?>(null)
    val userError: StateFlow<String?> = _userError.asStateFlow()

    private val _apiKeyMissingAlert = MutableStateFlow(false)
    val apiKeyMissingAlert: StateFlow<Boolean> = _apiKeyMissingAlert.asStateFlow()

    // Test Connection State
    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    private val _testConnectionResult = MutableStateFlow<Pair<Boolean, String>?>(null)
    val testConnectionResult: StateFlow<Pair<Boolean, String>?> = _testConnectionResult.asStateFlow()

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

        // Check startup API key status
        checkApiKeyStatus()

        // Seed initial message if database is empty
        viewModelScope.launch {
            try {
                val existing = repository.getRecentMessages(1)
                if (existing.isEmpty()) {
                    val welcome = ChatMessageEntity(
                        role = "jarvis",
                        content = "سسٹم آن لائن ہے۔ J.A.R.V.I.S. آپ کے حکم کا منتظر ہے۔ آپ مجھ سے کسی بھی کام کے متعلق اردو یا انگلش میں قدرتی انداز سے بات کر سکتے ہیں۔\n\nSystem Online. J.A.R.V.I.S. is ready to execute directives via multimodal AI.",
                        toolName = null
                    )
                    repository.insertMessage(welcome)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking initial message", e)
            }
        }
    }

    private fun checkApiKeyStatus() {
        val key = geminiService.resolveApiKey(_customApiKey.value)
        if (key.isBlank()) {
            _apiKeyMissingAlert.value = true
            _statusMessage.value = "API KEY MISSING"
        } else {
            _apiKeyMissingAlert.value = false
            _statusMessage.value = "SYSTEM READY"
        }
    }

    private fun isUrduText(text: String): Boolean {
        return text.any { it in '\u0600'..'\u06FF' || it in '\u0750'..'\u077F' || it in '\uFB50'..'\uFDFF' || it in '\uFE70'..'\uFEFF' }
    }

    fun onInputTextChanged(text: String) {
        _inputText.value = text
    }

    fun clearError() {
        _userError.value = null
        audioRecorderManager.clearError()
    }

    fun onRecordPermissionDenied() {
        _userError.value = "مائیکروفون کی اجازت درکار ہے۔ براہ کرم فون سیٹنگز میں جا کر مائیکروفون کی اجازت فعال کریں۔ (Microphone permission is required for voice commands)"
        _statusMessage.value = "MIC PERMISSION REQUIRED"
    }

    /**
     * Toggles voice recording via AudioRecord.
     * When stopped, sends the raw audio WAV directly to Gemini API.
     */
    fun toggleVoiceRecording() {
        if (audioRecorderManager.isRecording.value) {
            stopVoiceRecording()
        } else {
            startVoiceRecording()
        }
    }

    fun startVoiceRecording() {
        geminiAudioPlayer.stopPlayback()
        val started = audioRecorderManager.startRecording()
        if (started) {
            _statusMessage.value = "RECORDING VOICE DIRECTIVE..."
        } else {
            val err = audioRecorderManager.recorderError.value
            if (err == "RECORD_AUDIO_PERMISSION_DENIED") {
                onRecordPermissionDenied()
            } else {
                _userError.value = "ریکارڈنگ شروع نہیں ہو سکی: $err"
                _statusMessage.value = "RECORDING FAILED"
            }
        }
    }

    fun stopVoiceRecording() {
        val base64Wav = audioRecorderManager.stopRecording()
        if (base64Wav.isNullOrBlank()) {
            _statusMessage.value = "SYSTEM READY"
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            _statusMessage.value = "PROCESSING AUDIO VIA GEMINI..."

            // User turn placeholder
            val userMsg = ChatMessageEntity(
                role = "user",
                content = "🎤 [آڈیو وائس کمانڈ موصول ہوئی]"
            )
            repository.insertMessage(userMsg)

            val recentEntities = repository.getRecentMessages(4)
            val history = recentEntities.map { entity ->
                (if (entity.role == "user") "user" else "model") to entity.content
            }

            try {
                val response = geminiService.generateWithAudioInput(
                    base64WavAudio = base64Wav,
                    history = history,
                    customApiKey = _customApiKey.value.ifBlank { null }
                )

                handleGeminiResponse(response, "[صوتی حکم]", true)
            } catch (e: Exception) {
                Log.e(TAG, "Audio processing exception", e)
                _userError.value = "آڈیو پروسیسنگ میں خرابی پیش آئی: ${e.message}"
                _statusMessage.value = "AUDIO ERROR"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun toggleLiveVoiceSession() {
        liveSessionManager.toggleLiveSession(_customApiKey.value.ifBlank { null })
    }

    fun stopLiveVoiceSession() {
        liveSessionManager.stopLiveSession()
    }

    fun stopAudioPlayback() {
        liveSessionManager.stopLiveSession()
        geminiAudioPlayer.stopPlayback()
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
            geminiAudioPlayer.stopPlayback()
            speechManager.stopSpeaking()
            _isProcessing.value = true
            _statusMessage.value = "ANALYZING DIRECTIVE..."

            val userIsUrdu = isUrduText(promptText)

            // 1. Record user message
            val userMsg = ChatMessageEntity(
                role = "user",
                content = promptText
            )
            repository.insertMessage(userMsg)

            // Check if impossible / OS restricted action
            val impossibleCheck = taskExecutor.checkImpossibleAction(promptText)
            if (impossibleCheck != null) {
                handleExecutionResult(impossibleCheck)
                _isProcessing.value = false
                return@launch
            }

            // Check if API key is missing
            val resolvedKey = geminiService.resolveApiKey(_customApiKey.value)
            if (resolvedKey.isBlank()) {
                val localResult = taskExecutor.executeLocalFallback(promptText, null)
                if (localResult != null) {
                    _statusMessage.value = "EXECUTING LOCAL REDUNDANCY..."
                    handleExecutionResult(localResult)
                } else {
                    val keyMissingMsg = if (userIsUrdu) {
                        "⚠️ API key نہیں ملی، براہ کرم سیٹنگز میں اپنی Gemini API Key درج کریں۔"
                    } else {
                        "⚠️ Gemini API Key not found. Please configure your key in Settings."
                    }
                    val jarvisMsg = ChatMessageEntity(
                        role = "jarvis",
                        content = keyMissingMsg,
                        toolName = "API_KEY_REQUIRED"
                    )
                    repository.insertMessage(jarvisMsg)
                    _userError.value = keyMissingMsg
                    _apiKeyMissingAlert.value = true
                    _statusMessage.value = "API KEY MISSING"
                }
                _isProcessing.value = false
                return@launch
            }

            // 2. Prepare conversation memory
            val recentEntities = repository.getRecentMessages(6)
            val history = recentEntities.map { entity ->
                (if (entity.role == "user") "user" else "model") to entity.content
            }

            // 3. Invoke Gemini
            try {
                val geminiResult = geminiService.generateWithTools(
                    prompt = promptText,
                    history = history,
                    customApiKey = _customApiKey.value.ifBlank { null }
                )

                handleGeminiResponse(geminiResult, promptText, userIsUrdu)
            } catch (e: Exception) {
                Log.e(TAG, "processUserPrompt exception", e)
                val errMsg = if (userIsUrdu) "نیٹ ورک مواصلات میں خرابی پیش آئی: ${e.message}" else "Network error: ${e.message}"
                _userError.value = errMsg
                _statusMessage.value = "COMMUNICATION FAILURE"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private suspend fun handleGeminiResponse(
        geminiResult: GeminiResponse,
        promptText: String,
        userIsUrdu: Boolean
    ) {
        when (geminiResult) {
            is GeminiResponse.FunctionCallResponse -> {
                _statusMessage.value = "EXECUTING [${geminiResult.functionName.uppercase()}]..."
                val execResult = taskExecutor.executeTool(
                    functionName = geminiResult.functionName,
                    arguments = geminiResult.arguments,
                    userPrompt = promptText,
                    customApiKey = _customApiKey.value.ifBlank { null }
                )
                handleExecutionResult(execResult)
            }

            is GeminiResponse.TextResponse -> {
                val jarvisMsg = ChatMessageEntity(
                    role = "jarvis",
                    content = geminiResult.text
                )
                repository.insertMessage(jarvisMsg)

                // If Gemini returned native audio, play it via GeminiAudioPlayer
                if (!geminiResult.audioBase64.isNullOrBlank()) {
                    geminiAudioPlayer.playAudioBase64(
                        geminiResult.audioBase64,
                        geminiResult.audioMimeType ?: "audio/mp3"
                    )
                }
                _statusMessage.value = "DIRECTIVE COMPLETED"
            }

            is GeminiResponse.QuotaExceededResponse -> {
                val quotaDisplay = if (userIsUrdu) {
                    "⚠️ روزانہ مفت حد (Free Daily Quota) مکمل ہو گئی ہے:\n\n" +
                            "گوگل جیمنائی کی مفت روزانہ حد عارضی طور پر مکمل ہو گئی ہے۔ آپ بعد میں دوبارہ کوشش کر سکتے ہیں۔\n" +
                            "یا بغیر کسی حد کے استعمال جاری رکھنے کے لیے سیٹنگز میں اپنی ذاتی Gemini API Key درج فرمائیں۔"
                } else {
                    "⚠️ Daily Free Quota Limit Reached:\n\n" +
                            "The free-tier quota for Gemini has been exhausted. Please retry later, or enter your personal Gemini API key in Settings."
                }
                val jarvisMsg = ChatMessageEntity(
                    role = "jarvis",
                    content = quotaDisplay,
                    toolName = "QUOTA_EXCEEDED"
                )
                repository.insertMessage(jarvisMsg)
                _userError.value = quotaDisplay
                _statusMessage.value = "QUOTA LIMIT EXCEEDED"
            }

            is GeminiResponse.ErrorResponse -> {
                Log.e(TAG, "Gemini Error: ${geminiResult.error}, tech: ${geminiResult.technicalDetail}")

                // Local fallback attempt
                val localResult = taskExecutor.executeLocalFallback(promptText, _customApiKey.value.ifBlank { null })
                if (localResult != null) {
                    _statusMessage.value = "EXECUTING LOCAL REDUNDANCY..."
                    handleExecutionResult(localResult)
                } else {
                    val displayMsg = if (geminiResult.error == "API_KEY_MISSING") {
                        if (userIsUrdu) "API key نہیں ملی، براہ کرم سیٹنگز میں چیک کریں۔" else "Gemini API key missing. Please verify in Settings."
                    } else {
                        if (userIsUrdu) {
                            "مواصلاتی خرابی پیش آئی: ${geminiResult.technicalDetail ?: geminiResult.error}"
                        } else {
                            "Communication error: ${geminiResult.technicalDetail ?: geminiResult.error}"
                        }
                    }

                    val jarvisMsg = ChatMessageEntity(
                        role = "jarvis",
                        content = displayMsg,
                        toolName = "ERROR"
                    )
                    repository.insertMessage(jarvisMsg)
                    _userError.value = displayMsg
                    _statusMessage.value = "DIRECTIVE FAILED"
                }
            }
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
        _statusMessage.value = "MODULE COMPLETE: ${result.moduleName}"
    }

    // Direct Database Actions (for Vault UI)
    fun addDirective(title: String, content: String, type: String = "general", tags: String = "") {
        viewModelScope.launch {
            repository.insertDirective(
                DirectiveItemEntity(
                    title = title,
                    content = content,
                    type = type,
                    tags = tags
                )
            )
        }
    }

    fun toggleDirective(directive: DirectiveItemEntity) {
        viewModelScope.launch {
            repository.toggleDirectiveCompletion(directive.id, !directive.isCompleted)
        }
    }

    fun deleteDirective(id: Long) {
        viewModelScope.launch {
            repository.deleteDirectiveById(id)
        }
    }

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

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            repository.deleteReminderById(id)
        }
    }

    // Settings actions
    fun setCustomApiKey(key: String) {
        _customApiKey.value = key.trim()
        prefs.edit().putString("custom_gemini_key", key.trim()).apply()
        checkApiKeyStatus()
    }

    fun testGeminiConnection() {
        viewModelScope.launch {
            _isTestingConnection.value = true
            _testConnectionResult.value = null
            try {
                val result = geminiService.testConnection(_customApiKey.value.ifBlank { null })
                _testConnectionResult.value = result
            } catch (e: Exception) {
                Log.e(TAG, "testGeminiConnection exception", e)
                _testConnectionResult.value = Pair(false, "خرابی: ${e.message}")
            } finally {
                _isTestingConnection.value = false
            }
        }
    }

    fun setTtsEnabled(enabled: Boolean) {
        _isTtsEnabled.value = enabled
        speechManager.isTtsEnabled = enabled
        prefs.edit().putBoolean("tts_enabled", enabled).apply()
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

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearChatHistory()
            val restartMsg = ChatMessageEntity(
                role = "jarvis",
                content = "چیٹ ہسٹری صاف کر دی گئی ہے۔ تمام سسٹمز نارمل ہیں۔\n\nDirectives cleared. Standing by.",
                toolName = null
            )
            repository.insertMessage(restartMsg)
        }
    }

    override fun onCleared() {
        super.onCleared()
        liveSessionManager.stopLiveSession()
        audioRecorderManager.cancelRecording()
        geminiAudioPlayer.stopPlayback()
    }
}
