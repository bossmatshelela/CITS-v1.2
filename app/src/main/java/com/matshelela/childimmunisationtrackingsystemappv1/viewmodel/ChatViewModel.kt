package com.matshelela.childimmunisationtrackingsystemappv1.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.Chat
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.TextPart
import com.google.firebase.ai.type.generationConfig
import com.matshelela.childimmunisationtrackingsystemappv1.BuildConfig
import com.matshelela.childimmunisationtrackingsystemappv1.data.firebase.FirebaseInitializer
import com.matshelela.childimmunisationtrackingsystemappv1.data.repository.ChatRepository
import com.matshelela.childimmunisationtrackingsystemappv1.data.repository.ChatSessionState
import com.matshelela.childimmunisationtrackingsystemappv1.data.repository.FirebaseAiChatRepository
import com.matshelela.childimmunisationtrackingsystemappv1.ui.chat.ChatMessage
import com.matshelela.childimmunisationtrackingsystemappv1.ui.chat.ChatUiState
import com.matshelela.childimmunisationtrackingsystemappv1.ui.chat.ImmunisationBotRole
import com.matshelela.childimmunisationtrackingsystemappv1.ui.chat.MessageStatus
import com.matshelela.childimmunisationtrackingsystemappv1.util.NetworkConnectivityMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "ChatViewModel"
    private val networkMonitor = NetworkConnectivityMonitor(application)

    private val _uiState = MutableStateFlow(
        ChatUiState(
            messages = listOf(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = "Hello! I am your CITS Assistant, here to support you with your child's immunization journey and health tracking.\n\nYou can ask me about upcoming vaccination dates, what each vaccine protects against, how to comfort your baby after a dose, or how to catch up on missed immunizations.\n\n*Note: My guidance is for informational purposes only and does not replace professional medical advice. Always consult your local clinic or pediatrician for clinical decisions.*",
                    isUser = false,
                    senderName = "CITS Assistant",
                    status = MessageStatus.SENT,
                    modelUsed = "gemini-3.5-flash"
                )
            )
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val chatRepository: ChatRepository = FirebaseAiChatRepository(application)
    val sessionState: StateFlow<ChatSessionState> = chatRepository.sessionState

    private var currentChatSession: Chat? = null
    private var currentGenerativeModel: GenerativeModel? = null

    // Text to Speech
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    // Speech Recognizer for voice conversation
    private var speechRecognizer: SpeechRecognizer? = null

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    init {
        initTts(application)
        observeNetworkState()
        initAiEngine(
            role = _uiState.value.selectedRole,
            modelName = _uiState.value.selectedModel
        )
        // Asynchronously initialize App Check and Remote Config via FirebaseInitializer
        viewModelScope.launch(Dispatchers.IO) {
            val config = chatRepository.initializeSession(application)
            _uiState.update {
                it.copy(
                    selectedModel = config.modelName,
                    connectionEngine = "Firebase AI Logic SDK • App Check (Play Integrity) • Remote Config (${config.modelName})"
                )
            }
        }
    }

    private fun observeNetworkState() {
        viewModelScope.launch {
            networkMonitor.isConnectedFlow.collect { connected ->
                _uiState.update { it.copy(isOnline = connected) }
            }
        }
    }

    private fun initTts(application: Application) {
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.ENGLISH
                isTtsReady = true
            }
        }
    }

    /**
     * Initializes or updates the Firebase AI Logic SDK model & chatSession.
     * All initialization happens safely off the main thread.
     */
    private fun initAiEngine(role: ImmunisationBotRole, modelName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Ensure Firebase is initialized
                if (FirebaseApp.getApps(getApplication()).isEmpty()) {
                    FirebaseApp.initializeApp(getApplication())
                }

                val firebaseApp = FirebaseApp.getInstance()
                val firebaseAi = FirebaseAI.getInstance(firebaseApp)
                val genModel = firebaseAi.generativeModel(
                    modelName = modelName,
                    generationConfig = generationConfig {
                        temperature = 0.7f
                        topP = 0.95f
                    },
                    systemInstruction = Content(
                        role = "system",
                        parts = listOf(TextPart(role.systemInstruction))
                    )
                )
                currentGenerativeModel = genModel
                currentChatSession = genModel.startChat()

                _uiState.update {
                    it.copy(
                        connectionEngine = "Firebase AI Logic SDK • App Check Protected",
                        selectedRole = role,
                        selectedModel = modelName
                    )
                }
                Log.d(TAG, "Firebase AI Logic initialized with model: $modelName, role: ${role.title}")
            } catch (e: Exception) {
                Log.w(TAG, "Firebase AI init warning (will fallback to direct streaming): ${e.message}")
                _uiState.update {
                    it.copy(
                        connectionEngine = "Gemini Direct Cloud Engine (MOHCC Resilient)",
                        selectedRole = role,
                        selectedModel = modelName
                    )
                }
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun selectRole(role: ImmunisationBotRole) {
        if (_uiState.value.selectedRole == role) return
        val newModel = role.defaultModel
        _uiState.update {
            it.copy(
                selectedRole = role,
                selectedModel = newModel
            )
        }
        initAiEngine(role = role, modelName = newModel)

        // Add a system role shift announcement bubble
        val announcement = ChatMessage(
            text = "Switched to ${role.title}. ${role.subtitle}.",
            isUser = false,
            senderName = "System",
            status = MessageStatus.SENT
        )
        _uiState.update { it.copy(messages = it.messages + announcement) }
    }

    fun selectModel(modelName: String) {
        if (_uiState.value.selectedModel == modelName) return
        _uiState.update { it.copy(selectedModel = modelName) }
        initAiEngine(role = _uiState.value.selectedRole, modelName = modelName)
    }

    /**
     * Primary chat submission method adhering strictly to:
     * 1. Threading & Performance: Offloaded to Dispatchers.IO.
     * 2. Low-Latency Streaming: Collects chunks from chatSession.sendMessageStream().
     * 3. List Optimization: Assigns unique stable UUIDs for LazyColumn keys.
     * 4. Robust Error Handling: Wrapped in try-catch with friendly user feedback.
     */
    fun sendMessage(textOverride: String? = null, isVoice: Boolean = false) {
        val messageText = (textOverride ?: _uiState.value.inputText).trim()
        if (messageText.isBlank()) return

        // 1. Clear input field immediately
        _uiState.update { it.copy(inputText = "", errorMessage = null) }

        // 2. Create user message
        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = messageText,
            isUser = true,
            status = MessageStatus.SENT,
            senderName = "Parent",
            isVoice = isVoice
        )

        // 3. Create initial empty assistant message for streaming
        val assistantMessageId = UUID.randomUUID().toString()
        val assistantPlaceholder = ChatMessage(
            id = assistantMessageId,
            text = "",
            isUser = false,
            status = MessageStatus.STREAMING,
            senderName = _uiState.value.selectedRole.title,
            modelUsed = _uiState.value.selectedModel,
            isVoice = isVoice
        )

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage + assistantPlaceholder,
                isGenerating = true
            )
        }

        // 4. Execute streaming on Dispatchers.IO
        viewModelScope.launch(Dispatchers.IO) {
            val fullResponseBuilder = StringBuilder()
            var streamedSuccessfully = false

            try {
                // Primary path: Use Clean Architecture FirebaseAiChatRepository (Firebase AI Logic + App Check)
                try {
                    Log.d(TAG, "Streaming via ChatRepository (Firebase AI Logic)")
                    chatRepository.sendUserMessageStream(messageText).collect { chunkText ->
                        fullResponseBuilder.append(chunkText)
                        val currentText = fullResponseBuilder.toString()
                        _uiState.update { state ->
                            val updatedMessages = state.messages.map { msg ->
                                if (msg.id == assistantMessageId) {
                                    msg.copy(text = currentText, status = MessageStatus.STREAMING)
                                } else {
                                    msg
                                }
                            }
                            state.copy(messages = updatedMessages)
                        }
                    }
                    streamedSuccessfully = fullResponseBuilder.isNotEmpty()
                } catch (repoErr: Exception) {
                    Log.w(TAG, "ChatRepository stream attempt: ${repoErr.message}, checking fallback")
                }

                // If chatSession was null or returned empty, attempt direct REST streaming
                if (!streamedSuccessfully) {
                    Log.d(TAG, "Falling back to Gemini Direct REST Streaming engine")
                    streamViaRestApi(
                        prompt = messageText,
                        role = _uiState.value.selectedRole,
                        modelName = _uiState.value.selectedModel,
                        assistantMessageId = assistantMessageId,
                        fullResponseBuilder = fullResponseBuilder
                    )
                    streamedSuccessfully = fullResponseBuilder.isNotEmpty()
                }

                // If still empty (e.g. offline or strict timeout), use local ZEPI clinical guide
                if (!streamedSuccessfully) {
                    val localFallback = generateOfflineZepiResponse(messageText, _uiState.value.selectedRole)
                    fullResponseBuilder.append(localFallback)
                    _uiState.update { state ->
                        val updatedMessages = state.messages.map { msg ->
                            if (msg.id == assistantMessageId) {
                                msg.copy(text = localFallback, status = MessageStatus.SENT)
                            } else {
                                msg
                            }
                        }
                        state.copy(messages = updatedMessages)
                    }
                } else {
                    // Mark completed message as SENT
                    _uiState.update { state ->
                        val updatedMessages = state.messages.map { msg ->
                            if (msg.id == assistantMessageId) {
                                msg.copy(
                                    text = fullResponseBuilder.toString(),
                                    status = MessageStatus.SENT
                                )
                            } else {
                                msg
                            }
                        }
                        state.copy(messages = updatedMessages, isGenerating = false)
                    }
                }

                // If in voice mode or speaking enabled, read the response aloud
                if (_uiState.value.isVoiceModeActive || isVoice) {
                    speakText(fullResponseBuilder.toString())
                }

            } catch (e: Exception) {
                Log.e(TAG, "AI streaming error: ${e.message}", e)
                val friendlyError = if (!_uiState.value.isOnline) {
                    "You are currently offline. Here is the offline clinic advice for your query:\n\n" +
                            generateOfflineZepiResponse(messageText, _uiState.value.selectedRole)
                } else {
                    "Connection note: Unable to complete AI streaming response (${e.localizedMessage ?: "Network timeout"}). Tap to retry, or consult the local clinic."
                }

                _uiState.update { state ->
                    val updatedMessages = state.messages.map { msg ->
                        if (msg.id == assistantMessageId) {
                            msg.copy(
                                text = friendlyError,
                                status = if (!_uiState.value.isOnline) MessageStatus.SENT else MessageStatus.ERROR
                            )
                        } else {
                            msg
                        }
                    }
                    state.copy(
                        messages = updatedMessages,
                        isGenerating = false,
                        errorMessage = e.message
                    )
                }
            } finally {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    /**
     * Fallback direct REST streaming engine for high resilience
     */
    private suspend fun streamViaRestApi(
        prompt: String,
        role: ImmunisationBotRole,
        modelName: String,
        assistantMessageId: String,
        fullResponseBuilder: StringBuilder
    ) = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // No custom API key provided; yield so offline ZEPI generator responds
            return@withContext
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:streamGenerateContent?alt=sse&key=$apiKey"

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", role.systemInstruction))
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "Direct REST streaming failed with code: ${response.code}")
                return@withContext
            }

            val body = response.body ?: return@withContext
            val reader = BufferedReader(InputStreamReader(body.byteStream()))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line?.trim() ?: continue
                if (currentLine.startsWith("data:")) {
                    val jsonPayload = currentLine.removePrefix("data:").trim()
                    if (jsonPayload == "[DONE]" || jsonPayload.isEmpty()) continue
                    try {
                        val obj = JSONObject(jsonPayload)
                        val candidates = obj.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val candidate = candidates.getJSONObject(0)
                            val content = candidate.optJSONObject("content")
                            val parts = content?.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                val text = parts.getJSONObject(0).optString("text", "")
                                if (text.isNotEmpty()) {
                                    fullResponseBuilder.append(text)
                                    val currentText = fullResponseBuilder.toString()
                                    _uiState.update { state ->
                                        val updatedMessages = state.messages.map { msg ->
                                            if (msg.id == assistantMessageId) {
                                                msg.copy(text = currentText, status = MessageStatus.STREAMING)
                                            } else {
                                                msg
                                            }
                                        }
                                        state.copy(messages = updatedMessages)
                                    }
                                }
                            }
                        }
                    } catch (ignore: Exception) {}
                }
            }
        }
    }

    /**
     * Offline ZEPI Knowledge Engine providing 100% crash-proof answers even if disconnected.
     */
    private fun generateOfflineZepiResponse(query: String, role: ImmunisationBotRole): String {
        val q = query.lowercase()
        val content = when {
            q.contains("birth") || q.contains("bcg") -> {
                "• **At Birth**: BCG (Tuberculosis) in right upper arm intradermally, and OPV 0 (Oral Polio Vaccine 2 drops).\n• **What to expect**: A small red bump appears after 2–4 weeks which may ulcerate and heal into a small permanent scar. This is normal."
            }
            q.contains("6 week") || q.contains("six week") || q.contains("penta 1") -> {
                "• **At 6 Weeks**: Pentavalent 1 (DTP-HepB-Hib, left thigh), PCV 1 (right thigh), OPV 1 (oral), and Rotavirus 1 (oral).\n• **Protection**: Diphtheria, Tetanus, Pertussis, Hepatitis B, Hib, Pneumonia, Polio, and Rotavirus Diarrhea.\n• **Common reactions**: Mild fever or fussiness for 24–48 hours. Keep your baby cool and breastfeed frequently."
            }
            q.contains("10 week") || q.contains("ten week") -> {
                "• **At 10 Weeks**: Pentavalent 2 (left thigh), PCV 2 (right thigh), OPV 2 (oral), and Rotavirus 2 (oral).\n• **Important**: If delayed, do not restart the schedule! Simply receive the dose at the next clinic visit."
            }
            q.contains("14 week") || q.contains("fourteen week") -> {
                "• **At 14 Weeks**: Pentavalent 3 (left thigh), PCV 3 (right thigh), and IPV (Inactivated Polio Injection, right thigh).\n• Completes the primary infant series."
            }
            q.contains("measles") || q.contains("rubella") || q.contains("9 month") || q.contains("mr") -> {
                "• **At 9 Months**: Measles-Rubella 1 (MR 1, right upper arm), Typhoid Conjugate Vaccine (TCV), and Vitamin A (100,000 IU blue capsule).\n• **At 18 Months**: Measles-Rubella 2 (MR 2) booster and Vitamin A (200,000 IU)."
            }
            q.contains("fever") || q.contains("reaction") || q.contains("swelling") -> {
                "• **Mild Reactions**: Low fever (<38.5°C), irritability, or tenderness at injection site are common signs that the immune system is responding.\n• **Home Care**: Offer extra fluids/breast milk, dress in lightweight cotton, and do not apply hot compresses.\n• **Urgent Signs**: Seek clinic care immediately if fever exceeds 39°C, convulsions occur, or crying is continuous for >3 hours."
            }
            q.contains("miss") || q.contains("late") || q.contains("catch") || q.contains("overdue") -> {
                "• **Catch-Up Rule**: Never restart an immunization schedule! Missing a scheduled date simply means receiving the next dose as soon as possible.\n• The child retains memory of previous doses. Bring the child's Child Health Card (Road to Health Card) to any clinic."
            }
            else -> {
                "• **Zimbabwe National ZEPI Schedule Reminder**:\n- **Birth**: BCG, OPV 0\n- **6, 10, 14 Weeks**: Pentavalent, PCV, OPV, Rotavirus & IPV\n- **9 Months**: Measles-Rubella 1, Typhoid, Vitamin A\n- **18 Months**: Measles-Rubella 2 booster\n\nAll vaccines are provided free of charge by the Ministry of Health and Child Care at public clinics nationwide."
            }
        }
        val disclaimer = "\n\n*Reminder: This information is for educational purposes only and does not replace professional medical advice. Please consult your local clinic or pediatrician for clinical evaluations.*"
        return content + disclaimer
    }

    fun retryLastMessage() {
        val lastUserMsg = _uiState.value.messages.findLast { it.isUser }
        if (lastUserMsg != null) {
            sendMessage(textOverride = lastUserMsg.text, isVoice = lastUserMsg.isVoice)
        }
    }

    fun clearChat() {
        tts?.stop()
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.resetSession()
        }
        _uiState.update {
            it.copy(
                messages = listOf(
                    ChatMessage(
                        text = "Chat history cleared. How can I assist you with your child's immunization or health tracking today?\n\n*Note: Guidance provided is for informational purposes only. Please consult your local clinic or pediatrician for medical decisions.*",
                        isUser = false,
                        senderName = "CITS Assistant",
                        modelUsed = it.selectedModel
                    )
                ),
                errorMessage = null,
                isGenerating = false
            )
        }
        initAiEngine(role = _uiState.value.selectedRole, modelName = _uiState.value.selectedModel)
    }

    // --- Voice Conversations Methods ---

    fun toggleVoiceMode() {
        val newVoiceMode = !_uiState.value.isVoiceModeActive
        _uiState.update { it.copy(isVoiceModeActive = newVoiceMode) }
        if (newVoiceMode) {
            startSpeechListening()
        } else {
            stopSpeechListening()
            stopSpeaking()
        }
    }

    fun startSpeechListening() {
        val app = getApplication<Application>()
        if (!SpeechRecognizer.isRecognitionAvailable(app)) {
            Log.w(TAG, "Speech recognition is not available on this device")
            _uiState.update { it.copy(isListening = false, voiceTranscript = "Speech recognition unavailable") }
            return
        }

        try {
            stopSpeechListening()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(app).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _uiState.update { it.copy(isListening = true, voiceTranscript = "Listening...") }
                    }

                    override fun onBeginningOfSpeech() {
                        _uiState.update { it.copy(isListening = true, voiceTranscript = "Hearing audio...") }
                    }

                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        _uiState.update { it.copy(isListening = false, voiceTranscript = "Processing voice...") }
                    }

                    override fun onError(error: Int) {
                        Log.w(TAG, "SpeechRecognizer error: $error")
                        _uiState.update { it.copy(isListening = false, voiceTranscript = "") }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()
                        if (!text.isNullOrBlank()) {
                            _uiState.update { it.copy(isListening = false, voiceTranscript = text) }
                            // Auto-send voice input
                            sendMessage(textOverride = text, isVoice = true)
                        } else {
                            _uiState.update { it.copy(isListening = false, voiceTranscript = "") }
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull()
                        if (!partial.isNullOrBlank()) {
                            _uiState.update { it.copy(voiceTranscript = partial) }
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start speech listening: ${e.message}")
            _uiState.update { it.copy(isListening = false) }
        }
    }

    fun stopSpeechListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (ignore: Exception) {}
        _uiState.update { it.copy(isListening = false) }
    }

    fun speakText(text: String) {
        if (!isTtsReady || text.isBlank()) return
        try {
            // Strip markdown asterisks or bullet signs for cleaner speech
            val cleanText = text.replace("*", "").replace("#", "").take(400)
            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "UTTERANCE_ID")
            _uiState.update { it.copy(isSpeaking = true) }
        } catch (e: Exception) {
            Log.e(TAG, "TTS speak failed: ${e.message}")
        }
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (ignore: Exception) {}
        _uiState.update { it.copy(isSpeaking = false) }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            stopSpeechListening()
            tts?.stop()
            tts?.shutdown()
        } catch (ignore: Exception) {}
    }
}
