package com.remmi.core.ai.action

import com.remmi.core.context.dailystate.DailyState
import com.remmi.core.context.dailystate.DailyStateRepository
import com.remmi.plugins.knowledge.academic.models.AcademicCourse
import com.remmi.plugins.knowledge.academic.models.AcademicExam
import com.remmi.plugins.knowledge.academic.models.AcademicTopic
import com.remmi.plugins.knowledge.academic.repository.AcademicRepository
import com.remmi.plugins.planning.calendar.models.CalendarEvent
import com.remmi.plugins.planning.calendar.repository.CalendarEventRepository
import com.remmi.plugins.planning.models.PlanningTask
import com.remmi.plugins.planning.repository.PlanningTaskRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class NextBestActionEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var calendarRepo: CalendarEventRepository
    private lateinit var planningTaskRepo: PlanningTaskRepository
    private lateinit var academicRepo: AcademicRepository
    private lateinit var dailyStateRepo: DailyStateRepository
    private lateinit var engine: NextBestActionEngine

    private val currentDateIso = "2026-10-06"

    @Before
    fun setUp() {
        val folder = tempFolder.newFolder("action_test")
        calendarRepo = CalendarEventRepository(storageDir = folder)
        planningTaskRepo = PlanningTaskRepository(storageDir = folder)
        academicRepo = AcademicRepository(storageDir = folder)
        dailyStateRepo = DailyStateRepository(storageDir = folder)

        runBlocking {
            // Next event starts at 11:15 (75 minutes free when current time is 10:00)
            calendarRepo.addEvent(CalendarEvent(title = "Machine Learning Class", startDateIso = currentDateIso, startTimeIso = "11:15"))

            val course = AcademicCourse(degreeId = "deg_1", courseCode = "CS501", title = "Advanced ML")
            academicRepo.addCourse(course)
            academicRepo.addExam(
                AcademicExam(
                    courseId = course.id,
                    title = "Deep Learning Final Exam",
                    examDateIso = "2026-10-20",
                    topics = listOf(
                        AcademicTopic(name = "Topic 4 Transformer Self-Attention", estimatedStudyMinutes = 60, isMastered = false)
                    )
                )
            )

            planningTaskRepo.addTask(PlanningTask(title = "Read Chapter 4"))
            dailyStateRepo.updateDailyState(DailyState(dateIso = currentDateIso, energyRating = 8))
        }

        engine = NextBestActionEngine(
            calendarRepo = calendarRepo,
            planningTaskRepo = planningTaskRepo,
            academicRepo = academicRepo,
            dailyStateRepo = dailyStateRepo
        )
    }

    @Test
    fun test75MinuteFreeWindowExampleRecommendation() {
        val action = engine.calculateNextBestAction(currentTimeIso = "10:00", currentDateIso = currentDateIso)

        assertNotNull(action)
        assertTrue(action.respectsCalendar)
        assertEquals("Machine Learning Class", action.nextCommitmentTitle)
        assertEquals(75, action.availableMinutesBeforeNextCommitment)
        assertEquals(60, action.suggestedWorkMinutes)
        assertEquals(15, action.suggestedBreakMinutes)

        // Verifies example pattern: "You have 75 minutes before 'Machine Learning Class'. Use the next 60 minutes for 'Topic 4...'..."
        assertTrue(action.primaryRecommendation.contains("75 minutes before 'Machine Learning Class'"))
        assertTrue(action.primaryRecommendation.contains("Topic 4 Transformer Self-Attention"))
        assertTrue(action.primaryRecommendation.contains("60 minutes"))
        assertTrue(action.primaryRecommendation.contains("15 minutes"))
    }

    @Test
    fun testCalendarCommitmentsAreStrictlyRespected() {
        val action = engine.calculateNextBestAction(currentTimeIso = "10:00", currentDateIso = currentDateIso)
        assertTrue("Recommendation must explicitly respect calendar commitments", action.respectsCalendar)
    }

    @Test
    fun testTaskFallbackWhenNoExamsExist() = runBlocking {
        val folder = tempFolder.newFolder("no_exam_test")
        val calRepo = CalendarEventRepository(storageDir = folder)
        val taskRepo = PlanningTaskRepository(storageDir = folder)

        calRepo.addEvent(CalendarEvent(title = "Team Standup", startDateIso = currentDateIso, startTimeIso = "11:00"))
        taskRepo.addTask(PlanningTask(title = "Fix Bug #102"))

        val noExamEngine = NextBestActionEngine(calendarRepo = calRepo, planningTaskRepo = taskRepo)
        val action = noExamEngine.calculateNextBestAction(currentTimeIso = "10:00", currentDateIso = currentDateIso)

        assertTrue(action.primaryRecommendation.contains("Fix Bug #102"))
        assertEquals(60, action.availableMinutesBeforeNextCommitment)
        assertEquals(45, action.suggestedWorkMinutes)
        assertEquals(15, action.suggestedBreakMinutes)
    }
}
