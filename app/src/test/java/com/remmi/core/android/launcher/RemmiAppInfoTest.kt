package com.remmi.core.android.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemmiAppInfoTest {

    @Test
    fun remmiAppInfo_initializesWithPackageNameActivityNameAndLabel() {
        val appInfo = RemmiAppInfo(
            packageName = "com.example.app",
            activityName = "MainActivity",
            label = "Example App"
        )

        assertEquals("com.example.app", appInfo.packageName)
        assertEquals("MainActivity", appInfo.activityName)
        assertEquals("Example App", appInfo.label)
        assertNull("Default icon should be null", appInfo.icon)
    }

    @Test
    fun remmiAppInfo_equalityBasedOnProperties() {
        val app1 = RemmiAppInfo("com.example.app", "Main", "Label")
        val app2 = RemmiAppInfo("com.example.app", "Main", "Label")

        assertEquals(app1, app2)
    }
}
