package com.tokinote.app.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tokinote.app.R
import com.tokinote.app.data.AppSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

data class FocusState(
    val mode: FocusMode = FocusMode.Focus,
    val running: Boolean = false,
    val remainingSeconds: Int = 25 * 60,
    val totalSeconds: Int = 25 * 60,
    val completedInCycle: Int = 0,
    val settings: AppSettings = AppSettings(),
    val indefinite: Boolean = false,
    val elapsedSeconds: Int = 0,
) {
    val progress: Float
        get() = if (totalSeconds <= 0) 0f else {
            (1f - remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
        }

    val nextMode: FocusMode
        get() = when (mode) {
            FocusMode.Focus -> if ((completedInCycle + 1) % settings.sessionsBeforeLongBreak == 0) {
                FocusMode.LongBreak
            } else {
                FocusMode.ShortBreak
            }

            else -> FocusMode.Focus
        }

    val sessionIndex: Int
        get() = completedInCycle % settings.sessionsBeforeLongBreak + 1
}

/**
 * 番茄钟状态机，行为对齐 Tomato：
 * 专注结束进入短休息，每完成 N 段进入长休息，可自动开始下一段。
 */
class FocusViewModel : ViewModel() {

    private val _state = MutableStateFlow(FocusState())
    val state: StateFlow<FocusState> = _state.asStateFlow()

    private val _completed = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    val focusCompleted: SharedFlow<Int> = _completed.asSharedFlow()

    private val _breakCompleted = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    val breakCompleted: SharedFlow<Int> = _breakCompleted.asSharedFlow()

    private var ticker: Job? = null

    fun configure(settings: AppSettings) {
        val current = _state.value
        if (current.settings == settings) return
        val total = current.mode.minutes(settings) * 60
        _state.value = if (current.running) {
            current.copy(settings = settings, totalSeconds = total)
        } else {
            current.copy(settings = settings, totalSeconds = total, remainingSeconds = total)
        }
    }

    fun toggle() {
        if (_state.value.running) pause() else if (_state.value.indefinite) resumeIndefinite() else start()
    }

    /** 长按数字进入不定时专注：从 0 开始正向计时。 */
    fun startIndefinite() {
        stopTicker()
        _state.value = _state.value.copy(
            indefinite = true,
            running = true,
            elapsedSeconds = 0,
            remainingSeconds = 0,
            totalSeconds = 0,
        )
        tickUp()
    }

    private fun resumeIndefinite() {
        _state.value = _state.value.copy(running = true)
        tickUp()
    }

    private fun tickUp() {
        stopTicker()
        ticker = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val current = _state.value
                if (!current.running || !current.indefinite) break
                _state.value = current.copy(elapsedSeconds = current.elapsedSeconds + 1)
            }
        }
    }

    fun reset() {
        stopTicker()
        val current = _state.value
        val total = current.mode.minutes(current.settings) * 60
        _state.value = current.copy(
            running = false,
            indefinite = false,
            elapsedSeconds = 0,
            totalSeconds = total,
            remainingSeconds = total,
        )
    }

    fun skip() {
        stopTicker()
        val current = _state.value
        val next = when (current.mode) {
            FocusMode.Focus -> {
                if (current.completedInCycle > 0 &&
                    current.completedInCycle % current.settings.sessionsBeforeLongBreak == 0
                ) {
                    FocusMode.LongBreak
                } else {
                    FocusMode.ShortBreak
                }
            }

            else -> FocusMode.Focus
        }
        val total = next.minutes(current.settings) * 60
        _state.value = current.copy(
            mode = next,
            running = false,
            totalSeconds = total,
            remainingSeconds = total,
        )
    }

    private fun start() {
        if (_state.value.remainingSeconds <= 0) return
        _state.value = _state.value.copy(running = true)
        stopTicker()
        ticker = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val current = _state.value
                if (!current.running) break
                if (current.remainingSeconds > 1) {
                    _state.value = current.copy(remainingSeconds = current.remainingSeconds - 1)
                } else {
                    finishSegment()
                    break
                }
            }
        }
    }

    private fun pause() {
        stopTicker()
        _state.value = _state.value.copy(running = false)
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private fun finishSegment() {
        stopTicker()
        val current = _state.value
        val focusMinutes = current.totalSeconds / 60

        if (current.mode == FocusMode.Focus) {
            _completed.tryEmit(focusMinutes)
            val next = current.nextMode
            val total = next.minutes(current.settings) * 60
            _state.value = current.copy(
                mode = next,
                completedInCycle = current.completedInCycle + 1,
                totalSeconds = total,
                remainingSeconds = total,
                running = current.settings.autoStartNext,
            )
        } else {
            _breakCompleted.tryEmit(current.totalSeconds / 60)
            val next = FocusMode.Focus
            val total = next.minutes(current.settings) * 60
            _state.value = current.copy(
                mode = next,
                totalSeconds = total,
                remainingSeconds = total,
                running = current.settings.autoStartNext,
            )
        }

        if (_state.value.running) start()
    }
}
