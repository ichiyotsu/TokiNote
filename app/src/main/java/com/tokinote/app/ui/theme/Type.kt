@file:OptIn(ExperimentalTextApi::class)

package com.tokinote.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tokinote.app.R

/**
 * Google Sans Flex (variable), the typeface Tomato uses. Weight is applied through the
 * font's variable axis so the rounded 600/700 cuts look like the reference app.
 */
val GoogleSansFlex = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Normal,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Medium,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.SemiBold,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Bold,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
)

/** 三星 One UI / Samsung Sans（随包提供的拉丁字形，中文由系统字体回退）。 */
val SamsungSans = FontFamily(
    Font(R.font.samsung_sans, weight = FontWeight.Normal),
    Font(R.font.samsung_sans_medium, weight = FontWeight.Medium),
    Font(R.font.samsung_sans_medium, weight = FontWeight.SemiBold),
    Font(R.font.samsung_sans_medium, weight = FontWeight.Bold),
)

/** 左侧抽屉标题使用的展示字体（Google Fonts / Micro 5 Charted）。 */
val Micro5Charted = FontFamily(
    Font(
        resId = R.font.micro5_charted,
        weight = FontWeight.Normal,
        style = FontStyle.Normal,
    ),
)

fun Typography.withFont(family: FontFamily): Typography = copy(
    displayLarge = displayLarge.copy(fontFamily = family),
    displayMedium = displayMedium.copy(fontFamily = family),
    displaySmall = displaySmall.copy(fontFamily = family),
    headlineLarge = headlineLarge.copy(fontFamily = family),
    headlineMedium = headlineMedium.copy(fontFamily = family),
    headlineSmall = headlineSmall.copy(fontFamily = family),
    titleLarge = titleLarge.copy(fontFamily = family),
    titleMedium = titleMedium.copy(fontFamily = family),
    titleSmall = titleSmall.copy(fontFamily = family),
    bodyLarge = bodyLarge.copy(fontFamily = family),
    bodyMedium = bodyMedium.copy(fontFamily = family),
    bodySmall = bodySmall.copy(fontFamily = family),
    labelLarge = labelLarge.copy(fontFamily = family),
    labelMedium = labelMedium.copy(fontFamily = family),
    labelSmall = labelSmall.copy(fontFamily = family),
)

val TokiTypography: Typography = Typography()
    .withFont(GoogleSansFlex)
    .copy(
        displayLarge = Typography().displayLarge.copy(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-1.5).sp,
        ),
        headlineLarge = Typography().headlineLarge.copy(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            letterSpacing = (-0.4).sp,
        ),
        headlineMedium = Typography().headlineMedium.copy(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.3).sp,
        ),
        titleLarge = Typography().titleLarge.copy(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Medium,
        ),
        titleMedium = Typography().titleMedium.copy(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.SemiBold,
        ),
        titleSmall = Typography().titleSmall.copy(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.SemiBold,
        ),
        bodyLarge = Typography().bodyLarge.copy(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Normal,
            lineHeight = 26.sp,
        ),
        labelLarge = Typography().labelLarge.copy(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.SemiBold,
        ),
        labelMedium = Typography().labelMedium.copy(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.SemiBold,
        ),
    )

/**
 * One UI 的排版更接近系统无衬线字体：标题更粗、行高更舒展，
 * 正文使用系统字体而不是 Expressive 的 Google Sans Flex。
 */
val OneUITypography: Typography = Typography()
    .withFont(SamsungSans)
    .copy(
        displayLarge = Typography().displayLarge.copy(
            fontFamily = SamsungSans,
            fontWeight = FontWeight.Bold,
            fontSize = 56.sp,
            lineHeight = 64.sp,
            letterSpacing = (-1.2).sp,
        ),
        headlineLarge = Typography().headlineLarge.copy(
            fontFamily = SamsungSans,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
            lineHeight = 42.sp,
            letterSpacing = (-0.4).sp,
        ),
        headlineMedium = Typography().headlineMedium.copy(
            fontFamily = SamsungSans,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.2).sp,
        ),
        headlineSmall = Typography().headlineSmall.copy(
            fontFamily = SamsungSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
        ),
        titleLarge = Typography().titleLarge.copy(
            fontFamily = SamsungSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
        ),
        titleMedium = Typography().titleMedium.copy(
            fontFamily = SamsungSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            lineHeight = 24.sp,
        ),
        titleSmall = Typography().titleSmall.copy(
            fontFamily = SamsungSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = 21.sp,
        ),
        bodyLarge = Typography().bodyLarge.copy(
            fontFamily = SamsungSans,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
        ),
        bodyMedium = Typography().bodyMedium.copy(
            fontFamily = SamsungSans,
            fontSize = 14.sp,
            lineHeight = 21.sp,
        ),
        labelLarge = Typography().labelLarge.copy(
            fontFamily = SamsungSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        ),
        labelMedium = Typography().labelMedium.copy(
            fontFamily = SamsungSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        ),
    )
