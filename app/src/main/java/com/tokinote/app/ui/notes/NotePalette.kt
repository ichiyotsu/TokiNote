package com.tokinote.app.ui.notes

import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toPixelMap
import com.tokinote.app.ui.theme.LocalDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/** 从笔记首图提取出的页面色调；没有图片时退回到主题色。 */
data class NotePalette(
    val accent: Color,
    val onAccent: Color,
    val scrim: Color,
    val toolbar: Color,
    val title: Color,
)

/**
 * 采样首图里饱和度较高的一批像素，作为编辑页的主色。
 * 页面背景只混入很少的比例，保持深色可读性；标题和工具岛会明显一些。
 */
@Composable
fun rememberNotePalette(
    path: String?,
    fallbackAccent: Color,
    surface: Color,
    onSurface: Color,
    toolbarBase: Color,
): NotePalette {
    val dark = LocalDarkTheme.current
    val initial = NotePalette(
        accent = fallbackAccent,
        onAccent = onAccentFor(fallbackAccent),
        scrim = surface,
        toolbar = toolbarBase,
        title = onSurface,
    )
    val palette by produceState(
        initialValue = initial,
        path,
        surface,
        onSurface,
        toolbarBase,
        fallbackAccent,
        dark,
    ) {
        value = withContext(Dispatchers.IO) {
            extractPalette(path, fallbackAccent, surface, onSurface, toolbarBase, dark)
        }
    }
    return palette
}

private fun extractPalette(
    path: String?,
    fallbackAccent: Color,
    surface: Color,
    onSurface: Color,
    toolbarBase: Color,
    dark: Boolean,
): NotePalette {
    if (path.isNullOrBlank()) {
        return NotePalette(
            accent = fallbackAccent,
            onAccent = onAccentFor(fallbackAccent),
            scrim = surface,
            toolbar = toolbarBase,
            title = onSurface,
        )
    }

    val options = BitmapFactory.Options().apply { inSampleSize = 8 }
    val bitmap = runCatching { BitmapFactory.decodeFile(path, options) }.getOrNull()
        ?: return NotePalette(
            accent = fallbackAccent,
            onAccent = onAccentFor(fallbackAccent),
            scrim = surface,
            toolbar = toolbarBase,
            title = onSurface,
        )

    val pixels = bitmap.asImageBitmap().toPixelMap()
    val stepX = max(1, pixels.width / 48)
    val stepY = max(1, pixels.height / 48)

    var r = 0f
    var g = 0f
    var b = 0f
    var weightSum = 0f
    var y = 0
    while (y < pixels.height) {
        var x = 0
        while (x < pixels.width) {
            val color = pixels[x, y]
            val maximum = max(color.red, max(color.green, color.blue))
            val minimum = minOf(color.red, color.green, color.blue)
            val saturation = if (maximum <= 0.01f) 0f else (maximum - minimum) / maximum
            val luminance = luminance(color)
            if (saturation >= 0.12f && luminance in 0.07f..0.93f) {
                val weight = saturation * saturation
                r += color.red * weight
                g += color.green * weight
                b += color.blue * weight
                weightSum += weight
            }
            x += stepX
        }
        y += stepY
    }
    bitmap.recycle()

    if (weightSum <= 0f) {
        return NotePalette(
            accent = fallbackAccent,
            onAccent = onAccentFor(fallbackAccent),
            scrim = surface,
            toolbar = toolbarBase,
            title = onSurface,
        )
    }

    val hsv = FloatArray(3)
    AndroidColor.RGBToHSV(
        (r / weightSum * 255f).toInt().coerceIn(0, 255),
        (g / weightSum * 255f).toInt().coerceIn(0, 255),
        (b / weightSum * 255f).toInt().coerceIn(0, 255),
        hsv,
    )
    hsv[1] = (hsv[1] * 1.25f).coerceIn(0f, 1f)
    hsv[2] = hsv[2].coerceIn(0.58f, 0.84f)
    val accent = Color(AndroidColor.HSVToColor(hsv))

    return NotePalette(
        accent = accent,
        onAccent = onAccentFor(accent),
        scrim = lerp(surface, accent, if (dark) 0.13f else 0.08f),
        toolbar = lerp(toolbarBase, accent, if (dark) 0.26f else 0.20f),
        title = lerp(onSurface, accent, if (dark) 0.45f else 0.32f),
    )
}

private fun luminance(color: Color): Float =
    0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue

private fun onAccentFor(accent: Color): Color =
    if (luminance(accent) > 0.52f) Color(0xFF20160A) else Color.White
