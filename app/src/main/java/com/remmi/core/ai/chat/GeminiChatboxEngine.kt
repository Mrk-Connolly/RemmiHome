package com.remmi.core.ai.chat

import com.remmi.core.ai.RemmiAiCapability
import com.remmi.core.ai.draft.DraftActionType
import com.remmi.core.ai.draft.PendingDraftAction
import com.remmi.core.eventBus.commands.RemmiCommand
import com.remmi.core.eventBus.commands.RemmiCommandResult
import com.remmi.plugins.planning.alarms.commands.CreateAlarmCommand
import com.remmi.plugins.planning.calendar.commands.CreateCalendarEventCommand
import com.remmi.plugins.planning.tasks.commands.CreateTaskCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class ChatSender {
    USER,
    AI,
    SYSTEM
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: ChatSender,
    val content: String,
    val timestampIso: String = "2026-10-07T18:15:00",
    val attachedLogSnapshot: SystemLogSnapshot? = null,
    val resolvedCommand: RemmiCommand? = null,
    val pendingDraft: PendingDraftAction? = null
)

/**
 * Interactive Gemini Chatbox engine managing multi-turn conversation history,
 * log attachment injection, draft action creation, and Core write security boundaries.
 */
class GeminiChatboxEngine(
    private val aiCapability: RemmiAiCapability? = null,
    private val logReader: GeminiLogReader = GeminiLogReader()
) {

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = ChatSender.SYSTEM,
                content = "✨ Gemini Chatbox Ready! You can ask questions, analyze your system logs, or request new items (which will present a draft template for your confirmation)."
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _selectedLogCategory = MutableStateFlow<LogCategory?>(null)
    val selectedLogCategory: StateFlow<LogCategory?> = _selectedLogCategory.asStateFlow()

    private val _activeDraft = MutableStateFlow<PendingDraftAction?>(null)
    val activeDraft: StateFlow<PendingDraftAction?> = _activeDraft.asStateFlow()

    fun selectLogCategory(category: LogCategory?) {
        _selectedLogCategory.value = category
    }

    fun dismissDraft() {
        _activeDraft.value = null
    }

    fun confirmAndSaveDraft(
        draft: PendingDraftAction,
        commandExecutor: (RemmiCommand) -> RemmiCommandResult
    ): RemmiCommandResult {
        _activeDraft.value = null
        val result = commandExecutor(draft.commandToExecute)
        val resultMsg = when (result) {
            is RemmiCommandResult.Success -> "✅ Core successfully saved item '${draft.title}' to JSON storage."
            is RemmiCommandResult.Failure -> "❌ Core save failed: ${result.reason}"
        }
        _messages.value = _messages.value + ChatMessage(
            sender = ChatSender.SYSTEM,
            content = resultMsg
        )
        return result
    }

    suspend fun sendMessage(
        input: String,
        attachLogsCategory: LogCategory? = _selectedLogCategory.value,
        commandExecutor: ((RemmiCommand) -> RemmiCommandResult)? = null
    ): ChatMessage {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return _messages.value.last()
        }

        val snapshot = attachLogsCategory?.let { logReader.captureSnapshot(it) }

        // 1. Resolve possible system command
        val resolvedCmd = aiCapability?.resolveIntent(trimmed)

        // Check if command is a WRITE action requiring draft confirmation
        val isWriteAction = resolvedCmd is CreateTaskCommand ||
                resolvedCmd is CreateCalendarEventCommand ||
                resolvedCmd is CreateAlarmCommand

        var draft: PendingDraftAction? = null
        if (isWriteAction && resolvedCmd != null) {
            draft = when (resolvedCmd) {
                is CreateTaskCommand -> PendingDraftAction(
                    type = DraftActionType.CREATE_TASK,
                    title = resolvedCmd.task.title,
                    description = resolvedCmd.task.description ?: "",
                    commandToExecute = resolvedCmd
                )
                is CreateCalendarEventCommand -> PendingDraftAction(
                    type = DraftActionType.CREATE_CALENDAR_EVENT,
                    title = resolvedCmd.event.title,
                    description = resolvedCmd.event.description ?: "",
                    dateIso = resolvedCmd.event.startDateIso,
                    commandToExecute = resolvedCmd
                )
                is CreateAlarmCommand -> PendingDraftAction(
                    type = DraftActionType.CREATE_ALARM,
                    title = resolvedCmd.alarm.title,
                    timeIso = resolvedCmd.alarm.timeIso,
                    commandToExecute = resolvedCmd
                )
                else -> null
            }
        }

        // 2. Create User message
        val userMsg = ChatMessage(
            sender = ChatSender.USER,
            content = trimmed,
            attachedLogSnapshot = snapshot,
            resolvedCommand = if (!isWriteAction) resolvedCmd else null,
            pendingDraft = draft
        )

        _messages.value = _messages.value + userMsg
        _isGenerating.value = true

        if (draft != null) {
            _activeDraft.value = draft
        }

        try {
            val replyText = generateReplyText(trimmed, snapshot, resolvedCmd, isWriteAction, commandExecutor)
            val aiMsg = ChatMessage(
                sender = ChatSender.AI,
                content = replyText,
                resolvedCommand = if (!isWriteAction) resolvedCmd else null,
                pendingDraft = draft
            )
            _messages.value = _messages.value + aiMsg
            return aiMsg
        } finally {
            _isGenerating.value = false
        }
    }

    fun clearHistory() {
        _messages.value = listOf(
            ChatMessage(
                sender = ChatSender.SYSTEM,
                content = "History cleared. Gemini Chatbox is ready."
            )
        )
    }

    private fun generateReplyText(
        input: String,
        snapshot: SystemLogSnapshot?,
        resolvedCmd: RemmiCommand?,
        isWriteAction: Boolean,
        commandExecutor: ((RemmiCommand) -> RemmiCommandResult)?
    ): String {
        val lower = input.lowercase()

        // Handle WRITE action draft confirmation popup prompt
        if (isWriteAction) {
            return "📝 **Draft Template Created**: Gemini has generated a template for your item. Please review the confirmation popup and tap **'Confirm & Save'** to allow Core to execute the save."
        }

        // Handle READ / NAVIGATION explicit command execution if found
        if (resolvedCmd != null && commandExecutor != null) {
            val result = commandExecutor(resolvedCmd)
            return when (result) {
                is RemmiCommandResult.Success -> "Action executed successfully for '$input'."
                is RemmiCommandResult.Failure -> "Action failed: ${result.reason}"
            }
        }

        // Analyze attached logs if provided or requested
        if (snapshot != null || lower.contains("log") || lower.contains("diagnostics") || lower.contains("error") || lower.contains("status")) {
            val targetSnapshot = snapshot ?: logReader.captureSnapshot(LogCategory.ALL)
            val hasErrors = targetSnapshot.detailedLines.any { it.contains("Error") || it.contains("FAILED") || it.contains("REJECTED") }

            return buildString {
                append("📊 **Log Analysis for ${targetSnapshot.category.displayName}**:\n")
                if (hasErrors) {
                    append("⚠️ Identified potential issues or connection failures:\n")
                    targetSnapshot.detailedLines.filter { it.contains("Error") || it.contains("FAILED") || it.contains("REJECTED") || it.contains("Close") }
                        .forEach { append("• $it\n") }
                } else {
                    append("✅ All systems and telemetry channels in this snapshot appear healthy and active.\n")
                }
                append("\nCaptured ${targetSnapshot.detailedLines.size} log lines. Let me know if you want to inspect a specific service!")
            }
        }

        // Conversational Default
        return when {
            lower.contains("hello") || lower.contains("hi") -> "Hello! How can I assist you with Remmi today?"
            lower.contains("help") -> "I can analyze your system logs, launch apps, navigate workspaces, create task/event draft templates, or assist with daily planning. What would you like to do?"
            else -> "I processed your message: '$input'. You can attach system logs using the category chips below to have me analyze diagnostic or system state!"
        }
    }
}
