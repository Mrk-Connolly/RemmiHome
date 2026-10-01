package com.remmi.core.plugin

import com.remmi.core.host.RemmiHost
import com.remmi.plugins.RemmiPlugin
import com.remmi.plugins.api.RemmiPluginContext

/**
 * Core-owned registry for managing [RemmiPlugin] registration and lifecycle orchestration.
 */
class RemmiPluginRegistry {

    private val registeredPlugins = LinkedHashMap<String, RemmiPlugin>()
    private val initializedPluginIds = LinkedHashSet<String>()

    /**
     * Read-only view of registered plugins in registration order.
     */
    val plugins: List<RemmiPlugin>
        get() = registeredPlugins.values.toList()

    /**
     * Registers a [RemmiPlugin] instance.
     *
     * @throws IllegalArgumentException if a plugin with the same ID is already registered.
     */
    @Synchronized
    fun register(plugin: RemmiPlugin) {
        require(!registeredPlugins.containsKey(plugin.id)) {
            "Plugin with ID '${plugin.id}' is already registered."
        }
        registeredPlugins[plugin.id] = plugin
    }

    /**
     * Initializes all registered plugins using the provided [host].
     *
     * Failure Isolation: Errors thrown during a plugin's `onInitialize` are caught and recorded
     * in the returned map without halting the initialization of remaining plugins.
     */
    @Synchronized
    fun initializeAll(host: RemmiHost): Map<String, Result<Unit>> {
        val context = RemmiPluginContext(eventBus = host.eventBus, scope = host.scope)
        val results = LinkedHashMap<String, Result<Unit>>()

        for (plugin in registeredPlugins.values) {
            if (initializedPluginIds.contains(plugin.id)) {
                results[plugin.id] = Result.success(Unit)
                continue
            }

            val result = runCatching {
                plugin.onInitialize(context)
            }

            if (result.isSuccess) {
                initializedPluginIds.add(plugin.id)
            }
            results[plugin.id] = result
        }

        return results
    }

    /**
     * Shuts down all initialized plugins in reverse registration order.
     *
     * Failure Isolation: Errors thrown during a plugin's `onShutdown` are caught and recorded
     * without preventing other initialized plugins from shutting down.
     */
    @Synchronized
    fun shutdownAll(): Map<String, Result<Unit>> {
        val results = LinkedHashMap<String, Result<Unit>>()
        val toShutdown = registeredPlugins.values.filter { initializedPluginIds.contains(it.id) }.reversed()

        for (plugin in toShutdown) {
            val result = runCatching {
                plugin.onShutdown()
            }
            initializedPluginIds.remove(plugin.id)
            results[plugin.id] = result
        }

        return results
    }
}
