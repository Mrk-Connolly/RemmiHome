package com.remmi.core.eventBus

import com.remmi.core.android.launcher.RemmiAppInfo
import com.remmi.core.eventBus.commands.LaunchAppCommand
import com.remmi.core.eventBus.commands.RemmiCommand
import com.remmi.ui.search.GlobalSearchResult
import com.remmi.ui.search.MatchType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandArchitectureTest {

    private val sampleApp = RemmiAppInfo(
        packageName = "com.google.android.GoogleCamera",
        activityName = "CameraActivity",
        label = "Camera"
    )

    @Test
    fun launchAppCommand_implementsRemmiCommandAndContainsNoAndroidDependencies() {
        val command: RemmiCommand = LaunchAppCommand(sampleApp)
        val typedCommand = command as LaunchAppCommand
        assertEquals("com.google.android.GoogleCamera", typedCommand.appInfo.packageName)
        assertEquals("CameraActivity", typedCommand.appInfo.activityName)
    }

    @Test
    fun globalSearchResult_mapsToLaunchAppCommand() {
        val searchResult = GlobalSearchResult(
            appInfo = sampleApp,
            isFavorite = true,
            matchType = MatchType.EXACT
        )

        val command = searchResult.toCommand()
        assertEquals("com.google.android.GoogleCamera", command.appInfo.packageName)
        assertEquals("Camera", command.appInfo.label)
    }

    @Test
    fun eventBus_dispatchesAndObservesLaunchAppCommand() = runBlocking {
        val eventBus = RemmiEventBus()
        val command = LaunchAppCommand(sampleApp)

        var observedCommand: LaunchAppCommand? = null
        val job = launch(Dispatchers.Unconfined) {
            observedCommand = eventBus.commandsOfType<LaunchAppCommand>().first()
        }

        val success = eventBus.sendCommand(command)
        assertTrue(success)

        job.join()
        assertEquals("com.google.android.GoogleCamera", observedCommand?.appInfo?.packageName)
    }
}
