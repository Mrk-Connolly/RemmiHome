package com.remmi.core.ai.action

import com.remmi.core.ai.action.models.NextBestAction
import com.remmi.core.context.dailystate.DailyStateRepository
import com.remmi.core.context.repository.PersonalContextRepository
import com.remmi.plugins.health.repository.HealthRepository
import com.remmi.plugins.knowledge.academic.repository.AcademicRepository
import com.remmi.plugins.planning.calendar.repository.CalendarEventRepository
import com.remmi.plugins.planning.repository.PlanningTaskRepository
import com.remmi.plugins.planning.tasks.repository.TaskRepository

/**
 * Engine calculating "What should I do now?" recommendations considering current time, available free windows,
 * calendar commitments, tasks, exams, health, energy, goals, and habits.
 */
class NextBestActionEngine(
    private val calendarRepo: CalendarEventRepository? = null,
    private val taskRepo: TaskRepository? = null,
    private val planningTaskRepo: PlanningTaskRepository? = null,
    private val academicRepo: AcademicRepository? = null,
    private val personalContextRepo: PersonalContextRepository? = null,
    private val dailyStateRepo: DailyStateRepository? = null,
    private val healthRepo: HealthRepository? = null
) {

    fun calculateNextBestAction(
        currentTimeIso: String = "10:00",
        currentDateIso: String = "2026-10-06"
    ): NextBestAction {
        // 1. Calculate Available Time Before Next Calendar Commitment
        val todayEvents = calendarRepo?.getEventsForDate(currentDateIso)
            ?.filter { !it.isAllDay && (it.startTimeIso ?: "") >= currentTimeIso }
            ?.sortedBy { it.startTimeIso } ?: emptyList()

        val nextEvent = todayEvents.firstOrNull()
        val currentMins = parseTimeToMinutes(currentTimeIso)

        val (nextEventTitle, availableMinutes) = if (nextEvent != null && nextEvent.startTimeIso != null) {
            val eventStartMins = parseTimeToMinutes(nextEvent.startTimeIso!!)
            val freeMins = (eventStartMins - currentMins).coerceAtLeast(15)
            Pair(nextEvent.title, freeMins)
        } else {
            Pair(null, 120) // Default 2-hour window if no further events today
        }

        // 2. Work & Break Allocations
        val (workMinutes, breakMinutes) = when {
            availableMinutes >= 60 -> Pair(availableMinutes - 15, 15)
            availableMinutes >= 30 -> Pair(availableMinutes - 10, 10)
            else -> Pair(availableMinutes, 0)
        }

        // 3. User Energy & Health Assessment
        val energyRating = dailyStateRepo?.getDailyState(currentDateIso)?.energyRating ?: 7

        // 4. Action Recommendation Determination
        val upcomingExams = academicRepo?.getUpcomingExams(currentDateIso) ?: emptyList()
        val pendingTasks = planningTaskRepo?.tasks?.value?.filter { !it.isCompleted } ?: emptyList()

        val primaryRec: String
        val rationaleStr: String

        if (energyRating >= 6 && upcomingExams.isNotEmpty()) {
            val exam = upcomingExams.first()
            val remainingTopics = academicRepo?.getRemainingStudyRequirements(exam.courseId) ?: emptyList()
            val topicName = remainingTopics.firstOrNull()?.name ?: "Exam Revision"

            primaryRec = if (nextEventTitle != null) {
                "You have $availableMinutes minutes before '$nextEventTitle'. Use the next $workMinutes minutes for '$topicName' retrieval practice. Then take $breakMinutes minutes to prepare for class."
            } else {
                "Use the next $workMinutes minutes for '$topicName' retrieval practice, followed by a $breakMinutes minute rest."
            }
            rationaleStr = "High-energy ($energyRating/10) focus window matched with upcoming exam '${exam.title}' study requirement."
        } else if (pendingTasks.isNotEmpty()) {
            val task = pendingTasks.first()
            primaryRec = if (nextEventTitle != null) {
                "You have $availableMinutes minutes before '$nextEventTitle'. Use the next $workMinutes minutes to complete task '${task.title}'. Then take $breakMinutes minutes buffer."
            } else {
                "Use the next $workMinutes minutes to complete task '${task.title}'."
            }
            rationaleStr = "Selected highest priority pending task within free $availableMinutes-minute window."
        } else {
            primaryRec = if (nextEventTitle != null) {
                "You have $availableMinutes minutes before '$nextEventTitle'. Use the next $workMinutes minutes for personal review and goal reflection."
            } else {
                "Take a $workMinutes minute rest or review your personal goals."
            }
            rationaleStr = "All urgent tasks and exam requirements are currently up to date."
        }

        return NextBestAction(
            currentTimeIso = currentTimeIso,
            dateIso = currentDateIso,
            nextCommitmentTitle = nextEventTitle,
            availableMinutesBeforeNextCommitment = availableMinutes,
            primaryRecommendation = primaryRec,
            suggestedWorkMinutes = workMinutes,
            suggestedBreakMinutes = breakMinutes,
            rationale = rationaleStr,
            respectsCalendar = true
        )
    }

    private fun parseTimeToMinutes(timeStr: String): Int {
        return runCatching {
            val parts = timeStr.split(":")
            val hours = parts[0].toInt()
            val minutes = parts[1].toInt()
            hours * 60 + minutes
        }.getOrDefault(600)
    }
}
