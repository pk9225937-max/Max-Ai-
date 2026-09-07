package com.example.security

import com.example.domain.model.ConfirmationRequest
import com.example.domain.model.RiskLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConfirmationManager {

    private val _pendingConfirmation = MutableStateFlow<ConfirmationRequest?>(null)
    val pendingConfirmation: StateFlow<ConfirmationRequest?> = _pendingConfirmation.asStateFlow()

    fun evaluateRisk(toolName: String, params: Map<String, Any?>): RiskLevel {
        return when (toolName) {
            "searchAndCallContact", "sendSMS", "sendWhatsAppMessage", "sendGmail", "clearAllMemory", "clearAllReminders" -> RiskLevel.HIGH
            "createReminder", "createCalendarEvent", "deleteReminder", "deleteMemory" -> RiskLevel.MEDIUM
            "openApp", "webSearch", "searchYouTube", "flashlight", "getDeviceStatus", "mediaControl", "openCamera", "openGallery", "openSettings", "rememberInfo", "queryMemory" -> RiskLevel.LOW
            else -> RiskLevel.HIGH
        }
    }

    fun requestConfirmation(request: ConfirmationRequest) {
        _pendingConfirmation.value = request
    }

    fun clearConfirmation() {
        _pendingConfirmation.value = null
    }

    /**
     * Parse confirmation speech strictly. Avoid ambiguous matches.
     */
    fun isAffirmativeConfirmation(text: String): Boolean {
        val cleaned = text.trim().lowercase()
        val affirmativeTokens = setOf(
            "haan", "ha", "yes", "kar do", "kardo", "call kar do", "karo", "confirm", "proceed",
            "sure", "theek hai", "bhejo", "send", "okay", "ok"
        )
        return affirmativeTokens.any { cleaned == it || cleaned.startsWith("$it ") || cleaned.endsWith(" $it") }
    }

    fun isNegativeConfirmation(text: String): Boolean {
        val cleaned = text.trim().lowercase()
        val negativeTokens = setOf(
            "nahi", "no", "nah", "mat karo", "cancel", "don't", "dont", "abort", "stop", "ruk jao"
        )
        return negativeTokens.any { cleaned == it || cleaned.startsWith("$it ") || cleaned.endsWith(" $it") }
    }
}
