package com.remmi.core.ai

import com.remmi.core.android.launcher.RemmiAppInfo
import com.remmi.core.controller.RemmiController
import com.remmi.core.eventBus.commands.LaunchAppCommand
import com.remmi.core.eventBus.commands.NavigateCommand
import com.remmi.core.eventBus.commands.RemmiCommandResult
import com.remmi.core.host.RemmiHost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiCapabilityBoundaryTest {

    private val sampleApps = listOf(
        RemmiAppInfo("com.google.android.GoogleCamera", "CameraActivity", "Camera"),
        RemmiAppInfo("org.mozilla.firefox", "MainActivity", "Firefox Browser")
    )

    @Test
    fun resolveIntent_navigationIntents_resolvesToNavigateCommand() {
        val aiCapability = DefaultAiCapability()

        val commandWorkspaces = aiCapability.resolveIntent("go to workspaces")
        assertTrue(commandWorkspaces is NavigateCommand)
        assertEquals("WORKSPACES", (commandWorkspaces as NavigateCommand).destinationId)

        val commandApps = aiCapability.resolveIntent("open app list")
        assertTrue(commandApps is NavigateCommand)
        assertEquals("APPS", (commandApps as NavigateCommand).destinationId)

        val commandHome = aiCapability.resolveIntent("show home")
        assertTrue(commandHome is NavigateCommand)
        assertEquals("HOME", (commandHome as NavigateCommand).destinationId)
    }

    @Test
    fun resolveIntent_appLaunchIntents_resolvesToLaunchAppCommand() {
        val aiCapability = DefaultAiCapability(installedAppsProvider = { sampleApps })

        val commandCamera = aiCapability.resolveIntent("launch camera")
        assertTrue(commandCamera is LaunchAppCommand)
        assertEquals("com.google.android.GoogleCamera", (commandCamera as LaunchAppCommand).appInfo.packageName)

        val commandFirefox = aiCapability.resolveIntent("open firefox")
        assertTrue(commandFirefox is LaunchAppCommand)
        assertEquals("org.mozilla.firefox", (commandFirefox as LaunchAppCommand).appInfo.packageName)
    }

    @Test
    fun resolveIntent_unrecognizedInput_returnsNull() {
        val aiCapability = DefaultAiCapability(installedAppsProvider = { sampleApps })

        val command = aiCapability.resolveIntent("do something unsupported")
        assertNull(command)
    }

    @Test
    fun processInput_validIntent_executesThroughControllerRouter() {
        val host = RemmiHost()
        val controller = RemmiController(host)
        val aiCapability = DefaultAiCapability()

        val result = aiCapability.processInput("go to workspaces", controller::executeCommand)

        assertTrue(result is RemmiCommandResult.Success)
        val success = result as RemmiCommandResult.Success
        assertEquals("Navigation requested to WORKSPACES", success.message)
    }

    @Test
    fun processInput_unrecognizedIntent_returnsFailureResult() {
        val host = RemmiHost()
        val controller = RemmiController(host)
        val aiCapability = DefaultAiCapability()

        val result = aiCapability.processInput("fly to Mars", controller::executeCommand)

        assertTrue(result is RemmiCommandResult.Failure)
        val failure = result as RemmiCommandResult.Failure
        assertEquals("Unrecognized AI intent for input: 'fly to Mars'", failure.reason)
    }

    @Test
    fun host_containsAiCapabilityReference() {
        val host = RemmiHost()
        assertNotNull(host.aiCapability)
    }
}
