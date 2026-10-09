package com.remmi.core.ai.tools

import com.remmi.core.ai.tools.models.AiToolCallRequest
import com.remmi.core.ai.tools.models.ToolAccessType
import com.remmi.core.context.dailystate.DailyStateRepository
import com.remmi.core.context.repository.PersonalContextRepository
import com.remmi.plugins.health.repository.HealthRepository
import com.remmi.plugins.knowledge.academic.models.AcademicCourse
import com.remmi.plugins.knowledge.academic.models.AcademicExam
import com.remmi.plugins.knowledge.academic.repository.AcademicRepository
import com.remmi.plugins.planning.calendar.repository.CalendarEventRepository
import com.remmi.plugins.planning.repository.PlanningTaskRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RemmiAiToolRegistryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var calendarRepo: CalendarEventRepository
    private lateinit var planningTaskRepo: PlanningTaskRepository
    private lateinit var academicRepo: AcademicRepository
    private lateinit var healthRepo: HealthRepository
    private lateinit var personalContextRepo: PersonalContextRepository
    private lateinit var dailyStateRepo: DailyStateRepository
    private lateinit var registry: RemmiAiToolRegistry

    private val currentDateIso = "2026-10-06"

    @Before
    fun setUp() {
        val folder = tempFolder.newFolder("ai_tools_test")
        calendarRepo = CalendarEventRepository(storageDir = folder)
        planningTaskRepo = PlanningTaskRepository(storageDir = folder)
        academicRepo = AcademicRepository(storageDir = folder)
        healthRepo = HealthRepository(storageDir = folder)
        personalContextRepo = PersonalContextRepository(storageDir = folder)
        dailyStateRepo = DailyStateRepository(storageDir = folder)

        registry = RemmiAiToolRegistry(
            calendarRepo = calendarRepo,
            planningTaskRepo = planningTaskRepo,
            academicRepo = academicRepo,
            healthRepo = healthRepo,
            personalContextRepo = personalContextRepo,
            dailyStateRepo = dailyStateRepo
        )
    }

    @Test
    fun testDefaultToolsAreRegistered() {
        val defs = registry.getToolDefinitions()
        assertTrue("Registry must contain default AI tools", defs.isNotEmpty())
        assertTrue(defs.any { it.name == "get_events" })
        assertTrue(defs.any { it.name == "create_task" })
        assertTrue(defs.any { it.name == "delete_event" && it.accessType == ToolAccessType.WRITE })
    }

    @Test
    fun testParameterValidationFailure() = runBlocking {
        // Missing required parameter "dateIso" for get_events
        val request = AiToolCallRequest(toolName = "get_events", parameters = emptyMap())
        val result = registry.executeTool(request)

        assertFalse(result.success)
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage!!.contains("Missing required parameter 'dateIso'"))
    }

    @Test
    fun testHighRiskOperationRequiresConfirmation() = runBlocking {
        // High-risk tool delete_event without confirmation
        val request = AiToolCallRequest(
            toolName = "delete_event",
            parameters = mapOf("eventId" to "evt_123"),
            userConfirmed = false
        )
        val result = registry.executeTool(request)

        assertFalse(result.success)
        assertTrue(result.requiresConfirmation)
        assertTrue(result.errorMessage!!.contains("requires user confirmation"))
    }

    @Test
    fun testHighRiskOperationSucceedsWhenConfirmed() = runBlocking {
        // High-risk tool delete_event with userConfirmed = true
        val request = AiToolCallRequest(
            toolName = "delete_event",
            parameters = mapOf("eventId" to "evt_123"),
            userConfirmed = true
        )
        val result = registry.executeTool(request)

        assertTrue(result.success)
        assertFalse(result.requiresConfirmation)
        assertTrue(result.data.contains("deleted successfully"))
    }

    @Test
    fun testLowRiskWriteAndReadToolsExecution() = runBlocking {
        // Create task tool
        val createReq = AiToolCallRequest(
            toolName = "create_task",
            parameters = mapOf("title" to "Study for Machine Learning Exam")
        )
        val createRes = registry.executeTool(createReq)
        assertTrue(createRes.success)
        assertTrue(createRes.data.contains("created"))

        // Get tasks tool
        val getReq = AiToolCallRequest(toolName = "get_tasks")
        val getRes = registry.executeTool(getReq)
        assertTrue(getRes.success)
        assertEquals(ToolAccessType.READ, getRes.accessType)
    }

    @Test
    fun testAcademicToolsExecution() = runBlocking {
        val course = AcademicCourse(degreeId = "deg_1", courseCode = "CS501", title = "Advanced ML")
        academicRepo.addCourse(course)
        academicRepo.addExam(AcademicExam(courseId = course.id, title = "Final Exam", examDateIso = "2026-10-20"))

        val req = AiToolCallRequest(
            toolName = "get_exams",
            parameters = mapOf("currentDateIso" to currentDateIso)
        )
        val res = registry.executeTool(req)

        assertTrue(res.success)
        assertTrue(res.data.contains("Final Exam"))
    }

    @Test
    fun testActionLogTracking() = runBlocking {
        registry.executeTool(AiToolCallRequest(toolName = "get_tasks"))
        registry.executeTool(AiToolCallRequest(toolName = "create_task", parameters = mapOf("title" to "Review Notes")))

        val logs = registry.actionLog.value
        assertEquals(2, logs.size)
        assertEquals("get_tasks", logs[0].toolName)
        assertEquals("create_task", logs[1].toolName)
    }
}
