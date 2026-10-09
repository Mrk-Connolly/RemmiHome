package com.remmi.reliability

import com.remmi.core.ai.DefaultAiCapability
import com.remmi.core.eventBus.commands.RemmiCommandResult
import com.remmi.plugins.health.evaluator.HealthAiEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalIntegrationFailureTest {

    @Test
    fun `ai capability handles empty and unresolvable natural language input safely`() {
        val aiCapability = DefaultAiCapability()

        assertNull(aiCapability.resolveIntent(""))
        assertNull(aiCapability.resolveIntent("   "))
        assertNull(aiCapability.resolveIntent("xyz abc random gibberish 123"))

        val result = aiCapability.processInput("xyz abc random gibberish 123") {
            RemmiCommandResult.Success("Executed")
        }

        assertTrue(result is RemmiCommandResult.Failure)
        assertTrue((result as RemmiCommandResult.Failure).reason.contains("Unrecognized AI intent"))
    }

    @Test
    fun `health ai evaluator handles completely empty datasets without throwing exceptions`() {
        val evaluator = HealthAiEvaluator()

        val evaluation = evaluator.evaluate(
            observations = emptyList(),
            sleepLogs = emptyList(),
            activityLogs = emptyList(),
            foodLogs = emptyList(),
            baselines = emptyList()
        )

        assertNotNull(evaluation)
        assertEquals("Low (limited data)", evaluation.confidence)
        assertTrue(evaluation.subjectiveSummary.contains("No subjective observations recorded"))
        assertTrue(evaluation.objectiveFindings.contains("No objective sleep/activity records"))
        assertNotNull(evaluation.interpretation)
        assertFalse(evaluation.recommendations.isEmpty())
    }
}
