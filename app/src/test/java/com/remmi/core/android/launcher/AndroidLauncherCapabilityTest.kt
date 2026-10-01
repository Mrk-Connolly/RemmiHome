package com.remmi.core.android.launcher

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidLauncherCapabilityTest {

    private class FakeLauncherCapability : RemmiLauncherCapability {
        private val _installedApps = MutableStateFlow<List<RemmiAppInfo>>(emptyList())
        override val installedApps: StateFlow<List<RemmiAppInfo>> = _installedApps.asStateFlow()

        private val _isDiscovering = MutableStateFlow(false)
        override val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

        var launchCount = 0
        var lastLaunchedApp: RemmiAppInfo? = null
        var shouldLaunchSucceed = true

        override suspend fun refreshInstalledApps() {
            _isDiscovering.value = true
            _installedApps.value = listOf(
                RemmiAppInfo("com.example.browser", "MainActivity", "Browser"),
                RemmiAppInfo("com.example.camera", "CameraActivity", "Camera")
            )
            _isDiscovering.value = false
        }

        override fun launchApp(appInfo: RemmiAppInfo): Boolean {
            if (shouldLaunchSucceed) {
                launchCount++
                lastLaunchedApp = appInfo
                return true
            }
            return false
        }
    }

    @Test
    fun fakeCapability_refreshesAndLaunchesApp() = runBlocking {
        val capability = FakeLauncherCapability()
        assertEquals(0, capability.installedApps.value.size)
        assertFalse(capability.isDiscovering.value)

        capability.refreshInstalledApps()
        assertEquals(2, capability.installedApps.value.size)
        assertFalse(capability.isDiscovering.value)
        assertEquals("Browser", capability.installedApps.value.first().label)

        val targetApp = capability.installedApps.value.last()
        val success = capability.launchApp(targetApp)

        assertTrue(success)
        assertEquals(1, capability.launchCount)
        assertEquals("Camera", capability.lastLaunchedApp?.label)
    }

    @Test
    fun fakeCapability_handlesLaunchFailure() {
        val capability = FakeLauncherCapability()
        capability.shouldLaunchSucceed = false

        val app = RemmiAppInfo("com.example.failed", "Main", "Broken App")
        val success = capability.launchApp(app)

        assertFalse(success)
        assertEquals(0, capability.launchCount)
    }

    @Test
    fun fakeCapability_packageChangeTriggersListRefresh() = runBlocking {
        val capability = FakeLauncherCapability()
        capability.refreshInstalledApps()
        val initialCount = capability.installedApps.value.size

        // Simulating package change broadcast triggering refresh
        capability.refreshInstalledApps()
        assertEquals(initialCount, capability.installedApps.value.size)
    }
}
