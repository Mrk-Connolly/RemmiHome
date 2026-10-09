package com.remmi.core.ai.chat

import com.remmi.core.ai.context.ContextDomain
import com.remmi.core.ai.context.GeminiContextBuilder
import com.remmi.core.ai.permissions.repository.AiPermissionsRepository
import com.remmi.plugins.knowledge.lecturerecorder.diagnostics.RecordingDiagnosticsState

enum class LogCategory(val displayName: String, val iconSymbol: String) {
    ALL("All Logs & Diagnostics", "🔍"),
    DIAGNOSTICS("Live & Network Diagnostics", "🎙️"),
    SYSTEM_CONTEXT("System Context & Facts", "📊"),
    ACTION_HISTORY("AI Action History", "📜")
}

data class SystemLogSnapshot(
    val category: LogCategory,
    val timestampIso: String,
    val summaryText: String,
    val detailedLines: List<String>
)

/**
 * Log reader capability gathering system state, live recording telemetry,
 * and structured context facts for Gemini Chatbox log analysis.
 */
class GeminiLogReader(
    private val diagnosticsState: RecordingDiagnosticsState? = null,
    private val contextBuilder: GeminiContextBuilder? = null,
    private val permissionsRepo: AiPermissionsRepository? = null
) {

    fun captureSnapshot(
        category: LogCategory = LogCategory.ALL,
        timestampIso: String = "2026-10-07T18:15:00"
    ): SystemLogSnapshot {
        val lines = mutableListOf<String>()

        if (category == LogCategory.ALL || category == LogCategory.DIAGNOSTICS) {
            lines.add("=== LIVE & NETWORK DIAGNOSTICS ===")
            diagnosticsState?.let { diag ->
                lines.add("Gemini Model: ${diag.geminiModel.value}")
                lines.add("Gemini Endpoint: ${diag.geminiEndpoint.value}")
                lines.add("Connect State: ${diag.geminiConnectState.value}")
                lines.add("WebSocket State: ${diag.webSocketState.value}")
                lines.add("Setup Handshake: ${diag.setupHandshakeState.value}")
                lines.add("Audio Chunks Sent: ${diag.buffersSent.value} / Captured: ${diag.buffersCapturedCount.value}")
                lines.add("Last WS Error: ${diag.lastWebSocketError.value.ifBlank { "None" }}")
                lines.add("Last Close Code: ${diag.webSocketCloseCode.value} Reason: ${diag.webSocketCloseReason.value.ifBlank { "None" }}")
            } ?: lines.add("No active recording diagnostics attached.")
            lines.add("")
        }

        if (category == LogCategory.ALL || category == LogCategory.SYSTEM_CONTEXT) {
            lines.add("=== SYSTEM CONTEXT & FACTS ===")
            contextBuilder?.let { builder ->
                val generalCtx = builder.buildContext(ContextDomain.GENERAL)
                val dailyCtx = builder.buildContext(ContextDomain.DAILY_PLANNING)

                if (generalCtx.facts.isNotEmpty()) {
                    lines.add("[FACTS]")
                    generalCtx.facts.forEach { lines.add("- $it") }
                }
                if (dailyCtx.currentState.isNotEmpty()) {
                    lines.add("[DAILY STATE & TASKS]")
                    dailyCtx.currentState.forEach { lines.add("- $it") }
                }
                if (dailyCtx.goals.isNotEmpty()) {
                    lines.add("[PENDING GOALS]")
                    dailyCtx.goals.forEach { lines.add("- $it") }
                }
            } ?: lines.add("No structured context builder attached.")
            lines.add("")
        }

        if (category == LogCategory.ALL || category == LogCategory.ACTION_HISTORY) {
            lines.add("=== AI ACTION HISTORY ===")
            permissionsRepo?.let { repo ->
                val history = repo.actionHistory.value
                if (history.isEmpty()) {
                    lines.add("No recent AI actions recorded.")
                } else {
                    history.take(10).forEach { entry ->
                        val status = if (entry.wasBlocked) "BLOCKED" else if (entry.userConfirmed) "CONFIRMED" else "EXECUTED"
                        lines.add("${entry.timestamp} | Action: ${entry.actionName} (${entry.domain}) | Access: ${entry.accessType} | Status: $status | Details: ${entry.details}")
                    }
                }
            } ?: lines.add("No permissions/action history repository attached.")
            lines.add("")
        }

        val summary = "Captured ${lines.size} log lines for category '${category.displayName}' at $timestampIso"
        return SystemLogSnapshot(
            category = category,
            timestampIso = timestampIso,
            summaryText = summary,
            detailedLines = lines
        )
    }

    fun buildLogPromptPrefix(snapshot: SystemLogSnapshot): String {
        return buildString {
            appendLine("The user has attached the following system and diagnostic logs to their query:")
            appendLine("LOG TIMESTAMP: ${snapshot.timestampIso}")
            appendLine("LOG CATEGORY: ${snapshot.category.displayName}")
            appendLine("--- LOG CONTENT BEGIN ---")
            snapshot.detailedLines.forEach { appendLine(it) }
            appendLine("--- LOG CONTENT END ---")
            appendLine("Please analyze these logs if relevant to the user's question, identify any issues or errors, and provide a clear, helpful response.")
        }
    }
}
