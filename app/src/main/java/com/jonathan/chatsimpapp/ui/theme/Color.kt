package com.jonathan.chatsimpapp.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Paleta de marca: índigo como color principal, neutros ligeramente fríos.
val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Color(0xFF5B5F77),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE1E2F5),
    onSecondaryContainer = Color(0xFF181B2E),
    tertiary = Color(0xFF0F9F6E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCFF5E4),
    onTertiaryContainer = Color(0xFF00210F),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFBFAFF),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFBFAFF),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE4E1EC),
    onSurfaceVariant = Color(0xFF5E5D68),
    outline = Color(0xFF8A8894),
    outlineVariant = Color(0xFFDCD9E4),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F4FC),
    surfaceContainer = Color(0xFFF0EEF7),
    surfaceContainerHigh = Color(0xFFEAE8F2),
    surfaceContainerHighest = Color(0xFFE4E2EC),
    inverseSurface = Color(0xFF303036),
    inverseOnSurface = Color(0xFFF3EFF7),
    inversePrimary = Color(0xFFA5B4FC),
)

val DarkColors = darkColorScheme(
    primary = Color(0xFFA5B4FC),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFFC4C5DD),
    onSecondary = Color(0xFF2D3044),
    secondaryContainer = Color(0xFF3F4257),
    onSecondaryContainer = Color(0xFFE1E2F5),
    tertiary = Color(0xFF5EDBA6),
    onTertiary = Color(0xFF003822),
    tertiaryContainer = Color(0xFF005234),
    onTertiaryContainer = Color(0xFFCFF5E4),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121318),
    onBackground = Color(0xFFE4E1E9),
    surface = Color(0xFF121318),
    onSurface = Color(0xFFE4E1E9),
    surfaceVariant = Color(0xFF47464F),
    onSurfaceVariant = Color(0xFFB4B2BE),
    outline = Color(0xFF918F9A),
    outlineVariant = Color(0xFF34333B),
    surfaceContainerLowest = Color(0xFF0D0E13),
    surfaceContainerLow = Color(0xFF1A1B21),
    surfaceContainer = Color(0xFF1E1F25),
    surfaceContainerHigh = Color(0xFF292A30),
    surfaceContainerHighest = Color(0xFF34343B),
    inverseSurface = Color(0xFFE4E1E9),
    inverseOnSurface = Color(0xFF303036),
    inversePrimary = Color(0xFF4F46E5),
)

/** Tonos para avatares sin foto y para los nombres de los remitentes en grupos. */
val AvatarPalette = listOf(
    Color(0xFF6366F1), Color(0xFF0EA5E9), Color(0xFF10B981), Color(0xFFF59E0B),
    Color(0xFFEF4444), Color(0xFFEC4899), Color(0xFF8B5CF6), Color(0xFF14B8A6),
)

fun colorForKey(key: String): Color = AvatarPalette[(key.hashCode() and Int.MAX_VALUE) % AvatarPalette.size]

val OnlineGreen = Color(0xFF22C55E)
