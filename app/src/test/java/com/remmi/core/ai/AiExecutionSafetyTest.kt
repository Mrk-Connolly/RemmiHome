package com.remmi.core.ai

import com.remmi.core.android.launcher.RemmiAppInfo
import com.remmi.core.eventBus.commands.LaunchAppCommand
import com.remmi.core.eventBus.commands.NavigateCommand
import com.remmi.core.eventBus.commands.RemmiCommandResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiExecutionSafetyTest {

    private val validator = DefaultAiCommandValidator()

    @Test
    fun validator_validLaunchAppCommand_returnsNull() {
        val validApp = RemmiAppInfo("com.example.app", "MainActivity", "App")
        val command = LaunchAppCommand(validApp)

        val failure = validator.validateCommand(command)
        assertNull("Valid LaunchAppCommand should pass validation", failure)
    }

    @Test
    fun validator_invalidLaunchAppCommand_blankPackageName_returnsFailure() {
        val invalidApp = RemmiAppInfo("", "MainActivity", "App")
        val command = LaunchAppCommand(invalidApp)

        val failure = validator.validateCommand(command)
        assertTrue(failure is RemmiCommandResult.Failure)
        assertEquals("Invalid app launch command: packageName is blank", failure?.reason)
    }

    @Test
    fun validator_invalidLaunchAppCommand_blankActivityName_returnsFailure() {
        val invalidApp = RemmiAppInfo("com.example.app", "", "App")
        val command = LaunchAppCommand(invalidApp)

        val failure = validator.validateCommand(command)
        assertTrue(failure is RemmiCommandResult.Failure)
        assertEquals("Invalid app launch command: activityName is blank", failure?.reason)
    }

    @Test
    fun validator_validNavigateCommand_returnsNull() {
        val command = NavigateCommand("WORKSPACES")

        val failure = validator.validateCommand(command)
        assertNull("Valid NavigateCommand should pass validation", failure)
    }

    @Test
    fun validator_invalidNavigateCommand_unsupportedDestination_returnsFailure() {
        val command = NavigateCommand("INVALID_DESTINATION")

        val failure = validator.validateCommand(command)
        assertTrue(failure is RemmiCommandResult.Failure)
        assertEquals("Unsupported navigation destination: 'INVALID_DESTINATION'", failure?.reason)
    }

    @Test
    fun processInput_executionException_returnsFailureResultWithoutCrashing() {
        val sampleApp = RemmiAppInfo("com.example.app", "MainActivity", "App")
        val aiCapability = DefaultAiCapability(installedAppsProvider = { listOf(sampleApp) })

        val result = aiCapability.processInput("launch app") {
            throw RuntimeException("Simulated router crash")
        }

        assertTrue(result is RemmiCommandResult.Failure)
        val failure = result as RemmiCommandResult.Failure
        assertEquals("Command execution failed: Simulated router crash", failure.reason)
    }

    @Test
    fun requiresConfirmation_returnsFalseForStandardCommands() {
        val navCommand = NavigateCommand("HOME")
        val launchCommand = LaunchAppCommand(RemmiAppInfo("com.example.app", "MainActivity", "App"))

        assertFalse(validator.requiresConfirmation(navCommand))
        assertFalse(validator.requiresConfirmation(launchCommand))
    }
}
