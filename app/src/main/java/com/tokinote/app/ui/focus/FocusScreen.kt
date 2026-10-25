package com.tokinote.app.ui.focus

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokinote.app.R
import com.tokinote.app.ui.LocalMotionEnabled
import com.tokinote.app.ui.components.GlassIconButton
import com.tokinote.app.ui.formatClock
import com.tokinote.app.ui.pressScale

@Composable
fun FocusScreen(
    state: FocusState,
    contentPadding: PaddingValues,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    onOpenTimerSettings: () -> Unit,
    onStartIndefinite: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val motion = LocalMotionEnabled.current
    val focusMode = state.mode == FocusMode.Focus

    val accent by animateColorAsState(
        targetValue = if (focusMode) colors.primary else colors.tertiary,
        animationSpec = tween(520),
        label = "focusAccent",
    )
    val track by animateColorAsState(
        targetValue = colors.surfaceContainerHighest.copy(alpha = 0.55f),
        animationSpec = tween(520),
        label = "focusTrack",
    )
    val onAccent by animateColorAsState(
        targetValue = if (focusMode) colors.onPrimary else colors.onTertiary,
        animationSpec = tween(520),
        label = "focusOnAccent",
    )
    val secondaryContainer by animateColorAsState(
        targetValue = colors.surfaceContainerHigh.copy(alpha = 0.72f),
        animationSpec = tween(520),
        label = "focusSecondary",
    )

    var animatingOut by remember { mutableStateOf(false) }
    val timeScale by animateFloatAsState(
        targetValue = if (animatingOut) 2.4f else 1f,
        animationSpec = tween(420),
        label = "timeScale",
    )
    val timeAlpha by animateFloatAsState(
        targetValue = if (animatingOut) 0f else 1f,
        animationSpec = tween(420),
        label = "timeAlpha",
    )
    LaunchedEffect(animatingOut) {
        if (animatingOut) {
            kotlinx.coroutines.delay(430)
            onStartIndefinite()
            animatingOut = false
        }
    }

    val progress by animateFloatAsState(
        targetValue = state.progress,
        animationSpec = tween(if (motion) 900 else 0),
        label = "ringProgress",
    )
    val displaySeconds = if (state.indefinite) state.elapsedSeconds else state.remainingSeconds

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .widthIn(max = 390.dp)
                    .aspectRatio(1f)
                    .pointerInput(state.mode, state.running, state.indefinite) {
                        if (!state.running && !state.indefinite && state.mode == FocusMode.Focus) {
                            detectTapGestures(
                                onDoubleTap = { if (!animatingOut) animatingOut = true },
                                onLongPress = { if (!animatingOut) animatingOut = true },
                            )
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                TimerRing(
                    progress = if (state.indefinite) 0f else progress,
                    accent = accent,
                    track = track,
                    strokeWidth = 18.dp,
                    modifier = Modifier.fillMaxSize(),
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer {
                        scaleX = timeScale
                        scaleY = timeScale
                        alpha = timeAlpha
                    },
                ) {
                    Text(
                        text = if (state.indefinite) {
                            stringResource(R.string.focus_indefinite)
                        } else {
                            stringResource(state.mode.labelRes)
                        },
                        style = MaterialTheme.typography.labelLarge.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = accent,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = formatClock(displaySeconds),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 78.sp,
                            lineHeight = 82.sp,
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum",
                            letterSpacing = (-2).sp,
                        ),
                        color = colors.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (state.indefinite) {
                            stringResource(R.string.focus_indefinite_hint)
                        } else {
                            stringResource(
                                R.string.focus_session,
                                state.sessionIndex,
                                state.settings.sessionsBeforeLongBreak,
                            )
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(26.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PrimaryFocusButton(
                    running = state.running,
                    accent = accent,
                    onAccent = onAccent,
                    onClick = onToggle,
                )
                SecondaryFocusButton(
                    icon = Icons.Filled.Refresh,
                    label = stringResource(R.string.focus_reset),
                    container = secondaryContainer,
                    onClick = onReset,
                )
                SecondaryFocusButton(
                    icon = Icons.Filled.SkipNext,
                    label = stringResource(R.string.focus_skip),
                    container = secondaryContainer,
                    onClick = onSkip,
                )
            }

            Spacer(Modifier.height(22.dp))

            Text(
                text = stringResource(R.string.focus_up_next),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                color = colors.onSurfaceVariant.copy(alpha = 0.55f),
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = stringResource(
                    R.string.focus_next_hint,
                    formatClock(state.nextMode.minutes(state.settings) * 60),
                    stringResource(state.nextMode.labelRes),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                val total = state.settings.sessionsBeforeLongBreak
                val filled = if (state.completedInCycle > 0 && state.completedInCycle % total == 0) {
                    total
                } else {
                    state.completedInCycle % total
                }
                repeat(total) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == filled) 8.dp else 6.dp)
                            .background(
                                color = if (index < filled) accent else colors.outlineVariant.copy(alpha = 0.6f),
                                shape = CircleShape,
                            ),
                    )
                }
            }
        }

        GlassIconButton(
            icon = Icons.Filled.Settings,
            contentDescription = stringResource(R.string.focus_settings),
            onClick = onOpenTimerSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 12.dp),
        )
    }
}

/** Minimal M3E progress ring: translucent track + round-capped accent arc. */
@Composable
private fun TimerRing(
    progress: Float,
    accent: Color,
    track: Color,
    strokeWidth: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val stroke = strokeWidth.toPx()
        val diameter = size.minDimension - stroke
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        drawArc(
            color = track,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = androidx.compose.ui.geometry.Size(diameter, diameter),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        if (progress > 0.002f) {
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = androidx.compose.ui.geometry.Size(diameter, diameter),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
private fun PrimaryFocusButton(
    running: Boolean,
    accent: Color,
    onAccent: Color,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(42.dp),
        color = accent,
        contentColor = onAccent,
        shadowElevation = 4.dp,
        interactionSource = interaction,
        modifier = Modifier
            .width(186.dp)
            .height(84.dp)
            .pressScale(interaction, pressedScale = 0.88f),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (running) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(30.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(if (running) R.string.focus_pause else R.string.focus_play),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Composable
private fun SecondaryFocusButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    container: Color,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = container,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        interactionSource = interaction,
        modifier = Modifier
            .size(width = 64.dp, height = 74.dp)
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                RoundedCornerShape(22.dp),
            )
            .pressScale(interaction, pressedScale = 0.88f),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
