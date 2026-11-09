import Foundation
import Testing
@testable import TokiNoteCore

struct FocusTimerEngineTests {
    private let settings = AppSettings(
        focusMinutes: 25,
        shortBreakMinutes: 5,
        longBreakMinutes: 15,
        sessionsBeforeLongBreak: 4,
        autoStartNext: false
    )

    @Test("countdown follows elapsed time instead of tick count")
    func countdownFollowsElapsedTime() {
        let clock = ManualClock()
        let engine = makeEngine(clock: clock)
        engine.toggle()
        clock.advance(10 * 60_000)
        engine.sync()
        let state = engine.state
        #expect(state.remainingSeconds == 15 * 60)
        #expect(state.remainingMillis == 900_000)
        #expect(state.running)
    }

    @Test("display keeps the current second until it is fully consumed")
    func displayKeepsTheCurrentSecond() {
        let clock = ManualClock()
        let engine = makeEngine(clock: clock)
        engine.toggle()
        clock.advance(999)
        engine.sync()
        #expect(engine.state.remainingSeconds == 25 * 60)
        clock.advance(1)
        engine.sync()
        #expect(engine.state.remainingSeconds == 25 * 60 - 1)
    }

    @Test("pause and resume keep the partial second")
    func pauseAndResumeKeepThePartialSecond() {
        let clock = ManualClock()
        let engine = makeEngine(clock: clock)
        engine.toggle()
        clock.advance(1_500)
        engine.pause()
        #expect(engine.state.remainingMillis == 1_500_000 - 1_500)
        #expect(!engine.state.running)
        engine.toggle()
        clock.advance(500)
        engine.sync()
        #expect(engine.state.remainingMillis == 1_498_000)
        #expect(engine.state.remainingSeconds == 24 * 60 + 58)
    }

    @Test("background gap is applied on the next sync")
    func backgroundGapIsAppliedOnTheNextSync() {
        let clock = ManualClock()
        let engine = makeEngine(clock: clock, settings: settingsWith(autoStartNext: true))
        engine.toggle()
        clock.advance(25 * 60_000 - 1_000)
        engine.sync()
        #expect(engine.state.running)
        #expect(engine.state.mode == .focus)
        clock.advance(1_000)
        engine.sync()
        #expect(engine.state.mode == .shortBreak)
        #expect(engine.state.running)
    }

    @Test("completion records the planned minutes once")
    func completionRecordsThePlannedMinutesOnce() {
        let clock = ManualClock()
        let recorded = SessionBox()
        let engine = makeEngine(clock: clock, settings: settingsWith(autoStartNext: true), recorded: recorded)
        engine.toggle()
        clock.advance(25 * 60_000)
        engine.sync()
        engine.sync()
        #expect(recorded.sessions == [FocusSession(endedAt: clock.wall, minutes: 25)])
        #expect(engine.state.completedInCycle == 1)
        #expect(engine.state.mode == .shortBreak)
    }

    @Test("a late catch-up does not invent the following segments")
    func lateCatchUpDoesNotInventFollowingSegments() {
        let clock = ManualClock()
        let recorded = SessionBox()
        let engine = makeEngine(clock: clock, settings: settingsWith(autoStartNext: true), recorded: recorded)
        engine.toggle()
        clock.advance(25 * 60_000 + 3_001)
        engine.sync()
        #expect(recorded.sessions.count == 1)
        #expect(recorded.sessions[0].minutes == 25)
        #expect(engine.state.mode == .shortBreak)
        #expect(!engine.state.running)
        #expect(engine.state.phase == .idle)
    }

    @Test("completion inside the grace window still auto-starts")
    func completionInsideTheGraceWindowStillAutoStarts() {
        let clock = ManualClock()
        let engine = makeEngine(clock: clock, settings: settingsWith(autoStartNext: true))
        engine.toggle()
        clock.advance(25 * 60_000 + 3_000)
        engine.sync()
        #expect(engine.state.running)
        #expect(engine.state.mode == .shortBreak)
    }

    @Test("finishing a break returns to focus and records the break")
    func finishingABreakReturnsToFocus() {
        let clock = ManualClock()
        let recorded = SessionBox()
        let engine = makeEngine(clock: clock, recorded: recorded)
        engine.toggle()
        clock.advance(25 * 60_000)
        engine.sync()
        engine.toggle()
        clock.advance(5 * 60_000)
        engine.sync()
        #expect(recorded.sessions[0] == FocusSession(endedAt: clock.wall - 5 * 60_000, minutes: 25))
        #expect(recorded.sessions[1] == FocusSession(endedAt: clock.wall, minutes: 0, breakMinutes: 5))
        #expect(engine.state.mode == .focus)
        #expect(!engine.state.running)
    }

    @Test("skip uses the same next segment as the on-screen hint")
    func skipUsesTheSameNextSegment() {
        let clock = ManualClock()
        let engine = makeEngine(clock: clock)
        for _ in 0..<3 {
            engine.toggle()
            clock.advance(25 * 60_000)
            engine.sync()
            engine.toggle()
            clock.advance(5 * 60_000)
            engine.sync()
        }
        #expect(engine.state.completedInCycle == 3)
        #expect(engine.state.mode == .focus)
        #expect(engine.state.nextMode == .longBreak)
        engine.skip()
        #expect(engine.state.mode == .longBreak)
        #expect(!engine.state.indefinite)
        #expect(!engine.state.running)
        #expect(engine.state.remainingSeconds == 15 * 60)
    }

    @Test("skip leaves indefinite mode")
    func skipLeavesIndefiniteMode() {
        let clock = ManualClock()
        let recorded = SessionBox()
        let engine = makeEngine(clock: clock, recorded: recorded)
        engine.startIndefinite()
        clock.advance(90_000)
        engine.skip()
        #expect(!engine.state.indefinite)
        #expect(engine.state.mode == .shortBreak)
        #expect(recorded.sessions.isEmpty)
    }

    @Test("indefinite time includes time spent stopped")
    func indefiniteTimeIncludesTimeSpentStopped() {
        let clock = ManualClock()
        let engine = makeEngine(clock: clock)
        engine.startIndefinite()
        clock.advance(5_000)
        engine.sync()
        #expect(engine.state.elapsedSeconds == 5)
        clock.advance(90_000)
        engine.sync()
        #expect(engine.state.elapsedSeconds == 95)
        engine.pause()
        let paused = engine.state.elapsedSeconds
        clock.advance(30_000)
        engine.sync()
        #expect(engine.state.elapsedSeconds == paused)
        engine.toggle()
        clock.advance(2_000)
        engine.sync()
        #expect(engine.state.elapsedSeconds == paused + 2)
    }

    @Test("reset returns to the current segment and drops indefinite time")
    func resetReturnsToTheCurrentSegment() {
        let clock = ManualClock()
        let engine = makeEngine(clock: clock)
        engine.startIndefinite()
        clock.advance(20_000)
        engine.reset()
        let state = engine.state
        #expect(!state.indefinite)
        #expect(!state.running)
        #expect(state.mode == .focus)
        #expect(state.remainingSeconds == 25 * 60)
        #expect(state.elapsedSeconds == 0)
    }

    @Test("changing unrelated settings does not wipe a paused timer")
    func changingUnrelatedSettingsDoesNotWipeAPausedTimer() {
        let clock = ManualClock()
        let engine = makeEngine(clock: clock)
        engine.toggle()
        clock.advance(10_000)
        engine.pause()
        let remaining = engine.state.remainingMillis
        engine.configure(AppSettings(
            amoled: true,
            focusMinutes: 50,
            shortBreakMinutes: settings.shortBreakMinutes,
            longBreakMinutes: settings.longBreakMinutes,
            sessionsBeforeLongBreak: settings.sessionsBeforeLongBreak,
            autoStartNext: settings.autoStartNext
        ))
        #expect(engine.state.remainingMillis == remaining)
        #expect(engine.state.phase == .paused)
        #expect(engine.state.settings.amoled)
    }

    @Test("changing the duration updates an idle timer")
    func changingTheDurationUpdatesAnIdleTimer() {
        let engine = makeEngine(clock: ManualClock())
        engine.configure(AppSettings(
            focusMinutes: 45,
            shortBreakMinutes: settings.shortBreakMinutes,
            longBreakMinutes: settings.longBreakMinutes,
            sessionsBeforeLongBreak: settings.sessionsBeforeLongBreak,
            autoStartNext: settings.autoStartNext
        ))
        #expect(engine.state.remainingSeconds == 45 * 60)
        #expect(engine.state.totalSeconds == 45 * 60)
    }

    @Test("running timer keeps its original length when settings change")
    func runningTimerKeepsItsOriginalLength() {
        let clock = ManualClock()
        let engine = makeEngine(clock: clock)
        engine.toggle()
        clock.advance(10_000)
        engine.sync()
        engine.configure(AppSettings(
            focusMinutes: 50,
            shortBreakMinutes: settings.shortBreakMinutes,
            longBreakMinutes: settings.longBreakMinutes,
            sessionsBeforeLongBreak: settings.sessionsBeforeLongBreak,
            autoStartNext: settings.autoStartNext
        ))
        #expect(engine.state.remainingMillis == 1_490_000)
        #expect(engine.state.totalSeconds == 25 * 60)
    }

    @Test("process death restores the deadline")
    func processDeathRestoresTheDeadline() {
        let clock = ManualClock()
        let persistence = MemoryPersistence()
        let first = makeEngine(clock: clock, persistence: persistence)
        first.toggle()
        clock.advance(10_000)
        let restored = makeEngine(clock: clock, persistence: persistence)
        #expect(restored.state.running)
        #expect(restored.state.remainingMillis == 1_490_000)
        #expect(restored.state.totalSeconds == 25 * 60)
    }

    @Test("process death restores indefinite elapsed time from the anchor")
    func processDeathRestoresIndefiniteElapsedTime() {
        let clock = ManualClock()
        let persistence = MemoryPersistence()
        makeEngine(clock: clock, persistence: persistence).startIndefinite()
        clock.advance(12_000)
        let restored = makeEngine(clock: clock, persistence: persistence)
        #expect(restored.state.indefinite)
        #expect(restored.state.elapsedSeconds == 12)
    }

    @Test("reboot shifts the deadline onto the new elapsed clock")
    func rebootShiftsTheDeadline() {
        let persistence = MemoryPersistence()
        persistence.save(FocusTimerSnapshot(
            phase: .running,
            mode: .focus,
            indefinite: false,
            totalMillis: 30_000,
            remainingMillis: 30_000,
            deadlineElapsed: 80_000,
            elapsedBaseMillis: 0,
            runStartedElapsed: 0,
            completedInCycle: 2,
            segmentMinutes: 25,
            focusMinutes: 25,
            shortBreakMinutes: 5,
            longBreakMinutes: 15,
            sessionsBeforeLongBreak: 4,
            autoStartNext: false,
            savedAtElapsed: 50_000,
            savedAtWall: 10_000_000
        ))
        let clock = ManualClock(elapsed: 1_000, wall: 10_010_000)
        let restored = makeEngine(clock: clock, persistence: persistence)
        #expect(restored.state.remainingMillis == 20_000)
        #expect(restored.state.completedInCycle == 2)
        #expect(restored.state.running)
    }

    @Test("snapshot codec round-trips")
    func snapshotCodecRoundTrips() {
        let snapshot = FocusTimerSnapshot(
            phase: .paused,
            mode: .longBreak,
            indefinite: false,
            totalMillis: 15 * 60_000,
            remainingMillis: 8 * 60_000,
            deadlineElapsed: 0,
            elapsedBaseMillis: 0,
            runStartedElapsed: 0,
            completedInCycle: 4,
            segmentMinutes: 15,
            focusMinutes: 25,
            shortBreakMinutes: 5,
            longBreakMinutes: 15,
            sessionsBeforeLongBreak: 4,
            autoStartNext: true,
            savedAtElapsed: 99,
            savedAtWall: 100
        )
        #expect(FocusTimerSnapshot.decode(snapshot.encode()) == snapshot)
        #expect(FocusTimerSnapshot.decode("nope") == nil)
    }

    @Test("reset cancels a pending indefinite start and the ticker")
    func resetCancelsPendingIndefiniteStart() async throws {
        let clock = ManualClock()
        let recorded = SessionBox()
        let engine = makeEngine(clock: clock, recorded: recorded, enableTasks: true)
        engine.scheduleIndefinite(delayMs: 10_000)
        engine.toggle()
        try await Task.sleep(nanoseconds: 150_000_000)
        engine.reset()
        clock.armedJump = 120_000
        try await Task.sleep(nanoseconds: 1_200_000_000)
        #expect(!engine.state.running)
        #expect(!engine.state.indefinite)
        #expect(recorded.sessions.isEmpty)
    }

    private func settingsWith(autoStartNext: Bool) -> AppSettings {
        AppSettings(
            focusMinutes: settings.focusMinutes,
            shortBreakMinutes: settings.shortBreakMinutes,
            longBreakMinutes: settings.longBreakMinutes,
            sessionsBeforeLongBreak: settings.sessionsBeforeLongBreak,
            autoStartNext: autoStartNext
        )
    }

    private func makeEngine(
        clock: ManualClock,
        settings: AppSettings? = nil,
        recorded: SessionBox = SessionBox(),
        persistence: FocusTimerPersistence = MemoryPersistence(),
        enableTasks: Bool = false
    ) -> FocusTimerEngine {
        FocusTimerEngine(
            clock: clock,
            persistence: persistence,
            enableTasks: enableTasks,
            initialSettings: settings ?? self.settings,
            sessionRecorder: { recorded.sessions.append($0) }
        )
    }
}

final class ManualClock: FocusClock {
    private let lock = NSLock()
    private var elapsed: Int64
    private var wallValue: Int64
    var armedJump: Int64 = 0

    var wall: Int64 {
        lock.lock()
        defer { lock.unlock() }
        return wallValue
    }

    init(elapsed: Int64 = 0, wall: Int64 = 1_700_000_000_000) {
        self.elapsed = elapsed
        self.wallValue = wall
    }

    func elapsedRealtime() -> Int64 {
        lock.lock()
        defer { lock.unlock() }
        if armedJump != 0 {
            elapsed += armedJump
            wallValue += armedJump
            armedJump = 0
        }
        return elapsed
    }

    func wallClockMillis() -> Int64 {
        lock.lock()
        defer { lock.unlock() }
        return wallValue
    }

    func advance(_ millis: Int64) {
        lock.lock()
        elapsed += millis
        wallValue += millis
        lock.unlock()
    }
}

final class MemoryPersistence: FocusTimerPersistence {
    private var snapshot: FocusTimerSnapshot?

    func load() -> FocusTimerSnapshot? { snapshot }

    func save(_ snapshot: FocusTimerSnapshot) {
        self.snapshot = snapshot
    }

    func clear() {
        snapshot = nil
    }
}

final class SessionBox {
    var sessions: [FocusSession] = []
}
