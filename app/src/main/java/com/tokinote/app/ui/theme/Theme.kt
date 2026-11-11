package com.tokinote.app.ui.theme

import android.os.Build
import android.graphics.Typeface
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.tokinote.app.data.AppFont
import com.tokinote.app.data.ColorPreset
import java.io.File

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
val LocalCardShape = staticCompositionLocalOf<Shape> { ExpressiveCardShape }
val LocalChipShape = staticCompositionLocalOf<Shape> { ExpressiveChipShape }
val LocalFabShape = staticCompositionLocalOf<Shape> { ExpressiveFabShape }
val LocalIslandShape = staticCompositionLocalOf<Shape> { ExpressiveIslandShape }
val LocalSheetShape = staticCompositionLocalOf<Shape> { ExpressiveSheetShape }

private fun ColorScheme.trueBlack(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF121212),
    surfaceContainer = Color(0xFF171915),
    surfaceContainerHigh = Color(0xFF1D1F1A),
    surfaceContainerHighest = Color(0xFF24271F),
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TokiNoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    font: AppFont = AppFont.Auto,
    customFontPath: String = "",
    dynamicColor: Boolean = false,
    amoled: Boolean = false,
    colorPreset: ColorPreset = ColorPreset.Matcha,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val base = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> darkScheme
        else -> lightScheme
    }
    val presetBase = if (dynamicColor) base else base.withPreset(colorPreset, darkTheme)
    val seed = if (dynamicColor) Color.Transparent else presetBase.primary
    val preset = if (seed == Color.Transparent) {
        presetBase
    } else {
        val blend: (Color, Float) -> Color = { color, amount -> androidx.compose.ui.graphics.lerp(color, seed, amount) }
        presetBase.copy(
            background = blend(presetBase.background, 0.04f),
            surface = blend(presetBase.surface, 0.04f),
            surfaceContainerLowest = blend(presetBase.surfaceContainerLowest, 0.03f),
            surfaceContainerLow = blend(presetBase.surfaceContainerLow, 0.06f),
            surfaceContainer = blend(presetBase.surfaceContainer, 0.08f),
            surfaceContainerHigh = blend(presetBase.surfaceContainerHigh, 0.10f),
            surfaceContainerHighest = blend(presetBase.surfaceContainerHighest, 0.13f),
        )
    }
    val scheme = if (amoled && darkTheme) preset.trueBlack() else preset
    val typography = TokiTypography.withFont(resolveFontFamily(context, font, customFontPath))

    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalCardShape provides ExpressiveCardShape,
        LocalChipShape provides ExpressiveChipShape,
        LocalFabShape provides ExpressiveFabShape,
        LocalIslandShape provides ExpressiveIslandShape,
        LocalSheetShape provides ExpressiveSheetShape,
    ) {
        MaterialExpressiveTheme(
            colorScheme = scheme,
            motionScheme = if (reduceMotion) MotionScheme.standard() else MotionScheme.expressive(),
            typography = typography,
            shapes = TokiShapes,
            content = content,
        )
    }
}

/** 字体预设解析；自定义字体从应用私有目录读取，失败时回退到 Google Sans。 */
private fun resolveFontFamily(
    context: android.content.Context,
    font: AppFont,
    customFontPath: String,
): FontFamily {
    val fallback = GoogleSansFlex
    return when (font) {
        AppFont.Auto -> fallback
        AppFont.Samsung -> SamsungSans
        AppFont.Md3e -> GoogleSansFlex
        AppFont.PingFang -> systemFontFamily("PingFang SC")
        AppFont.Custom -> runCatching {
            val file = File(customFontPath)
            if (file.isFile && file.length() > 0) FontFamily(Font(file)) else fallback
        }.getOrDefault(fallback)
    }
}

private fun systemFontFamily(name: String): FontFamily =
    FontFamily(Typeface.create(name, Typeface.NORMAL))
