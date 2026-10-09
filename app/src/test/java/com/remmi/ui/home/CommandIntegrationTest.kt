package com.remmi.ui.home

import com.remmi.core.android.launcher.RemmiAppInfo
import com.remmi.core.android.launcher.RemmiLauncherCapability
import com.remmi.core.controller.RemmiController
import com.remmi.core.eventBus.commands.LaunchAppCommand
import com.remmi.core.eventBus.commands.NavigateCommand
import com.remmi.core.eventBus.commands.RemmiCommandResult
import com.remmi.core.host.RemmiHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandIntegrationTest {

    private class FakeLauncherCapability : RemmiLauncherCapability {
        private val _installedApps = MutableStateFlow<List<RemmiAppInfo>>(emptyList())
        override val installedApps: StateFlow<List<RemmiAppInfo>> = _installedApps.asStateFlow()

        var launchedApp: RemmiAppInfo? = null

        override suspend fun refreshInstalledApps() {}

        override fun launchApp(appInfo: RemmiAppInfo): Boolean {
            launchedApp = appInfo
            return true
        }
    }

    @Test
    fun executeCommand_launchesAppAndEmitsToEventBus() = runBlocking {
        val launcher = FakeLauncherCapability()
        val host = RemmiHost(launcherCapability = launcher)
        val controller = RemmiController(host)

        val targetApp = RemmiAppInfo("com.example.maps", "MapsActivity", "Maps")
        val command = LaunchAppCommand(targetApp)

        var observedCommand: LaunchAppCommand? = null
        val job = launch(Dispatchers.Unconfined) {
            observedCommand = host.eventBus.observeCommands(LaunchAppCommand::class.java).first()
        }

        val result = controller.executeCommand(command)

        assertTrue(result is RemmiCommandResult.Success)
        job.join()
        assertEquals("com.example.maps", launcher.launchedApp?.packageName)
        assertEquals("com.example.maps", observedCommand?.appInfo?.packageName)
    }

    @Test
    fun executeNavigateCommand_emitsToEventBusAndReturnsSuccess() = runBlocking {
        val host = RemmiHost()
        val controller = RemmiController(host)
        val command = NavigateCommand("WORKSPACES")

        var observedCommand: NavigateCommand? = null
        val job = launch(Dispatchers.Unconfined) {
            observedCommand = host.eventBus.observeCommands(NavigateCommand::class.java).first()
        }

        val result = controller.executeCommand(command)

        assertTrue(result is RemmiCommandResult.Success)
        job.join()
        assertEquals("WORKSPACES", observedCommand?.destinationId)
    }
}
