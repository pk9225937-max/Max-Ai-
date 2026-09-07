package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004E58),
    onPrimaryContainer = Color(0xFF9CF0FF),
    secondary = ElectricViolet,
    onSecondary = Color(0xFF230062),
    secondaryContainer = Color(0xFF3B187E),
    onSecondaryContainer = Color(0xFFE2DFFF),
    tertiary = PulseMagenta,
    onTertiary = Color(0xFF5D0026),
    background = DarkSpaceBg,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = AlertRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

