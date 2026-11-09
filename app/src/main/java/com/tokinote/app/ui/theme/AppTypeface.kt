package com.tokinote.app.ui.theme

import android.content.Context
import android.graphics.Typeface
import android.graphics.fonts.Font as PlatformFont
import android.graphics.fonts.FontFamily as PlatformFamily
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.tokinote.app.R

/**
 * Google Sans Flex 负责拉丁字母和数字，思源黑体（Noto Sans SC，同一套设计）负责中文。
 * 子集里没有的字再交给系统 sans-serif，中文设备上那一层通常仍是思源黑体。
 */
val GoogleSansFlex: FontFamily = FontFamily(
    StackedFont(FontWeight.Normal, StackKind.GoogleSans),
    StackedFont(FontWeight.Medium, StackKind.GoogleSans),
    StackedFont(FontWeight.SemiBold, StackKind.GoogleSans),
    StackedFont(FontWeight.Bold, StackKind.GoogleSans),
)

/** 可选的 Samsung Sans。拉丁用内置字，中文走系统字体。 */
val SamsungSans: FontFamily = FontFamily(
    StackedFont(FontWeight.Normal, StackKind.Samsung),
    StackedFont(FontWeight.Medium, StackKind.Samsung),
    StackedFont(FontWeight.SemiBold, StackKind.Samsung),
    StackedFont(FontWeight.Bold, StackKind.Samsung),
)

private enum class StackKind { GoogleSans, Samsung }

private class StackedFont(
    override val weight: FontWeight,
    private val kind: StackKind,
) : AndroidFont(
    loadingStrategy = FontLoadingStrategy.Blocking,
    typefaceLoader = StackedFontLoader,
    variationSettings = FontVariation.Settings(),
) {
    override val style: FontStyle = FontStyle.Normal

    fun load(context: Context): Typeface = when (kind) {
        StackKind.GoogleSans -> googleSansWithSourceHan(context, weight.weight)
        StackKind.Samsung -> samsungWithSystemFallback(context, weight.weight)
    }
}

private object StackedFontLoader : AndroidFont.TypefaceLoader {
    override fun loadBlocking(context: Context, font: AndroidFont): Typeface =
        (font as StackedFont).load(context)

    override suspend fun awaitLoad(context: Context, font: AndroidFont): Typeface =
        loadBlocking(context, font)
}

private val typefaceCache = HashMap<String, Typeface>()

private fun googleSansWithSourceHan(context: Context, weight: Int): Typeface {
    val key = "gs-$weight"
    synchronized(typefaceCache) {
        typefaceCache[key]?.let { return it }
        val resources = context.applicationContext.resources
        val latin = variableFont(resources, R.font.google_sans_flex, weight)
        val han = variableFont(resources, R.font.noto_sans_sc, weight)
        val typeface = Typeface.CustomFallbackBuilder(PlatformFamily.Builder(latin).build())
            .addCustomFallback(PlatformFamily.Builder(han).build())
            .setSystemFallback("sans-serif")
            .build()
        typefaceCache[key] = typeface
        return typeface
    }
}

private fun samsungWithSystemFallback(context: Context, weight: Int): Typeface {
    val key = "ss-$weight"
    synchronized(typefaceCache) {
        typefaceCache[key]?.let { return it }
        val res = if (weight >= 500) R.font.samsung_sans_medium else R.font.samsung_sans
        val font = PlatformFont.Builder(context.applicationContext.resources, res)
            .setWeight(weight)
            .build()
        val typeface = Typeface.CustomFallbackBuilder(PlatformFamily.Builder(font).build())
            .setSystemFallback("sans-serif")
            .build()
        typefaceCache[key] = typeface
        return typeface
    }
}

private fun variableFont(resources: android.content.res.Resources, resId: Int, weight: Int): PlatformFont =
    PlatformFont.Builder(resources, resId)
        .setWeight(weight)
        .setFontVariationSettings("'wght' $weight")
        .build()
