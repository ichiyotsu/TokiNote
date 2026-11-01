package com.tokinote.app.ui.focus

import android.os.SystemClock
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokinote.app.R
import com.tokinote.app.ui.LocalMotionEnabled
import com.tokinote.app.ui.components.GlassIconButton
import com.tokinote.app.ui.formatElapsed
import com.tokinote.app.ui.pressScale
import kotlinx.coroutines.isActive

@Composable
fun FocusScreen(
    state: FocusState,
    contentPadding: PaddingValues,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    onOpenTimerSettings: () -> Unit,
    onScheduleIndefinite: (Long) -> Unit,
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
            animatingOut = false
        }
    }

    val requestIndefinite = {
        if (!animatingOut && !state.running && !state.indefinite) {
            if (motion) animatingOut = true
            onScheduleIndefinite(if (motion) 430L else 0L)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        val configuration = LocalConfiguration.current
        val wide = configuration.smallestScreenWidthDp >= 600 && maxWidth > maxHeight
        if (wide) {
            val ringSize = minOf(480.dp, maxHeight * 0.82f)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .weight(6f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    FocusTimer(
                        state = state,
                        accent = accent,
                        track = track,
                        timeScale = timeScale,
                        timeAlpha = timeAlpha,
                        animatingOut = animatingOut,
                        onRequestIndefinite = requestIndefinite,
                        maxSize = ringSize,
                        timeSize = 96.sp,
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(4f)
                        .padding(start = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    FocusActions(
                        state = state,
                        accent = accent,
                        onAccent = onAccent,
                        secondaryContainer = secondaryContainer,
                        onToggle = onToggle,
                        onReset = onReset,
                        onSkip = onSkip,
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                FocusTimer(
                    state = state,
                    accent = accent,
                    track = track,
                    timeScale = timeScale,
                    timeAlpha = timeAlpha,
                    animatingOut = animatingOut,
                    onRequestIndefinite = requestIndefinite,
                    maxSize = 390.dp,
                    timeSize = 78.sp,
                )
                Spacer(Modifier.height(26.dp))
                FocusActions(
                    state = state,
                    accent = accent,
                    onAccent = onAccent,
                    secondaryContainer = secondaryContainer,
                    onToggle = onToggle,
                    onReset = onReset,
                    onSkip = onSkip,
                )
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

@Composable
private fun FocusTimer(
    state: FocusState,
    accent: Color,
    track: Color,
    timeScale: Float,
    timeAlpha: Float,
    animatingOut: Boolean,
    onRequestIndefinite: () -> Unit,
    maxSize: Dp,
    timeSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    val now = rememberTimerNow(state.running)
    val remainingMillis = liveRemainingMillis(state, now)
    val elapsedMillis = liveElapsedMillis(state, now)
    val showSeconds = if (state.indefinite) (elapsedMillis / 1000L).toInt() else ceilSeconds(remainingMillis)
    val progress = if (state.indefinite || state.totalMillis <= 0L) {
        0f
    } else {
        (1f - remainingMillis.toFloat() / state.totalMillis.toFloat()).coerceIn(0f, 1f)
    }
    val clockText = formatElapsed(showSeconds)
    Box(
        modifier = modifier
            .widthIn(max = maxSize)
            .fillMaxWidth(0.94f)
            .aspectRatio(1f)
            .pointerInput(state.mode, state.running, state.indefinite) {
                if (!state.running && !state.indefinite && state.mode == FocusMode.Focus) {
                    detectTapGestures(
                        onDoubleTap = { if (!animatingOut) onRequestIndefinite() },
                        onLongPress = { if (!animatingOut) onRequestIndefinite() },
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
                text = clockText,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = timeSize,
                    lineHeight = timeSize * 1.05f,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum",
                    letterSpacing = (-2).sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FocusActions(
    state: FocusState,
    accent: Color,
    onAccent: Color,
    secondaryContainer: Color,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PrimaryFocusButton(
                running = state.running,
                accent = accent,
                onAccent = onAccent,
                onClick = onToggle,
                modifier = Modifier.weight(1f),
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
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = stringResource(
                R.string.focus_next_hint,
                formatElapsed(state.nextMode.minutes(state.settings) * 60),
                stringResource(state.nextMode.labelRes),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            val total = state.settings.sessionsBeforeLongBreak.coerceAtLeast(1)
            val filled = filledSessionCount(state.completedInCycle, total, state.mode)
            repeat(total) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == filled) 8.dp else 6.dp)
                        .background(
                            color = if (index < filled) accent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            shape = CircleShape,
                        ),
                )
            }
        }
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
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(42.dp),
        color = accent,
        contentColor = onAccent,
        shadowElevation = 4.dp,
        interactionSource = interaction,
        modifier = modifier
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

internal fun filledSessionCount(completedInCycle: Int, cycleLength: Int, mode: FocusMode): Int {
    val cycle = cycleLength.coerceAtLeast(1)
    return if (mode != FocusMode.Focus && completedInCycle > 0 && completedInCycle % cycle == 0) {
        cycle
    } else {
        completedInCycle.mod(cycle)
    }
}

@Composable
private fun rememberTimerNow(running: Boolean): Long {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(running) {
        now = SystemClock.elapsedRealtime()
        if (!running) return@LaunchedEffect
        while (isActive) {
            withFrameMillis {
                now = SystemClock.elapsedRealtime()
            }
        }
    }
    return now
}

private fun liveRemainingMillis(state: FocusState, now: Long): Long {
    return if (state.running && !state.indefinite && state.deadlineElapsed > 0L) {
        (state.deadlineElapsed - now).coerceAtLeast(0L)
    } else {
        state.remainingMillis
    }
}

private fun liveElapsedMillis(state: FocusState, now: Long): Long {
    return when {
        !state.indefinite -> 0L
        state.running -> state.elapsedBaseMillis + (now - state.runStartedElapsed).coerceAtLeast(0L)
        else -> state.elapsedBaseMillis
    }
}
