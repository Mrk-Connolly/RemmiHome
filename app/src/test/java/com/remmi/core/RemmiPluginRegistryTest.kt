package com.remmi.core

import com.remmi.core.host.RemmiHost
import com.remmi.core.plugin.RemmiPluginRegistry
import com.remmi.plugins.RemmiPlugin
import com.remmi.plugins.api.RemmiPluginContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemmiPluginRegistryTest {

    private open class TestPlugin(
        override val id: String,
        override val name: String,
        private val initTracker: MutableList<String>? = null,
        private val shutdownTracker: MutableList<String>? = null,
        private val failOnInit: Boolean = false,
        private val failOnShutdown: Boolean = false
    ) : RemmiPlugin {

        var isInitialized: Boolean = false
            private set
        var isShutdown: Boolean = false
            private set

        override fun onInitialize(context: RemmiPluginContext) {
            if (failOnInit) throw RuntimeException("Initialization failure for $id")
            isInitialized = true
            initTracker?.add(id)
        }

        override fun onShutdown() {
            if (failOnShutdown) throw RuntimeException("Shutdown failure for $id")
            isShutdown = true
            shutdownTracker?.add(id)
        }
    }

    @Test
    fun register_addsPluginToCollection() {
        val registry = RemmiPluginRegistry()
        val plugin = TestPlugin("p1", "Plugin 1")

        registry.register(plugin)

        assertEquals(1, registry.plugins.size)
        assertEquals("p1", registry.plugins.first().id)
    }

    @Test(expected = IllegalArgumentException::class)
    fun register_duplicatePluginId_throwsIllegalArgumentException() {
        val registry = RemmiPluginRegistry()
        val plugin1 = TestPlugin("p1", "Plugin 1")
        val plugin2 = TestPlugin("p1", "Duplicate Plugin 1")

        registry.register(plugin1)
        registry.register(plugin2)
    }

    @Test
    fun initializeAll_initializesPluginsInRegistrationOrder() {
        val host = RemmiHost()
        val registry = host.pluginRegistry
        val order = mutableListOf<String>()

        registry.register(TestPlugin("p1", "Plugin 1", initTracker = order))
        registry.register(TestPlugin("p2", "Plugin 2", initTracker = order))
        registry.register(TestPlugin("p3", "Plugin 3", initTracker = order))

        val results = registry.initializeAll(host)

        assertEquals(3, results.size)
        assertTrue(results.values.all { it.isSuccess })
        assertEquals(listOf("p1", "p2", "p3"), order)
    }

    @Test
    fun initializeAll_failingPlugin_doesNotHaltOtherPlugins() {
        val host = RemmiHost()
        val registry = host.pluginRegistry
        val order = mutableListOf<String>()

        val p1 = TestPlugin("p1", "Plugin 1", initTracker = order)
        val p2Failing = TestPlugin("p2_fail", "Failing Plugin", initTracker = order, failOnInit = true)
        val p3 = TestPlugin("p3", "Plugin 3", initTracker = order)

        registry.register(p1)
        registry.register(p2Failing)
        registry.register(p3)

        val results = registry.initializeAll(host)

        assertTrue(results["p1"]!!.isSuccess)
        assertTrue(results["p2_fail"]!!.isFailure)
        assertTrue(results["p3"]!!.isSuccess)

        assertTrue(p1.isInitialized)
        assertFalse(p2Failing.isInitialized)
        assertTrue(p3.isInitialized)
        assertEquals(listOf("p1", "p3"), order)
    }

    @Test
    fun shutdownAll_shutsDownPluginsInReverseRegistrationOrder() {
        val host = RemmiHost()
        val registry = host.pluginRegistry
        val shutdownOrder = mutableListOf<String>()

        val p1 = TestPlugin("p1", "Plugin 1", shutdownTracker = shutdownOrder)
        val p2 = TestPlugin("p2", "Plugin 2", shutdownTracker = shutdownOrder)
        val p3 = TestPlugin("p3", "Plugin 3", shutdownTracker = shutdownOrder)

        registry.register(p1)
        registry.register(p2)
        registry.register(p3)

        registry.initializeAll(host)
        val results = registry.shutdownAll()

        assertEquals(3, results.size)
        assertTrue(results.values.all { it.isSuccess })
        assertEquals(listOf("p3", "p2", "p1"), shutdownOrder)
        assertTrue(p1.isShutdown)
        assertTrue(p2.isShutdown)
        assertTrue(p3.isShutdown)
    }

    @Test
    fun shutdownAll_failingPlugin_doesNotHaltOtherShutdowns() {
        val host = RemmiHost()
        val registry = host.pluginRegistry
        val shutdownOrder = mutableListOf<String>()

        val p1 = TestPlugin("p1", "Plugin 1", shutdownTracker = shutdownOrder)
        val p2Failing = TestPlugin("p2_fail", "Failing Shutdown", shutdownTracker = shutdownOrder, failOnShutdown = true)
        val p3 = TestPlugin("p3", "Plugin 3", shutdownTracker = shutdownOrder)

        registry.register(p1)
        registry.register(p2Failing)
        registry.register(p3)

        registry.initializeAll(host)
        val results = registry.shutdownAll()

        assertTrue(results["p3"]!!.isSuccess)
        assertTrue(results["p2_fail"]!!.isFailure)
        assertTrue(results["p1"]!!.isSuccess)

        assertTrue(p3.isShutdown)
        assertFalse(p2Failing.isShutdown)
        assertTrue(p1.isShutdown)
        assertEquals(listOf("p3", "p1"), shutdownOrder)
    }
}
