package com.remmi.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.remmi.core.ai.draft.PendingDraftAction
import com.remmi.ui.theme.remmiColors

@Composable
fun PendingActionConfirmDialog(
    draftAction: PendingDraftAction,
    onConfirm: (PendingDraftAction) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.remmiColors

    var editableTitle by remember { mutableStateOf(draftAction.title) }
    var editableDescription by remember { mutableStateOf(draftAction.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = draftAction.type.iconSymbol,
                    style = MaterialTheme.typography.titleLarge
                )
                Column {
                    Text(
                        text = draftAction.type.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Core Save Confirmation",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.accent
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surfaceSubtle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "🔒 Safety Rule: Gemini reads data but cannot write to files directly. Review this template and confirm to let Core execute the save.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                OutlinedTextField(
                    value = editableTitle,
                    onValueChange = { editableTitle = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = editableDescription,
                    onValueChange = { editableDescription = it },
                    label = { Text("Details / Note") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalDraft = draftAction.copy(
                        title = editableTitle,
                        description = editableDescription
                    )
                    onConfirm(finalDraft)
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirm & Save", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Discard", color = colors.textMuted)
            }
        }
    )
}
