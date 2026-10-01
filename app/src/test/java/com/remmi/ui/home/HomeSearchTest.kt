package com.remmi.ui.home

import com.remmi.core.android.launcher.RemmiAppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSearchTest {

    private val sampleApps = listOf(
        RemmiAppInfo("com.example.browser", "MainActivity", "Web Browser"),
        RemmiAppInfo("com.example.camera", "CameraActivity", "Camera"),
        RemmiAppInfo("com.example.calculator", "CalcActivity", "Calculator")
    )

    @Test
    fun filterApps_emptyQuery_returnsAllApps() {
        val query = ""
        val filtered = sampleApps.filter {
            query.isBlank() || it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
        }
        assertEquals(3, filtered.size)
    }

    @Test
    fun filterApps_matchingLabelQuery_returnsFilteredApps() {
        val query = "calc"
        val filtered = sampleApps.filter {
            it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
        }
        assertEquals(1, filtered.size)
        assertEquals("Calculator", filtered.first().label)
    }

    @Test
    fun filterApps_matchingPackageNameQuery_returnsFilteredApps() {
        val query = "camera"
        val filtered = sampleApps.filter {
            it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
        }
        assertEquals(1, filtered.size)
        assertEquals("Camera", filtered.first().label)
    }

    @Test
    fun filterApps_caseInsensitive_returnsFilteredApps() {
        val query = "BROWSER"
        val filtered = sampleApps.filter {
            it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
        }
        assertEquals(1, filtered.size)
        assertEquals("Web Browser", filtered.first().label)
    }

    @Test
    fun filterApps_noMatch_returnsEmptyList() {
        val query = "nonexistent"
        val filtered = sampleApps.filter {
            it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
        }
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun filterApps_blankWhitespaceQuery_returnsAllApps() {
        val query = "   "
        val filtered = sampleApps.filter {
            query.isBlank() || it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
        }
        assertEquals(3, filtered.size)
    }

    @Test
    fun filterApps_partialQuery_returnsMultipleMatches() {
        val query = "com.example"
        val filtered = sampleApps.filter {
            query.isBlank() || it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
        }
        assertEquals(3, filtered.size)
    }
}
