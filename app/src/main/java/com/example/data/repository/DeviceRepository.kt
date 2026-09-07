package com.example.data.repository

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import com.example.domain.model.DeviceStatus

class DeviceRepository(private val context: Context) {

    private val cameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    }

    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    private var isTorchOn = false

    // 13. APP LAUNCHING
    fun openApp(targetNameOrPackage: String): Pair<Boolean, String> {
        val pm = context.packageManager

        // First check if it's already a package name
        var launchIntent = pm.getLaunchIntentForPackage(targetNameOrPackage)

        // If not found, search known app aliases or installed apps
        if (launchIntent == null) {
            val commonPackages = mapOf(
                "youtube" to "com.google.android.youtube",
                "whatsapp" to "com.whatsapp",
                "gmail" to "com.google.android.gm",
                "chrome" to "com.android.chrome",
                "instagram" to "com.instagram.android",
                "calculator" to "com.google.android.calculator",
                "camera" to "com.google.android.GoogleCamera",
                "maps" to "com.google.android.apps.maps",
                "settings" to "com.android.settings",
                "photos" to "com.google.android.apps.photos",
                "gallery" to "com.google.android.apps.photos"
            )

            val lower = targetNameOrPackage.lowercase().trim()
            val matchedPackage = commonPackages[lower]
            if (matchedPackage != null) {
                launchIntent = pm.getLaunchIntentForPackage(matchedPackage)
            }

            if (launchIntent == null) {
                // Search installed apps by label
                val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val apps = pm.queryIntentActivities(mainIntent, 0)
                for (resolveInfo in apps) {
                    val label = resolveInfo.loadLabel(pm).toString()
                    if (label.contains(targetNameOrPackage, ignoreCase = true) ||
                        resolveInfo.activityInfo.packageName.contains(targetNameOrPackage, ignoreCase = true)
                    ) {
                        launchIntent = pm.getLaunchIntentForPackage(resolveInfo.activityInfo.packageName)
                        if (launchIntent != null) break
                    }
                }
            }
        }

        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            Pair(true, "App opened successfully.")
        } else {
            Pair(false, "Ye app phone me installed nahi hai.")
        }
    }

    // 14. PHONE CALL
    fun makePhoneCall(phoneNumber: String): Pair<Boolean, String> {
        val cleanedNumber = phoneNumber.replace("[^0-9+]".toRegex(), "")
        if (cleanedNumber.isBlank()) {
            return Pair(false, "Invalid phone number.")
        }

        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intent = if (hasCallPermission) {
            Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleanedNumber"))
        } else {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanedNumber"))
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            Pair(true, if (hasCallPermission) "Calling $phoneNumber" else "Dialer opened for $phoneNumber")
        } catch (e: Exception) {
            Pair(false, "Call initiate nahi ho paya: ${e.localizedMessage}")
        }
    }

    // 15. WHATSAPP
    fun sendWhatsAppMessage(phoneNumber: String, message: String): Pair<Boolean, String> {
        val pm = context.packageManager
        val isInstalled = try {
            pm.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

        if (!isInstalled) {
            return Pair(false, "WhatsApp installed nahi mil raha.")
        }

        val cleanedNumber = phoneNumber.replace("[^0-9]".toRegex(), "")
        return try {
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanedNumber&text=${Uri.encode(message)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "WhatsApp chat opened with pre-filled message.")
        } catch (e: Exception) {
            Pair(false, "WhatsApp open nahi ho paya: ${e.localizedMessage}")
        }
    }

    // 16. SMS
    fun sendSMS(phoneNumber: String, message: String): Pair<Boolean, String> {
        val uri = Uri.parse("smsto:$phoneNumber")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            Pair(true, "SMS compose screen opened with message.")
        } catch (e: Exception) {
            Pair(false, "SMS open nahi ho paya: ${e.localizedMessage}")
        }
    }

    // 17. EMAIL
    fun sendGmail(recipientEmail: String, subject: String, body: String): Pair<Boolean, String> {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            Pair(true, "Email compose screen opened.")
        } catch (e: Exception) {
            Pair(false, "Email client open nahi ho paya: ${e.localizedMessage}")
        }
    }

    // 21. DEVICE STATUS
    fun getDeviceStatus(): DeviceStatus {
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) ((level.toFloat() / scale.toFloat()) * 100).toInt() else 100

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val stat = StatFs(Environment.getDataDirectory().path)
        val blockSize = stat.blockSizeLong
        val availableBlocks = stat.availableBlocksLong
        val totalBlocks = stat.blockCountLong

        val freeGb = (availableBlocks * blockSize).toDouble() / (1024 * 1024 * 1024)
        val totalGb = (totalBlocks * blockSize).toDouble() / (1024 * 1024 * 1024)

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNetwork)
        val isConnected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val netType = when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Mobile Data"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
            else -> "Disconnected"
        }

        return DeviceStatus(
            batteryPercentage = batteryPct,
            isCharging = isCharging,
            freeStorageGb = (freeGb * 10.0).toInt() / 10.0,
            totalStorageGb = (totalGb * 10.0).toInt() / 10.0,
            model = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            isConnected = isConnected,
            networkType = netType
        )
    }

    // 22. SETTINGS
    fun openSetting(settingType: String): Pair<Boolean, String> {
        val action = when (settingType.lowercase().trim()) {
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "sound", "volume" -> Settings.ACTION_SOUND_SETTINGS
            "display", "brightness" -> Settings.ACTION_DISPLAY_SETTINGS
            "battery" -> Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
            "notifications" -> Settings.ACTION_APP_NOTIFICATION_SETTINGS
            "permissions" -> Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }

        val intent = if (action is String) Intent(action) else action as Intent
        if (action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
            intent.data = Uri.fromParts("package", context.packageName, null)
        } else if (action == Settings.ACTION_APP_NOTIFICATION_SETTINGS) {
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            Pair(true, "$settingType settings opened.")
        } catch (e: Exception) {
            Pair(false, "Setting open nahi ho saki: ${e.localizedMessage}")
        }
    }

    // 23. FLASHLIGHT
    fun setFlashlight(enable: Boolean): Pair<Boolean, String> {
        val cm = cameraManager ?: return Pair(false, "Camera hardware unavailable.")
        return try {
            val cameraId = cm.cameraIdList.firstOrNull { id ->
                val chars = cm.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return Pair(false, "Phone me flashlight hardware nahi mila.")

            cm.setTorchMode(cameraId, enable)
            isTorchOn = enable
            Pair(true, if (enable) "Flashlight turned on." else "Flashlight turned off.")
        } catch (e: CameraAccessException) {
            Pair(false, "Flashlight access error: ${e.localizedMessage}")
        } catch (e: Exception) {
            Pair(false, "Flashlight toggle failed: ${e.localizedMessage}")
        }
    }

    // 24. MEDIA CONTROL
    fun controlMedia(command: String): Pair<Boolean, String> {
        val am = audioManager ?: return Pair(false, "Audio service unavailable.")
        val keyCode = when (command.lowercase().trim()) {
            "play" -> KeyEvent.KEYCODE_MEDIA_PLAY
            "pause" -> KeyEvent.KEYCODE_MEDIA_PAUSE
            "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "previous", "prev" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            "stop" -> KeyEvent.KEYCODE_MEDIA_STOP
            else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        }

        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val upEvent = KeyEvent(KeyEvent.ACTION_UP, keyCode)

        am.dispatchMediaKeyEvent(downEvent)
        am.dispatchMediaKeyEvent(upEvent)
        return Pair(true, "Media command '$command' sent.")
    }

    // 25. CAMERA / GALLERY / DOWNLOADS
    fun openCamera(): Pair<Boolean, String> {
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            Pair(true, "Camera opened.")
        } catch (e: Exception) {
            Pair(false, "Camera open nahi ho paya.")
        }
    }

    fun openGallery(): Pair<Boolean, String> {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            type = "image/*"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            Pair(true, "Gallery opened.")
        } catch (e: Exception) {
            Pair(false, "Gallery open nahi ho payi.")
        }
    }

    // 26. MAPS
    fun openMaps(query: String): Pair<Boolean, String> {
        val uri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(mapIntent)
            Pair(true, "Opening map for '$query'.")
        } catch (e: Exception) {
            // Web fallback
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(webIntent)
                Pair(true, "Opening Google Maps in browser for '$query'.")
            } catch (ex: Exception) {
                Pair(false, "Maps open nahi ho saka.")
            }
        }
    }

    // 27. WEB SEARCH
    fun webSearch(query: String): Pair<Boolean, String> {
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra("query", query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            Pair(true, "Searching web for '$query'.")
        } catch (e: Exception) {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(browserIntent)
                Pair(true, "Opened browser search for '$query'.")
            } catch (ex: Exception) {
                Pair(false, "Search open nahi ho paya.")
            }
        }
    }

    // 28. YOUTUBE SEARCH
    fun searchYouTube(query: String): Pair<Boolean, String> {
        val intent = Intent(Intent.ACTION_SEARCH).apply {
            setPackage("com.google.android.youtube")
            putExtra("query", query)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            Pair(true, "YouTube opened for '$query'.")
        } catch (e: Exception) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(webIntent)
                Pair(true, "YouTube opened in browser for '$query'.")
            } catch (ex: Exception) {
                Pair(false, "YouTube open nahi ho paya.")
            }
        }
    }

    // 19. ALARMS
    fun setAlarm(hour: Int, minutes: Int, message: String): Pair<Boolean, String> {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minutes)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            Pair(true, "Alarm set for $hour:$minutes ($message).")
        } catch (e: Exception) {
            Pair(false, "Alarm app open nahi ho paya: ${e.localizedMessage}")
        }
    }

    // 20. CALENDAR
    fun addCalendarEvent(title: String, beginTimeMs: Long, endTimeMs: Long, description: String): Pair<Boolean, String> {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTimeMs)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTimeMs)
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            Pair(true, "Calendar event compose screen opened for '$title'.")
        } catch (e: Exception) {
            Pair(false, "Calendar open nahi ho paya: ${e.localizedMessage}")
        }
    }
}
