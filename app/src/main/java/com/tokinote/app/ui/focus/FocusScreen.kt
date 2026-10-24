package com.tokinote.app.ui.focus

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tokinote.app.ui.formatElapsed
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokinote.app.R
import com.tokinote.app.ui.LocalMotionEnabled
import com.tokinote.app.ui.formatClock
import com.tokinote.app.ui.formatMinutes
import com.tokinote.app.ui.pressScale

@Composable
fun FocusScreen(
    state: FocusState,
    todayMinutes: Int,
    contentPadding: PaddingValues,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    onOpenTimerSettings: () -> Unit,
    onStartIndefinite: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val motion = LocalMotionEnabled.current
    val accent = if (state.mode == FocusMode.Focus) colorScheme.primary else colorScheme.tertiary
    val track = if (state.mode == FocusMode.Focus) colorScheme.secondaryContainer else colorScheme.tertiaryContainer
    val accentContainer = if (state.mode == FocusMode.Focus) {
        colorScheme.primaryContainer
    } else {
        colorScheme.tertiaryContainer
    }
    val onAccent = if (state.mode == FocusMode.Focus) colorScheme.onPrimary else colorScheme.onTertiary
    val onAccentContainer = if (state.mode == FocusMode.Focus) {
        colorScheme.onPrimaryContainer
    } else {
        colorScheme.onTertiaryContainer
    }

    var animatingOut by remember { mutableStateOf(false) }
    val timeScale by animateFloatAsState(
        targetValue = if (animatingOut) 2.6f else 1f,
        animationSpec = tween(420),
        label = "timeScale",
    )
    val timeAlpha by animateFloatAsState(
        targetValue = if (animatingOut) 0f else 1f,
        animationSpec = tween(420),
        label = "timeAlpha",
    )
    val timeBlur by animateFloatAsState(
        targetValue = if (animatingOut) 26f else 0f,
        animationSpec = tween(420),
        label = "timeBlur",
    )
    val displaySeconds = if (state.indefinite) state.elapsedSeconds else state.remainingSeconds
    LaunchedEffect(animatingOut) {
        if (animatingOut) {
            kotlinx.coroutines.delay(440)
            onStartIndefinite()
            animatingOut = false
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = state.progress,
        animationSpec = tween(durationMillis = if (motion) 950 else 0, easing = LinearEasing),
        label = "ringProgress",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(vertical = 28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .fillMaxWidth()
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
                WavyRing(
                    progress = animatedProgress,
                    accent = accent,
                    track = track,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = timeScale
                                scaleY = timeScale
                                alpha = timeAlpha
                            }
                            .blur(timeBlur.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded),
                    ) {
                        Text(
                            text = formatClock(displaySeconds),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 60.sp,
                                lineHeight = 64.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum",
                            ),
                            color = colorScheme.onSurface,
                        )
                        if (state.indefinite) {
                            Text(
                                text = stringResource(R.string.focus_indefinite),
                                style = MaterialTheme.typography.labelLarge,
                                color = colorScheme.onSurfaceVariant,
                            )
                        } else if (state.mode == FocusMode.Focus) {
                            Text(
                                text = stringResource(
                                    R.string.focus_session,
                                    state.sessionIndex,
                                    state.settings.sessionsBeforeLongBreak,
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FocusAction(
                    icon = if (state.running) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    width = 132.dp,
                    height = 72.dp,
                    shape = RoundedCornerShape(36.dp),
                    iconSize = 28.dp,
                    label = stringResource(if (state.running) R.string.focus_pause else R.string.focus_play),
                    container = accent,
                    content = onAccent,
                    onClick = onToggle,
                )
                FocusAction(
                    icon = Icons.Filled.Refresh,
                    width = 64.dp,
                    height = 64.dp,
                    shape = RoundedCornerShape(20.dp),
                    label = stringResource(R.string.focus_reset),
                    container = accentContainer,
                    content = onAccentContainer,
                    onClick = onReset,
                )
                FocusAction(
                    icon = Icons.Filled.SkipNext,
                    width = 54.dp,
                    height = 64.dp,
                    shape = RoundedCornerShape(20.dp),
                    label = stringResource(R.string.focus_skip),
                    container = MaterialTheme.colorScheme.surfaceContainerHigh,
                    content = colorScheme.onSurfaceVariant,
                    onClick = onSkip,
                )
                FocusAction(
                    icon = Icons.Filled.Settings,
                    width = 54.dp,
                    height = 64.dp,
                    shape = RoundedCornerShape(20.dp),
                    label = stringResource(R.string.focus_settings),
                    container = MaterialTheme.colorScheme.surfaceContainerHigh,
                    content = colorScheme.onSurfaceVariant,
                    onClick = onOpenTimerSettings,
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.focus_up_next),
                style = MaterialTheme.typography.labelLarge,
                color = colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = formatClock(state.nextMode.minutes(state.settings) * 60),
                style = MaterialTheme.typography.titleLarge,
                color = if (state.nextMode == FocusMode.Focus) colorScheme.primary else colorScheme.tertiary,
            )
            Text(
                text = stringResource(state.nextMode.labelRes),
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.onSurface,
            )

            Spacer(Modifier.height(18.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.padding(bottom = 28.dp),
            ) {
                val total = state.settings.sessionsBeforeLongBreak
                val filled = if (state.completedInCycle > 0 && state.completedInCycle % total == 0) {
                    total
                } else {
                    state.completedInCycle % total
                }
                repeat(total) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (index < filled) accent else colorScheme.outlineVariant,
                                shape = CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun FocusAction(
    icon: ImageVector,
    label: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    width: androidx.compose.ui.unit.Dp = 64.dp,
    height: androidx.compose.ui.unit.Dp = 64.dp,
    shape: androidx.compose.ui.graphics.Shape = CircleShape,
    iconSize: androidx.compose.ui.unit.Dp = 22.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        shape = shape,
        color = container,
        contentColor = content,
        interactionSource = interaction,
        modifier = Modifier.pressScale(interaction, pressedScale = 0.95f),
    ) {
        Box(
            modifier = Modifier
                .height(height)
                .width(width),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(iconSize))
        }
    }
}
