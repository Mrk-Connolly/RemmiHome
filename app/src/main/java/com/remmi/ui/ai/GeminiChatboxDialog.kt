package com.remmi.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remmi.core.ai.chat.ChatMessage
import com.remmi.core.ai.chat.ChatSender
import com.remmi.core.ai.chat.GeminiChatboxEngine
import com.remmi.core.ai.chat.LogCategory
import com.remmi.core.controller.RemmiController
import com.remmi.ui.theme.remmiColors
import kotlinx.coroutines.launch

@Composable
fun GeminiChatboxDialog(
    chatEngine: GeminiChatboxEngine,
    controller: RemmiController? = null,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.remmiColors
    val messages by chatEngine.messages.collectAsState()
    val isGenerating by chatEngine.isGenerating.collectAsState()
    val selectedLogCategory by chatEngine.selectedLogCategory.collectAsState()

    var inputPrompt by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val handleSend: () -> Unit = {
        if (inputPrompt.isNotBlank()) {
            val textToSend = inputPrompt
            inputPrompt = ""
            focusManager.clearFocus()
            coroutineScope.launch {
                chatEngine.sendMessage(
                    input = textToSend,
                    commandExecutor = controller?.let { c -> { cmd -> c.executeCommand(cmd) } }
                )
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colors.accent.copy(alpha = 0.2f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("✨", fontSize = 20.sp)
                        }
                    }
                    Column {
                        Text(
                            text = "Gemini AI Chatbox",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent
                        )
                        Text(
                            text = "Interactive Assistant & Log Reader",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                }
                TextButton(onClick = { chatEngine.clearHistory() }) {
                    Text("Clear", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Log Attachment Category Chips
                Text(
                    text = "Attach Log Context:",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    items(LogCategory.values()) { category ->
                        val isSelected = selectedLogCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                chatEngine.selectLogCategory(if (isSelected) null else category)
                            },
                            label = {
                                Text(
                                    text = "${category.iconSymbol} ${category.displayName}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.accent.copy(alpha = 0.25f),
                                selectedLabelColor = colors.accent
                            )
                        )
                    }
                }

                // Chat Message Stream
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatMessageBubble(
                            message = msg,
                            onExecuteCommand = controller?.let { c ->
                                { cmd -> c.executeCommand(cmd) }
                            }
                        )
                    }
                }

                if (isGenerating) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = colors.accent,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Gemini is analyzing...",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Input Bar
                OutlinedTextField(
                    value = inputPrompt,
                    onValueChange = { inputPrompt = it },
                    placeholder = { Text("Ask Gemini or analyze logs...", color = colors.textMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.divider
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = handleSend,
                            enabled = inputPrompt.isNotBlank()
                        ) {
                            Text(
                                text = "➔",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (inputPrompt.isNotBlank()) colors.accent else colors.textMuted
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { handleSend() })
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = colors.accent)
            }
        }
    )

    val activeDraft by chatEngine.activeDraft.collectAsState()
    activeDraft?.let { draft ->
        PendingActionConfirmDialog(
            draftAction = draft,
            onConfirm = { finalDraft ->
                controller?.let { c ->
                    chatEngine.confirmAndSaveDraft(finalDraft) { cmd ->
                        c.executeCommand(cmd)
                    }
                }
            },
            onDismiss = {
                chatEngine.dismissDraft()
            }
        )
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    onExecuteCommand: ((com.remmi.core.eventBus.commands.RemmiCommand) -> com.remmi.core.eventBus.commands.RemmiCommandResult)?
) {
    val colors = MaterialTheme.remmiColors

    val isUser = message.sender == ChatSender.USER
    val isSystem = message.sender == ChatSender.SYSTEM

    val align = if (isUser) Alignment.End else Alignment.Start
    val bgColor = when {
        isUser -> colors.accent.copy(alpha = 0.2f)
        isSystem -> colors.surfaceSubtle
        else -> colors.surface
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(containerColor = bgColor),
            modifier = Modifier.fillMaxWidth(if (isSystem) 1f else 0.92f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = when (message.sender) {
                            ChatSender.USER -> "You"
                            ChatSender.AI -> "✨ Gemini"
                            ChatSender.SYSTEM -> "ℹ️ System"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) colors.accent else colors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary
                )

                // Display Log Attachment Badge if present
                message.attachedLogSnapshot?.let { snapshot ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.accent.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "📎 Attached: ${snapshot.summaryText}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.accent,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }

                // Display Command Execution Button if command is resolved
                message.resolvedCommand?.let { cmd ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onExecuteCommand?.invoke(cmd) },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚡ Execute Resolved Action",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
