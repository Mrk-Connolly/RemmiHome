package com.remmi.core.controller

import com.remmi.core.android.launcher.RemmiAppInfo
import com.remmi.core.android.launcher.RemmiLauncherCapability
import com.remmi.core.eventBus.commands.LaunchAppCommand
import com.remmi.core.eventBus.commands.NavigateCommand
import com.remmi.core.eventBus.commands.RemmiCommand
import com.remmi.core.eventBus.commands.RemmiCommandResult
import com.remmi.core.host.RemmiHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandRoutingTest {

    private val sampleApp = RemmiAppInfo(
        packageName = "com.example.app",
        activityName = "MainActivity",
        label = "Sample App"
    )

    private class FakeLauncherCapability(
        private val shouldSucceed: Boolean
    ) : RemmiLauncherCapability {
        override val installedApps: StateFlow<List<RemmiAppInfo>> = MutableStateFlow(emptyList())
        override suspend fun refreshInstalledApps() {}
        override fun launchApp(appInfo: RemmiAppInfo): Boolean = shouldSucceed
    }

    private data class UnhandledCommand(val id: Int = 1) : RemmiCommand

    @Test
    fun executeCommand_launchAppSuccess_returnsSuccessResult() {
        val host = RemmiHost(launcherCapability = FakeLauncherCapability(shouldSucceed = true))
        val controller = RemmiController(host)

        val result = controller.executeCommand(LaunchAppCommand(sampleApp))

        assertTrue(result is RemmiCommandResult.Success)
        val success = result as RemmiCommandResult.Success
        assertEquals("Launched Sample App", success.message)
    }

    @Test
    fun executeCommand_launchAppFailure_returnsFailureResult() {
        val host = RemmiHost(launcherCapability = FakeLauncherCapability(shouldSucceed = false))
        val controller = RemmiController(host)

        val result = controller.executeCommand(LaunchAppCommand(sampleApp))

        assertTrue(result is RemmiCommandResult.Failure)
        val failure = result as RemmiCommandResult.Failure
        assertEquals("Failed to launch Sample App", failure.reason)
    }

    @Test
    fun executeCommand_launcherCapabilityMissing_returnsFailureResult() {
        val host = RemmiHost(launcherCapability = null)
        val controller = RemmiController(host)

        val result = controller.executeCommand(LaunchAppCommand(sampleApp))

        assertTrue(result is RemmiCommandResult.Failure)
        val failure = result as RemmiCommandResult.Failure
        assertEquals("Launcher capability is unavailable", failure.reason)
    }

    @Test
    fun executeCommand_navigateCommand_returnsSuccessResult() {
        val host = RemmiHost()
        val controller = RemmiController(host)

        val result = controller.executeCommand(NavigateCommand("WORKSPACES"))

        assertTrue(result is RemmiCommandResult.Success)
        val success = result as RemmiCommandResult.Success
        assertEquals("Navigation requested to WORKSPACES", success.message)
    }

    @Test
    fun executeCommand_unhandledCommand_returnsFailureResult() {
        val host = RemmiHost()
        val controller = RemmiController(host)

        val result = controller.executeCommand(UnhandledCommand())

        assertTrue(result is RemmiCommandResult.Failure)
        val failure = result as RemmiCommandResult.Failure
        assertEquals("Unhandled command type: UnhandledCommand", failure.reason)
    }

    @Test
    fun executeCommand_emitsCommandToEventBus() = runBlocking {
        val host = RemmiHost()
        val controller = RemmiController(host)
        val command = NavigateCommand("APPS")

        var receivedCommand: NavigateCommand? = null
        val job = launch(Dispatchers.Unconfined) {
            receivedCommand = host.eventBus.observeCommands(NavigateCommand::class.java).first()
        }

        controller.executeCommand(command)

        job.join()
        assertEquals("APPS", receivedCommand?.destinationId)
    }

    @Test
    fun executeCommandAsync_executesOffCallingThread() = runBlocking {
        val host = RemmiHost()
        val controller = RemmiController(host)
        val command = NavigateCommand("HOME")

        val result = controller.executeCommandAsync(command)

        assertTrue(result is RemmiCommandResult.Success)
        val success = result as RemmiCommandResult.Success
        assertEquals("Navigation requested to HOME", success.message)
    }
}
