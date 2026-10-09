package com.remmi.reliability

import com.remmi.core.host.RemmiHost
import com.remmi.core.plugin.RemmiPluginRegistry
import com.remmi.plugins.RemmiPlugin
import com.remmi.plugins.api.RemmiPluginContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginFailureIsolationTest {

    private class HealthyPlugin(override val id: String, override val name: String) : RemmiPlugin {
        var initialized = false
        var shutdown = false

        override fun onInitialize(context: RemmiPluginContext) {
            initialized = true
        }

        override fun onShutdown() {
            shutdown = true
        }
    }

    private class ThrowingPlugin(override val id: String, override val name: String) : RemmiPlugin {
        override fun onInitialize(context: RemmiPluginContext) {
            throw RuntimeException("Simulated plugin initialization crash!")
        }

        override fun onShutdown() {
            throw RuntimeException("Simulated plugin shutdown crash!")
        }
    }

    private class ThrowingShutdownPlugin(override val id: String, override val name: String) : RemmiPlugin {
        override fun onInitialize(context: RemmiPluginContext) {}

        override fun onShutdown() {
            throw RuntimeException("Simulated plugin shutdown crash!")
        }
    }

    @Test
    fun `initializeAll isolates throwing plugin and initializes healthy plugins`() {
        val registry = RemmiPluginRegistry()
        val host = RemmiHost()

        val healthy1 = HealthyPlugin("p1", "Healthy Plugin 1")
        val throwing = ThrowingPlugin("p2", "Throwing Plugin")
        val healthy2 = HealthyPlugin("p3", "Healthy Plugin 2")

        registry.register(healthy1)
        registry.register(throwing)
        registry.register(healthy2)

        val results = registry.initializeAll(host)

        assertEquals(3, results.size)
        assertTrue(results["p1"]!!.isSuccess)
        assertTrue(healthy1.initialized)

        assertTrue(results["p2"]!!.isFailure)
        assertEquals("Simulated plugin initialization crash!", results["p2"]!!.exceptionOrNull()?.message)

        assertTrue(results["p3"]!!.isSuccess)
        assertTrue(healthy2.initialized)
    }

    @Test
    fun `shutdownAll isolates throwing plugin shutdown and shuts down healthy plugins`() {
        val registry = RemmiPluginRegistry()
        val host = RemmiHost()

        val healthy1 = HealthyPlugin("p1", "Healthy Plugin 1")
        val throwingShutdown = ThrowingShutdownPlugin("p2", "Throwing Shutdown Plugin")

        registry.register(healthy1)
        registry.register(throwingShutdown)

        registry.initializeAll(host)

        val shutdownResults = registry.shutdownAll()

        assertEquals(2, shutdownResults.size)
        assertTrue(shutdownResults["p1"]!!.isSuccess)
        assertTrue(healthy1.shutdown)

        assertTrue(shutdownResults["p2"]!!.isFailure)
        assertEquals("Simulated plugin shutdown crash!", shutdownResults["p2"]!!.exceptionOrNull()?.message)
    }
}
