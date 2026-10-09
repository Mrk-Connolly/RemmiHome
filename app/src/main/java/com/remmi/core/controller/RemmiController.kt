package com.remmi.core.controller

import com.remmi.core.eventBus.commands.LaunchAppCommand
import com.remmi.core.eventBus.commands.NavigateCommand
import com.remmi.core.eventBus.commands.RemmiCommand
import com.remmi.core.eventBus.commands.RemmiCommandResult
import com.remmi.core.host.RemmiHost
import com.remmi.plugins.planning.alarms.commands.CreateAlarmCommand
import com.remmi.plugins.planning.alarms.commands.DeleteAlarmCommand
import com.remmi.plugins.planning.alarms.commands.EditAlarmCommand
import com.remmi.plugins.planning.alarms.commands.ToggleAlarmCommand
import com.remmi.plugins.planning.calendar.commands.CreateCalendarEventCommand
import com.remmi.plugins.planning.calendar.commands.DeleteCalendarEventCommand
import com.remmi.plugins.planning.calendar.commands.EditCalendarEventCommand
import com.remmi.plugins.planning.tasks.commands.CompleteTaskCommand
import com.remmi.plugins.planning.tasks.commands.CreateTaskCommand
import com.remmi.plugins.planning.tasks.commands.DeleteTaskCommand
import com.remmi.plugins.planning.tasks.commands.EditTaskCommand

/**
 * Lightweight coordinator and command router for Remmi core operations.
 */
class RemmiController(
    val host: RemmiHost
) {
    /**
     * Executes a given [RemmiCommand] by routing it to its designated owning system
     * and emitting it onto the [RemmiEventBus] for decoupled observers.
     *
     * @param command The command instance to route and execute.
     * @return [RemmiCommandResult] indicating whether the execution succeeded or failed.
     */
    fun executeCommand(command: RemmiCommand): RemmiCommandResult {
        // Emit onto EventBus for decoupled observers (metrics, logging, history)
        host.eventBus.sendCommand(command)

        return when (command) {
            is LaunchAppCommand -> {
                val launcher = host.launcherCapability
                if (launcher == null) {
                    RemmiCommandResult.Failure("Launcher capability is unavailable")
                } else {
                    val launched = launcher.launchApp(command.appInfo)
                    if (launched) {
                        RemmiCommandResult.Success("Launched ${command.appInfo.label}")
                    } else {
                        RemmiCommandResult.Failure("Failed to launch ${command.appInfo.label}")
                    }
                }
            }

            is NavigateCommand -> {
                RemmiCommandResult.Success("Navigation requested to ${command.destinationId}")
            }

            // Calendar Commands
            is CreateCalendarEventCommand -> {
                RemmiCommandResult.Success("Created calendar event ${command.event.title}")
            }
            is EditCalendarEventCommand -> {
                RemmiCommandResult.Success("Edited calendar event ${command.event.title}")
            }
            is DeleteCalendarEventCommand -> {
                RemmiCommandResult.Success("Deleted calendar event ${command.eventId}")
            }

            // Task Commands
            is CreateTaskCommand -> {
                RemmiCommandResult.Success("Created task ${command.task.title}")
            }
            is CompleteTaskCommand -> {
                RemmiCommandResult.Success("Updated completion state for task ${command.taskId}")
            }
            is EditTaskCommand -> {
                RemmiCommandResult.Success("Edited task ${command.task.title}")
            }
            is DeleteTaskCommand -> {
                RemmiCommandResult.Success("Deleted task ${command.taskId}")
            }

            // Alarm Commands
            is CreateAlarmCommand -> {
                RemmiCommandResult.Success("Created alarm ${command.alarm.title}")
            }
            is ToggleAlarmCommand -> {
                RemmiCommandResult.Success("Toggled alarm ${command.alarmId}")
            }
            is EditAlarmCommand -> {
                RemmiCommandResult.Success("Edited alarm ${command.alarm.title}")
            }
            is DeleteAlarmCommand -> {
                RemmiCommandResult.Success("Deleted alarm ${command.alarmId}")
            }

            else -> {
                RemmiCommandResult.Failure("Unhandled command type: ${command::class.simpleName}")
            }
        }
    }

    /**
     * Executes a given [RemmiCommand] asynchronously off the calling thread on [kotlinx.coroutines.Dispatchers.Default].
     * Guarantees non-blocking dispatcher hand-off for commands dispatched from UI threads or background workers.
     */
    suspend fun executeCommandAsync(command: RemmiCommand): RemmiCommandResult =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            executeCommand(command)
        }
}
