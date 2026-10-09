package com.remmi.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remmi.core.ai.permissions.models.AiPermissionsConfig
import com.remmi.core.ai.permissions.repository.AiPermissionsRepository
import com.remmi.ui.theme.remmiColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiPermissionsSection(
    repository: AiPermissionsRepository,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.remmiColors
    val config by repository.permissionsConfig.collectAsState()
    val history by repository.actionHistory.collectAsState()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "AI PERMISSIONS & SECURITY",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. WHAT AI CAN READ
            Text(
                text = "WHAT AI CAN READ",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = colors.textMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                config.readableDomains.forEach { domain ->
                    PermissionChip(label = "👁 $domain")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. WHAT AI CAN CHANGE
            Text(
                text = "WHAT AI CAN CHANGE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = colors.textMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                config.changeableDomains.forEach { domain ->
                    PermissionChip(label = "✏️ $domain")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. WHAT REQUIRES CONFIRMATION
            Text(
                text = "WHAT REQUIRES CONFIRMATION",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = colors.textMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                config.requiresConfirmationActions.forEach { action ->
                    PermissionChip(label = "⚠️ $action", isHighlight = true)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. WHAT AUTOMATIONS ARE ENABLED
            Text(
                text = "WHAT AUTOMATIONS ARE ENABLED",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = colors.textMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                config.enabledAutomations.forEach { auto ->
                    PermissionChip(label = "⚡ $auto")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. ACTION HISTORY
            Text(
                text = "ACTION HISTORY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = colors.textMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (history.isEmpty()) {
                Text(
                    text = "No recent AI actions logged.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    history.takeLast(5).reversed().forEach { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${if (entry.accessType == "WRITE") "✏️" else "👁"} ${entry.actionName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textPrimary
                            )
                            Text(
                                text = if (entry.wasBlocked) "Blocked" else "Allowed",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (entry.wasBlocked) colors.accent else colors.textMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionChip(label: String, isHighlight: Boolean = false) {
    val colors = MaterialTheme.remmiColors
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isHighlight) colors.accent.copy(alpha = 0.2f) else colors.surface.copy(alpha = 0.6f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = if (isHighlight) colors.accent else colors.textPrimary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
