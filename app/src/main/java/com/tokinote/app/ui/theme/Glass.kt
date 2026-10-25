package com.tokinote.app.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

/**
 * Design-system glass recipe. Callers only choose radius and tint; the
 * Haze wiring and fallback stay in one place.
 */
fun Modifier.tokiGlass(
    hazeState: HazeState,
    blurRadius: Dp,
    tints: List<Color>,
    fallback: Color,
    noise: Float = 0.02f,
): Modifier = hazeBlur(
    input = HazeInput.Sources(hazeState),
    style = HazeBlurStyle {
        blurRadius(blurRadius)
        noiseFactor(noise)
        colorEffects(tints.map { tint -> HazeColorEffect.tint(tint) })
        fallbackColorEffect(HazeColorEffect.tint(fallback))
    },
)
