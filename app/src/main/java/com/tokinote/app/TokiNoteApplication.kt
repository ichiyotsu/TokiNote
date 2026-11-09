package com.tokinote.app

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.tokinote.app.data.TokiStore
import com.tokinote.app.ui.focus.FocusNotifications
import com.tokinote.app.ui.focus.FocusTimerEngine
import com.tokinote.app.ui.focus.FocusTimerService
import com.tokinote.app.ui.focus.FocusWidgetUpdater
import com.tokinote.app.ui.focus.SharedPrefsFocusTimerPersistence
import com.tokinote.app.ui.focus.SystemFocusClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TokiNoteApplication : Application() {
    lateinit var store: TokiStore
        private set

    lateinit var focusEngine: FocusTimerEngine
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        store = TokiStore(this)
        focusEngine = FocusTimerEngine(
            clock = SystemFocusClock,
            persistence = SharedPrefsFocusTimerPersistence(this),
            scope = appScope,
            initialSettings = store.loadSettings(),
        )
        focusEngine.sessionRecorder = { session -> store.appendSession(session) }
        FocusNotifications.ensureChannels(this)
        appScope.launch(start = CoroutineStart.UNDISPATCHED) {
            focusEngine.events.collect { event ->
                FocusNotifications.showCompleted(this@TokiNoteApplication, event)
            }
        }
        appScope.launch(start = CoroutineStart.UNDISPATCHED) {
            var serviceRunning = false
            var widgetKey = ""
            focusEngine.state.collect { state ->
                if (state.running != serviceRunning) {
                    serviceRunning = state.running
                    if (state.running) {
                        FocusTimerService.start(this@TokiNoteApplication)
                    } else {
                        FocusTimerService.stop(this@TokiNoteApplication)
                    }
                }
                val key = listOf(
                    state.phase,
                    state.mode,
                    state.indefinite,
                    state.deadlineElapsed,
                    state.runStartedElapsed,
                    state.elapsedBaseMillis,
                    if (state.running) 0L else state.remainingMillis,
                    state.totalMillis,
                ).joinToString()
                if (key != widgetKey) {
                    widgetKey = key
                    FocusWidgetUpdater.updateAll(this@TokiNoteApplication, state)
                }
            }
        }
        focusEngine.sync()
    }
}
