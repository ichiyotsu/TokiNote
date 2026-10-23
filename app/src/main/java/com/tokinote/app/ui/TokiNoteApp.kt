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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tokinote.app.R
import com.tokinote.app.data.AppViewModel
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
import com.tokinote.app.ui.settings.DataSettings
import com.tokinote.app.ui.settings.SettingsScreen

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
    LaunchedEffect(focusViewModel) {
        focusViewModel.breakCompleted.collect { minutes -> viewModel.recordBreakSession(minutes) }
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
        Dest.Timer, Dest.FocusSummary -> Dest.Focus
        is Dest.Editor, Dest.Calendar, is Dest.MediaViewer -> Dest.Notes
        else -> current
    }
    val showNavBar = current.isTopLevel() || current is Dest.SettingsDetail
    val motion = LocalMotionEnabled.current

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
                    action = if (selectedTab == Dest.Notes) {
                        {
                            NavActionFor(
                                tab = selectedTab,
                                onNewNote = {
                                    val note = viewModel.createNote()
                                    openNote(note.id)
                                },
                            )
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
                    showTime = settings.showTime,
                    contentPadding = padding,
                    onOpenNote = ::openNote,
                    onOpenCalendar = { backStack.add(Dest.Calendar) },
                    onOpenMedia = { noteId, index -> backStack.add(Dest.MediaViewer(noteId, index)) },
                    onOpenFocusStats = { backStack.add(Dest.FocusSummary) },
                    onTodoToggle = viewModel::toggleDone,
                )

                Dest.Focus -> FocusScreen(
                    state = focusState,
                    todayMinutes = todayMinutes,
                    contentPadding = padding,
                    onToggle = focusViewModel::toggle,
                    onReset = focusViewModel::reset,
                    onSkip = focusViewModel::skip,
                    onOpenTimerSettings = { backStack.add(Dest.Timer) },
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
                            showTime = settings.showTime,
                            contentPadding = padding,
                            onBack = { goBack() },
                            onChange = viewModel::updateNote,
                            onDelete = {
                                viewModel.deleteNote(note.id)
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
                    onBack = { goBack() },
                    onOpenNote = ::openNote,
                )

                Dest.FocusSummary -> FocusSummaryScreen(
                    sessions = sessions,
                    contentPadding = padding,
                    onBack = { goBack() },
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

                is Dest.SettingsDetail -> when (dest.page) {
                    SettingsPage.Appearance -> AppearanceSettings(
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

@Composable
private fun NavActionFor(
    tab: Dest,
    onNewNote: () -> Unit,
) {
    when (tab) {
        Dest.Notes -> NavActionButton(
            icon = Icons.Filled.Add,
            contentDescription = stringResource(R.string.entry_new),
            onClick = onNewNote,
        )

        else -> Unit
    }
}
