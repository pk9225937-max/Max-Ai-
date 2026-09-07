package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ai.LiveSessionManager
import com.example.presentation.navigation.AssistantNavGraph
import com.example.ui.theme.DarkSpaceBg
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var sessionManager: LiveSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sessionManager = LiveSessionManager(applicationContext)

        val prefs = getSharedPreferences("max_assistant_prefs", Context.MODE_PRIVATE)
        val isFirstLaunch = !sessionManager.permissionManager.hasRecordAudio() && prefs.getBoolean("is_first_launch", true)
        if (isFirstLaunch) {
            prefs.edit().putBoolean("is_first_launch", false).apply()
        }

        setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkSpaceBg
                ) {
                    AssistantNavGraph(
                        sessionManager = sessionManager,
                        isFirstLaunch = isFirstLaunch
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sessionManager.permissionManager.refreshPermissions()
    }

    override fun onDestroy() {
        sessionManager.release()
        super.onDestroy()
    }
}

