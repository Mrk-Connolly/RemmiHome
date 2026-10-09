package com.remmi.core.ai

import com.remmi.core.android.launcher.RemmiAppInfo
import com.remmi.core.eventBus.commands.LaunchAppCommand
import com.remmi.core.eventBus.commands.NavigateCommand
import com.remmi.core.eventBus.commands.RemmiCommand
import com.remmi.core.eventBus.commands.RemmiCommandResult
import com.remmi.plugins.planning.alarms.commands.CreateAlarmCommand
import com.remmi.plugins.planning.alarms.models.AlarmItem
import com.remmi.plugins.planning.calendar.commands.CreateCalendarEventCommand
import com.remmi.plugins.planning.calendar.models.CalendarEvent
import com.remmi.plugins.planning.tasks.commands.CreateTaskCommand
import com.remmi.plugins.planning.tasks.models.TaskItem
import java.time.LocalDate

/**
 * Default implementation of [RemmiAiCapability] isolating AI execution strictly behind
 * command resolution, safety validation, and explicit capability execution.
 */
class DefaultAiCapability(
    private val installedAppsProvider: () -> List<RemmiAppInfo> = { emptyList() },
    private val validator: RemmiAiCommandValidator = DefaultAiCommandValidator()
) : RemmiAiCapability {

    override fun resolveIntent(input: String): RemmiCommand? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        val lower = trimmed.lowercase()

        // Direct Shortcuts
        if (lower == "settings" || lower == "preferences" || lower == "open settings" || lower == "show settings") {
            return NavigateCommand("SETTINGS")
        }
        if (lower == "planning" || lower == "planning workspace" || lower == "open planning") {
            return NavigateCommand("PLANNING")
        }

        // 1. Navigation Intent matching
        val navPrefixes = listOf("go to ", "show ", "navigate to ", "switch to ", "open ")
        for (prefix in navPrefixes) {
            if (lower.startsWith(prefix)) {
                val target = lower.substring(prefix.length).trim()
                when {
                    target.contains("planning") -> return NavigateCommand("PLANNING")
                    target.contains("workspace") -> return NavigateCommand("WORKSPACES")
                    target == "home" || target == "main screen" || target == "remmi home" -> return NavigateCommand("HOME")
                    target == "apps" || target == "app list" || target == "applications" || target == "all apps" -> return NavigateCommand("APPS")
                    target == "settings" || target == "preferences" -> return NavigateCommand("SETTINGS")
                }
            }
        }

        // Direct page names
        when (lower) {
            "workspaces", "workspace" -> return NavigateCommand("WORKSPACES")
            "home", "remmi home" -> return NavigateCommand("HOME")
            "apps", "all apps" -> return NavigateCommand("APPS")
        }

        // 2. Planning Commands Intent Matching
        if (lower.startsWith("create task ") || lower.startsWith("add task ")) {
            val title = trimmed.substring(12).trim()
            if (title.isNotBlank()) {
                return CreateTaskCommand(TaskItem(title = title))
            }
        }

        if (lower.startsWith("set alarm ") || lower.startsWith("create alarm ")) {
            val details = trimmed.substring(10).trim()
            val timePart = details.substringAfterLast(" ", "07:00")
            val titlePart = details.substringBeforeLast(" ", details).ifBlank { "Alarm" }
            return CreateAlarmCommand(AlarmItem(title = titlePart, timeIso = if (timePart.contains(":")) timePart else "07:00"))
        }

        if (lower.startsWith("add event ") || lower.startsWith("create event ")) {
            val title = trimmed.substring(10).trim()
            if (title.isNotBlank()) {
                return CreateCalendarEventCommand(CalendarEvent(title = title, startDateIso = LocalDate.now().toString()))
            }
        }

        // 3. App Launch Intent matching
        val appPrefixes = listOf("launch ", "open ", "run ", "start ", "exec ")
        var appNameCandidate: String? = null
        for (prefix in appPrefixes) {
            if (lower.startsWith(prefix)) {
                appNameCandidate = lower.substring(prefix.length).trim()
                break
            }
        }

        if (!appNameCandidate.isNullOrBlank()) {
            val apps = installedAppsProvider()
            val matchedApp = apps.find { it.label.lowercase() == appNameCandidate }
                ?: apps.find { it.label.lowercase().startsWith(appNameCandidate) }
                ?: apps.find { it.label.lowercase().contains(appNameCandidate) }

            if (matchedApp != null) {
                return LaunchAppCommand(matchedApp)
            }
        }

        return null
    }

    override fun processInput(
        input: String,
        executeRouter: (RemmiCommand) -> RemmiCommandResult
    ): RemmiCommandResult {
        val command = resolveIntent(input)
            ?: return RemmiCommandResult.Failure("Unrecognized AI intent for input: '$input'")

        val validationFailure = validator.validateCommand(command)
        if (validationFailure != null) {
            return validationFailure
        }

        return try {
            executeRouter(command)
        } catch (e: Exception) {
            RemmiCommandResult.Failure("Command execution failed: ${e.message ?: e.javaClass.simpleName}", e)
        }
    }
}
