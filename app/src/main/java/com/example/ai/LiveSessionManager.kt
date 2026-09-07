package com.example.ai

import android.content.Context
import com.example.audio.AudioInputManager
import com.example.audio.AudioOutputManager
import com.example.audio.MicrophoneTestResult
import com.example.audio.WakeWordManager
import com.example.data.local.AppDatabase
import com.example.data.repository.ContactRepository
import com.example.data.repository.DeviceRepository
import com.example.domain.model.AssistantState
import com.example.domain.model.ConfirmationRequest
import com.example.domain.model.LiveDebugInfo
import com.example.domain.model.ToolCall
import com.example.memory.MemoryManager
import com.example.permissions.PermissionManager
import com.example.reminders.ReminderManager
import com.example.security.ConfirmationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LiveSessionManager(private val context: Context) {

    val database by lazy { AppDatabase.getDatabase(context) }
    val deviceRepository by lazy { DeviceRepository(context) }
    val contactRepository by lazy { ContactRepository(context) }
    val reminderManager by lazy { ReminderManager(context) }
    val memoryManager by lazy { MemoryManager(context) }
    val permissionManager by lazy { PermissionManager(context) }
    val confirmationManager by lazy { ConfirmationManager() }

    val audioInputManager by lazy { AudioInputManager(context) }
    val audioOutputManager by lazy { AudioOutputManager(context) }
    val wallpaperThemeManager by lazy { WallpaperThemeManager(context) }

    val toolExecutionEngine by lazy {
        ToolExecutionEngine(
            deviceRepository = deviceRepository,
            contactRepository = contactRepository,
            reminderManager = reminderManager,
            memoryManager = memoryManager,
            permissionManager = permissionManager,
            confirmationManager = confirmationManager,
            database = database,
            wallpaperThemeManager = wallpaperThemeManager
        )
    }

    val geminiLiveManager by lazy {
        GeminiLiveManager(
            context = context,
            audioInputManager = audioInputManager,
            audioOutputManager = audioOutputManager,
            toolExecutionEngine = toolExecutionEngine,
            wallpaperThemeManager = wallpaperThemeManager
        )
    }

    val wakeWordManager by lazy {
        WakeWordManager(context) { detectedWord ->
            geminiLiveManager.activateListening()
        }
    }

    val assistantState: StateFlow<AssistantState> get() = geminiLiveManager.assistantState
    val statusMessage: StateFlow<String> get() = geminiLiveManager.statusMessage
    val debugInfo: StateFlow<LiveDebugInfo> get() = geminiLiveManager.debugInfo
    val pendingConfirmation: StateFlow<ConfirmationRequest?> get() = confirmationManager.pendingConfirmation

    val inputAmplitude: StateFlow<Float> get() = audioInputManager.audioAmplitude
    val outputAmplitude: StateFlow<Float> get() = audioOutputManager.outputAmplitude

    private val scope = CoroutineScope(Dispatchers.Main)

    fun startAssistant(enableWakeWord: Boolean = true) {
        geminiLiveManager.startSession()
        if (enableWakeWord && permissionManager.hasRecordAudio()) {
            wakeWordManager.startListening()
        }
    }

    fun stopAssistant() {
        wakeWordManager.stopListening()
        geminiLiveManager.stopSession()
    }

    fun onOrbClicked() {
        geminiLiveManager.toggleVoiceInteraction()
    }

    suspend fun testMicrophone(): MicrophoneTestResult {
        return audioInputManager.testMicrophone()
    }

    fun confirmPendingAction(confirmed: Boolean) {
        val pending = pendingConfirmation.value ?: return
        confirmationManager.clearConfirmation()

        if (confirmed) {
            scope.launch(Dispatchers.IO) {
                val call = ToolCall(
                    callId = java.util.UUID.randomUUID().toString(),
                    name = pending.toolName,
                    arguments = pending.payload
                )
                toolExecutionEngine.executeTool(call, userAlreadyConfirmed = true)
            }
        }
    }

    fun release() {
        wakeWordManager.stopListening()
        geminiLiveManager.release()
    }
}
