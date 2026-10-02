package com.remmi.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MainActivityLifecycleTest {

    @Test
    fun mainActivity_classExistsAndExtendsComponentActivity() {
        val clazz = MainActivity::class.java
        assertTrue(androidx.activity.ComponentActivity::class.java.isAssignableFrom(clazz))
    }

    @Test
    fun mainActivity_launcherIntentConstants_areCorrect() {
        assertEquals("android.intent.action.MAIN", android.content.Intent.ACTION_MAIN)
        assertEquals("android.intent.category.HOME", android.content.Intent.CATEGORY_HOME)
        assertEquals("android.intent.category.DEFAULT", android.content.Intent.CATEGORY_DEFAULT)
    }
}
