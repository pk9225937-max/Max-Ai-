package com.example.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ai.LiveSessionManager
import com.example.presentation.screens.DebugScreen
import com.example.presentation.screens.MainAssistantScreen
import com.example.presentation.screens.OnboardingScreen
import com.example.presentation.screens.PermissionsScreen
import com.example.presentation.screens.PrivacyScreen
import com.example.presentation.screens.SettingsScreen

object NavRoutes {
    const val MAIN = "main"
    const val SETTINGS = "settings"
    const val PERMISSIONS = "permissions"
    const val PRIVACY = "privacy"
    const val DEBUG = "debug"
    const val ONBOARDING = "onboarding"
}

@Composable
fun AssistantNavGraph(
    sessionManager: LiveSessionManager,
    isFirstLaunch: Boolean,
    navController: NavHostController = rememberNavController()
) {
    val startDestination = if (isFirstLaunch) NavRoutes.ONBOARDING else NavRoutes.MAIN

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavRoutes.MAIN) {
            MainAssistantScreen(
                sessionManager = sessionManager,
                onNavigateToSettings = { navController.navigate(NavRoutes.SETTINGS) },
                onNavigateToPermissions = { navController.navigate(NavRoutes.PERMISSIONS) },
                onNavigateToPrivacy = { navController.navigate(NavRoutes.PRIVACY) },
                onNavigateToDebug = { navController.navigate(NavRoutes.DEBUG) }
            )
        }

        composable(NavRoutes.ONBOARDING) {
            OnboardingScreen(
                permissionManager = sessionManager.permissionManager,
                onComplete = {
                    navController.navigate(NavRoutes.MAIN) {
                        popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                sessionManager = sessionManager,
                onBack = { navController.popBackStack() },
                onNavigateToPermissions = { navController.navigate(NavRoutes.PERMISSIONS) },
                onNavigateToPrivacy = { navController.navigate(NavRoutes.PRIVACY) }
            )
        }

        composable(NavRoutes.PERMISSIONS) {
            PermissionsScreen(
                permissionManager = sessionManager.permissionManager,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.PRIVACY) {
            PrivacyScreen(
                sessionManager = sessionManager,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.DEBUG) {
            DebugScreen(
                sessionManager = sessionManager,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
