package com.tokinote.app.ui.focus

import com.tokinote.app.data.AppSettings
import com.tokinote.app.data.FocusSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusTimerEngineTest {

    private val settings = AppSettings(
        focusMinutes = 25,
        shortBreakMinutes = 5,
        longBreakMinutes = 15,
        sessionsBeforeLongBreak = 4,
        autoStartNext = false,
    )

    @Test
    fun `countdown follows elapsed time instead of tick count`() {
        val clock = ManualClock()
        val engine = engine(clock)

        engine.toggle()
        clock.advance(10 * 60_000L)
        engine.sync()

        val state = engine.state.value
        assertEquals(15 * 60, state.remainingSeconds)
        assertEquals(900_000L, state.remainingMillis)
        assertTrue(state.running)
    }

    @Test
    fun `display keeps the current second until it is fully consumed`() {
        val clock = ManualClock()
        val engine = engine(clock)
        engine.toggle()

        clock.advance(999)
        engine.sync()
        assertEquals(25 * 60, engine.state.value.remainingSeconds)

        clock.advance(1)
        engine.sync()
        assertEquals(25 * 60 - 1, engine.state.value.remainingSeconds)
    }

    @Test
    fun `pause and resume keep the partial second`() {
        val clock = ManualClock()
        val engine = engine(clock)
        engine.toggle()
        clock.advance(1_500)
        engine.pause()

        assertEquals(1_500_000L - 1_500L, engine.state.value.remainingMillis)
        assertFalse(engine.state.value.running)

        engine.toggle()
        clock.advance(500)
        engine.sync()

        assertEquals(1_498_000L, engine.state.value.remainingMillis)
        assertEquals(24 * 60 + 58, engine.state.value.remainingSeconds)
    }

    @Test
    fun `background gap is applied on the next sync`() {
        val clock = ManualClock()
        val engine = engine(clock, settings.copy(autoStartNext = true))
        engine.toggle()
        clock.advance(25 * 60_000L - 1_000L)
        engine.sync()
        assertTrue(engine.state.value.running)
        assertEquals(FocusMode.Focus, engine.state.value.mode)

        clock.advance(1_000L)
        engine.sync()

        assertEquals(FocusMode.ShortBreak, engine.state.value.mode)
        assertTrue(engine.state.value.running)
    }

    @Test
    fun `completion records the planned minutes once`() {
        val clock = ManualClock()
        val recorded = mutableListOf<FocusSession>()
        val engine = engine(clock, settings.copy(autoStartNext = true), recorded)
        engine.toggle()
        clock.advance(25 * 60_000L)
        engine.sync()
        engine.sync()

        assertEquals(listOf(FocusSession(clock.wall, 25)), recorded)
        assertEquals(1, engine.state.value.completedInCycle)
        assertEquals(FocusMode.ShortBreak, engine.state.value.mode)
    }

    @Test
    fun `a late catch-up does not invent the following segments`() {
        val clock = ManualClock()
        val recorded = mutableListOf<FocusSession>()
        val engine = engine(clock, settings.copy(autoStartNext = true), recorded)
        engine.toggle()
        clock.advance(25 * 60_000L + 3_001L)
        engine.sync()

        assertEquals(1, recorded.size)
        assertEquals(25, recorded.single().minutes)
        assertEquals(FocusMode.ShortBreak, engine.state.value.mode)
        assertFalse(engine.state.value.running)
        assertEquals(FocusPhase.Idle, engine.state.value.phase)
    }

    @Test
    fun `completion inside the grace window still auto-starts`() {
        val clock = ManualClock()
        val engine = engine(clock, settings.copy(autoStartNext = true))
        engine.toggle()
        clock.advance(25 * 60_000L + 3_000L)
        engine.sync()

        assertTrue(engine.state.value.running)
        assertEquals(FocusMode.ShortBreak, engine.state.value.mode)
    }

    @Test
    fun `finishing a break returns to focus and records the break`() {
        val clock = ManualClock()
        val recorded = mutableListOf<FocusSession>()
        val engine = engine(clock, recorder = recorded)
        engine.toggle()
        clock.advance(25 * 60_000L)
        engine.sync()
        engine.toggle()
        clock.advance(5 * 60_000L)
        engine.sync()

        assertEquals(FocusSession(clock.wall - 5 * 60_000L, 25), recorded[0])
        assertEquals(FocusSession(clock.wall, 0, 5), recorded[1])
        assertEquals(FocusMode.Focus, engine.state.value.mode)
        assertFalse(engine.state.value.running)
    }

    @Test
    fun `skip uses the same next segment as the on-screen hint`() {
        val clock = ManualClock()
        val engine = engine(clock)
        repeat(3) {
            engine.toggle()
            clock.advance(25 * 60_000L)
            engine.sync()
            engine.toggle()
            clock.advance(5 * 60_000L)
            engine.sync()
        }

        assertEquals(3, engine.state.value.completedInCycle)
        assertEquals(FocusMode.Focus, engine.state.value.mode)
        assertEquals(FocusMode.LongBreak, engine.state.value.nextMode)

        engine.skip()

        assertEquals(FocusMode.LongBreak, engine.state.value.mode)
        assertFalse(engine.state.value.indefinite)
        assertFalse(engine.state.value.running)
        assertEquals(15 * 60, engine.state.value.remainingSeconds)
    }

    @Test
    fun `skip leaves indefinite mode`() {
        val clock = ManualClock()
        val recorded = mutableListOf<FocusSession>()
        val engine = engine(clock, recorder = recorded)
        engine.startIndefinite()
        clock.advance(90_000)
        engine.skip()

        assertFalse(engine.state.value.indefinite)
        assertEquals(FocusMode.ShortBreak, engine.state.value.mode)
        assertTrue(recorded.isEmpty())
    }

    @Test
    fun `indefinite time includes time spent stopped`() {
        val clock = ManualClock()
        val engine = engine(clock)
        engine.startIndefinite()
        clock.advance(5_000)
        engine.sync()
        assertEquals(5, engine.state.value.elapsedSeconds)

        clock.advance(90_000)
        engine.sync()
        assertEquals(95, engine.state.value.elapsedSeconds)

        engine.pause()
        val paused = engine.state.value.elapsedSeconds
        clock.advance(30_000)
        engine.sync()
        assertEquals(paused, engine.state.value.elapsedSeconds)

        engine.toggle()
        clock.advance(2_000)
        engine.sync()
        assertEquals(paused + 2, engine.state.value.elapsedSeconds)
    }

    @Test
    fun `reset returns to the current segment and drops indefinite time`() {
        val clock = ManualClock()
        val engine = engine(clock)
        engine.startIndefinite()
        clock.advance(20_000)
        engine.reset()

        val state = engine.state.value
        assertFalse(state.indefinite)
        assertFalse(state.running)
        assertEquals(FocusMode.Focus, state.mode)
        assertEquals(25 * 60, state.remainingSeconds)
        assertEquals(0, state.elapsedSeconds)
    }

    @Test
    fun `changing unrelated settings does not wipe a paused timer`() {
        val clock = ManualClock()
        val engine = engine(clock)
        engine.toggle()
        clock.advance(10_000)
        engine.pause()
        val remaining = engine.state.value.remainingMillis

        engine.configure(settings.copy(amoled = true, focusMinutes = 50))

        assertEquals(remaining, engine.state.value.remainingMillis)
        assertEquals(FocusPhase.Paused, engine.state.value.phase)
        assertTrue(engine.state.value.settings.amoled)
    }

    @Test
    fun `changing the duration updates an idle timer`() {
        val engine = engine(ManualClock())

        engine.configure(settings.copy(focusMinutes = 45))

        assertEquals(45 * 60, engine.state.value.remainingSeconds)
        assertEquals(45 * 60, engine.state.value.totalSeconds)
    }

    @Test
    fun `running timer keeps its original length when settings change`() {
        val clock = ManualClock()
        val engine = engine(clock)
        engine.toggle()
        clock.advance(10_000)
        engine.sync()

        engine.configure(settings.copy(focusMinutes = 50))

        assertEquals(1_490_000L, engine.state.value.remainingMillis)
        assertEquals(25 * 60, engine.state.value.totalSeconds)
    }

    @Test
    fun `process death restores the deadline`() {
        val clock = ManualClock()
        val persistence = MemoryPersistence()
        val first = engine(clock, persistence = persistence)
        first.toggle()
        clock.advance(10_000)

        val restored = engine(clock, persistence = persistence)
        assertTrue(restored.state.value.running)
        assertEquals(1_490_000L, restored.state.value.remainingMillis)
        assertEquals(25 * 60, restored.state.value.totalSeconds)
    }

    @Test
    fun `process death restores indefinite elapsed time from the anchor`() {
        val clock = ManualClock()
        val persistence = MemoryPersistence()
        engine(clock, persistence = persistence).startIndefinite()
        clock.advance(12_000)

        val restored = engine(clock, persistence = persistence)
        assertTrue(restored.state.value.indefinite)
        assertEquals(12, restored.state.value.elapsedSeconds)
    }

    @Test
    fun `reboot shifts the deadline onto the new elapsed clock`() {
        val persistence = MemoryPersistence()
        persistence.save(
            FocusTimerSnapshot(
                phase = FocusPhase.Running,
                mode = FocusMode.Focus,
                indefinite = false,
                totalMillis = 30_000L,
                remainingMillis = 30_000L,
                deadlineElapsed = 80_000L,
                elapsedBaseMillis = 0L,
                runStartedElapsed = 0L,
                completedInCycle = 2,
                segmentMinutes = 25,
                focusMinutes = 25,
                shortBreakMinutes = 5,
                longBreakMinutes = 15,
                sessionsBeforeLongBreak = 4,
                autoStartNext = false,
                savedAtElapsed = 50_000L,
                savedAtWall = 10_000_000L,
            ),
        )
        val clock = ManualClock(elapsed = 1_000L, wall = 10_010_000L)

        val restored = engine(clock, persistence = persistence)

        assertEquals(20_000L, restored.state.value.remainingMillis)
        assertEquals(2, restored.state.value.completedInCycle)
        assertTrue(restored.state.value.running)
    }

    @Test
    fun `snapshot codec round-trips`() {
        val snapshot = FocusTimerSnapshot(
            phase = FocusPhase.Paused,
            mode = FocusMode.LongBreak,
            indefinite = false,
            totalMillis = 15 * 60_000L,
            remainingMillis = 8 * 60_000L,
            deadlineElapsed = 0L,
            elapsedBaseMillis = 0L,
            runStartedElapsed = 0L,
            completedInCycle = 4,
            segmentMinutes = 15,
            focusMinutes = 25,
            shortBreakMinutes = 5,
            longBreakMinutes = 15,
            sessionsBeforeLongBreak = 4,
            autoStartNext = true,
            savedAtElapsed = 99L,
            savedAtWall = 100L,
        )

        assertEquals(snapshot, FocusTimerSnapshot.decode(snapshot.encode()))
        assertEquals(null, FocusTimerSnapshot.decode("nope"))
    }

    @Test
    fun `reset cancels a pending indefinite start and the ticker`() = runBlocking {
        val clock = ManualClock()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val recorded = mutableListOf<FocusSession>()
        val engine = engine(clock, scope = scope, recorder = recorded)
        engine.scheduleIndefinite(10_000L)
        engine.toggle()
        delay(150)
        engine.reset()
        clock.armedJump = 120_000L
        delay(1_200)

        assertFalse(engine.state.value.running)
        assertFalse(engine.state.value.indefinite)
        assertTrue(recorded.isEmpty())
        scope.cancel()
    }

    private fun engine(
        clock: ManualClock,
        settings: AppSettings = this.settings,
        recorder: MutableList<FocusSession> = mutableListOf(),
        persistence: FocusTimerPersistence = MemoryPersistence(),
        scope: CoroutineScope? = null,
    ): FocusTimerEngine = FocusTimerEngine(
        clock = clock,
        persistence = persistence,
        scope = scope,
        initialSettings = settings,
        sessionRecorder = { recorder.add(it) },
    )
}

private class ManualClock(
    @Volatile var elapsed: Long = 0L,
    @Volatile var wall: Long = 1_700_000_000_000L,
) : FocusClock {
    @Volatile var armedJump: Long = 0L

    override fun elapsedRealtime(): Long {
        val jump = armedJump
        if (jump != 0L) {
            armedJump = 0L
            elapsed += jump
            wall += jump
        }
        return elapsed
    }

    override fun wallClockMillis(): Long = wall

    fun advance(millis: Long) {
        elapsed += millis
        wall += millis
    }
}

private class MemoryPersistence : FocusTimerPersistence {
    private var snapshot: FocusTimerSnapshot? = null

    override fun load(): FocusTimerSnapshot? = snapshot

    override fun save(snapshot: FocusTimerSnapshot) {
        this.snapshot = snapshot
    }

    override fun clear() {
        snapshot = null
    }
}
