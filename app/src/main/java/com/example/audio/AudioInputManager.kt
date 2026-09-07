package com.example.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class AudioInputManager(private val context: Context) {

    companion object {
        const val SAMPLE_RATE = 16000 // 16kHz standard for Gemini Live input
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    fun startRecording(
        scope: CoroutineScope,
        onAudioChunk: (ByteArray) -> Unit,
        onError: (String) -> Unit
    ) {
        if (_isRecording.value) return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            onError("Microphone permission granted nahi hai.")
            return
        }

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )

        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            onError("Audio hardware buffer error.")
            return
        }

        val bufferSize = maxOf(minBufferSize, 2048)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                onError("AudioRecord initialization failed.")
                return
            }

            audioRecord?.startRecording()
            _isRecording.value = true

            recordingJob = scope.launch(Dispatchers.IO) {
                val buffer = ByteArray(2048)
                while (isActive && _isRecording.value) {
                    val readCount = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (readCount > 0) {
                        // Calculate RMS for visualizer waveform
                        val rms = calculateRMS(buffer, readCount)
                        val normalized = (rms / 32767f).coerceIn(0f, 1f)
                        _audioAmplitude.value = normalized

                        val chunk = buffer.copyOf(readCount)
                        onAudioChunk(chunk)
                    } else if (readCount < 0) {
                        break
                    }
                }
            }
        } catch (e: SecurityException) {
            onError("Microphone security restriction: ${e.localizedMessage}")
            stopRecording()
        } catch (e: Exception) {
            onError("Microphone error: ${e.localizedMessage}")
            stopRecording()
        }
    }

    fun stopRecording() {
        _isRecording.value = false
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignore during cleanup
        } finally {
            audioRecord = null
            _audioAmplitude.value = 0f
        }
    }

    private fun calculateRMS(buffer: ByteArray, length: Int): Float {
        var sum = 0.0
        var i = 0
        while (i < length - 1) {
            // Little-endian 16-bit PCM
            val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
            val signed = sample.toShort()
            sum += signed * signed
            i += 2
        }
        val count = length / 2
        return if (count > 0) sqrt(sum / count).toFloat() else 0f
    }
}
