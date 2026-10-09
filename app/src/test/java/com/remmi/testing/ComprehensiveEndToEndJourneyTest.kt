package com.remmi.testing

import com.remmi.core.controller.RemmiController
import com.remmi.core.eventBus.commands.NavigateCommand
import com.remmi.core.eventBus.commands.RemmiCommandResult
import com.remmi.core.host.RemmiHost
import com.remmi.core.intelligence.CrossWorkspaceContext
import com.remmi.core.intelligence.CrossWorkspaceIntelligenceEngine
import com.remmi.core.intelligence.HealthSnapshot
import com.remmi.core.intelligence.KnowledgeSnapshot
import com.remmi.core.intelligence.PlanningSnapshot
import com.remmi.plugins.health.HealthPlugin
import com.remmi.plugins.health.models.ActivityIntensity
import com.remmi.plugins.health.models.ActivityLog
import com.remmi.plugins.health.models.ActivityType
import com.remmi.plugins.health.models.FoodLog
import com.remmi.plugins.health.models.Habit
import com.remmi.plugins.health.models.MealType
import com.remmi.plugins.health.models.ObservationDimension
import com.remmi.plugins.health.models.SleepLog
import com.remmi.plugins.health.models.SubjectiveObservation
import com.remmi.plugins.knowledge.KnowledgePlugin
import com.remmi.plugins.planning.PlanningPlugin
import com.remmi.plugins.planning.tasks.commands.CreateTaskCommand
import com.remmi.plugins.planning.tasks.models.TaskItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ComprehensiveEndToEndJourneyTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `end to end multi workspace user journey completes successfully`() = runBlocking {
        val storageDir = tempFolder.newFolder("e2e_storage")

        val host = RemmiHost()
        val controller = RemmiController(host)

        // 1. Navigation Flow
        val navHome = controller.executeCommand(NavigateCommand("HOME"))
        assertTrue(navHome is RemmiCommandResult.Success)

        val navWorkspaces = controller.executeCommand(NavigateCommand("WORKSPACES"))
        assertTrue(navWorkspaces is RemmiCommandResult.Success)

        // 2. Planning Workspace Journey
        val planningPlugin = PlanningPlugin()
        host.pluginRegistry.register(planningPlugin)
        host.pluginRegistry.initializeAll(host)

        val taskCommand = CreateTaskCommand(TaskItem(title = "E2E Study Task"))
        val taskResult = controller.executeCommand(taskCommand)
        assertTrue(taskResult is RemmiCommandResult.Success)

        planningPlugin.taskRepository.addTask(TaskItem(title = "E2E Study Task"))
        assertEquals(1, planningPlugin.taskRepository.tasks.value.size)

        // 3. Knowledge Workspace Journey
        val knowledgePlugin = KnowledgePlugin()
        host.pluginRegistry.register(knowledgePlugin)
        host.pluginRegistry.initializeAll(host)

        val cls = knowledgePlugin.classRepository.createClass("E2E Computer Science")
        val lec = knowledgePlugin.lectureRepository.createLecture("E2E Algorithms", cls.id)
        assertNotNull(cls)
        assertNotNull(lec)
        assertEquals(1, knowledgePlugin.classRepository.classes.value.size)
        assertEquals(1, knowledgePlugin.lectureRepository.lectures.value.size)

        // 4. Health Workspace Journey
        val healthRepo = com.remmi.plugins.health.repository.HealthRepository(storageDir = storageDir)
        val healthPlugin = HealthPlugin(healthRepository = healthRepo)
        host.pluginRegistry.register(healthPlugin)
        host.pluginRegistry.initializeAll(host)

        healthRepo.addObservation(SubjectiveObservation(dimension = ObservationDimension.ENERGY, rating = 2))
        healthRepo.addObservation(SubjectiveObservation(dimension = ObservationDimension.STRESS, rating = 4))
        healthRepo.addSleepLog(SleepLog(sleepStart = 0L, sleepEnd = 18000000L, durationMinutes = 300, quality = 2)) // 5h
        healthRepo.addActivityLog(ActivityLog(type = ActivityType.WALKING, durationMinutes = 20, intensity = ActivityIntensity.LOW))
        healthRepo.addFoodLog(FoodLog(mealType = MealType.BREAKFAST, caffeineMg = 80, hydrationMl = 250))
        healthRepo.addHabit(Habit(name = "Daily Walk"))

        assertEquals(2, healthRepo.observations.value.size)
        assertEquals(1, healthRepo.sleepLogs.value.size)
        assertEquals(1, healthRepo.activityLogs.value.size)
        assertEquals(1, healthRepo.foodLogs.value.size)
        assertEquals(1, healthRepo.habits.value.size)

        // 5. Health AI Evaluation
        val evaluation = healthPlugin.aiEvaluator.evaluate(
            observations = healthRepo.observations.value,
            sleepLogs = healthRepo.sleepLogs.value,
            activityLogs = healthRepo.activityLogs.value,
            foodLogs = healthRepo.foodLogs.value,
            baselines = emptyList()
        )
        assertNotNull(evaluation)
        assertFalse(evaluation.recommendations.isEmpty())

        // 6. Cross-Workspace Intelligence Evaluation
        val intelEngine = CrossWorkspaceIntelligenceEngine()
        val crossContext = CrossWorkspaceContext(
            planning = PlanningSnapshot(pendingTaskCount = planningPlugin.taskRepository.tasks.value.size),
            health = HealthSnapshot(
                latestEnergy = 2,
                latestStress = 4,
                latestSleepDurationMinutes = 300
            ),
            knowledge = KnowledgeSnapshot(classCount = knowledgePlugin.classRepository.classes.value.size)
        )

        val crossRecommendations = intelEngine.evaluate(crossContext)
        assertFalse(crossRecommendations.isEmpty())
        assertTrue(crossRecommendations.any { it.sourceWorkspaces.contains("Health") && it.sourceWorkspaces.contains("Planning") })
    }
}
