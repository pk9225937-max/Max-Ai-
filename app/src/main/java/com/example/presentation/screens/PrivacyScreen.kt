package com.example.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.LiveSessionManager
import com.example.domain.model.ConfirmationRequest
import com.example.domain.model.RiskLevel
import com.example.presentation.components.ConfirmationDialog
import com.example.presentation.components.GlassPanel
import com.example.ui.theme.AlertRed
import com.example.ui.theme.DarkSpaceBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun PrivacyScreen(
    sessionManager: LiveSessionManager,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showClearConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSpaceBg)
            .padding(16.dp)
    ) {
        // TOP HEADER
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 16.dp, bottom = 20.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Text(
                text = "Privacy & Security",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            item {
                Text(
                    text = "MAX is engineered with privacy as a foundational principle. Your privacy rights and device integrity are non-negotiable.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }

            item {
                PrivacyPolicyItem(
                    title = "Microphone & Audio Streaming",
                    description = "Microphone audio is accessed only when you actively trigger MAX (via wake-word or tapping the Orb). Audio streaming stops immediately when the conversation completes."
                )
            }

            item {
                PrivacyPolicyItem(
                    title = "Contacts & Communication",
                    description = "Contact searches happen strictly on your local device. Contact details are never saved on external servers or used for marketing."
                )
            }

            item {
                PrivacyPolicyItem(
                    title = "Local Memory Isolation",
                    description = "Remembrances and facts you explicitly ask MAX to store reside exclusively in your device's local Room database. Sensitive data like passwords, OTPs, or financial keys are strictly blocked."
                )
            }

            item {
                PrivacyPolicyItem(
                    title = "Background Operation & Notifications",
                    description = "When running in the background, MAX presents an ongoing status notification with a clear microphone indicator. You can stop the background service at any moment."
                )
            }

            item {
                PrivacyPolicyItem(
                    title = "Risk-Gated Confirmation Engine",
                    description = "High-risk actions (initiating calls, composing SMS, WhatsApp, deleting data) require explicit confirmation dialog authorization."
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Clear All Data
        Button(
            onClick = { showClearConfirm = true },
            colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
            Text(" Delete All Stored Local Data", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
        }

        if (showClearConfirm) {
            ConfirmationDialog(
                request = ConfirmationRequest(
                    toolName = "clearAllData",
                    title = "Erase All Local Data?",
                    description = "Are you sure you want to delete all stored memories, reminders, and audit history from this device? This action cannot be undone.",
                    riskLevel = RiskLevel.HIGH
                ),
                onConfirm = {
                    showClearConfirm = false
                    scope.launch {
                        sessionManager.database.reminderDao().clearAllReminders()
                        sessionManager.database.memoryDao().clearAllMemory()
                        sessionManager.database.auditLogDao().clearAllLogs()
                    }
                },
                onDismiss = { showClearConfirm = false }
            )
        }
    }
}

@Composable
private fun PrivacyPolicyItem(title: String, description: String) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        backgroundColor = DarkSurface.copy(alpha = 0.85f),
        borderColor = EmeraldGreen.copy(alpha = 0.3f)
    ) {
        Column {
            Text(title, color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(description, color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
        }
    }
}
