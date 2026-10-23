package com.tokinote.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tokinote.app.data.AppViewModel
import com.tokinote.app.data.JournalFile
import com.tokinote.app.ui.components.FloatingNavBar
import com.tokinote.app.ui.focus.FocusScreen
import com.tokinote.app.ui.focus.FocusViewModel
import com.tokinote.app.ui.map.LocationPickerScreen
import com.tokinote.app.ui.notes.CalendarScreen
import com.tokinote.app.ui.notes.DocumentPreviewScreen
import com.tokinote.app.ui.notes.JournalEditorScreen
import com.tokinote.app.ui.notes.MediaViewerScreen
import com.tokinote.app.ui.notes.TimelineScreen
import com.tokinote.app.ui.settings.AboutScreen
import com.tokinote.app.ui.settings.AppearanceSettings
import com.tokinote.app.ui.settings.DataSettings
import com.tokinote.app.ui.settings.SettingsScreen
import com.tokinote.app.ui.settings.TimerSettings

@Composable
fun TokiNoteApp(viewModel: AppViewModel) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val labels by viewModel.labels.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()

    val focusViewModel: FocusViewModel = viewModel()
    val focusState by focusViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(settings) { focusViewModel.configure(settings) }
    LaunchedEffect(focusViewModel) {
        focusViewModel.focusCompleted.collect { minutes -> viewModel.recordFocusSession(minutes) }
    }

    val todayMinutes = remember(sessions) {
        sessions.filter { isToday(it.endedAt) }.sumOf { it.minutes }
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
        is Dest.Editor, Dest.Calendar, is Dest.Preview, is Dest.MediaViewer, is Dest.LocationPicker -> Dest.Notes
        else -> current
    }
    val showNavBar = current.isTopLevel() || current is Dest.SettingsDetail

    fun goBack() {
        val popped = backStack.removeAt(backStack.lastIndex)
        if (popped is Dest.Editor) viewModel.deleteIfBlank(popped.noteId)
    }

    fun openNote(noteId: String) {
        backStack.add(Dest.Editor(noteId))
    }

    BackHandler(enabled = backStack.size > 1) { goBack() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            AnimatedVisibility(
                visible = showNavBar,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
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
                )
            }
        },
    ) { padding ->
        AnimatedContent(
            targetState = current,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) },
            label = "tokinote-screen",
        ) { dest ->
            when (dest) {
                Dest.Notes -> TimelineScreen(
                    notes = notes,
                    contentPadding = padding,
                    onOpenNote = ::openNote,
                    onCreateNote = {
                        val note = viewModel.createNote()
                        openNote(note.id)
                    },
                    onOpenCalendar = { backStack.add(Dest.Calendar) },
                    onOpenFile = { file -> backStack.add(Dest.Preview(file.path)) },
                )

                Dest.Focus -> FocusScreen(
                    state = focusState,
                    todayMinutes = todayMinutes,
                    contentPadding = padding,
                    onToggle = focusViewModel::toggle,
                    onReset = focusViewModel::reset,
                    onSkip = focusViewModel::skip,
                    onOpenTimerSettings = { backStack.add(Dest.SettingsDetail(SettingsPage.Timer)) },
                )

                Dest.Settings -> SettingsScreen(
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
                            contentPadding = padding,
                            onBack = { goBack() },
                            onChange = viewModel::updateNote,
                            onDelete = {
                                viewModel.deleteNote(note.id)
                                goBack()
                            },
                            onCreateLabel = viewModel::addLabel,
                            onDeleteLabel = viewModel::deleteLabel,
                            onPickLocation = { backStack.add(Dest.LocationPicker(note.id)) },
                            onOpenFile = { file -> backStack.add(Dest.Preview(file.path)) },
                            onOpenMedia = { _, index -> backStack.add(Dest.MediaViewer(note.id, index)) },
                        )
                    }
                }

                Dest.Calendar -> CalendarScreen(
                    notes = notes,
                    contentPadding = padding,
                    onBack = { goBack() },
                    onOpenNote = ::openNote,
                )

                is Dest.Preview -> {
                    val file = notes.flatMap { it.files }.firstOrNull { it.path == dest.path }
                        ?: JournalFile(
                            name = dest.path.substringAfterLast('/'),
                            path = dest.path,
                            mime = "",
                            size = 0L,
                        )
                    DocumentPreviewScreen(file = file, contentPadding = padding, onBack = { goBack() })
                }

                is Dest.MediaViewer -> {
                    val note = notes.firstOrNull { it.id == dest.noteId }
                    if (note == null) {
                        LaunchedEffect(dest) {
                            if (backStack.lastOrNull() == dest) backStack.removeAt(backStack.lastIndex)
                        }
                    } else {
                        MediaViewerScreen(paths = note.media, initialIndex = dest.index, onBack = { goBack() })
                    }
                }

                is Dest.LocationPicker -> LocationPickerScreen(
                    contentPadding = padding,
                    onBack = { goBack() },
                    onConfirm = { location ->
                        viewModel.setLocation(dest.noteId, location)
                        goBack()
                    },
                )

                is Dest.SettingsDetail -> when (dest.page) {
                    SettingsPage.Appearance -> AppearanceSettings(
                        settings = settings,
                        contentPadding = padding,
                        onBack = { goBack() },
                        onUpdate = viewModel::updateSettings,
                    )

                    SettingsPage.Timer -> TimerSettings(
                        settings = settings,
                        contentPadding = padding,
                        onBack = { goBack() },
                        onUpdate = viewModel::updateSettings,
                    )

                    SettingsPage.Data -> DataSettings(
                        noteCount = notes.size,
                        contentPadding = padding,
                        onBack = { goBack() },
                        exportJson = viewModel::exportJson,
                        onClearAll = viewModel::clearAll,
                    )

                    SettingsPage.About -> AboutScreen(
                        contentPadding = padding,
                        onBack = { goBack() },
                    )
                }
            }
        }
    }
}
