package com.remmi.reliability

import com.remmi.core.android.launcher.RemmiAppInfo
import com.remmi.core.controller.RemmiController
import com.remmi.core.eventBus.commands.LaunchAppCommand
import com.remmi.core.eventBus.commands.NavigateCommand
import com.remmi.core.eventBus.commands.RemmiCommand
import com.remmi.core.eventBus.commands.RemmiCommandResult
import com.remmi.core.host.RemmiHost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LifecycleReliabilityTest {

    private class UnknownTestCommand : RemmiCommand

    @Test
    fun `host initializes core capabilities safely`() {
        val host = RemmiHost()
        assertNotNull(host.eventBus)
        assertNotNull(host.scope)
        assertNotNull(host.pluginRegistry)
        assertNotNull(host.systemInfoCapability)
        assertNotNull(host.aiCapability)
        assertNotNull(host.audioCapability)
        assertNotNull(host.alarmCapability)
        assertNotNull(host.notificationCapability)
    }

    @Test
    fun `controller routes valid and invalid commands gracefully without exceptions`() {
        val host = RemmiHost()
        val controller = RemmiController(host)

        val navResult = controller.executeCommand(NavigateCommand("HOME"))
        assertTrue(navResult is RemmiCommandResult.Success)

        val unknownCommand = UnknownTestCommand()
        val unknownResult = controller.executeCommand(unknownCommand)
        assertTrue(unknownResult is RemmiCommandResult.Failure)
        assertEquals("Unhandled command type: UnknownTestCommand", (unknownResult as RemmiCommandResult.Failure).reason)
    }

    @Test
    fun `controller handles launch app command gracefully when launcher is null`() {
        val host = RemmiHost(launcherCapability = null)
        val controller = RemmiController(host)

        val dummyApp = RemmiAppInfo(
            packageName = "com.example.app",
            activityName = "com.example.app.MainActivity",
            label = "Example App"
        )
        val result = controller.executeCommand(LaunchAppCommand(dummyApp))
        assertTrue(result is RemmiCommandResult.Failure)
        assertEquals("Launcher capability is unavailable", (result as RemmiCommandResult.Failure).reason)
    }
}
