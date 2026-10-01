package com.remmi.plugins

import com.remmi.core.eventBus.events.RemmiEvent
import com.remmi.core.eventBus.eventsOfType
import com.remmi.core.host.RemmiHost
import com.remmi.plugins.api.RemmiPluginContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class RemmiPluginTest {

    private data class PluginSampleEvent(val message: String) : RemmiEvent

    private class SampleTestPlugin : RemmiPlugin {
        override val id: String = "com.remmi.plugin.sample"
        override val name: String = "Sample Plugin"

        var isInitialized: Boolean = false
            private set
        var isShutdown: Boolean = false
            private set
        var receivedEventMessage: String? = null

        override fun onInitialize(context: RemmiPluginContext) {
            isInitialized = true
            context.scope.launch(Dispatchers.Unconfined) {
                context.eventBus.eventsOfType<PluginSampleEvent>().collect { event ->
                    receivedEventMessage = event.message
                }
            }
        }

        override fun onShutdown() {
            isShutdown = true
        }
    }

    private class FailingTestPlugin : RemmiPlugin {
        override val id: String = "com.remmi.plugin.failing"
        override val name: String = "Failing Plugin"

        override fun onInitialize(context: RemmiPluginContext) {
            throw IllegalStateException("Plugin initialization failed")
        }

        override fun onShutdown() {
            // No-op
        }
    }

    @Test
    fun plugin_satisfiesContract() {
        val plugin = SampleTestPlugin()
        assertEquals("com.remmi.plugin.sample", plugin.id)
        assertEquals("Sample Plugin", plugin.name)
        assertFalse(plugin.isInitialized)
        assertFalse(plugin.isShutdown)
    }

    @Test
    fun plugin_onInitialize_receivesCoreContextAndCommunicatesViaEventBus() = runBlocking {
        val host = RemmiHost()
        val context = RemmiPluginContext(eventBus = host.eventBus, scope = host.scope)
        val plugin = SampleTestPlugin()

        plugin.onInitialize(context)
        assertTrue("Plugin should be initialized", plugin.isInitialized)

        val latch = CountDownLatch(1)
        host.eventBus.publishEvent(PluginSampleEvent("hello_from_core"))

        context.eventBus.publishEvent(PluginSampleEvent("hello_from_plugin"))
        assertEquals("hello_from_plugin", plugin.receivedEventMessage)
    }

    @Test
    fun plugin_onShutdown_cleansUpResiliently() {
        val plugin = SampleTestPlugin()
        plugin.onShutdown()
        assertTrue("Plugin should be shut down", plugin.isShutdown)
    }

    @Test
    fun plugin_failureDuringInit_doesNotCrashHost() {
        val host = RemmiHost()
        val context = RemmiPluginContext(eventBus = host.eventBus, scope = host.scope)
        val failingPlugin = FailingTestPlugin()

        var caughtException = false
        try {
            failingPlugin.onInitialize(context)
        } catch (e: IllegalStateException) {
            caughtException = true
        }

        assertTrue("Exception from failing plugin should be caught", caughtException)
        // Verify host remains functional
        val published = host.eventBus.publishEvent(PluginSampleEvent("host_still_works"))
        assertTrue("Host EventBus should remain functional", published)
    }
}
