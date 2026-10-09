package com.remmi.testing

import com.remmi.core.ai.DefaultAiCapability
import com.remmi.core.android.launcher.RemmiAppInfo
import com.remmi.core.controller.RemmiController
import com.remmi.core.eventBus.commands.LaunchAppCommand
import com.remmi.core.eventBus.commands.RemmiCommandResult
import com.remmi.core.host.RemmiHost
import com.remmi.core.intelligence.CrossWorkspaceContext
import com.remmi.core.intelligence.CrossWorkspaceIntelligenceEngine
import com.remmi.plugins.health.evaluator.HealthAiEvaluator
import com.remmi.plugins.health.repository.HealthRepository
import com.remmi.plugins.knowledge.lecturerecorder.repository.ClassRepository
import com.remmi.plugins.knowledge.lecturerecorder.repository.LectureRepository
import com.remmi.plugins.planning.tasks.repository.TaskRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ComprehensiveFailureRecoveryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `system recovers safely across multi failure scenarios simultaneously`() = runBlocking {
        val corruptedDir = tempFolder.newFolder("corrupted_storage")

        // 1. Storage Corruption in all repositories
        File(corruptedDir, "health_observations.json").writeText("{ corrupt: json }")
        File(corruptedDir, "health_sleep.json").writeText("TRASH_DATA")
        File(corruptedDir, "lecture_classes.json").writeText("[ malformed_array ")
        File(corruptedDir, "planning_tasks.json").writeText("{ bad_task: 123 ")

        val healthRepo = HealthRepository(storageDir = corruptedDir)
        val classRepo = ClassRepository(storageDir = corruptedDir)
        val lectureRepo = LectureRepository(storageDir = corruptedDir)
        val taskRepo = TaskRepository(storageDir = corruptedDir)

        healthRepo.loadFromStorage()
        classRepo.loadFromStorage()
        lectureRepo.loadFromStorage()
        taskRepo.loadFromStorage()

        assertEquals(0, healthRepo.observations.value.size)
        assertEquals(0, classRepo.classes.value.size)
        assertEquals(0, lectureRepo.lectures.value.size)
        assertEquals(0, taskRepo.tasks.value.size)

        // 2. Offline AI Evaluation and Cross-Workspace Intelligence
        val evaluator = HealthAiEvaluator()
        val evaluation = evaluator.evaluate(
            observations = emptyList(),
            sleepLogs = emptyList(),
            activityLogs = emptyList(),
            foodLogs = emptyList(),
            baselines = emptyList()
        )
        assertNotNull(evaluation)

        val intelEngine = CrossWorkspaceIntelligenceEngine()
        val crossRecs = intelEngine.evaluate(CrossWorkspaceContext())
        assertEquals(1, crossRecs.size)

        // 3. Unresolvable AI Inputs & Null Launcher
        val aiCap = DefaultAiCapability()
        assertNull(aiCap.resolveIntent("non_existent_command_xyz"))

        val host = RemmiHost(launcherCapability = null)
        val controller = RemmiController(host)
        val dummyApp = RemmiAppInfo(packageName = "com.pkg", activityName = "com.pkg.Act", label = "Dummy App")
        val launchResult = controller.executeCommand(LaunchAppCommand(dummyApp))

        assertTrue(launchResult is RemmiCommandResult.Failure)
        assertEquals("Launcher capability is unavailable", (launchResult as RemmiCommandResult.Failure).reason)
    }
}
