package com.tokinote.app.ui.focus

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.tokinote.app.R
import com.tokinote.app.TokiNoteApplication
import com.tokinote.app.data.AppSettings
import kotlinx.coroutines.flow.StateFlow

enum class FocusMode(val labelRes: Int) {
    Focus(R.string.focus_title),
    ShortBreak(R.string.focus_short_break),
    LongBreak(R.string.focus_long_break),
}

fun FocusMode.minutes(settings: AppSettings): Int = when (this) {
    FocusMode.Focus -> settings.focusMinutes
    FocusMode.ShortBreak -> settings.shortBreakMinutes
    FocusMode.LongBreak -> settings.longBreakMinutes
}

fun nextFocusMode(mode: FocusMode, completedInCycle: Int, sessionsBeforeLongBreak: Int): FocusMode {
    val cycle = sessionsBeforeLongBreak.coerceAtLeast(1)
    return when (mode) {
        FocusMode.Focus -> if ((completedInCycle + 1) % cycle == 0) {
            FocusMode.LongBreak
        } else {
            FocusMode.ShortBreak
        }

        else -> FocusMode.Focus
    }
}

data class FocusState(
    val mode: FocusMode = FocusMode.Focus,
    val running: Boolean = false,
    val remainingSeconds: Int = 25 * 60,
    val totalSeconds: Int = 25 * 60,
    val completedInCycle: Int = 0,
    val settings: AppSettings = AppSettings(),
    val indefinite: Boolean = false,
    val elapsedSeconds: Int = 0,
    val phase: FocusPhase = if (running) FocusPhase.Running else FocusPhase.Idle,
    val remainingMillis: Long = remainingSeconds * 1000L,
    val totalMillis: Long = totalSeconds * 1000L,
    val deadlineElapsed: Long = 0L,
    val elapsedBaseMillis: Long = elapsedSeconds * 1000L,
    val runStartedElapsed: Long = 0L,
) {
    val progress: Float
        get() = if (totalMillis <= 0L) 0f else {
            (1f - remainingMillis.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)
        }

    val nextMode: FocusMode
        get() = nextFocusMode(mode, completedInCycle, settings.sessionsBeforeLongBreak)

    val sessionIndex: Int
        get() {
            val cycle = settings.sessionsBeforeLongBreak.coerceAtLeast(1)
            return completedInCycle % cycle + 1
        }
}

/**
 * 界面上的计时入口。真正的时钟在进程级 [FocusTimerEngine] 里，
 * 这样旋转、离开页面甚至 Activity 重建都不会把正在走的表清掉。
 */
class FocusViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = (application as TokiNoteApplication).focusEngine

    val state: StateFlow<FocusState> = engine.state

    fun configure(settings: AppSettings) = engine.configure(settings)

    fun toggle() = engine.toggle()

    fun reset() = engine.reset()

    fun skip() = engine.skip()

    fun startIndefinite() = engine.startIndefinite()

    /** 长按数字后等退场动画结束再开始正向计时。延迟放在引擎里，旋转不会把这次启动弄丢。 */
    fun scheduleIndefinite(delayMs: Long = 430L) = engine.scheduleIndefinite(delayMs)

    fun sync() = engine.sync()
}
