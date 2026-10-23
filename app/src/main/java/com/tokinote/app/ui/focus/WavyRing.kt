package com.tokinote.app.ui.focus

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 「波浪断点」计时环（docs/design-refs/03-wavy-ring.png）：
 * 轨道是顺时针一整圈平滑细线，进度则画成一条带正弦起伏的粗线，
 * 从 12 点方向开始，波峰沿圆周前进。
 */
@Composable
fun WavyRing(
    progress: Float,
    accent: Color,
    track: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 16.dp,
    amplitude: Dp = 4.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val radius = size.minDimension / 2f - stroke / 2f
            drawCircle(
                color = track,
                radius = radius,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            val sweep = 360f * progress.coerceIn(0f, 1f)
            if (sweep > 0.5f) {
                val path = Path()
                val steps = (sweep / 2.5f).toInt().coerceAtLeast(24)
                val amp = amplitude.toPx()
                val waveLength = 26f

                for (index in 0..steps) {
                    val t = index / steps.toFloat()
                    val angleDeg = -90f + sweep * t
                    val angle = Math.toRadians(angleDeg.toDouble())
                    // 两端收窄波幅，让线头落在轨道上，不会突兀。
                    val taper = minOf(1f, t * 12f, (1f - t) * 12f)
                    val wave = sin(angleDeg / waveLength * 2.0 * PI).toFloat() * amp * taper
                    val r = radius + wave
                    val x = center.x + cos(angle).toFloat() * r
                    val y = center.y + sin(angle).toFloat() * r
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = accent,
                    style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
        content()
    }
}
