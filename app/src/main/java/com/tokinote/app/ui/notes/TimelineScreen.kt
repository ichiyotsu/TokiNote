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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokinote.app.R
import com.tokinote.app.data.AlbumFolder
import com.tokinote.app.data.AppSettings
import com.tokinote.app.data.Label
import com.tokinote.app.data.Note
import com.tokinote.app.data.NoteGroup
import androidx.compose.ui.draw.blur
import com.tokinote.app.ui.theme.tokiGlass
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val albumFolderDate = DateTimeFormatter.ofPattern("M月d日创建", Locale.CHINA)

@Composable
fun TimelineScreen(
    notes: List<Note>,
    groups: List<NoteGroup>,
    folders: List<AlbumFolder>,
    labels: List<Label>,
    settings: AppSettings,
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
    onCreateFolder: (String, Set<String>) -> AlbumFolder?,
    onUpdateFolder: (String, String, Set<String>) -> Boolean,
    onDeleteFolder: (String) -> Unit,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onPreviewVisibilityChange: (Boolean) -> Unit = {},
    onDrawerVisibilityChange: (Boolean) -> Unit = {},
) {
    var tab by rememberSaveable { mutableStateOf(HomeTab.Notes) }
    var query by rememberSaveable { mutableStateOf("") }
    var viewMode by rememberSaveable { mutableStateOf(HomeViewMode.List) }
    var sortAscending by rememberSaveable { mutableStateOf(false) }
    var selectedGroupId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedFolderId by rememberSaveable { mutableStateOf<String?>(null) }
    var drawerOpen by remember { mutableStateOf(false) }
    var profileOpen by remember { mutableStateOf(false) }
    var groupSheetOpen by rememberSaveable { mutableStateOf(false) }
    var editingGroupId by rememberSaveable { mutableStateOf<String?>(null) }
    var folderSheetOpen by rememberSaveable { mutableStateOf(false) }
    var editingFolderId by rememberSaveable { mutableStateOf<String?>(null) }
    var previewNote by remember { mutableStateOf<Note?>(null) }
    var previewBounds by remember { mutableStateOf<Rect?>(null) }
    var topBarElevated by remember { mutableStateOf(false) }
    val backdropProgress by animateFloatAsState(
        targetValue = if (topBarElevated) 1f else 0f,
        animationSpec = tween(180, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "topBarBackdrop",
    )

    val comparator = remember(sortAscending) {
        if (sortAscending) {
            compareBy<Note> { it.updatedAt }
        } else {
            compareByDescending<Note> { it.updatedAt }
        }
    }
    val activeNotes = remember(notes, comparator) {
        notes.filter { !it.archived && it.deletedAt == null }.sortedWith(comparator)
    }
    val selectedGroup = groups.firstOrNull { it.id == selectedGroupId }
    val groupNotes = remember(activeNotes, selectedGroup) {
        if (selectedGroup == null) activeNotes else activeNotes.filter { it.id in selectedGroup.noteIds }
    }
    val searchedNotes = remember(groupNotes, query) {
        if (query.isBlank()) {
            groupNotes
        } else {
            groupNotes.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.body.contains(query, ignoreCase = true)
            }
        }
    }
    val allPhotos = remember(activeNotes, query) {
        val matching = if (query.isBlank()) {
            activeNotes
        } else {
            activeNotes.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.body.contains(query, ignoreCase = true)
            }
        }
        matching.flatMap { note ->
            note.media.mapIndexed { index, path ->
                AlbumPhoto(
                    noteId = note.id,
                    path = path,
                    index = index,
                    date = note.journalDate,
                    noteTitle = note.title,
                )
            }
        }.let { photos ->
            if (sortAscending) photos.sortedBy { it.date } else photos.sortedByDescending { it.date }
        }
    }
    val selectedFolder = folders.firstOrNull { it.id == selectedFolderId }
    val visiblePhotos = remember(allPhotos, selectedFolder) {
        if (selectedFolder == null) allPhotos else allPhotos.filter { it.path in selectedFolder.paths }
    }

    var headerHeight by remember { mutableIntStateOf(0) }
    val headerInset = with(LocalDensity.current) { headerHeight.toDp() }

    val hazeState = rememberHazeState()
    val headerTint = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
    val headerFallback = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    val glass = Modifier.tokiGlass(
        hazeState = hazeState,
        blurRadius = 52.dp,
        tints = listOf(headerTint),
        fallback = headerFallback,
    )
    val drawerBlur by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (drawerOpen) 24.dp else 0.dp,
        animationSpec = tween(200),
        label = "drawerBlur",
    )

    LaunchedEffect(previewNote) {
        onPreviewVisibilityChange(previewNote != null)
    }
    LaunchedEffect(drawerOpen) {
        onDrawerVisibilityChange(drawerOpen)
    }
    LaunchedEffect(tab) {
        if (tab != HomeTab.Notes) topBarElevated = false
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            onPreviewVisibilityChange(false)
            onDrawerVisibilityChange(false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(drawerBlur)
                .hazeSource(hazeState),
        ) {
            when (tab) {
                HomeTab.Notes -> NoteFeed(
                    notes = searchedNotes,
                    labels = labels,
                    showTime = settings.showTime,
                    viewMode = viewMode,
                    contentPadding = contentPadding,
                    topInset = headerInset,
                    listTitle = when {
                        query.isNotBlank() -> stringResource(R.string.search_results)
                        selectedGroup != null -> selectedGroup.name
                        else -> stringResource(R.string.drawer_all_notes)
                    },
                    onOpenNote = onOpenNote,
                    onLongPress = { note, bounds ->
                        previewNote = note
                        previewBounds = bounds
                    },
                    onScrollChanged = {
                        topBarElevated = it
                    },
                )

                HomeTab.Album -> AlbumContent(
                    photos = visiblePhotos,
                    viewMode = viewMode,
                    contentPadding = contentPadding,
                    topInset = headerInset,
                    title = selectedFolder?.name ?: stringResource(R.string.drawer_all_photos),
                    subtitle = selectedFolder?.let {
                        Instant.ofEpochMilli(it.createdAt)
                            .atZone(ZoneId.systemDefault())
                            .format(albumFolderDate)
                    },
                    onOpenMedia = onOpenMedia,
                )

                HomeTab.Stats -> CalendarScreen(
                    notes = searchedNotes,
                    contentPadding = contentPadding,
                    topInset = headerInset,
                    onOpenNote = onOpenNote,
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .onSizeChanged { headerHeight = it.height },
        ) {
            TopSearchBar(
                query = query,
                onQueryChange = { query = it },
                viewMode = viewMode,
                onToggleView = {
                    viewMode = if (viewMode == HomeViewMode.List) {
                        HomeViewMode.Grid
                    } else {
                        HomeViewMode.List
                    }
                },
                sortAscending = sortAscending,
                onToggleSort = { sortAscending = !sortAscending },
                avatarEmoji = settings.avatarEmoji,
                avatarPath = settings.avatarPath,
                backdropProgress = backdropProgress,
                showViewSort = tab != HomeTab.Stats,
                onMenu = { drawerOpen = true },
                onAvatar = { profileOpen = true },
                glass = glass,
            )
        }
    }

    GroupDrawer(
        open = drawerOpen,
        selectedTab = tab,
        groups = groups,
        folders = folders,
        selectedGroupId = selectedGroupId,
        selectedFolderId = selectedFolderId,
        activeNotes = activeNotes,
        photos = allPhotos,
        topInset = contentPadding.calculateTopPadding(),
        onDismiss = { drawerOpen = false },
        onSelectTab = {
            tab = it
            drawerOpen = false
        },
        onSelectGroup = {
            tab = HomeTab.Notes
            selectedGroupId = it
            drawerOpen = false
        },
        onSelectFolder = {
            tab = HomeTab.Album
            selectedFolderId = it
            drawerOpen = false
        },
        onCreateGroup = {
            editingGroupId = null
            groupSheetOpen = true
            drawerOpen = false
        },
        onEditGroup = {
            editingGroupId = it.id
            groupSheetOpen = true
            drawerOpen = false
        },
        onCreateFolder = {
            editingFolderId = null
            folderSheetOpen = true
            drawerOpen = false
        },
        onEditFolder = {
            editingFolderId = it.id
            folderSheetOpen = true
            drawerOpen = false
        },
    )

    if (profileOpen) {
        ProfileSheet(
            settings = settings,
            onUpdate = onUpdateSettings,
            onDismiss = { profileOpen = false },
        )
    }

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
                    onCreateGroup(name, noteIds)?.let { selectedGroupId = it.id }
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

    if (folderSheetOpen) {
        AlbumFolderSheet(
            folder = editingFolderId?.let { id -> folders.firstOrNull { it.id == id } },
            photos = allPhotos,
            existingNames = folders.map { it.name },
            onDismiss = {
                folderSheetOpen = false
                editingFolderId = null
            },
            onSave = { name, paths ->
                val editing = editingFolderId
                if (editing == null) {
                    val created = onCreateFolder(name, paths)
                    if (created != null) {
                        tab = HomeTab.Album
                        selectedFolderId = created.id
                        true
                    } else {
                        false
                    }
                } else {
                    onUpdateFolder(editing, name, paths)
                }
            },
            onDelete = editingFolderId?.let { id ->
                {
                    onDeleteFolder(id)
                    if (selectedFolderId == id) selectedFolderId = null
                }
            },
        )
    }

    previewNote?.let { note ->
        NotePreviewOverlay(
            note = note,
            labels = labels,
            sourceBounds = previewBounds,
            onDismiss = {
                previewNote = null
                previewBounds = null
            },
            onOpen = { id ->
                previewNote = null
                previewBounds = null
                onOpenNote(id)
            },
            onArchive = { id ->
                previewNote = null
                previewBounds = null
                onArchiveNote(id)
            },
            onTrash = { id ->
                previewNote = null
                previewBounds = null
                onTrashNote(id)
            },
        )
    }
}

@Composable
private fun NoteFeed(
    notes: List<Note>,
    labels: List<Label>,
    showTime: Boolean,
    viewMode: HomeViewMode,
    contentPadding: PaddingValues,
    topInset: androidx.compose.ui.unit.Dp,
    listTitle: String,
    onOpenNote: (String) -> Unit,
    onLongPress: (Note, Rect) -> Unit,
    onScrollChanged: (Boolean) -> Unit,
) {
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val scrolled = if (viewMode == HomeViewMode.Grid) {
        gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 4
    } else {
        listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 4
    }
    LaunchedEffect(scrolled) { onScrollChanged(scrolled) }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { onScrollChanged(false) }
    }
    if (notes.isEmpty()) {
        EmptyHint(stringResource(R.string.no_entries))
        return
    }
    val pinned = notes.filter { it.pinned }
    val others = notes.filterNot { it.pinned }
    val padding = PaddingValues(
        start = 16.dp,
        end = 16.dp,
        top = topInset + 4.dp,
        bottom = contentPadding.calculateBottomPadding() + 120.dp,
    )

    if (viewMode == HomeViewMode.Grid) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            state = gridState,
            contentPadding = padding,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (pinned.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader(stringResource(R.string.section_pinned))
                }
                items(pinned, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        labels = labels,
                        showTime = showTime,
                        compact = true,
                        onClick = { onOpenNote(note.id) },
                        onLongPress = { rect -> onLongPress(note, rect) },
                    )
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(listTitle)
            }
            items(others, key = { it.id }) { note ->
                NoteCard(
                    note = note,
                    labels = labels,
                    showTime = showTime,
                    compact = true,
                    onClick = { onOpenNote(note.id) },
                    onLongPress = { rect -> onLongPress(note, rect) },
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (pinned.isNotEmpty()) {
                item(key = "pinned-header") {
                    SectionHeader(stringResource(R.string.section_pinned))
                }
                items(pinned, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        labels = labels,
                        showTime = showTime,
                        compact = false,
                        onClick = { onOpenNote(note.id) },
                        onLongPress = { rect -> onLongPress(note, rect) },
                    )
                }
            }
            item(key = "all-header") {
                SectionHeader(listTitle)
            }
            items(others, key = { it.id }) { note ->
                NoteCard(
                    note = note,
                    labels = labels,
                    showTime = showTime,
                    compact = false,
                    onClick = { onOpenNote(note.id) },
                    onLongPress = { rect -> onLongPress(note, rect) },
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, letterSpacing = 0.2.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 2.dp),
    )
}

@Composable
private fun AlbumContent(
    photos: List<AlbumPhoto>,
    viewMode: HomeViewMode,
    contentPadding: PaddingValues,
    topInset: androidx.compose.ui.unit.Dp,
    title: String,
    subtitle: String?,
    onOpenMedia: (String, Int) -> Unit,
) {
    if (photos.isEmpty()) {
        EmptyHint(stringResource(R.string.album_empty))
        return
    }
    val padding = PaddingValues(
        start = 16.dp,
        end = 16.dp,
        top = topInset + 4.dp,
        bottom = contentPadding.calculateBottomPadding() + 120.dp,
    )
    if (viewMode == HomeViewMode.Grid) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                AlbumHeader(title, subtitle, photos.size)
            }
            items(photos, key = { it.path }) { photo ->
                val image = rememberImage(photo.path)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { onOpenMedia(photo.noteId, photo.index) },
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
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "album-header") {
                AlbumHeader(title, subtitle, photos.size)
            }
            items(photos, key = { it.path }) { photo ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .clickable { onOpenMedia(photo.noteId, photo.index) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val image = rememberImage(photo.path)
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
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
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = photo.noteTitle.ifBlank { stringResource(R.string.note_untitled) },
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = photo.date.dayLabel(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumHeader(title: String, subtitle: String?, count: Int) {
    Row(
        modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.entry_count, count),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
        if (subtitle != null) {
            Spacer(Modifier.weight(1f))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
