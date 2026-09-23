package com.example.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * Manages raw audio recording via Android's AudioRecord.
 * Captures 16kHz, 16-bit Mono PCM audio, emits real-time RMS audio levels for UI visualization,
 * and compiles the recording into a standard RIFF/WAV Base64-encoded string for Gemini Multimodal Audio API.
 */
class AudioRecorderManager(private val context: Context) {

    companion object {
        private const val TAG = "AudioRecorderManager"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _recorderError = MutableStateFlow<String?>(null)
    val recorderError: StateFlow<String?> = _recorderError.asStateFlow()

    private val pcmOutputStream = ByteArrayOutputStream()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private var streamingJob: Job? = null
    private var streamingChunkCallback: ((ByteArray, Float) -> Unit)? = null

    /**
     * Starts continuous live audio streaming in small real-time PCM chunks (100ms at 16kHz).
     * Invokes [onChunk] with the raw 16-bit Mono Little-Endian PCM byte array and the normalized RMS level.
     * If streaming is already active, safely updates the destination callback without restarting the microphone.
     */
    @Synchronized
    fun startStreaming(onChunk: (ByteArray, Float) -> Unit): Boolean {
        streamingChunkCallback = onChunk

        if (!hasRecordPermission()) {
            _recorderError.value = "RECORD_AUDIO_PERMISSION_DENIED"
            Log.e(TAG, "Cannot start streaming: RECORD_AUDIO permission not granted")
            return false
        }

        if (_isStreaming.value && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            Log.i(TAG, "Streaming already active; updated chunk callback.")
            return true
        }

        try {
            val chunkSamples = 1600 // 100ms at 16kHz
            val chunkBytes = chunkSamples * 2
            val minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            ).coerceAtLeast(chunkBytes * 2)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                minBufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed for streaming")
                _recorderError.value = "AUDIO_INIT_FAILED"
                audioRecord?.release()
                audioRecord = null
                return false
            }

            audioRecord?.startRecording()
            _isStreaming.value = true
            _recorderError.value = null

            streamingJob = scope.launch {
                val shortBuffer = ShortArray(chunkSamples)
                val byteBuffer = ByteBuffer.allocate(chunkBytes).order(ByteOrder.LITTLE_ENDIAN)

                while (isActive && _isStreaming.value) {
                    val readShorts = audioRecord?.read(shortBuffer, 0, chunkSamples) ?: 0
                    if (readShorts > 0) {
                        byteBuffer.clear()
                        var sum = 0.0
                        for (i in 0 until readShorts) {
                            val sample = shortBuffer[i]
                            byteBuffer.putShort(sample)
                            sum += sample * sample
                        }
                        val rms = sqrt(sum / readShorts)
                        val normalizedRms = (rms / 32768.0f).toFloat().coerceIn(0f, 1f)
                        _rmsLevel.value = normalizedRms

                        val chunkArray = ByteArray(readShorts * 2)
                        System.arraycopy(byteBuffer.array(), 0, chunkArray, 0, readShorts * 2)
                        streamingChunkCallback?.invoke(chunkArray, normalizedRms)
                    }
                }
            }

            Log.i(TAG, "Continuous live audio streaming started at $SAMPLE_RATE Hz")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start live streaming", e)
            _recorderError.value = e.message ?: "Failed to start live audio streaming"
            stopStreaming()
            return false
        }
    }

    @Synchronized
    fun stopStreaming() {
        _isStreaming.value = false
        _rmsLevel.value = 0f
        streamingChunkCallback = null
        streamingJob?.cancel()
        streamingJob = null

        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            // Ignore
        }

        try {
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignore
        }
        audioRecord = null
        Log.i(TAG, "Continuous live audio streaming stopped")
    }

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Starts recording audio using AudioRecord.
     * Returns true if recording started successfully, false if permission missing or initialization failed.
     */
    @Synchronized
    fun startRecording(): Boolean {
        if (!hasRecordPermission()) {
            _recorderError.value = "RECORD_AUDIO_PERMISSION_DENIED"
            Log.e(TAG, "Cannot start recording: RECORD_AUDIO permission not granted")
            return false
        }

        if (_isRecording.value) {
            Log.w(TAG, "startRecording called while already recording")
            return true
        }

        try {
            val minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            ).coerceAtLeast(4096)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                minBufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed")
                _recorderError.value = "AUDIO_INIT_FAILED"
                audioRecord?.release()
                audioRecord = null
                return false
            }

            pcmOutputStream.reset()
            audioRecord?.startRecording()
            _isRecording.value = true
            _recorderError.value = null

            recordingJob = scope.launch {
                val buffer = ShortArray(minBufferSize / 2)
                val byteBuffer = ByteBuffer.allocate(buffer.size * 2).order(ByteOrder.LITTLE_ENDIAN)

                while (isActive && _isRecording.value) {
                    val readShorts = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readShorts > 0) {
                        // Calculate RMS for HUD visualizer
                        var sum = 0.0
                        byteBuffer.clear()
                        for (i in 0 until readShorts) {
                            val sample = buffer[i]
                            byteBuffer.putShort(sample)
                            sum += sample * sample
                        }
                        val rms = sqrt(sum / readShorts)
                        // Normalize RMS roughly to 0.0 .. 1.0
                        val normalizedRms = (rms / 32768.0f).toFloat().coerceIn(0f, 1f)
                        _rmsLevel.value = normalizedRms

                        synchronized(pcmOutputStream) {
                            pcmOutputStream.write(byteBuffer.array(), 0, readShorts * 2)
                        }
                    }
                }
            }

            Log.i(TAG, "AudioRecord started successfully at $SAMPLE_RATE Hz")
            return true
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException starting AudioRecord", e)
            _recorderError.value = "RECORD_AUDIO_PERMISSION_DENIED"
            cleanup()
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting AudioRecord", e)
            _recorderError.value = e.message ?: "Failed to start audio recording"
            cleanup()
            return false
        }
    }

    /**
     * Stops recording and returns the complete audio as a Base64-encoded WAV string.
     * Returns null if no audio was captured or recording was not active.
     */
    @Synchronized
    fun stopRecording(): String? {
        if (!_isRecording.value && pcmOutputStream.size() == 0) {
            return null
        }

        _isRecording.value = false
        _rmsLevel.value = 0f

        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        }

        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioRecord", e)
        }
        audioRecord = null

        val pcmData = synchronized(pcmOutputStream) {
            val bytes = pcmOutputStream.toByteArray()
            pcmOutputStream.reset()
            bytes
        }

        if (pcmData.isEmpty()) {
            Log.w(TAG, "No PCM data captured")
            return null
        }

        // Build standard WAV container around PCM bytes
        val wavBytes = createWavFile(pcmData, SAMPLE_RATE, 1, 16)
        val base64Wav = Base64.encodeToString(wavBytes, Base64.NO_WRAP)
        Log.i(TAG, "Audio recorded: ${pcmData.size} bytes PCM -> ${wavBytes.size} bytes WAV -> Base64 len: ${base64Wav.length}")
        return base64Wav
    }

    fun cancelRecording() {
        _isRecording.value = false
        _rmsLevel.value = 0f
        cleanup()
    }

    fun clearError() {
        _recorderError.value = null
    }

    private fun cleanup() {
        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            // Ignore
        }
        try {
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignore
        }
        audioRecord = null
        recordingJob?.cancel()
        recordingJob = null
        synchronized(pcmOutputStream) {
            pcmOutputStream.reset()
        }
    }

    /**
     * Constructs a standard 44-byte RIFF/WAV header around raw 16-bit PCM bytes.
     */
    fun createWavFile(pcmData: ByteArray, sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
        val totalAudioLen = pcmData.size.toLong()
        val totalDataLen = totalAudioLen + 36
        val byteRate = (sampleRate * channels * bitsPerSample / 8).toLong()

        val header = ByteArray(44)

        // RIFF chunk descriptor
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()

        // "fmt " sub-chunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // Subchunk1Size for PCM
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // AudioFormat 1 = PCM (linear quantization)
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * bitsPerSample / 8).toByte() // Block align
        header[33] = 0
        header[34] = bitsPerSample.toByte()
        header[35] = 0

        // "data" sub-chunk
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        val wavFile = ByteArray(44 + pcmData.size)
        System.arraycopy(header, 0, wavFile, 0, 44)
        System.arraycopy(pcmData, 0, wavFile, 44, pcmData.size)
        return wavFile
    }
}
