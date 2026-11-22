package com.kitchentimer.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val WarmOrange = Color(0xFFE85D04)
val DeepAmber = Color(0xFFF48C06)
val AlarmRed = Color(0xFFD90429)
val SuccessGreen = Color(0xFF2B9348)

private val LightColors = lightColorScheme(
    primary = WarmOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE5D4),
    onPrimaryContainer = Color(0xFF4A1900),
    secondary = Color(0xFF5C4033),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E4DC),
    onSecondaryContainer = Color(0xFF2B1A12),
    background = Color(0xFFFAF7F4),
    onBackground = Color(0xFF1F1A17),
    surface = Color.White,
    onSurface = Color(0xFF1F1A17),
    surfaceVariant = Color(0xFFF2EAE4),
    onSurfaceVariant = Color(0xFF574B43),
    error = AlarmRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF9B54),
    onPrimary = Color(0xFF4A1900),
    primaryContainer = Color(0xFF6B2800),
    onPrimaryContainer = Color(0xFFFFDBC7),
    secondary = Color(0xFFD8C2B8),
    onSecondary = Color(0xFF3C2B22),
    background = Color(0xFF171311),
    onBackground = Color(0xFFF2ECE8),
    surface = Color(0xFF221C19),
    onSurface = Color(0xFFF2ECE8),
    surfaceVariant = Color(0xFF332A25),
    onSurfaceVariant = Color(0xFFD0C2B9),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF5C000A),
    errorContainer = Color(0xFF8C001A),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun KitchenTimerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
