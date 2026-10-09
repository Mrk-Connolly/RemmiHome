package com.remmi.ui.ai
import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remmi.core.diagnostics.GeminiErrorEntry
import com.remmi.core.diagnostics.GeminiErrorLogger
import com.remmi.ui.theme.remmiColors

@Composable
fun GeminiErrorLogDialog(
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.remmiColors
    val logs by GeminiErrorLogger.errorLogs.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.80f),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colors.accent.copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🚨", fontSize = 18.sp)
                        }
                    }
                    Column {
                        Text(
                            text = "Real-Time Gemini Error Log",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent
                        )
                        Text(
                            text = "${logs.size} connection error(s) captured",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                }
                if (logs.isNotEmpty()) {
                    TextButton(onClick = { GeminiErrorLogger.clearLogs() }) {
                        Text("Clear", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surfaceSubtle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "ℹ️ Error-Only Policy: Shows failed stages, HTTP error status, and WebSocket rejections. Actions that finish fine are not included.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                if (logs.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("✅", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Zero Error Events Recorded",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "No Gemini connection or API errors have been encountered.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(logs, key = { it.id }) { entry ->
                            ErrorLogCard(entry = entry)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = colors.accent)
            }
        }
    )
}

@Composable
private fun ErrorLogCard(entry: GeminiErrorEntry) {
    val colors = MaterialTheme.remmiColors

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = colors.accent.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = entry.stage,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = entry.code,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = entry.reason,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )

            if (entry.technicalDetails.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surfaceSubtle,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = entry.technicalDetails,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}
