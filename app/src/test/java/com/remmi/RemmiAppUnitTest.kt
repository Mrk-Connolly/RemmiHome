package com.remmi

import com.remmi.core.RemmiApplication
import com.remmi.plugins.PluginsMarker
import org.junit.Assert.assertNotNull
import org.junit.Test

class RemmiAppUnitTest {

    @Test
    fun application_initializesSuccessfully() {
        val app = RemmiApplication()
        assertNotNull("RemmiApplication should instantiate cleanly", app)
    }

    @Test
    fun pluginBoundary_isDefined() {
        assertNotNull("PluginsMarker should exist as a package anchor", PluginsMarker)
    }
}
