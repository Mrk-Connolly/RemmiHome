package com.remmi.core.ai.briefing.models

/**
 * Concise, structured daily briefing generated automatically by the AI Life Management System.
 *
 * @property dateIso ISO 8601 date key for the briefing (e.g. "2026-10-06").
 * @property whatMattersToday Core events, main focus, and key commitments for today.
 * @property whatCannotBeForgotten Hard deadlines, exam countdowns, and non-negotiable tasks.
 * @property whatToDoFirst The single highest priority action to execute first today.
 * @property warnings Critical schedule conflicts, low sleep/energy indicators, or upcoming deadlines.
 * @property oneRecommendation The single most impactful behavioral recommendation for the day.
 * @property formattedBriefingText Rendered text representation formatted for user display.
 */
data class DailyBriefing(
    val dateIso: String,
    val whatMattersToday: List<String> = emptyList(),
    val whatCannotBeForgotten: List<String> = emptyList(),
    val whatToDoFirst: String = "",
    val warnings: List<String> = emptyList(),
    val oneRecommendation: String = "",
    val formattedBriefingText: String = ""
)
