package com.matshelela.childimmunisationtrackingsystemappv1.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MedicalAccentGreenLight,
    onPrimary = DarkBackground,
    primaryContainer = MedicalGreenPrimary,
    onPrimaryContainer = Color.White,
    secondary = MedicalCyanSecondary,
    onSecondary = Color.White,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = TextLight,
    onSurface = TextLight,
    error = Color(0xFFEF4444),
    onError = Color.White,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = Color(0xFF9CA3AF)
)

private val LightColorScheme = lightColorScheme(
    primary = MedicalGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF0F6FC), // Premium light-blue-slate gradient container
    onPrimaryContainer = MedicalGreenDark,
    secondary = MedicalCyanSecondary,
    onSecondary = Color.White,
    background = MedicalSurfaceLight, // F7F9FC
    surface = Color.White,
    onBackground = TextDark,
    onSurface = TextDark,
    error = WarningRed,
    onError = Color.White,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to enforce our high-quality custom clinical styling!
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
