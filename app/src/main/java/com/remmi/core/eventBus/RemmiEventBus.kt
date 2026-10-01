package com.remmi.core.eventBus

import com.remmi.core.eventBus.commands.RemmiCommand
import com.remmi.core.eventBus.events.RemmiEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlin.reflect.KClass

/**
 * Lightweight, coroutine-friendly EventBus for independent system communication in Remmi.
 *
 * Distinguishes between:
 * - [RemmiCommand]: Intent that something should happen.
 * - [RemmiEvent]: Notification that something happened.
 */
class RemmiEventBus(
    extraBufferCapacity: Int = 64,
    onBufferOverflow: BufferOverflow = BufferOverflow.DROP_OLDEST
) {

    private val _events = MutableSharedFlow<RemmiEvent>(
        replay = 0,
        extraBufferCapacity = extraBufferCapacity,
        onBufferOverflow = onBufferOverflow
    )
    val events: SharedFlow<RemmiEvent> = _events.asSharedFlow()

    private val _commands = MutableSharedFlow<RemmiCommand>(
        replay = 0,
        extraBufferCapacity = extraBufferCapacity,
        onBufferOverflow = onBufferOverflow
    )
    val commands: SharedFlow<RemmiCommand> = _commands.asSharedFlow()

    /**
     * Publishes a system event.
     */
    fun publishEvent(event: RemmiEvent): Boolean {
        return _events.tryEmit(event)
    }

    /**
     * Dispatches a system command.
     */
    fun sendCommand(command: RemmiCommand): Boolean {
        return _commands.tryEmit(command)
    }

    /**
     * Observes events filtered by the specified type [eventType].
     */
    fun <T : RemmiEvent> observeEvents(eventType: KClass<T>): Flow<T> {
        return events.filterIsInstance(eventType)
    }

    /**
     * Observes events filtered by Java class [eventType].
     */
    fun <T : RemmiEvent> observeEvents(eventType: Class<T>): Flow<T> {
        return events.filterIsInstance(eventType.kotlin)
    }

    /**
     * Observes commands filtered by the specified type [commandType].
     */
    fun <T : RemmiCommand> observeCommands(commandType: KClass<T>): Flow<T> {
        return commands.filterIsInstance(commandType)
    }

    /**
     * Observes commands filtered by Java class [commandType].
     */
    fun <T : RemmiCommand> observeCommands(commandType: Class<T>): Flow<T> {
        return commands.filterIsInstance(commandType.kotlin)
    }
}

/**
 * Extension function for type-safe event subscription.
 */
inline fun <reified T : RemmiEvent> RemmiEventBus.eventsOfType(): Flow<T> {
    return events.filterIsInstance<T>()
}

/**
 * Extension function for type-safe command subscription.
 */
inline fun <reified T : RemmiCommand> RemmiEventBus.commandsOfType(): Flow<T> {
    return commands.filterIsInstance<T>()
}
