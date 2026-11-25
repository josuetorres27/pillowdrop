package com.josuetorres.pillowdrop.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NightBackground = Color(0xFF090C14)
val NightSurface = Color(0xFF121726)
val NightSurfaceVariant = Color(0xFF1B2236)
val LavenderPrimary = Color(0xFFB4C5FF)
val OnLavenderPrimary = Color(0xFF102254)
val PrimaryContainerDark = Color(0xFF25386B)
val SecondaryTeal = Color(0xFF8BD6C8)
val MintSuccess = Color(0xFF66BB6A)
val AmberWarning = Color(0xFFFFB74D)
val CoralDanger = Color(0xFFEF5350)

private val PillowDropColorScheme = darkColorScheme(
    primary = LavenderPrimary,
    onPrimary = OnLavenderPrimary,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = Color(0xFFDBE2FF),
    secondary = SecondaryTeal,
    onSecondary = Color(0xFF003730),
    background = NightBackground,
    onBackground = Color(0xFFE3E6F0),
    surface = NightSurface,
    onSurface = Color(0xFFE3E6F0),
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = Color(0xFFB8C0D4),
    error = CoralDanger,
    onError = Color(0xFF3B0807)
)

@Composable
fun PillowDropTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PillowDropColorScheme,
        content = content
    )
}
