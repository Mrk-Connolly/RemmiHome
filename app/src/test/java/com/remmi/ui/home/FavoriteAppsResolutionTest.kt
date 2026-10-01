package com.remmi.ui.home

import com.remmi.core.android.launcher.RemmiAppInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class FavoriteAppsResolutionTest {

    private val sampleInstalledApps = listOf(
        RemmiAppInfo("com.example.browser", "MainActivity", "Browser"),
        RemmiAppInfo("com.example.camera", "CameraActivity", "Camera"),
        RemmiAppInfo("com.example.calculator", "CalcActivity", "Calculator"),
        RemmiAppInfo("com.example.notes", "NotesActivity", "Notes"),
        RemmiAppInfo("com.example.photos", "PhotosActivity", "Photos"),
        RemmiAppInfo("com.example.clock", "ClockActivity", "Clock"),
        RemmiAppInfo("com.example.maps", "MapsActivity", "Maps")
    )

    @Test
    fun favoriteAppsResolution_resolvesPackageNamesInUserOrder() {
        val savedFavorites = listOf("com.example.camera", "com.example.browser", "com.example.notes")

        val resolvedFavorites = savedFavorites.mapNotNull { pkg ->
            sampleInstalledApps.find { it.packageName == pkg }
        }.take(6)

        assertEquals(3, resolvedFavorites.size)
        assertEquals("Camera", resolvedFavorites[0].label)
        assertEquals("Browser", resolvedFavorites[1].label)
        assertEquals("Notes", resolvedFavorites[2].label)
    }

    @Test
    fun favoriteAppsResolution_filtersOutUninstalledPackages() {
        val savedFavorites = listOf("com.example.camera", "com.example.uninstalled", "com.example.clock")

        val resolvedFavorites = savedFavorites.mapNotNull { pkg ->
            sampleInstalledApps.find { it.packageName == pkg }
        }.take(6)

        assertEquals(2, resolvedFavorites.size)
        assertEquals("Camera", resolvedFavorites[0].label)
        assertEquals("Clock", resolvedFavorites[1].label)
    }

    @Test
    fun favoriteAppsResolution_capsAtSixItems() {
        val savedFavorites = listOf(
            "com.example.browser",
            "com.example.camera",
            "com.example.calculator",
            "com.example.notes",
            "com.example.photos",
            "com.example.clock",
            "com.example.maps"
        )

        val resolvedFavorites = savedFavorites.mapNotNull { pkg ->
            sampleInstalledApps.find { it.packageName == pkg }
        }.take(6)

        assertEquals(6, resolvedFavorites.size)
        assertEquals("Clock", resolvedFavorites.last().label)
    }

    private fun resolveFavorites(savedFavorites: List<String>): List<RemmiAppInfo> {
        return if (savedFavorites.isNotEmpty()) {
            savedFavorites.mapNotNull { pkg -> sampleInstalledApps.find { it.packageName == pkg } }.take(6)
        } else {
            sampleInstalledApps.take(6)
        }
    }

    @Test
    fun favoriteAppsResolution_fallbackToFirstAppsIfNoFavorites() {
        val resolvedFavorites = resolveFavorites(emptyList())

        assertEquals(6, resolvedFavorites.size)
        assertEquals("Browser", resolvedFavorites.first().label)
    }
}
