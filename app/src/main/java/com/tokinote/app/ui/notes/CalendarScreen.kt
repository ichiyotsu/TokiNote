package com.tokinote.app.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokinote.app.R
import com.tokinote.app.data.Note
import com.tokinote.app.ui.components.ExpressiveTopBar
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val calendarMonthFormatter = DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)

private enum class StatsScope { Day, Week, Month, Year }

@Composable
fun CalendarScreen(
    notes: List<Note>,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenNote: (String) -> Unit,
    showHeader: Boolean = true,
    onOpenFocusStats: (() -> Unit)? = null,
) {
    var monthString by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month = remember(monthString) { YearMonth.parse(monthString) }
    var selectedDayString by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val selectedDay = remember(selectedDayString) { LocalDate.parse(selectedDayString) }

    val entriesByDay = remember(notes, month) {
        notes.filter { YearMonth.from(it.journalDate) == month }
            .groupBy { it.journalDate }
    }
    val dayEntries = entriesByDay[selectedDay].orEmpty().sortedByDescending { it.updatedAt }

    var scopeName by rememberSaveable { mutableStateOf(StatsScope.Month.name) }
    val scope = StatsScope.valueOf(scopeName)
    val rangeStart = remember(scope, selectedDay) {
        when (scope) {
            StatsScope.Day -> selectedDay
            StatsScope.Week -> selectedDay.minusDays((selectedDay.dayOfWeek.value - 1).toLong())
            StatsScope.Month -> selectedDay.withDayOfMonth(1)
            StatsScope.Year -> selectedDay.withDayOfYear(1)
        }
    }
    val rangeEnd = remember(scope, selectedDay) {
        when (scope) {
            StatsScope.Day -> selectedDay
            StatsScope.Week -> rangeStart.plusDays(6)
            StatsScope.Month -> rangeStart.plusMonths(1).minusDays(1)
            StatsScope.Year -> rangeStart.plusYears(1).minusDays(1)
        }
    }
    val rangeNotes = notes.filter { it.journalDate in rangeStart..rangeEnd }
    val rangeImages = rangeNotes.sumOf { it.media.size }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        if (showHeader) {
            ExpressiveTopBar(
                title = stringResource(R.string.calendar_title),
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
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatsScope.entries.forEach { item ->
                val selected = item == scope
                Surface(
                    onClick = { scopeName = item.name },
                    shape = CircleShape,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    contentColor = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(
                            when (item) {
                                StatsScope.Day -> R.string.stats_scope_day
                                StatsScope.Week -> R.string.stats_scope_week
                                StatsScope.Month -> R.string.stats_scope_month
                                StatsScope.Year -> R.string.stats_scope_year
                            },
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 9.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.stats_notes_count, rangeNotes.size),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = stringResource(R.string.stats_photos_count, rangeImages),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                }
                if (onOpenFocusStats != null) {
                    Surface(
                        onClick = onOpenFocusStats,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Text(
                            text = stringResource(R.string.stats_focus_link),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { monthString = month.minusMonths(1).toString() }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
            }
            Text(
                text = month.format(calendarMonthFormatter),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            IconButton(onClick = { monthString = month.plusMonths(1).toString() }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }

        MonthGrid(
            month = month,
            entriesByDay = entriesByDay,
            selectedDay = selectedDay,
            onSelectDay = { selectedDayString = it.toString() },
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selectedDay.dayLabel(),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.calendar_entries, dayEntries.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (dayEntries.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.no_entries),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = contentPadding.calculateBottomPadding() + 100.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(dayEntries, key = { it.id }) { note ->
                    JournalCard(note = note, onClick = { onOpenNote(note.id) }, onLongClick = {})
                }
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

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
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

        var dayCounter = 1
        val totalCells = offset + days
        val weeks = (totalCells + 6) / 7
        repeat(weeks) { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { column ->
                    val cellIndex = week * 7 + column
                    val dayNumber = cellIndex - offset + 1
                    if (dayNumber in 1..days) {
                        val date = month.atDay(dayNumber)
                        val emoji: String? = null
                        DayCell(
                            day = dayNumber,
                            emoji = emoji,
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
    emoji: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(2.dp)
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    selected -> MaterialTheme.colorScheme.primary
                    emoji != null -> MaterialTheme.colorScheme.surfaceContainerHigh
                    else -> MaterialTheme.colorScheme.surface
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (emoji != null) {
            Text(text = emoji, fontSize = 18.sp)
        } else {
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
}
