package com.remmi.ui.home

import com.remmi.core.android.launcher.RemmiAppInfo
import com.remmi.core.host.RemmiHost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

class HomePerformanceTest {

    @Test
    fun hostInitialization_completesRapidly() {
        val elapsedTimeMs = measureTimeMillis {
            val host = RemmiHost()
            checkNotNull(host.eventBus)
            checkNotNull(host.scope)
        }
        assertTrue("RemmiHost initialization must complete in under 50ms (actual: ${elapsedTimeMs}ms)", elapsedTimeMs < 50)
    }

    @Test
    fun searchFiltering_handlesLargeAppCollectionsRapidly() {
        // Construct a large app collection of 500 installed apps
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
            // 11 matches expected: app4, app40..app49, app400..app499 (111 matches)
            assertTrue(filtered.isNotEmpty())
        }

        val elapsedTimeMs = elapsedTimeNanos / 1_000_000.0
        assertTrue("Filtering 500 apps must take under 15ms (actual: ${elapsedTimeMs}ms)", elapsedTimeMs < 15.0)
    }

    @Test
    fun appLabelSorting_isEfficient() {
        val unsortedApps = (500 downTo 1).map { i ->
            RemmiAppInfo(
                packageName = "com.example.app$i",
                activityName = "Activity$i",
                label = "App $i"
            )
        }

        val elapsedTimeMs = measureTimeMillis {
            val sorted = unsortedApps.sortedBy { it.label.lowercase() }
            assertEquals(500, sorted.size)
        }

        assertTrue("Sorting 500 apps must take under 20ms (actual: ${elapsedTimeMs}ms)", elapsedTimeMs < 20)
    }
}
