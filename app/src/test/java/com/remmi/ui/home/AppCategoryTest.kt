package com.remmi.ui.home

import com.remmi.core.android.launcher.RemmiAppInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class AppCategoryTest {

    @Test
    fun resolveAppCategory_heuristicsMatchCorrectCategories() {
        val messagingApp = RemmiAppInfo("com.google.android.apps.messaging", "Main", "Messages")
        assertEquals(AppCategory.MESSAGING, resolveAppCategory(null, messagingApp))

        val mapsApp = RemmiAppInfo("com.google.android.apps.maps", "Main", "Maps")
        assertEquals(AppCategory.MAPS_TRIPS, resolveAppCategory(null, mapsApp))

        val musicApp = RemmiAppInfo("com.spotify.music", "Main", "Spotify")
        assertEquals(AppCategory.MUSIC, resolveAppCategory(null, musicApp))

        val filesApp = RemmiAppInfo("com.google.android.documentsui", "Main", "Files")
        assertEquals(AppCategory.FILES, resolveAppCategory(null, filesApp))

        val shoppingApp = RemmiAppInfo("com.amazon.mShop.android.shopping", "Main", "Amazon")
        assertEquals(AppCategory.SHOPPING, resolveAppCategory(null, shoppingApp))

        val gameApp = RemmiAppInfo("com.google.android.play.games", "Main", "Play Games")
        assertEquals(AppCategory.GAMES, resolveAppCategory(null, gameApp))

        val toolsApp = RemmiAppInfo("com.android.calculator2", "Main", "Calculator")
        assertEquals(AppCategory.TOOLS, resolveAppCategory(null, toolsApp))
    }
}
