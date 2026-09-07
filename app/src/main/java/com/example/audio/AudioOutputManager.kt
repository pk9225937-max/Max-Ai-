package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class AudioOutputManager(private val context: Context) {

    companion object {
        const val OUTPUT_SAMPLE_RATE = 24000 // Gemini Live outputs 24kHz PCM16 mono
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    private val trackLock = Any()
    private var audioTrack: AudioTrack? = null
    private val audioQueue = Channel<ByteArray>(Channel.UNLIMITED)
    private var playbackJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _outputAmplitude = MutableStateFlow(0f)
    val outputAmplitude: StateFlow<Float> = _outputAmplitude.asStateFlow()

    fun initPlayer(scope: CoroutineScope) {
        synchronized(trackLock) {
            if (audioTrack != null) return

            val minBufferSize = AudioTrack.getMinBufferSize(
                OUTPUT_SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            )
            val bufferSize = maxOf(minBufferSize * 2, 4096)

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val format = AudioFormat.Builder()
                .setSampleRate(OUTPUT_SAMPLE_RATE)
                .setChannelMask(CHANNEL_CONFIG)
                .setEncoding(AUDIO_FORMAT)
                .build()

            try {
                audioTrack = AudioTrack(
                    attributes,
                    format,
                    bufferSize,
                    AudioTrack.MODE_STREAM,
                    AudioManager.AUDIO_SESSION_ID_GENERATE
                )
                audioTrack?.play()
                MaxVoiceLogger.i("AudioTrack initialized at ${OUTPUT_SAMPLE_RATE}Hz mono PCM")
            } catch (e: Exception) {
                MaxVoiceLogger.e("AudioTrack initialization failed: ${e.message}", e)
            }

            playbackJob = scope.launch(Dispatchers.IO) {
                for (chunk in audioQueue) {
                    if (!isActive) break
                    _isPlaying.value = true
                    val rms = calculateRMS(chunk, chunk.size)
                    _outputAmplitude.value = (rms / 32767f).coerceIn(0f, 1f)

                    try {
                        val track = audioTrack
                        if (track != null && track.state == AudioTrack.STATE_INITIALIZED) {
                            if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                                track.play()
                            }
                            track.write(chunk, 0, chunk.size)
                        }
                    } catch (e: Exception) {
                        MaxVoiceLogger.w("AudioTrack write error: ${e.message}")
                        break
                    }
                }
                _isPlaying.value = false
                _outputAmplitude.value = 0f
            }
        }
    }

    fun enqueueAudio(chunk: ByteArray) {
        requestAudioFocus()
        MaxVoiceLogger.d("Enqueuing ${chunk.size} bytes of native audio for playback")
        audioQueue.trySend(chunk)
    }

    /**
     * Requirement 9: Stop playback immediately upon user interruption or barge-in.
     */
    fun stopPlayback() {
        // Drain any unplayed audio chunks
        var drainedCount = 0
        while (audioQueue.tryReceive().isSuccess) {
            drainedCount++
        }
        if (drainedCount > 0) {
            MaxVoiceLogger.d("Drained $drainedCount pending audio chunks for interruption")
        }

        try {
            audioTrack?.let { track ->
                if (track.state == AudioTrack.STATE_INITIALIZED) {
                    track.pause()
                    track.flush()
                }
            }
        } catch (e: Exception) {
            MaxVoiceLogger.w("Exception stopping/flushing AudioTrack: ${e.message}")
        }

        _isPlaying.value = false
        _outputAmplitude.value = 0f
        abandonAudioFocus()
    }

    fun release() {
        stopPlayback()
        playbackJob?.cancel()
        playbackJob = null
        synchronized(trackLock) {
            try {
                audioTrack?.stop()
                audioTrack?.release()
                MaxVoiceLogger.d("AudioTrack released")
            } catch (e: Exception) {
                MaxVoiceLogger.w("Exception releasing AudioTrack: ${e.message}")
            } finally {
                audioTrack = null
            }
        }
    }

    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .build()
                audioManager?.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager?.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        } catch (e: Exception) {
            MaxVoiceLogger.w("Failed to request audio focus: ${e.message}")
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun calculateRMS(buffer: ByteArray, length: Int): Float {
        var sum = 0.0
        var i = 0
        while (i < length - 1) {
            val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
            val signed = sample.toShort()
            sum += signed * signed
            i += 2
        }
        val count = length / 2
        return if (count > 0) sqrt(sum / count).toFloat() else 0f
    }
}
