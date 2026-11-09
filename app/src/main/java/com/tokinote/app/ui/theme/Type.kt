@file:OptIn(ExperimentalTextApi::class)

package com.tokinote.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tokinote.app.R

/**
 * 左侧抽屉标题使用的展示字体（Google Fonts / Micro 5 Charted）。
 * 正文和界面排版不用它，走 [GoogleSansFlex]。
 */
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

