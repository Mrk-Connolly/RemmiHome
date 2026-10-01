package com.remmi.ui.home

import com.remmi.core.android.launcher.RemmiAppInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class AppCategoryTest {

    @Test
    fun resolveAppCategory_heuristicsMatchCorrectCategories() {
        val cameraApp = RemmiAppInfo("com.google.android.GoogleCamera", "CameraActivity", "Camera")
        assertEquals(AppCategory.MEDIA, resolveAppCategory(null, cameraApp))

        val messagingApp = RemmiAppInfo("com.google.android.apps.messaging", "Main", "Messages")
        assertEquals(AppCategory.COMMUNICATION, resolveAppCategory(null, messagingApp))

        val driveApp = RemmiAppInfo("com.google.android.apps.docs", "Main", "Drive")
        assertEquals(AppCategory.PRODUCTIVITY, resolveAppCategory(null, driveApp))

        val settingsApp = RemmiAppInfo("com.android.settings", "Settings", "Settings")
        assertEquals(AppCategory.UTILITIES, resolveAppCategory(null, settingsApp))

        val unknownApp = RemmiAppInfo("com.example.unknown", "Main", "Custom Tool")
        assertEquals(AppCategory.OTHER, resolveAppCategory(null, unknownApp))
    }
}
