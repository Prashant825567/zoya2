package com.zoya.ai.live

import android.net.Uri
import android.util.Base64
import android.util.Log
import com.zoya.ai.audio.AudioPlayer
import com.zoya.ai.audio.AudioRecorder
import com.zoya.ai.data.SessionState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class LiveSession(
    private val apiKey: String,
    private val modelId: String,
    private val userName: String = "User",
    private val personalityPrompt: String,
    private val onStateChange: (SessionState) -> Unit,
    private val onError: (String) -> Unit,
    private val onUserVoiceTranscript: (String) -> Unit,
    private val onAssistantVoiceTranscript: (String) -> Unit,
    private val openUrl: (String) -> Unit
) {
    companion object {
        private const val TAG = "LiveSession"
        private const val WS_BASE_URL = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"
    }

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private val audioRecorder = AudioRecorder()
    private var audioPlayer: AudioPlayer? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _state = MutableStateFlow(SessionState.DISCONNECTED)
    val state: StateFlow<SessionState> = _state

    @Volatile
    var micMuted = false
    fun setMicMuted(muted: Boolean) { micMuted = muted }

    private val systemInstruction: String
        get() = """
$personalityPrompt

The user's name is $userName. Use their name sometimes to make it personal.
You can open websites, get current time, and search Google when asked.
Keep responses conversational and natural for voice.
        """.trimIndent()

    fun connect() {
        if (_state.value != SessionState.DISCONNECTED && _state.value != SessionState.IDLE) return
        _state.value = SessionState.CONNECTING
        onStateChange(SessionState.CONNECTING)

        val url = "$WS_BASE_URL?key=$apiKey"
        Log.d(TAG, "Connecting model=$modelId")

        audioPlayer = AudioPlayer { isPlaying ->
            scope.launch(Dispatchers.Main) {
                if (isPlaying && _state.value != SessionState.SPEAKING) {
                    _state.value = SessionState.SPEAKING
                    onStateChange(SessionState.SPEAKING)
                } else if (!isPlaying && _state.value == SessionState.SPEAKING) {
                    _state.value = SessionState.LISTENING
                    onStateChange(SessionState.LISTENING)
                }
            }
        }
        audioPlayer?.initialize()

        webSocket = client.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WS opened")
                sendSetupMessage()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "MSG: ${text.take(300)}")
                handleMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {
                handleMessage(bytes.utf8())
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "FAIL: ${t.message}")
                scope.launch(Dispatchers.Main) {
                    onError(t.message ?: "Failed")
                    disconnect()
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "CLOSING: $code $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "CLOSED: $code $reason")
                scope.launch(Dispatchers.Main) { disconnect() }
            }
        })
    }

    private fun sendSetupMessage() {
        val fullModel = if (modelId.startsWith("models/")) modelId else "models/$modelId"
        val msg = JSONObject().apply {
            put("setup", JSONObject().apply {
                put("model", fullModel)
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().put("AUDIO"))
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", "Aoede")
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply { put("text", systemInstruction) }))
                })
                put("tools", JSONArray().put(JSONObject().apply {
                    put("functionDeclarations", JSONArray().apply {
                        put(JSONObject().apply {
                            put("name", "openWebsite")
                            put("description", "Opens a website URL")
                            put("parameters", JSONObject().apply {
                                put("type", "OBJECT")
                                put("properties", JSONObject().apply {
                                    put("url", JSONObject().apply {
                                        put("type", "STRING")
                                        put("description", "URL to open")
                                    })
                                })
                                put("required", JSONArray().put("url"))
                            })
                        })
                        put(JSONObject().apply {
                            put("name", "getCurrentTime")
                            put("description", "Gets current date and time")
                            put("parameters", JSONObject().apply {
                                put("type", "OBJECT")
                                put("properties", JSONObject())
                            })
                        })
                        put(JSONObject().apply {
                            put("name", "searchGoogle")
                            put("description", "Searches Google")
                            put("parameters", JSONObject().apply {
                                put("type", "OBJECT")
                                put("properties", JSONObject().apply {
                                    put("query", JSONObject().apply {
                                        put("type", "STRING")
                                        put("description", "Search query")
                                    })
                                })
                                put("required", JSONArray().put("query"))
                            })
                        })
                    })
                }))
            })
        }
        Log.d(TAG, "SETUP sent")
        webSocket?.send(msg.toString())
    }

    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            if (json.has("error")) {
                val m = json.getJSONObject("error").optString("message", "Error")
                Log.e(TAG, "ERR: $m")
                scope.launch(Dispatchers.Main) {
                    onError(m)
                    disconnect()
                }
                return
            }
            if (json.has("setupComplete")) {
                Log.d(TAG, "✓ SETUP COMPLETE")
                scope.launch(Dispatchers.Main) {
                    _state.value = SessionState.LISTENING
                    onStateChange(SessionState.LISTENING)
                }
                startAudioCapture()
                return
            }
            if (json.has("serverContent")) {
                val sc = json.getJSONObject("serverContent")
                if (sc.optBoolean("interrupted", false)) {
                    audioPlayer?.stop()
                    scope.launch(Dispatchers.Main) {
                        _state.value = SessionState.LISTENING
                        onStateChange(SessionState.LISTENING)
                    }
                }

                if (sc.has("inputTranscription")) {
                    val t = sc.getJSONObject("inputTranscription").optString("text", "")
                    if (t.isNotBlank()) {
                        scope.launch(Dispatchers.Main) { onUserVoiceTranscript(t) }
                    }
                }

                if (sc.has("outputTranscription")) {
                    val t = sc.getJSONObject("outputTranscription").optString("text", "")
                    if (t.isNotBlank()) {
                        scope.launch(Dispatchers.Main) { onAssistantVoiceTranscript(t) }
                    }
                }

                if (sc.has("modelTurn")) {
                    val mt = sc.getJSONObject("modelTurn")
                    if (mt.has("parts")) {
                        val parts = mt.getJSONArray("parts")
                        for (i in 0 until parts.length()) {
                            val p = parts.getJSONObject(i)
                            if (p.has("inlineData") && p.getJSONObject("inlineData").has("data")) {
                                scope.launch {
                                    audioPlayer?.addAudioChunk(p.getJSONObject("inlineData").getString("data"))
                                }
                            }
                        }
                    }
                }
            }
            if (json.has("toolCall")) handleToolCall(json.getJSONObject("toolCall"))
        } catch (e: Exception) {
            Log.e(TAG, "PARSE: ${e.message}")
        }
    }

    private fun handleToolCall(tc: JSONObject) {
        val fcs = tc.getJSONArray("functionCalls")
        val resps = JSONArray()
        for (i in 0 until fcs.length()) {
            val fc = fcs.getJSONObject(i)
            val name = fc.getString("name")
            val id = fc.getString("id")
            val args = fc.optJSONObject("args") ?: JSONObject()
            val result = when (name) {
                "openWebsite" -> {
                    val u = args.optString("url", "")
                    scope.launch(Dispatchers.Main) { openUrl(if (u.startsWith("http")) u else "https://$u") }
                    JSONObject().put("result", "Opened $u")
                }
                "getCurrentTime" -> JSONObject().put(
                    "result",
                    SimpleDateFormat("EEEE, MMMM d, yyyy h:mm:ss a", Locale.getDefault()).format(Date())
                )
                "searchGoogle" -> {
                    val q = args.optString("query", "")
                    scope.launch(Dispatchers.Main) { openUrl("https://www.google.com/search?q=${Uri.encode(q)}") }
                    JSONObject().put("result", "Searched: $q")
                }
                else -> JSONObject().put("error", "Unknown")
            }
            resps.put(JSONObject().apply {
                put("name", name)
                put("id", id)
                put("response", result)
            })
        }
        webSocket?.send(
            JSONObject().apply {
                put("toolResponse", JSONObject().apply { put("functionResponses", resps) })
            }.toString()
        )
    }

    private fun startAudioCapture() {
        recordingJob = scope.launch {
            try {
                audioRecorder.startRecording().collect { bytes ->
                    if (webSocket != null && _state.value != SessionState.DISCONNECTED && !micMuted) {
                        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        webSocket?.send(
                            JSONObject().apply {
                                put("realtimeInput", JSONObject().apply {
                                    put("audio", JSONObject().apply {
                                        put("data", b64)
                                        put("mimeType", "audio/pcm;rate=16000")
                                    })
                                })
                            }.toString()
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "MIC: ${e.message}")
            }
        }
    }

    fun sendText(text: String) {
        if (_state.value == SessionState.DISCONNECTED || _state.value == SessionState.CONNECTING) return
        webSocket?.send(
            JSONObject().apply {
                put("clientContent", JSONObject().apply {
                    put("turns", JSONArray().put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().put(JSONObject().apply { put("text", text) }))
                    }))
                    put("turnComplete", true)
                })
            }.toString()
        )
    }

    fun disconnect() {
        recordingJob?.cancel()
        audioRecorder.stopRecording()
        audioPlayer?.release()
        audioPlayer = null
        try { webSocket?.close(1000, "Bye") } catch (_: Exception) {}
        webSocket = null
        _state.value = SessionState.DISCONNECTED
        onStateChange(SessionState.DISCONNECTED)
    }
}
