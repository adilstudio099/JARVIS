package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * Audio player dedicated to playing Gemini API generated audio/TTS responses.
 * Plays either raw PCM (via AudioTrack) or encoded audio (WAV/MP3 via MediaPlayer).
 * Replaces Android's native TextToSpeech engine.
 */
class GeminiAudioPlayer(private val context: Context) {

    companion object {
        private const val TAG = "GeminiAudioPlayer"
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var mediaPlayer: MediaPlayer? = null
    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    @Synchronized
    fun playAudioBase64(base64Audio: String, mimeType: String = "audio/mp3"): Boolean {
        stopPlayback()

        return try {
            val audioBytes = Base64.decode(base64Audio, Base64.DEFAULT)
            if (audioBytes.isEmpty()) {
                Log.w(TAG, "Empty audio bytes received")
                return false
            }

            Log.i(TAG, "Playing Gemini audio of length ${audioBytes.size} bytes, mimeType=$mimeType")

            if (mimeType.contains("pcm", ignoreCase = true) || mimeType.contains("rate=24000", ignoreCase = true)) {
                playPcm24kHz(audioBytes)
            } else {
                playWithMediaPlayer(audioBytes, mimeType)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio", e)
            _isPlaying.value = false
            false
        }
    }

    private fun playWithMediaPlayer(audioBytes: ByteArray, mimeType: String) {
        playbackJob = scope.launch {
            try {
                val extension = when {
                    mimeType.contains("wav", ignoreCase = true) -> ".wav"
                    mimeType.contains("ogg", ignoreCase = true) -> ".ogg"
                    mimeType.contains("aac", ignoreCase = true) -> ".aac"
                    else -> ".mp3"
                }

                val tempFile = File.createTempFile("gemini_tts_", extension, context.cacheDir)
                FileOutputStream(tempFile).use { it.write(audioBytes) }

                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                            .build()
                    )
                    setDataSource(tempFile.absolutePath)
                    prepare()
                    setOnCompletionListener {
                        _isPlaying.value = false
                        it.release()
                        mediaPlayer = null
                        tempFile.delete()
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                        _isPlaying.value = false
                        tempFile.delete()
                        true
                    }
                }

                synchronized(this@GeminiAudioPlayer) {
                    mediaPlayer = player
                }

                _isPlaying.value = true
                player.start()
            } catch (e: Exception) {
                Log.e(TAG, "MediaPlayer playback error", e)
                _isPlaying.value = false
            }
        }
    }

    private fun playPcm24kHz(pcmBytes: ByteArray) {
        playbackJob = scope.launch {
            try {
                val sampleRate = 24000
                val channelConfig = AudioFormat.CHANNEL_OUT_MONO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(audioFormat)
                            .setSampleRate(sampleRate)
                            .setChannelMask(channelConfig)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufferSize.coerceAtLeast(pcmBytes.size))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                synchronized(this@GeminiAudioPlayer) {
                    audioTrack = track
                }

                _isPlaying.value = true
                track.play()
                track.write(pcmBytes, 0, pcmBytes.size)

                // Wait until played
                track.stop()
                track.release()
                audioTrack = null
                _isPlaying.value = false
            } catch (e: Exception) {
                Log.e(TAG, "AudioTrack playback error", e)
                _isPlaying.value = false
            }
        }
    }

    private var streamingAudioTrack: AudioTrack? = null

    /**
     * Initializes AudioTrack for near real-time streaming playback of 24kHz 16-bit Mono PCM.
     */
    @Synchronized
    fun initStreamingTrack(sampleRate: Int = 24000) {
        stopStreamingTrack()
        try {
            val channelConfig = AudioFormat.CHANNEL_OUT_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(8192)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(minBufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            streamingAudioTrack = track
            track.play()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init streaming AudioTrack", e)
        }
    }

    /**
     * Plays a small streamed PCM chunk in near real-time as it arrives from the Live API.
     */
    @Synchronized
    fun writeStreamingPcmChunk(pcmBytes: ByteArray) {
        if (streamingAudioTrack == null) {
            initStreamingTrack(24000)
        }
        try {
            _isPlaying.value = true
            streamingAudioTrack?.write(pcmBytes, 0, pcmBytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error writing streaming chunk to AudioTrack", e)
        }
    }

    /**
     * Natural Interruption (Barge-in):
     * Immediately pauses, flushes, and stops playback if the user interrupts or starts speaking.
     */
    @Synchronized
    fun interruptAndFlush() {
        try {
            streamingAudioTrack?.let {
                it.pause()
                it.flush()
                it.play() // Ready for next turn
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error flushing AudioTrack on interruption", e)
        }
        stopPlayback()
    }

    @Synchronized
    fun stopStreamingTrack() {
        try {
            streamingAudioTrack?.let {
                it.pause()
                it.flush()
                it.stop()
                it.release()
            }
        } catch (e: Exception) {
            // Ignore
        }
        streamingAudioTrack = null
        _isPlaying.value = false
    }

    @Synchronized
    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        stopStreamingTrack()

        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            // Ignore
        }
        mediaPlayer = null

        try {
            audioTrack?.let {
                it.stop()
                it.release()
            }
        } catch (e: Exception) {
            // Ignore
        }
        audioTrack = null

        _isPlaying.value = false
    }
}
