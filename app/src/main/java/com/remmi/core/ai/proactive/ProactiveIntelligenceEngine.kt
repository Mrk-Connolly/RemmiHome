package com.remmi.core.ai.proactive

import com.remmi.core.ai.proactive.models.ProactiveAlert
import com.remmi.core.ai.proactive.models.ProactiveAlertLevel
import com.remmi.core.ai.proactive.models.ProactiveNotificationControls
import com.remmi.core.context.dailystate.DailyStateRepository
import com.remmi.core.context.repository.PersonalContextRepository
import com.remmi.plugins.health.repository.HealthRepository
import com.remmi.plugins.knowledge.academic.repository.AcademicRepository
import com.remmi.plugins.planning.calendar.repository.CalendarEventRepository
import com.remmi.plugins.planning.repository.PlanningTaskRepository
import com.remmi.plugins.planning.tasks.repository.TaskRepository

/**
 * Proactive Intelligence Engine evaluating real application data to generate grounded alerts and warnings.
 * Strictly requires source data citations and respects user notification controls.
 */
class ProactiveIntelligenceEngine(
    private val calendarRepo: CalendarEventRepository? = null,
    private val taskRepo: TaskRepository? = null,
    private val planningTaskRepo: PlanningTaskRepository? = null,
    private val academicRepo: AcademicRepository? = null,
    private val personalContextRepo: PersonalContextRepository? = null,
    private val dailyStateRepo: DailyStateRepository? = null,
    private val healthRepo: HealthRepository? = null
) {

    fun evaluateProactiveAlerts(
        currentDateIso: String = "2026-10-06",
        controls: ProactiveNotificationControls = ProactiveNotificationControls()
    ): List<ProactiveAlert> {
        val alerts = mutableListOf<ProactiveAlert>()

        // 1. Detect Approaching Exams & Insufficient Exam Preparation
        academicRepo?.let { repo ->
            val upcomingExams = repo.getUpcomingExams(currentDateIso)
            upcomingExams.forEach { exam ->
                val daysRemain = repo.getDaysUntilExam(exam.id, currentDateIso)
                if (daysRemain in 0..14) {
                    val level = if (daysRemain <= 7) ProactiveAlertLevel.URGENT else ProactiveAlertLevel.WARNING
                    alerts.add(
                        ProactiveAlert(
                            title = "Upcoming Exam: ${exam.title}",
                            message = "'${exam.title}' is coming up in $daysRemain days on ${exam.examDateIso}.",
                            level = level,
                            category = "APPROACHING_EXAM",
                            referencedSourceData = "AcademicExam(id=${exam.id}, title=${exam.title}, examDateIso=${exam.examDateIso}, daysRemain=$daysRemain)"
                        )
                    )

                    val prep = repo.getExamPreparationStatus(exam.id, currentDateIso)
                    if (prep != null && prep.preparationProgressPercentage < 50.0) {
                        alerts.add(
                            ProactiveAlert(
                                title = "Insufficient Exam Prep: ${exam.title}",
                                message = "Exam '${exam.title}' is in $daysRemain days, but preparation progress is only ${prep.preparationProgressPercentage.toInt()}%.",
                                level = ProactiveAlertLevel.URGENT,
                                category = "INSUFFICIENT_EXAM_PREP",
                                referencedSourceData = "ExamPreparationStatus(examId=${exam.id}, progress=${prep.preparationProgressPercentage}%, remainingTopics=${prep.remainingTopicsCount})"
                            )
                        )
                    }
                }
            }

            // 2. Detect Approaching Assignments
            val upcomingAssignments = repo.getUpcomingAssignments(currentDateIso)
            upcomingAssignments.forEach { assign ->
                val daysRemain = repo.getDaysUntilExam(assign.id, currentDateIso) // Uses ISO date diff
                if (daysRemain in 0..7) {
                    alerts.add(
                        ProactiveAlert(
                            title = "Assignment Due Soon: ${assign.title}",
                            message = "'${assign.title}' is due in $daysRemain days on ${assign.dueDateIso}.",
                            level = ProactiveAlertLevel.WARNING,
                            category = "APPROACHING_ASSIGNMENT",
                            referencedSourceData = "AcademicAssignment(id=${assign.id}, title=${assign.title}, dueDateIso=${assign.dueDateIso})"
                        )
                    )
                }
            }
        }

        // 3. Detect Calendar Conflicts
        calendarRepo?.getEventsForDate(currentDateIso)?.let { events ->
            val timedEvents = events.filter { !it.isAllDay && it.startTimeIso != null && it.endTimeIso != null }
            for (i in timedEvents.indices) {
                for (j in i + 1 until timedEvents.size) {
                    val ev1 = timedEvents[i]
                    val ev2 = timedEvents[j]
                    if (ev1.startTimeIso!! < ev2.endTimeIso!! && ev2.startTimeIso!! < ev1.endTimeIso!!) {
                        alerts.add(
                            ProactiveAlert(
                                title = "Calendar Conflict Detected",
                                message = "Overlap between '${ev1.title}' (${ev1.startTimeIso}) and '${ev2.title}' (${ev2.startTimeIso}).",
                                level = ProactiveAlertLevel.URGENT,
                                category = "CALENDAR_CONFLICT",
                                referencedSourceData = "CalendarEventsOverlap(event1=${ev1.id}, event2=${ev2.id}, start1=${ev1.startTimeIso}, start2=${ev2.startTimeIso})"
                            )
                        )
                    }
                }
            }
        }

        // 4. Detect Repeated Poor Sleep
        dailyStateRepo?.getRecentDailyStates(limit = 3)?.let { states ->
            val poorSleepDays = states.filter { it.sleepHours != null && it.sleepHours!! < 6.5 }
            if (poorSleepDays.size >= 2) {
                val dates = poorSleepDays.joinToString { "${it.dateIso} (${it.sleepHours} hrs)" }
                alerts.add(
                    ProactiveAlert(
                        title = "Repeated Poor Sleep Detected",
                        message = "You recorded less than 6.5 hours of sleep across multiple recent days ($dates).",
                        level = ProactiveAlertLevel.WARNING,
                        category = "REPEATED_POOR_SLEEP",
                        referencedSourceData = "DailyStateSleepLogs($dates)"
                    )
                )
            }
        }

        // 5. Detect Neglected Tasks
        planningTaskRepo?.tasks?.value?.filter { !it.isCompleted }?.let { tasks ->
            val now = System.currentTimeMillis()
            val sevenDaysMillis = 7 * 24 * 60 * 60 * 1000L
            tasks.filter { now - it.createdAtTimestamp >= sevenDaysMillis }.forEach { task ->
                alerts.add(
                    ProactiveAlert(
                        title = "Neglected Task: ${task.title}",
                        message = "Task '${task.title}' has been pending for over 7 days.",
                        level = ProactiveAlertLevel.INFO,
                        category = "NEGLECTED_TASK",
                        referencedSourceData = "PlanningTask(id=${task.id}, title=${task.title}, createdAt=${task.createdAtTimestamp})"
                    )
                )
            }
        }

        // Apply Notification Controls Filters
        return alerts.filter { alert ->
            when (alert.level) {
                ProactiveAlertLevel.INFO -> controls.enableInfoAlerts
                ProactiveAlertLevel.WARNING -> controls.enableWarningAlerts
                ProactiveAlertLevel.URGENT -> controls.enableUrgentAlerts
            }
        }
    }
}
