package com.example.ai

import android.content.Context
import android.util.Base64
import com.example.BuildConfig
import com.example.audio.AudioInputManager
import com.example.audio.AudioOutputManager
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
import kotlinx.coroutines.isActive
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
    private val toolExecutionEngine: ToolExecutionEngine
) {

    companion object {
        private const val LIVE_MODEL = "models/gemini-2.5-flash-native-audio-preview-12-2025"
        private const val LIVE_WS_URL = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent"
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _statusMessage = MutableStateFlow("Tap the Orb or say 'MAX'")
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

    init {
        audioOutputManager.initPlayer(scope)
    }

    fun isApiKeyConfigured(): Boolean {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            key.isNotBlank() && key != "MY_GEMINI_API_KEY"
        } catch (e: Throwable) {
            false
        }
    }

    fun startSession() {
        if (!isApiKeyConfigured()) {
            _assistantState.value = AssistantState.ERROR
            _statusMessage.value = "Gemini connection configuration is missing."
            updateDebug(connection = "Config missing", err = "ERR_NO_API_KEY")
            return
        }

        if (webSocket != null) return

        _assistantState.value = AssistantState.CONNECTING
        _statusMessage.value = "Connecting to MAX Core..."
        updateDebug(connection = "Connecting...", state = "CONNECTING")

        val apiKey = BuildConfig.GEMINI_API_KEY
        val requestUrl = "$LIVE_WS_URL?key=$apiKey"
        val request = Request.Builder().url(requestUrl).build()

        webSocket = okHttpClient.newWebSocket(request, createWebSocketListener())
    }

    fun stopSession() {
        reconnectJob?.cancel()
        reconnectJob = null
        reconnectAttempts = 0

        audioInputManager.stopRecording()
        audioOutputManager.stopPlayback()

        webSocket?.close(1000, "User stopped session")
        webSocket = null

        _assistantState.value = AssistantState.IDLE
        _statusMessage.value = "MAX is ready. Tap orb or say 'MAX'."
        updateDebug(connection = "Disconnected", state = "IDLE", audio = "Idle")
    }

    fun toggleVoiceInteraction() {
        if (_assistantState.value == AssistantState.LISTENING || _assistantState.value == AssistantState.SPEAKING) {
            stopListeningToUser()
        } else {
            activateListening()
        }
    }

    fun activateListening() {
        if (!isApiKeyConfigured()) {
            _assistantState.value = AssistantState.ERROR
            _statusMessage.value = "Gemini connection configuration is missing."
            return
        }

        audioOutputManager.stopPlayback()

        if (webSocket == null) {
            startSession()
        }

        _assistantState.value = AssistantState.LISTENING
        _statusMessage.value = "Listening... Bolिये boss!"
        updateDebug(state = "LISTENING", audio = "Microphone Active")

        audioInputManager.startRecording(
            scope = scope,
            onAudioChunk = { pcmChunk ->
                sendAudioChunk(pcmChunk)
            },
            onError = { errMsg ->
                _assistantState.value = AssistantState.ERROR
                _statusMessage.value = errMsg
                updateDebug(state = "ERROR", audio = "Mic Error", err = errMsg)
            }
        )
    }

    fun stopListeningToUser() {
        audioInputManager.stopRecording()
        if (_assistantState.value == AssistantState.LISTENING) {
            _assistantState.value = AssistantState.THINKING
            _statusMessage.value = "MAX thinking..."
            updateDebug(state = "THINKING", audio = "Processing")
        }
    }

    private fun sendAudioChunk(pcmChunk: ByteArray) {
        val ws = webSocket ?: return
        val base64 = Base64.encodeToString(pcmChunk, Base64.NO_WRAP)
        val msg = JSONObject().apply {
            val realtimeInput = JSONObject().apply {
                val mediaChunks = JSONArray().apply {
                    val chunk = JSONObject().apply {
                        put("mimeType", "audio/pcm;rate=16000")
                        put("data", base64)
                    }
                    put(chunk)
                }
                put("mediaChunks", mediaChunks)
            }
            put("realtimeInput", realtimeInput)
        }
        ws.send(msg.toString())
    }

    private fun sendSetupHandshake(ws: WebSocket) {
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
                                put("voiceName", "Aoede")
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
                            put("text", ToolDefinitions.SYSTEM_INSTRUCTION)
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
    }

    private fun createWebSocketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                reconnectAttempts = 0
                updateDebug(connection = "Connected (Gemini Live)")
                sendSetupHandshake(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                scope.launch {
                    handleServerMessage(text)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                this@GeminiLiveManager.webSocket = null
                updateDebug(connection = "Closed ($reason)")
                if (_assistantState.value != AssistantState.IDLE) {
                    scheduleReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                this@GeminiLiveManager.webSocket = null
                val err = t.localizedMessage ?: "Connection error"
                updateDebug(connection = "Failed", err = err)
                scheduleReconnect()
            }
        }
    }

    private suspend fun handleServerMessage(jsonText: String) {
        try {
            val json = JSONObject(jsonText)

            // 1. Check for setupComplete
            if (json.has("setupComplete")) {
                withContext(Dispatchers.Main) {
                    _statusMessage.value = "MAX Connected! Ready to speak."
                    updateDebug(connection = "Live Ready")
                }
                return
            }

            // 2. Check for toolCall
            if (json.has("toolCall")) {
                val toolCallObj = json.getJSONObject("toolCall")
                val functionCalls = toolCallObj.optJSONArray("functionCalls")
                if (functionCalls != null && functionCalls.length() > 0) {
                    _assistantState.value = AssistantState.THINKING
                    _statusMessage.value = "Executing action..."
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

                        updateDebug(lastTool = name)

                        val toolCall = ToolCall(callId, name, argsMap)
                        val result = toolExecutionEngine.executeTool(toolCall)

                        updateDebug(lastResult = "${result.name}: ${result.message}")
                        sendToolResponse(callId, result.message)

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

                if (serverContent.optBoolean("interrupted", false)) {
                    audioOutputManager.stopPlayback()
                    _assistantState.value = AssistantState.LISTENING
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

                                _assistantState.value = AssistantState.SPEAKING
                                updateDebug(state = "SPEAKING", audio = "Playing response")
                                audioOutputManager.enqueueAudio(audioBytes)
                            }
                        }
                    }
                }

                if (serverContent.optBoolean("turnComplete", false)) {
                    delay(300)
                    if (!audioOutputManager.isPlaying.value) {
                        _assistantState.value = AssistantState.IDLE
                        _statusMessage.value = "MAX is listening. Say 'MAX' or tap orb."
                        updateDebug(state = "IDLE")
                    }
                }
            }
        } catch (e: Exception) {
            updateDebug(err = "JSON Parse: ${e.localizedMessage}")
        }
    }

    private fun sendToolResponse(callId: String, resultText: String) {
        val ws = webSocket ?: return
        val toolResponseMsg = JSONObject().apply {
            val toolResponse = JSONObject().apply {
                val functionResponses = JSONArray().apply {
                    val resp = JSONObject().apply {
                        put("id", callId)
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
    }

    private fun scheduleReconnect() {
        if (reconnectAttempts >= 5) {
            _assistantState.value = AssistantState.OFFLINE
            _statusMessage.value = "Internet connection issue. Offline local features available."
            updateDebug(state = "OFFLINE", connection = "Offline fallback active")
            return
        }

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val backoffMs = (1000L * (1 shl reconnectAttempts)).coerceAtMost(16000L)
            reconnectAttempts++
            _statusMessage.value = "Reconnecting in ${backoffMs / 1000}s..."
            updateDebug(connection = "Backoff ${backoffMs / 1000}s (Attempt $reconnectAttempts)")
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
    }
}
