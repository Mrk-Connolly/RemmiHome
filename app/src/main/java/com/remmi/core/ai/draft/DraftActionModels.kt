package com.remmi.core.ai.draft

import com.remmi.core.eventBus.commands.RemmiCommand
import java.util.UUID

enum class DraftActionType(val displayName: String, val iconSymbol: String) {
    CREATE_TASK("New Task Draft", "✅"),
    CREATE_CALENDAR_EVENT("New Calendar Event Draft", "📅"),
    CREATE_INGREDIENT("New Food Ingredient Draft", "🥦"),
    CREATE_ALARM("New Alarm Draft", "⏰")
}

data class PendingDraftAction(
    val id: String = UUID.randomUUID().toString(),
    val type: DraftActionType,
    val title: String,
    val description: String = "",
    val dateIso: String? = null,
    val timeIso: String? = null,
    val category: String? = null,
    val commandToExecute: RemmiCommand
)
