package com.remmi.ui.home

import com.remmi.core.android.launcher.RemmiAppInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class AppsScreenCategoryTest {

    private val sampleApps = listOf(
        RemmiAppInfo("com.example.chat", "MainActivity", "Messages"),
        RemmiAppInfo("com.example.notes", "NotesActivity", "Notes"),
        RemmiAppInfo("com.example.music", "MusicActivity", "Music"),
        RemmiAppInfo("com.example.game", "GameActivity", "Racing Game"),
        RemmiAppInfo("com.example.maps", "MapsActivity", "Maps")
    )

    @Test
    fun categoryResolution_mapsPackageHeuristicsCorrectly() {
        assertEquals(AppCategory.MESSAGING, resolveAppCategory(null, sampleApps[0]))
        assertEquals(AppCategory.TOOLS, resolveAppCategory(null, sampleApps[1]))
        assertEquals(AppCategory.MUSIC, resolveAppCategory(null, sampleApps[2]))
        assertEquals(AppCategory.GAMES, resolveAppCategory(null, sampleApps[3]))
        assertEquals(AppCategory.MAPS_TRIPS, resolveAppCategory(null, sampleApps[4]))
    }

    @Test
    fun categoryFiltering_filtersAppsBySelectedCategory() {
        val filteredMusic = sampleApps.filter { resolveAppCategory(null, it) == AppCategory.MUSIC }
        assertEquals(1, filteredMusic.size)
        assertEquals("Music", filteredMusic.first().label)

        val filteredGames = sampleApps.filter { resolveAppCategory(null, it) == AppCategory.GAMES }
        assertEquals(1, filteredGames.size)
        assertEquals("Racing Game", filteredGames.first().label)
    }

    @Test
    fun categorySelectionToggle_returnsToAllWhenSameCategoryReselected() {
        fun selectCategory(current: AppCategory, tapped: AppCategory): AppCategory {
            return if (current == tapped && tapped != AppCategory.ALL) AppCategory.ALL else tapped
        }

        assertEquals(AppCategory.ALL, selectCategory(AppCategory.MESSAGING, AppCategory.MESSAGING))
        assertEquals(AppCategory.MUSIC, selectCategory(AppCategory.ALL, AppCategory.MUSIC))
    }
}
