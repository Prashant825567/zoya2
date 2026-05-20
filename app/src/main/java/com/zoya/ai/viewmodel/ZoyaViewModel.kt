package com.zoya.ai.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zoya.ai.data.*
import com.zoya.ai.live.LiveSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ZoyaViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val GEMINI_KEY = "AIzaSyAW5tQp2RkkktD708z0NmISSalPGCHIBDA"
        private const val PREFS = "zoya_prefs"
        private const val KEY_ACCESS = "access_key"
        private const val KEY_MODE = "personality_mode"
        private const val KEY_LANGUAGE = "response_language"
        private const val IDLE_TIMEOUT_MS = 30_000L
        private const val TRANSCRIPT_MERGE_WINDOW_MS = 15_000L
    }

    private val repo = FirestoreRepo(application)
    private val prefs = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _userData = MutableStateFlow<UserData?>(null)
    val userData: StateFlow<UserData?> = _userData.asStateFlow()
    private val _authLoading = MutableStateFlow(true)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _state = MutableStateFlow(SessionState.DISCONNECTED)
    val state: StateFlow<SessionState> = _state.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _selectedModel = MutableStateFlow(AVAILABLE_MODELS[0])
    val selectedModel: StateFlow<GeminiModel> = _selectedModel.asStateFlow()
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    private val _showModelPicker = MutableStateFlow(false)
    val showModelPicker: StateFlow<Boolean> = _showModelPicker.asStateFlow()
    private val _textInput = MutableStateFlow("")
    val textInput: StateFlow<String> = _textInput.asStateFlow()
    private val _isMicMuted = MutableStateFlow(false)
    val isMicMuted: StateFlow<Boolean> = _isMicMuted.asStateFlow()
    private val _history = MutableStateFlow<List<ChatMessage>>(emptyList())
    val history: StateFlow<List<ChatMessage>> = _history.asStateFlow()

    private val _selectedMode = MutableStateFlow(PERSONALITY_MODES[0])
    val selectedMode: StateFlow<PersonalityMode> = _selectedMode.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(
        AVAILABLE_LANGUAGES.find { it.code == prefs.getString(KEY_LANGUAGE, "hi") } ?: AVAILABLE_LANGUAGES[0]
    )
    val selectedLanguage: StateFlow<LanguageOption> = _selectedLanguage.asStateFlow()

    private var idleJob: Job? = null
    private var liveSession: LiveSession? = null
    private var msgIdCounter = 0

    init {
        val savedMode = prefs.getString(KEY_MODE, "boss") ?: "boss"
        _selectedMode.value = PERSONALITY_MODES.find { it.id == savedMode } ?: PERSONALITY_MODES[0]
        checkSavedAccessKey()
    }

    private fun currentModelName(): String = _selectedModel.value.name

    fun getLanguageInstruction(): String {
        val lang = _selectedLanguage.value
        return "You MUST always respond in ${lang.name} (${lang.nativeName}) language. The user may speak in any language but you must reply in ${lang.name}. This is very important — always respond in ${lang.name}."
    }

    fun setLanguage(lang: LanguageOption) {
        _selectedLanguage.value = lang
        prefs.edit().putString(KEY_LANGUAGE, lang.code).apply()
    }

    private fun checkSavedAccessKey() {
        val saved = prefs.getString(KEY_ACCESS, null)
        if (saved.isNullOrEmpty()) {
            _authLoading.value = false
            return
        }
        viewModelScope.launch {
            repo.validateAccessKey(saved)
                .onSuccess { _userData.value = it }
                .onFailure { prefs.edit().remove(KEY_ACCESS).apply() }
            _authLoading.value = false
        }
    }

    fun submitAccessKey(key: String) {
        _authError.value = null
        _authLoading.value = true
        viewModelScope.launch {
            repo.validateAccessKey(key.trim())
                .onSuccess {
                    prefs.edit().putString(KEY_ACCESS, key.trim()).apply()
                    _userData.value = it
                }
                .onFailure { _authError.value = it.message }
            _authLoading.value = false
        }
    }

    fun logout() {
        val accessKey = _userData.value?.accessKey
        disconnect()
        prefs.edit().remove(KEY_ACCESS).apply()
        _userData.value = null
        _messages.value = emptyList()
        _history.value = emptyList()
        // Clear device binding from Firebase so key can be reused
        if (!accessKey.isNullOrEmpty()) {
            viewModelScope.launch { repo.clearDeviceBinding(accessKey) }
        }
    }

    fun onToggleConnection(requestPermission: () -> Unit) {
        when (_state.value) {
            SessionState.DISCONNECTED, SessionState.IDLE -> requestPermission()
            SessionState.CONNECTING -> {}
            else -> disconnect()
        }
    }

    fun onPermissionGranted() { connect() }

    fun onPermissionDenied() {
        _error.value = "Mic permission required"
        viewModelScope.launch {
            delay(4000)
            _error.value = null
        }
    }

    private fun connect() {
        _error.value = null
        val user = _userData.value ?: return
        liveSession = LiveSession(
            apiKey = GEMINI_KEY,
            modelId = _selectedModel.value.id,
            userName = user.name,
            personalityPrompt = _selectedMode.value.systemPrompt,
            languageInstruction = getLanguageInstruction(),
            onStateChange = {
                _state.value = it
                resetIdleTimer()
            },
            onError = { m ->
                _error.value = m
                viewModelScope.launch {
                    delay(5000)
                    _error.value = null
                }
            },
            onUserVoiceTranscript = { text ->
                handleVoiceTranscript(text = text, isUser = true)
            },
            onAssistantVoiceTranscript = { text ->
                handleVoiceTranscript(text = text, isUser = false)
            },
            openUrl = { url ->
                try {
                    val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    getApplication<Application>().startActivity(i)
                } catch (_: Exception) {
                    _error.value = "Could not open URL"
                }
            }
        )
        liveSession?.connect()
        resetIdleTimer()
    }

    private fun handleVoiceTranscript(text: String, isUser: Boolean) {
        val clean = text.trim()
        if (clean.isEmpty()) return

        val now = System.currentTimeMillis()
        val existing = _messages.value.lastOrNull()

        val shouldMerge = existing != null &&
            existing.isUser == isUser &&
            existing.source == MessageSource.VOICE &&
            now - existing.timestamp <= TRANSCRIPT_MERGE_WINDOW_MS

        val updatedMessage = if (shouldMerge && existing != null) {
            existing.copy(
                text = clean,
                timestamp = now,
                modelName = currentModelName()
            )
        } else {
            msgIdCounter++
            ChatMessage(
                id = msgIdCounter,
                text = clean,
                isUser = isUser,
                timestamp = now,
                source = MessageSource.VOICE,
                modelName = currentModelName()
            )
        }

        if (shouldMerge) {
            _messages.value = _messages.value.dropLast(1) + updatedMessage
        } else {
            _messages.value = _messages.value + updatedMessage
        }

        _userData.value?.let { user ->
            viewModelScope.launch {
                repo.saveMessage(user.accessKey, updatedMessage)
            }
        }
    }

    fun disconnect() {
        idleJob?.cancel()
        liveSession?.disconnect()
        liveSession = null
        _state.value = SessionState.DISCONNECTED
    }

    fun goIdle() {
        idleJob?.cancel()
        liveSession?.disconnect()
        liveSession = null
        _state.value = SessionState.IDLE
    }

    private fun resetIdleTimer() {
        idleJob?.cancel()
        if (_state.value == SessionState.LISTENING) {
            idleJob = viewModelScope.launch {
                delay(IDLE_TIMEOUT_MS)
                if (_state.value == SessionState.LISTENING) goIdle()
            }
        }
    }

    fun wakeUp(requestPermission: () -> Unit) {
        if (_state.value == SessionState.IDLE || _state.value == SessionState.DISCONNECTED) requestPermission()
    }

    fun toggleMicMute() {
        _isMicMuted.value = !_isMicMuted.value
        liveSession?.setMicMuted(_isMicMuted.value)
    }

    fun setPersonalityMode(mode: PersonalityMode) {
        _selectedMode.value = mode
        prefs.edit().putString(KEY_MODE, mode.id).apply()
        if (_state.value != SessionState.DISCONNECTED && _state.value != SessionState.IDLE) disconnect()
    }

    fun selectModel(model: GeminiModel) {
        _selectedModel.value = model
        _showModelPicker.value = false
    }

    fun toggleModelPicker() { _showModelPicker.value = !_showModelPicker.value }
    fun dismissModelPicker() { _showModelPicker.value = false }
    fun updateTextInput(text: String) { _textInput.value = text }

    fun sendTextMessage() {
        val text = _textInput.value.trim()
        if (text.isEmpty() || _state.value == SessionState.DISCONNECTED || _state.value == SessionState.CONNECTING) return

        msgIdCounter++
        val msg = ChatMessage(
            id = msgIdCounter,
            text = text,
            isUser = true,
            timestamp = System.currentTimeMillis(),
            source = MessageSource.TEXT,
            modelName = currentModelName()
        )
        _messages.value = _messages.value + msg
        liveSession?.sendText(text)
        _textInput.value = ""
        _userData.value?.let { u ->
            viewModelScope.launch { repo.saveMessage(u.accessKey, msg) }
        }
        resetIdleTimer()
    }

    fun loadHistory() {
        val user = _userData.value ?: return
        viewModelScope.launch {
            repo.deleteExpiredMessages(user.accessKey)
            _history.value = repo.loadHistory(user.accessKey)
        }
    }

    fun clearHistory() {
        val user = _userData.value ?: return
        viewModelScope.launch {
            repo.clearHistory(user.accessKey)
            _history.value = emptyList()
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
