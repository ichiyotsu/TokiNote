package com.tokinote.app.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import com.tokinote.app.R
import com.tokinote.app.data.JournalFile
import com.tokinote.app.data.Note
import com.tokinote.app.ui.map.TencentMap
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun TimelineScreen(
    notes: List<Note>,
    contentPadding: PaddingValues,
    onOpenNote: (String) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenFile: (JournalFile) -> Unit,
) {
    var selectedMonthString by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val selectedMonth = remember(selectedMonthString) { YearMonth.parse(selectedMonthString) }
    var tab by rememberSaveable { mutableStateOf(JournalTab.Journal) }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    val months = remember { (11 downTo 0).map { YearMonth.now().minusMonths(it.toLong()) } }
    val monthNotes = remember(notes, selectedMonth) {
        notes
            .filter { YearMonth.from(it.journalDate) == selectedMonth }
            .sortedByDescending { it.updatedAt }
    }
    val groups = remember(monthNotes) {
        monthNotes.groupBy { it.journalDate }.toSortedMap(compareByDescending { it })
    }

    val results = remember(notes, query) {
        if (query.isBlank()) {
            emptyList()
        } else {
            notes
                .filter {
                    it.title.contains(query, ignoreCase = true) || it.body.contains(query, ignoreCase = true)
                }
                .sortedByDescending { it.updatedAt }
        }
    }
    val groupedResults = remember(results) {
        results.groupBy { it.journalDate }.toSortedMap(compareByDescending { it })
    }

    Column(modifier = Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
        JournalTopBar(
            searchActive = searchActive,
            query = query,
            onQueryChange = { query = it },
            onOpenSearch = { searchActive = true },
            onCloseSearch = {
                query = ""
                searchActive = false
            },
            onOpenCalendar = onOpenCalendar,
        )

        if (searchActive) {
            SearchResults(
                query = query,
                results = results,
                groups = groupedResults,
                contentPadding = contentPadding,
                onOpenNote = onOpenNote,
            )
        } else {
            MonthStrip(
                months = months,
                selected = selectedMonth,
                hasEntries = { month ->
                    notes.any { YearMonth.from(it.journalDate) == month }
                },
                onSelect = { selectedMonthString = it.toString() },
            )

            Spacer(Modifier.height(8.dp))
            JournalTabs(selected = tab, onSelect = { tab = it })
            Spacer(Modifier.height(4.dp))

            Box(modifier = Modifier.weight(1f)) {
                when (tab) {
                    JournalTab.Journal -> JournalTabContent(
                        groups = groups,
                        contentPadding = contentPadding,
                        onOpenNote = onOpenNote,
                    )

                    JournalTab.Media -> MediaTabContent(groups = groups, contentPadding = contentPadding)
                    JournalTab.Files -> FilesTabContent(
                        groups = groups,
                        contentPadding = contentPadding,
                        onOpenFile = onOpenFile,
                    )

                    JournalTab.Map -> MapTabContent(
                        notes = monthNotes.filter { it.location != null },
                        contentPadding = contentPadding,
                        onOpenNote = onOpenNote,
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}

/**
 * 搜索结果：先按「标题命中 / 正文命中」给出容器化的统计，再把结果按日期分组，
 * 每组放进一张大圆角容器里，符合 MD3E 的分组（grouping）与容器（container）原则。
 */
@Composable
private fun SearchResults(
    query: String,
    results: List<Note>,
    groups: Map<LocalDate, List<Note>>,
    contentPadding: PaddingValues,
    onOpenNote: (String) -> Unit,
) {
    when {
        query.isBlank() -> SearchHint(stringResource(R.string.search_prompt))
        results.isEmpty() -> SearchHint(stringResource(R.string.search_results_empty, query))
        else -> {
            val titleHits = results.count { it.title.contains(query, ignoreCase = true) }
            val bodyHits = results.count { it.body.contains(query, ignoreCase = true) }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 4.dp,
                    bottom = contentPadding.calculateBottomPadding() + 120.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "search-summary") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ResultChip(stringResource(R.string.search_result_count, results.size), emphasized = true)
                        ResultChip(stringResource(R.string.search_title_match, titleHits))
                        ResultChip(stringResource(R.string.search_body_match, bodyHits))
                    }
                }
                groups.forEach { (date, dayNotes) ->
                    item(key = "search-header-$date") { DayHeader(date, dayNotes.size) }
                    item(key = "search-group-$date") {
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column {
                                dayNotes.forEachIndexed { index, note ->
                                    if (index > 0) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.padding(start = 68.dp),
                                        )
                                    }
                                    SearchResultRow(
                                        note = note,
                                        query = query,
                                        onClick = { onOpenNote(note.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultChip(text: String, emphasized: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (emphasized) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (emphasized) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun SearchHint(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun JournalTabContent(
    groups: Map<LocalDate, List<Note>>,
    contentPadding: PaddingValues,
    onOpenNote: (String) -> Unit,
) {
    if (groups.isEmpty()) {
        EmptyMonthHint(stringResource(R.string.no_entries))
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 4.dp,
            bottom = contentPadding.calculateBottomPadding() + 120.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        groups.forEach { (date, dayNotes) ->
            item(key = "header-$date") { DayHeader(date, dayNotes.size) }
            items(dayNotes, key = { it.id }) { note ->
                JournalCard(note = note, onClick = { onOpenNote(note.id) }, onLongClick = {})
            }
        }
    }
}

@Composable
private fun MediaTabContent(
    groups: Map<LocalDate, List<Note>>,
    contentPadding: PaddingValues,
) {
    val mediaGroups = groups.mapValues { (_, dayNotes) -> dayNotes.flatMap { it.media } }
        .filterValues { it.isNotEmpty() }

    if (mediaGroups.isEmpty()) {
        EmptyMonthHint(stringResource(R.string.media_empty))
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 4.dp,
            bottom = contentPadding.calculateBottomPadding() + 120.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        mediaGroups.forEach { (date, paths) ->
            item(key = "media-$date") { DayHeader(date, paths.size) }
            paths.chunked(3).forEach { rowPaths ->
                item(key = "row-$date-${rowPaths.first()}") {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        rowPaths.forEach { path ->
                            MediaThumb(path = path, modifier = Modifier.weight(1f))
                        }
                        repeat(3 - rowPaths.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaThumb(path: String, modifier: Modifier = Modifier) {
    val image = rememberImage(path)
    Box(
        modifier = modifier
            .height(110.dp)
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        if (image != null) {
            androidx.compose.foundation.Image(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun FilesTabContent(
    groups: Map<LocalDate, List<Note>>,
    contentPadding: PaddingValues,
    onOpenFile: (JournalFile) -> Unit,
) {
    val fileGroups = groups.mapValues { (_, dayNotes) -> dayNotes.flatMap { it.files } }
        .filterValues { it.isNotEmpty() }

    if (fileGroups.isEmpty()) {
        EmptyMonthHint(stringResource(R.string.files_empty))
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 4.dp,
            bottom = contentPadding.calculateBottomPadding() + 120.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        fileGroups.forEach { (date, files) ->
            item(key = "files-$date") { DayHeader(date, files.size) }
            items(files, key = { it.path }) { file -> FileCard(file = file, onClick = { onOpenFile(file) }) }
        }
    }
}

@Composable
private fun MapTabContent(
    notes: List<Note>,
    contentPadding: PaddingValues,
    onOpenNote: (String) -> Unit,
) {
    if (notes.isEmpty()) {
        EmptyMonthHint(stringResource(R.string.map_empty))
        return
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = contentPadding.calculateBottomPadding() + 8.dp),
    ) {
        TencentMap(
            notes = notes,
            modifier = Modifier.fillMaxSize(),
            onOpenNote = onOpenNote,
        )
    }
}

@Composable
private fun EmptyMonthHint(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
