package com.ichiyotsu.tokinote

import android.app.Activity
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.edit
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ichiyotsu.tokinote.ui.NoteAccents
import com.ichiyotsu.tokinote.ui.TokiNoteTheme
import com.ichiyotsu.tokinote.ui.noteTone
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.function.Consumer

private enum class MainTab(val label: String) {
    NOTES("笔记"), TAGS("标签"), TIMELINE("时间线"), FOCUS("专注")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = AndroidColor.TRANSPARENT
        window.navigationBarColor = AndroidColor.TRANSPARENT
        setContent { TokiNoteApp() }
    }
}

@Composable
private fun TokiNoteApp() {
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) { context.getSharedPreferences("tokinote_ui", Activity.MODE_PRIVATE) }
    var darkTheme by rememberSaveable { mutableStateOf(preferences.getBoolean("dark_theme", false)) }
    var notes by remember { mutableStateOf(emptyList<Note>()) }
    var loaded by remember { mutableStateOf(false) }
    var activeFilter by rememberSaveable { mutableStateOf(NoteFilter.ALL) }
    var selectedTag by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var showMobileEditor by rememberSaveable { mutableStateOf(false) }
    var mainTab by rememberSaveable { mutableStateOf(MainTab.NOTES) }
    var query by rememberSaveable { mutableStateOf("") }
    var showTagDialog by remember { mutableStateOf(false) }
    var deletedNote by remember { mutableStateOf<Pair<Note, Int>?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val activity = LocalActivity.current

    LaunchedEffect(Unit) {
        notes = loadNotes(context)
        selectedId = notes.firstOrNull { !it.archived }?.id
        loaded = true
    }
    LaunchedEffect(notes, loaded) {
        if (loaded) {
            delay(320)
            saveNotes(context, notes)
        }
    }
    SideEffect {
        activity?.window?.let { window ->
            WindowInsetsControllerCompat(window, window.decorView).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    val allTags = remember(notes) { notes.filterNot { it.archived }.flatMap { it.tags }.distinct().sorted() }
    val visibleNotes = remember(notes, activeFilter, selectedTag, query) {
        notes.asSequence()
            .filter { note ->
                when (activeFilter) {
                    NoteFilter.ALL -> !note.archived
                    NoteFilter.FAVORITES -> !note.archived && note.favorite
                    NoteFilter.ARCHIVE -> note.archived
                }
            }
            .filter { note -> selectedTag == null || selectedTag in note.tags }
            .filter { note ->
                query.isBlank() || listOf(note.title, note.body, note.tags.joinToString(" "))
                    .any { it.contains(query.trim(), ignoreCase = true) }
            }
            .sortedByDescending { it.updatedAt }
            .toList()
    }
    val selectedNote = notes.firstOrNull { it.id == selectedId }

    fun updateNote(updated: Note) {
        val stamped = updated.copy(updatedAt = System.currentTimeMillis())
        notes = notes.map { if (it.id == updated.id) stamped else it }
    }

    fun createNote(tag: String? = null) {
        val timestamp = System.currentTimeMillis()
        val note = Note(
            id = UUID.randomUUID().toString(), title = "", body = "", tags = listOfNotNull(tag),
            tone = notes.size % 5, favorite = false, archived = false,
            createdAt = timestamp, updatedAt = timestamp
        )
        notes = listOf(note) + notes
        selectedId = note.id
        selectedTag = null
        activeFilter = NoteFilter.ALL
        query = ""
        showMobileEditor = true
    }

    fun changeFilter(filter: NoteFilter) {
        activeFilter = filter
        selectedTag = null
        val next = notes.filter { note ->
            when (filter) {
                NoteFilter.ALL -> !note.archived
                NoteFilter.FAVORITES -> !note.archived && note.favorite
                NoteFilter.ARCHIVE -> note.archived
            }
        }
        if (next.none { it.id == selectedId }) selectedId = next.firstOrNull()?.id
        showMobileEditor = false
    }

    fun chooseTag(tag: String?) {
        selectedTag = tag
        if (tag != null) activeFilter = NoteFilter.ALL
        val next = notes.filter { !it.archived && (tag == null || tag in it.tags) }
        if (next.none { it.id == selectedId }) selectedId = next.firstOrNull()?.id
        showMobileEditor = false
    }

    fun deleteNote(note: Note) {
        val index = notes.indexOfFirst { it.id == note.id }
        if (index < 0) return
        deletedNote = note to index
        notes = notes.filterNot { it.id == note.id }
        if (selectedId == note.id) selectedId = notes.firstOrNull { !it.archived }?.id
        showMobileEditor = false
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "笔记已删除",
                actionLabel = "撤销",
                withDismissAction = true
            )
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                deletedNote?.let { (restore, restoreAt) ->
                    notes = notes.toMutableList().apply { add(restoreAt.coerceIn(0, size), restore) }
                    selectedId = restore.id
                    deletedNote = null
                }
            } else {
                deletedNote = null
            }
        }
    }

    fun toggleFavorite(note: Note) = updateNote(note.copy(favorite = !note.favorite))

    fun toggleArchive(note: Note) {
        val updated = note.copy(archived = !note.archived, updatedAt = System.currentTimeMillis())
        notes = notes.map { if (it.id == note.id) updated else it }
        if (updated.archived && selectedId == note.id) {
            selectedId = notes.firstOrNull { it.id != note.id && !it.archived }?.id
            showMobileEditor = false
        }
        if (!updated.archived && activeFilter == NoteFilter.ARCHIVE) activeFilter = NoteFilter.ALL
    }

    fun addTagToCurrent(tagValue: String) {
        val tag = tagValue.trim().removePrefix("#")
        if (tag.isBlank()) return
        val current = selectedNote
        if (current != null) {
            updateNote(current.copy(tags = (current.tags + tag).distinct()))
        } else {
            createNote(tag)
        }
    }

    BackHandler(enabled = showMobileEditor) { showMobileEditor = false }
    BackHandler(enabled = mainTab == MainTab.FOCUS && !showMobileEditor) { mainTab = MainTab.NOTES }

    TokiNoteTheme(darkTheme = darkTheme) {
        BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            val compact = maxWidth < 1040.dp
            AmbientBackdrop(darkTheme = darkTheme)
            val safePadding = WindowInsets.safeDrawing.asPaddingValues()
            Box(
                Modifier.fillMaxSize()
                    .padding(safePadding)
                    .padding(if (compact) 8.dp else 14.dp)
                    .clip(RoundedCornerShape(if (compact) 25.dp else 31.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = if (darkTheme) .80f else .72f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f), RoundedCornerShape(if (compact) 25.dp else 31.dp))
            ) {
                if (!loaded) {
                    LoadingState()
                } else if (mainTab == MainTab.FOCUS) {
                    FocusScreen(darkTheme = darkTheme, onBack = { mainTab = MainTab.NOTES })
                } else if (compact) {
                    if (showMobileEditor && selectedNote != null) {
                        NoteEditorPane(
                            note = selectedNote,
                            compact = true,
                            darkTheme = darkTheme,
                            onBack = { showMobileEditor = false },
                            onChange = ::updateNote,
                            onFavorite = ::toggleFavorite,
                            onArchive = ::toggleArchive,
                            onDelete = ::deleteNote
                        )
                    } else {
                        when (mainTab) {
                            MainTab.NOTES -> PhoneNotesHome(
                                notes = visibleNotes,
                                allTags = allTags,
                                activeFilter = activeFilter,
                                selectedTag = selectedTag,
                                selectedId = selectedId,
                                query = query,
                                darkTheme = darkTheme,
                                onQueryChange = { query = it },
                                onFilter = ::changeFilter,
                                onTag = ::chooseTag,
                                onNew = { createNote() },
                                onAddTag = { showTagDialog = true },
                                onSelect = { note -> selectedId = note.id; showMobileEditor = true },
                                onFavorite = ::toggleFavorite,
                                onArchive = ::toggleArchive,
                                onTheme = {
                                    darkTheme = !darkTheme
                                    preferences.edit { putBoolean("dark_theme", darkTheme) }
                                }
                            )
                            MainTab.TAGS -> TagExplorePage(
                                tags = allTags,
                                notes = notes.filterNot { it.archived },
                                selectedTag = selectedTag,
                                onAddTag = { showTagDialog = true },
                                onTag = { tag -> chooseTag(tag); mainTab = MainTab.NOTES }
                            )
                            MainTab.TIMELINE -> TimelinePage(
                            notes = visibleNotes,
                            darkTheme = darkTheme,
                            onSelect = { note -> selectedId = note.id; showMobileEditor = true },
                            onFavorite = ::toggleFavorite,
                            onArchive = ::toggleArchive
                            )
                            MainTab.FOCUS -> Unit
                        }
                    }
                } else {
                    when (mainTab) {
                        MainTab.NOTES -> DesktopNotesHome(
                            notes = visibleNotes,
                            allNotes = notes,
                            allTags = allTags,
                            activeFilter = activeFilter,
                            selectedTag = selectedTag,
                            selectedId = selectedId,
                            selectedNote = selectedNote,
                            query = query,
                            darkTheme = darkTheme,
                            onQueryChange = { query = it },
                            onFilter = ::changeFilter,
                            onTag = ::chooseTag,
                            onNew = { createNote() },
                            onAddTag = { showTagDialog = true },
                            onSelect = { selectedId = it.id },
                            onChange = ::updateNote,
                            onFavorite = ::toggleFavorite,
                            onArchive = ::toggleArchive,
                            onDelete = ::deleteNote,
                            onTheme = {
                                darkTheme = !darkTheme
                                preferences.edit { putBoolean("dark_theme", darkTheme) }
                            },
                            onFocus = { mainTab = MainTab.FOCUS }
                        )
                        MainTab.TAGS -> TagExplorePage(
                            tags = allTags,
                            notes = notes.filterNot { it.archived },
                            selectedTag = selectedTag,
                            onAddTag = { showTagDialog = true },
                            onTag = { tag -> chooseTag(tag); mainTab = MainTab.NOTES }
                        )
                        MainTab.TIMELINE -> TimelinePage(
                            notes = visibleNotes,
                            darkTheme = darkTheme,
                            onSelect = { note -> selectedId = note.id; mainTab = MainTab.NOTES },
                            onFavorite = ::toggleFavorite,
                            onArchive = ::toggleArchive
                        )
                        MainTab.FOCUS -> Unit
                    }
                }
            }
            if (loaded && !showMobileEditor && mainTab != MainTab.FOCUS && (compact || mainTab != MainTab.NOTES)) {
                RootNavigationBar(
                    selected = mainTab,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onSelect = { mainTab = it }
                )
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + if (compact && mainTab != MainTab.FOCUS && !showMobileEditor) 88.dp else 18.dp,
                    start = 18.dp,
                    end = 18.dp
                )
            )
            if (showTagDialog) {
                AddTagDialog(
                    onDismiss = { showTagDialog = false },
                    onSave = { tag -> addTagToCurrent(tag); showTagDialog = false }
                )
            }
        }
    }
}

@Composable
private fun AmbientBackdrop(darkTheme: Boolean) {
    val transition = rememberInfiniteTransition(label = "soft-glow")
    val drift by transition.animateFloat(
        initialValue = -18f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(11_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow-drift"
    )
    val lilac = if (darkTheme) Color(0xFF6954A5).copy(alpha = .25f) else Color(0xFFD9CDF7).copy(alpha = .66f)
    val peach = if (darkTheme) Color(0xFF9A665B).copy(alpha = .17f) else Color(0xFFF2D8C9).copy(alpha = .58f)
    val mint = if (darkTheme) Color(0xFF447A68).copy(alpha = .17f) else Color(0xFFCFE6DC).copy(alpha = .43f)
    val blurLayer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(44.dp) else Modifier
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.offset { IntOffset(x = (drift - 20).dp.roundToPx(), y = (-100).dp.roundToPx()) }.size(390.dp)
                .then(blurLayer)
                .background(Brush.radialGradient(listOf(lilac, lilac.copy(alpha = 0f))), CircleShape)
        )
        Box(
            Modifier.align(Alignment.BottomStart).offset(x = (-115).dp, y = 85.dp).size(390.dp)
                .then(blurLayer)
                .background(Brush.radialGradient(listOf(peach, peach.copy(alpha = 0f))), CircleShape)
        )
        Box(
            Modifier.align(Alignment.BottomEnd).offset(x = 110.dp, y = 115.dp).size(310.dp)
                .then(blurLayer)
                .background(Brush.radialGradient(listOf(mint, mint.copy(alpha = 0f))), CircleShape)
        )
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BrandMark(54.dp)
            Spacer(Modifier.height(14.dp))
            Text("正在打开你的笔记…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BrandMark(size: androidx.compose.ui.unit.Dp = 40.dp) {
    Row(
        modifier = Modifier.size(size).shadow(8.dp, RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp, bottomEnd = 15.dp, bottomStart = 6.dp))
            .clip(RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp, bottomEnd = 15.dp, bottomStart = 6.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF927BEA), Color(0xFF7054D3))))
            .padding(horizontal = size * .29f),
        horizontalArrangement = Arrangement.spacedBy(size * .085f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(size * .075f).height(size * .29f).clip(CircleShape).background(Color.White.copy(alpha = .92f)).offset(y = 2.dp))
        Box(Modifier.width(size * .075f).height(size * .46f).clip(CircleShape).background(Color.White))
        Box(Modifier.width(size * .075f).height(size * .33f).clip(CircleShape).background(Color.White.copy(alpha = .92f)).offset(y = (-2).dp))
    }
}

@Composable
private fun BrandLockup(compact: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BrandMark(if (compact) 34.dp else 39.dp)
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("toki", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-.6).sp))
                Text("note", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, letterSpacing = (-.6).sp))
            }
            Text("留住此刻的想法", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
        }
    }
}

@Composable
private fun PhoneNotesHome(
    notes: List<Note>,
    allTags: List<String>,
    activeFilter: NoteFilter,
    selectedTag: String?,
    selectedId: String?,
    query: String,
    darkTheme: Boolean,
    onQueryChange: (String) -> Unit,
    onFilter: (NoteFilter) -> Unit,
    onTag: (String?) -> Unit,
    onNew: () -> Unit,
    onAddTag: () -> Unit,
    onSelect: (Note) -> Unit,
    onFavorite: (Note) -> Unit,
    onArchive: (Note) -> Unit,
    onTheme: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 18.dp, end = 13.dp, top = 13.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BrandLockup(compact = true)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onAddTag) { Icon(Icons.Default.Add, contentDescription = "新建标签") }
                IconButton(onClick = onTheme) {
                    Icon(if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = "切换主题")
                }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 19.dp)) {
                Text(todayText(), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(7.dp))
                Text("把灵感，", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-.6).sp))
                Text("轻轻记下。", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = (-.6).sp))
                Text("一页一念，慢慢整理属于你的日常。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
                SearchField(query = query, onQueryChange = onQueryChange)
                Spacer(Modifier.height(9.dp))
                FilterStrip(activeFilter = activeFilter, selectedTag = selectedTag, tags = allTags, onFilter = onFilter, onTag = onTag)
                Row(Modifier.fillMaxWidth().padding(top = 15.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(if (selectedTag != null) "# $selectedTag" else selectedTagForHeader(activeFilter), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("${notes.size} 篇灵感，在这里慢慢生长", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onAddTag) { Icon(Icons.AutoMirrored.Filled.Label, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("标签") }
                }
            }
            if (notes.isEmpty()) {
                EmptyNotesState(Modifier.weight(1f).fillMaxWidth(), activeFilter)
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(start = 15.dp, end = 15.dp, top = 3.dp, bottom = 170.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            selected = note.id == selectedId,
                            darkTheme = darkTheme,
                            onClick = { onSelect(note) },
                            onFavorite = { onFavorite(note) },
                            onArchive = { onArchive(note) },
                            compact = true
                        )
                    }
                }
            }
        }
        androidx.compose.material3.ExtendedFloatingActionButton(
            onClick = onNew,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 84.dp),
            shape = RoundedCornerShape(19.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("新建笔记", fontWeight = FontWeight.SemiBold) }
        )
    }
}

@Composable
private fun DesktopNotesHome(
    notes: List<Note>,
    allNotes: List<Note>,
    allTags: List<String>,
    activeFilter: NoteFilter,
    selectedTag: String?,
    selectedId: String?,
    selectedNote: Note?,
    query: String,
    darkTheme: Boolean,
    onQueryChange: (String) -> Unit,
    onFilter: (NoteFilter) -> Unit,
    onTag: (String?) -> Unit,
    onNew: () -> Unit,
    onAddTag: () -> Unit,
    onSelect: (Note) -> Unit,
    onChange: (Note) -> Unit,
    onFavorite: (Note) -> Unit,
    onArchive: (Note) -> Unit,
    onDelete: (Note) -> Unit,
    onTheme: () -> Unit,
    onFocus: () -> Unit
) {
    Row(Modifier.fillMaxSize()) {
        SideNavigation(
            allNotes = allNotes,
            tags = allTags,
            activeFilter = activeFilter,
            selectedTag = selectedTag,
            onFilter = onFilter,
            onTag = onTag,
            onNew = onNew,
            onAddTag = onAddTag
        )
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 27.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("我的空间", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("  /  ", color = MaterialTheme.colorScheme.outline)
                Text(selectedTag ?: activeFilter.label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                Spacer(Modifier.weight(1f))
                SearchField(query = query, onQueryChange = onQueryChange, modifier = Modifier.width(245.dp))
                Spacer(Modifier.width(9.dp))
                IconButton(onClick = onFocus) {
                    Icon(Icons.Default.Timer, contentDescription = "打开专注")
                }
                IconButton(onClick = onTheme) {
                    Icon(if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = "切换主题")
                }
                Surface(shape = CircleShape, color = BrushColorAvatar) {
                    Box(Modifier.size(33.dp), contentAlignment = Alignment.Center) {
                        Text("t", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .58f))
            WelcomeBanner(Modifier.fillMaxWidth())
            Row(Modifier.fillMaxSize()) {
                NoteListPanel(
                    notes = notes,
                    selectedId = selectedId,
                    filterLabel = selectedTag ?: activeFilter.label,
                    darkTheme = darkTheme,
                    modifier = Modifier.width(344.dp).fillMaxHeight(),
                    onSelect = onSelect,
                    onFavorite = onFavorite,
                    onArchive = onArchive,
                    onNew = onNew
                )
                VerticalDividerThin()
                NoteEditorPane(
                    note = selectedNote,
                    compact = false,
                    darkTheme = darkTheme,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onBack = {},
                    onChange = onChange,
                    onFavorite = onFavorite,
                    onArchive = onArchive,
                    onDelete = onDelete
                )
            }
        }
    }
}

private val BrushColorAvatar = Color(0xFFEDE4F6)

@Composable
private fun SideNavigation(
    allNotes: List<Note>,
    tags: List<String>,
    activeFilter: NoteFilter,
    selectedTag: String?,
    onFilter: (NoteFilter) -> Unit,
    onTag: (String?) -> Unit,
    onNew: () -> Unit,
    onAddTag: () -> Unit
) {
    Column(
        Modifier.width(236.dp).fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = .58f))
            .padding(horizontal = 15.dp, vertical = 20.dp)
    ) {
        BrandLockup()
        Spacer(Modifier.height(24.dp))
        FilledTonalButton(
            onClick = onNew,
            modifier = Modifier.fillMaxWidth().height(47.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
            contentPadding = PaddingValues(horizontal = 13.dp)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("写一篇新笔记", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
        Text("工作区", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 10.dp, top = 24.dp, bottom = 8.dp))
        NavigationRow("全部笔记", allNotes.count { !it.archived }, Icons.Default.Description, activeFilter == NoteFilter.ALL && selectedTag == null) { onFilter(NoteFilter.ALL) }
        NavigationRow("我喜欢的", allNotes.count { it.favorite && !it.archived }, Icons.Default.Star, activeFilter == NoteFilter.FAVORITES) { onFilter(NoteFilter.FAVORITES) }
        NavigationRow("归档", allNotes.count { it.archived }, Icons.Default.Archive, activeFilter == NoteFilter.ARCHIVE) { onFilter(NoteFilter.ARCHIVE) }
        Row(Modifier.fillMaxWidth().padding(start = 10.dp, top = 22.dp, end = 2.dp, bottom = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("我的标签", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.05.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onAddTag, modifier = Modifier.size(29.dp)) { Icon(Icons.Default.Add, "新建标签", modifier = Modifier.size(17.dp)) }
        }
        if (tags.isEmpty()) {
            Text("写几篇笔记，标签会出现在这里。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 10.dp, top = 6.dp))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                tags.take(9).forEachIndexed { index, tag ->
                    TagNavigationRow(tag, allNotes.count { !it.archived && tag in it.tags }, selectedTag == tag, NoteAccents[index % NoteAccents.size]) { onTag(tag) }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .7f))
        Row(Modifier.padding(start = 9.dp, top = 15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(7.dp).background(Color(0xFF68AD8E), CircleShape))
            Text("只保存在此设备", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("简单一点，想法就有地方安放。", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(start = 9.dp, top = 8.dp, bottom = 4.dp))
    }
}

@Composable
private fun NavigationRow(label: String, count: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .8f) else Color.Transparent
    val foreground = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(13.dp)).background(container).clickable(onClick = onClick).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(17.dp), tint = foreground)
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal), color = foreground)
        Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = foreground.copy(alpha = .72f))
    }
}

@Composable
private fun TagNavigationRow(tag: String, count: Int, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(34.dp).clip(RoundedCornerShape(11.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .65f) else Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(Modifier.size(7.dp).background(accent, CircleShape))
        Text(tag, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun WelcomeBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.height(133.dp).background(
            Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = .34f), Color.Transparent, Color(0xFFF7E8DE).copy(alpha = .25f)))
        ).padding(horizontal = 30.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(todayText(), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("把灵感，", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-.8).sp))
                Text("轻轻记下。", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = (-.8).sp))
            }
            Text("一页一念，慢慢整理属于你的日常。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 5.dp))
        }
        Surface(
            modifier = Modifier.width(142.dp).height(84.dp).graphicsLayer { rotationZ = 3f },
            shape = RoundedCornerShape(topStart = 15.dp, topEnd = 28.dp, bottomEnd = 15.dp, bottomStart = 24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = .56f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .48f)),
            shadowElevation = 5.dp
        ) {
            Box(Modifier.padding(14.dp)) {
                Text("✳", modifier = Modifier.align(Alignment.TopEnd), color = MaterialTheme.colorScheme.tertiary, fontSize = 16.sp)
                Text("little thoughts,\nbig beginnings", modifier = Modifier.align(Alignment.CenterStart), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, lineHeight = 15.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(Modifier.align(Alignment.BottomStart).width(48.dp).height(1.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .35f)))
            }
        }
    }
}

@Composable
private fun NoteListPanel(
    notes: List<Note>,
    selectedId: String?,
    filterLabel: String,
    darkTheme: Boolean,
    modifier: Modifier,
    onSelect: (Note) -> Unit,
    onFavorite: (Note) -> Unit,
    onArchive: (Note) -> Unit,
    onNew: () -> Unit
) {
    Column(modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = .34f)).padding(start = 17.dp, end = 14.dp, top = 18.dp, bottom = 12.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(filterLabel, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(if (notes.isEmpty()) "这里还没有笔记" else "${notes.size} 篇灵感正在这里", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
            }
            TextButton(onClick = onNew) { Icon(Icons.Default.Add, null, modifier = Modifier.size(17.dp)); Text("新建", style = MaterialTheme.typography.labelMedium) }
        }
        Spacer(Modifier.height(10.dp))
        if (notes.isEmpty()) {
            EmptyNotesState(Modifier.fillMaxSize(), NoteFilter.ALL)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 8.dp)) {
                items(notes, key = { it.id }) { note ->
                    NoteCard(
                        note = note, selected = selectedId == note.id, darkTheme = darkTheme,
                        onClick = { onSelect(note) }, onFavorite = { onFavorite(note) }, onArchive = { onArchive(note) }, compact = false
                    )
                }
            }
        }
    }
}

@Composable
private fun VerticalDividerThin() {
    Box(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f)))
}

@Composable
private fun NoteCard(
    note: Note,
    selected: Boolean,
    darkTheme: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onArchive: () -> Unit,
    compact: Boolean
) {
    val shape = RoundedCornerShape(if (compact) 18.dp else 17.dp)
    val tone = noteTone(note.tone, darkTheme)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().then(if (selected) Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .45f), shape) else Modifier),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.surface.copy(alpha = .96f) else MaterialTheme.colorScheme.surface.copy(alpha = .70f)),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .2f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 3.dp else 0.dp)
    ) {
        Row {
            Box(Modifier.width(3.dp).fillMaxHeight().defaultMinSize(minHeight = if (compact) 104.dp else 125.dp).background(NoteAccents[note.tone.coerceIn(0, 4)].copy(alpha = .8f)))
            Column(Modifier.weight(1f).padding(start = 13.dp, end = 10.dp, top = 10.dp, bottom = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(9.dp), color = tone.copy(alpha = .7f)) {
                        Box(Modifier.size(if (compact) 25.dp else 24.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Description, null, modifier = Modifier.size(14.dp), tint = NoteAccents[note.tone.coerceIn(0, 4)])
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text(shortTime(note.updatedAt), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.outline)
                    IconButton(onClick = onFavorite, modifier = Modifier.size(29.dp)) {
                        Icon(if (note.favorite) Icons.Default.Star else Icons.Default.StarBorder, contentDescription = if (note.favorite) "取消喜欢" else "加入喜欢", modifier = Modifier.size(16.dp), tint = if (note.favorite) Color(0xFFC2942C) else MaterialTheme.colorScheme.outline)
                    }
                }
                Text(
                    text = note.title.ifBlank { "无标题笔记" },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp, end = 2.dp)
                )
                Text(
                    text = note.body.replace('\n', ' ').ifBlank { "写下第一句话，让这个想法开始生长。" },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 16.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row(Modifier.fillMaxWidth().padding(top = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(fullTime(note.updatedAt), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.weight(1f))
                    note.tags.take(2).forEach { tag ->
                        Surface(Modifier.padding(start = 4.dp), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .58f)) {
                            Text(tag, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteEditorPane(
    note: Note?,
    compact: Boolean,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onChange: (Note) -> Unit,
    onFavorite: (Note) -> Unit,
    onArchive: (Note) -> Unit,
    onDelete: (Note) -> Unit
) {
    if (note == null) {
        EmptyEditorState(modifier.fillMaxSize())
        return
    }
    Column(modifier.fillMaxSize().padding(horizontal = if (compact) 21.dp else 34.dp, vertical = if (compact) 11.dp else 23.dp)) {
        Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
            if (compact) {
                IconButton(onClick = onBack, modifier = Modifier.size(34.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                Spacer(Modifier.width(7.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.weight(1f)) {
                Box(Modifier.size(7.dp).background(Color(0xFF68AD8E), CircleShape))
                Text("已保存在此设备", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { onFavorite(note) }, modifier = Modifier.size(34.dp)) {
                Icon(if (note.favorite) Icons.Default.Star else Icons.Default.StarBorder, if (note.favorite) "取消喜欢" else "喜欢", tint = if (note.favorite) Color(0xFFC2942C) else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(19.dp))
            }
            IconButton(onClick = { onArchive(note) }, modifier = Modifier.size(34.dp)) {
                Icon(if (note.archived) Icons.Default.Unarchive else Icons.Default.Archive, if (note.archived) "取消归档" else "归档", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { onDelete(note) }, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Delete, "删除笔记", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = if (compact) 16.dp else 20.dp, bottom = 4.dp)) {
            BasicTextField(
                value = note.title,
                onValueChange = { onChange(note.copy(title = it)) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-.7).sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 36.sp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    Box {
                        if (note.title.isEmpty()) Text("给这篇笔记起个名字…", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline.copy(alpha = .65f)))
                        inner()
                    }
                }
            )
            Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(fullTime(note.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Box(Modifier.padding(horizontal = 9.dp).size(3.dp).background(MaterialTheme.colorScheme.outlineVariant, CircleShape))
                TagLine(note = note, onChange = onChange)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .66f))
            Spacer(Modifier.height(15.dp))
            BasicTextField(
                value = note.body,
                onValueChange = { onChange(note.copy(body = it)) },
                modifier = Modifier.fillMaxWidth().heightIn(min = if (compact) 310.dp else 360.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, lineHeight = 28.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    Box {
                        if (note.body.isEmpty()) Text("从一个念头开始写……\n\n这里没有格式要求，也不必一次写完。", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, lineHeight = 28.sp, color = MaterialTheme.colorScheme.outline.copy(alpha = .75f)))
                        inner()
                    }
                }
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f))
        Row(Modifier.fillMaxWidth().padding(top = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("一点颜色", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(9.dp))
            NoteTones.forEachIndexed { index, color ->
                Box(
                    Modifier.padding(end = 8.dp).size(21.dp).clip(CircleShape).background(color).border(
                        width = if (index == note.tone) 2.dp else 1.dp,
                        color = if (index == note.tone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    ).clickable { onChange(note.copy(tone = index)) },
                    contentAlignment = Alignment.Center
                ) {
                    if (index == note.tone) Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.weight(1f))
            val characterCount = note.body.replace("\\s".toRegex(), "").length
            Text("$characterCount 字", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

private val NoteTones = listOf(
    Color(0xFFB6A3E8), Color(0xFFE8AC96), Color(0xFF8EC4AD), Color(0xFFE0C356), Color(0xFF8BB2D1)
)

@Composable
private fun TagLine(note: Note, onChange: (Note) -> Unit) {
    var draft by remember(note.id) { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        note.tags.forEach { tag ->
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .72f)) {
                Row(Modifier.padding(start = 7.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(tag, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = { onChange(note.copy(tags = note.tags - tag)) }, modifier = Modifier.size(19.dp)) { Icon(Icons.Default.Close, "移除标签 $tag", modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.primary) }
                }
            }
        }
        BasicTextField(
            value = draft,
            onValueChange = { incoming ->
                val delimiter = incoming.contains(',') || incoming.contains('，') || incoming.contains('\n')
                if (delimiter) {
                    val newTags = incoming.split(',', '，', '\n').map { it.trim().removePrefix("#") }.filter(String::isNotBlank)
                    if (newTags.isNotEmpty()) onChange(note.copy(tags = (note.tags + newTags).distinct()))
                    draft = ""
                } else draft = incoming
            },
            modifier = Modifier.widthIn(min = 84.dp, max = 145.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                val tag = draft.trim().removePrefix("#")
                if (tag.isNotEmpty()) onChange(note.copy(tags = (note.tags + tag).distinct()))
                draft = ""
                keyboard?.hide()
            }),
            decorationBox = { inner ->
                Box {
                    if (draft.isEmpty()) Text("添加标签…", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.outline)
                    inner()
                }
            }
        )
    }
}

@Composable
private fun PhoneEditorPlaceholder(onNew: () -> Unit) {
    EmptyEditorState(Modifier.fillMaxSize(), onNew)
}

@Composable
private fun EmptyEditorState(modifier: Modifier = Modifier, onNew: (() -> Unit)? = null) {
    Box(modifier.padding(25.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = RoundedCornerShape(27.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .75f), shadowElevation = 7.dp) {
                Box(Modifier.size(78.dp), contentAlignment = Alignment.Center) { Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp)) }
            }
            Spacer(Modifier.height(16.dp))
            Text("每个想法，都值得被记住。", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text("选一篇笔记继续写，或创建一篇新的，把此刻的灵感安放在这里。", modifier = Modifier.widthIn(max = 260.dp).padding(top = 6.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
            if (onNew != null) {
                Spacer(Modifier.height(15.dp))
                Button(onClick = onNew, shape = RoundedCornerShape(15.dp)) { Icon(Icons.Default.Add, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Text("写一篇新笔记") }
            }
        }
    }
}

@Composable
private fun EmptyNotesState(modifier: Modifier, filter: NoteFilter) {
    Box(modifier.padding(20.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = RoundedCornerShape(19.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .72f)) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                when (filter) { NoteFilter.ALL -> "这里还空着呢"; NoteFilter.FAVORITES -> "还没有喜欢的笔记"; NoteFilter.ARCHIVE -> "归档夹是空的" },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Text("把脑海里的第一件小事，写下来吧。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 5.dp))
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .73f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .62f))
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxSize().padding(horizontal = 11.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { inner ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Search, null, modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.outline)
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) Text("搜索笔记…", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.outline)
                        inner()
                    }
                    if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, "清除搜索", modifier = Modifier.size(14.dp)) }
                }
            }
        )
    }
}

@Composable
private fun FilterStrip(
    activeFilter: NoteFilter,
    selectedTag: String?,
    tags: List<String>,
    onFilter: (NoteFilter) -> Unit,
    onTag: (String?) -> Unit
) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        NoteFilter.entries.forEach { filter ->
            FilterChip(
                selected = activeFilter == filter && selectedTag == null,
                onClick = { onFilter(filter) },
                label = { Text(filter.label, style = MaterialTheme.typography.labelSmall) },
                shape = RoundedCornerShape(12.dp)
            )
        }
        tags.take(5).forEach { tag ->
            FilterChip(
                selected = selectedTag == tag,
                onClick = { onTag(if (selectedTag == tag) null else tag) },
                label = { Text("# $tag", style = MaterialTheme.typography.labelSmall) },
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
private fun AddTagDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var tag by rememberSaveable { mutableStateOf("") }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        NativeDialogBlurEffect()
        Surface(
            modifier = Modifier.fillMaxWidth(.88f).widthIn(max = 420.dp),
            shape = RoundedCornerShape(27.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = .91f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f)),
            tonalElevation = 5.dp,
            shadowElevation = 16.dp
        ) {
            Column(Modifier.padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.Label, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text("新建一个标签", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("让相近的想法更容易找到。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "关闭") }
                }
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 17.dp),
                    label = { Text("标签名称") },
                    placeholder = { Text("例如：灵感、阅读、生活") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
                Row(Modifier.fillMaxWidth().padding(top = 15.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(Modifier.width(6.dp))
                    Button(onClick = { if (tag.isNotBlank()) onSave(tag.trim()) }, enabled = tag.isNotBlank(), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text("添加")
                    }
                }
            }
        }
    }
}

@Composable
private fun NativeDialogBlurEffect() {
    val context = LocalContext.current
    val view = LocalView.current
    val window = (view.parent as? DialogWindowProvider)?.window
    DisposableEffect(window) {
        if (window == null) {
            onDispose { }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val windowManager = context.getSystemService(WindowManager::class.java)
            val listener = Consumer<Boolean> { enabled ->
                window.setBackgroundBlurRadius(if (enabled) 56 else 0)
                window.setDimAmount(if (enabled) .13f else .36f)
                val alpha = if (enabled) 180 else 245
                window.setBackgroundDrawable(AndroidColor.argb(alpha, 248, 245, 241).toDrawable())
            }
            windowManager.addCrossWindowBlurEnabledListener(listener)
            listener.accept(windowManager.isCrossWindowBlurEnabled)
            onDispose {
                windowManager.removeCrossWindowBlurEnabledListener(listener)
                window.setBackgroundBlurRadius(0)
            }
        } else {
            window.setBackgroundDrawable(AndroidColor.TRANSPARENT.toDrawable())
            onDispose { }
        }
    }
}

private fun todayText(): String = SimpleDateFormat("EEEE · M月d日", Locale.SIMPLIFIED_CHINESE).format(Date())

private fun shortTime(timestamp: Long): String {
    val calendar = Calendar.getInstance()
    val today = calendar.get(Calendar.DAY_OF_YEAR)
    val year = calendar.get(Calendar.YEAR)
    calendar.timeInMillis = timestamp
    return when {
        calendar.get(Calendar.YEAR) != year -> SimpleDateFormat("yyyy/M/d", Locale.SIMPLIFIED_CHINESE).format(Date(timestamp))
        calendar.get(Calendar.DAY_OF_YEAR) == today -> "今天"
        calendar.get(Calendar.DAY_OF_YEAR) == today - 1 -> "昨天"
        else -> SimpleDateFormat("M月d日", Locale.SIMPLIFIED_CHINESE).format(Date(timestamp))
    }
}

private fun fullTime(timestamp: Long): String =
    SimpleDateFormat("M月d日  HH:mm", Locale.SIMPLIFIED_CHINESE).format(Date(timestamp))

private fun selectedTagForHeader(filter: NoteFilter): String = when (filter) {
    NoteFilter.ALL -> "全部笔记"
    NoteFilter.FAVORITES -> "我喜欢的"
    NoteFilter.ARCHIVE -> "归档"
}


@Composable
private fun RootNavigationBar(
    selected: MainTab,
    modifier: Modifier = Modifier,
    onSelect: (MainTab) -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Surface(
        modifier = modifier
            .widthIn(max = 480.dp)
            .fillMaxWidth()
            .padding(
                start = 12.dp,
                end = 12.dp,
                top = 9.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 9.dp
            )
            .height(67.dp)
            .shadow(12.dp, shape),
        shape = shape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = .96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f)),
        tonalElevation = 4.dp
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            MainTab.entries.forEach { tab ->
                val active = selected == tab
                val icon = when (tab) {
                    MainTab.NOTES -> Icons.Default.Description
                    MainTab.TAGS -> Icons.AutoMirrored.Filled.Label
                    MainTab.TIMELINE -> Icons.Default.CalendarMonth
                    MainTab.FOCUS -> Icons.Default.Timer
                }
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().clip(CircleShape)
                        .clickable { onSelect(tab) }
                        .padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                    ) {
                        Icon(
                            icon,
                            contentDescription = tab.label,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 3.dp).size(19.dp),
                            tint = if (active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        tab.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal),
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TagExplorePage(
    tags: List<String>,
    notes: List<Note>,
    selectedTag: String?,
    onAddTag: () -> Unit,
    onTag: (String) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 19.dp, end = 13.dp, top = 17.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrandLockup(compact = true)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onAddTag) { Icon(Icons.Default.Add, contentDescription = "新建标签") }
        }
        Text("把相近的想法，放在一起。", modifier = Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Text("用标签整理生活里的小片段", modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 14.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (tags.isEmpty()) {
            EmptyNotesState(Modifier.fillMaxSize(), NoteFilter.ALL)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 15.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(tags, key = { it }) { tag ->
                    val related = notes.filter { tag in it.tags }
                    val accent = NoteAccents[(tags.indexOf(tag)).mod(NoteAccents.size)]
                    Card(
                        onClick = { onTag(tag) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(19.dp),
                        colors = CardDefaults.cardColors(containerColor = if (selectedTag == tag) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .72f) else MaterialTheme.colorScheme.surface.copy(alpha = .82f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f))
                    ) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = .17f)), contentAlignment = Alignment.Center) {
                                Text("#", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = accent)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(tag, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                                Text(related.firstOrNull()?.title?.ifBlank { "无标题笔记" } ?: "从一个新想法开始", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text("${related.size} 篇", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
private fun TimelinePage(
    notes: List<Note>,
    darkTheme: Boolean,
    onSelect: (Note) -> Unit,
    onFavorite: (Note) -> Unit = {},
    onArchive: (Note) -> Unit = {}
) {
    val groups = remember(notes) {
        notes.sortedByDescending { it.updatedAt }.groupBy { shortTime(it.updatedAt) }.toList()
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(todayText(), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("时间线", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .72f)) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(11.dp).size(20.dp))
            }
        }
        Text("每一篇，都留住了当时的自己。", modifier = Modifier.padding(start = 20.dp, bottom = 12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (notes.isEmpty()) {
            EmptyNotesState(Modifier.fillMaxSize(), NoteFilter.ALL)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 15.dp, end = 15.dp, top = 2.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(groups, key = { it.first }) { (day, dayNotes) ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(7.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text(day, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Spacer(Modifier.width(8.dp))
                            HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f))
                        }
                        dayNotes.forEach { note ->
                            NoteCard(
                                note = note,
                                selected = false,
                                darkTheme = darkTheme,
                                onClick = { onSelect(note) },
                                onFavorite = { onFavorite(note) },
                                onArchive = { onArchive(note) },
                                compact = true
                            )
                        }
                    }
                }
            }
        }
    }
}
