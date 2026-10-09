package com.remmi.core.ai

import com.remmi.core.eventBus.commands.LaunchAppCommand
import com.remmi.core.eventBus.commands.NavigateCommand
import com.remmi.core.eventBus.commands.RemmiCommand
import com.remmi.core.eventBus.commands.RemmiCommandResult
import com.remmi.plugins.planning.alarms.commands.CreateAlarmCommand
import com.remmi.plugins.planning.calendar.commands.CreateCalendarEventCommand
import com.remmi.plugins.planning.tasks.commands.CreateTaskCommand

/**
 * Interface for AI command safety validation before routing and execution.
 */
interface RemmiAiCommandValidator {
    /**
     * Validates [command] for structural and permission safety.
     *
     * @param command The command instance to validate.
     * @return [RemmiCommandResult.Failure] if validation fails, or null if valid.
     */
    fun validateCommand(command: RemmiCommand): RemmiCommandResult.Failure?

    /**
     * Determines whether execution of [command] requires explicit user confirmation.
     *
     * @param command The command instance to evaluate.
     * @return true if confirmation is required before execution.
     */
    fun requiresConfirmation(command: RemmiCommand): Boolean
}

/**
 * Default implementation of [RemmiAiCommandValidator].
 */
class DefaultAiCommandValidator : RemmiAiCommandValidator {

    private val supportedDestinations = setOf("WORKSPACES", "HOME", "REMMI_HOME", "APPS", "SETTINGS", "PLANNING")

    override fun validateCommand(command: RemmiCommand): RemmiCommandResult.Failure? {
        return when (command) {
            is LaunchAppCommand -> {
                if (command.appInfo.packageName.isBlank()) {
                    RemmiCommandResult.Failure("Invalid app launch command: packageName is blank")
                } else if (command.appInfo.activityName.isBlank()) {
                    RemmiCommandResult.Failure("Invalid app launch command: activityName is blank")
                } else null
            }

            is NavigateCommand -> {
                if (command.destinationId.isBlank()) {
                    RemmiCommandResult.Failure("Invalid navigate command: destinationId is blank")
                } else if (command.destinationId.uppercase() !in supportedDestinations) {
                    RemmiCommandResult.Failure("Unsupported navigation destination: '${command.destinationId}'")
                } else null
            }

            is CreateCalendarEventCommand -> {
                if (command.event.title.isBlank()) {
                    RemmiCommandResult.Failure("Invalid calendar event: title is blank")
                } else null
            }

            is CreateTaskCommand -> {
                if (command.task.title.isBlank()) {
                    RemmiCommandResult.Failure("Invalid task: title is blank")
                } else null
            }

            is CreateAlarmCommand -> {
                if (command.alarm.title.isBlank()) {
                    RemmiCommandResult.Failure("Invalid alarm: title is blank")
                } else null
            }

            else -> null
        }
    }

    override fun requiresConfirmation(command: RemmiCommand): Boolean {
        // Core launcher navigation, app launch actions, and planning item additions are safe and do not require confirmation.
        return false
    }
}
