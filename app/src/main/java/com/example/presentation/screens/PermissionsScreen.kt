package com.example.presentation.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.permissions.PermissionManager
import com.example.presentation.components.GlassPanel
import com.example.ui.theme.AlertRed
import com.example.ui.theme.DarkSpaceBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PermissionsScreen(
    permissionManager: PermissionManager,
    onBack: () -> Unit
) {
    val state by permissionManager.state.collectAsState()

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissionManager.refreshPermissions() }

    val contactsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissionManager.refreshPermissions() }

    val callLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissionManager.refreshPermissions() }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissionManager.refreshPermissions() }

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
                text = "Assistant Permissions",
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
                    text = "MAX requires specific runtime permissions to interact with your phone on your command. Data remains on-device.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                PermissionCard(
                    title = "Microphone",
                    description = "Required for real-time speech input and Gemini Live audio conversation.",
                    icon = Icons.Default.Mic,
                    isGranted = state.recordAudio,
                    onRequest = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                )
            }

            item {
                PermissionCard(
                    title = "Contacts",
                    description = "Used to search and match contact names when you say 'Call [Name]' or 'WhatsApp [Name]'.",
                    icon = Icons.Default.Contacts,
                    isGranted = state.readContacts,
                    onRequest = { contactsLauncher.launch(Manifest.permission.READ_CONTACTS) }
                )
            }

            item {
                PermissionCard(
                    title = "Phone Calls",
                    description = "Allows MAX to directly initiate phone calls to your requested contacts.",
                    icon = Icons.Default.Call,
                    isGranted = state.callPhone,
                    onRequest = { callLauncher.launch(Manifest.permission.CALL_PHONE) }
                )
            }

            item {
                PermissionCard(
                    title = "Notifications",
                    description = "Delivers your scheduled reminders, alarms, and background assistant status.",
                    icon = Icons.Default.Notifications,
                    isGranted = state.postNotifications,
                    onRequest = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Open App Settings button
        OutlinedButton(
            onClick = { permissionManager.openAppSettings() },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Open System App Settings",
                color = NeonCyan,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    GlassPanel(
        shape = RoundedCornerShape(16.dp),
        backgroundColor = DarkSurface.copy(alpha = 0.85f),
        borderColor = if (isGranted) EmeraldGreen.copy(alpha = 0.4f) else AlertRed.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isGranted) EmeraldGreen.copy(alpha = 0.15f) else AlertRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) EmeraldGreen else AlertRed,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isGranted) EmeraldGreen.copy(alpha = 0.2f) else AlertRed.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isGranted) "GRANTED" else "NOT GRANTED",
                            color = if (isGranted) EmeraldGreen else AlertRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            if (!isGranted) {
                Button(
                    onClick = onRequest,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Allow", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Granted",
                    tint = EmeraldGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
