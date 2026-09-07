package com.example.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

data class MicrophoneTestResult(
    val success: Boolean,
    val message: String,
    val peakAmplitudePercentage: Int = 0,
    val bytesCaptured: Int = 0
)

class AudioInputManager(private val context: Context) {

    companion object {
        const val SAMPLE_RATE = 16000 // 16kHz standard for Gemini Live API
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val CHUNK_SIZE_BYTES = 2048 // 1024 16-bit PCM samples = 64ms chunk
        private const val INTERRUPTION_ENERGY_THRESHOLD = 0.08f // Speech energy threshold for barge-in
    }

    private val recorderLock = Any()
    private var audioRecord: AudioRecord? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var recordingJob: Job? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    /**
     * Checks if microphone permission is granted.
     */
    fun hasMicrophonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if microphone hardware is present on the device.
     */
    fun hasMicrophoneHardware(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
    }

    /**
     * Starts continuous PCM microphone capture using Android AudioRecord.
     * Prevents duplicate recorders and validates all hardware/permission requirements first.
     */
    fun startRecording(
        scope: CoroutineScope,
        isOutputPlaying: () -> Boolean,
        onInterruption: () -> Unit,
        onAudioChunk: (ByteArray) -> Unit,
        onStarted: () -> Unit,
        onError: (String) -> Unit
    ) {
        synchronized(recorderLock) {
            // Requirement 13: Stop/release any previous recorder before creating a new one
            stopRecordingInternal()

            MaxVoiceLogger.i("Starting AudioRecord microphone capture...")

            // 1. Permission check
            if (!hasMicrophonePermission()) {
                MaxVoiceLogger.e("Microphone permission RECORD_AUDIO not granted")
                onError("Microphone permission not granted. Please enable microphone permission in Settings.")
                return
            }

            // 2. Hardware check
            if (!hasMicrophoneHardware()) {
                MaxVoiceLogger.e("Microphone hardware feature not found on device")
                onError("Microphone hardware is not available on this device.")
                return
            }

            // 3. Buffer calculation
            val minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            )

            if (minBufferSize <= 0 || minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                MaxVoiceLogger.e("AudioRecord.getMinBufferSize returned error code: $minBufferSize")
                onError("Microphone hardware buffer configuration failed (Error code: $minBufferSize).")
                return
            }

            val bufferSize = maxOf(minBufferSize * 2, CHUNK_SIZE_BYTES * 2)
            MaxVoiceLogger.d("AudioRecord buffer size calculated: $bufferSize bytes (minBufferSize: $minBufferSize)")

            // 4. Initialize AudioRecord (prefer VOICE_COMMUNICATION for hardware echo cancellation, fallback to MIC)
            var recorder: AudioRecord? = null
            var sourceUsed = MediaRecorder.AudioSource.VOICE_COMMUNICATION

            try {
                recorder = AudioRecord(
                    sourceUsed,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )
                if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                    recorder.release()
                    recorder = null
                }
            } catch (e: Exception) {
                MaxVoiceLogger.w("VOICE_COMMUNICATION source failed: ${e.message}. Trying standard MIC source...")
                recorder?.release()
                recorder = null
            }

            if (recorder == null) {
                sourceUsed = MediaRecorder.AudioSource.MIC
                try {
                    recorder = AudioRecord(
                        sourceUsed,
                        SAMPLE_RATE,
                        CHANNEL_CONFIG,
                        AUDIO_FORMAT,
                        bufferSize
                    )
                } catch (e: Exception) {
                    MaxVoiceLogger.e("AudioRecord creation failed with MIC source: ${e.message}", e)
                    onError("Microphone unavailable. Please check microphone permission and make sure another app is not using the microphone.")
                    return
                }
            }

            // 5. Check initialization state
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                MaxVoiceLogger.e("AudioRecord failed to initialize (State: ${recorder.state})")
                recorder.release()
                onError("Microphone unavailable. Please check microphone permission and make sure another app is not using the microphone.")
                return
            }

            // 6. Attach hardware audio effects if available
            try {
                val sessionId = recorder.audioSessionId
                if (AcousticEchoCanceler.isAvailable()) {
                    echoCanceler = AcousticEchoCanceler.create(sessionId)?.apply {
                        enabled = true
                        MaxVoiceLogger.d("AcousticEchoCanceler enabled on session $sessionId")
                    }
                }
                if (NoiseSuppressor.isAvailable()) {
                    noiseSuppressor = NoiseSuppressor.create(sessionId)?.apply {
                        enabled = true
                        MaxVoiceLogger.d("NoiseSuppressor enabled on session $sessionId")
                    }
                }
            } catch (e: Exception) {
                MaxVoiceLogger.w("Could not initialize audio effects: ${e.message}")
            }

            // 7. Start recording on hardware
            try {
                recorder.startRecording()
                if (recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                    MaxVoiceLogger.e("AudioRecord.startRecording failed (RecordingState: ${recorder.recordingState})")
                    recorder.release()
                    onError("Microphone is currently in use by another application or phone call.")
                    return
                }
            } catch (e: Exception) {
                MaxVoiceLogger.e("Exception during AudioRecord.startRecording: ${e.message}", e)
                recorder.release()
                onError("Microphone conflict. Please close any background recording app and try again.")
                return
            }

            audioRecord = recorder
            _isRecording.value = true
            MaxVoiceLogger.i("AudioRecord successfully initialized and recording started (source: $sourceUsed, sampleRate: $SAMPLE_RATE)")

            // Notify caller that recording has started BEFORE UI enters LISTENING
            onStarted()

            // 8. Launch continuous read loop on IO thread
            recordingJob = scope.launch(Dispatchers.IO) {
                val pcmBuffer = ByteArray(CHUNK_SIZE_BYTES)
                var consecutiveSpeechFrames = 0

                while (isActive && _isRecording.value) {
                    val rec = audioRecord ?: break
                    if (rec.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                        MaxVoiceLogger.w("AudioRecord was interrupted or preempted by system")
                        break
                    }

                    val readCount = rec.read(pcmBuffer, 0, pcmBuffer.size)

                    when {
                        readCount > 0 -> {
                            val rms = calculateRMS(pcmBuffer, readCount)
                            val normalized = (rms / 32767f).coerceIn(0f, 1f)
                            _audioAmplitude.value = normalized

                            // Requirement 9: Barge-in / Interruption handling
                            // If assistant is currently speaking and user speaks with sustained energy
                            if (isOutputPlaying()) {
                                if (normalized > INTERRUPTION_ENERGY_THRESHOLD) {
                                    consecutiveSpeechFrames++
                                    if (consecutiveSpeechFrames >= 2) {
                                        MaxVoiceLogger.i("User barge-in detected! Normalized energy: $normalized. Stopping assistant speech.")
                                        onInterruption()
                                        consecutiveSpeechFrames = 0
                                    }
                                } else {
                                    consecutiveSpeechFrames = 0
                                }
                            } else {
                                consecutiveSpeechFrames = 0
                            }

                            // Send PCM chunk
                            val chunk = pcmBuffer.copyOf(readCount)
                            onAudioChunk(chunk)
                        }
                        readCount == AudioRecord.ERROR_INVALID_OPERATION -> {
                            MaxVoiceLogger.e("AudioRecord read: ERROR_INVALID_OPERATION")
                            break
                        }
                        readCount == AudioRecord.ERROR_BAD_VALUE -> {
                            MaxVoiceLogger.e("AudioRecord read: ERROR_BAD_VALUE")
                            break
                        }
                        readCount == AudioRecord.ERROR_DEAD_OBJECT -> {
                            MaxVoiceLogger.e("AudioRecord read: ERROR_DEAD_OBJECT (Audio server died)")
                            break
                        }
                        readCount == AudioRecord.ERROR -> {
                            MaxVoiceLogger.e("AudioRecord read: generic ERROR")
                            break
                        }
                    }
                }

                MaxVoiceLogger.d("AudioRecord read loop finished")
                _isRecording.value = false
                _audioAmplitude.value = 0f
            }
        }
    }

    /**
     * Safely stops recording and cleans up all audio hardware resources.
     */
    fun stopRecording() {
        synchronized(recorderLock) {
            stopRecordingInternal()
        }
    }

    private fun stopRecordingInternal() {
        _isRecording.value = false
        _audioAmplitude.value = 0f

        recordingJob?.cancel()
        recordingJob = null

        try {
            echoCanceler?.release()
            echoCanceler = null
        } catch (e: Exception) {
            MaxVoiceLogger.w("Error releasing echo canceler: ${e.message}")
        }

        try {
            noiseSuppressor?.release()
            noiseSuppressor = null
        } catch (e: Exception) {
            MaxVoiceLogger.w("Error releasing noise suppressor: ${e.message}")
        }

        try {
            audioRecord?.let { rec ->
                if (rec.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    rec.stop()
                }
                rec.release()
                MaxVoiceLogger.d("AudioRecord released successfully")
            }
        } catch (e: Exception) {
            MaxVoiceLogger.w("Exception while stopping/releasing AudioRecord: ${e.message}")
        } finally {
            audioRecord = null
        }
    }

    /**
     * Requirement 28: Test Microphone diagnostic function.
     * Verifies:
     * - RECORD_AUDIO permission
     * - Microphone hardware availability
     * - AudioRecord initialization
     * - Actual PCM audio capture & amplitude
     */
    suspend fun testMicrophone(): MicrophoneTestResult = withContext(Dispatchers.IO) {
        if (!hasMicrophonePermission()) {
            return@withContext MicrophoneTestResult(
                success = false,
                message = "RECORD_AUDIO permission is NOT granted. Please allow microphone access in Settings."
            )
        }

        if (!hasMicrophoneHardware()) {
            return@withContext MicrophoneTestResult(
                success = false,
                message = "Microphone hardware feature is not reported on this device."
            )
        }

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )

        if (minBufferSize <= 0) {
            return@withContext MicrophoneTestResult(
                success = false,
                message = "Hardware audio buffer calculation failed (Error code: $minBufferSize)."
            )
        }

        var testRecorder: AudioRecord? = null
        try {
            testRecorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                maxOf(minBufferSize * 2, 4096)
            )

            if (testRecorder.state != AudioRecord.STATE_INITIALIZED) {
                return@withContext MicrophoneTestResult(
                    success = false,
                    message = "AudioRecord failed to initialize (State: ${testRecorder.state}). Another app may be using the microphone."
                )
            }

            testRecorder.startRecording()
            if (testRecorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                return@withContext MicrophoneTestResult(
                    success = false,
                    message = "AudioRecord failed to start recording (Recording state: ${testRecorder.recordingState})."
                )
            }

            val buffer = ByteArray(2048)
            var totalBytesRead = 0
            var maxRms = 0f

            // Read 5 test frames
            for (i in 0 until 5) {
                val count = testRecorder.read(buffer, 0, buffer.size)
                if (count > 0) {
                    totalBytesRead += count
                    val rms = calculateRMS(buffer, count)
                    if (rms > maxRms) maxRms = rms
                }
                kotlinx.coroutines.delay(40)
            }

            val peakPercent = ((maxRms / 32767f) * 100).toInt().coerceIn(0, 100)

            return@withContext MicrophoneTestResult(
                success = true,
                message = "Microphone is working! Successfully captured $totalBytesRead bytes of native PCM audio (Peak: $peakPercent%).",
                peakAmplitudePercentage = peakPercent,
                bytesCaptured = totalBytesRead
            )
        } catch (e: Exception) {
            return@withContext MicrophoneTestResult(
                success = false,
                message = "Microphone test failed with error: ${e.localizedMessage ?: e.message}"
            )
        } finally {
            try {
                if (testRecorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    testRecorder.stop()
                }
                testRecorder?.release()
            } catch (e: Exception) {
                // Ignore
            }
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
