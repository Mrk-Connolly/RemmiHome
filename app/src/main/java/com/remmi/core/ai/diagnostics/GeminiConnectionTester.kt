package com.remmi.core.ai.diagnostics

import com.remmi.core.diagnostics.GeminiErrorLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

data class ConnectionDiagnosticStage(
    val stageName: String,
    val isPassed: Boolean,
    val summary: String,
    val details: String = ""
)

data class ConnectionTestReport(
    val apiKeyPresent: Boolean,
    val overallPassed: Boolean,
    val stages: List<ConnectionDiagnosticStage>,
    val failureStageName: String? = null,
    val failureReason: String? = null
)

/**
 * Diagnostic tester executing end-to-end connection verification for Gemini API.
 * Any stage failure is logged directly to [GeminiErrorLogger].
 */
class GeminiConnectionTester(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
) {

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()

    private val _lastReport = MutableStateFlow<ConnectionTestReport?>(null)
    val lastReport: StateFlow<ConnectionTestReport?> = _lastReport.asStateFlow()

    suspend fun runDiagnosticTest(
        apiKey: String?,
        modelName: String = "models/gemini-2.0-flash-exp"
    ): ConnectionTestReport = withContext(Dispatchers.IO) {
        _isTesting.value = true
        val stages = mutableListOf<ConnectionDiagnosticStage>()

        try {
            // STAGE 1: API Key Check
            if (apiKey.isNullOrBlank()) {
                val errStage = ConnectionDiagnosticStage(
                    stageName = "1. API KEY CHECK",
                    isPassed = false,
                    summary = "API Key is missing or blank."
                )
                stages.add(errStage)
                GeminiErrorLogger.logError(
                    stage = "API_KEY_CHECK",
                    code = "KEY_MISSING",
                    reason = "Gemini API key is not configured in Settings or local.properties.",
                    technicalDetails = "apiKeySupplier returned blank or null string."
                )
                val report = ConnectionTestReport(
                    apiKeyPresent = false,
                    overallPassed = false,
                    stages = stages,
                    failureStageName = errStage.stageName,
                    failureReason = errStage.summary
                )
                _lastReport.value = report
                return@withContext report
            } else {
                stages.add(
                    ConnectionDiagnosticStage(
                        stageName = "1. API KEY CHECK",
                        isPassed = true,
                        summary = "API Key present (${apiKey.take(4)}...${apiKey.takeLast(4)})."
                    )
                )
            }

            // STAGE 2: REST HTTP Endpoint Reachability Check
            var httpPassed = false
            var httpStatusMsg = ""
            runCatching {
                val httpUrl = "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"
                val request = Request.Builder().url(httpUrl).get().build()
                client.newCall(request).execute().use { response ->
                    httpPassed = response.isSuccessful
                    httpStatusMsg = "HTTP ${response.code} ${response.message}"
                    if (!response.isSuccessful) {
                        val bodySnippet = response.body?.string()?.take(250) ?: ""
                        GeminiErrorLogger.logError(
                            stage = "HTTP_REST_CHECK",
                            code = "HTTP_${response.code}",
                            reason = "REST HTTP check failed with code ${response.code}",
                            technicalDetails = "Response body: $bodySnippet"
                        )
                    }
                }
            }.onFailure { ex ->
                httpPassed = false
                httpStatusMsg = ex.message ?: ex.javaClass.simpleName
                GeminiErrorLogger.logError(
                    stage = "HTTP_REST_CHECK",
                    code = "HTTP_NETWORK_ERROR",
                    reason = "HTTP endpoint reachability failed: ${ex.message}",
                    technicalDetails = ex.stackTraceToString().take(300)
                )
            }

            stages.add(
                ConnectionDiagnosticStage(
                    stageName = "2. REST HTTP REACHABILITY",
                    isPassed = httpPassed,
                    summary = if (httpPassed) "REST API endpoint reachable ($httpStatusMsg)" else "REST API failed: $httpStatusMsg"
                )
            )

            if (!httpPassed) {
                val report = ConnectionTestReport(
                    apiKeyPresent = true,
                    overallPassed = false,
                    stages = stages,
                    failureStageName = "2. REST HTTP REACHABILITY",
                    failureReason = httpStatusMsg
                )
                _lastReport.value = report
                return@withContext report
            }

            // STAGE 3 & 4: WebSocket Connection & Bidi Setup Handshake
            var wsPassed = false
            var setupPassed = false
            var wsErrorMsg = ""

            val latch = CountDownLatch(1)
            val wsUrl = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey"
            val request = Request.Builder().url(wsUrl).build()

            var activeWs: WebSocket? = null

            activeWs = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    wsPassed = true
                    val setupPayload = """
                        {
                          "setup": {
                            "model": "$modelName",
                            "generationConfig": {
                              "responseModalities": ["TEXT"]
                            }
                          }
                        }
                    """.trimIndent()
                    webSocket.send(setupPayload)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (text.contains("\"setupComplete\"") || text.contains("\"setup\"")) {
                        setupPassed = true
                        latch.countDown()
                    } else if (text.contains("\"error\"")) {
                        setupPassed = false
                        wsErrorMsg = text.take(200)
                        GeminiErrorLogger.logError(
                            stage = "SETUP_HANDSHAKE",
                            code = "SETUP_REJECTED",
                            reason = "Gemini rejected model setup payload for model $modelName",
                            technicalDetails = text
                        )
                        latch.countDown()
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    wsPassed = false
                    setupPassed = false
                    val codeStr = response?.code?.let { "HTTP $it " } ?: ""
                    wsErrorMsg = "$codeStr${response?.message ?: t.message ?: t.javaClass.simpleName}"
                    GeminiErrorLogger.logError(
                        stage = "WEBSOCKET_CONNECT",
                        code = response?.code?.toString() ?: "WS_FAILURE",
                        reason = "WebSocket failure: $wsErrorMsg",
                        technicalDetails = t.stackTraceToString().take(300)
                    )
                    latch.countDown()
                }
            })

            val completed = latch.await(6, TimeUnit.SECONDS)
            activeWs.close(1000, "Diagnostic test complete")

            stages.add(
                ConnectionDiagnosticStage(
                    stageName = "3. WEBSOCKET CONNECT",
                    isPassed = wsPassed,
                    summary = if (wsPassed) "WebSocket connected successfully" else "WebSocket connection failed: $wsErrorMsg"
                )
            )

            stages.add(
                ConnectionDiagnosticStage(
                    stageName = "4. BIDI SETUP HANDSHAKE",
                    isPassed = setupPassed,
                    summary = if (setupPassed) "Model setup accepted ($modelName)" else "Setup rejected or timed out: ${wsErrorMsg.ifBlank { "No ACK" }}"
                )
            )

            val overall = httpPassed && wsPassed && setupPassed
            val report = ConnectionTestReport(
                apiKeyPresent = true,
                overallPassed = overall,
                stages = stages,
                failureStageName = if (!overall) stages.firstOrNull { !it.isPassed }?.stageName else null,
                failureReason = if (!overall) stages.firstOrNull { !it.isPassed }?.summary else null
            )
            _lastReport.value = report
            return@withContext report

        } finally {
            _isTesting.value = false
        }
    }
}
