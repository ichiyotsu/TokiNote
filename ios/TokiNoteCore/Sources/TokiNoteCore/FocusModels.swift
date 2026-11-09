import Foundation

public enum FocusPhase: String, Codable, Equatable, Sendable {
    case idle = "Idle"
    case running = "Running"
    case paused = "Paused"
}

public enum FocusMode: String, Codable, Equatable, Sendable {
    case focus = "Focus"
    case shortBreak = "ShortBreak"
    case longBreak = "LongBreak"

    public func minutes(_ settings: AppSettings) -> Int {
        switch self {
        case .focus: return settings.focusMinutes
        case .shortBreak: return settings.shortBreakMinutes
        case .longBreak: return settings.longBreakMinutes
        }
    }
}

public struct AppSettings: Equatable, Sendable {
    public var amoled: Bool
    public var focusMinutes: Int
    public var shortBreakMinutes: Int
    public var longBreakMinutes: Int
    public var sessionsBeforeLongBreak: Int
    public var autoStartNext: Bool

    public init(
        amoled: Bool = false,
        focusMinutes: Int = 25,
        shortBreakMinutes: Int = 5,
        longBreakMinutes: Int = 15,
        sessionsBeforeLongBreak: Int = 4,
        autoStartNext: Bool = true
    ) {
        self.amoled = amoled
        self.focusMinutes = focusMinutes
        self.shortBreakMinutes = shortBreakMinutes
        self.longBreakMinutes = longBreakMinutes
        self.sessionsBeforeLongBreak = sessionsBeforeLongBreak
        self.autoStartNext = autoStartNext
    }
}

public struct FocusSession: Equatable, Sendable {
    public var endedAt: Int64
    public var minutes: Int
    public var breakMinutes: Int

    public init(endedAt: Int64, minutes: Int, breakMinutes: Int = 0) {
        self.endedAt = endedAt
        self.minutes = minutes
        self.breakMinutes = breakMinutes
    }
}

public func nextFocusMode(mode: FocusMode, completedInCycle: Int, sessionsBeforeLongBreak: Int) -> FocusMode {
    let cycle = max(sessionsBeforeLongBreak, 1)
    switch mode {
    case .focus:
        return (completedInCycle + 1) % cycle == 0 ? .longBreak : .shortBreak
    case .shortBreak, .longBreak:
        return .focus
    }
}

public struct FocusState: Equatable, Sendable {
    public var mode: FocusMode
    public var running: Bool
    public var remainingSeconds: Int
    public var totalSeconds: Int
    public var completedInCycle: Int
    public var settings: AppSettings
    public var indefinite: Bool
    public var elapsedSeconds: Int
    public var phase: FocusPhase
    public var remainingMillis: Int64
    public var totalMillis: Int64
    public var deadlineElapsed: Int64
    public var elapsedBaseMillis: Int64
    public var runStartedElapsed: Int64

    public init(
        mode: FocusMode = .focus,
        running: Bool = false,
        remainingSeconds: Int = 25 * 60,
        totalSeconds: Int = 25 * 60,
        completedInCycle: Int = 0,
        settings: AppSettings = AppSettings(),
        indefinite: Bool = false,
        elapsedSeconds: Int = 0,
        phase: FocusPhase? = nil,
        remainingMillis: Int64? = nil,
        totalMillis: Int64? = nil,
        deadlineElapsed: Int64 = 0,
        elapsedBaseMillis: Int64? = nil,
        runStartedElapsed: Int64 = 0
    ) {
        self.mode = mode
        self.running = running
        self.remainingSeconds = remainingSeconds
        self.totalSeconds = totalSeconds
        self.completedInCycle = completedInCycle
        self.settings = settings
        self.indefinite = indefinite
        self.elapsedSeconds = elapsedSeconds
        self.phase = phase ?? (running ? .running : .idle)
        self.remainingMillis = remainingMillis ?? Int64(remainingSeconds) * 1000
        self.totalMillis = totalMillis ?? Int64(totalSeconds) * 1000
        self.deadlineElapsed = deadlineElapsed
        self.elapsedBaseMillis = elapsedBaseMillis ?? Int64(elapsedSeconds) * 1000
        self.runStartedElapsed = runStartedElapsed
    }

    public var progress: Double {
        guard totalMillis > 0 else { return 0 }
        return min(1, max(0, 1 - Double(remainingMillis) / Double(totalMillis)))
    }

    public var nextMode: FocusMode {
        nextFocusMode(mode: mode, completedInCycle: completedInCycle, sessionsBeforeLongBreak: settings.sessionsBeforeLongBreak)
    }

    public var sessionIndex: Int {
        let cycle = max(settings.sessionsBeforeLongBreak, 1)
        return completedInCycle % cycle + 1
    }
}

public func ceilSeconds(_ millis: Int64) -> Int {
    if millis <= 0 { return 0 }
    let value = (millis + 999) / 1000
    return Int(min(value, Int64(Int.max)))
}

public func filledSessionCount(completedInCycle: Int, cycleLength: Int, mode: FocusMode) -> Int {
    let cycle = max(cycleLength, 1)
    if mode != .focus && completedInCycle > 0 && completedInCycle % cycle == 0 {
        return cycle
    }
    let remainder = completedInCycle % cycle
    return remainder >= 0 ? remainder : remainder + cycle
}

public func liveRemainingMillis(_ state: FocusState, now: Int64) -> Int64 {
    if state.running && !state.indefinite && state.deadlineElapsed > 0 {
        return max(0, state.deadlineElapsed - now)
    }
    return state.remainingMillis
}

public func liveElapsedMillis(_ state: FocusState, now: Int64) -> Int64 {
    if !state.indefinite { return 0 }
    if state.running {
        return state.elapsedBaseMillis + max(0, now - state.runStartedElapsed)
    }
    return state.elapsedBaseMillis
}

public func displaySeconds(_ state: FocusState, now: Int64) -> Int {
    if state.indefinite {
        return max(0, Int(liveElapsedMillis(state, now: now) / 1000))
    }
    return ceilSeconds(liveRemainingMillis(state, now: now))
}

public func formatClock(_ totalSeconds: Int) -> String {
    let safe = max(totalSeconds, 0)
    return String(format: "%02d:%02d", safe / 60, safe % 60)
}

/// Count-up and the focus ring clock. Past one hour this is h:mm:ss.
public func formatElapsed(_ totalSeconds: Int) -> String {
    let safe = max(totalSeconds, 0)
    let hours = safe / 3600
    if hours > 0 {
        return String(format: "%d:%02d:%02d", hours, (safe % 3600) / 60, safe % 60)
    }
    return String(format: "%02d:%02d", safe / 60, safe % 60)
}

public func formatMinutes(_ minutes: Int) -> String {
    if minutes < 60 {
        return "\(minutes) 分钟"
    }
    let hours = minutes / 60
    let rest = minutes % 60
    if rest == 0 {
        return "\(hours) 小时"
    }
    return "\(hours) 小时 \(rest) 分钟"
}

public func formatDurationLabel(_ minutes: Int) -> String {
    if minutes < 60 {
        return "\(minutes) 分"
    }
    return "\(minutes / 60) 小时 \(minutes % 60) 分"
}

public func formatNoteDate(_ date: Date, locale: Locale = Locale(identifier: "zh_CN")) -> String {
    let formatter = DateFormatter()
    formatter.locale = locale
    formatter.dateFormat = "M月d日 HH:mm"
    return formatter.string(from: date)
}
