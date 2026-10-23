package com.tokinote.app.ui.focus

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokinote.app.R
import com.tokinote.app.ui.components.ExpressiveTopBar
import com.tokinote.app.ui.formatClock
import com.tokinote.app.ui.formatMinutes

@Composable
fun FocusScreen(
    state: FocusState,
    todayMinutes: Int,
    contentPadding: PaddingValues,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    onOpenTimerSettings: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val accent = if (state.mode == FocusMode.Focus) colorScheme.primary else colorScheme.tertiary
    val track = if (state.mode == FocusMode.Focus) colorScheme.secondaryContainer else colorScheme.tertiaryContainer
    val accentContainer = if (state.mode == FocusMode.Focus) {
        colorScheme.primaryContainer
    } else {
        colorScheme.tertiaryContainer
    }
    val onAccentContainer = if (state.mode == FocusMode.Focus) {
        colorScheme.onPrimaryContainer
    } else {
        colorScheme.onTertiaryContainer
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        ExpressiveTopBar(
            title = stringResource(state.mode.labelRes),
            subtitle = stringResource(R.string.focus_today, formatMinutes(todayMinutes)),
            titleColor = accent,
            actions = {
                IconButton(onClick = onOpenTimerSettings) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.settings_timer),
                        tint = colorScheme.onSurfaceVariant,
                    )
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 330.dp)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 16.dp.toPx()
                    val gap = 8.dp.toPx()
                    val trackRadius = size.minDimension / 2f - strokeWidth / 2f
                    val progressRadius = trackRadius - gap

                    drawCircle(
                        color = track,
                        radius = trackRadius,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                    if (state.progress > 0f) {
                        drawArc(
                            color = accent,
                            startAngle = -90f,
                            sweepAngle = 360f * state.progress,
                            useCenter = false,
                            topLeft = Offset(
                                center.x - progressRadius,
                                center.y - progressRadius,
                            ),
                            size = Size(progressRadius * 2f, progressRadius * 2f),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatClock(state.remainingSeconds),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 72.sp,
                            lineHeight = 80.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFeatureSettings = "tnum",
                        ),
                        color = colorScheme.onSurface,
                    )
                    if (state.mode == FocusMode.Focus) {
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

            Spacer(Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onToggle,
                    shape = RoundedCornerShape(34.dp),
                    color = if (state.running) accent else colorScheme.surfaceContainerHigh,
                    contentColor = if (state.running) colorScheme.onPrimary else colorScheme.onSurface,
                    modifier = Modifier.size(width = 126.dp, height = 92.dp),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (state.running) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(
                                if (state.running) R.string.focus_pause else R.string.focus_play,
                            ),
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }

                Surface(
                    onClick = onReset,
                    shape = CircleShape,
                    color = accentContainer,
                    contentColor = onAccentContainer,
                    modifier = Modifier.size(92.dp),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.focus_reset),
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }

                Surface(
                    onClick = onSkip,
                    shape = RoundedCornerShape(34.dp),
                    color = accentContainer,
                    contentColor = onAccentContainer,
                    modifier = Modifier.size(width = 72.dp, height = 92.dp),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = stringResource(R.string.focus_skip),
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

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

            Spacer(Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.padding(bottom = 24.dp),
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
fun FocusMiniStatus(state: FocusState, todayMinutes: Int) {
    Text(
        text = "${stringResource(state.mode.labelRes)} · ${formatClock(state.remainingSeconds)}",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}
