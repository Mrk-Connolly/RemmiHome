package com.remmi.plugins

import com.remmi.plugins.api.RemmiPluginContext

/**
 * Fundamental contract for independent Remmi feature plugins.
 */
interface RemmiPlugin {

    /** Unique stable identifier for the plugin. */
    val id: String

    /** Human-readable display name for the plugin. */
    val name: String

    /**
     * Initializes the plugin using the provided public Core [context].
     */
    fun onInitialize(context: RemmiPluginContext)

    /**
     * Shuts down the plugin cleanly and releases resources.
     */
    fun onShutdown()
}
