package com.example.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PermissionState(
    val recordAudio: Boolean = false,
    val readContacts: Boolean = false,
    val callPhone: Boolean = false,
    val postNotifications: Boolean = false
) {
    val allGranted: Boolean
        get() = recordAudio && readContacts && callPhone && postNotifications
}

class PermissionManager(private val context: Context) {

    private val _state = MutableStateFlow(checkAllPermissions())
    val state: StateFlow<PermissionState> = _state.asStateFlow()

    fun refreshPermissions(): PermissionState {
        val updated = checkAllPermissions()
        _state.value = updated
        return updated
    }

    fun hasRecordAudio(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasReadContacts(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasCallPhone(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasPostNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun checkAllPermissions(): PermissionState {
        return PermissionState(
            recordAudio = hasRecordAudio(),
            readContacts = hasReadContacts(),
            callPhone = hasCallPhone(),
            postNotifications = hasPostNotifications()
        )
    }

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
