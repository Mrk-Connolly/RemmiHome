package com.remmi.core.ai.action.models

/**
 * Single primary recommendation produced by the "What should I do now?" engine.
 *
 * @property currentTimeIso Current timestamp/time string evaluated.
 * @property dateIso ISO 8601 date key.
 * @property nextCommitmentTitle Title of the next upcoming fixed calendar event, if any.
 * @property availableMinutesBeforeNextCommitment Available free minutes before the next event.
 * @property primaryRecommendation Single clear primary action recommendation.
 * @property suggestedWorkMinutes Recommended active work/study duration in minutes.
 * @property suggestedBreakMinutes Recommended rest/prep buffer duration before the next commitment.
 * @property rationale Explanation of why this action was selected (considering energy, deadlines, and tasks).
 * @property respectsCalendar Guarantees that this recommendation strictly respects fixed calendar commitments.
 */
data class NextBestAction(
    val currentTimeIso: String,
    val dateIso: String,
    val nextCommitmentTitle: String? = null,
    val availableMinutesBeforeNextCommitment: Int = 60,
    val primaryRecommendation: String,
    val suggestedWorkMinutes: Int = 45,
    val suggestedBreakMinutes: Int = 15,
    val rationale: String = "",
    val respectsCalendar: Boolean = true
)
