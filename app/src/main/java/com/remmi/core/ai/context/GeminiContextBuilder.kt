package com.remmi.core.ai.context

import com.remmi.core.android.launcher.RemmiLauncherCapability
import com.remmi.core.context.dailystate.DailyStateRepository
import com.remmi.core.context.models.AiMemoryCategory
import com.remmi.core.context.models.GoalLevel
import com.remmi.core.context.repository.PersonalContextRepository
import com.remmi.plugins.health.models.HealthDimension8
import com.remmi.plugins.health.repository.HealthRepository
import com.remmi.plugins.knowledge.academic.repository.AcademicRepository
import com.remmi.plugins.planning.calendar.repository.CalendarEventRepository
import com.remmi.plugins.planning.tasks.repository.TaskRepository

/**
 * Domain categories supported by the Gemini Context Engine.
 */
enum class ContextDomain {
    GENERAL,
    DAILY_PLANNING,
    ACADEMIC,
    HEALTH,
    LEARNING,
    CALENDAR,
    TASKS,
    APP_MANAGEMENT
}

/**
 * Structured context payload provided to Gemini, enforcing strict category separation.
 */
data class StructuredAiContext(
    val domain: ContextDomain,
    val facts: List<String> = emptyList(),
    val goals: List<String> = emptyList(),
    val preferences: List<String> = emptyList(),
    val currentState: List<String> = emptyList(),
    val observations: List<String> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val formattedPromptText: String = ""
)

/**
 * Dynamic Context Builder constructing relevant domain-sliced contexts for Gemini without dumping full storage.
 */
class GeminiContextBuilder(
    private val personalContextRepo: PersonalContextRepository? = null,
    private val dailyStateRepo: DailyStateRepository? = null,
    private val healthRepo: HealthRepository? = null,
    private val academicRepo: AcademicRepository? = null,
    private val calendarRepo: CalendarEventRepository? = null,
    private val taskRepo: TaskRepository? = null,
    private val launcherCapability: RemmiLauncherCapability? = null
) {

    fun buildContext(domain: ContextDomain, currentDateIso: String = "2026-10-06"): StructuredAiContext {
        val facts = mutableListOf<String>()
        val goals = mutableListOf<String>()
        val preferences = mutableListOf<String>()
        val currentState = mutableListOf<String>()
        val observations = mutableListOf<String>()
        val recommendations = mutableListOf<String>()

        // 1. Personal Profile & AI Memory Base Context
        personalContextRepo?.let { repo ->
            val profile = repo.profile.value
            if (profile.fullName.isNotBlank()) facts.add("User Name: ${profile.fullName}")
            if (profile.occupation.isNotBlank()) facts.add("Occupation: ${profile.occupation}")
            if (profile.timeZone.isNotBlank()) facts.add("Timezone: ${profile.timeZone}")

            repo.getMemoriesByCategory(AiMemoryCategory.FACT).forEach { facts.add(it.content) }
            repo.getMemoriesByCategory(AiMemoryCategory.PREFERENCE).forEach { preferences.add(it.content) }
            repo.getMemoriesByCategory(AiMemoryCategory.GOAL).forEach { goals.add(it.content) }
            repo.getMemoriesByCategory(AiMemoryCategory.CURRENT_STATE).forEach { currentState.add(it.content) }
            repo.getMemoriesByCategory(AiMemoryCategory.OBSERVATION).forEach { observations.add(it.content) }
            repo.getMemoriesByCategory(AiMemoryCategory.RECOMMENDATION).forEach { recommendations.add(it.content) }

            val userPref = repo.preferences.value
            preferences.add("Preferred Wake Time: ${userPref.preferredWakeTime}, Sleep Time: ${userPref.preferredSleepTime}")
            preferences.add("Communication Style: ${userPref.communicationStyle}")
        }

        // 2. Domain-Specific Selective Slicing
        when (domain) {
            ContextDomain.GENERAL -> {
                // Include high-level goals and current life context
                personalContextRepo?.let { repo ->
                    repo.getGoalsByLevel(GoalLevel.LONG_TERM).forEach { goals.add("Long-term Goal: ${it.title}") }
                    val lc = repo.lifeContext.value
                    if (lc.busyLevel.isNotBlank()) currentState.add("Busy Level: ${lc.busyLevel}")
                }
            }

            ContextDomain.DAILY_PLANNING -> {
                dailyStateRepo?.getDailyState(currentDateIso)?.let { state ->
                    state.energyRating?.let { currentState.add("Today's Energy Rating: $it/10") }
                    state.focusRating?.let { currentState.add("Today's Focus Rating: $it/10") }
                }
                calendarRepo?.getEventsForDate(currentDateIso)?.forEach { event ->
                    facts.add("Calendar Event Today: ${event.title} (${event.startTimeIso} - ${event.endTimeIso})")
                }
                taskRepo?.tasks?.value?.filter { !it.isCompleted }?.forEach { task ->
                    goals.add("Pending Task: ${task.title}")
                }
            }

            ContextDomain.ACADEMIC, ContextDomain.LEARNING -> {
                academicRepo?.let { repo ->
                    repo.degreeProgram.value?.let { deg ->
                        facts.add("Degree Program: ${deg.name} (${deg.institution})")
                    }
                    repo.getUpcomingExams(currentDateIso).forEach { exam ->
                        val days = repo.getDaysUntilExam(exam.id, currentDateIso)
                        val course = repo.getCourseForExam(exam.id)
                        facts.add("Upcoming Exam: ${exam.title} (${course?.courseCode ?: "Course"}) in $days days on ${exam.examDateIso}")
                        val prep = repo.getExamPreparationStatus(exam.id, currentDateIso)
                        prep?.let {
                            currentState.add("Exam Prep Progress for ${exam.title}: ${it.preparationProgressPercentage.toInt()}% (${it.remainingTopicsCount} topics remaining, est. ${it.estimatedStudyHoursRemaining} hrs)")
                        }
                    }
                    repo.getUpcomingAssignments(currentDateIso).forEach { assign ->
                        facts.add("Upcoming Assignment: ${assign.title} due on ${assign.dueDateIso}")
                    }
                }
            }

            ContextDomain.HEALTH -> {
                dailyStateRepo?.getDailyState(currentDateIso)?.let { state ->
                    state.sleepHours?.let { currentState.add("Sleep: $it hours") }
                    state.energyRating?.let { currentState.add("Energy: $it/10") }
                    state.stressRating?.let { currentState.add("Stress: $it/10") }
                }
                healthRepo?.let { repo ->
                    val scores = repo.getDimensionScoresMap(periodDays = 1)
                    scores.forEach { (dim, score) ->
                        currentState.add("Health Dimension ${dim.label}: $score/10.0")
                    }
                    val cogState = repo.getDimensionState(HealthDimension8.COGNITIVE_PERFORMANCE)
                    cogState.recommendations.forEach { recommendations.add(it) }
                }
            }

            ContextDomain.CALENDAR -> {
                calendarRepo?.getEventsForDate(currentDateIso)?.forEach { event ->
                    facts.add("Scheduled Event: ${event.title} from ${event.startTimeIso} to ${event.endTimeIso}")
                }
            }

            ContextDomain.TASKS -> {
                taskRepo?.tasks?.value?.filter { !it.isCompleted }?.forEach { task ->
                    goals.add("Action Item: ${task.title} (Due: ${task.dueDateIso ?: "No Date"})")
                }
            }

            ContextDomain.APP_MANAGEMENT -> {
                launcherCapability?.installedApps?.value?.take(15)?.forEach { app ->
                    facts.add("Installed App: ${app.label} (${app.packageName})")
                }
            }
        }

        val promptText = buildString {
            appendLine("=== CONTEXT DOMAIN: ${domain.name} ===")
            if (facts.isNotEmpty()) {
                appendLine("[FACTS]")
                facts.forEach { appendLine("- $it") }
            }
            if (goals.isNotEmpty()) {
                appendLine("[GOALS]")
                goals.forEach { appendLine("- $it") }
            }
            if (preferences.isNotEmpty()) {
                appendLine("[PREFERENCES]")
                preferences.forEach { appendLine("- $it") }
            }
            if (currentState.isNotEmpty()) {
                appendLine("[CURRENT_STATE]")
                currentState.forEach { appendLine("- $it") }
            }
            if (observations.isNotEmpty()) {
                appendLine("[OBSERVATIONS]")
                observations.forEach { appendLine("- $it") }
            }
            if (recommendations.isNotEmpty()) {
                appendLine("[RECOMMENDATIONS]")
                recommendations.forEach { appendLine("- $it") }
            }
        }

        return StructuredAiContext(
            domain = domain,
            facts = facts,
            goals = goals,
            preferences = preferences,
            currentState = currentState,
            observations = observations,
            recommendations = recommendations,
            formattedPromptText = promptText
        )
    }
}
