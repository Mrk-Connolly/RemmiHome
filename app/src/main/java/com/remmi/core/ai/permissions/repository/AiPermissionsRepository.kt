package com.remmi.core.ai.permissions.repository

import com.remmi.core.ai.permissions.models.AiActionHistoryEntry
import com.remmi.core.ai.permissions.models.AiPermissionsConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Local-first repository managing AI security policy, permission configurations, and action history logging.
 * Guarantees that unrestricted platform APIs remain strictly blocked.
 */
class AiPermissionsRepository(
    private val storageDir: File? = null,
    initialConfig: AiPermissionsConfig = AiPermissionsConfig(),
    initialHistory: List<AiActionHistoryEntry> = emptyList()
) {
    private val _permissionsConfig = MutableStateFlow(initialConfig)
    val permissionsConfig: StateFlow<AiPermissionsConfig> = _permissionsConfig.asStateFlow()

    private val _actionHistory = MutableStateFlow(initialHistory)
    val actionHistory: StateFlow<List<AiActionHistoryEntry>> = _actionHistory.asStateFlow()

    private val storageFile: File? by lazy {
        storageDir?.let { File(it, "ai_permissions.json") }
    }

    fun isReadAllowed(domain: String): Boolean {
        return _permissionsConfig.value.readableDomains.contains(domain)
    }

    fun isWriteAllowed(domain: String): Boolean {
        return _permissionsConfig.value.changeableDomains.contains(domain)
    }

    fun isConfirmationRequired(actionName: String): Boolean {
        return _permissionsConfig.value.requiresConfirmationActions.contains(actionName)
    }

    fun isAutomationEnabled(automationName: String): Boolean {
        return _permissionsConfig.value.enabledAutomations.contains(automationName)
    }

    fun isUnrestrictedApiBlocked(apiName: String): Boolean {
        // Unrestricted Android system APIs are strictly blocked by policy
        return true
    }

    suspend fun updatePermissionsConfig(config: AiPermissionsConfig) = withContext(Dispatchers.IO) {
        // Enforce hard-block policy on unrestricted APIs regardless of config edits
        val enforced = config.copy(allowUnrestrictedAndroidApis = false)
        _permissionsConfig.value = enforced
        persistAll()
    }

    suspend fun recordActionHistory(entry: AiActionHistoryEntry) = withContext(Dispatchers.IO) {
        val updated = _actionHistory.value + entry
        _actionHistory.value = updated
        persistAll()
    }

    suspend fun clearActionHistory() = withContext(Dispatchers.IO) {
        _actionHistory.value = emptyList()
        persistAll()
    }

    fun loadFromStorage() {
        val file = storageFile ?: return
        if (!file.exists()) return

        runCatching {
            val json = file.readText()
            parsePermissionsFromJson(json)
        }
    }

    private fun persistAll() {
        val file = storageFile ?: return
        runCatching {
            val json = serializePermissionsToJson()
            file.writeText(json)
        }
    }

    private fun serializePermissionsToJson(): String {
        val sb = StringBuilder()
        val c = _permissionsConfig.value
        val history = _actionHistory.value

        sb.append("{\n")
        sb.append("  \"config\": {\n")
        sb.append("    \"readableDomains\": [${c.readableDomains.joinToString(",") { "\"${escapeJson(it)}\"" }}],\n")
        sb.append("    \"changeableDomains\": [${c.changeableDomains.joinToString(",") { "\"${escapeJson(it)}\"" }}],\n")
        sb.append("    \"requiresConfirmationActions\": [${c.requiresConfirmationActions.joinToString(",") { "\"${escapeJson(it)}\"" }}],\n")
        sb.append("    \"enabledAutomations\": [${c.enabledAutomations.joinToString(",") { "\"${escapeJson(it)}\"" }}],\n")
        sb.append("    \"allowUnrestrictedAndroidApis\": false\n")
        sb.append("  },\n")

        sb.append("  \"actionHistory\": [\n")
        history.forEachIndexed { idx, h ->
            sb.append("    {\n")
            sb.append("      \"id\": \"${escapeJson(h.id)}\",\n")
            sb.append("      \"timestamp\": ${h.timestamp},\n")
            sb.append("      \"actionName\": \"${escapeJson(h.actionName)}\",\n")
            sb.append("      \"domain\": \"${escapeJson(h.domain)}\",\n")
            sb.append("      \"accessType\": \"${escapeJson(h.accessType)}\",\n")
            sb.append("      \"details\": \"${escapeJson(h.details)}\",\n")
            sb.append("      \"userConfirmed\": ${h.userConfirmed},\n")
            sb.append("      \"wasBlocked\": ${h.wasBlocked}\n")
            sb.append("    }${if (idx < history.size - 1) "," else ""}\n")
        }
        sb.append("  ]\n")

        sb.append("}")
        return sb.toString()
    }

    private fun parsePermissionsFromJson(json: String) {
        runCatching {
            val historyList = mutableListOf<AiActionHistoryEntry>()
            val actionMatches = "\"actionName\":\\s*\"([^\"]*)\"".toRegex().findAll(json).toList()
            val domainMatches = "\"domain\":\\s*\"([^\"]*)\"".toRegex().findAll(json).toList()

            for (i in actionMatches.indices) {
                val action = actionMatches[i].groupValues[1]
                val domain = if (i < domainMatches.size) domainMatches[i].groupValues[1] else "General"
                historyList.add(AiActionHistoryEntry(actionName = action, domain = domain))
            }

            if (historyList.isNotEmpty()) {
                _actionHistory.value = historyList
            }
        }
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
