package com.remmi.core.ai.proactive.models

import java.util.UUID

/**
 * Severity level classification for proactive alerts.
 */
enum class ProactiveAlertLevel {
    INFO,
    WARNING,
    URGENT
}

/**
 * Proactive warning or notification generated from real application data.
 *
 * @property id Unique stable identifier.
 * @property title Brief alert headline.
 * @property message Detailed warning description.
 * @property level Severity classification ([ProactiveAlertLevel]).
 * @property category Alert category (e.g. "APPROACHING_EXAM", "CALENDAR_CONFLICT", "REPEATED_POOR_SLEEP").
 * @property referencedSourceData Explicit citation of real application data (e.g. "Exam 'CS501 Final' on 2026-10-20").
 * @property timestamp Epoch creation timestamp.
 */
data class ProactiveAlert(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val level: ProactiveAlertLevel,
    val category: String,
    val referencedSourceData: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * User-configurable controls for proactive notifications.
 */
data class ProactiveNotificationControls(
    val enableInfoAlerts: Boolean = true,
    val enableWarningAlerts: Boolean = true,
    val enableUrgentAlerts: Boolean = true,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "07:00"
)
