package com.example.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.domain.model.ConfirmationRequest
import com.example.domain.model.RiskLevel
import com.example.ui.theme.AlertRed
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun ConfirmationDialog(
    request: ConfirmationRequest?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (request == null) return

    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(
            shape = RoundedCornerShape(24.dp),
            borderColor = if (request.riskLevel == RiskLevel.HIGH) AlertRed else WarningAmber,
            borderWidth = 1.5.dp,
            backgroundColor = DarkSurface.copy(alpha = 0.95f),
            contentPadding = 24.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Confirmation Warning",
                    tint = if (request.riskLevel == RiskLevel.HIGH) AlertRed else WarningAmber,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = request.title,
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = request.description,
                    color = TextSecondary,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (request.riskLevel == RiskLevel.HIGH) AlertRed else NeonCyan
                        )
                    ) {
                        Text("Confirm", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
