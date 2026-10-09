package com.remmi.core.ai

import com.remmi.core.ai.agent.PersonalAiAgentOrchestrator
import com.remmi.core.ai.context.ContextDomain
import com.remmi.core.ai.permissions.repository.AiPermissionsRepository
import com.remmi.core.ai.tools.models.AiToolCallRequest
import com.remmi.core.ai.tools.models.ToolAccessType
import com.remmi.core.context.dailystate.DailyState
import com.remmi.core.context.dailystate.DailyStateRepository
import com.remmi.core.context.models.AiMemoryCategory
import com.remmi.core.context.models.AiMemoryItem
import com.remmi.core.context.models.GoalLevel
import com.remmi.core.context.models.PersonalGoal
import com.remmi.core.context.models.PersonalProfile
import com.remmi.core.context.repository.PersonalContextRepository
import com.remmi.core.host.RemmiHost
import com.remmi.plugins.health.models.DimensionAssessmentAnswer
import com.remmi.plugins.health.models.DimensionQuestionsCatalog
import com.remmi.plugins.health.models.HealthDimension8
import com.remmi.plugins.health.repository.HealthRepository
import com.remmi.plugins.knowledge.academic.models.AcademicCourse
import com.remmi.plugins.knowledge.academic.models.AcademicExam
import com.remmi.plugins.knowledge.academic.models.AcademicTopic
import com.remmi.plugins.knowledge.academic.models.DegreeProgram
import com.remmi.plugins.knowledge.academic.repository.AcademicRepository
import com.remmi.plugins.planning.calendar.models.CalendarEvent
import com.remmi.plugins.planning.calendar.repository.CalendarEventRepository
import com.remmi.plugins.planning.models.PlanningTask
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

class PersonalAiAgentMasterIntegrationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var host: RemmiHost
    private lateinit var personalContextRepo: PersonalContextRepository
    private lateinit var dailyStateRepo: DailyStateRepository
    private lateinit var healthRepo: HealthRepository
    private lateinit var academicRepo: AcademicRepository
    private lateinit var calendarRepo: CalendarEventRepository
    private lateinit var planningTaskRepo: PlanningTaskRepository
    private lateinit var permissionsRepo: AiPermissionsRepository
    private lateinit var orchestrator: PersonalAiAgentOrchestrator

    private val currentDateIso = "2026-10-06"
    private val currentTimeIso = "10:00"

    @Before
    fun setUp() {
        val folder = tempFolder.newFolder("master_ai_test")
        host = RemmiHost()
        personalContextRepo = PersonalContextRepository(storageDir = folder)
        dailyStateRepo = DailyStateRepository(storageDir = folder)
        healthRepo = HealthRepository(storageDir = folder)
        academicRepo = AcademicRepository(storageDir = folder)
        calendarRepo = CalendarEventRepository(storageDir = folder)
        planningTaskRepo = PlanningTaskRepository(storageDir = folder)
        permissionsRepo = AiPermissionsRepository(storageDir = folder)

        orchestrator = PersonalAiAgentOrchestrator(
            personalContextRepo = personalContextRepo,
            dailyStateRepo = dailyStateRepo,
            healthRepo = healthRepo,
            academicRepo = academicRepo,
            calendarRepo = calendarRepo,
            planningTaskRepo = planningTaskRepo,
            permissionsRepo = permissionsRepo
        )
    }

    @Test
    fun masterE2EValidationAcrossAll14RoadmapPhases() = runBlocking {
        // --- PHASE 1: Architecture Foundation & Core Host ---
        assertNotNull(host)
        assertNotNull(host.eventBus)

        // --- PHASE 2: Personal Context Data Model ---
        personalContextRepo.updateProfile(PersonalProfile(fullName = "Alex Mercer", occupation = "Master's Student"))
        personalContextRepo.addGoal(PersonalGoal(title = "Graduate with High Distinction", level = GoalLevel.LONG_TERM))
        personalContextRepo.addAiMemory(AiMemoryItem(category = AiMemoryCategory.FACT, content = "Enrolled in M.Sc. CS"))
        personalContextRepo.addAiMemory(AiMemoryItem(category = AiMemoryCategory.PREFERENCE, content = "Prefers dark theme"))
        assertEquals("Alex Mercer", personalContextRepo.profile.value.fullName)
        assertEquals(1, personalContextRepo.getMemoriesByCategory(AiMemoryCategory.FACT).size)

        // --- PHASE 3: Daily State System ---
        val dailyState = DailyState(dateIso = currentDateIso, sleepHours = 7.5, energyRating = 8, focusRating = 9, learningMinutes = 120)
        dailyStateRepo.updateDailyState(dailyState)
        val fetchedState = dailyStateRepo.getDailyState(currentDateIso)
        assertNotNull(fetchedState)
        assertEquals(7.5, fetchedState?.sleepHours!!, 0.01)

        // --- PHASE 4: Health & Wellbeing (8 Dimensions) ---
        val cogQuestions = DimensionQuestionsCatalog.getQuestionsForDimension(HealthDimension8.COGNITIVE_PERFORMANCE)
        val cogAnswers = cogQuestions.map { DimensionAssessmentAnswer(it.id, it.questionNumber, it.defaultValue) }
        healthRepo.submitDimensionAssessment(HealthDimension8.COGNITIVE_PERFORMANCE, cogAnswers)
        val scoresMap = healthRepo.getDimensionScoresMap()
        assertEquals(8, scoresMap.size)

        // --- PHASE 5: Academic System ---
        val degree = DegreeProgram(name = "M.Sc. Computer Science")
        academicRepo.setDegreeProgram(degree)
        val course = AcademicCourse(degreeId = degree.id, courseCode = "CS501", title = "Advanced ML")
        academicRepo.addCourse(course)
        val exam = AcademicExam(
            courseId = course.id,
            title = "Machine Learning Final Exam",
            examDateIso = "2026-10-20", // 14 days away
            topics = listOf(
                AcademicTopic(name = "Transformers & Self-Attention", estimatedStudyMinutes = 90, isMastered = false)
            )
        )
        academicRepo.addExam(exam)
        val upcomingExams = academicRepo.getUpcomingExams(currentDateIso)
        assertEquals(1, upcomingExams.size)
        assertEquals(14L, academicRepo.getDaysUntilExam(exam.id, currentDateIso))

        // --- PHASE 6: Gemini Context Engine ---
        val academicContext = orchestrator.buildContext(ContextDomain.ACADEMIC, currentDateIso)
        assertNotNull(academicContext)
        assertTrue(academicContext.formattedPromptText.contains("Machine Learning Final Exam"))

        // --- PHASE 7: Controlled AI Tools ---
        calendarRepo.addEvent(CalendarEvent(title = "ML Lecture", startDateIso = currentDateIso, startTimeIso = "11:15", endTimeIso = "12:30"))
        planningTaskRepo.addTask(PlanningTask(title = "Complete Problem Set"))

        val readTaskResult = orchestrator.executeTool(AiToolCallRequest(toolName = "get_tasks"))
        assertTrue(readTaskResult.success)
        assertEquals(ToolAccessType.READ, readTaskResult.accessType)

        val unconfirmedDeleteResult = orchestrator.executeTool(AiToolCallRequest(toolName = "delete_event", parameters = mapOf("eventId" to "evt_1"), userConfirmed = false))
        assertFalse(unconfirmedDeleteResult.success)
        assertTrue(unconfirmedDeleteResult.requiresConfirmation)

        // --- PHASE 8 & 9: Daily AI Briefing ---
        val briefing = orchestrator.generateBriefing(currentDateIso)
        assertNotNull(briefing)
        assertTrue(briefing.whatCannotBeForgotten.any { it.contains("Machine Learning Final Exam") })
        assertTrue(briefing.formattedBriefingText.contains("WHAT MATTERS TODAY"))

        // --- PHASE 10: Next Best Action ---
        val nextAction = orchestrator.getNextBestAction(currentTimeIso, currentDateIso)
        assertNotNull(nextAction)
        assertTrue(nextAction.respectsCalendar)
        assertEquals(75, nextAction.availableMinutesBeforeNextCommitment)
        assertEquals(60, nextAction.suggestedWorkMinutes)
        assertEquals(15, nextAction.suggestedBreakMinutes)

        // --- PHASE 11: Proactive Intelligence Alerts ---
        val alerts = orchestrator.getProactiveAlerts(currentDateIso)
        assertNotNull(alerts)
        assertTrue("Must generate exam & prep alerts", alerts.any { it.category == "APPROACHING_EXAM" || it.category == "INSUFFICIENT_EXAM_PREP" })

        // --- PHASE 12: Personal Pattern Analysis ---
        dailyStateRepo.updateDailyState(DailyState(dateIso = "2026-10-01", sleepHours = 8.0, learningScore = 8))
        dailyStateRepo.updateDailyState(DailyState(dateIso = "2026-10-02", sleepHours = 8.5, learningScore = 9))
        dailyStateRepo.updateDailyState(DailyState(dateIso = "2026-10-03", sleepHours = 7.5, learningScore = 8))
        dailyStateRepo.updateDailyState(DailyState(dateIso = "2026-10-04", sleepHours = 6.0, learningScore = 5))
        dailyStateRepo.updateDailyState(DailyState(dateIso = "2026-10-05", sleepHours = 5.5, learningScore = 4))

        val patterns = orchestrator.getPatternInsights()
        assertTrue("Must compute non-causal pattern insights when samples >= 5", patterns.isNotEmpty())
        assertTrue("Pattern text must be non-causal", orchestrator.patternEngine.isNonCausal(patterns.first().observedCorrelationPattern))

        // --- PHASE 13: Security & Action History ---
        val actionHistory = permissionsRepo.actionHistory.value
        assertTrue("Action history must contain executed tool logs", actionHistory.isNotEmpty())
        assertTrue("Unrestricted Android APIs must be strictly blocked", permissionsRepo.isUnrestrictedApiBlocked("shell_execute"))

        // --- PHASE 14: Master AI Agent Home Overview (Answers the 4 Core Questions) ---
        val homeOverview = orchestrator.getHomeOverview(currentDateIso, currentTimeIso)
        assertNotNull(homeOverview)
        assertEquals(currentDateIso, homeOverview.dateIso)

        // 1. WHAT MATTERS?
        assertTrue("Home Overview must answer WHAT MATTERS", homeOverview.whatMatters.isNotEmpty())

        // 2. WHAT IS HAPPENING?
        assertTrue("Home Overview must answer WHAT IS HAPPENING", homeOverview.whatIsHappening.isNotEmpty())

        // 3. WHAT SHOULD I DO?
        assertTrue("Home Overview must answer WHAT SHOULD I DO", homeOverview.whatShouldIDo.isNotBlank())

        // 4. WHAT NEEDS ATTENTION?
        assertTrue("Home Overview must answer WHAT NEEDS ATTENTION", homeOverview.whatNeedsAttention.isNotEmpty())
    }
}
