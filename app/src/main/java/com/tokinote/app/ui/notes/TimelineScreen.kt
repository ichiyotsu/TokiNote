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
import com.tokinote.app.data.NoteGroup
import java.time.LocalDate
import java.time.YearMonth

private data class AlbumItem(val noteId: String, val path: String, val index: Int, val date: LocalDate)

@Composable
fun TimelineScreen(
    notes: List<Note>,
    groups: List<NoteGroup>,
    showTime: Boolean,
    contentPadding: PaddingValues,
    onOpenNote: (String) -> Unit,
    onOpenMedia: (String, Int) -> Unit,
    onOpenFocusStats: () -> Unit,
    onTodoToggle: (String) -> Unit,
    onArchiveNote: (String) -> Unit,
    onTrashNote: (String) -> Unit,
    onCreateGroup: (String, Set<String>) -> NoteGroup?,
    onUpdateGroup: (String, String, Set<String>) -> Boolean,
    onDeleteGroup: (String) -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(JournalTab.Journal) }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var previewNote by remember { mutableStateOf<Note?>(null) }
    var selectedGroupId by rememberSaveable { mutableStateOf<String?>(null) }
    var groupSheetOpen by rememberSaveable { mutableStateOf(false) }
    var editingGroupId by rememberSaveable { mutableStateOf<String?>(null) }

    val activeNotes = remember(notes) {
        notes.filter { !it.archived && it.deletedAt == null }.sortedByDescending { it.updatedAt }
    }
    val selectedGroup = groups.firstOrNull { it.id == selectedGroupId }
    val monthNotes = remember(activeNotes, selectedGroup) {
        if (selectedGroup == null) {
            activeNotes
        } else {
            activeNotes.filter { it.id in selectedGroup.noteIds }
        }
    }
    val album = remember(activeNotes) {
        activeNotes.flatMap { note ->
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
            JournalTabs(selected = tab, onSelect = { tab = it })
            if (tab == JournalTab.Journal) {
                Spacer(Modifier.height(10.dp))
                NoteGroupBar(
                    groups = groups,
                    selectedId = selectedGroupId,
                    onSelect = { selectedGroupId = it },
                    onCreate = {
                        editingGroupId = null
                        groupSheetOpen = true
                    },
                    onEdit = { group ->
                        editingGroupId = group.id
                        groupSheetOpen = true
                    },
                )
            }
            Spacer(Modifier.height(14.dp))

            Box(modifier = Modifier.weight(1f)) {
                when (tab) {
                    JournalTab.Journal -> JournalTabContent(
                        notes = monthNotes,
                        showTime = showTime,
                        contentPadding = contentPadding,
                        emptyText = if (selectedGroup != null) {
                            stringResource(R.string.group_empty)
                        } else {
                            stringResource(R.string.no_entries)
                        },
                        onOpenNote = onOpenNote,
                        onTodoToggle = onTodoToggle,
                        onLongPress = { previewNote = it },
                    )

                    JournalTab.Album -> AlbumTabContent(
                        items = album,
                        contentPadding = contentPadding,
                        onOpenMedia = onOpenMedia,
                    )

                    JournalTab.Stats -> CalendarScreen(
                        notes = notes,
                        contentPadding = contentPadding,
                        onOpenNote = onOpenNote,
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
        }
    }

    PreviewHost(
        note = previewNote,
        onDismiss = { previewNote = null },
        onOpen = { previewNote = null; onOpenNote(it) },
        onArchive = { previewNote = null; onArchiveNote(it) },
        onTrash = { previewNote = null; onTrashNote(it) },
    )

    if (groupSheetOpen) {
        NoteGroupSheet(
            group = editingGroupId?.let { id -> groups.firstOrNull { it.id == id } },
            notes = activeNotes,
            groups = groups,
            onDismiss = {
                groupSheetOpen = false
                editingGroupId = null
            },
            onSave = { name, noteIds ->
                val editing = editingGroupId
                if (editing == null) {
                    onCreateGroup(name, noteIds)?.let { created ->
                        selectedGroupId = created.id
                    }
                } else {
                    onUpdateGroup(editing, name, noteIds)
                }
            },
            onDelete = editingGroupId?.let { id ->
                {
                    onDeleteGroup(id)
                    if (selectedGroupId == id) selectedGroupId = null
                }
            },
        )
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
    notes: List<Note>,
    showTime: Boolean,
    contentPadding: PaddingValues,
    emptyText: String,
    onOpenNote: (String) -> Unit,
    onTodoToggle: (String) -> Unit,
    onLongPress: (Note) -> Unit = {},
) {
    if (notes.isEmpty()) {
        EmptyMonthHint(emptyText)
        return
    }
    val pinned = notes.filter { it.pinned }.sortedByDescending { it.updatedAt }
    val others = notes.filterNot { it.pinned }.sortedByDescending { it.updatedAt }

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
        if (pinned.isNotEmpty()) {
            items(pinned, key = { it.id }) { note ->
                JournalCard(
                    note = note,
                    showTime = showTime,
                    onClick = { onOpenNote(note.id) },
                    onLongClick = { onLongPress(note) },
                )
            }
        }
        if (pinned.isNotEmpty() && others.isNotEmpty()) {
            item(key = "section-gap") { Spacer(Modifier.height(14.dp)) }
        }
        items(others, key = { it.id }) { note ->
            JournalCard(
                note = note,
                showTime = showTime,
                onClick = { onOpenNote(note.id) },
                onLongClick = { onLongPress(note) },
            )
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

@Composable
private fun NotePreviewDialog(
    note: Note,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onArchive: () -> Unit,
    onTrash: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(note.title.ifBlank { stringResource(R.string.note_untitled) }) },
        text = {
            Column {
                Text(
                    text = note.body.ifBlank { stringResource(R.string.editor_hint) },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 6,
                )
                androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
                Row {
                    androidx.compose.material3.TextButton(onClick = onOpen) {
                        Text(stringResource(R.string.preview_open))
                    }
                    androidx.compose.material3.TextButton(onClick = onArchive) {
                        Text(stringResource(R.string.preview_archive))
                    }
                    androidx.compose.material3.TextButton(onClick = onTrash) {
                        Text(
                            text = stringResource(R.string.preview_delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
@Composable
private fun PreviewHost(
    note: Note?,
    onDismiss: () -> Unit,
    onOpen: (String) -> Unit,
    onArchive: (String) -> Unit,
    onTrash: (String) -> Unit,
) {
    if (note == null) return
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(note.title.ifBlank { stringResource(R.string.note_untitled) }) },
        text = {
            Column {
                Text(
                    text = note.body.ifBlank { stringResource(R.string.editor_hint) },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 6,
                )
                androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
                Row {
                    androidx.compose.material3.TextButton(onClick = { onOpen(note.id) }) {
                        Text(stringResource(R.string.preview_open))
                    }
                    androidx.compose.material3.TextButton(onClick = { onArchive(note.id) }) {
                        Text(stringResource(R.string.preview_archive))
                    }
                    androidx.compose.material3.TextButton(onClick = { onTrash(note.id) }) {
                        Text(stringResource(R.string.preview_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}