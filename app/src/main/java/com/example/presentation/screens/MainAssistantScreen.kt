package com.example.presentation.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ai.AssistantPersonality
import com.example.ai.IndiaContextHelper
import com.example.ai.LiveSessionManager
import com.example.ai.WallpaperType
import com.example.domain.model.AssistantState
import com.example.presentation.components.ConfirmationDialog
import com.example.presentation.components.GlassPanel
import com.example.presentation.components.GlowingOrb
import com.example.service.BackgroundAudioService
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
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val assistantState by sessionManager.assistantState.collectAsState()
    val statusMessage by sessionManager.statusMessage.collectAsState()
    val pendingConfirmation by sessionManager.pendingConfirmation.collectAsState()
    val inputAmplitude by sessionManager.inputAmplitude.collectAsState()
    val outputAmplitude by sessionManager.outputAmplitude.collectAsState()
    val permissionState by sessionManager.permissionManager.state.collectAsState()

    val wallpaperThemeManager = sessionManager.wallpaperThemeManager
    val wallpaperType by wallpaperThemeManager.wallpaperType.collectAsState()
    val customPhotoUri by wallpaperThemeManager.customPhotoUri.collectAsState()
    val wallpaperDim by wallpaperThemeManager.wallpaperDim.collectAsState()
    val cleanMode by wallpaperThemeManager.cleanMode.collectAsState()
    val personality by wallpaperThemeManager.personality.collectAsState()

    var showCommandsSheet by remember { mutableStateOf(false) }
    var isBgRunning by remember { mutableStateOf(BackgroundAudioService.isServiceRunning) }
    var showMicErrorDialog by remember { mutableStateOf(false) }

    val isGirlfriend = personality == AssistantPersonality.GIRLFRIEND_MODE

    val quickCommands = if (isGirlfriend) {
        listOf(
            "Babu kahan ho?",
            "Aapne khana khaya?",
            "Aaj ka din kaisa tha?",
            "Aaj kaun sa festival hai?",
            "Kitne baje hain?",
            "Switch mode"
        )
    } else {
        listOf(
            "Battery status",
            "Open YouTube",
            "Torch on",
            "Aaj kya date hai?",
            "Upcoming Indian festivals",
            "Switch to girlfriend mode"
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. FULL SCREEN WALLPAPER LAYER
        when (wallpaperType) {
            WallpaperType.CUTE_GIRL -> {
                Image(
                    painter = painterResource(id = R.drawable.img_cute_wallpaper),
                    contentDescription = "Cute Wallpaper",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            WallpaperType.CUSTOM_PHOTO -> {
                if (!customPhotoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = customPhotoUri,
                        contentDescription = "Custom User Wallpaper",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.img_cute_wallpaper),
                        contentDescription = "Cute Wallpaper",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            WallpaperType.CYBER_DARK -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(DarkSpaceBg, Color(0xFF090E1F), DarkSpaceBg)
                            )
                        )
                )
            }
        }

        // 2. VIGNETTE & CONTRAST OVERLAY (Keep wallpaper vibrant while ensuring text is readable)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = (wallpaperDim * 1.3f).coerceAtMost(0.85f)),
                            Color.Black.copy(alpha = (wallpaperDim * 0.35f)),
                            Color.Black.copy(alpha = (wallpaperDim * 1.4f).coerceAtMost(0.90f))
                        )
                    )
                )
        )

        // 3. MAIN UI CONTENT
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
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
                // Secret Reel Mode Switch Zone: Tapping this silently toggles GF / Normal mode!
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            wallpaperThemeManager.togglePersonality()
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isGirlfriend) {
                                    Brush.linearGradient(listOf(PulseMagenta, Color(0xFFFF80AB)))
                                } else {
                                    Brush.linearGradient(listOf(NeonCyan, ElectricViolet))
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isGirlfriend) "♥" else "M",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = if (isGirlfriend) 18.sp else 16.sp
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isGirlfriend) "MAX BABU" else "MAX AI",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                letterSpacing = 0.5.sp
                            )
                            if (isGirlfriend) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "❤️",
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (assistantState) {
                                            AssistantState.LISTENING -> EmeraldGreen
                                            AssistantState.THINKING -> PulseMagenta
                                            AssistantState.SPEAKING -> if (isGirlfriend) PulseMagenta else NeonCyan
                                            AssistantState.ERROR -> AlertRed
                                            AssistantState.OFFLINE -> WarningAmber
                                            else -> if (isGirlfriend) PulseMagenta.copy(alpha = 0.7f) else NeonCyan.copy(alpha = 0.7f)
                                        }
                                    )
                            )
                            Text(
                                text = if (isGirlfriend) "Cute & Pyari Voice" else assistantState.name,
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }

                // Top Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick Clean Mode Toggle (Hides all extra screen text for clean reel/wallpaper look)
                    IconButton(onClick = { wallpaperThemeManager.toggleCleanMode() }) {
                        Icon(
                            imageVector = if (cleanMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Clean Mode Toggle",
                            tint = if (cleanMode) NeonCyan else TextSecondary
                        )
                    }

                    // Background Audio Service Toggle (Runs in background even when app closed)
                    IconButton(onClick = {
                        if (isBgRunning) {
                            BackgroundAudioService.stopService(context)
                            isBgRunning = false
                        } else {
                            BackgroundAudioService.startService(context)
                            isBgRunning = true
                        }
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }) {
                        Icon(
                            imageVector = if (isBgRunning) Icons.Default.Hearing else Icons.Default.CloudOff,
                            contentDescription = "Background Audio",
                            tint = if (isBgRunning) EmeraldGreen else TextSecondary
                        )
                    }

                    // Mic Permission indicator
                    IconButton(onClick = onNavigateToPermissions) {
                        Icon(
                            imageVector = if (permissionState.recordAudio) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = "Permissions",
                            tint = if (permissionState.recordAudio) NeonCyan else AlertRed
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

            // CENTER: FLOATING GLOWING ORB OVER WALLPAPER
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                GlowingOrb(
                    state = assistantState,
                    inputAmplitude = inputAmplitude,
                    outputAmplitude = outputAmplitude,
                    size = if (cleanMode) 220.dp else 200.dp,
                    isGirlfriendMode = isGirlfriend,
                    onClick = {
                        sessionManager.onOrbClicked()
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Translucent Floating Speech Feedback (Only shows when speaking, listening or not in ultra-clean mode)
                val shouldShowSpeechPanel = !cleanMode ||
                        assistantState == AssistantState.LISTENING ||
                        assistantState == AssistantState.SPEAKING ||
                        assistantState == AssistantState.THINKING ||
                        assistantState == AssistantState.ERROR

                AnimatedVisibility(
                    visible = shouldShowSpeechPanel,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    GlassPanel(
                        modifier = Modifier.fillMaxWidth(0.90f),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = 14.dp,
                        backgroundColor = Color.Black.copy(alpha = 0.55f),
                        borderColor = when (assistantState) {
                            AssistantState.LISTENING -> EmeraldGreen.copy(alpha = 0.5f)
                            AssistantState.THINKING -> PulseMagenta.copy(alpha = 0.5f)
                            AssistantState.SPEAKING -> if (isGirlfriend) PulseMagenta.copy(alpha = 0.6f) else NeonCyan.copy(alpha = 0.6f)
                            AssistantState.ERROR -> AlertRed.copy(alpha = 0.5f)
                            else -> GlassBorder.copy(alpha = 0.4f)
                        }
                    ) {
                        Text(
                            text = if (statusMessage.isBlank()) {
                                if (isGirlfriend) "Haan babu bolo, sun rahi hoon... ❤️" else "Listening..."
                            } else statusMessage,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                val isKeyConfigured = sessionManager.geminiLiveManager.isApiKeyConfigured()
                val isMicError = assistantState == AssistantState.ERROR &&
                        (statusMessage.contains("Microphone", ignoreCase = true) || !permissionState.recordAudio)

                if (assistantState == AssistantState.ERROR) {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (!isKeyConfigured) {
                        Button(
                            onClick = onNavigateToSettings,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Enter Gemini API Key",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    } else if (isMicError) {
                        Button(
                            onClick = { showMicErrorDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MicOff,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Microphone Unavailable",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Requirement 29: Clear error dialog for microphone unavailability
            if (showMicErrorDialog) {
                AlertDialog(
                    onDismissRequest = { showMicErrorDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MicOff,
                                contentDescription = null,
                                tint = AlertRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Microphone Unavailable",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    },
                    text = {
                        Text(
                            text = "Microphone unavailable. Please check microphone permission and make sure another app is not using the microphone.",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showMicErrorDialog = false
                                onNavigateToPermissions()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            Text("Open Permissions", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showMicErrorDialog = false }) {
                            Text("Dismiss", color = TextSecondary)
                        }
                    },
                    containerColor = DarkSurface
                )
            }

            // BOTTOM CONTROLS
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // If cleanMode is enabled, show only a minimal sleek pill to toggle commands
                if (cleanMode) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .clickable { showCommandsSheet = !showCommandsSheet }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (isGirlfriend) PulseMagenta else NeonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (showCommandsSheet) "Hide Prompts" else if (isGirlfriend) "Girlfriend Mode Active ❤️" else "✨ Quick Prompts",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Prompts row (visible if cleanMode is OFF or user expanded it)
                AnimatedVisibility(visible = !cleanMode || showCommandsSheet) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    ) {
                        items(quickCommands) { command ->
                            AssistChip(
                                onClick = {
                                    sessionManager.geminiLiveManager.activateListening()
                                },
                                label = {
                                    Text(
                                        text = command,
                                        color = TextPrimary,
                                        fontSize = 12.sp
                                    )
                                },
                                shape = RoundedCornerShape(18.dp),
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Color.Black.copy(alpha = 0.65f)
                                ),
                                border = AssistChipDefaults.assistChipBorder(
                                    enabled = true,
                                    borderColor = if (isGirlfriend) PulseMagenta.copy(alpha = 0.4f) else GlassBorder
                                )
                            )
                        }
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
