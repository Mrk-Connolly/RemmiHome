package com.remmi.core.android.launcher

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Public Core contract for querying and launching installed Android applications.
 */
interface RemmiLauncherCapability {

    /**
     * Observable StateFlow of launchable applications on the device.
     */
    val installedApps: StateFlow<List<RemmiAppInfo>>

    /**
     * Observable StateFlow indicating whether application discovery is currently in progress.
     */
    val isDiscovering: StateFlow<Boolean>
        get() = MutableStateFlow(false)

    /**
     * Asynchronously queries and refreshes the list of launchable applications.
     */
    suspend fun refreshInstalledApps()

    /**
     * Launches the application identified by [appInfo].
     * @return true if launched successfully, false otherwise.
     */
    fun launchApp(appInfo: RemmiAppInfo): Boolean
}
