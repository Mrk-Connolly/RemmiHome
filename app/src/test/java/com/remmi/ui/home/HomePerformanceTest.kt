package com.remmi.ui.home

import com.remmi.core.android.launcher.RemmiAppInfo
import com.remmi.core.controller.RemmiController
import com.remmi.core.eventBus.commands.LaunchAppCommand
import com.remmi.core.host.RemmiHost
import com.remmi.ui.search.MatchType
import com.remmi.ui.search.filterGlobalSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

class HomePerformanceTest {

    @Test
    fun hostInitialization_completesRapidly() {
        val elapsedTimeMs = measureTimeMillis {
            val host = RemmiHost()
            val controller = RemmiController(host)
            checkNotNull(host.eventBus)
            checkNotNull(host.scope)
            checkNotNull(controller.host)
        }
        assertTrue("RemmiHost & Controller initialization must complete in under 50ms (actual: ${elapsedTimeMs}ms)", elapsedTimeMs < 50)
    }

    @Test
    fun searchFiltering_handlesLargeAppCollectionsRapidly() {
        val largeAppList = (1..500).map { i ->
            RemmiAppInfo(
                packageName = "com.example.app$i",
                activityName = "Activity$i",
                label = "Application $i"
            )
        }

        val query = "app4"
        val elapsedTimeNanos = measureNanoTime {
            val filtered = largeAppList.filter {
                it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
            }
            assertTrue(filtered.isNotEmpty())
        }

        val elapsedTimeMs = elapsedTimeNanos / 1_000_000.0
        assertTrue("Filtering 500 apps must take under 15ms (actual: ${elapsedTimeMs}ms)", elapsedTimeMs < 15.0)
    }

    @Test
    fun appLabelSorting_isEfficientFor1000Apps() {
        val unsortedApps = (1000 downTo 1).map { i ->
            RemmiAppInfo(
                packageName = "com.example.app$i",
                activityName = "Activity$i",
                label = "App $i"
            )
        }

        val elapsedTimeMs = measureTimeMillis {
            val sorted = unsortedApps.sortedBy { it.label.lowercase() }
            assertEquals(1000, sorted.size)
        }

        assertTrue("Sorting 1000 apps must take under 25ms (actual: ${elapsedTimeMs}ms)", elapsedTimeMs < 25)
    }

    @Test
    fun globalSearchRanking_isEfficientFor1000Apps() {
        val largeAppList = (1..1000).map { i ->
            RemmiAppInfo(
                packageName = "com.example.app$i",
                activityName = "Activity$i",
                label = if (i == 42) "Camera" else "App $i"
            )
        }
        val favoritePackages = listOf("com.example.app42")

        val elapsedTimeNanos = measureNanoTime {
            val results = filterGlobalSearch(largeAppList, favoritePackages, "camera")
            assertEquals(1, results.size)
            assertEquals("Camera", results.first().appInfo.label)
            assertEquals(MatchType.EXACT, results.first().matchType)
            assertTrue(results.first().isFavorite)
        }

        val elapsedTimeMs = elapsedTimeNanos / 1_000_000.0
        assertTrue("Global search over 1000 apps must take under 20ms (actual: ${elapsedTimeMs}ms)", elapsedTimeMs < 20.0)
    }

    @Test
    fun favoriteAppsResolution_isFastFor1000InstalledApps() {
        val installedApps = (1..1000).map { i ->
            RemmiAppInfo(
                packageName = "com.example.app$i",
                activityName = "Activity$i",
                label = "App $i"
            )
        }
        val savedFavorites = listOf("com.example.app500", "com.example.app100", "com.example.nonexistent")

        val elapsedTimeNanos = measureNanoTime {
            val resolved = savedFavorites.asSequence()
                .mapNotNull { pkg -> installedApps.find { it.packageName == pkg } }
                .take(6)
                .toList()

            assertEquals(2, resolved.size)
            assertEquals("App 500", resolved[0].label)
            assertEquals("App 100", resolved[1].label)
        }

        val elapsedTimeMs = elapsedTimeNanos / 1_000_000.0
        assertTrue("Favorite resolution over 1000 apps must take under 10ms (actual: ${elapsedTimeMs}ms)", elapsedTimeMs < 10.0)
    }

    @Test
    fun categoryResolution_heuristicsAreFastWithoutContext() {
        val sampleApps = (1..500).map { i ->
            RemmiAppInfo(
                packageName = when (i % 5) {
                    0 -> "com.google.android.GoogleCamera"
                    1 -> "com.google.android.apps.messaging"
                    2 -> "com.google.android.apps.docs"
                    3 -> "com.android.settings"
                    else -> "com.example.game$i"
                },
                activityName = "Activity$i",
                label = "App $i"
            )
        }

        val elapsedTimeMs = measureTimeMillis {
            val categories = sampleApps.map { resolveAppCategory(null, it) }
            assertEquals(500, categories.size)
        }

        assertTrue("Category heuristic resolution for 500 apps must take under 15ms (actual: ${elapsedTimeMs}ms)", elapsedTimeMs < 15)
    }

    @Test
    fun launchAppCommand_mappingLatencyIsNegligible() {
        val appInfo = RemmiAppInfo("com.example.target", "TargetActivity", "Target App")

        val elapsedTimeNanos = measureNanoTime {
            val command = LaunchAppCommand(appInfo)
            assertNotNull(command)
            assertEquals("com.example.target", command.appInfo.packageName)
        }

        val elapsedTimeMs = elapsedTimeNanos / 1_000_000.0
        assertTrue("Command mapping must take under 1ms (actual: ${elapsedTimeMs}ms)", elapsedTimeMs < 1.0)
    }
}
