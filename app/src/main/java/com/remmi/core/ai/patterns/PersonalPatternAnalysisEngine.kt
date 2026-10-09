package com.remmi.core.ai.patterns

import com.remmi.core.ai.patterns.models.PatternInsight
import com.remmi.core.context.dailystate.DailyStateRepository
import com.remmi.plugins.health.repository.HealthRepository
import com.remmi.plugins.knowledge.academic.repository.AcademicRepository
import kotlin.math.sqrt

/**
 * Historical pattern analysis engine evaluating statistical correlations across personal life metrics.
 * Strictly enforces non-causal language (e.g. "Your learning scores were higher on days when...")
 * and requires a minimum threshold of 5 historical data points.
 */
class PersonalPatternAnalysisEngine(
    private val dailyStateRepo: DailyStateRepository? = null,
    private val healthRepo: HealthRepository? = null,
    private val academicRepo: AcademicRepository? = null
) {

    companion object {
        const val MIN_SAMPLE_THRESHOLD = 5
        private val FORBIDDEN_CAUSAL_WORDS = listOf("causes", "caused", "causing", "makes you", "leads to", "resulted in")
    }

    fun analyzeHistoricalPatterns(daysLimit: Int = 30): List<PatternInsight> {
        val states = dailyStateRepo?.getRecentDailyStates(daysLimit) ?: emptyList()
        if (states.size < MIN_SAMPLE_THRESHOLD) {
            return emptyList()
        }

        val insights = mutableListOf<PatternInsight>()

        // 1. Sleep Duration vs Learning Score
        val sleepLearningPairs = states.mapNotNull {
            val s = it.sleepHours
            val l = it.learningScore?.toDouble() ?: it.focusRating?.toDouble()
            if (s != null && l != null) Pair(s, l) else null
        }
        if (sleepLearningPairs.size >= MIN_SAMPLE_THRESHOLD) {
            val r = calculatePearsonCorrelation(sleepLearningPairs)
            if (r >= 0.3) {
                val text = "Your learning and focus scores were higher on days with increased sleep duration."
                if (isNonCausal(text)) {
                    insights.add(
                        PatternInsight(
                            dimensionA = "Sleep",
                            dimensionB = "Learning Performance",
                            correlationCoefficient = r,
                            sampleSize = sleepLearningPairs.size,
                            observedCorrelationPattern = text
                        )
                    )
                }
            }
        }

        // 2. Exercise vs Energy Rating
        val exerciseEnergyPairs = states.mapNotNull {
            val ex = it.exerciseMinutes?.toDouble()
            val en = it.energyRating?.toDouble()
            if (ex != null && en != null) Pair(ex, en) else null
        }
        if (exerciseEnergyPairs.size >= MIN_SAMPLE_THRESHOLD) {
            val r = calculatePearsonCorrelation(exerciseEnergyPairs)
            if (r >= 0.3) {
                val text = "Daytime energy ratings were higher on days that included physical exercise."
                if (isNonCausal(text)) {
                    insights.add(
                        PatternInsight(
                            dimensionA = "Physical Exercise",
                            dimensionB = "Energy Level",
                            correlationCoefficient = r,
                            sampleSize = exerciseEnergyPairs.size,
                            observedCorrelationPattern = text
                        )
                    )
                }
            }
        }

        // 3. Screen Time vs Deep Work
        val screenDeepWorkPairs = states.mapNotNull {
            val sc = it.screenTimeMinutes?.toDouble()
            val dw = it.deepWorkMinutes?.toDouble()
            if (sc != null && dw != null) Pair(sc, dw) else null
        }
        if (screenDeepWorkPairs.size >= MIN_SAMPLE_THRESHOLD) {
            val r = calculatePearsonCorrelation(screenDeepWorkPairs)
            if (r <= -0.3) {
                val text = "Deep work session minutes tended to be lower on days with higher recorded phone screen time."
                if (isNonCausal(text)) {
                    insights.add(
                        PatternInsight(
                            dimensionA = "Phone Usage",
                            dimensionB = "Deep Work",
                            correlationCoefficient = r,
                            sampleSize = screenDeepWorkPairs.size,
                            observedCorrelationPattern = text
                        )
                    )
                }
            }
        }

        return insights
    }

    private fun calculatePearsonCorrelation(pairs: List<Pair<Double, Double>>): Double {
        val n = pairs.size
        val sumX = pairs.sumOf { it.first }
        val sumY = pairs.sumOf { it.second }
        val sumX2 = pairs.sumOf { it.first * it.first }
        val sumY2 = pairs.sumOf { it.second * it.second }
        val sumXY = pairs.sumOf { it.first * it.second }

        val numerator = n * sumXY - sumX * sumY
        val denominator = sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY))

        return if (denominator != 0.0) (Math.round((numerator / denominator) * 100.0) / 100.0) else 0.0
    }

    fun isNonCausal(text: String): Boolean {
        val lower = text.lowercase()
        return FORBIDDEN_CAUSAL_WORDS.none { lower.contains(it) }
    }
}
