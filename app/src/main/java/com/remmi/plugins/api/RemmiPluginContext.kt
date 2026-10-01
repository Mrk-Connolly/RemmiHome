package com.remmi.plugins.api

import com.remmi.core.eventBus.RemmiEventBus
import kotlinx.coroutines.CoroutineScope

/**
 * Context provided to a [com.remmi.plugins.RemmiPlugin] during initialization.
 *
 * Exposes public Remmi Core capabilities without leaking Core implementation details.
 */
data class RemmiPluginContext(
    val eventBus: RemmiEventBus,
    val scope: CoroutineScope
)
