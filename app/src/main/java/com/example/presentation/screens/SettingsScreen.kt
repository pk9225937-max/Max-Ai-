package com.example.presentation.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AssistantPersonality
import com.example.ai.IndiaContextHelper
import com.example.ai.LiveSessionManager
import com.example.ai.WallpaperType
import com.example.presentation.components.GlassPanel
import com.example.service.BackgroundAudioService
import com.example.ui.theme.AlertRed
import com.example.ui.theme.DarkSpaceBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.PulseMagenta
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

    val wallpaperThemeManager = sessionManager.wallpaperThemeManager
    val wallpaperType by wallpaperThemeManager.wallpaperType.collectAsState()
    val customPhotoUri by wallpaperThemeManager.customPhotoUri.collectAsState()
    val wallpaperDim by wallpaperThemeManager.wallpaperDim.collectAsState()
    val cleanMode by wallpaperThemeManager.cleanMode.collectAsState()
    val personality by wallpaperThemeManager.personality.collectAsState()
    val selectedVoice by wallpaperThemeManager.selectedVoice.collectAsState()

    var wakeWordEnabled by remember { mutableStateOf(true) }
    var bgServiceEnabled by remember { mutableStateOf(BackgroundAudioService.isServiceRunning) }
    var voiceSpeed by remember { mutableFloatStateOf(1.0f) }

    val memories by sessionManager.memoryManager.getAllMemory().collectAsState(initial = emptyList())

    var customApiKeyInput by remember {
        mutableStateOf(sessionManager.geminiLiveManager.getCustomApiKey())
    }
    var showApiKey by remember { mutableStateOf(false) }
    var keySaveStatus by remember { mutableStateOf<String?>(null) }
    var isKeyConfiguredState by remember {
        mutableStateOf(sessionManager.geminiLiveManager.isApiKeyConfigured())
    }

    var micTestRunning by remember { mutableStateOf(false) }
    var micTestResult by remember { mutableStateOf<com.example.audio.MicrophoneTestResult?>(null) }

    // Photo picker launcher for user's own photos
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            // Persist read permission if applicable and store URI
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore if not persistable
            }
            wallpaperThemeManager.setCustomPhotoUri(uri.toString())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSpaceBg)
            .padding(16.dp)
    ) {
        // TOP HEADER
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Text(
                text = "Assistant & Theme Settings",
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
            // 1. WALLPAPER & LIVE THEME SECTION
            item {
                SectionHeader("WALLPAPER & LIVE THEME")
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = 16.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "Background Wallpaper",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )

                        // Option 1: Cute Girl Wallpaper (Default)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (wallpaperType == WallpaperType.CUTE_GIRL) DarkSurface else Color.Transparent)
                                .clickable { wallpaperThemeManager.setWallpaperType(WallpaperType.CUTE_GIRL) }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(PulseMagenta.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = PulseMagenta,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text("Cute AI Companion (Cute Girl)", color = TextPrimary, fontWeight = FontWeight.Medium)
                                    Text("Full-screen aesthetic portrait", color = TextSecondary, fontSize = 12.sp)
                                }
                            }
                            if (wallpaperType == WallpaperType.CUTE_GIRL) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = PulseMagenta)
                            }
                        }

                        // Option 2: Select from phone gallery (User's own pic)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (wallpaperType == WallpaperType.CUSTOM_PHOTO) DarkSurface else Color.Transparent)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(NeonCyan.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text("Apni Photo Lagayein (From Gallery)", color = TextPrimary, fontWeight = FontWeight.Medium)
                                    Text(
                                        if (customPhotoUri != null) "Custom photo selected • Tap to change" else "Select any photo from your phone",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            if (wallpaperType == WallpaperType.CUSTOM_PHOTO) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = NeonCyan)
                            }
                        }

                        // Option 3: Cyber Dark Space
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (wallpaperType == WallpaperType.CYBER_DARK) DarkSurface else Color.Transparent)
                                .clickable { wallpaperThemeManager.setWallpaperType(WallpaperType.CYBER_DARK) }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(DarkSurface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text("Cyber Minimalist (Dark Canvas)", color = TextPrimary, fontWeight = FontWeight.Medium)
                                    Text("Deep dark void gradient", color = TextSecondary, fontSize = 12.sp)
                                }
                            }
                            if (wallpaperType == WallpaperType.CYBER_DARK) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = NeonCyan)
                            }
                        }

                        HorizontalDivider(color = DarkSurface)

                        // Wallpaper Dimming slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Wallpaper Brightness / Contrast", color = TextPrimary, fontWeight = FontWeight.Medium)
                                Text("${((1f - wallpaperDim) * 100).toInt()}%", color = NeonCyan, fontWeight = FontWeight.Bold)
                            }
                            Text("Adjust background dimming so wallpaper looks bright and clear", color = TextSecondary, fontSize = 12.sp)
                            Slider(
                                value = wallpaperDim,
                                onValueChange = { wallpaperThemeManager.setWallpaperDim(it) },
                                valueRange = 0.05f..0.75f,
                                colors = SliderDefaults.colors(
                                    thumbColor = NeonCyan,
                                    activeTrackColor = NeonCyan
                                )
                            )
                        }

                        HorizontalDivider(color = DarkSurface)

                        // Clean Screen Mode toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Clean Screen Mode (No Extra Text)", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text("Hides extra prompts and clutter so wallpaper remains clean for Instagram videos", color = TextSecondary, fontSize = 12.sp)
                            }
                            Switch(
                                checked = cleanMode,
                                onCheckedChange = { wallpaperThemeManager.setCleanMode(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                            )
                        }
                    }
                }
            }

            // 2. GIRLFRIEND MODE & PERSONALITY (INSTAGRAM REEL SPECIAL)
            item {
                SectionHeader("GIRLFRIEND MODE & VOICE")
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
                                Text(
                                    text = if (personality == AssistantPersonality.GIRLFRIEND_MODE) "Girlfriend Mode (Active ❤️)" else "Girlfriend Mode",
                                    color = if (personality == AssistantPersonality.GIRLFRIEND_MODE) PulseMagenta else TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Speaks in a very cute, sweet, loving Hindi/Hinglish voice ('Babu', 'Jaan', caring & playful)",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            Switch(
                                checked = personality == AssistantPersonality.GIRLFRIEND_MODE,
                                onCheckedChange = {
                                    if (it) wallpaperThemeManager.setPersonality(AssistantPersonality.GIRLFRIEND_MODE)
                                    else wallpaperThemeManager.setPersonality(AssistantPersonality.MAX_NORMAL)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = PulseMagenta)
                            )
                        }

                        HorizontalDivider(color = DarkSurface)

                        // Voice Selection (Aoede / Kore)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Voice Tone", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text("Cute, sweet, melodic female voices", color = TextSecondary, fontSize = 12.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { wallpaperThemeManager.setVoice("Aoede") },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = if (selectedVoice == "Aoede") PulseMagenta else TextSecondary
                                    ),
                                    border = ButtonDefaults.outlinedButtonBorder.takeIf { selectedVoice == "Aoede" }
                                ) {
                                    Text("Aoede (Sweet)")
                                }
                                OutlinedButton(
                                    onClick = { wallpaperThemeManager.setVoice("Kore") },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = if (selectedVoice == "Kore") PulseMagenta else TextSecondary
                                    )
                                ) {
                                    Text("Kore (Playful)")
                                }
                            }
                        }

                        HorizontalDivider(color = DarkSurface)

                        // Secret Switch guide for Instagram videos
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .border(1.dp, PulseMagenta.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = PulseMagenta, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Secret Mode Switch for Instagram Reels", color = PulseMagenta, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Text(
                                    text = "• Voice Trigger: Say 'GF mode on', 'Switch to girlfriend mode', or 'Normal mode'.\n" +
                                            "• Stealth Action: MAX will NEVER say 'Switching mode'. She will instantly speak in her sweet girlfriend persona in her very next sentence without giving it away!\n" +
                                            "• Stealth Touch: Double-tap the top-left 'M / ♥' icon on the home screen for an invisible silent toggle with haptic feedback.",
                                    color = TextPrimary.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            // 3. INDIA CALENDAR, TIME & FESTIVALS
            item {
                SectionHeader("INDIA TIME & FESTIVAL ENGINE")
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
                            Text("Current Indian Time (IST)", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = IndiaContextHelper.getCurrentIndianTimeOnly(),
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Text(
                            text = IndiaContextHelper.getCurrentIndianDateTimeFormatted(),
                            color = TextSecondary,
                            fontSize = 12.sp
                        )

                        HorizontalDivider(color = DarkSurface)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Indian Festivals Knowledge", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("Active & Synced", color = EmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Text(
                            text = "Aware of Diwali, Holi, Raksha Bandhan, Navratri, Dussehra, Eid, Chhath Puja, Janmashtami, Independence Day & all major Indian holidays.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // 4. WAKE WORD & BACKGROUND SERVICE
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
                                Text(
                                    text = if (bgServiceEnabled) "Background Audio (Running Active)" else "Background Audio Service",
                                    color = if (bgServiceEnabled) EmeraldGreen else TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text("Keeps listening and responding even when app is minimized or phone screen locked", color = TextSecondary, fontSize = 12.sp)
                            }
                            Switch(
                                checked = bgServiceEnabled,
                                onCheckedChange = {
                                    bgServiceEnabled = it
                                    if (it) BackgroundAudioService.startService(context)
                                    else BackgroundAudioService.stopService(context)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = EmeraldGreen)
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
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Battery Optimization Settings", color = WarningAmber, fontSize = 13.sp)
                        }
                    }
                }
            }

            // 5. MICROPHONE DIAGNOSTICS & HARDWARE TEST (Requirement 28)
            item {
                SectionHeader("MICROPHONE HARDWARE DIAGNOSTICS")
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = 16.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Native Audio Pipeline", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text("AudioRecord 16kHz PCM (No Google SpeechRecognizer dependency)", color = TextSecondary, fontSize = 12.sp)
                            }
                            Text(
                                text = "Native AudioRecord",
                                color = EmeraldGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        HorizontalDivider(color = DarkSurface)

                        Button(
                            onClick = {
                                scope.launch {
                                    micTestRunning = true
                                    micTestResult = sessionManager.testMicrophone()
                                    micTestRunning = false
                                }
                            },
                            enabled = !micTestRunning,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (micTestRunning) "Testing Microphone Hardware..." else "Test Microphone Now",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        micTestResult?.let { result ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (result.success) EmeraldGreen.copy(alpha = 0.15f) else AlertRed.copy(alpha = 0.15f))
                                    .border(
                                        1.dp,
                                        if (result.success) EmeraldGreen.copy(alpha = 0.5f) else AlertRed.copy(alpha = 0.5f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (result.success) Icons.Default.Check else Icons.Default.Close,
                                            contentDescription = null,
                                            tint = if (result.success) EmeraldGreen else AlertRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (result.success) "Microphone Test: PASSED" else "Microphone Test: FAILED",
                                            color = if (result.success) EmeraldGreen else AlertRed,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Text(
                                        text = result.message,
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                    if (result.success) {
                                        Text(
                                            text = "PCM Volume Level: ${result.peakAmplitudePercentage}% | Captured: ${result.bytesCaptured} bytes",
                                            color = NeonCyan,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        )
                                    } else {
                                        OutlinedButton(
                                            onClick = onNavigateToPermissions,
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Open Permissions Screen", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. AI ENGINE & GEMINI LIVE CONFIG
            item {
                SectionHeader("AI ENGINE & GEMINI LIVE")
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = 16.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Model Architecture", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("Gemini 2.5 Flash Native Audio", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("API Key Status", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (isKeyConfiguredState) "Configured (Ready)" else "Missing / Incomplete",
                                color = if (isKeyConfiguredState) EmeraldGreen else AlertRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        HorizontalDivider(color = DarkSurface)

                        Text(
                            text = "Gemini API Key",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Enter your Google AI Studio Gemini API key to power voice chat and live AI responses.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )

                        OutlinedTextField(
                            value = customApiKeyInput,
                            onValueChange = {
                                customApiKeyInput = it
                                keySaveStatus = null
                            },
                            placeholder = {
                                Text("AIzaSy...", color = TextSecondary.copy(alpha = 0.5f), fontSize = 13.sp)
                            },
                            singleLine = true,
                            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showApiKey = !showApiKey }) {
                                    Icon(
                                        imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showApiKey) "Hide Key" else "Show Key",
                                        tint = TextSecondary
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = GlassBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = NeonCyan
                            )
                        )

                        if (keySaveStatus != null) {
                            Text(
                                text = keySaveStatus ?: "",
                                color = if (isKeyConfiguredState) EmeraldGreen else AlertRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val trimmed = customApiKeyInput.trim()
                                    if (trimmed.isNotBlank()) {
                                        sessionManager.geminiLiveManager.saveCustomApiKey(trimmed)
                                        isKeyConfiguredState = true
                                        keySaveStatus = "Key saved! Connecting to MAX..."
                                        sessionManager.geminiLiveManager.startSession()
                                    } else {
                                        keySaveStatus = "Please enter a valid API key."
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Key", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            if (sessionManager.geminiLiveManager.getCustomApiKey().isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        sessionManager.geminiLiveManager.clearCustomApiKey()
                                        customApiKeyInput = ""
                                        isKeyConfiguredState = sessionManager.geminiLiveManager.isApiKeyConfigured()
                                        keySaveStatus = "Key cleared."
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed)
                                ) {
                                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, tint = AlertRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clear", color = AlertRed, fontSize = 13.sp)
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Get Free Gemini API Key", color = NeonCyan, fontSize = 13.sp)
                        }

                        Button(
                            onClick = { sessionManager.geminiLiveManager.startSession() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurface)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reconnect Live Session", color = NeonCyan, fontSize = 13.sp)
                        }
                    }
                }
            }

            // 6. LOCAL MEMORY
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
                            Text("No saved memories yet. Say \"Remember my meeting tomorrow\" to save info.", color = TextSecondary, fontSize = 12.sp)
                        } else {
                            memories.take(5).forEach { memory ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkSurface)
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(memory.topic, color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(memory.content, color = TextPrimary, fontSize = 13.sp)
                                    }
                                    IconButton(
                                        onClick = {
                                            scope.launch { sessionManager.memoryManager.forgetMemory(memory.topic) }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = TextSecondary.copy(alpha = 0.7f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}
