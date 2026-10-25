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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokinote.app.R
import com.tokinote.app.data.FocusSession
import com.tokinote.app.ui.LocalMotionEnabled
import com.tokinote.app.ui.components.ExpressiveTopBar
import com.tokinote.app.ui.components.GlassIconButton
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

private fun sessionDate(session: FocusSession): LocalDate =
    Instant.ofEpochMilli(session.endedAt).atZone(ZoneId.systemDefault()).toLocalDate()

/**
 * 统计页，布局参考 Tomato：今天两张卡片、上周柱状图、上个月迷你柱状图。
 */
@Composable
fun FocusSummaryScreen(
    sessions: List<FocusSession>,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
    val today = LocalDate.now()
    val todayFocus = sessions.filter { sessionDate(it) == today }.sumOf { it.minutes }
    val todayBreak = sessions.filter { sessionDate(it) == today }.sumOf { it.breakMinutes }

    val weekDays = remember(today) { (6 downTo 0).map { today.minusDays(it.toLong()) } }
    val weekFocus = weekDays.map { day ->
        sessions.filter { sessionDate(it) == day }.sumOf { it.minutes }
    }
    val weekTotal = weekFocus.sum()
    val weekAvg = weekTotal / 7

    val monthDays = remember(today) { (29 downTo 0).map { today.minusDays(it.toLong()) } }
    val monthFocus = monthDays.map { day ->
        sessions.filter { sessionDate(it) == day }.sumOf { it.minutes }
    }
    val monthAvg = monthFocus.sum() / 30

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        ExpressiveTopBar(
            title = stringResource(R.string.focus_summary),
            alignStart = true,
            navigationIcon = {
                GlassIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    onClick = onBack,
                    modifier = Modifier.padding(start = 20.dp),
                )
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = stringResource(R.string.stats_today),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    label = stringResource(R.string.stats_focus),
                    value = durationLabel(todayFocus),
                    container = MaterialTheme.colorScheme.primaryContainer,
                    content = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = stringResource(R.string.stats_break),
                    value = durationLabel(todayBreak),
                    container = MaterialTheme.colorScheme.tertiaryContainer,
                    content = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(26.dp))
            Text(
                text = stringResource(R.string.stats_last_week),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = durationLabel(weekAvg),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 40.sp,
                    lineHeight = 44.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.stats_avg_focus),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            BarChart(values = weekFocus, labels = weekDays.map { dayLabel(it) })

            Spacer(Modifier.height(26.dp))
            Text(
                text = stringResource(R.string.stats_last_month),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = durationLabel(monthAvg),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 40.sp,
                    lineHeight = 44.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.stats_avg_focus),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            MiniChart(values = monthFocus)

            Spacer(Modifier.height(28.dp))
        }
    }
}

private fun dayLabel(day: LocalDate): String =
    day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.CHINA)

private fun durationLabel(minutes: Int): String =
    if (minutes < 60) "$minutes 分" else "${minutes / 60} 小时 ${minutes % 60} 分"

@Composable
private fun StatCard(
    label: String,
    value: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    Surface(shape = RoundedCornerShape(22.dp), color = container, modifier = modifier) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = content.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = content,
            )
        }
    }
}

@Composable
private fun BarChart(values: List<Int>, labels: List<String>) {
    val motion = LocalMotionEnabled.current
    val maxValue = maxOf(values.maxOrNull() ?: 0, 30)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(
            modifier = Modifier
                .height(150.dp)
                .padding(end = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End,
        ) {
            Text(durationLabel(maxValue), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("30 分", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            values.forEachIndexed { index, value ->
                val fraction by animateFloatAsState(
                    targetValue = if (value > 0) (value.toFloat() / maxValue).coerceIn(0.05f, 1f) else 0.02f,
                    animationSpec = tween(if (motion) 520 else 0),
                    label = "bar",
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .height(130.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (value > 0) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    },
                                ),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = labels.getOrElse(index) { "" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniChart(values: List<Int>) {
    val maxValue = maxOf(values.maxOrNull() ?: 0, 1)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEach { value ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight((value.toFloat() / maxValue).coerceIn(0.04f, 1f))
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (value > 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        },
                    ),
            )
        }
    }
}
