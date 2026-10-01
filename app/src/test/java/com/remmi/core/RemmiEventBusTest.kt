package com.remmi.core

import com.remmi.core.eventBus.RemmiEventBus
import com.remmi.core.eventBus.commands.RemmiCommand
import com.remmi.core.eventBus.commandsOfType
import com.remmi.core.eventBus.events.RemmiEvent
import com.remmi.core.eventBus.eventsOfType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class RemmiEventBusTest {

    private data class TestEvent(val name: String) : RemmiEvent
    private data class AnotherEvent(val id: Int) : RemmiEvent
    private data class TestCommand(val action: String) : RemmiCommand

    @Test
    fun publishEvent_deliversEventToSubscribers() = runBlocking {
        val eventBus = RemmiEventBus()
        val received = mutableListOf<RemmiEvent>()
        val latch = CountDownLatch(1)

        val job = launch(Dispatchers.Unconfined) {
            eventBus.events.collect { event ->
                received.add(event)
                latch.countDown()
            }
        }

        val emitted = eventBus.publishEvent(TestEvent("bootstrapped"))
        assertTrue("Event emission should return true", emitted)

        assertTrue("Subscriber should receive event", latch.await(2, TimeUnit.SECONDS))
        assertEquals(1, received.size)
        assertEquals(TestEvent("bootstrapped"), received.first())

        job.cancel()
    }

    @Test
    fun sendCommand_deliversCommandToSubscribers() = runBlocking {
        val eventBus = RemmiEventBus()
        val received = mutableListOf<RemmiCommand>()
        val latch = CountDownLatch(1)

        val job = launch(Dispatchers.Unconfined) {
            eventBus.commands.collect { command ->
                received.add(command)
                latch.countDown()
            }
        }

        val emitted = eventBus.sendCommand(TestCommand("execute_task"))
        assertTrue("Command emission should return true", emitted)

        assertTrue("Subscriber should receive command", latch.await(2, TimeUnit.SECONDS))
        assertEquals(1, received.size)
        assertEquals(TestCommand("execute_task"), received.first())

        job.cancel()
    }

    @Test
    fun typedEvents_filtersCorrectTypes() = runBlocking {
        val eventBus = RemmiEventBus()
        val testEvents = mutableListOf<TestEvent>()
        val latch = CountDownLatch(1)

        val job = launch(Dispatchers.Unconfined) {
            eventBus.eventsOfType<TestEvent>().collect { event ->
                testEvents.add(event)
                latch.countDown()
            }
        }

        eventBus.publishEvent(AnotherEvent(42))
        eventBus.publishEvent(TestEvent("matching_event"))

        assertTrue("Typed subscriber should receive matching event", latch.await(2, TimeUnit.SECONDS))
        assertEquals(1, testEvents.size)
        assertEquals(TestEvent("matching_event"), testEvents.first())

        job.cancel()
    }

    @Test
    fun typedCommands_filtersCorrectTypes() = runBlocking {
        val eventBus = RemmiEventBus()
        val testCommands = mutableListOf<TestCommand>()
        val latch = CountDownLatch(1)

        val job = launch(Dispatchers.Unconfined) {
            eventBus.commandsOfType<TestCommand>().collect { command ->
                testCommands.add(command)
                latch.countDown()
            }
        }

        eventBus.sendCommand(TestCommand("matching_command"))

        assertTrue("Typed command subscriber should receive command", latch.await(2, TimeUnit.SECONDS))
        assertEquals(1, testCommands.size)
        assertEquals(TestCommand("matching_command"), testCommands.first())

        job.cancel()
    }

    @Test
    fun multipleSubscribers_receiveEventsConcurrently() = runBlocking {
        val eventBus = RemmiEventBus()
        val list1 = CopyOnWriteArrayList<RemmiEvent>()
        val list2 = CopyOnWriteArrayList<RemmiEvent>()
        val latch = CountDownLatch(2)

        val job1 = launch(Dispatchers.Unconfined) {
            eventBus.events.collect {
                list1.add(it)
                latch.countDown()
            }
        }

        val job2 = launch(Dispatchers.Unconfined) {
            eventBus.events.collect {
                list2.add(it)
                latch.countDown()
            }
        }

        eventBus.publishEvent(TestEvent("broadcast"))

        assertTrue("Both subscribers should receive event", latch.await(2, TimeUnit.SECONDS))
        assertEquals(1, list1.size)
        assertEquals(1, list2.size)

        job1.cancel()
        job2.cancel()
    }

    @Test
    fun cancellation_stopsEventDelivery() = runBlocking {
        val eventBus = RemmiEventBus()
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val received = mutableListOf<RemmiEvent>()

        val job = scope.launch {
            eventBus.events.collect {
                received.add(it)
            }
        }

        eventBus.publishEvent(TestEvent("first"))
        assertEquals(1, received.size)

        scope.cancel()

        eventBus.publishEvent(TestEvent("second"))
        assertEquals(1, received.size)
    }

    @Test
    fun failingSubscriber_doesNotImpactOtherSubscribers() = runBlocking {
        val eventBus = RemmiEventBus()
        val healthyList = mutableListOf<RemmiEvent>()
        val healthyLatch = CountDownLatch(1)

        val supervisorScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())

        supervisorScope.launch {
            try {
                eventBus.events.collect {
                    throw RuntimeException("Subscriber error")
                }
            } catch (_: Exception) {
                // Isolated failure handling
            }
        }

        supervisorScope.launch {
            eventBus.events.collect {
                healthyList.add(it)
                healthyLatch.countDown()
            }
        }

        eventBus.publishEvent(TestEvent("resilient_test"))

        assertTrue("Healthy subscriber should still receive event", healthyLatch.await(2, TimeUnit.SECONDS))
        assertEquals(1, healthyList.size)

        supervisorScope.cancel()
    }
}
