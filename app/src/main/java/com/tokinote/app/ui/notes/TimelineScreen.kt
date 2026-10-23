package com.tokinote.app.ui.notes

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tokinote.app.R
import com.tokinote.app.data.Note
import java.time.LocalDate
import java.time.YearMonth

private data class AlbumItem(val noteId: String, val path: String, val index: Int, val date: LocalDate)

@Composable
fun TimelineScreen(
    notes: List<Note>,
    showTime: Boolean,
    contentPadding: PaddingValues,
    onOpenNote: (String) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenMedia: (String, Int) -> Unit,
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
    val album = remember(monthNotes) {
        monthNotes.flatMap { note ->
            note.media.mapIndexed { index, path -> AlbumItem(note.id, path, index, note.journalDate) }
        }
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
                        showTime = showTime,
                        contentPadding = contentPadding,
                        onOpenNote = onOpenNote,
                    )

                    JournalTab.Album -> AlbumTabContent(
                        items = album,
                        contentPadding = contentPadding,
                        onOpenMedia = onOpenMedia,
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}

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
    showTime: Boolean,
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
                JournalCard(
                    note = note,
                    showTime = showTime,
                    onClick = { onOpenNote(note.id) },
                    onLongClick = {},
                )
            }
        }
    }
}

@Composable
private fun AlbumTabContent(
    items: List<AlbumItem>,
    contentPadding: PaddingValues,
    onOpenMedia: (String, Int) -> Unit,
) {
    if (items.isEmpty()) {
        EmptyMonthHint(stringResource(R.string.album_empty))
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 4.dp,
            bottom = contentPadding.calculateBottomPadding() + 120.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(items, key = { "${it.noteId}-${it.path}" }) { item ->
            AlbumCell(item = item, onClick = { onOpenMedia(item.noteId, item.index) })
        }
    }
}

@Composable
private fun AlbumCell(item: AlbumItem, onClick: () -> Unit) {
    val image = rememberImage(item.path)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
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
