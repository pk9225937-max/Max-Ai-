package com.example.audio

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * WakeWordManager handles the wake word detection state for MAX Assistant.
 *
 * CRITICAL ARCHITECTURAL MANDATE:
 * Does NOT use android.speech.SpeechRecognizer or Google's Speech Recognition Service.
 * Native AudioRecord and Gemini Live native-audio bidirectional streaming are used exclusively.
 */
class WakeWordManager(
    private val context: Context,
    private val onWakeWordDetected: (String) -> Unit
) {
    private val _isListeningForWakeWord = MutableStateFlow(false)
    val isListeningForWakeWord: StateFlow<Boolean> = _isListeningForWakeWord.asStateFlow()

    fun startListening() {
        if (_isListeningForWakeWord.value) return
        _isListeningForWakeWord.value = true
        MaxVoiceLogger.i("Wake word listening enabled (Zero-touch voice listener active)")
    }

    fun stopListening() {
        _isListeningForWakeWord.value = false
        MaxVoiceLogger.i("Wake word listening disabled")
    }

    /**
     * Triggers wake detection programmatically (e.g. from hotword or hardware button)
     */
    fun triggerWakeWord(keyword: String = "MAX") {
        if (_isListeningForWakeWord.value) {
            MaxVoiceLogger.i("Wake word detected: $keyword")
            onWakeWordDetected(keyword)
        }
    }
}
