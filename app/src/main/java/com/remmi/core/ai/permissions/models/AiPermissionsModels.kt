package com.remmi.core.ai.permissions.models

import java.util.UUID

/**
 * Permission configuration for the AI Life Management System.
 *
 * @property readableDomains Set of domain names AI is permitted to read.
 * @property changeableDomains Set of domain names AI is permitted to modify.
 * @property requiresConfirmationActions Set of action/tool names that require explicit user confirmation.
 * @property enabledAutomations Set of background AI automations currently enabled.
 * @property allowUnrestrictedAndroidApis Policy flag hardcoded to false to block raw unrestricted platform APIs.
 */
data class AiPermissionsConfig(
    val readableDomains: Set<String> = setOf("Calendar", "Tasks", "Academic", "Health", "Habits", "Installed Apps"),
    val changeableDomains: Set<String> = setOf("Calendar", "Tasks", "Academic", "Health", "Habits"),
    val requiresConfirmationActions: Set<String> = setOf("delete_event", "delete_task", "delete_habit", "format_data"),
    val enabledAutomations: Set<String> = setOf("Daily Briefing", "Next Best Action", "Proactive Warnings", "Pattern Analysis"),
    val allowUnrestrictedAndroidApis: Boolean = false
)

/**
 * Historical log entry recording an executed or blocked AI action.
 *
 * @property id Unique stable identifier.
 * @property timestamp Epoch timestamp when action was recorded.
 * @property actionName Name of the action or tool invoked.
 * @property domain Target domain category.
 * @property accessType Access type ("READ" or "WRITE").
 * @property details Human-readable parameters or summary.
 * @property userConfirmed Whether explicit user confirmation was provided.
 * @property wasBlocked Whether the action was blocked by security policy.
 */
data class AiActionHistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val actionName: String,
    val domain: String,
    val accessType: String = "READ",
    val details: String = "",
    val userConfirmed: Boolean = false,
    val wasBlocked: Boolean = false
)
