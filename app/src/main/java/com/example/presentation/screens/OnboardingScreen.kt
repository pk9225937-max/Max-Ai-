package com.example.presentation.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.permissions.PermissionManager
import com.example.presentation.components.GlassPanel
import com.example.ui.theme.DarkSpaceBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.PulseMagenta
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun OnboardingScreen(
    permissionManager: PermissionManager,
    onComplete: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissionManager.refreshPermissions() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSpaceBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Step Indicator Dots
        Row(
            modifier = Modifier.padding(top = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(4) { i ->
                Box(
                    modifier = Modifier
                        .size(if (step == i) 24.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (step == i) NeonCyan else TextSecondary.copy(alpha = 0.3f)
                        )
                )
            }
        }

        AnimatedContent(targetState = step, label = "OnboardingSteps") { currentStep ->
            when (currentStep) {
                0 -> WelcomeStep()
                1 -> PrivacyStep()
                2 -> PermissionsStep(
                    permissionManager = permissionManager,
                    onRequestMic = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                )
                3 -> ReadyStep()
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (step > 0 && step < 3) {
                OutlinedButton(
                    onClick = { step-- },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Back", color = TextSecondary)
                }
            }

            Button(
                onClick = {
                    if (step < 3) {
                        step++
                    } else {
                        onComplete()
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                modifier = Modifier.weight(if (step == 0 || step == 3) 2f else 1f)
            ) {
                Text(
                    text = if (step == 3) "Start Using MAX" else "Continue",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(NeonCyan, ElectricViolet, PulseMagenta))
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("MAX", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 28.sp)
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Meet MAX Assistant",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Your voice-first native Android AI companion. Smart, confident, slightly witty, and fluent in Hindi, English, and Hinglish.",
            color = TextSecondary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun PrivacyStep() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = EmeraldGreen,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Privacy First Architecture",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        GlassPanel(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            contentPadding = 16.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("• Wake-word is processed locally on your device.", color = TextPrimary, fontSize = 13.sp)
                Text("• Contacts stay completely on your phone.", color = TextPrimary, fontSize = 13.sp)
                Text("• All tool calls require your safety verification.", color = TextPrimary, fontSize = 13.sp)
                Text("• You can wipe all stored data at any time.", color = TextPrimary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun PermissionsStep(
    permissionManager: PermissionManager,
    onRequestMic: () -> Unit
) {
    val hasMic = permissionManager.hasRecordAudio()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            tint = NeonCyan,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Microphone Access",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "To hold real-time voice conversations and detect wake-words, MAX requires microphone permission.",
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (!hasMic) {
            Button(
                onClick = onRequestMic,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Allow Microphone", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = EmeraldGreen)
                Text("Microphone Permission Granted", color = EmeraldGreen, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ReadyStep() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(EmeraldGreen.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = EmeraldGreen,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "\"Hi, I'm MAX.\nReady when you are.\"",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Tap the glowing Orb or say \"Hey MAX\" anytime to begin talking.",
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}
