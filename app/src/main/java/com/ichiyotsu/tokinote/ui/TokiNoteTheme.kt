package com.ichiyotsu.tokinote.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val Violet = Color(0xFF7157D6)
private val VioletDeep = Color(0xFF5540B5)
private val WarmPaper = Color(0xFFF6F3EF)
private val Ink = Color(0xFF28252E)
private val MutedInk = Color(0xFF6F6976)
private val LilacWash = Color(0xFFECE6FF)

private val LightScheme = lightColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = LilacWash,
    onPrimaryContainer = Color(0xFF30215C),
    secondary = Color(0xFF547E70),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7EEE3),
    onSecondaryContainer = Color(0xFF18392F),
    tertiary = Color(0xFFB06D54),
    onTertiary = Color.White,
    background = WarmPaper,
    onBackground = Ink,
    surface = Color(0xFFFFFCF9),
    onSurface = Ink,
    surfaceVariant = Color(0xFFF0ECF3),
    onSurfaceVariant = MutedInk,
    outline = Color(0xFFD8D2DE),
    outlineVariant = Color(0xFFE9E4EC),
    error = Color(0xFFB3261E)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFC2B2FF),
    onPrimary = Color(0xFF362473),
    primaryContainer = Color(0xFF4C3B88),
    onPrimaryContainer = Color(0xFFE9DFFF),
    secondary = Color(0xFFA7D0BE),
    onSecondary = Color(0xFF18382D),
    secondaryContainer = Color(0xFF315447),
    onSecondaryContainer = Color(0xFFC3EBD8),
    tertiary = Color(0xFFFFB69A),
    onTertiary = Color(0xFF57200F),
    background = Color(0xFF19171E),
    onBackground = Color(0xFFEAE6EF),
    surface = Color(0xFF24212A),
    onSurface = Color(0xFFEAE6EF),
    surfaceVariant = Color(0xFF34303B),
    onSurfaceVariant = Color(0xFFC7C0CF),
    outline = Color(0xFF8F8798),
    outlineVariant = Color(0xFF49434F),
    error = Color(0xFFFFB4AB)
)

val NoteTones = listOf(
    Color(0xFFEAE3FF),
    Color(0xFFF6DDD2),
    Color(0xFFD9EEE4),
    Color(0xFFF5E7B5),
    Color(0xFFDCEAF5)
)

val NoteTonesDark = listOf(
    Color(0xFF463A5F),
    Color(0xFF5D4039),
    Color(0xFF2F5147),
    Color(0xFF514A31),
    Color(0xFF34495A)
)

val NoteAccents = listOf(
    Color(0xFF9C82E9),
    Color(0xFFD99076),
    Color(0xFF6DA88F),
    Color(0xFFB5993E),
    Color(0xFF6F9ABD)
)

fun noteTone(index: Int, dark: Boolean): Color =
    (if (dark) NoteTonesDark else NoteTones)[index.coerceIn(0, 4)]

@Composable
fun TokiNoteTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(10.dp),
        small = RoundedCornerShape(14.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(27.dp),
        extraLarge = RoundedCornerShape(34.dp)
    )
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        shapes = shapes,
        content = content
    )
}
