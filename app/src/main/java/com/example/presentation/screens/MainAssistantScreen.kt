package com.example.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.LiveSessionManager
import com.example.domain.model.AssistantState
import com.example.presentation.components.ConfirmationDialog
import com.example.presentation.components.GlassPanel
import com.example.presentation.components.GlowingOrb
import com.example.ui.theme.AlertRed
import com.example.ui.theme.DarkSpaceBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.PulseMagenta
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun MainAssistantScreen(
    sessionManager: LiveSessionManager,
    onNavigateToSettings: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToDebug: () -> Unit
) {
    val assistantState by sessionManager.assistantState.collectAsState()
    val statusMessage by sessionManager.statusMessage.collectAsState()
    val pendingConfirmation by sessionManager.pendingConfirmation.collectAsState()
    val inputAmplitude by sessionManager.inputAmplitude.collectAsState()
    val outputAmplitude by sessionManager.outputAmplitude.collectAsState()
    val permissionState by sessionManager.permissionManager.state.collectAsState()

    val quickCommands = listOf(
        "Battery status",
        "Open YouTube",
        "Torch on",
        "Call Mom",
        "Remind me in 10 mins",
        "Open Settings"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DarkSpaceBg, Color(0xFF090E1F), DarkSpaceBg)
                )
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // MAX Logo & State Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(NeonCyan, ElectricViolet))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "M",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }

                    Column {
                        Text(
                            text = "MAX ASSISTANT",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 1.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (assistantState) {
                                            AssistantState.LISTENING -> EmeraldGreen
                                            AssistantState.THINKING -> PulseMagenta
                                            AssistantState.SPEAKING -> NeonCyan
                                            AssistantState.ERROR -> AlertRed
                                            AssistantState.OFFLINE -> WarningAmber
                                            else -> NeonCyan.copy(alpha = 0.5f)
                                        }
                                    )
                            )
                            Text(
                                text = assistantState.name,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Action Icons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Mic Permission shortcut
                    IconButton(onClick = onNavigateToPermissions) {
                        Icon(
                            imageVector = if (permissionState.recordAudio) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = "Permissions",
                            tint = if (permissionState.recordAudio) NeonCyan else AlertRed
                        )
                    }

                    // Privacy
                    IconButton(onClick = onNavigateToPrivacy) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Privacy",
                            tint = TextSecondary
                        )
                    }

                    // Debug
                    IconButton(onClick = onNavigateToDebug) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Debug",
                            tint = TextSecondary
                        )
                    }

                    // Settings
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary
                        )
                    }
                }
            }

            // CENTER: GLOWING ORB & DYNAMIC FEEDBACK
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                GlowingOrb(
                    state = assistantState,
                    inputAmplitude = inputAmplitude,
                    outputAmplitude = outputAmplitude,
                    size = 230.dp,
                    onClick = {
                        sessionManager.onOrbClicked()
                    }
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Futuristic status speech display
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(0.92f),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = 16.dp,
                    backgroundColor = DarkSurface.copy(alpha = 0.85f),
                    borderColor = when (assistantState) {
                        AssistantState.LISTENING -> EmeraldGreen.copy(alpha = 0.6f)
                        AssistantState.THINKING -> PulseMagenta.copy(alpha = 0.6f)
                        AssistantState.SPEAKING -> NeonCyan.copy(alpha = 0.7f)
                        AssistantState.ERROR -> AlertRed.copy(alpha = 0.6f)
                        else -> GlassBorder
                    }
                ) {
                    Text(
                        text = statusMessage,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when (assistantState) {
                        AssistantState.IDLE -> "Tap orb or say \"Hey MAX\""
                        AssistantState.LISTENING -> "Speaking to MAX... (Tap orb to finish)"
                        AssistantState.THINKING -> "MAX is analyzing..."
                        AssistantState.SPEAKING -> "Tap orb to interrupt"
                        AssistantState.ERROR -> "Check permissions or network"
                        else -> "Ready"
                    },
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }

            // BOTTOM: QUICK SUGGESTIONS
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = "TRY ASKING",
                    color = TextSecondary.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickCommands) { command ->
                        AssistChip(
                            onClick = {
                                // Trigger assistant with example intent
                                sessionManager.geminiLiveManager.activateListening()
                            },
                            label = {
                                Text(
                                    text = command,
                                    color = TextPrimary,
                                    fontSize = 13.sp
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = DarkSurface.copy(alpha = 0.8f)
                            ),
                            border = AssistChipDefaults.assistChipBorder(
                                enabled = true,
                                borderColor = GlassBorder
                            )
                        )
                    }
                }
            }
        }

        // CONFIRMATION DIALOG GATING
        ConfirmationDialog(
            request = pendingConfirmation,
            onConfirm = { sessionManager.confirmPendingAction(true) },
            onDismiss = { sessionManager.confirmPendingAction(false) }
        )
    }
}
