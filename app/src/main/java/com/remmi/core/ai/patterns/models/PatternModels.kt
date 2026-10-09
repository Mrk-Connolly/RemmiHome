package com.remmi.core.ai.patterns.models

import java.util.UUID

/**
 * Historical correlation insight strictly adhering to non-causal language guidelines.
 *
 * @property id Unique stable identifier.
 * @property dimensionA Primary observed metric/dimension (e.g., "Sleep Duration").
 * @property dimensionB Secondary observed metric/dimension (e.g., "Learning Score").
 * @property correlationCoefficient Calculated Pearson correlation coefficient from -1.0 to +1.0.
 * @property sampleSize Number of historical daily state data points evaluated.
 * @property observedCorrelationPattern Human-readable non-causal description (e.g. "Your learning scores were higher on days when...").
 * @property timestamp Epoch creation timestamp.
 */
data class PatternInsight(
    val id: String = UUID.randomUUID().toString(),
    val dimensionA: String,
    val dimensionB: String,
    val correlationCoefficient: Double,
    val sampleSize: Int,
    val observedCorrelationPattern: String,
    val timestamp: Long = System.currentTimeMillis()
)
