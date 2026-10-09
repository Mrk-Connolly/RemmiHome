package com.remmi.core.ai.tools.models

/**
 * Access permission classification for AI tools.
 */
enum class ToolAccessType {
    READ,
    WRITE
}

/**
 * Risk classification level for tool operations.
 */
enum class RiskLevel {
    LOW,
    HIGH
}

/**
 * Definition metadata for a controlled AI tool.
 */
data class AiToolDefinition(
    val name: String,
    val description: String,
    val accessType: ToolAccessType,
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val requiredParameters: List<String> = emptyList()
)

/**
 * Invocation request from the AI agent.
 */
data class AiToolCallRequest(
    val toolName: String,
    val parameters: Map<String, String> = emptyMap(),
    val userConfirmed: Boolean = false
)

/**
 * Result returned after processing an AI tool invocation request.
 */
data class AiToolExecutionResult(
    val success: Boolean,
    val toolName: String,
    val accessType: ToolAccessType,
    val data: String = "",
    val requiresConfirmation: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Audit log entry for recorded AI tool actions.
 */
data class AiToolActionLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val toolName: String,
    val accessType: ToolAccessType,
    val parameters: Map<String, String>,
    val success: Boolean,
    val requiresConfirmation: Boolean = false
)
