package com.remmi.core.ai.briefing

import com.remmi.core.context.dailystate.DailyState
import com.remmi.core.context.dailystate.DailyStateRepository
import com.remmi.core.context.models.GoalLevel
import com.remmi.core.context.models.PersonalGoal
import com.remmi.core.context.repository.PersonalContextRepository
import com.remmi.plugins.health.repository.HealthRepository
import com.remmi.plugins.knowledge.academic.models.AcademicCourse
import com.remmi.plugins.knowledge.academic.models.AcademicExam
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

class DailyAiBriefingEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var calendarRepo: CalendarEventRepository
    private lateinit var planningTaskRepo: PlanningTaskRepository
    private lateinit var academicRepo: AcademicRepository
    private lateinit var personalContextRepo: PersonalContextRepository
    private lateinit var dailyStateRepo: DailyStateRepository
    private lateinit var healthRepo: HealthRepository
    private lateinit var engine: DailyAiBriefingEngine

    private val currentDateIso = "2026-10-06"

    @Before
    fun setUp() {
        val folder = tempFolder.newFolder("briefing_test")
        calendarRepo = CalendarEventRepository(storageDir = folder)
        planningTaskRepo = PlanningTaskRepository(storageDir = folder)
        academicRepo = AcademicRepository(storageDir = folder)
        personalContextRepo = PersonalContextRepository(storageDir = folder)
        dailyStateRepo = DailyStateRepository(storageDir = folder)
        healthRepo = HealthRepository(storageDir = folder)

        runBlocking {
            calendarRepo.addEvent(CalendarEvent(title = "Morning Standup", startDateIso = currentDateIso, startTimeIso = "09:00"))
            planningTaskRepo.addTask(PlanningTask(title = "Review Machine Learning Ch. 3"))

            val course = AcademicCourse(degreeId = "deg_1", courseCode = "CS501", title = "Advanced ML")
            academicRepo.addCourse(course)
            academicRepo.addExam(AcademicExam(courseId = course.id, title = "Deep Learning Final Exam", examDateIso = "2026-10-20"))

            personalContextRepo.addGoal(PersonalGoal(title = "Graduate with High Honors", level = GoalLevel.MONTHLY))
            dailyStateRepo.updateDailyState(DailyState(dateIso = currentDateIso, sleepHours = 5.5, stressRating = 8))
        }

        engine = DailyAiBriefingEngine(
            calendarRepo = calendarRepo,
            planningTaskRepo = planningTaskRepo,
            academicRepo = academicRepo,
            personalContextRepo = personalContextRepo,
            dailyStateRepo = dailyStateRepo,
            healthRepo = healthRepo
        )
    }

    @Test
    fun testDeterministicBriefingGeneration() {
        val briefing = engine.generateBriefing(currentDateIso)

        assertNotNull(briefing)
        assertEquals(currentDateIso, briefing.dateIso)

        // 1. WHAT MATTERS TODAY
        assertTrue(briefing.whatMattersToday.any { it.contains("calendar event(s)") })
        assertTrue(briefing.whatMattersToday.any { it.contains("Graduate with High Honors") })

        // 2. WHAT CANNOT BE FORGOTTEN
        assertTrue(briefing.whatCannotBeForgotten.any { it.contains("Deep Learning Final Exam") })
        assertTrue(briefing.whatCannotBeForgotten.any { it.contains("Pending Tasks") })

        // 3. WHAT TO DO FIRST
        assertTrue(briefing.whatToDoFirst.contains("Morning Standup"))

        // 4. WARNINGS
        assertTrue("Briefing must warn about low sleep", briefing.warnings.any { it.contains("Sleep Deficit") || it.contains("5.5") })

        // 5. ONE RECOMMENDATION
        assertTrue("Briefing recommendation must address sleep recovery", briefing.oneRecommendation.contains("sleep"))

        // Formatted Text
        assertTrue(briefing.formattedBriefingText.contains("=== DAILY AI BRIEFING (2026-10-06) ==="))
    }

    @Test
    fun testEmptyDatasetFallbackSafety() {
        val emptyEngine = DailyAiBriefingEngine()
        val briefing = emptyEngine.generateBriefing(currentDateIso)

        assertNotNull(briefing)
        assertTrue(briefing.whatMattersToday.isNotEmpty())
        assertTrue(briefing.whatCannotBeForgotten.isNotEmpty())
        assertTrue(briefing.whatToDoFirst.isNotBlank())
        assertTrue(briefing.oneRecommendation.isNotBlank())
    }
}
