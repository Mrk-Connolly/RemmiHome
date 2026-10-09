package com.remmi.core.ai.briefing

import com.remmi.core.ai.briefing.models.DailyBriefing
import com.remmi.core.context.dailystate.DailyStateRepository
import com.remmi.core.context.models.GoalLevel
import com.remmi.core.context.repository.PersonalContextRepository
import com.remmi.plugins.health.models.HealthDimension8
import com.remmi.plugins.health.repository.HealthRepository
import com.remmi.plugins.knowledge.academic.repository.AcademicRepository
import com.remmi.plugins.planning.calendar.repository.CalendarEventRepository
import com.remmi.plugins.planning.repository.PlanningTaskRepository
import com.remmi.plugins.planning.tasks.repository.TaskRepository

/**
 * Automated Briefing Engine analyzing calendar, tasks, exams, assignments, habits, health, and goals
 * to produce structured, concise Daily AI Briefings.
 */
class DailyAiBriefingEngine(
    private val calendarRepo: CalendarEventRepository? = null,
    private val taskRepo: TaskRepository? = null,
    private val planningTaskRepo: PlanningTaskRepository? = null,
    private val academicRepo: AcademicRepository? = null,
    private val personalContextRepo: PersonalContextRepository? = null,
    private val dailyStateRepo: DailyStateRepository? = null,
    private val healthRepo: HealthRepository? = null
) {

    fun generateBriefing(currentDateIso: String = "2026-10-06"): DailyBriefing {
        val whatMatters = mutableListOf<String>()
        val cannotBeForgotten = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        var whatToDoFirst = ""
        var oneRecommendation = ""

        // 1. Calendar Events Today
        val todayEvents = calendarRepo?.getEventsForDate(currentDateIso) ?: emptyList()
        if (todayEvents.isNotEmpty()) {
            whatMatters.add("${todayEvents.size} calendar event(s) scheduled for today")
            val firstEvent = todayEvents.first()
            whatToDoFirst = "Prepare for '${firstEvent.title}' (${firstEvent.startTimeIso ?: "All day"})"
        }

        // 2. High Level Goals
        personalContextRepo?.getGoalsByLevel(GoalLevel.MONTHLY)?.forEach { goal ->
            whatMatters.add("Active Goal: ${goal.title}")
        }

        // 3. Upcoming Exams & Assignments
        academicRepo?.let { repo ->
            val upcomingExams = repo.getUpcomingExams(currentDateIso)
            upcomingExams.forEach { exam ->
                val days = repo.getDaysUntilExam(exam.id, currentDateIso)
                cannotBeForgotten.add("EXAM: '${exam.title}' in $days days (${exam.examDateIso})")
                val prep = repo.getExamPreparationStatus(exam.id, currentDateIso)
                if (prep != null && days <= 14 && prep.preparationProgressPercentage < 50.0) {
                    warnings.add("Exam Prep Alert: '${exam.title}' is in $days days with only ${prep.preparationProgressPercentage.toInt()}% progress!")
                }
            }

            val upcomingAssignments = repo.getUpcomingAssignments(currentDateIso)
            upcomingAssignments.forEach { assign ->
                cannotBeForgotten.add("ASSIGNMENT: '${assign.title}' due on ${assign.dueDateIso}")
            }
        }

        // 4. Pending Tasks
        val pendingTasks = planningTaskRepo?.tasks?.value?.filter { !it.isCompleted } ?: emptyList()
        if (pendingTasks.isNotEmpty()) {
            cannotBeForgotten.add("Pending Tasks: ${pendingTasks.size} item(s) awaiting completion")
            if (whatToDoFirst.isBlank()) {
                whatToDoFirst = "Execute task '${pendingTasks.first().title}'"
            }
        }

        // 5. Health & Daily State Analysis
        dailyStateRepo?.getDailyState(currentDateIso)?.let { state ->
            state.sleepHours?.let { hours ->
                if (hours < 6.5) {
                    warnings.add("Sleep Deficit: Recorded only $hours hours of sleep last night.")
                }
            }
            state.stressRating?.let { stress ->
                if (stress >= 7) {
                    warnings.add("High Stress: Recorded elevated stress rating ($stress/10).")
                }
            }
        }

        healthRepo?.let { repo ->
            val cogState = repo.getDimensionState(HealthDimension8.COGNITIVE_PERFORMANCE)
            if (cogState.calculatedScore < 6.0) {
                warnings.add("Cognitive Fatigue: Cognitive performance rating is low (${cogState.calculatedScore}/10.0).")
            }
        }

        // Default Fallbacks
        if (whatMatters.isEmpty()) {
            whatMatters.add("Clear schedule today. Great opportunity for deep work or study.")
        }
        if (cannotBeForgotten.isEmpty()) {
            cannotBeForgotten.add("No immediate urgent deadlines due today.")
        }
        if (whatToDoFirst.isBlank()) {
            whatToDoFirst = "Review personal goals and conduct 45 minutes of focus study."
        }
        if (warnings.isEmpty()) {
            warnings.add("None. All parameters within optimal operating thresholds.")
        }

        oneRecommendation = when {
            warnings.any { it.contains("Sleep") } -> "Prioritize an early sleep routine tonight to restore recovery."
            cannotBeForgotten.any { it.contains("EXAM") } -> "Dedicate 60 minutes today to active recall testing on upcoming exam topics."
            else -> "Maintain steady focus and take a 10-minute break after every 45-minute work session."
        }

        val formattedText = buildString {
            appendLine("=== DAILY AI BRIEFING ($currentDateIso) ===")
            appendLine("\n📌 WHAT MATTERS TODAY:")
            whatMatters.forEach { appendLine("• $it") }

            appendLine("\n⏰ WHAT CANNOT BE FORGOTTEN:")
            cannotBeForgotten.forEach { appendLine("• $it") }

            appendLine("\n🚀 WHAT TO DO FIRST:")
            appendLine("➔ $whatToDoFirst")

            appendLine("\n⚠️ WARNINGS:")
            warnings.forEach { appendLine("• $it") }

            appendLine("\n💡 ONE RECOMMENDATION:")
            appendLine("➔ $oneRecommendation")
        }

        return DailyBriefing(
            dateIso = currentDateIso,
            whatMattersToday = whatMatters,
            whatCannotBeForgotten = cannotBeForgotten,
            whatToDoFirst = whatToDoFirst,
            warnings = warnings,
            oneRecommendation = oneRecommendation,
            formattedBriefingText = formattedText
        )
    }
}
