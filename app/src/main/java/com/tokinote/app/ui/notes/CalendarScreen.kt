package com.tokinote.app.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tokinote.app.R
import com.tokinote.app.data.Note
import com.tokinote.app.ui.components.PillGroup
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private enum class StatsScope { Year, Month, Week }

private val monthTitle = DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)
private val dayTitle = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)

/** 笔记统计：年 / 月 / 周三种粒度，整页可滚动，日历随粒度切换。 */
@Composable
fun CalendarScreen(
    notes: List<Note>,
    contentPadding: PaddingValues,
    topInset: androidx.compose.ui.unit.Dp = 0.dp,
    onOpenNote: (String) -> Unit,
) {
    var scopeName by rememberSaveable { mutableStateOf(StatsScope.Month.name) }
    val scope = StatsScope.valueOf(scopeName)
    var monthString by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month = remember(monthString) { YearMonth.parse(monthString) }
    var selectedDayString by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var dayOnly by rememberSaveable { mutableStateOf(false) }
    val selectedDay = remember(selectedDayString) { LocalDate.parse(selectedDayString) }

    val visibleNotes = remember(notes) { notes.filter { !it.archived && it.deletedAt == null } }
    val yearValue = month.year
    val rangeNotes = remember(scope, month, selectedDay, dayOnly, visibleNotes) {
        if (dayOnly) {
            return@remember visibleNotes.filter { it.journalDate == selectedDay }
                .sortedByDescending { it.updatedAt }
        }
        when (scope) {
            StatsScope.Year -> visibleNotes.filter { it.journalDate.year == yearValue }
            StatsScope.Month -> visibleNotes.filter { YearMonth.from(it.journalDate) == month }
            StatsScope.Week -> {
                val start = selectedDay.with(DayOfWeek.MONDAY)
                val end = start.plusDays(6)
                visibleNotes.filter { it.journalDate in start..end }
            }
        }.sortedByDescending { it.updatedAt }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 4.dp + topInset,
            bottom = contentPadding.calculateBottomPadding() + 120.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "scope") {
            PillGroup(
                options = listOf(
                    StatsScope.Year to stringResource(R.string.stats_scope_year),
                    StatsScope.Month to stringResource(R.string.stats_scope_month),
                    StatsScope.Week to stringResource(R.string.stats_scope_week),
                ),
                selected = scope,
                onSelect = { scopeName = it.name },
            )
        }

        when (scope) {
            StatsScope.Year -> {
                item(key = "year-nav") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { monthString = month.minusYears(1).toString() }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                        }
                        Text(
                            text = "${month.year}" + "\u5E74",
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { dayOnly = false },
                        )
                        IconButton(onClick = { monthString = month.plusYears(1).toString() }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                        }
                    }
                }
            }

            StatsScope.Month -> {
                item(key = "month-nav") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { monthString = month.minusMonths(1).toString() }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                        }
                        Text(
                            text = month.format(monthTitle),
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { dayOnly = false },
                        )
                        IconButton(onClick = { monthString = month.plusMonths(1).toString() }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                        }
                    }
                }
                item(key = "month-grid") {
                    val entriesByDay = notes
                        .filter { YearMonth.from(it.journalDate) == month }
                        .groupBy { it.journalDate }
                    MonthGrid(
                        month = month,
                        entriesByDay = entriesByDay,
                        selectedDay = if (dayOnly) selectedDay else LocalDate.MIN,
                        onSelectDay = {
                            selectedDayString = it.toString()
                            dayOnly = true
                        },
                    )
                }
            }

            StatsScope.Week -> {
                item(key = "week-nav") {
                    val start = selectedDay.with(DayOfWeek.MONDAY)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { selectedDayString = selectedDay.minusWeeks(1).toString() }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                        }
                        Text(
                            text = "${start.format(dayTitle)} - ${start.plusDays(6).format(dayTitle)}",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { dayOnly = false },
                        )
                        IconButton(onClick = { selectedDayString = selectedDay.plusWeeks(1).toString() }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                        }
                    }
                }
                item(key = "week-strip") {
                    val start = selectedDay.with(DayOfWeek.MONDAY)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        repeat(7) { offset ->
                            val day = start.plusDays(offset.toLong())
                            val active = day == selectedDay
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (active) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                        },
                                    )
                                    .clickable {
                                        selectedDayString = day.toString()
                                        dayOnly = true
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.CHINA),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (active) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                    Text(
                                        text = day.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (active) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (rangeNotes.isEmpty()) {
            item(key = "empty") {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.no_entries),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(rangeNotes, key = { it.id }) { note ->
                JournalCard(
                    note = note,
                    showTime = true,
                    onClick = { onOpenNote(note.id) },
                    onLongClick = {},
                )
            }
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    entriesByDay: Map<LocalDate, List<Note>>,
    selectedDay: LocalDate,
    onSelectDay: (LocalDate) -> Unit,
) {
    val firstDay = month.atDay(1)
    val offset = (firstDay.dayOfWeek.value - 1).coerceAtLeast(0)
    val days = month.lengthOfMonth()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(vertical = 6.dp),
                )
            }
        }

        val totalCells = offset + days
        val weeks = (totalCells + 6) / 7
        repeat(weeks) { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { column ->
                    val cellIndex = week * 7 + column
                    val dayNumber = cellIndex - offset + 1
                    if (dayNumber in 1..days) {
                        val date = month.atDay(dayNumber)
                        val hasEntry = entriesByDay[date]?.isNotEmpty() == true
                        DayCell(
                            day = dayNumber,
                            hasEntry = hasEntry,
                            selected = date == selectedDay,
                            onClick = { onSelectDay(date) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f).height(46.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    hasEntry: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(2.dp)
            .height(46.dp)
            .clip(CircleShape)
            .background(
                when {
                    selected -> MaterialTheme.colorScheme.primary
                    hasEntry -> MaterialTheme.colorScheme.surfaceContainerHigh
                    else -> MaterialTheme.colorScheme.surface
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}
