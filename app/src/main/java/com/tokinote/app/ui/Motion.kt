package com.tokinote.app.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * 统一的动效开关。MainActivity 依据「减弱动效」设置提供值，
 * 所有转场、按压反馈都读这里，关掉后直接落到终态。
 */
val LocalMotionEnabled = staticCompositionLocalOf { true }

/** MD3E 的几档动效参数：空间位移用带一点回弹的弹簧，颜色/透明度用短 tween。 */
object TokiMotion {
    val spatial = spring<Float>(dampingRatio = 0.82f, stiffness = 380f)
    val emphasized = spring<Float>(dampingRatio = 0.62f, stiffness = 420f)
    val overshoot = spring<Float>(dampingRatio = 0.5f, stiffness = 460f)

    /** Material 3 emphasized decelerate，用于展开 / 悬浮窗。 */
    val emphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    const val quick = 140
    const val standard = 220
    const val expressive = 340
    const val slow = 520
}

/**
 * 按压缩放反馈。传入与 clickable 共用的 [InteractionSource]，
 * 按下时轻微缩小，松开回弹，配合 MD3E 的「有生命的容器」手感。
 */
@Composable
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.96f,
): Modifier {
    val enabled = LocalMotionEnabled.current
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && pressed) pressedScale else 1f,
        animationSpec = TokiMotion.spatial,
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
