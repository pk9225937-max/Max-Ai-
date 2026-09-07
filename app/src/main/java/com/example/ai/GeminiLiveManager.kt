package com.example.ai

import android.content.Context
import android.util.Base64
import com.example.BuildConfig
import com.example.audio.AudioInputManager
import com.example.audio.AudioOutputManager
import com.example.audio.MaxVoiceLogger
import com.example.domain.model.AssistantState
import com.example.domain.model.LiveDebugInfo
import com.example.domain.model.ToolCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiLiveManager(
    private val context: Context,
    private val audioInputManager: AudioInputManager,
    private val audioOutputManager: AudioOutputManager,
    private val toolExecutionEngine: ToolExecutionEngine,
    val wallpaperThemeManager: WallpaperThemeManager
) {

    companion object {
        // Official Gemini Live native-audio model for real-time bidirectional voice
        const val LIVE_MODEL = "models/gemini-3.1-flash-live-preview"
        private const val LIVE_WS_BASE_URL = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"
        private const val CONNECTION_TIMEOUT_MS = 15000L
        private const val MAX_RECONNECT_ATTEMPTS = 5
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val sessionLock = Any()
    private var webSocket: WebSocket? = null
    private var isHandshakeComplete = false
    private var isConnecting = false
    private var connectionTimeoutJob: Job? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0
    private var isUserExplicitlyStopped = false

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _statusMessage = MutableStateFlow("Tap the Orb to speak")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _transcriptFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val transcriptFlow: SharedFlow<String> = _transcriptFlow.asSharedFlow()

    private val _debugInfo = MutableStateFlow(LiveDebugInfo())
    val debugInfo: StateFlow<LiveDebugInfo> = _debugInfo.asStateFlow()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(10, TimeUnit.SECONDS)
        .build()

    private val prefs = context.getSharedPreferences("max_assistant_prefs", Context.MODE_PRIVATE)

    init {
        audioOutputManager.initPlayer(scope)
    }

    fun getApiKey(): String {
        val customKey = prefs.getString("custom_gemini_api_key", "")?.trim() ?: ""
        if (customKey.isNotBlank()) {
            return customKey
        }
        return try {
            val buildKey = BuildConfig.GEMINI_API_KEY.trim()
            if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") buildKey else ""
        } catch (e: Throwable) {
            ""
        }
    }

    fun isApiKeyConfigured(): Boolean {
        return getApiKey().isNotBlank()
    }

    fun saveCustomApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString("custom_gemini_api_key", trimmed).apply()
        MaxVoiceLogger.i("Custom Gemini API key saved by user")
        if (trimmed.isNotBlank()) {
            if (_assistantState.value == AssistantState.ERROR) {
                _assistantState.value = AssistantState.IDLE
                _statusMessage.value = "MAX is ready. Tap orb to speak."
                updateDebug(connection = "Configured", state = "IDLE", err = "")
            }
        }
    }

    fun getCustomApiKey(): String {
        return prefs.getString("custom_gemini_api_key", "")?.trim() ?: ""
    }

    fun clearCustomApiKey() {
        prefs.edit().remove("custom_gemini_api_key").apply()
        MaxVoiceLogger.i("Custom Gemini API key cleared")
        if (!isApiKeyConfigured()) {
            _assistantState.value = AssistantState.ERROR
            _statusMessage.value = "Gemini API key is missing. Please configure in Settings."
            updateDebug(connection = "Config missing", state = "ERROR", err = "ERR_NO_API_KEY")
        }
    }

    /**
     * Connects bidirectional WebSocket session to Gemini Live API.
     * Prevents multiple concurrent sessions (Requirement 14).
     */
    fun startSession(onConnectedCallback: (() -> Unit)? = null) {
        synchronized(sessionLock) {
            val apiKey = getApiKey()
            if (apiKey.isBlank()) {
                MaxVoiceLogger.w("Cannot start Gemini Live session: API key is blank")
                _assistantState.value = AssistantState.ERROR
                _statusMessage.value = "Please configure Gemini API Key in Settings."
                updateDebug(connection = "Missing API Key", state = "ERROR", err = "ERR_NO_API_KEY")
                return
            }

            // Close existing session before creating a new one and cancel pending reconnection
            closeExistingWebSocket()
            reconnectJob?.cancel()
            reconnectJob = null
            isUserExplicitlyStopped = false
            isConnecting = true

            _assistantState.value = AssistantState.CONNECTING
            _statusMessage.value = "Connecting to Gemini Live..."
            updateDebug(connection = "Connecting...", state = "CONNECTING")

            MaxVoiceLogger.i("Connecting to Gemini Live API: $LIVE_WS_BASE_URL with model: $LIVE_MODEL")

            val requestUrl = "$LIVE_WS_BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(requestUrl)
                .addHeader("x-goog-api-key", apiKey)
                .build()

            // Connection timeout handling: if setupComplete is not received within timeout, trigger error
            connectionTimeoutJob?.cancel()
            connectionTimeoutJob = scope.launch {
                delay(CONNECTION_TIMEOUT_MS)
                synchronized(sessionLock) {
                    if (!isHandshakeComplete && isConnecting) {
                        MaxVoiceLogger.w("Gemini Live connection timed out after ${CONNECTION_TIMEOUT_MS}ms")
                        closeExistingWebSocket()
                        _assistantState.value = AssistantState.ERROR
                        _statusMessage.value = "Connection timeout: Gemini Live server did not respond. Check network/API key."
                        updateDebug(connection = "Timed Out", state = "ERROR", err = "ERR_CONNECTION_TIMEOUT")
                    }
                }
            }

            try {
                webSocket = okHttpClient.newWebSocket(request, createWebSocketListener(onConnectedCallback))
            } catch (e: Exception) {
                MaxVoiceLogger.e("Exception creating WebSocket: ${e.message}", e)
                connectionTimeoutJob?.cancel()
                isConnecting = false
                _assistantState.value = AssistantState.ERROR
                _statusMessage.value = "Connection error: ${e.localizedMessage}"
                updateDebug(connection = "Error", state = "ERROR", err = e.message)
            }
        }
    }

    private fun closeExistingWebSocket() {
        connectionTimeoutJob?.cancel()
        connectionTimeoutJob = null
        isConnecting = false
        try {
            webSocket?.close(1000, "Closing previous session")
        } catch (e: Exception) {
            // Ignored
        } finally {
            webSocket = null
            isHandshakeComplete = false
        }
    }

    fun stopSession() {
        isUserExplicitlyStopped = true
        reconnectJob?.cancel()
        reconnectJob = null
        reconnectAttempts = 0

        audioInputManager.stopRecording()
        audioOutputManager.stopPlayback()

        synchronized(sessionLock) {
            closeExistingWebSocket()
        }

        _assistantState.value = AssistantState.IDLE
        _statusMessage.value = "MAX is ready. Tap orb to speak."
        updateDebug(connection = "Disconnected", state = "IDLE", audio = "Idle")
        MaxVoiceLogger.i("Gemini Live session stopped by user")
    }

    fun toggleVoiceInteraction() {
        if (_assistantState.value == AssistantState.LISTENING || _assistantState.value == AssistantState.SPEAKING) {
            stopListeningToUser()
        } else {
            activateListening()
        }
    }

    /**
     * Requirement 4 & 11:
     * Validates API key, microphone permission and hardware BEFORE entering LISTENING state.
     * Starts AudioRecord capture and only enters LISTENING when recording actually starts.
     */
    fun activateListening() {
        if (!isApiKeyConfigured()) {
            _assistantState.value = AssistantState.ERROR
            _statusMessage.value = "Please configure your Gemini API Key in Settings."
            MaxVoiceLogger.w("activateListening aborted: API key not configured")
            return
        }

        if (!audioInputManager.hasMicrophonePermission()) {
            _assistantState.value = AssistantState.ERROR
            _statusMessage.value = "Microphone unavailable. Please check microphone permission and make sure another app is not using the microphone."
            MaxVoiceLogger.w("activateListening aborted: RECORD_AUDIO permission not granted")
            return
        }

        if (!audioInputManager.hasMicrophoneHardware()) {
            _assistantState.value = AssistantState.ERROR
            _statusMessage.value = "Microphone hardware is not available on this device."
            MaxVoiceLogger.e("activateListening aborted: No mic hardware")
            return
        }

        audioOutputManager.stopPlayback()

        // Ensure WebSocket session is active
        if (webSocket == null || !isHandshakeComplete) {
            _assistantState.value = AssistantState.CONNECTING
            _statusMessage.value = "Connecting to MAX Core..."
            startSession {
                startMicrophoneCapture()
            }
        } else {
            startMicrophoneCapture()
        }
    }

    private fun startMicrophoneCapture() {
        audioInputManager.startRecording(
            scope = scope,
            isOutputPlaying = { audioOutputManager.isPlaying.value },
            onInterruption = {
                handleUserBargeIn()
            },
            onAudioChunk = { pcmChunk ->
                sendAudioChunk(pcmChunk)
            },
            onStarted = {
                // Requirement 11: Only set LISTENING once AudioRecord has successfully started!
                _assistantState.value = AssistantState.LISTENING
                val isGf = wallpaperThemeManager.personality.value == AssistantPersonality.GIRLFRIEND_MODE
                _statusMessage.value = if (isGf) "Haan babu bolo, sun rahi hoon... ❤️" else "Listening... Go ahead!"
                updateDebug(state = "LISTENING", audio = "Microphone Active")
                MaxVoiceLogger.i("Microphone capture active. Assistant state -> LISTENING")
            },
            onError = { errMsg ->
                _assistantState.value = AssistantState.ERROR
                _statusMessage.value = errMsg
                updateDebug(state = "ERROR", audio = "Mic Error", err = errMsg)
                MaxVoiceLogger.e("AudioRecord start failed: $errMsg")
            }
        )
    }

    /**
     * Requirement 9: Barge-in interruption handler.
     * When user speaks while MAX is speaking, immediately cut off playback,
     * flush the audio buffer, and return to LISTENING state.
     */
    private fun handleUserBargeIn() {
        MaxVoiceLogger.i("Handling user barge-in interruption: stopping playback immediately")
        audioOutputManager.stopPlayback()
        _assistantState.value = AssistantState.LISTENING
        updateDebug(state = "LISTENING", audio = "User Interrupted -> Listening")
    }

    fun stopListeningToUser() {
        audioInputManager.stopRecording()
        if (_assistantState.value == AssistantState.LISTENING) {
            _assistantState.value = AssistantState.THINKING
            _statusMessage.value = "Processing..."
            updateDebug(state = "THINKING", audio = "Idle")
        }
    }

    /**
     * Requirement 6 & 7: Sends PCM audio continuously to Gemini Live API in real-time.
     */
    private fun sendAudioChunk(pcmChunk: ByteArray) {
        val ws = webSocket ?: return
        if (!isHandshakeComplete) return

        try {
            val base64Data = Base64.encodeToString(pcmChunk, Base64.NO_WRAP)
            val msg = JSONObject().apply {
                val realtimeInput = JSONObject().apply {
                    val audioObj = JSONObject().apply {
                        put("mimeType", "audio/pcm;rate=16000")
                        put("data", base64Data)
                    }
                    put("audio", audioObj)
                }
                put("realtimeInput", realtimeInput)
            }
            ws.send(msg.toString())
        } catch (e: Exception) {
            MaxVoiceLogger.w("Failed to send audio chunk: ${e.message}")
        }
    }

    private fun sendSetupHandshake(ws: WebSocket) {
        MaxVoiceLogger.i("Sending Gemini Live setup handshake with model: $LIVE_MODEL")
        try {
            val setupMsg = JSONObject().apply {
                val setup = JSONObject().apply {
                    put("model", LIVE_MODEL)

                    val generationConfig = JSONObject().apply {
                        val modalities = JSONArray().apply {
                            put("AUDIO")
                        }
                        put("responseModalities", modalities)

                        val speechConfig = JSONObject().apply {
                            val voiceConfig = JSONObject().apply {
                                val prebuilt = JSONObject().apply {
                                    val savedVoice = wallpaperThemeManager.selectedVoice.value
                                    val validVoices = setOf("Aoede", "Charon", "Fenrir", "Kore", "Puck")
                                    val voice = if (savedVoice in validVoices) savedVoice else "Aoede"
                                    put("voiceName", voice)
                                }
                                put("prebuiltVoiceConfig", prebuilt)
                            }
                            put("voiceConfig", voiceConfig)
                        }
                        put("speechConfig", speechConfig)
                    }
                    put("generationConfig", generationConfig)

                    val systemInstruction = JSONObject().apply {
                        val parts = JSONArray().apply {
                            val part = JSONObject().apply {
                                val activePersonality = wallpaperThemeManager.personality.value
                                put("text", ToolDefinitions.buildSystemInstruction(activePersonality))
                            }
                            put(part)
                        }
                        put("parts", parts)
                    }
                    put("systemInstruction", systemInstruction)

                    val toolsArray = JSONArray().apply {
                        val toolDecl = JSONObject().apply {
                            put("functionDeclarations", ToolDefinitions.getGeminiToolDeclarations())
                        }
                        put(toolDecl)
                    }
                    put("tools", toolsArray)
                }
                put("setup", setup)
            }

            ws.send(setupMsg.toString())
            MaxVoiceLogger.i("Setup handshake message sent successfully")
        } catch (e: Exception) {
            MaxVoiceLogger.e("Failed to construct or send setup handshake: ${e.message}", e)
            _assistantState.value = AssistantState.ERROR
            _statusMessage.value = "Setup failed: ${e.localizedMessage}"
            updateDebug(state = "ERROR", err = "Setup failed: ${e.message}")
        }
    }

    private fun createWebSocketListener(onConnectedCallback: (() -> Unit)?): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                reconnectAttempts = 0
                MaxVoiceLogger.i("Gemini Live WebSocket opened successfully (HTTP ${response.code})")
                updateDebug(connection = "WebSocket Open (Sending Setup)")
                sendSetupHandshake(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                scope.launch {
                    handleServerMessage(text, onConnectedCallback)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                MaxVoiceLogger.d("WebSocket closing: code=$code, reason='$reason'")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                MaxVoiceLogger.i("WebSocket closed: code=$code, reason='$reason'")
                connectionTimeoutJob?.cancel()
                isConnecting = false
                this@GeminiLiveManager.webSocket = null
                isHandshakeComplete = false

                updateDebug(connection = "Closed ($code: $reason)")
                if (!isUserExplicitlyStopped && _assistantState.value != AssistantState.IDLE) {
                    if (code != 1000) {
                        _assistantState.value = AssistantState.ERROR
                        _statusMessage.value = if (reason.isNotBlank()) "Disconnected: $reason" else "Disconnected from Gemini Live ($code)"
                        scheduleReconnect()
                    } else {
                        _assistantState.value = AssistantState.IDLE
                        _statusMessage.value = "MAX is ready. Tap orb to speak."
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                connectionTimeoutJob?.cancel()
                isConnecting = false
                this@GeminiLiveManager.webSocket = null
                isHandshakeComplete = false

                val httpCode = response?.code
                val responseBody = try {
                    response?.body?.string()
                } catch (e: Exception) {
                    null
                }

                val fullErrorLog = buildString {
                    append(t.message ?: "Connection failure")
                    if (httpCode != null) append(" (HTTP $httpCode)")
                    if (!responseBody.isNullOrBlank()) append(" - Response: $responseBody")
                }
                MaxVoiceLogger.e("WebSocket failure: $fullErrorLog", t)

                val userErrorMessage = when {
                    httpCode == 400 -> "Connection failed (400 Bad Request): Verify Gemini Live API schema and settings."
                    httpCode == 401 || httpCode == 403 -> "Gemini API key rejected ($httpCode). Please check your key in Settings."
                    httpCode == 404 -> "Gemini Live model not found ($httpCode). Endpoint: $LIVE_MODEL"
                    httpCode != null -> "Gemini Live connection error (HTTP $httpCode): ${t.message ?: "Failed to connect"}"
                    t is java.net.UnknownHostException -> "No internet connection. Please check network."
                    t is java.net.SocketTimeoutException -> "Connection timed out connecting to Gemini Live."
                    else -> "Connection error: ${t.localizedMessage ?: "Failed to connect to Gemini Live"}"
                }

                _assistantState.value = AssistantState.ERROR
                _statusMessage.value = userErrorMessage
                updateDebug(connection = "Failed (${httpCode ?: "ERR"})", state = "ERROR", err = fullErrorLog)

                // Only reconnect for network or transient errors, not for fatal auth/bad-request errors
                val isFatalError = httpCode in listOf(400, 401, 403, 404)
                if (!isUserExplicitlyStopped && !isFatalError) {
                    scheduleReconnect()
                }
            }
        }
    }

    private suspend fun handleServerMessage(jsonText: String, onConnectedCallback: (() -> Unit)?) {
        try {
            val json = JSONObject(jsonText)

            // 0. Check for error from Gemini Live server
            if (json.has("error")) {
                connectionTimeoutJob?.cancel()
                isConnecting = false
                val errorObj = json.optJSONObject("error")
                val errorCode = errorObj?.optInt("code") ?: 0
                val errorMessage = errorObj?.optString("message") ?: json.optString("error")
                val errorStatus = errorObj?.optString("status") ?: ""
                val fullError = "Gemini Live Error ($errorCode $errorStatus): $errorMessage"
                MaxVoiceLogger.e(fullError)

                withContext(Dispatchers.Main) {
                    _assistantState.value = AssistantState.ERROR
                    _statusMessage.value = errorMessage.ifBlank { "Gemini Live Error ($errorCode)" }
                    updateDebug(connection = "Error", state = "ERROR", err = fullError)
                }
                return
            }

            // 1. Check for setupComplete
            if (json.has("setupComplete")) {
                connectionTimeoutJob?.cancel()
                isConnecting = false
                isHandshakeComplete = true
                MaxVoiceLogger.i("Gemini Live setupComplete handshake received. Session is LIVE.")
                withContext(Dispatchers.Main) {
                    updateDebug(connection = "Live Ready")
                    if (onConnectedCallback != null) {
                        onConnectedCallback.invoke()
                    } else {
                        if (_assistantState.value == AssistantState.CONNECTING) {
                            _assistantState.value = AssistantState.IDLE
                            _statusMessage.value = "Connected to Gemini Live. Tap orb to speak."
                        }
                    }
                }
                return
            }

            // 2. Check for toolCall
            if (json.has("toolCall")) {
                val toolCallObj = json.getJSONObject("toolCall")
                val functionCalls = toolCallObj.optJSONArray("functionCalls")
                if (functionCalls != null && functionCalls.length() > 0) {
                    withContext(Dispatchers.Main) {
                        _assistantState.value = AssistantState.THINKING
                        _statusMessage.value = "Executing action..."
                    }
                    for (i in 0 until functionCalls.length()) {
                        val call = functionCalls.getJSONObject(i)
                        val callId = call.optString("id", java.util.UUID.randomUUID().toString())
                        val name = call.optString("name", "")
                        val argsJson = call.optJSONObject("args") ?: JSONObject()

                        val argsMap = mutableMapOf<String, Any?>()
                        val keys = argsJson.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            argsMap[k] = argsJson.get(k)
                        }

                        MaxVoiceLogger.i("Tool call received from Gemini Live: $name (callId: $callId)")
                        updateDebug(lastTool = name)

                        val toolCall = ToolCall(callId, name, argsMap)
                        val result = toolExecutionEngine.executeTool(toolCall)

                        updateDebug(lastResult = "${result.name}: ${result.message}")
                        sendToolResponse(callId, name, result.message)

                        withContext(Dispatchers.Main) {
                            _statusMessage.value = result.message
                        }
                    }
                }
                return
            }

            // 3. Check for serverContent
            if (json.has("serverContent")) {
                val serverContent = json.getJSONObject("serverContent")

                // Gemini signaled barge-in / interruption
                if (serverContent.optBoolean("interrupted", false)) {
                    MaxVoiceLogger.i("Server signaled interruption event")
                    handleUserBargeIn()
                    return
                }

                val modelTurn = serverContent.optJSONObject("modelTurn")
                if (modelTurn != null) {
                    val parts = modelTurn.optJSONArray("parts")
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)
                            if (part.has("text")) {
                                val text = part.getString("text")
                                _transcriptFlow.emit(text)
                            }
                            if (part.has("inlineData")) {
                                val inlineData = part.getJSONObject("inlineData")
                                val base64 = inlineData.getString("data")
                                val audioBytes = Base64.decode(base64, Base64.DEFAULT)

                                withContext(Dispatchers.Main) {
                                    _assistantState.value = AssistantState.SPEAKING
                                    val isGf = wallpaperThemeManager.personality.value == AssistantPersonality.GIRLFRIEND_MODE
                                    if (_statusMessage.value.startsWith("Listening") || _statusMessage.value.startsWith("Processing") || _statusMessage.value.startsWith("Haan babu")) {
                                        _statusMessage.value = if (isGf) "MAX bol rahi hai... ❤️" else "MAX speaking..."
                                    }
                                }
                                updateDebug(state = "SPEAKING", audio = "Playing response")
                                audioOutputManager.enqueueAudio(audioBytes)
                            }
                        }
                    }
                }

                if (serverContent.optBoolean("turnComplete", false)) {
                    delay(400)
                    if (!audioOutputManager.isPlaying.value && _assistantState.value == AssistantState.SPEAKING) {
                        withContext(Dispatchers.Main) {
                            _assistantState.value = AssistantState.IDLE
                            val isGf = wallpaperThemeManager.personality.value == AssistantPersonality.GIRLFRIEND_MODE
                            _statusMessage.value = if (isGf) "Aapke saath hoon babu... ❤️" else "Tap orb to speak"
                            updateDebug(state = "IDLE")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            MaxVoiceLogger.e("Error parsing Gemini Live message: ${e.message}", e)
            updateDebug(err = "JSON Parse: ${e.localizedMessage}")
        }
    }

    private fun sendToolResponse(callId: String, name: String, resultText: String) {
        val ws = webSocket ?: return
        try {
            val toolResponseMsg = JSONObject().apply {
                val toolResponse = JSONObject().apply {
                    val functionResponses = JSONArray().apply {
                        val resp = JSONObject().apply {
                            if (callId.isNotBlank()) put("id", callId)
                            if (name.isNotBlank()) put("name", name)
                            val responseContent = JSONObject().apply {
                                val output = JSONObject().apply {
                                    put("result", resultText)
                                }
                                put("output", output)
                            }
                            put("response", responseContent)
                        }
                        put(resp)
                    }
                    put("functionResponses", functionResponses)
                }
                put("toolResponse", toolResponse)
            }
            ws.send(toolResponseMsg.toString())
            MaxVoiceLogger.d("Tool response sent for callId: $callId, name: $name")
        } catch (e: Exception) {
            MaxVoiceLogger.e("Failed to send tool response: ${e.message}", e)
        }
    }

    /**
     * Requirement 12: Safely reconnects with exponential backoff without creating duplicate sessions.
     */
    private fun scheduleReconnect() {
        if (isUserExplicitlyStopped) return

        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            _assistantState.value = AssistantState.OFFLINE
            _statusMessage.value = "Connection lost. Tap Orb to reconnect."
            updateDebug(state = "OFFLINE", connection = "Max retries reached")
            MaxVoiceLogger.w("Max reconnection attempts ($MAX_RECONNECT_ATTEMPTS) reached")
            return
        }

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val backoffMs = (1000L * (1 shl reconnectAttempts)).coerceAtMost(16000L)
            reconnectAttempts++
            _statusMessage.value = "Reconnecting in ${backoffMs / 1000}s (Attempt $reconnectAttempts)..."
            updateDebug(connection = "Backoff ${backoffMs / 1000}s (Attempt $reconnectAttempts)")
            MaxVoiceLogger.i("Scheduling reconnect in ${backoffMs}ms (Attempt $reconnectAttempts)")
            delay(backoffMs)
            startSession()
        }
    }

    private fun updateDebug(
        connection: String? = null,
        audio: String? = null,
        state: String? = null,
        lastTool: String? = null,
        lastResult: String? = null,
        err: String? = null
    ) {
        val cur = _debugInfo.value
        _debugInfo.value = cur.copy(
            connectionState = connection ?: cur.connectionState,
            audioState = audio ?: cur.audioState,
            assistantState = state ?: cur.assistantState,
            lastToolCall = lastTool ?: cur.lastToolCall,
            lastToolResult = lastResult ?: cur.lastToolResult,
            lastErrorCode = err ?: cur.lastErrorCode
        )
    }

    fun release() {
        stopSession()
        audioOutputManager.release()
        audioInputManager.stopRecording()
    }
}
