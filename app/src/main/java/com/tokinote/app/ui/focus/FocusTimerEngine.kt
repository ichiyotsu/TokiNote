package com.tokinote.app.ui.focus

import com.tokinote.app.data.AppSettings
import com.tokinote.app.data.FocusSession
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineScope
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

enum class FocusPhase {
    Idle,
    Running,
    Paused,
}

sealed interface FocusTimerEvent {
    val minutes: Int
    val endedAt: Long

    data class FocusCompleted(override val minutes: Int, override val endedAt: Long) : FocusTimerEvent
    data class BreakCompleted(override val minutes: Int, override val endedAt: Long) : FocusTimerEvent
}

/**
 * 可恢复的计时快照。时间锚点用 elapsedRealtime，重启后 elapsedRealtime 归零时
 * 再用墙钟把锚点换算回来。
 */
data class FocusTimerSnapshot(
    val phase: FocusPhase,
    val mode: FocusMode,
    val indefinite: Boolean,
    val totalMillis: Long,
    val remainingMillis: Long,
    val deadlineElapsed: Long,
    val elapsedBaseMillis: Long,
    val runStartedElapsed: Long,
    val completedInCycle: Int,
    val segmentMinutes: Int,
    val focusMinutes: Int,
    val shortBreakMinutes: Int,
    val longBreakMinutes: Int,
    val sessionsBeforeLongBreak: Int,
    val autoStartNext: Boolean,
    val savedAtElapsed: Long,
    val savedAtWall: Long,
) {
    fun encode(): String = listOf(
        VERSION,
        phase.name,
        mode.name,
        if (indefinite) "1" else "0",
        totalMillis.toString(),
        remainingMillis.toString(),
        deadlineElapsed.toString(),
        elapsedBaseMillis.toString(),
        runStartedElapsed.toString(),
        completedInCycle.toString(),
        segmentMinutes.toString(),
        focusMinutes.toString(),
        shortBreakMinutes.toString(),
        longBreakMinutes.toString(),
        sessionsBeforeLongBreak.toString(),
        if (autoStartNext) "1" else "0",
        savedAtElapsed.toString(),
        savedAtWall.toString(),
    ).joinToString("\n")

    companion object {
        const val VERSION = "1"

        fun decode(raw: String): FocusTimerSnapshot? = runCatching {
            val lines = raw.lines()
            if (lines.size < 18 || lines[0] != VERSION) return null
            FocusTimerSnapshot(
                phase = FocusPhase.valueOf(lines[1]),
                mode = FocusMode.valueOf(lines[2]),
                indefinite = lines[3] == "1",
                totalMillis = lines[4].toLong(),
                remainingMillis = lines[5].toLong(),
                deadlineElapsed = lines[6].toLong(),
                elapsedBaseMillis = lines[7].toLong(),
                runStartedElapsed = lines[8].toLong(),
                completedInCycle = lines[9].toInt(),
                segmentMinutes = lines[10].toInt(),
                focusMinutes = lines[11].toInt(),
                shortBreakMinutes = lines[12].toInt(),
                longBreakMinutes = lines[13].toInt(),
                sessionsBeforeLongBreak = lines[14].toInt(),
                autoStartNext = lines[15] == "1",
                savedAtElapsed = lines[16].toLong(),
                savedAtWall = lines[17].toLong(),
            )
        }.getOrNull()
    }
}

internal fun ceilSeconds(millis: Long): Int {
    if (millis <= 0L) return 0
    return ((millis + 999L) / 1000L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}

/**
 * 专注计时状态机。
 *
 * 倒计时锚定在 [FocusClock.elapsedRealtime] 的截止点上，不定时专注锚定在开始时刻。
 * 每次刷新都用「现在 − 锚点」重算，不靠 delay(1000) 累加，所以休眠、后台和补帧都不会漂移。
 * 截止点错过超过 [LATE_GRACE_MS] 时只结算当前这一段，避免补记用户不在时的整串番茄。
 */
class FocusTimerEngine(
    private val clock: FocusClock,
    private val persistence: FocusTimerPersistence,
    private val scope: CoroutineScope? = null,
    initialSettings: AppSettings = AppSettings(),
    sessionRecorder: (FocusSession) -> Unit = {},
) {
    @Volatile
    var sessionRecorder: (FocusSession) -> Unit = sessionRecorder

    private val lock = Any()
    private val _state = MutableStateFlow(FocusState(settings = initialSettings))
    val state: StateFlow<FocusState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<FocusTimerEvent>(replay = 4, extraBufferCapacity = 8)
    val events: SharedFlow<FocusTimerEvent> = _events.asSharedFlow()

    private var settings: AppSettings = initialSettings
    private var phase: FocusPhase = FocusPhase.Idle
    private var mode: FocusMode = FocusMode.Focus
    private var indefinite: Boolean = false
    private var totalMillis: Long = 0L
    private var remainingMillis: Long = 0L
    private var deadlineElapsed: Long = 0L
    private var elapsedBaseMillis: Long = 0L
    private var runStartedElapsed: Long = 0L
    private var completedInCycle: Int = 0
    private var segmentMinutes: Int = 0
    private var scheduleGeneration: Int = 0
    private var ticker: Job? = null
    private var indefiniteJob: Job? = null

    init {
        val saved = persistence.load()
        if (saved != null) {
            applySnapshot(saved)
        } else {
            installIdleSegment(FocusMode.Focus)
        }
        _state.value = derive(clock.elapsedRealtime())
    }

    fun configure(newSettings: AppSettings) {
        mutate {
            if (newSettings == settings) return@mutate
            val idle = phase == FocusPhase.Idle && !indefinite
            settings = newSettings
            if (idle) installIdleSegment(mode)
            commitLocked()
        }
    }

    fun toggle() {
        mutate { events ->
            invalidateScheduleLocked()
            if (phase == FocusPhase.Running) pauseLocked(events) else resumeOrStartLocked()
        }
    }

    fun pause() {
        mutate { events -> pauseLocked(events) }
    }

    fun reset() {
        mutate {
            invalidateScheduleLocked()
            stopTickerLocked()
            indefinite = false
            elapsedBaseMillis = 0L
            runStartedElapsed = 0L
            deadlineElapsed = 0L
            phase = FocusPhase.Idle
            installIdleSegment(mode)
            commitLocked()
        }
    }

    fun skip() {
        mutate {
            invalidateScheduleLocked()
            stopTickerLocked()
            mode = nextFocusMode(mode, completedInCycle, settings.sessionsBeforeLongBreak)
            indefinite = false
            elapsedBaseMillis = 0L
            runStartedElapsed = 0L
            deadlineElapsed = 0L
            phase = FocusPhase.Idle
            installIdleSegment(mode)
            commitLocked()
        }
    }

    fun startIndefinite() {
        mutate { startIndefiniteLocked() }
    }

    fun scheduleIndefinite(delayMs: Long = INDEFINITE_ANIMATION_MS) {
        val jobScope = scope
        synchronized(lock) {
            scheduleGeneration += 1
            val generation = scheduleGeneration
            indefiniteJob?.cancel()
            if (delayMs <= 0L || jobScope == null) {
                startIndefiniteLocked()
                return
            }
            indefiniteJob = jobScope.launch {
                delay(delayMs)
                mutate {
                    if (generation != scheduleGeneration) return@mutate
                    startIndefiniteLocked()
                }
            }
        }
    }

    /** 立刻按当前时钟重算。回到前台、进程恢复后都调用一次，补上后台流逝的时间。 */
    fun sync() {
        val events = mutableListOf<FocusTimerEvent>()
        val running = synchronized(lock) {
            catchUpLocked(clock.elapsedRealtime(), events)
            phase == FocusPhase.Running
        }
        dispatch(events)
        if (running) {
            synchronized(lock) { ensureTickerLocked() }
        }
    }

    private fun applySnapshot(saved: FocusTimerSnapshot) {
        settings = settings.copy(
            focusMinutes = saved.focusMinutes.coerceAtLeast(1),
            shortBreakMinutes = saved.shortBreakMinutes.coerceAtLeast(1),
            longBreakMinutes = saved.longBreakMinutes.coerceAtLeast(1),
            sessionsBeforeLongBreak = saved.sessionsBeforeLongBreak.coerceAtLeast(1),
            autoStartNext = saved.autoStartNext,
        )
        phase = saved.phase
        mode = saved.mode
        indefinite = saved.indefinite
        totalMillis = saved.totalMillis.coerceAtLeast(0L)
        remainingMillis = saved.remainingMillis.coerceAtLeast(0L)
        elapsedBaseMillis = saved.elapsedBaseMillis.coerceAtLeast(0L)
        completedInCycle = saved.completedInCycle.coerceAtLeast(0)
        segmentMinutes = saved.segmentMinutes.coerceAtLeast(0)
        deadlineElapsed = saved.deadlineElapsed
        runStartedElapsed = saved.runStartedElapsed

        val nowElapsed = clock.elapsedRealtime()
        val nowWall = clock.wallClockMillis()
        val rebooted = nowElapsed + REBOOT_SKEW_MS < saved.savedAtElapsed
        if (!rebooted) return
        if (deadlineElapsed != 0L) {
            val deadlineWall = saved.savedAtWall + (saved.deadlineElapsed - saved.savedAtElapsed)
            deadlineElapsed = nowElapsed + (deadlineWall - nowWall)
        }
        if (runStartedElapsed != 0L) {
            val startWall = saved.savedAtWall + (saved.runStartedElapsed - saved.savedAtElapsed)
            runStartedElapsed = nowElapsed + (startWall - nowWall)
        }
    }

    private fun installIdleSegment(target: FocusMode) {
        mode = target
        val minutes = minutesOf(target)
        val total = minutes * 60_000L
        totalMillis = total
        remainingMillis = total
        segmentMinutes = minutes
    }

    private fun resumeOrStartLocked() {
        val now = clock.elapsedRealtime()
        if (indefinite) {
            phase = FocusPhase.Running
            runStartedElapsed = now
            commitLocked()
            ensureTickerLocked()
            return
        }
        if (phase == FocusPhase.Paused && remainingMillis > 0L) {
            deadlineElapsed = now + remainingMillis
            phase = FocusPhase.Running
        } else {
            val minutes = minutesOf(mode)
            val total = minutes * 60_000L
            totalMillis = total
            remainingMillis = total
            segmentMinutes = minutes
            deadlineElapsed = now + total
            phase = FocusPhase.Running
            indefinite = false
            elapsedBaseMillis = 0L
            runStartedElapsed = 0L
        }
        commitLocked()
        ensureTickerLocked()
    }

    private fun pauseLocked(events: MutableList<FocusTimerEvent>) {
        if (phase != FocusPhase.Running) return
        val now = clock.elapsedRealtime()
        if (!indefinite && deadlineElapsed > 0L && now >= deadlineElapsed) {
            completeLocked(now, events)
            return
        }
        if (indefinite) {
            elapsedBaseMillis += (now - runStartedElapsed).coerceAtLeast(0L)
            runStartedElapsed = 0L
        } else {
            remainingMillis = (deadlineElapsed - now).coerceAtLeast(0L)
            deadlineElapsed = 0L
        }
        phase = FocusPhase.Paused
        stopTickerLocked()
        commitLocked()
    }

    private fun startIndefiniteLocked() {
        scheduleGeneration += 1
        indefiniteJob?.cancel()
        indefiniteJob = null
        val now = clock.elapsedRealtime()
        indefinite = true
        phase = FocusPhase.Running
        elapsedBaseMillis = 0L
        runStartedElapsed = now
        deadlineElapsed = 0L
        remainingMillis = 0L
        totalMillis = 0L
        commitLocked()
        ensureTickerLocked()
    }

    private fun catchUpLocked(now: Long, events: MutableList<FocusTimerEvent>): Long {
        if (phase != FocusPhase.Running) return 0L
        if (indefinite) {
            publishLocked()
            return indefiniteDelay(now)
        }
        if (deadlineElapsed <= 0L) return 0L
        if (now >= deadlineElapsed) {
            completeLocked(now, events)
            return if (phase == FocusPhase.Running) countdownDelay(clock.elapsedRealtime()) else 0L
        }
        publishLocked()
        return countdownDelay(now)
    }

    private fun completeLocked(now: Long, events: MutableList<FocusTimerEvent>) {
        val overtime = (now - deadlineElapsed).coerceAtLeast(0L)
        val endedAt = clock.wallClockMillis() - overtime
        val live = overtime <= LATE_GRACE_MS
        val finishedMode = mode
        val minutes = segmentMinutes
        if (finishedMode == FocusMode.Focus) {
            mode = nextFocusMode(FocusMode.Focus, completedInCycle, settings.sessionsBeforeLongBreak)
            completedInCycle += 1
            if (minutes > 0) events += FocusTimerEvent.FocusCompleted(minutes, endedAt)
        } else {
            mode = FocusMode.Focus
            if (minutes > 0) events += FocusTimerEvent.BreakCompleted(minutes, endedAt)
        }
        indefinite = false
        elapsedBaseMillis = 0L
        runStartedElapsed = 0L
        val nextMinutes = minutesOf(mode)
        val total = nextMinutes * 60_000L
        totalMillis = total
        remainingMillis = total
        segmentMinutes = nextMinutes
        if (live && settings.autoStartNext) {
            phase = FocusPhase.Running
            deadlineElapsed = now + total
            ensureTickerLocked()
        } else {
            phase = FocusPhase.Idle
            deadlineElapsed = 0L
            stopTickerLocked()
        }
        commitLocked()
    }

    private fun countdownDelay(now: Long): Long {
        val remaining = (deadlineElapsed - now).coerceAtLeast(0L)
        if (remaining == 0L) return 16L
        val mod = remaining % 1000L
        val until = if (mod == 0L) 1000L else mod
        return until.coerceIn(16L, 1000L)
    }

    private fun indefiniteDelay(now: Long): Long {
        val elapsed = elapsedBaseMillis + (now - runStartedElapsed).coerceAtLeast(0L)
        val mod = elapsed % 1000L
        val until = if (mod == 0L) 1000L else 1000L - mod
        return until.coerceIn(16L, 1000L)
    }

    private fun ensureTickerLocked() {
        val jobScope = scope ?: return
        if (ticker?.isActive == true) return
        ticker = jobScope.launch {
            try {
                while (isActive) {
                    val events = mutableListOf<FocusTimerEvent>()
                    val wait = synchronized(lock) {
                        if (phase != FocusPhase.Running) 0L else catchUpLocked(clock.elapsedRealtime(), events)
                    }
                    dispatch(events)
                    if (wait <= 0L) break
                    delay(wait)
                }
            } finally {
                val job = coroutineContext[Job]
                synchronized(lock) {
                    if (ticker === job) ticker = null
                }
            }
        }
    }

    private fun stopTickerLocked() {
        val job = ticker
        ticker = null
        job?.cancel()
    }

    private fun invalidateScheduleLocked() {
        scheduleGeneration += 1
        indefiniteJob?.cancel()
        indefiniteJob = null
    }

    private fun commitLocked() {
        persistence.save(snapshot(clock.elapsedRealtime(), clock.wallClockMillis()))
        publishLocked()
    }

    private fun publishLocked() {
        _state.value = derive(clock.elapsedRealtime())
    }

    private fun snapshot(nowElapsed: Long, nowWall: Long): FocusTimerSnapshot {
        val liveRemaining = if (phase == FocusPhase.Running && !indefinite && deadlineElapsed > 0L) {
            (deadlineElapsed - nowElapsed).coerceAtLeast(0L)
        } else {
            remainingMillis
        }
        return FocusTimerSnapshot(
            phase = phase,
            mode = mode,
            indefinite = indefinite,
            totalMillis = totalMillis,
            remainingMillis = liveRemaining,
            deadlineElapsed = if (phase == FocusPhase.Running && !indefinite) deadlineElapsed else 0L,
            elapsedBaseMillis = elapsedBaseMillis,
            runStartedElapsed = if (phase == FocusPhase.Running && indefinite) runStartedElapsed else 0L,
            completedInCycle = completedInCycle,
            segmentMinutes = segmentMinutes,
            focusMinutes = settings.focusMinutes,
            shortBreakMinutes = settings.shortBreakMinutes,
            longBreakMinutes = settings.longBreakMinutes,
            sessionsBeforeLongBreak = settings.sessionsBeforeLongBreak,
            autoStartNext = settings.autoStartNext,
            savedAtElapsed = nowElapsed,
            savedAtWall = nowWall,
        )
    }

    private fun derive(now: Long): FocusState {
        val liveRemaining = when {
            indefinite -> 0L
            phase == FocusPhase.Running && deadlineElapsed > 0L -> (deadlineElapsed - now).coerceAtLeast(0L)
            else -> remainingMillis
        }
        val liveElapsed = when {
            !indefinite -> 0L
            phase == FocusPhase.Running -> elapsedBaseMillis + (now - runStartedElapsed).coerceAtLeast(0L)
            else -> elapsedBaseMillis
        }
        return FocusState(
            mode = mode,
            running = phase == FocusPhase.Running,
            remainingSeconds = ceilSeconds(liveRemaining),
            totalSeconds = (totalMillis / 1000L).toInt().coerceAtLeast(0),
            completedInCycle = completedInCycle,
            settings = settings,
            indefinite = indefinite,
            elapsedSeconds = (liveElapsed / 1000L).toInt().coerceAtLeast(0),
            phase = phase,
            remainingMillis = liveRemaining,
            totalMillis = totalMillis,
            deadlineElapsed = if (phase == FocusPhase.Running && !indefinite) deadlineElapsed else 0L,
            elapsedBaseMillis = if (indefinite) elapsedBaseMillis else 0L,
            runStartedElapsed = if (phase == FocusPhase.Running && indefinite) runStartedElapsed else 0L,
        )
    }

    private fun minutesOf(target: FocusMode): Int = target.minutes(settings).coerceAtLeast(1)

    private fun mutate(block: (MutableList<FocusTimerEvent>) -> Unit) {
        val events = mutableListOf<FocusTimerEvent>()
        synchronized(lock) { block(events) }
        dispatch(events)
    }

    private fun dispatch(events: List<FocusTimerEvent>) {
        if (events.isEmpty()) return
        val record = sessionRecorder
        events.forEach { event ->
            record(event.toSession())
            _events.tryEmit(event)
        }
    }

    private fun FocusTimerEvent.toSession(): FocusSession = when (this) {
        is FocusTimerEvent.FocusCompleted -> FocusSession(endedAt, minutes)
        is FocusTimerEvent.BreakCompleted -> FocusSession(endedAt, 0, minutes)
    }

    private companion object {
        const val INDEFINITE_ANIMATION_MS = 430L
        const val LATE_GRACE_MS = 3_000L
        const val REBOOT_SKEW_MS = 1_000L
    }
}
