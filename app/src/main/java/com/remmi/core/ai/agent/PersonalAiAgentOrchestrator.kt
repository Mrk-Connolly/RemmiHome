package com.remmi.core.ai.agent

import com.remmi.core.ai.action.NextBestActionEngine
import com.remmi.core.ai.action.models.NextBestAction
import com.remmi.core.ai.briefing.DailyAiBriefingEngine
import com.remmi.core.ai.briefing.models.DailyBriefing
import com.remmi.core.ai.context.ContextDomain
import com.remmi.core.ai.context.GeminiContextBuilder
import com.remmi.core.ai.context.StructuredAiContext
import com.remmi.core.ai.patterns.PersonalPatternAnalysisEngine
import com.remmi.core.ai.patterns.models.PatternInsight
import com.remmi.core.ai.permissions.models.AiActionHistoryEntry
import com.remmi.core.ai.permissions.repository.AiPermissionsRepository
import com.remmi.core.ai.proactive.ProactiveIntelligenceEngine
import com.remmi.core.ai.proactive.models.ProactiveAlert
import com.remmi.core.ai.tools.RemmiAiToolRegistry
import com.remmi.core.ai.tools.models.AiToolCallRequest
import com.remmi.core.ai.tools.models.AiToolExecutionResult
import com.remmi.core.android.launcher.RemmiLauncherCapability
import com.remmi.core.context.dailystate.DailyStateRepository
import com.remmi.core.context.repository.PersonalContextRepository
import com.remmi.plugins.health.repository.HealthRepository
import com.remmi.plugins.knowledge.academic.repository.AcademicRepository
import com.remmi.plugins.planning.calendar.repository.CalendarEventRepository
import com.remmi.plugins.planning.repository.PlanningTaskRepository
import com.remmi.plugins.planning.tasks.repository.TaskRepository

/**
 * Overview payload answering the 4 Core Questions on the Home Surface.
 */
data class PersonalAiHomeOverview(
    val dateIso: String,
    val whatMatters: List<String>,
    val whatIsHappening: List<String>,
    val whatShouldIDo: String,
    val whatNeedsAttention: List<String>
)

/**
 * Master Personal AI Agent Orchestrator unifying all 14 roadmap phases into a single cohesive intelligence agent.
 */
class PersonalAiAgentOrchestrator(
    val personalContextRepo: PersonalContextRepository? = null,
    val dailyStateRepo: DailyStateRepository? = null,
    val healthRepo: HealthRepository? = null,
    val academicRepo: AcademicRepository? = null,
    val calendarRepo: CalendarEventRepository? = null,
    val taskRepo: TaskRepository? = null,
    val planningTaskRepo: PlanningTaskRepository? = null,
    val permissionsRepo: AiPermissionsRepository? = null,
    val launcherCapability: RemmiLauncherCapability? = null
) {

    val contextBuilder = GeminiContextBuilder(
        personalContextRepo = personalContextRepo,
        dailyStateRepo = dailyStateRepo,
        healthRepo = healthRepo,
        academicRepo = academicRepo,
        calendarRepo = calendarRepo,
        taskRepo = taskRepo,
        launcherCapability = launcherCapability
    )

    val toolRegistry = RemmiAiToolRegistry(
        calendarRepo = calendarRepo,
        taskRepo = taskRepo,
        planningTaskRepo = planningTaskRepo,
        academicRepo = academicRepo,
        healthRepo = healthRepo,
        personalContextRepo = personalContextRepo,
        dailyStateRepo = dailyStateRepo,
        launcherCapability = launcherCapability
    )

    val briefingEngine = DailyAiBriefingEngine(
        calendarRepo = calendarRepo,
        taskRepo = taskRepo,
        planningTaskRepo = planningTaskRepo,
        academicRepo = academicRepo,
        personalContextRepo = personalContextRepo,
        dailyStateRepo = dailyStateRepo,
        healthRepo = healthRepo
    )

    val actionEngine = NextBestActionEngine(
        calendarRepo = calendarRepo,
        taskRepo = taskRepo,
        planningTaskRepo = planningTaskRepo,
        academicRepo = academicRepo,
        personalContextRepo = personalContextRepo,
        dailyStateRepo = dailyStateRepo,
        healthRepo = healthRepo
    )

    val proactiveEngine = ProactiveIntelligenceEngine(
        calendarRepo = calendarRepo,
        taskRepo = taskRepo,
        planningTaskRepo = planningTaskRepo,
        academicRepo = academicRepo,
        personalContextRepo = personalContextRepo,
        dailyStateRepo = dailyStateRepo,
        healthRepo = healthRepo
    )

    val patternEngine = PersonalPatternAnalysisEngine(
        dailyStateRepo = dailyStateRepo,
        healthRepo = healthRepo,
        academicRepo = academicRepo
    )

    /**
     * Directly answers the 4 Core Questions required for the Home Surface:
     * 1. WHAT MATTERS?
     * 2. WHAT IS HAPPENING?
     * 3. WHAT SHOULD I DO?
     * 4. WHAT NEEDS ATTENTION?
     */
    fun getHomeOverview(
        currentDateIso: String = "2026-10-06",
        currentTimeIso: String = "10:00"
    ): PersonalAiHomeOverview {
        val briefing = briefingEngine.generateBriefing(currentDateIso)
        val action = actionEngine.calculateNextBestAction(currentTimeIso, currentDateIso)
        val alerts = proactiveEngine.evaluateProactiveAlerts(currentDateIso)

        // 1. WHAT MATTERS?
        val mattersList = briefing.whatMattersToday.ifEmpty { listOf("Focus on daily goals and learning priorities.") }

        // 2. WHAT IS HAPPENING?
        val happeningList = mutableListOf<String>()
        val eventsToday = calendarRepo?.getEventsForDate(currentDateIso) ?: emptyList()
        if (eventsToday.isNotEmpty()) {
            happeningList.add("${eventsToday.size} event(s) scheduled on calendar today")
        } else {
            happeningList.add("No fixed calendar commitments currently in progress")
        }
        personalContextRepo?.lifeContext?.value?.busyLevel?.let {
            happeningList.add("Current Life Context Busy Level: $it")
        }

        // 3. WHAT SHOULD I DO?
        val shouldDoStr = action.primaryRecommendation

        // 4. WHAT NEEDS ATTENTION?
        val needsAttentionList = alerts.map { "⚠️ [${it.level.name}] ${it.title}: ${it.message}" }
            .ifEmpty { listOf("All parameters operating within normal parameters. No urgent alerts.") }

        return PersonalAiHomeOverview(
            dateIso = currentDateIso,
            whatMatters = mattersList,
            whatIsHappening = happeningList,
            whatShouldIDo = shouldDoStr,
            whatNeedsAttention = needsAttentionList
        )
    }

    suspend fun executeTool(request: AiToolCallRequest): AiToolExecutionResult {
        val result = toolRegistry.executeTool(request)
        permissionsRepo?.recordActionHistory(
            AiActionHistoryEntry(
                actionName = request.toolName,
                domain = request.toolName.substringBefore("_").uppercase(),
                accessType = result.accessType.name,
                details = request.parameters.toString(),
                userConfirmed = request.userConfirmed,
                wasBlocked = !result.success && result.requiresConfirmation
            )
        )
        return result
    }

    fun buildContext(domain: ContextDomain, currentDateIso: String = "2026-10-06"): StructuredAiContext {
        return contextBuilder.buildContext(domain, currentDateIso)
    }

    fun generateBriefing(currentDateIso: String = "2026-10-06"): DailyBriefing {
        return briefingEngine.generateBriefing(currentDateIso)
    }

    fun getNextBestAction(currentTimeIso: String = "10:00", currentDateIso: String = "2026-10-06"): NextBestAction {
        return actionEngine.calculateNextBestAction(currentTimeIso, currentDateIso)
    }

    fun getProactiveAlerts(currentDateIso: String = "2026-10-06"): List<ProactiveAlert> {
        return proactiveEngine.evaluateProactiveAlerts(currentDateIso)
    }

    fun getPatternInsights(daysLimit: Int = 30): List<PatternInsight> {
        return patternEngine.analyzeHistoricalPatterns(daysLimit)
    }
}
