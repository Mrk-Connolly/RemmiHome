package com.remmi.release

import com.remmi.core.host.RemmiHost
import com.remmi.plugins.health.HealthPlugin
import com.remmi.plugins.knowledge.KnowledgePlugin
import com.remmi.plugins.planning.PlanningPlugin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FinalReleaseReadinessTest {

    @Test
    fun `architecture boundaries audit enforces clean package hierarchy`() {
        val appSrcDir = File("src/main/java/com/remmi").let { if (it.exists()) it else File("app/src/main/java/com/remmi") }
        val coreSrcDir = File("../core/src/main/java/com/remmi").let { if (it.exists()) it else File("core/src/main/java/com/remmi") }
        val pluginsSrcDir = File("../plugins/src/main/java/com/remmi/plugins").let { if (it.exists()) it else File("plugins/src/main/java/com/remmi/plugins") }

        assertTrue("App source directory com/remmi must exist", appSrcDir.exists())
        assertTrue("Core module source directory com/remmi must exist", coreSrcDir.exists())
        assertTrue("Plugins module source directory com/remmi/plugins must exist", pluginsSrcDir.exists())

        val appSubDirs = appSrcDir.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()
        val coreSubDirs = coreSrcDir.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()

        // Core packages must be present in their respective modules
        assertTrue("app or core must contain core", appSubDirs.contains("core") || coreSubDirs.contains("core"))
        assertTrue("app or core must contain ui", appSubDirs.contains("ui") || coreSubDirs.contains("ui"))

        // Forbid generic dumping grounds at top level
        assertFalse("Must not contain generic dumping ground 'models'", appSubDirs.contains("models"))
        assertFalse("Must not contain generic dumping ground 'utils'", appSubDirs.contains("utils"))
        assertFalse("Must not contain generic dumping ground 'helpers'", appSubDirs.contains("helpers"))
        assertFalse("Must not contain generic dumping ground 'common'", appSubDirs.contains("common"))
        assertFalse("Must not contain generic dumping ground 'shared'", appSubDirs.contains("shared"))
        assertFalse("Must not contain generic dumping ground 'misc'", appSubDirs.contains("misc"))
    }

    @Test
    fun `forbidden architecture audit guarantees zero room sqlite hilt dagger or koin`() {
        val rootGradle = File("../build.gradle.kts").let { if (it.exists()) it else File("build.gradle.kts") }
        val appGradle = File("build.gradle.kts").let { if (it.exists()) it else File("app/build.gradle.kts") }

        val combinedGradleText = (if (rootGradle.exists()) rootGradle.readText() else "") + "\n" + (if (appGradle.exists()) appGradle.readText() else "")

        assertFalse("Must not import Room", combinedGradleText.contains("androidx.room"))
        assertFalse("Must not import SQLite", combinedGradleText.contains("androidx.sqlite"))
        assertFalse("Must not import Hilt", combinedGradleText.contains("hilt"))
        assertFalse("Must not import Dagger", combinedGradleText.contains("dagger"))
        assertFalse("Must not import Koin", combinedGradleText.contains("koin"))
    }

    @Test
    fun `host and plugin registry manage all first class plugins cleanly`() {
        val host = RemmiHost()

        val planningPlugin = PlanningPlugin()
        val knowledgePlugin = KnowledgePlugin()
        val healthPlugin = HealthPlugin()

        host.pluginRegistry.register(planningPlugin)
        host.pluginRegistry.register(knowledgePlugin)
        host.pluginRegistry.register(healthPlugin)

        assertEquals(3, host.pluginRegistry.plugins.size)

        val initResults = host.pluginRegistry.initializeAll(host)
        assertEquals(3, initResults.size)
        assertTrue(initResults.values.all { it.isSuccess })

        val shutdownResults = host.pluginRegistry.shutdownAll()
        assertEquals(3, shutdownResults.size)
        assertTrue(shutdownResults.values.all { it.isSuccess })
    }

    @Test
    fun `all core capability containers initialize without errors`() {
        val host = RemmiHost()
        assertNotNull(host.systemInfoCapability)
        assertNotNull(host.aiCapability)
        assertNotNull(host.audioCapability)
        assertNotNull(host.alarmCapability)
        assertNotNull(host.notificationCapability)
    }
}
