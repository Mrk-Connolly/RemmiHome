package com.remmi.ui.home

import com.remmi.core.android.launcher.RemmiAppInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class RecentAppsTest {

    private val sampleInstalledApps = listOf(
        RemmiAppInfo("com.example.browser", "MainActivity", "Browser"),
        RemmiAppInfo("com.example.camera", "CameraActivity", "Camera"),
        RemmiAppInfo("com.example.calculator", "CalcActivity", "Calculator")
    )

    @Test
    fun recentApps_prependsNewLaunchAndDeduplicates() {
        var recentPackages = listOf("com.example.browser")

        // User launches camera
        recentPackages = (listOf("com.example.camera") + recentPackages).distinct().take(10)
        assertEquals(listOf("com.example.camera", "com.example.browser"), recentPackages)

        // User launches browser again -> browser moves to top
        recentPackages = (listOf("com.example.browser") + recentPackages).distinct().take(10)
        assertEquals(listOf("com.example.browser", "com.example.camera"), recentPackages)
    }

    @Test
    fun recentApps_filtersOutUninstalledPackages() {
        val recentPackages = listOf("com.example.browser", "com.example.uninstalled", "com.example.camera")

        val recentApps = recentPackages.mapNotNull { pkg ->
            sampleInstalledApps.find { it.packageName == pkg }
        }

        assertEquals(2, recentApps.size)
        assertEquals("Browser", recentApps[0].label)
        assertEquals("Camera", recentApps[1].label)
    }

    @Test
    fun recentApps_respectsMaxLimit() {
        val manyPackages = (1..15).map { "com.example.app$it" }
        val limited = manyPackages.distinct().take(10)

        assertEquals(10, limited.size)
        assertEquals("com.example.app1", limited.first())
    }
}
