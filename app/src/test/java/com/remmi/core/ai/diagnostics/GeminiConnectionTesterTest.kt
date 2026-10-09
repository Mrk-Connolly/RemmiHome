package com.remmi.core.ai.diagnostics

import com.remmi.core.ai.diagnostics.GeminiConnectionTester
import com.remmi.core.diagnostics.GeminiErrorLogger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GeminiConnectionTesterTest {

    @Before
    fun setUp() {
        GeminiErrorLogger.clearLogs()
    }

    @Test
    fun `error logger records error entries and excludes successful events`() {
        assertEquals(0, GeminiErrorLogger.errorLogs.value.size)

        GeminiErrorLogger.logError(
            stage = "WEBSOCKET_CONNECT",
            code = "403",
            reason = "Forbidden",
            technicalDetails = "Invalid API key"
        )

        val logs = GeminiErrorLogger.errorLogs.value
        assertEquals(1, logs.size)
        val entry = logs.first()
        assertEquals("WEBSOCKET_CONNECT", entry.stage)
        assertEquals("403", entry.code)
        assertEquals("Forbidden", entry.reason)

        val summary = GeminiErrorLogger.getFormattedSummary()
        assertTrue(summary.contains("WEBSOCKET_CONNECT"))
        assertTrue(summary.contains("403"))
    }

    @Test
    fun `tester fails at stage 1 when api key is missing and logs error`() = runBlocking {
        val tester = GeminiConnectionTester()

        val report = tester.runDiagnosticTest(apiKey = null)

        assertFalse(report.apiKeyPresent)
        assertFalse(report.overallPassed)
        assertEquals("1. API KEY CHECK", report.failureStageName)

        val logs = GeminiErrorLogger.errorLogs.value
        assertTrue(logs.isNotEmpty())
        assertEquals("API_KEY_CHECK", logs.first().stage)
    }

    @Test
    fun `tester reports HTTP failure and logs error entry when api key is invalid`() = runBlocking {
        val tester = GeminiConnectionTester()

        val report = tester.runDiagnosticTest(apiKey = "invalid_key_123")

        assertTrue(report.apiKeyPresent)
        assertFalse(report.overallPassed)
        assertNotNull(report.failureStageName)

        val logs = GeminiErrorLogger.errorLogs.value
        assertTrue(logs.isNotEmpty())
    }
}
