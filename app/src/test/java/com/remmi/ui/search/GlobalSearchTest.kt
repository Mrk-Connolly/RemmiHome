package com.remmi.ui.search

import com.remmi.core.android.launcher.RemmiAppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalSearchTest {

    private val sampleApps = listOf(
        RemmiAppInfo("com.google.android.GoogleCamera", "CameraActivity", "Camera"),
        RemmiAppInfo("com.example.calculator", "CalcActivity", "Calculator"),
        RemmiAppInfo("com.example.browser", "MainActivity", "Web Browser"),
        RemmiAppInfo("com.google.android.apps.photos", "PhotosActivity", "Google Photos")
    )

    private val favoritePackages = listOf("com.google.android.GoogleCamera")

    @Test
    fun filterGlobalSearch_emptyQuery_returnsEmptyList() {
        val results = filterGlobalSearch(sampleApps, favoritePackages, "")
        assertTrue(results.isEmpty())
    }

    @Test
    fun filterGlobalSearch_blankWhitespaceQuery_returnsEmptyList() {
        val results = filterGlobalSearch(sampleApps, favoritePackages, "   ")
        assertTrue(results.isEmpty())
    }

    @Test
    fun filterGlobalSearch_exactMatch_ranksFirst() {
        val results = filterGlobalSearch(sampleApps, favoritePackages, "camera")
        assertEquals(1, results.size)
        assertEquals("Camera", results.first().appInfo.label)
        assertEquals(MatchType.EXACT, results.first().matchType)
        assertTrue(results.first().isFavorite)
    }

    @Test
    fun filterGlobalSearch_prefixMatch_ranksAbovePartialMatch() {
        // "calc" matches "Calculator" (prefix)
        val results = filterGlobalSearch(sampleApps, favoritePackages, "calc")
        assertEquals(1, results.size)
        assertEquals("Calculator", results.first().appInfo.label)
        assertEquals(MatchType.PREFIX, results.first().matchType)
        assertFalse(results.first().isFavorite)
    }

    @Test
    fun filterGlobalSearch_partialMatch_matchesPackageNameOrLabel() {
        val results = filterGlobalSearch(sampleApps, favoritePackages, "photo")
        assertEquals(1, results.size)
        assertEquals("Google Photos", results.first().appInfo.label)
        assertEquals(MatchType.PARTIAL, results.first().matchType)
    }

    @Test
    fun filterGlobalSearch_caseInsensitive_returnsCorrectMatches() {
        val results = filterGlobalSearch(sampleApps, favoritePackages, "WEB BROWSER")
        assertEquals(1, results.size)
        assertEquals("Web Browser", results.first().appInfo.label)
        assertEquals(MatchType.EXACT, results.first().matchType)
    }

    @Test
    fun filterGlobalSearch_noMatch_returnsEmptyList() {
        val results = filterGlobalSearch(sampleApps, favoritePackages, "nonexistent")
        assertTrue(results.isEmpty())
    }

    @Test
    fun filterGlobalSearch_alphabeticalTieBreaker_ordersCorrectly() {
        val appsWithSamePrefix = listOf(
            RemmiAppInfo("com.example.z", "Main", "App Z"),
            RemmiAppInfo("com.example.a", "Main", "App A")
        )

        val results = filterGlobalSearch(appsWithSamePrefix, emptyList(), "app")
        assertEquals(2, results.size)
        assertEquals("App A", results[0].appInfo.label)
        assertEquals("App Z", results[1].appInfo.label)
    }
}
