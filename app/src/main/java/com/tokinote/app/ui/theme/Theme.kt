package com.tokinote.app.ui.theme

import android.os.Build
import android.graphics.Typeface
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.tokinote.app.data.AppFont
import com.tokinote.app.data.ColorPreset
import com.tokinote.app.data.DesignStyle
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
val LocalDesignStyle = staticCompositionLocalOf { DesignStyle.MaterialExpressive }
val LocalCardShape = staticCompositionLocalOf<Shape> { ExpressiveCardShape }
val LocalChipShape = staticCompositionLocalOf<Shape> { ExpressiveChipShape }
val LocalFabShape = staticCompositionLocalOf<Shape> { ExpressiveFabShape }
val LocalIslandShape = staticCompositionLocalOf<Shape> { ExpressiveIslandShape }
val LocalSheetShape = staticCompositionLocalOf<Shape> { ExpressiveSheetShape }

/** One UI 的中性底色与 Samsung 蓝强调色。 */
val oneUiLightScheme = lightColorScheme(
    primary = Color(0xFF0381FE),
    onPrimary = Color(0xFFFAFAFA),
    primaryContainer = Color(0xFFD6E9FF),
    onPrimaryContainer = Color(0xFF001B33),
    secondary = Color(0xFF0072DE),
    onSecondary = Color(0xFFFAFAFA),
    secondaryContainer = Color(0xFFE1E8EF),
    onSecondaryContainer = Color(0xFF111A22),
    tertiary = Color(0xFF3E91FF),
    onTertiary = Color(0xFFFAFAFA),
    tertiaryContainer = Color(0xFFC9E9FF),
    onTertiaryContainer = Color(0xFF001E2D),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF2F2F2),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFF2F2F2),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFE8E8E8),
    onSurfaceVariant = Color(0xFF656565),
    outline = Color(0xFF8A8A8A),
    outlineVariant = Color(0xFFDADADA),
    scrim = Color.Black,
    inverseSurface = Color(0xFF2E2E2E),
    inverseOnSurface = Color(0xFFFAFAFA),
    inversePrimary = Color(0xFF3E91FF),
    surfaceDim = Color(0xFFE2E2E2),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF2F2F2),
    surfaceContainerHigh = Color(0xFFE8E8E8),
    surfaceContainerHighest = Color(0xFFE0E0E0),
)

val oneUiDarkScheme = darkColorScheme(
    primary = Color(0xFF0381FE),
    onPrimary = Color(0xFFFAFAFA),
    primaryContainer = Color(0xFF00345C),
    onPrimaryContainer = Color(0xFFD6E9FF),
    secondary = Color(0xFF3E91FF),
    onSecondary = Color(0xFFFAFAFA),
    secondaryContainer = Color(0xFF34414D),
    onSecondaryContainer = Color(0xFFD4E4F5),
    tertiary = Color(0xFF3E91FF),
    onTertiary = Color(0xFFFAFAFA),
    tertiaryContainer = Color(0xFF004D69),
    onTertiaryContainer = Color(0xFFC9E9FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF080808),
    onBackground = Color(0xFFFAFAFA),
    surface = Color(0xFF080808),
    onSurface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFA8A8A8),
    outline = Color(0xFF8A8A8A),
    outlineVariant = Color(0xFF3A3A3A),
    scrim = Color.Black,
    inverseSurface = Color(0xFFE8E8E8),
    inverseOnSurface = Color(0xFF2E2E2E),
    inversePrimary = Color(0xFF0381FE),
    surfaceDim = Color(0xFF111111),
    surfaceBright = Color(0xFF3A3A3A),
    surfaceContainerLowest = Color(0xFF080808),
    surfaceContainerLow = Color(0xFF121212),
    surfaceContainer = Color(0xFF1A1A1A),
    surfaceContainerHigh = Color(0xFF242424),
    surfaceContainerHighest = Color(0xFF2E2E2E),
)

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

@Composable
fun TokiNoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    designStyle: DesignStyle = DesignStyle.MaterialExpressive,
    font: AppFont = AppFont.Auto,
    customFontPath: String = "",
    dynamicColor: Boolean = false,
    amoled: Boolean = false,
    colorPreset: ColorPreset = ColorPreset.Matcha,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val oneUi = designStyle == DesignStyle.OneUI
    val base = when {
        oneUi -> if (darkTheme) oneUiDarkScheme else oneUiLightScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> darkScheme
        else -> lightScheme
    }
    val presetBase = when {
        oneUi && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val dynamic = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            base.copy(
                primary = dynamic.primary,
                onPrimary = dynamic.onPrimary,
                primaryContainer = dynamic.primaryContainer,
                onPrimaryContainer = dynamic.onPrimaryContainer,
            )
        }

        oneUi -> base.withPreset(colorPreset, darkTheme)
        dynamicColor -> base
        else -> base.withPreset(colorPreset, darkTheme)
    }
    val seed = if (dynamicColor || oneUi) {
        Color.Transparent
    } else {
        presetBase.primary
    }
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
    val scheme = when {
        amoled && darkTheme && oneUi -> preset.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF0A0A0A),
            surfaceContainer = Color(0xFF121212),
            surfaceContainerHigh = Color(0xFF1A1A1A),
            surfaceContainerHighest = Color(0xFF222222),
        )

        amoled && darkTheme -> preset.trueBlack()
        else -> preset
    }
    val baseTypography = if (oneUi) OneUITypography else TokiTypography
    val typography = baseTypography.withFont(
        resolveFontFamily(context, font, customFontPath, oneUi),
    )

    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalDesignStyle provides designStyle,
        LocalCardShape provides if (oneUi) OneUICardShape else ExpressiveCardShape,
        LocalChipShape provides if (oneUi) OneUIChipShape else ExpressiveChipShape,
        LocalFabShape provides if (oneUi) OneUIFabShape else ExpressiveFabShape,
        LocalIslandShape provides if (oneUi) OneUIIslandShape else ExpressiveIslandShape,
        LocalSheetShape provides if (oneUi) OneUISheetShape else ExpressiveSheetShape,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typography,
            shapes = if (oneUi) OneUIShapes else TokiShapes,
            content = content,
        )
    }
}

/** 字体预设解析；自定义字体从应用私有目录读取，失败时回退到当前风格默认字体。 */
private fun resolveFontFamily(
    context: android.content.Context,
    font: AppFont,
    customFontPath: String,
    oneUi: Boolean,
): FontFamily {
    val fallback = if (oneUi) SamsungSans else GoogleSansFlex
    return when (font) {
        AppFont.Auto -> fallback
        AppFont.Roboto -> FontFamily.SansSerif
        AppFont.Samsung -> SamsungSans
        AppFont.Md3e -> GoogleSansFlex
        AppFont.PingFang -> systemFontFamily("PingFang SC")
        AppFont.MiSans -> systemFontFamily("MiSans")
        AppFont.OppoSans -> systemFontFamily("OPPO Sans")
        AppFont.Custom -> runCatching {
            val file = File(customFontPath)
            if (file.isFile && file.length() > 0) FontFamily(Font(file)) else fallback
        }.getOrDefault(fallback)
    }
}

private fun systemFontFamily(name: String): FontFamily =
    FontFamily(Typeface.create(name, Typeface.NORMAL))
