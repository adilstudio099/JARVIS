package com.example.audio

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.data.JarvisRepository
import com.example.data.local.ChatMessageEntity
import com.example.data.remote.GeminiResponse
import com.example.data.remote.GeminiService
import com.example.domain.TaskExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

/**
 * State representing the Gemini Live conversational session.
 */
enum class LiveSessionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED_IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR
}

/**
 * Manages real-time live voice sessions for J.A.R.V.I.S.
 *
 * Supports:
 * - Single-tap to open live continuous voice session.
 * - Continuous 16kHz 16-bit Mono PCM audio capture with Voice Activity Detection (VAD).
 * - Real-time speech understanding with gemini-3.5-flash.
 * - Spoken response playback in Urdu via SpeechManager / AudioPlayer.
 * - Real-time conversational memory persistence into Room DB.
 * - Full function calling and Intent execution for real tasks.
 * - Natural barge-in (user speaks while assistant is talking -> stops audio immediately).
 * - 12-second timeout fallback: displays and speaks "کوئی جواب نہیں ملا، دوبارہ کوشش کریں" if no response.
 * - Comprehensive logging on every event to prevent silent failures.
 */
class GeminiLiveSessionManager(
    private val context: Context,
    private val audioRecorder: AudioRecorderManager,
    private val audioPlayer: GeminiAudioPlayer,
    private val geminiService: GeminiService,
    private val taskExecutor: TaskExecutor,
    private val speechManager: SpeechManager? = null,
    private val repository: JarvisRepository? = null
) {
    companion object {
        private const val TAG = "GeminiLiveSession"
        private const val BIDI_WS_URL =
            "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent"

        // Audio threshold for voice activity detection (VAD) and barge-in interruption
        private const val SPEECH_RMS_THRESHOLD = 0.045f
        private const val SILENCE_TIMEOUT_MS = 800L
        private const val RESPONSE_TIMEOUT_MS = 12000L
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var vadJob: Job? = null
    private var activeProcessingJob: Job? = null

    private val _sessionState = MutableStateFlow(LiveSessionState.DISCONNECTED)
    val sessionState: StateFlow<LiveSessionState> = _sessionState.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var isWebSocketConnected = false
    private var activeApiKey: String? = null

    // VAD Buffer for continuous speech capture
    private val speechBuffer = ByteArrayOutputStream()
    private var lastSpeechTimestamp = 0L
    private var hasSpeechOccurred = false

    /**
     * Toggles the live conversational voice session on or off with a single tap.
     */
    fun toggleLiveSession(customApiKey: String? = null) {
        if (_sessionState.value == LiveSessionState.DISCONNECTED || _sessionState.value == LiveSessionState.ERROR) {
            startLiveSession(customApiKey)
        } else {
            stopLiveSession()
        }
    }

    /**
     * Starts continuous live conversational voice session.
     */
    fun startLiveSession(customApiKey: String? = null) {
        Log.i(TAG, "Starting Live Voice Session...")
        if (!audioRecorder.hasRecordPermission()) {
            _errorMessage.value = "براہ کرم مائیکروفون کی اجازت عنایت فرمائیں۔"
            _sessionState.value = LiveSessionState.ERROR
            Log.e(TAG, "RECORD_AUDIO permission missing; cannot start live session.")
            return
        }

        val apiKey = geminiService.resolveApiKey(customApiKey)
        if (apiKey.isBlank()) {
            _errorMessage.value = "Gemini API key درکار ہے۔ براہ کرم سیٹنگز میں چیک کریں۔"
            _sessionState.value = LiveSessionState.ERROR
            Log.e(TAG, "Gemini API key missing; cannot start live session.")
            return
        }

        activeApiKey = customApiKey
        _errorMessage.value = null
        _sessionState.value = LiveSessionState.LISTENING
        _liveTranscript.value = "لائیو سیشن فعال ہے — بولیں..."

        // Initialize Audio Player for 24kHz PCM if received
        audioPlayer.initStreamingTrack(24000)

        // 1. Attach Audio Capture with Voice Activity Detection (VAD)
        startContinuousAudioStream()

        // 2. Attempt WebSocket Live connection in background (with informative diagnostics)
        connectWebSocketIfSupported(apiKey)
    }

    /**
     * Continuously captures 16kHz Mono PCM from microphone with automatic VAD and barge-in.
     */
    private fun startContinuousAudioStream() {
        audioRecorder.startStreaming { chunk, normalizedRms ->
            _rmsLevel.value = normalizedRms

            // Natural Interruption (Barge-in):
            // If user speaks while assistant is talking, immediately silence assistant
            if (normalizedRms > SPEECH_RMS_THRESHOLD) {
                if (_sessionState.value == LiveSessionState.SPEAKING) {
                    Log.i(TAG, "Barge-in detected: User spoke during assistant speech. Interrupting.")
                    speechManager?.stopSpeaking()
                    audioPlayer.interruptAndFlush()
                    activeProcessingJob?.cancel()
                    _sessionState.value = LiveSessionState.LISTENING
                    _liveTranscript.value = "سن رہا ہوں..."
                }
            }

            // Stream chunk to WebSocket if connected
            if (isWebSocketConnected) {
                try {
                    val base64Chunk = Base64.encodeToString(chunk, Base64.NO_WRAP)
                    val realtimeMessage = JSONObject().apply {
                        put("realtimeInput", JSONObject().apply {
                            put("mediaChunks", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("mimeType", "audio/pcm;rate=16000")
                                    put("data", base64Chunk)
                                })
                            })
                        })
                    }
                    webSocket?.send(realtimeMessage.toString())
                } catch (e: Exception) {
                    Log.e(TAG, "Error streaming chunk to WebSocket", e)
                }
            }

            // Real-time VAD buffering
            if (normalizedRms > SPEECH_RMS_THRESHOLD) {
                hasSpeechOccurred = true
                lastSpeechTimestamp = System.currentTimeMillis()
                synchronized(speechBuffer) {
                    speechBuffer.write(chunk)
                }
            } else if (hasSpeechOccurred) {
                // Buffer trailing pause up to timeout
                synchronized(speechBuffer) {
                    speechBuffer.write(chunk)
                }
            }
        }

        // Start VAD utterance detection loop
        vadJob?.cancel()
        vadJob = scope.launch {
            while (isActive) {
                delay(80)
                val now = System.currentTimeMillis()
                if (hasSpeechOccurred && (now - lastSpeechTimestamp > SILENCE_TIMEOUT_MS)) {
                    val capturedPcm = synchronized(speechBuffer) {
                        val bytes = speechBuffer.toByteArray()
                        speechBuffer.reset()
                        bytes
                    }
                    hasSpeechOccurred = false

                    // Minimum 0.4 seconds of audio (16,000 samples/sec * 2 bytes = 32,000 Bps -> ~12,800 bytes)
                    if (capturedPcm.size > 12000) {
                        Log.i(TAG, "Speech utterance finished. Captured ${capturedPcm.size} bytes PCM. Disagreeing silence.")
                        dispatchVoiceUtterance(capturedPcm)
                    }
                }
            }
        }
    }

    /**
     * Processes captured speech utterance through gemini-3.5-flash with timeout and speech playback.
     */
    private fun dispatchVoiceUtterance(pcmData: ByteArray) {
        if (_sessionState.value == LiveSessionState.SPEAKING) return

        activeProcessingJob?.cancel()
        activeProcessingJob = scope.launch {
            _sessionState.value = LiveSessionState.THINKING
            _liveTranscript.value = "پروسیسنگ ہو رہی ہے..."

            // Log utterance start
            val startTime = System.currentTimeMillis()
            Log.i(TAG, "Dispatching voice utterance to Gemini (${pcmData.size} bytes PCM)...")

            // Wrap in 12-second timeout fallback
            val outcome = withTimeoutOrNull(RESPONSE_TIMEOUT_MS) {
                try {
                    // Create standard 16kHz 16-bit Mono WAV container
                    val wavBytes = audioRecorder.createWavFile(pcmData, 16000, 1, 16)
                    val base64Wav = Base64.encodeToString(wavBytes, Base64.NO_WRAP)

                    // User Turn placeholder in conversation history
                    repository?.insertMessage(
                        ChatMessageEntity(
                            role = "user",
                            content = "🎤 [صوتی حکم / Spoken Directive]"
                        )
                    )

                    // Recent history for multi-turn context
                    val recentEntities = repository?.getRecentMessages(4) ?: emptyList()
                    val history = recentEntities.map { entity ->
                        (if (entity.role == "user") "user" else "model") to entity.content
                    }

                    // Call Gemini with audio input
                    val response = geminiService.generateWithAudioInput(
                        base64WavAudio = base64Wav,
                        history = history,
                        customApiKey = activeApiKey
                    )

                    val elapsed = System.currentTimeMillis() - startTime
                    Log.i(TAG, "Gemini responded in ${elapsed}ms: ${response.javaClass.simpleName}")

                    handleGeminiLiveResponse(response)
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Exception during voice processing", e)
                    false
                }
            }

            // Handle timeout fallback
            if (outcome == null) {
                Log.w(TAG, "Response timed out after ${RESPONSE_TIMEOUT_MS}ms.")
                val timeoutMsg = "کوئی جواب نہیں ملا، دوبارہ کوشش کریں۔"
                _liveTranscript.value = timeoutMsg
                _sessionState.value = LiveSessionState.SPEAKING

                repository?.insertMessage(
                    ChatMessageEntity(
                        role = "jarvis",
                        content = timeoutMsg,
                        toolName = "TIMEOUT_FALLBACK"
                    )
                )

                speechManager?.speak(timeoutMsg)
                delay(2400)
                if (_sessionState.value != LiveSessionState.DISCONNECTED) {
                    _sessionState.value = LiveSessionState.LISTENING
                    _liveTranscript.value = "لائیو سیشن فعال ہے — بولیں..."
                }
            }
        }
    }

    private suspend fun handleGeminiLiveResponse(response: GeminiResponse) {
        when (response) {
            is GeminiResponse.TextResponse -> {
                Log.i(TAG, "TextResponse received: ${response.text.take(60)}...")
                _liveTranscript.value = response.text
                _sessionState.value = LiveSessionState.SPEAKING

                // Record in Room Database
                repository?.insertMessage(
                    ChatMessageEntity(
                        role = "jarvis",
                        content = response.text
                    )
                )

                // Vocalize response
                if (!response.audioBase64.isNullOrBlank()) {
                    Log.i(TAG, "Playing Gemini returned native audio.")
                    audioPlayer.playAudioBase64(response.audioBase64, response.audioMimeType ?: "audio/mp3")
                } else if (speechManager != null) {
                    Log.i(TAG, "Vocalizing Urdu response via SpeechManager.")
                    speechManager.speak(response.text)
                }

                // Await speech completion before returning to listening
                val estimatedSpeechMs = min(9000L, max(2200L, response.text.length * 60L))
                delay(estimatedSpeechMs)

                if (_sessionState.value != LiveSessionState.DISCONNECTED) {
                    _sessionState.value = LiveSessionState.LISTENING
                    _liveTranscript.value = "لائیو سیشن فعال ہے — بولیں..."
                }
            }

            is GeminiResponse.FunctionCallResponse -> {
                Log.i(TAG, "FunctionCallResponse: ${response.functionName} with args ${response.arguments}")
                _sessionState.value = LiveSessionState.THINKING
                _liveTranscript.value = "حکم کی تعمیل: [${response.functionName.uppercase()}]..."

                val result = taskExecutor.executeTool(
                    functionName = response.functionName,
                    arguments = response.arguments,
                    userPrompt = "[صوتی حکم]",
                    customApiKey = activeApiKey
                )

                repository?.insertMessage(
                    ChatMessageEntity(
                        role = "jarvis",
                        content = result.displayContent,
                        toolName = result.moduleName,
                        toolDataJson = result.rawJson
                    )
                )

                _liveTranscript.value = result.spokenResponse
                _sessionState.value = LiveSessionState.SPEAKING

                if (speechManager != null) {
                    speechManager.speak(result.spokenResponse)
                }

                val waitMs = min(7000L, max(2000L, result.spokenResponse.length * 55L))
                delay(waitMs)

                if (_sessionState.value != LiveSessionState.DISCONNECTED) {
                    _sessionState.value = LiveSessionState.LISTENING
                    _liveTranscript.value = "لائیو سیشن فعال ہے — بولیں..."
                }
            }

            is GeminiResponse.QuotaExceededResponse -> {
                Log.w(TAG, "Quota limit exceeded during live session.")
                val quotaMsg = "روزانہ کی حد ختم ہو گئی ہے، براہ کرم تھوڑی دیر بعد دوبارہ کوشش کریں۔"
                _liveTranscript.value = quotaMsg
                _errorMessage.value = quotaMsg
                _sessionState.value = LiveSessionState.ERROR

                repository?.insertMessage(
                    ChatMessageEntity(
                        role = "jarvis",
                        content = quotaMsg,
                        toolName = "QUOTA_EXCEEDED"
                    )
                )

                speechManager?.speak(quotaMsg)
            }

            is GeminiResponse.ErrorResponse -> {
                Log.e(TAG, "ErrorResponse: ${response.error} - ${response.technicalDetail}")
                val errMsg = "معذرت، رابطہ نہیں ہو سکا۔ دوبارہ ارشاد فرمائیں۔"
                _liveTranscript.value = errMsg
                _sessionState.value = LiveSessionState.SPEAKING

                speechManager?.speak(errMsg)
                delay(2000)

                if (_sessionState.value != LiveSessionState.DISCONNECTED) {
                    _sessionState.value = LiveSessionState.LISTENING
                    _liveTranscript.value = "لائیو سیشن فعال ہے — بولیں..."
                }
            }
        }
    }

    /**
     * Connects to WebSocket if supported by the model, attaching all listeners beforehand.
     */
    private fun connectWebSocketIfSupported(apiKey: String) {
        try {
            Log.i(TAG, "Attempting Live WebSocket connection with model: ${GeminiService.DEFAULT_MODEL}")
            val request = Request.Builder()
                .url("$BIDI_WS_URL?key=$apiKey")
                .build()

            // Pre-attach listener before starting stream
            webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.i(TAG, "Gemini Live WebSocket connection established. Sending setup message.")
                    isWebSocketConnected = true
                    sendLiveSetupMessage(webSocket)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    Log.i(TAG, "WebSocket server event received: ${text.take(100)}")
                    handleServerMessage(text)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.w(TAG, "Live WebSocket notification: ${t.message} (HTTP ${response?.code}). Note: Model ${GeminiService.DEFAULT_MODEL} operates seamlessly via real-time VAD voice engine.")
                    isWebSocketConnected = false
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.i(TAG, "WebSocket closed ($code / $reason). Seamless VAD voice engine remains active.")
                    isWebSocketConnected = false
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "WebSocket initialization caught exception", e)
            isWebSocketConnected = false
        }
    }

    private fun sendLiveSetupMessage(ws: WebSocket) {
        try {
            val setupJson = JSONObject().apply {
                put("setup", JSONObject().apply {
                    put("model", "models/${GeminiService.DEFAULT_MODEL}")
                    put("generationConfig", JSONObject().apply {
                        put("responseModalities", JSONArray().apply {
                            put("AUDIO")
                        })
                        put("speechConfig", JSONObject().apply {
                            put("voiceConfig", JSONObject().apply {
                                put("prebuiltVoiceConfig", JSONObject().apply {
                                    put("voiceName", "Puck")
                                })
                            })
                        })
                    })
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put(
                                    "text",
                                    "You are J.A.R.V.I.S., a real-time conversational AI assistant. " +
                                            "Always respond naturally in fluent, grammatically correct Urdu script or spoken Urdu. " +
                                            "You understand both Urdu and English audio directly. " +
                                            "Keep replies concise, conversational, and direct for live voice exchange."
                                )
                            })
                        })
                    })
                })
            }
            ws.send(setupJson.toString())
            Log.i(TAG, "Sent Live setup message to WebSocket server.")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending WebSocket setup", e)
        }
    }

    private fun handleServerMessage(messageText: String) {
        try {
            val root = JSONObject(messageText)

            if (root.has("serverContent")) {
                val serverContent = root.getJSONObject("serverContent")

                if (serverContent.optBoolean("interrupted", false)) {
                    Log.i(TAG, "Server signaled speech interruption.")
                    speechManager?.stopSpeaking()
                    audioPlayer.interruptAndFlush()
                    _sessionState.value = LiveSessionState.LISTENING
                    return
                }

                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.optJSONArray("parts") ?: JSONArray()

                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)

                        if (part.has("text")) {
                            val text = part.getString("text")
                            _liveTranscript.value = text
                            repository?.let { repo ->
                                scope.launch {
                                    repo.insertMessage(ChatMessageEntity(role = "jarvis", content = text))
                                }
                            }
                        }

                        if (part.has("inlineData")) {
                            val inlineData = part.getJSONObject("inlineData")
                            val data = inlineData.optString("data", "")
                            if (data.isNotEmpty()) {
                                _sessionState.value = LiveSessionState.SPEAKING
                                val audioBytes = Base64.decode(data, Base64.DEFAULT)
                                audioPlayer.writeStreamingPcmChunk(audioBytes)
                            }
                        }
                    }
                }

                if (serverContent.optBoolean("turnComplete", false)) {
                    Log.i(TAG, "Server turnComplete received.")
                    _sessionState.value = LiveSessionState.LISTENING
                    _liveTranscript.value = "لائیو سیشن فعال ہے — بولیں..."
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing server WebSocket message", e)
        }
    }

    /**
     * Cleanly terminates the live conversational session.
     */
    fun stopLiveSession() {
        Log.i(TAG, "Stopping Live Voice Session cleanly.")
        _sessionState.value = LiveSessionState.DISCONNECTED
        _rmsLevel.value = 0f
        _liveTranscript.value = ""
        isWebSocketConnected = false
        hasSpeechOccurred = false

        vadJob?.cancel()
        vadJob = null

        activeProcessingJob?.cancel()
        activeProcessingJob = null

        audioRecorder.stopStreaming()
        audioPlayer.stopPlayback()
        speechManager?.stopSpeaking()

        try {
            webSocket?.close(1000, "User ended session")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing WebSocket", e)
        }
        webSocket = null
        Log.i(TAG, "Live voice session stopped.")
    }
}
