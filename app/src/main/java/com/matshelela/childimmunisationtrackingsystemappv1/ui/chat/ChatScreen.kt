package com.matshelela.childimmunisationtrackingsystemappv1.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onSendMessage: (String) -> Unit,
    onInputTextChanged: (String) -> Unit,
    onRetry: () -> Unit,
    onClearChat: () -> Unit,
    onRoleSelected: (ImmunisationBotRole) -> Unit,
    onModelSelected: (String) -> Unit,
    onToggleVoiceMode: () -> Unit,
    onStartSpeechListening: () -> Unit,
    onStopSpeechListening: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    BackHandler(onBack = onBack)

    // Automatically scroll to bottom when messages change or streaming updates arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Also auto-scroll on last message chunk update
    val lastMessageTextLength = uiState.messages.lastOrNull()?.text?.length ?: 0
    LaunchedEffect(lastMessageTextLength) {
        if (uiState.messages.isNotEmpty() && uiState.isGenerating) {
            listState.scrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = "MOHCC AI Assistant",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "EPI AI Advisor",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (uiState.isOnline) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                                ) {
                                    Text(
                                        text = if (uiState.isOnline) "Live AI" else "Offline",
                                        color = if (uiState.isOnline) Color(0xFF166534) else Color(0xFF991B1B),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = uiState.selectedRole.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to previous screen"
                        )
                    }
                },
                actions = {
                    // Voice mode quick toggle
                    IconButton(
                        onClick = onToggleVoiceMode,
                        modifier = Modifier.testTag("voice_mode_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isVoiceModeActive) Icons.Default.PlayArrow else Icons.Default.Call,
                            contentDescription = "Toggle Voice Conversation",
                            tint = if (uiState.isVoiceModeActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Clear chat
                    IconButton(
                        onClick = onClearChat,
                        modifier = Modifier.testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Clear Chat History"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8FAFC)) // Clean Medical Soft Slate background
        ) {
            // Engine and Role status bar
            RoleAndModelBar(
                selectedRole = uiState.selectedRole,
                selectedModel = uiState.selectedModel,
                connectionEngine = uiState.connectionEngine,
                onRoleSelected = onRoleSelected,
                onModelSelected = onModelSelected
            )

            // Voice Conversation Live Banner (when active)
            AnimatedVisibility(visible = uiState.isVoiceModeActive) {
                VoiceConversationBanner(
                    isListening = uiState.isListening,
                    isSpeaking = uiState.isSpeaking,
                    transcript = uiState.voiceTranscript,
                    onStartListening = onStartSpeechListening,
                    onStopListening = onStopSpeechListening
                )
            }

            // Quick suggestion prompts
            QuickSuggestionsRow(
                onSuggestionClicked = { suggestion ->
                    onSendMessage(suggestion)
                }
            )

            // Chat Message List (Jetpack Compose LazyColumn with key optimization)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = uiState.messages,
                    key = { it.id } // Keyed to message ID to optimize recomposition performance and memory layout
                ) { message ->
                    ChatBubbleItem(
                        message = message,
                        onRetry = onRetry
                    )
                }

                // Streaming / Generating Indicator
                if (uiState.isGenerating && uiState.messages.lastOrNull()?.status != MessageStatus.STREAMING) {
                    item(key = "generating_indicator") {
                        StreamingIndicatorBubble()
                    }
                }
            }

            // Input Bar
            ChatInputBar(
                inputText = uiState.inputText,
                isGenerating = uiState.isGenerating,
                onInputTextChanged = onInputTextChanged,
                onSend = { onSendMessage(uiState.inputText) },
                onMicClick = onToggleVoiceMode
            )
        }
    }
}

/**
 * Top control row for selecting Chatbot Roles & Gemini Models
 */
@Composable
private fun RoleAndModelBar(
    selectedRole: ImmunisationBotRole,
    selectedModel: String,
    connectionEngine: String,
    onRoleSelected: (ImmunisationBotRole) -> Unit,
    onModelSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(bottom = 6.dp)
    ) {
        // System roles chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ImmunisationBotRole.values().forEach { role ->
                val isSelected = selectedRole == role
                FilterChip(
                    selected = isSelected,
                    onClick = { onRoleSelected(role) },
                    label = {
                        Text(
                            text = role.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = when (role) {
                                ImmunisationBotRole.PEDIATRIC_ADVISOR -> Icons.Default.Favorite
                                ImmunisationBotRole.SCHEDULE_ASSISTANT -> Icons.Default.DateRange
                                ImmunisationBotRole.CLINICAL_PROTOCOL -> Icons.Default.Info
                            },
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    modifier = Modifier.testTag("role_chip_${role.name.lowercase()}")
                )
            }
        }

        // Models chips row: gemini-3.1-pro-preview, gemini-3.5-flash, gemini-3.1-flash-lite
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Model:",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val availableModels = listOf(
                "gemini-3.5-flash" to "3.5 Flash (General)",
                "gemini-3.1-pro-preview" to "3.1 Pro (Complex Clinical)",
                "gemini-3.1-flash-lite-preview" to "Flash-Lite (Fast Catch-Up)",
                "gemini-3.8-live" to "3.8 Live (Voice)"
            )

            availableModels.forEach { (modelId, displayLabel) ->
                val isSelected = selectedModel == modelId
                SuggestionChip(
                    onClick = { onModelSelected(modelId) },
                    label = {
                        Text(
                            text = displayLabel,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.height(28.dp)
                )
            }
        }

        // Connection Engine Subtitle
        Text(
            text = connectionEngine,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )
    }
}

/**
 * Voice conversation live visualizer banner
 */
@Composable
private fun VoiceConversationBanner(
    isListening: Boolean,
    isSpeaking: Boolean,
    transcript: String,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pulsing voice circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isListening) Color(0xFFEF4444)
                        else if (isSpeaking) Color(0xFF3B82F6)
                        else Color(0xFF10B981)
                    )
                    .clickable {
                        if (isListening) onStopListening() else onStartListening()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.PlayArrow else Icons.Default.Call,
                    contentDescription = "Mic Status",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        isListening -> "Live Listening (Speak your query)..."
                        isSpeaking -> "AI Advisor Speaking Response..."
                        else -> "Voice Mode Ready • Tap to Speak"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                if (transcript.isNotEmpty()) {
                    Text(
                        text = transcript,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = {
                    if (isListening) onStopListening() else onStartListening()
                }
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Close else Icons.Default.PlayArrow,
                    contentDescription = "Toggle speech",
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * Quick Suggestion Pills for Zimbabwe Immunization Context
 */
@Composable
private fun QuickSuggestionsRow(onSuggestionClicked: (String) -> Unit) {
    val suggestions = listOf(
        "What vaccines are due at 6 weeks?",
        "Mild fever after Pentavalent 1",
        "Missed 10-week dose catch-up plan",
        "BCG scar didn't form, what next?",
        "When is Measles-Rubella given in Zimbabwe?"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEach { prompt ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.clickable { onSuggestionClicked(prompt) }
            ) {
                Text(
                    text = prompt,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * Individual Chat Bubble Item with clean styling and custom keying
 */
@Composable
private fun ChatBubbleItem(
    message: ChatMessage,
    onRetry: () -> Unit
) {
    val isUser = message.isUser
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.Top)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    message.status == MessageStatus.ERROR -> Color(0xFFFEE2E2)
                    isUser -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surface
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .widthIn(max = 300.dp)
                .testTag(if (isUser) "user_chat_bubble" else "assistant_chat_bubble")
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header if from Assistant
                if (!isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = message.senderName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        message.modelUsed?.let { model ->
                            Text(
                                text = model.replace("gemini-", "").replace("-preview", ""),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Message Text Content
                Text(
                    text = if (message.text.isEmpty() && message.status == MessageStatus.STREAMING) {
                        "Thinking..."
                    } else {
                        message.text
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        message.status == MessageStatus.ERROR -> Color(0xFF991B1B)
                        isUser -> Color.White
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    lineHeight = 20.sp
                )

                // Error Retry button
                if (message.status == MessageStatus.ERROR) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tap to Retry", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Timestamp and Status
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeFormat.format(Date(message.timestamp)),
                        fontSize = 9.sp,
                        color = if (isUser) Color.White.copy(alpha = 0.7f) else Color.Gray
                    )
                    if (isUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Sent",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(12.dp)
                        )
                    } else if (message.status == MessageStatus.STREAMING) {
                        Spacer(modifier = Modifier.width(4.dp))
                        CircularProgressIndicator(
                            strokeWidth = 1.5.dp,
                            modifier = Modifier.size(10.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Animated Streaming typing dots indicator
 */
@Composable
private fun StreamingIndicatorBubble() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(6.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.padding(4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "AI is drafting clinical advice...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(14.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Chat Input Bottom Bar with Voice / Text integration
 */
@Composable
private fun ChatInputBar(
    inputText: String,
    isGenerating: Boolean,
    onInputTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Voice Mic Shortcut Button
            IconButton(
                onClick = onMicClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("chat_mic_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Voice Input",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Text Input Field
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChanged,
                placeholder = {
                    Text(
                        "Ask about vaccines or schedules...",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp, max = 120.dp)
                    .testTag("chat_text_input"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                ),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Send Button
            FilledIconButton(
                onClick = onSend,
                enabled = inputText.isNotBlank() && !isGenerating,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("chat_send_button"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color(0xFFCBD5E1)
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send message",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
