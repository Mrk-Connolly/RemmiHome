package com.remmi.core.ai.proactive

import com.remmi.core.ai.proactive.models.ProactiveAlertLevel
import com.remmi.core.ai.proactive.models.ProactiveNotificationControls
import com.remmi.core.context.dailystate.DailyState
import com.remmi.core.context.dailystate.DailyStateRepository
import com.remmi.plugins.knowledge.academic.models.AcademicCourse
import com.remmi.plugins.knowledge.academic.models.AcademicExam
import com.remmi.plugins.knowledge.academic.models.AcademicTopic
import com.remmi.plugins.knowledge.academic.repository.AcademicRepository
import com.remmi.plugins.planning.calendar.models.CalendarEvent
import com.remmi.plugins.planning.calendar.repository.CalendarEventRepository
import com.remmi.plugins.planning.repository.PlanningTaskRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ProactiveIntelligenceEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var calendarRepo: CalendarEventRepository
    private lateinit var planningTaskRepo: PlanningTaskRepository
    private lateinit var academicRepo: AcademicRepository
    private lateinit var dailyStateRepo: DailyStateRepository
    private lateinit var engine: ProactiveIntelligenceEngine

    private val currentDateIso = "2026-10-06"

    @Before
    fun setUp() {
        val folder = tempFolder.newFolder("proactive_test")
        calendarRepo = CalendarEventRepository(storageDir = folder)
        planningTaskRepo = PlanningTaskRepository(storageDir = folder)
        academicRepo = AcademicRepository(storageDir = folder)
        dailyStateRepo = DailyStateRepository(storageDir = folder)

        runBlocking {
            // 1. Calendar Conflict: 10:00-11:30 vs 11:00-12:00
            calendarRepo.addEvent(CalendarEvent(title = "Team Meeting", startDateIso = currentDateIso, startTimeIso = "10:00", endTimeIso = "11:30"))
            calendarRepo.addEvent(CalendarEvent(title = "Project Sync", startDateIso = currentDateIso, startTimeIso = "11:00", endTimeIso = "12:00"))

            // 2. Approaching Exam with Insufficient Prep
            val course = AcademicCourse(degreeId = "deg_1", courseCode = "CS501", title = "Advanced ML")
            academicRepo.addCourse(course)
            academicRepo.addExam(
                AcademicExam(
                    courseId = course.id,
                    title = "Deep Learning Exam",
                    examDateIso = "2026-10-12", // 6 days away
                    topics = listOf(
                        AcademicTopic(name = "Topic 1", isMastered = false),
                        AcademicTopic(name = "Topic 2", isMastered = false)
                    ) // 0% prep
                )
            )

            // 3. Repeated Poor Sleep
            dailyStateRepo.updateDailyState(DailyState(dateIso = "2026-10-04", sleepHours = 5.0))
            dailyStateRepo.updateDailyState(DailyState(dateIso = "2026-10-05", sleepHours = 5.5))
        }

        engine = ProactiveIntelligenceEngine(
            calendarRepo = calendarRepo,
            planningTaskRepo = planningTaskRepo,
            academicRepo = academicRepo,
            dailyStateRepo = dailyStateRepo
        )
    }

    @Test
    fun testRealDataGroundingAndCitations() {
        val alerts = engine.evaluateProactiveAlerts(currentDateIso)

        assertNotNull(alerts)
        assertTrue("Must detect multiple proactive alerts from real data", alerts.size >= 3)

        // Verify every alert explicitly contains real source data citation
        alerts.forEach { alert ->
            assertTrue("Alert '${alert.title}' must contain source citation", alert.referencedSourceData.isNotBlank())
        }

        // Verify specific detections
        assertTrue(alerts.any { it.category == "CALENDAR_CONFLICT" && it.level == ProactiveAlertLevel.URGENT })
        assertTrue(alerts.any { it.category == "APPROACHING_EXAM" })
        assertTrue(alerts.any { it.category == "INSUFFICIENT_EXAM_PREP" && it.level == ProactiveAlertLevel.URGENT })
        assertTrue(alerts.any { it.category == "REPEATED_POOR_SLEEP" })
    }

    @Test
    fun testNotificationControlsFiltering() {
        // Disable URGENT alerts
        val controlsNoUrgent = ProactiveNotificationControls(
            enableInfoAlerts = true,
            enableWarningAlerts = true,
            enableUrgentAlerts = false
        )
        val filtered = engine.evaluateProactiveAlerts(currentDateIso, controlsNoUrgent)

        assertFalse(filtered.any { it.level == ProactiveAlertLevel.URGENT })
        assertTrue(filtered.any { it.level == ProactiveAlertLevel.WARNING })
    }
}
