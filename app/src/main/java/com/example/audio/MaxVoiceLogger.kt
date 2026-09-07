package com.example.audio

import android.util.Log

/**
 * Dedicated secure logger for MAX AI Voice System.
 * Tag: "MaxVoiceSystem"
 * Security mandate: NEVER exposes Gemini API keys or sensitive user credentials.
 */
object MaxVoiceLogger {
    private const val TAG = "MaxVoiceSystem"

    fun d(message: String) {
        Log.d(TAG, sanitize(message))
    }

    fun i(message: String) {
        Log.i(TAG, sanitize(message))
    }

    fun w(message: String) {
        Log.w(TAG, sanitize(message))
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(TAG, sanitize(message), throwable)
        } else {
            Log.e(TAG, sanitize(message))
        }
    }

    /**
     * Sanitizes any message to ensure API keys (e.g. AIzaSy...) are never logged.
     */
    fun sanitize(input: String): String {
        return input.replace(Regex("key=[A-Za-z0-9_-]+"), "key=***REDACTED***")
            .replace(Regex("AIza[0-9A-Za-z-_]{35}"), "AIza***REDACTED***")
    }
}
