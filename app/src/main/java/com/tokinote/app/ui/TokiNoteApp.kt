package com.tokinote.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tokinote.app.R
import com.tokinote.app.data.AppViewModel
import com.tokinote.app.data.FocusComponent
import com.tokinote.app.ui.components.FloatingNavBar
import com.tokinote.app.ui.components.NavActionButton
import com.tokinote.app.ui.focus.FocusScreen
import com.tokinote.app.ui.focus.FocusSummaryScreen
import com.tokinote.app.ui.focus.FocusViewModel
import com.tokinote.app.ui.focus.TimerSettingsScreen
import com.tokinote.app.ui.notes.CalendarScreen
import com.tokinote.app.ui.notes.JournalEditorScreen
import com.tokinote.app.ui.notes.MediaViewerScreen
import com.tokinote.app.ui.notes.TimelineScreen
import com.tokinote.app.ui.settings.AboutScreen
import com.tokinote.app.ui.settings.AppearanceSettings
import com.tokinote.app.ui.settings.ComponentsSettings
import com.tokinote.app.ui.settings.DataSettings
import com.tokinote.app.ui.settings.ArchiveListScreen
import com.tokinote.app.ui.settings.SettingsScreen
import com.tokinote.app.ui.settings.SettingsSidebar
import com.tokinote.app.ui.settings.UpdateAnnouncementHost
import com.tokinote.app.ui.theme.LocalDarkTheme
import com.tokinote.app.ui.theme.LocalDesignStyle
import com.tokinote.app.ui.theme.tokiGlass
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@Composable
fun TokiNoteApp(viewModel: AppViewModel) {
    val context = LocalContext.current
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val labels by viewModel.labels.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    var componentRevision by remember { mutableIntStateOf(0) }
    val focusInstalled = remember(settings.focusComponentEnabled, componentRevision) {
        FocusComponent.installed(context)
    }
    val focusAvailable = settings.focusComponentEnabled && focusInstalled
    val configuration = LocalConfiguration.current
    val wideSettings = configuration.smallestScreenWidthDp >= 600 &&
        configuration.screenWidthDp > configuration.screenHeightDp

    val focusViewModel: FocusViewModel = viewModel()
    val focusState by focusViewModel.state.collectAsStateWithLifecycle()
    val hazeState = rememberHazeState()
    val darkTheme = LocalDarkTheme.current
    val oneUi = LocalDesignStyle.current == com.tokinote.app.data.DesignStyle.OneUI
    val smoke = when {
        oneUi && darkTheme -> Color.Black.copy(alpha = 0.55f)
        oneUi -> Color.White.copy(alpha = 0.55f)
        darkTheme -> Color.Black.copy(alpha = 0.16f)
        else -> Color.Black.copy(alpha = 0.03f)
    }
    val frost = if (oneUi) {
        Color.White.copy(alpha = if (darkTheme) 0.06f else 0.18f)
    } else {
        Color.White.copy(alpha = if (darkTheme) 0.08f else 0.10f)
    }
    val glassFallback = if (oneUi) {
        if (darkTheme) Color.Black.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.72f)
    } else {
        Color.Black.copy(alpha = if (darkTheme) 0.55f else 0.30f)
    }
    val glass = Modifier.tokiGlass(
        hazeState = hazeState,
        blurRadius = if (oneUi) 40.dp else 30.dp,
        tints = listOf(smoke, frost),
        fallback = glassFallback,
    )

    LaunchedEffect(settings) { focusViewModel.configure(settings) }
    LaunchedEffect(focusViewModel) {
        focusViewModel.focusCompleted.collect { minutes -> viewModel.recordFocusSession(minutes) }
    }
    LaunchedEffect(focusViewModel) {
        focusViewModel.breakCompleted.collect { minutes -> viewModel.recordBreakSession(minutes) }
    }

    val backStack = rememberSaveable(
        saver = listSaver(
            save = { stack -> stack.map { it.encode() } },
            restore = { saved ->
                saved.mapNotNull(::decodeDest).toMutableStateList()
                    .takeIf { it.isNotEmpty() }
                    ?: mutableStateListOf(Dest.Notes)
            },
        ),
    ) { mutableStateListOf<Dest>(Dest.Notes) }
    val current = backStack.lastOrNull() ?: Dest.Notes
    val selectedTab: Dest = when (current) {
        is Dest.SettingsDetail -> Dest.Settings
        Dest.Timer, Dest.FocusSummary -> Dest.Focus
        Dest.Archive, Dest.Trash -> Dest.Settings
        is Dest.Editor, Dest.Calendar, is Dest.MediaViewer -> Dest.Notes
        else -> current
    }
    LaunchedEffect(focusAvailable) {
        if (!focusAvailable && backStack.any {
                it == Dest.Focus || it == Dest.FocusSummary || it == Dest.Timer
            }
        ) {
            backStack.clear()
            backStack.add(Dest.Notes)
        }
    }
    var settingsSearch by remember { mutableStateOf(false) }
    var previewOpen by remember { mutableStateOf(false) }
    var drawerOpen by remember { mutableStateOf(false) }
    val showNavBar = (current.isTopLevel() || current is Dest.SettingsDetail) &&
        !previewOpen && !drawerOpen
    val motion = LocalMotionEnabled.current

    fun goBack() {
        val popped = backStack.removeAt(backStack.lastIndex)
        if (popped is Dest.Editor) viewModel.deleteIfBlank(popped.noteId)
    }

    fun openNote(noteId: String) {
        backStack.add(Dest.Editor(noteId))
    }

    BackHandler(enabled = backStack.size > 1) { goBack() }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
            AnimatedVisibility(
                visible = showNavBar,
                enter = slideInVertically(tween(if (motion) TokiMotion.standard else 0)) { it } + fadeIn(
                    tween(if (motion) TokiMotion.standard else 0),
                ),
                exit = slideOutVertically(tween(if (motion) TokiMotion.quick else 0)) { it } + fadeOut(
                    tween(if (motion) TokiMotion.quick else 0),
                ),
            ) {
                FloatingNavBar(
                    current = selectedTab,
                    onSelect = { dest ->
                        while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                        backStack[0] = dest
                    },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp),
                    glass = glass,
                    showFocus = focusAvailable,
                    action = if (selectedTab == Dest.Notes || selectedTab == Dest.Focus || selectedTab == Dest.Settings) {
                        {
                            androidx.compose.runtime.remember(selectedTab) { androidx.compose.animation.core.Animatable(0.55f) }.let { pop ->
                                androidx.compose.runtime.LaunchedEffect(selectedTab) {
                                    pop.animateTo(
                                        1f,
                                        androidx.compose.animation.core.spring(
                                            dampingRatio = 0.5f,
                                            stiffness = 420f,
                                        ),
                                    )
                                }
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = pop.value
                                        scaleY = pop.value
                                        alpha = pop.value.coerceIn(0f, 1f)
                                    },
                                ) {
                            NavActionFor(
                                tab = selectedTab,
                                onNewNote = {
                                    val note = viewModel.createNote()
                                    openNote(note.id)
                                },
                                onOpenStats = { backStack.add(Dest.FocusSummary) },
                                onOpenSettingsSearch = { settingsSearch = true },
                            )
                                }
                            }
                        }
                    } else {
                        null
                    },
                )
            }
        },
    ) { padding ->
        AnimatedContent(
            targetState = current,
            modifier = Modifier.fillMaxSize().hazeSource(hazeState),
            transitionSpec = {
                if (!motion) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    val forward = targetState.depth() >= initialState.depth()
                    if (forward) {
                        (slideInHorizontally(tween(TokiMotion.expressive)) { it / 10 } +
                            fadeIn(tween(TokiMotion.standard))) togetherWith
                            (slideOutHorizontally(tween(TokiMotion.quick)) { -it / 10 } +
                                fadeOut(tween(TokiMotion.quick)))
                    } else {
                        (fadeIn(tween(TokiMotion.standard)) +
                            scaleIn(initialScale = 0.98f, animationSpec = tween(TokiMotion.standard))) togetherWith
                            (slideOutHorizontally(tween(TokiMotion.quick)) { it / 10 } +
                                fadeOut(tween(TokiMotion.quick)))
                    }
                }
            },
            label = "tokinote-screen",
        ) { dest ->
            when (dest) {
                Dest.Notes -> TimelineScreen(
                    notes = notes,
                    groups = groups,
                    folders = folders,
                    labels = labels,
                    settings = settings,
                    contentPadding = padding,
                    onOpenNote = ::openNote,
                    onOpenMedia = { noteId, index -> backStack.add(Dest.MediaViewer(noteId, index)) },
                    onOpenFocusStats = { backStack.add(Dest.FocusSummary) },
                    onArchiveNote = viewModel::archive,
                    onTrashNote = viewModel::trash,
                    onTodoToggle = viewModel::toggleDone,
                    onCreateGroup = viewModel::createGroup,
                    onUpdateGroup = viewModel::updateGroup,
                    onDeleteGroup = viewModel::deleteGroup,
                    onCreateFolder = viewModel::createFolder,
                    onUpdateFolder = viewModel::updateFolder,
                    onDeleteFolder = viewModel::deleteFolder,
                    onUpdateSettings = viewModel::updateSettings,
                    onPreviewVisibilityChange = { previewOpen = it },
                    onDrawerVisibilityChange = { drawerOpen = it },
                )

                Dest.Focus -> FocusScreen(
                    state = focusState,
                    contentPadding = padding,
                    onToggle = focusViewModel::toggle,
                    onReset = focusViewModel::reset,
                    onSkip = focusViewModel::skip,
                    onOpenTimerSettings = { backStack.add(Dest.Timer) },
                    onStartIndefinite = focusViewModel::startIndefinite,
                )

                Dest.Settings -> SettingsScreen(
                    searchActive = settingsSearch,
                    onCloseSearch = { settingsSearch = false },
                    settings = settings,
                    noteCount = notes.size,
                    contentPadding = padding,
                    onOpen = { page -> backStack.add(Dest.SettingsDetail(page)) },
                )

                is Dest.Editor -> {
                    val note = notes.firstOrNull { it.id == dest.noteId }
                    if (note == null) {
                        LaunchedEffect(dest) {
                            if (backStack.lastOrNull() == dest) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        }
                    } else {
                        JournalEditorScreen(
                            note = note,
                            labels = labels,
                            showTime = settings.showTime,
                            contentPadding = padding,
                            onBack = { goBack() },
                            onChange = viewModel::updateNote,
                            onDelete = {
                                viewModel.deleteNote(note.id)
                                goBack()
                            },
                            onArchive = {
                                viewModel.archive(note.id)
                                goBack()
                            },
                            onCreateLabel = viewModel::addLabel,
                            onDeleteLabel = viewModel::deleteLabel,
                            onOpenMedia = { _, index -> backStack.add(Dest.MediaViewer(note.id, index)) },
                        )
                    }
                }

                Dest.Calendar -> CalendarScreen(
                    notes = notes,
                    contentPadding = padding,
                    onOpenNote = ::openNote,
                )

                Dest.FocusSummary -> FocusSummaryScreen(
                    sessions = sessions,
                    contentPadding = padding,
                    onBack = { goBack() },
                )

                Dest.Archive -> ArchiveListScreen(
                    title = stringResource(R.string.archive_title),
                    subtitle = stringResource(R.string.archive_subtitle),
                    notes = notes.filter { it.archived },
                    onBack = { goBack() },
                    onRestore = viewModel::unarchive,
                    onDelete = viewModel::trash,
                    deleteLabel = stringResource(R.string.archive_delete),
                )

                Dest.Trash -> ArchiveListScreen(
                    title = stringResource(R.string.trash_title),
                    subtitle = stringResource(R.string.trash_subtitle),
                    notes = notes.filter { it.deletedAt != null },
                    onBack = { goBack() },
                    onRestore = viewModel::restore,
                    onDelete = viewModel::deletePermanently,
                    deleteLabel = stringResource(R.string.archive_delete_forever),
                )

                Dest.Timer -> TimerSettingsScreen(
                    settings = settings,
                    contentPadding = padding,
                    onBack = { goBack() },
                    onUpdate = viewModel::updateSettings,
                )

                is Dest.MediaViewer -> {
                    val note = notes.firstOrNull { it.id == dest.noteId }
                    if (note == null) {
                        LaunchedEffect(dest) {
                            if (backStack.lastOrNull() == dest) backStack.removeAt(backStack.lastIndex)
                        }
                    } else {
                        MediaViewerScreen(
                            note = note,
                            initialIndex = dest.index,
                            onBack = { goBack() },
                            onOpenNote = { id -> openNote(id) },
                        )
                    }
                }

                is Dest.SettingsDetail -> {
                    val page = dest.page
                    if (wideSettings) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding),
                        ) {
                            SettingsSidebar(
                                selected = page,
                                onSelect = { selected ->
                                    backStack[backStack.lastIndex] = Dest.SettingsDetail(selected)
                                },
                                modifier = Modifier.weight(3f),
                            )
                            Box(
                                modifier = Modifier
                                    .weight(7f)
                                    .fillMaxSize(),
                            ) {
                                when (page) {
                                    SettingsPage.Appearance -> AppearanceSettings(
                                        settings = settings,
                                        contentPadding = PaddingValues(0.dp),
                                        onBack = { goBack() },
                                        onUpdate = viewModel::updateSettings,
                                        showHeader = false,
                                    )

                                    SettingsPage.Components -> ComponentsSettings(
                                        settings = settings,
                                        contentPadding = PaddingValues(0.dp),
                                        onBack = { goBack() },
                                        onUpdate = viewModel::updateSettings,
                                        onComponentChanged = { componentRevision++ },
                                        showHeader = false,
                                    )

                                    SettingsPage.Data -> DataSettings(
                                        noteCount = notes.count { !it.archived && it.deletedAt == null },
                                        archiveCount = notes.count { it.archived },
                                        trashCount = notes.count { it.deletedAt != null },
                                        onOpenArchive = { backStack.add(Dest.Archive) },
                                        onOpenTrash = { backStack.add(Dest.Trash) },
                                        contentPadding = PaddingValues(0.dp),
                                        onBack = { goBack() },
                                        exportJson = viewModel::exportJson,
                                        onClearAll = viewModel::clearAll,
                                        showHeader = false,
                                    )

                                    SettingsPage.About -> AboutScreen(
                                        settings = settings,
                                        onUpdate = viewModel::updateSettings,
                                        contentPadding = PaddingValues(0.dp),
                                        onBack = { goBack() },
                                        showHeader = false,
                                    )
                                }
                            }
                        }
                    } else {
                        when (page) {
                            SettingsPage.Appearance -> AppearanceSettings(
                                settings = settings,
                                contentPadding = padding,
                                onBack = { goBack() },
                                onUpdate = viewModel::updateSettings,
                            )

                            SettingsPage.Components -> ComponentsSettings(
                                settings = settings,
                                contentPadding = padding,
                                onBack = { goBack() },
                                onUpdate = viewModel::updateSettings,
                                onComponentChanged = { componentRevision++ },
                            )

                            SettingsPage.Data -> DataSettings(
                                noteCount = notes.count { !it.archived && it.deletedAt == null },
                                archiveCount = notes.count { it.archived },
                                trashCount = notes.count { it.deletedAt != null },
                                onOpenArchive = { backStack.add(Dest.Archive) },
                                onOpenTrash = { backStack.add(Dest.Trash) },
                                contentPadding = padding,
                                onBack = { goBack() },
                                exportJson = viewModel::exportJson,
                                onClearAll = viewModel::clearAll,
                            )

                            SettingsPage.About -> AboutScreen(
                                settings = settings,
                                onUpdate = viewModel::updateSettings,
                                contentPadding = padding,
                                onBack = { goBack() },
                            )
                        }
                    }
                }
                }
            }
        }
        AnimatedVisibility(
            visible = drawerOpen,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(160)),
            modifier = Modifier.fillMaxSize(),
        ) {
            DrawerSystemBarScrim()
        }
    }

    UpdateAnnouncementHost(channel = settings.updateChannel)
}

@Composable
private fun DrawerSystemBarScrim() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(Color.Black.copy(alpha = 0.42f)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                .background(Color.Black.copy(alpha = 0.42f)),
        )
    }
}

@Composable
private fun NavActionFor(
    tab: Dest,
    onNewNote: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettingsSearch: () -> Unit,
) {
    when (tab) {
        Dest.Notes -> NavActionButton(
            icon = Icons.Filled.Add,
            contentDescription = stringResource(R.string.entry_new),
            onClick = onNewNote,
        )

        Dest.Settings -> NavActionButton(
            icon = Icons.Filled.Search,
            contentDescription = "搜索设置",
            onClick = onOpenSettingsSearch,
        )

        Dest.Focus -> NavActionButton(
            icon = Icons.Filled.BarChart,
            contentDescription = stringResource(R.string.focus_summary),
            onClick = onOpenStats,
        )

        else -> Unit
    }
}
