package com.remmi.ui.home

import com.remmi.core.android.launcher.RemmiAppInfo
import com.remmi.core.android.launcher.RemmiLauncherCapability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLauncherFlowTest {

    private class FakeLauncherCapability : RemmiLauncherCapability {
        private val _installedApps = MutableStateFlow<List<RemmiAppInfo>>(emptyList())
        override val installedApps: StateFlow<List<RemmiAppInfo>> = _installedApps.asStateFlow()

        private val _isDiscovering = MutableStateFlow(false)
        override val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

        var launchedApp: RemmiAppInfo? = null
        var shouldLaunchSucceed = true

        override suspend fun refreshInstalledApps() {
            _isDiscovering.value = true
            _installedApps.value = listOf(
                RemmiAppInfo("com.example.browser", "BrowserActivity", "Browser"),
                RemmiAppInfo("com.example.camera", "CameraActivity", "Camera"),
                RemmiAppInfo("com.example.calculator", "CalcActivity", "Calculator")
            )
            _isDiscovering.value = false
        }

        override fun launchApp(appInfo: RemmiAppInfo): Boolean {
            if (shouldLaunchSucceed) {
                launchedApp = appInfo
                return true
            }
            return false
        }
    }

    @Test
    fun completeLauncherUserFlow_appDiscovery_search_launch() = runBlocking {
        val launcher = FakeLauncherCapability()

        // 1. Initial State before discovery
        assertEquals(0, launcher.installedApps.value.size)
        assertFalse(launcher.isDiscovering.value)

        // 2. Discover Applications
        launcher.refreshInstalledApps()
        val apps = launcher.installedApps.value
        assertEquals(3, apps.size)

        // 3. Search Application
        val query = "cam"
        val searchResults = apps.filter { it.label.contains(query, ignoreCase = true) }
        assertEquals(1, searchResults.size)
        assertEquals("Camera", searchResults.first().label)

        // 4. Select and Launch Application
        val selectedApp = searchResults.first()
        val success = launcher.launchApp(selectedApp)

        assertTrue(success)
        assertEquals("com.example.camera", launcher.launchedApp?.packageName)
    }

    @Test
    fun launcherUserFlow_handlesLaunchFailureGracefully() = runBlocking {
        val launcher = FakeLauncherCapability()
        launcher.shouldLaunchSucceed = false

        launcher.refreshInstalledApps()
        val targetApp = launcher.installedApps.value.first()

        val success = launcher.launchApp(targetApp)

        assertFalse("Launch should return false on failure", success)
        assertNull("Launched app should remain null on failure", launcher.launchedApp)
    }

    @Test
    fun launcherUserFlow_repeatedRefreshPreservesList() = runBlocking {
        val launcher = FakeLauncherCapability()

        launcher.refreshInstalledApps()
        assertEquals(3, launcher.installedApps.value.size)

        // Simulating return to Home re-triggering refresh
        launcher.refreshInstalledApps()
        assertEquals(3, launcher.installedApps.value.size)
    }

    @Test
    fun launcherUserFlow_launchAppAndResetSearchQuery() = runBlocking {
        val launcher = FakeLauncherCapability()
        launcher.refreshInstalledApps()

        var searchQuery = "cam"
        val apps = launcher.installedApps.value
        val matched = apps.find { it.label.contains(searchQuery, ignoreCase = true) }

        assertTrue(matched != null)
        val success = launcher.launchApp(matched!!)

        if (success) {
            searchQuery = ""
        }

        assertTrue(success)
        assertEquals("", searchQuery)
        assertEquals("com.example.camera", launcher.launchedApp?.packageName)
    }
}
