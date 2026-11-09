import Foundation

public enum FocusTimerEvent: Equatable, Sendable {
    case focusCompleted(minutes: Int, endedAt: Int64)
    case breakCompleted(minutes: Int, endedAt: Int64)

    public var minutes: Int {
        switch self {
        case .focusCompleted(let minutes, _), .breakCompleted(let minutes, _):
            return minutes
        }
    }

    public var endedAt: Int64 {
        switch self {
        case .focusCompleted(_, let endedAt), .breakCompleted(_, let endedAt):
            return endedAt
        }
    }

    public func session() -> FocusSession {
        switch self {
        case .focusCompleted(let minutes, let endedAt):
            return FocusSession(endedAt: endedAt, minutes: minutes)
        case .breakCompleted(let minutes, let endedAt):
            return FocusSession(endedAt: endedAt, minutes: 0, breakMinutes: minutes)
        }
    }
}

/// Deadline-based focus timer.
///
/// The countdown is anchored to `FocusClock.elapsedRealtime`, and indefinite
/// focus is anchored to the moment it started. Every refresh recomputes
/// "now − anchor" instead of adding up one-second ticks, so sleep, backgrounding,
/// and dropped frames do not drift. If the deadline was missed by more than
/// `lateGraceMillis`, only the current segment is settled — the engine does not
/// invent the pomodoros that would have happened while the user was away.
public final class FocusTimerEngine {
    public static let indefiniteAnimationMillis: Int64 = 430
    public static let lateGraceMillis: Int64 = 3_000
    private static let rebootSkewMillis: Int64 = 1_000

    private let clock: FocusClock
    private let persistence: FocusTimerPersistence
    private let enableTasks: Bool
    private let lock = NSRecursiveLock()

    public var sessionRecorder: (FocusSession) -> Void
    private var stateSink: ((FocusState) -> Void)?

    private var settings: AppSettings
    private var phase: FocusPhase = .idle
    private var mode: FocusMode = .focus
    private var indefinite = false
    private var totalMillis: Int64 = 0
    private var remainingMillis: Int64 = 0
    private var deadlineElapsed: Int64 = 0
    private var elapsedBaseMillis: Int64 = 0
    private var runStartedElapsed: Int64 = 0
    private var completedInCycle = 0
    private var segmentMinutes = 0
    private var scheduleGeneration = 0
    private var ticker: Task<Void, Never>?
    private var tickerToken: TickerToken?
    private var indefiniteJob: Task<Void, Never>?
    private var _state: FocusState

    public init(
        clock: FocusClock,
        persistence: FocusTimerPersistence,
        enableTasks: Bool = false,
        initialSettings: AppSettings = AppSettings(),
        sessionRecorder: @escaping (FocusSession) -> Void = { _ in }
    ) {
        self.clock = clock
        self.persistence = persistence
        self.enableTasks = enableTasks
        self.sessionRecorder = sessionRecorder
        self.settings = initialSettings
        self._state = FocusState(settings: initialSettings)
        if let saved = persistence.load() {
            applySnapshot(saved)
        } else {
            installIdleSegment(.focus)
        }
        _state = derive(clock.elapsedRealtime())
    }

    public var state: FocusState {
        lock.lock()
        defer { lock.unlock() }
        return _state
    }

    public var onStateChange: ((FocusState) -> Void)? {
        get {
            lock.lock()
            defer { lock.unlock() }
            return stateSink
        }
        set {
            lock.lock()
            stateSink = newValue
            lock.unlock()
        }
    }

    public func configure(_ newSettings: AppSettings) {
        mutate { _ in
            if newSettings == settings { return }
            let idle = phase == .idle && !indefinite
            settings = newSettings
            if idle { installIdleSegment(mode) }
            commitLocked()
        }
    }

    public func toggle() {
        mutate { events in
            invalidateScheduleLocked()
            if phase == .running {
                pauseLocked(&events)
            } else {
                resumeOrStartLocked()
            }
        }
    }

    public func pause() {
        mutate { events in
            pauseLocked(&events)
        }
    }

    public func reset() {
        mutate { _ in
            invalidateScheduleLocked()
            stopTickerLocked()
            indefinite = false
            elapsedBaseMillis = 0
            runStartedElapsed = 0
            deadlineElapsed = 0
            phase = .idle
            installIdleSegment(mode)
            commitLocked()
        }
    }

    public func skip() {
        mutate { _ in
            invalidateScheduleLocked()
            stopTickerLocked()
            mode = nextFocusMode(mode: mode, completedInCycle: completedInCycle, sessionsBeforeLongBreak: settings.sessionsBeforeLongBreak)
            indefinite = false
            elapsedBaseMillis = 0
            runStartedElapsed = 0
            deadlineElapsed = 0
            phase = .idle
            installIdleSegment(mode)
            commitLocked()
        }
    }

    public func startIndefinite() {
        mutate { _ in
            startIndefiniteLocked()
        }
    }

    public func scheduleIndefinite(delayMs: Int64 = indefiniteAnimationMillis) {
        lock.lock()
        scheduleGeneration += 1
        let generation = scheduleGeneration
        indefiniteJob?.cancel()
        indefiniteJob = nil
        if delayMs <= 0 || !enableTasks {
            startIndefiniteLocked()
            lock.unlock()
            return
        }
        let job = Task { [weak self] in
            do {
                try await Task.sleep(nanoseconds: UInt64(delayMs) * 1_000_000)
            } catch {
                return
            }
            guard let self, !Task.isCancelled else { return }
            self.mutate { _ in
                if generation != self.scheduleGeneration { return }
                self.startIndefiniteLocked()
            }
        }
        indefiniteJob = job
        lock.unlock()
    }

    /// Recompute against the clock immediately. Call this when returning to the
    /// foreground or after the process is restored.
    public func sync() {
        var events: [FocusTimerEvent] = []
        lock.lock()
        _ = catchUpLocked(now: clock.elapsedRealtime(), events: &events)
        let running = phase == .running
        lock.unlock()
        dispatch(events)
        if running {
            lock.lock()
            ensureTickerLocked()
            lock.unlock()
        }
    }

    private func applySnapshot(_ saved: FocusTimerSnapshot) {
        settings = AppSettings(
            amoled: settings.amoled,
            focusMinutes: max(saved.focusMinutes, 1),
            shortBreakMinutes: max(saved.shortBreakMinutes, 1),
            longBreakMinutes: max(saved.longBreakMinutes, 1),
            sessionsBeforeLongBreak: max(saved.sessionsBeforeLongBreak, 1),
            autoStartNext: saved.autoStartNext
        )
        phase = saved.phase
        mode = saved.mode
        indefinite = saved.indefinite
        totalMillis = max(saved.totalMillis, 0)
        remainingMillis = max(saved.remainingMillis, 0)
        elapsedBaseMillis = max(saved.elapsedBaseMillis, 0)
        completedInCycle = max(saved.completedInCycle, 0)
        segmentMinutes = max(saved.segmentMinutes, 0)
        deadlineElapsed = saved.deadlineElapsed
        runStartedElapsed = saved.runStartedElapsed

        let nowElapsed = clock.elapsedRealtime()
        let nowWall = clock.wallClockMillis()
        let rebooted = nowElapsed + Self.rebootSkewMillis < saved.savedAtElapsed
        if !rebooted { return }
        if deadlineElapsed != 0 {
            let deadlineWall = saved.savedAtWall + (saved.deadlineElapsed - saved.savedAtElapsed)
            deadlineElapsed = nowElapsed + (deadlineWall - nowWall)
        }
        if runStartedElapsed != 0 {
            let startWall = saved.savedAtWall + (saved.runStartedElapsed - saved.savedAtElapsed)
            runStartedElapsed = nowElapsed + (startWall - nowWall)
        }
    }

    private func installIdleSegment(_ target: FocusMode) {
        mode = target
        let minutes = minutesOf(target)
        let total = Int64(minutes) * 60_000
        totalMillis = total
        remainingMillis = total
        segmentMinutes = minutes
    }

    private func resumeOrStartLocked() {
        let now = clock.elapsedRealtime()
        if indefinite {
            phase = .running
            runStartedElapsed = now
            commitLocked()
            ensureTickerLocked()
            return
        }
        if phase == .paused && remainingMillis > 0 {
            deadlineElapsed = now + remainingMillis
            phase = .running
        } else {
            let minutes = minutesOf(mode)
            let total = Int64(minutes) * 60_000
            totalMillis = total
            remainingMillis = total
            segmentMinutes = minutes
            deadlineElapsed = now + total
            phase = .running
            indefinite = false
            elapsedBaseMillis = 0
            runStartedElapsed = 0
        }
        commitLocked()
        ensureTickerLocked()
    }

    private func pauseLocked(_ events: inout [FocusTimerEvent]) {
        if phase != .running { return }
        let now = clock.elapsedRealtime()
        if !indefinite && deadlineElapsed > 0 && now >= deadlineElapsed {
            completeLocked(now: now, events: &events)
            return
        }
        if indefinite {
            elapsedBaseMillis += max(0, now - runStartedElapsed)
            runStartedElapsed = 0
        } else {
            remainingMillis = max(0, deadlineElapsed - now)
            deadlineElapsed = 0
        }
        phase = .paused
        stopTickerLocked()
        commitLocked()
    }

    private func startIndefiniteLocked() {
        scheduleGeneration += 1
        indefiniteJob?.cancel()
        indefiniteJob = nil
        let now = clock.elapsedRealtime()
        indefinite = true
        phase = .running
        elapsedBaseMillis = 0
        runStartedElapsed = now
        deadlineElapsed = 0
        remainingMillis = 0
        totalMillis = 0
        commitLocked()
        ensureTickerLocked()
    }

    private func catchUpLocked(now: Int64, events: inout [FocusTimerEvent]) -> Int64 {
        if phase != .running { return 0 }
        if indefinite {
            publishLocked()
            return indefiniteDelay(now)
        }
        if deadlineElapsed <= 0 { return 0 }
        if now >= deadlineElapsed {
            completeLocked(now: now, events: &events)
            if phase == .running {
                return countdownDelay(clock.elapsedRealtime())
            }
            return 0
        }
        publishLocked()
        return countdownDelay(now)
    }

    private func completeLocked(now: Int64, events: inout [FocusTimerEvent]) {
        let overtime = max(0, now - deadlineElapsed)
        let endedAt = clock.wallClockMillis() - overtime
        let live = overtime <= Self.lateGraceMillis
        let finishedMode = mode
        let minutes = segmentMinutes
        if finishedMode == .focus {
            mode = nextFocusMode(mode: .focus, completedInCycle: completedInCycle, sessionsBeforeLongBreak: settings.sessionsBeforeLongBreak)
            completedInCycle += 1
            if minutes > 0 {
                events.append(.focusCompleted(minutes: minutes, endedAt: endedAt))
            }
        } else {
            mode = .focus
            if minutes > 0 {
                events.append(.breakCompleted(minutes: minutes, endedAt: endedAt))
            }
        }
        indefinite = false
        elapsedBaseMillis = 0
        runStartedElapsed = 0
        let nextMinutes = minutesOf(mode)
        let total = Int64(nextMinutes) * 60_000
        totalMillis = total
        remainingMillis = total
        segmentMinutes = nextMinutes
        if live && settings.autoStartNext {
            phase = .running
            deadlineElapsed = now + total
            ensureTickerLocked()
        } else {
            phase = .idle
            deadlineElapsed = 0
            stopTickerLocked()
        }
        commitLocked()
    }

    private func countdownDelay(_ now: Int64) -> Int64 {
        let remaining = max(0, deadlineElapsed - now)
        if remaining == 0 { return 16 }
        let mod = remaining % 1000
        let until: Int64 = mod == 0 ? 1000 : mod
        return min(1000, max(16, until))
    }

    private func indefiniteDelay(_ now: Int64) -> Int64 {
        let elapsed = elapsedBaseMillis + max(0, now - runStartedElapsed)
        let mod = elapsed % 1000
        let until: Int64 = mod == 0 ? 1000 : 1000 - mod
        return min(1000, max(16, until))
    }

    private func ensureTickerLocked() {
        guard enableTasks else { return }
        if let ticker, !ticker.isCancelled { return }
        let token = TickerToken()
        tickerToken = token
        ticker = Task { [weak self] in
            guard let self else { return }
            while !Task.isCancelled {
                let (wait, events) = self.tickOnce()
                self.dispatch(events)
                if wait <= 0 || Task.isCancelled { break }
                do {
                    try await Task.sleep(nanoseconds: UInt64(wait) * 1_000_000)
                } catch {
                    break
                }
            }
            self.clearTicker(token)
        }
    }

    private func clearTicker(_ token: TickerToken) {
        lock.lock()
        if tickerToken === token {
            ticker = nil
            tickerToken = nil
        }
        lock.unlock()
    }

    private func tickOnce() -> (Int64, [FocusTimerEvent]) {
        lock.lock()
        defer { lock.unlock() }
        var events: [FocusTimerEvent] = []
        if phase != .running { return (0, events) }
        let wait = catchUpLocked(now: clock.elapsedRealtime(), events: &events)
        return (wait, events)
    }

    private func stopTickerLocked() {
        let job = ticker
        ticker = nil
        tickerToken = nil
        job?.cancel()
    }

    private func invalidateScheduleLocked() {
        scheduleGeneration += 1
        indefiniteJob?.cancel()
        indefiniteJob = nil
    }

    private func commitLocked() {
        persistence.save(snapshot(nowElapsed: clock.elapsedRealtime(), nowWall: clock.wallClockMillis()))
        publishLocked()
    }

    private func publishLocked() {
        _state = derive(clock.elapsedRealtime())
        let value = _state
        guard let sink = stateSink else { return }
        DispatchQueue.main.async {
            sink(value)
        }
    }

    private func snapshot(nowElapsed: Int64, nowWall: Int64) -> FocusTimerSnapshot {
        let liveRemaining: Int64
        if phase == .running && !indefinite && deadlineElapsed > 0 {
            liveRemaining = max(0, deadlineElapsed - nowElapsed)
        } else {
            liveRemaining = remainingMillis
        }
        return FocusTimerSnapshot(
            phase: phase,
            mode: mode,
            indefinite: indefinite,
            totalMillis: totalMillis,
            remainingMillis: liveRemaining,
            deadlineElapsed: (phase == .running && !indefinite) ? deadlineElapsed : 0,
            elapsedBaseMillis: elapsedBaseMillis,
            runStartedElapsed: (phase == .running && indefinite) ? runStartedElapsed : 0,
            completedInCycle: completedInCycle,
            segmentMinutes: segmentMinutes,
            focusMinutes: settings.focusMinutes,
            shortBreakMinutes: settings.shortBreakMinutes,
            longBreakMinutes: settings.longBreakMinutes,
            sessionsBeforeLongBreak: settings.sessionsBeforeLongBreak,
            autoStartNext: settings.autoStartNext,
            savedAtElapsed: nowElapsed,
            savedAtWall: nowWall
        )
    }

    private func derive(_ now: Int64) -> FocusState {
        let liveRemaining: Int64
        if indefinite {
            liveRemaining = 0
        } else if phase == .running && deadlineElapsed > 0 {
            liveRemaining = max(0, deadlineElapsed - now)
        } else {
            liveRemaining = remainingMillis
        }
        let liveElapsed: Int64
        if !indefinite {
            liveElapsed = 0
        } else if phase == .running {
            liveElapsed = elapsedBaseMillis + max(0, now - runStartedElapsed)
        } else {
            liveElapsed = elapsedBaseMillis
        }
        return FocusState(
            mode: mode,
            running: phase == .running,
            remainingSeconds: ceilSeconds(liveRemaining),
            totalSeconds: max(0, Int(totalMillis / 1000)),
            completedInCycle: completedInCycle,
            settings: settings,
            indefinite: indefinite,
            elapsedSeconds: max(0, Int(liveElapsed / 1000)),
            phase: phase,
            remainingMillis: liveRemaining,
            totalMillis: totalMillis,
            deadlineElapsed: (phase == .running && !indefinite) ? deadlineElapsed : 0,
            elapsedBaseMillis: indefinite ? elapsedBaseMillis : 0,
            runStartedElapsed: (phase == .running && indefinite) ? runStartedElapsed : 0
        )
    }

    private func minutesOf(_ target: FocusMode) -> Int {
        max(target.minutes(settings), 1)
    }

    private func mutate(_ block: (inout [FocusTimerEvent]) -> Void) {
        var events: [FocusTimerEvent] = []
        lock.lock()
        block(&events)
        lock.unlock()
        dispatch(events)
    }

    private func dispatch(_ events: [FocusTimerEvent]) {
        guard !events.isEmpty else { return }
        let record = sessionRecorder
        for event in events {
            record(event.session())
        }
    }
}

private final class TickerToken {}
