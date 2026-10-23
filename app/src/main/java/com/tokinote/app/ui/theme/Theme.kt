package com.tokinote.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.tokinote.app.data.ColorPreset

val lightScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

val darkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

val LocalDarkTheme = staticCompositionLocalOf { false }

private fun ColorScheme.trueBlack(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0C07),
    surfaceContainer = Color(0xFF101208),
    surfaceContainerHigh = Color(0xFF171A11),
    surfaceContainerHighest = Color(0xFF1F2317),
)

private fun ColorScheme.withPreset(preset: ColorPreset, dark: Boolean): ColorScheme = when (preset) {
    ColorPreset.Matcha -> this
    ColorPreset.Blue -> if (dark) {
        copy(
            primary = Color(0xFFA6C8FF),
            onPrimary = Color(0xFF002F52),
            primaryContainer = Color(0xFF1E3F63),
            onPrimaryContainer = Color(0xFFCFE4FF),
            tertiary = Color(0xFF9FCBFF),
        )
    } else {
        copy(
            primary = Color(0xFF2E5D8C),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFCFE4FF),
            onPrimaryContainer = Color(0xFF0B2E4F),
            tertiary = Color(0xFF3A6491),
        )
    }

    ColorPreset.Purple -> if (dark) {
        copy(
            primary = Color(0xFFD5BBFF),
            onPrimary = Color(0xFF2F1A4A),
            primaryContainer = Color(0xFF4A3168),
            onPrimaryContainer = Color(0xFFEADDFF),
            tertiary = Color(0xFFE3B7FF),
        )
    } else {
        copy(
            primary = Color(0xFF6A4C93),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFEADDFF),
            onPrimaryContainer = Color(0xFF3A2552),
            tertiary = Color(0xFF7B4E9E),
        )
    }

    ColorPreset.Orange -> if (dark) {
        copy(
            primary = Color(0xFFFFB870),
            onPrimary = Color(0xFF4A2A00),
            primaryContainer = Color(0xFF663D0E),
            onPrimaryContainer = Color(0xFFFFDCC2),
            tertiary = Color(0xFFFFB4A0),
        )
    } else {
        copy(
            primary = Color(0xFF9A5B12),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFDCC2),
            onPrimaryContainer = Color(0xFF4A2A00),
            tertiary = Color(0xFFA8452E),
        )
    }
}

@Composable
fun TokiNoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    amoled: Boolean = false,
    colorPreset: ColorPreset = ColorPreset.Matcha,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val base = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> darkScheme
        else -> lightScheme
    }
    val preset = if (dynamicColor) base else base.withPreset(colorPreset, darkTheme)
    val scheme = if (amoled && darkTheme) preset.trueBlack() else preset

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = scheme,
            typography = TokiTypography,
            shapes = TokiShapes,
            content = content,
        )
    }
}
