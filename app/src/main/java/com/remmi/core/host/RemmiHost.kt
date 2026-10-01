package com.remmi.core.host

import com.remmi.core.android.launcher.RemmiLauncherCapability
import com.remmi.core.android.systemInfo.AndroidSystemInfoCapability
import com.remmi.core.android.systemInfo.RemmiSystemInfoCapability
import com.remmi.core.eventBus.RemmiEventBus
import com.remmi.core.plugin.RemmiPluginRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Owner and container for fundamental Remmi core infrastructure.
 */
class RemmiHost(
    val eventBus: RemmiEventBus = RemmiEventBus(),
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    val pluginRegistry: RemmiPluginRegistry = RemmiPluginRegistry(),
    val systemInfoCapability: RemmiSystemInfoCapability = AndroidSystemInfoCapability(),
    val launcherCapability: RemmiLauncherCapability? = null
)
