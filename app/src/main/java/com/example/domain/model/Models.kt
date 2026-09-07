package com.example.domain.model

enum class AssistantState {
    IDLE,
    CONNECTING,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR,
    OFFLINE
}

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

data class ConfirmationRequest(
    val id: String = java.util.UUID.randomUUID().toString(),
    val toolName: String,
    val title: String,
    val description: String,
    val riskLevel: RiskLevel,
    val payload: Map<String, String> = emptyMap()
)

data class ContactMatch(
    val id: String,
    val displayName: String,
    val phoneNumber: String
)

data class ToolCall(
    val callId: String,
    val name: String,
    val arguments: Map<String, Any?>
)

data class ToolResult(
    val callId: String,
    val name: String,
    val success: Boolean,
    val message: String,
    val outputData: Map<String, Any?> = emptyMap()
)

data class DeviceStatus(
    val batteryPercentage: Int,
    val isCharging: Boolean,
    val freeStorageGb: Double,
    val totalStorageGb: Double,
    val model: String,
    val androidVersion: String,
    val isConnected: Boolean,
    val networkType: String
)

data class AssistantConfig(
    val assistantName: String = "MAX",
    val personalityTone: String = "Witty & Sassy",
    val primaryLanguage: String = "Hinglish", // Hinglish, Hindi, English
    val voiceSpeed: Float = 1.0f,
    val wakeWordEnabled: Boolean = true,
    val wakeWordPhrase: String = "MAX",
    val backgroundServiceEnabled: Boolean = false,
    val memoryEnabled: Boolean = true,
    val debugModeEnabled: Boolean = false
)

data class LiveDebugInfo(
    val connectionState: String = "Disconnected",
    val audioState: String = "Idle",
    val assistantState: String = "IDLE",
    val lastToolCall: String = "None",
    val lastToolResult: String = "None",
    val permissionsGranted: String = "None",
    val serviceState: String = "Stopped",
    val lastErrorCode: String = "None"
)
