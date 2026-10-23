package com.tokinote.app.ui.focus

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokinote.app.R
import com.tokinote.app.data.FocusSession
import com.tokinote.app.ui.LocalMotionEnabled
import com.tokinote.app.ui.components.ExpressiveTopBar
import com.tokinote.app.ui.formatMinutes
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val sessionStamp = DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.CHINA)

private fun sessionDay(session: FocusSession): LocalDate =
    Instant.ofEpochMilli(session.endedAt).atZone(ZoneId.systemDefault()).toLocalDate()

/**
 * 专注总结页（docs/design-refs/02-focus-summary.png）：
 * 大数字 + 一周柱状图 + 最近记录，数据来自 [FocusSession] 历史。
 */
@Composable
fun FocusSummaryScreen(
    sessions: List<FocusSession>,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
    var weekOffset by rememberSaveable { mutableStateOf(0) }
    val today = LocalDate.now()
    val monday = remember(weekOffset, today) {
        today.with(DayOfWeek.MONDAY).plusWeeks(weekOffset.toLong())
    }
    val days = remember(monday) { (0L..6L).map { monday.plusDays(it) } }
    val byDay = remember(sessions, days) {
        days.associateWith { day -> sessions.filter { sessionDay(it) == day } }
    }
    val totalMinutes = byDay.values.sumOf { list -> list.sumOf { it.minutes } }
    val sessionCount = byDay.values.sumOf { it.size }
    val maxMinutes = maxOf(
        byDay.values.maxOfOrNull { list -> list.sumOf { it.minutes } } ?: 0,
        30,
    )
    val recent = byDay.values.flatten().sortedByDescending { it.endedAt }.take(8)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        ExpressiveTopBar(
            title = stringResource(R.string.focus_summary),
            alignStart = true,
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                    )
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            WeekSwitcher(
                weekOffset = weekOffset,
                today = today,
                monday = monday,
                onPrevious = { weekOffset -= 1 },
                onNext = { if (weekOffset < 0) weekOffset += 1 },
            )

            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = totalMinutes.toString(),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 64.sp,
                        lineHeight = 68.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum",
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.summary_minutes_unit),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
            Text(
                text = stringResource(
                    R.string.summary_caption,
                    sessionCount,
                    formatMinutes(if (sessionCount == 0) 0 else totalMinutes / 7),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    days.forEach { day ->
                        val minutes = byDay[day].orEmpty().sumOf { it.minutes }
                        DayBar(
                            day = day,
                            minutes = minutes,
                            maxMinutes = maxMinutes,
                            isToday = day == today,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.summary_recent),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(10.dp))

            if (recent.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = if (weekOffset == 0) {
                            stringResource(R.string.summary_recent_empty)
                        } else {
                            stringResource(R.string.summary_empty)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        recent.forEachIndexed { index, session ->
                            if (index > 0) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(start = 18.dp, end = 18.dp),
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                )
                                Spacer(Modifier.size(12.dp))
                                Text(
                                    text = Instant.ofEpochMilli(session.endedAt)
                                        .atZone(ZoneId.systemDefault())
                                        .format(sessionStamp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = stringResource(R.string.minutes_value, session.minutes),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun WeekSwitcher(
    weekOffset: Int,
    today: LocalDate,
    monday: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val label = when (weekOffset) {
        0 -> stringResource(R.string.summary_this_week)
        -1 -> stringResource(R.string.summary_last_week)
        else -> stringResource(R.string.summary_weeks_ago, -weekOffset)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(44.dp),
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.Filled.ChevronLeft,
                    contentDescription = stringResource(R.string.summary_prev_week),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.size(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = formatRange(monday, monday.plusDays(6)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(44.dp),
        ) {
            IconButton(onClick = onNext, enabled = weekOffset < 0) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = stringResource(R.string.summary_next_week),
                    modifier = Modifier.size(20.dp),
                    tint = if (weekOffset < 0) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    },
                )
            }
        }
    }
}

private val rangeFormatter = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)

private fun formatRange(start: LocalDate, end: LocalDate): String =
    "${start.format(rangeFormatter)} – ${end.format(rangeFormatter)}"

@Composable
private fun DayBar(
    day: LocalDate,
    minutes: Int,
    maxMinutes: Int,
    isToday: Boolean,
    modifier: Modifier = Modifier,
) {
    val motion = LocalMotionEnabled.current
    val target = if (maxMinutes <= 0) 0f else (minutes.toFloat() / maxMinutes).coerceIn(0f, 1f)
    val fraction by animateFloatAsState(
        targetValue = if (minutes > 0) target else 0.02f,
        animationSpec = tween(durationMillis = if (motion) 520 else 0),
        label = "dayBar",
    )
    val barColor: Color = when {
        minutes == 0 -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            modifier = Modifier
                .height(132.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction)
                    .clip(RoundedCornerShape(50))
                    .background(barColor),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.CHINA),
            style = MaterialTheme.typography.labelMedium,
            color = if (isToday) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
