package com.matshelela.childimmunisationtrackingsystemappv1.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.Chat
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.TextPart
import com.google.firebase.ai.type.generationConfig
import com.matshelela.childimmunisationtrackingsystemappv1.data.firebase.FirebaseConfigParameters
import com.matshelela.childimmunisationtrackingsystemappv1.data.firebase.FirebaseInitializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Sealed class representing UI state transitions for multi-turn chat interactions:
 * - Idle: Session ready for user input.
 * - Loading: Request dispatched, receiving streaming chunks.
 * - Success: AI completed full generation.
 * - Error: Exception encountered (network, timeout, auth).
 */
sealed interface ChatSessionState {
    object Idle : ChatSessionState
    data class Loading(val currentChunkText: String = "") : ChatSessionState
    data class Success(val responseText: String) : ChatSessionState
    data class Error(val error: Throwable, val friendlyMessage: String) : ChatSessionState
}

/**
 * Clean Architecture repository contract for managing Firebase AI Logic sessions.
 */
interface ChatRepository {
    val sessionState: StateFlow<ChatSessionState>
    suspend fun initializeSession(context: Context): FirebaseConfigParameters
    fun sendUserMessageStream(userMessage: String): Flow<String>
    suspend fun resetSession()
}

/**
 * Production implementation of ChatRepository using Firebase AI Logic SDK with App Check.
 */
class FirebaseAiChatRepository(
    private val context: Context
) : ChatRepository {

    private val TAG = "FirebaseAiChatRepo"

    private val _sessionState = MutableStateFlow<ChatSessionState>(ChatSessionState.Idle)
    override val sessionState: StateFlow<ChatSessionState> = _sessionState.asStateFlow()

    private var activeGenerativeModel: GenerativeModel? = null
    private var activeChatSession: Chat? = null
    private var currentConfig: FirebaseConfigParameters = FirebaseConfigParameters()

    /**
     * Initializes the chat session using parameters from Firebase Remote Config
     * and protected by Firebase App Check.
     */
    override suspend fun initializeSession(context: Context): FirebaseConfigParameters = withContext(Dispatchers.IO) {
        val config = FirebaseInitializer.initializeAsync(context)
        currentConfig = config
        setupGenerativeModel(config)
        config
    }

    private fun setupGenerativeModel(config: FirebaseConfigParameters) {
        try {
            val app = if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context) ?: FirebaseApp.getInstance()
            } else {
                FirebaseApp.getInstance()
            }

            val firebaseAi = FirebaseAI.getInstance(app)
            val model = firebaseAi.generativeModel(
                modelName = config.modelName,
                generationConfig = generationConfig {
                    temperature = config.temperature
                    topP = 0.95f
                },
                systemInstruction = Content(
                    role = "system",
                    parts = listOf(TextPart(config.systemInstruction))
                )
            )

            activeGenerativeModel = model
            activeChatSession = model.startChat()
            Log.d(TAG, "Chat session successfully configured with model: ${config.modelName}")
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring generative model: ${e.message}", e)
        }
    }

    /**
     * Sends a user message and yields streamed text chunks as they arrive from Firebase AI Logic.
     * Fully offloaded to Dispatchers.IO to maintain 60 FPS UI performance.
     */
    override fun sendUserMessageStream(userMessage: String): Flow<String> = flow {
        if (userMessage.isBlank()) return@flow

        _sessionState.value = ChatSessionState.Loading("")
        val responseBuilder = StringBuilder()

        try {
            val chat = activeChatSession ?: run {
                // Lazy initialize if not yet ready
                setupGenerativeModel(currentConfig)
                activeChatSession ?: throw IllegalStateException("Firebase AI ChatSession could not be initialized")
            }

            // Stream response chunks from Firebase AI Logic
            val chunkFlow: Flow<GenerateContentResponse> = chat.sendMessageStream(userMessage)

            chunkFlow.collect { chunk ->
                val text = chunk.text
                if (!text.isNullOrEmpty()) {
                    responseBuilder.append(text)
                    _sessionState.value = ChatSessionState.Loading(responseBuilder.toString())
                    emit(text)
                }
            }

            val fullText = responseBuilder.toString()
            if (fullText.isNotBlank()) {
                _sessionState.value = ChatSessionState.Success(fullText)
            } else {
                val fallbackText = "I received your query. Please visit your local clinic for direct consultation."
                _sessionState.value = ChatSessionState.Success(fallbackText)
                emit(fallbackText)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Streaming failure in ChatRepository: ${e.message}", e)
            val friendlyMsg = "Unable to reach the AI immunization service (${e.localizedMessage ?: "Network issue"}). Please check your connection or consult your healthcare provider."
            _sessionState.value = ChatSessionState.Error(e, friendlyMsg)
            throw e
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Resets the active chat session history to start a fresh multi-turn conversation.
     */
    override suspend fun resetSession() = withContext(Dispatchers.IO) {
        _sessionState.value = ChatSessionState.Idle
        setupGenerativeModel(currentConfig)
    }
}
