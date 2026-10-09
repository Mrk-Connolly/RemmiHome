package com.remmi.core.ai

import com.remmi.core.android.launcher.RemmiAppInfo
import com.remmi.core.controller.RemmiController
import com.remmi.core.eventBus.commands.LaunchAppCommand
import com.remmi.core.eventBus.commands.NavigateCommand
import com.remmi.core.eventBus.commands.RemmiCommandResult
import com.remmi.core.host.RemmiHost
import com.remmi.plugins.planning.alarms.commands.CreateAlarmCommand
import com.remmi.plugins.planning.calendar.commands.CreateCalendarEventCommand
import com.remmi.plugins.planning.tasks.commands.CreateTaskCommand
import com.remmi.ui.search.resolveAiSearchCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiCommandResolutionTest {

    private val sampleApps = listOf(
        RemmiAppInfo("com.google.android.GoogleCamera", "CameraActivity", "Camera"),
        RemmiAppInfo("org.mozilla.firefox", "MainActivity", "Firefox"),
        RemmiAppInfo("com.android.calculator2", "Calculator", "Calculator")
    )

    @Test
    fun resolveIntent_diverseNavigationPhrases() {
        val aiCapability = DefaultAiCapability()

        val navWorkspaces = aiCapability.resolveIntent("switch to workspaces")
        assertTrue(navWorkspaces is NavigateCommand)
        assertEquals("WORKSPACES", (navWorkspaces as NavigateCommand).destinationId)

        val navPlanning = aiCapability.resolveIntent("open planning")
        assertTrue(navPlanning is NavigateCommand)
        assertEquals("PLANNING", (navPlanning as NavigateCommand).destinationId)

        val navSettings = aiCapability.resolveIntent("open settings")
        assertTrue(navSettings is NavigateCommand)
        assertEquals("SETTINGS", (navSettings as NavigateCommand).destinationId)

        val navApps = aiCapability.resolveIntent("navigate to apps")
        assertTrue(navApps is NavigateCommand)
        assertEquals("APPS", (navApps as NavigateCommand).destinationId)

        val navHome = aiCapability.resolveIntent("go to remmi home")
        assertTrue(navHome is NavigateCommand)
        assertEquals("HOME", (navHome as NavigateCommand).destinationId)
    }

    @Test
    fun resolveIntent_planningCommands_resolvesToTypedCommands() {
        val aiCapability = DefaultAiCapability()

        val createTask = aiCapability.resolveIntent("create task Buy Milk")
        assertTrue(createTask is CreateTaskCommand)
        assertEquals("Buy Milk", (createTask as CreateTaskCommand).task.title)

        val createAlarm = aiCapability.resolveIntent("set alarm Morning 07:00")
        assertTrue(createAlarm is CreateAlarmCommand)
        assertEquals("Morning", (createAlarm as CreateAlarmCommand).alarm.title)
        assertEquals("07:00", (createAlarm as CreateAlarmCommand).alarm.timeIso)

        val createEvent = aiCapability.resolveIntent("add event Architecture Review")
        assertTrue(createEvent is CreateCalendarEventCommand)
        assertEquals("Architecture Review", (createEvent as CreateCalendarEventCommand).event.title)
    }

    @Test
    fun resolveIntent_diverseAppLaunchPhrases() {
        val aiCapability = DefaultAiCapability(installedAppsProvider = { sampleApps })

        val launchFirefox = aiCapability.resolveIntent("run firefox")
        assertTrue(launchFirefox is LaunchAppCommand)
        assertEquals("org.mozilla.firefox", (launchFirefox as LaunchAppCommand).appInfo.packageName)

        val launchCamera = aiCapability.resolveIntent("exec camera")
        assertTrue(launchCamera is LaunchAppCommand)
        assertEquals("com.google.android.GoogleCamera", (launchCamera as LaunchAppCommand).appInfo.packageName)

        val launchCalc = aiCapability.resolveIntent("start calculator")
        assertTrue(launchCalc is LaunchAppCommand)
        assertEquals("com.android.calculator2", (launchCalc as LaunchAppCommand).appInfo.packageName)
    }

    @Test
    fun resolveIntent_unsupportedOperations_strictlyRejectsWithoutInvention() {
        val aiCapability = DefaultAiCapability(installedAppsProvider = { sampleApps })

        val unsupportedActions = listOf(
            "delete file",
            "send text message",
            "format disk",
            "call mom",
            "make coffee",
            "turn off lights"
        )

        for (action in unsupportedActions) {
            val command = aiCapability.resolveIntent(action)
            assertNull("Action '$action' should resolve to null", command)
        }
    }

    @Test
    fun processInput_unsupportedOperation_returnsFailure() {
        val host = RemmiHost()
        val controller = RemmiController(host)
        val aiCapability = DefaultAiCapability(installedAppsProvider = { sampleApps })

        val result = aiCapability.processInput("format disk", controller::executeCommand)

        assertTrue(result is RemmiCommandResult.Failure)
        val failure = result as RemmiCommandResult.Failure
        assertEquals("Unrecognized AI intent for input: 'format disk'", failure.reason)
    }

    @Test
    fun resolveAiSearchCommand_integratesWithSearchHelper() {
        val aiCapability = DefaultAiCapability(installedAppsProvider = { sampleApps })

        val command = resolveAiSearchCommand("launch camera", aiCapability)
        assertTrue(command is LaunchAppCommand)
        assertEquals("com.google.android.GoogleCamera", (command as LaunchAppCommand).appInfo.packageName)

        val blankCommand = resolveAiSearchCommand("", aiCapability)
        assertNull(blankCommand)
    }
}
