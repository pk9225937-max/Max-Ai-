package com.example.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class WakeWordManager(
    private val context: Context,
    private val onWakeWordDetected: (String) -> Unit
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val _isListeningForWakeWord = MutableStateFlow(false)
    val isListeningForWakeWord: StateFlow<Boolean> = _isListeningForWakeWord.asStateFlow()

    private val wakeKeywords = listOf("max", "hey max", "hey marks", "marks", "hello max", "hi max")

    fun startListening() {
        if (_isListeningForWakeWord.value) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                // Request on-device offline recognition for low power and privacy
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }

            speechRecognizer?.startListening(intent)
            _isListeningForWakeWord.value = true
        } catch (e: Exception) {
            _isListeningForWakeWord.value = false
        }
    }

    fun stopListening() {
        _isListeningForWakeWord.value = false
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // Ignored
        } finally {
            speechRecognizer = null
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                // If wake word listening is still enabled, restart seamlessly
                if (_isListeningForWakeWord.value) {
                    try {
                        stopListening()
                        startListening()
                    } catch (e: Exception) {
                        _isListeningForWakeWord.value = false
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                handleSpeechResults(results)
                if (_isListeningForWakeWord.value) {
                    startListening()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                handleSpeechResults(partialResults)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun handleSpeechResults(bundle: Bundle?) {
        val matches = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
        for (match in matches) {
            val lower = match.lowercase(Locale.getDefault()).trim()
            for (keyword in wakeKeywords) {
                if (lower.contains(keyword)) {
                    onWakeWordDetected(keyword)
                    return
                }
            }
        }
    }
}
