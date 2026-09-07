package com.example.presentation.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.LiveSessionManager
import com.example.presentation.components.GlassPanel
import com.example.service.BackgroundAudioService
import com.example.ui.theme.AlertRed
import com.example.ui.theme.DarkSpaceBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    sessionManager: LiveSessionManager,
    onBack: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToPrivacy: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var wakeWordEnabled by remember { mutableStateOf(true) }
    var bgServiceEnabled by remember { mutableStateOf(false) }
    var voiceSpeed by remember { mutableFloatStateOf(1.0f) }

    val memories by sessionManager.memoryManager.getAllMemory().collectAsState(initial = emptyList())
    var selectedLanguage by remember { mutableStateOf("Hinglish (Hindi + English)") }

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
                text = "Assistant Settings",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            // 1. ASSISTANT PERSONALITY & VOICE
            item {
                SectionHeader("ASSISTANT PERSONALITY & VOICE")
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = 16.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Personality Tone", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text("Smart, confident, slightly sassy", color = TextSecondary, fontSize = 12.sp)
                            }
                            Text("Witty", color = NeonCyan, fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider(color = DarkSurface)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Language Preference", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text(selectedLanguage, color = TextSecondary, fontSize = 12.sp)
                            }
                        }

                        HorizontalDivider(color = DarkSurface)

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Voice Speed", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text("${(voiceSpeed * 10).toInt() / 10.0}x", color = NeonCyan, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = voiceSpeed,
                                onValueChange = { voiceSpeed = it },
                                valueRange = 0.75f..1.5f,
                                colors = SliderDefaults.colors(
                                    thumbColor = NeonCyan,
                                    activeTrackColor = NeonCyan
                                )
                            )
                        }
                    }
                }
            }

            // 2. WAKE WORD & BACKGROUND SERVICE
            item {
                SectionHeader("WAKE WORD & BACKGROUND OPERATION")
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = 16.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Wake Word Detection", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text("Listens locally for 'MAX' or 'Hey MAX'", color = TextSecondary, fontSize = 12.sp)
                            }
                            Switch(
                                checked = wakeWordEnabled,
                                onCheckedChange = {
                                    wakeWordEnabled = it
                                    if (it) sessionManager.wakeWordManager.startListening()
                                    else sessionManager.wakeWordManager.stopListening()
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                            )
                        }

                        HorizontalDivider(color = DarkSurface)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Background Audio Service", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text("Keeps wake-word active with ongoing notification", color = TextSecondary, fontSize = 12.sp)
                            }
                            Switch(
                                checked = bgServiceEnabled,
                                onCheckedChange = {
                                    bgServiceEnabled = it
                                    if (it) BackgroundAudioService.startService(context)
                                    else BackgroundAudioService.stopService(context)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                            )
                        }

                        HorizontalDivider(color = DarkSurface)

                        // Battery optimization guide
                        OutlinedButton(
                            onClick = {
                                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                                if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.BatteryAlert, contentDescription = null, tint = WarningAmber)
                            Text(" Battery Optimization Settings", color = WarningAmber, fontSize = 13.sp)
                        }
                    }
                }
            }

            // 3. AI & GEMINI LIVE CONFIG
            item {
                SectionHeader("AI ENGINE & GEMINI LIVE")
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = 16.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Model Architecture", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("Gemini 2.5 Flash Native Audio", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("API Key Status", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            val isKeySet = sessionManager.geminiLiveManager.isApiKeyConfigured()
                            Text(
                                if (isKeySet) "Configured (Secure)" else "Missing / Incomplete",
                                color = if (isKeySet) EmeraldGreen else AlertRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { sessionManager.geminiLiveManager.startSession() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurface)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = NeonCyan)
                            Text(" Reconnect Live Session", color = NeonCyan, fontSize = 13.sp)
                        }
                    }
                }
            }

            // 4. LOCAL MEMORY
            item {
                SectionHeader("LOCAL MEMORY & KNOWLEDGE")
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = 16.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Saved Knowledge Items (${memories.size})", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            if (memories.isNotEmpty()) {
                                IconButton(onClick = {
                                    scope.launch { sessionManager.memoryManager.clearAllMemory() }
                                }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear All", tint = AlertRed)
                                }
                            }
                        }

                        if (memories.isEmpty()) {
                            Text("No saved memories yet. Say 'MAX ye yaad rakhna...' to remember notes!", color = TextSecondary, fontSize = 12.sp)
                        } else {
                            memories.take(5).forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.topic, color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(item.content, color = TextSecondary, fontSize = 12.sp)
                                    }
                                    IconButton(onClick = {
                                        scope.launch { sessionManager.memoryManager.deleteMemoryById(item.id) }
                                    }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. PRIVACY & RESET
            item {
                SectionHeader("PRIVACY & DATA CONTROLS")
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = 16.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onNavigateToPermissions,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurface)
                        ) {
                            Text("Manage App Permissions", color = TextPrimary)
                        }

                        Button(
                            onClick = onNavigateToPrivacy,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurface)
                        ) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = EmeraldGreen)
                            Text(" Privacy Policy & Disclosures", color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = NeonCyan,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
    )
}
