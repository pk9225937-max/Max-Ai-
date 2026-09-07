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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.LiveSessionManager
import com.example.presentation.components.GlassPanel
import com.example.ui.theme.AlertRed
import com.example.ui.theme.DarkSpaceBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun DebugScreen(
    sessionManager: LiveSessionManager,
    onBack: () -> Unit
) {
    val debugInfo by sessionManager.debugInfo.collectAsState()
    val permissionState by sessionManager.permissionManager.state.collectAsState()
    val auditLogs by sessionManager.database.auditLogDao().getRecentLogs().collectAsState(initial = emptyList())

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
                text = "System Telemetry & Debug",
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
                    text = "REAL-TIME ENGINE STATE",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = 14.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DebugItem("Gemini Connection", debugInfo.connectionState)
                        DebugItem("Audio Engine", debugInfo.audioState)
                        DebugItem("Assistant FSM State", debugInfo.assistantState)
                        DebugItem("Last Tool Invocation", debugInfo.lastToolCall)
                        DebugItem("Last Result", debugInfo.lastToolResult)
                        DebugItem("Error Code", debugInfo.lastErrorCode, isError = debugInfo.lastErrorCode != "None")
                    }
                }
            }

            item {
                Text(
                    text = "PERMISSIONS & HARNESS",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = 14.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DebugItem("RECORD_AUDIO", if (permissionState.recordAudio) "GRANTED" else "DENIED")
                        DebugItem("READ_CONTACTS", if (permissionState.readContacts) "GRANTED" else "DENIED")
                        DebugItem("CALL_PHONE", if (permissionState.callPhone) "GRANTED" else "DENIED")
                        DebugItem("POST_NOTIFICATIONS", if (permissionState.postNotifications) "GRANTED" else "DENIED")
                    }
                }
            }

            item {
                Text(
                    text = "SECURITY AUDIT LOGS (${auditLogs.size})",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                if (auditLogs.isEmpty()) {
                    Text("No tool actions executed yet.", color = TextSecondary, fontSize = 12.sp)
                } else {
                    GlassPanel(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = 12.dp
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            auditLogs.take(15).forEach { log ->
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(log.actionType, color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(
                                            log.status,
                                            color = if (log.status == "SUCCESS") EmeraldGreen else if (log.status == "BLOCKED") AlertRed else WarningAmber,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(log.details, color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    HorizontalDivider(color = DarkSurface, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { sessionManager.geminiLiveManager.startSession() },
            colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = NeonCyan)
            Text(" Force Engine Heartbeat", color = NeonCyan, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun DebugItem(label: String, value: String, isError: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(
            value,
            color = if (isError) AlertRed else TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}
