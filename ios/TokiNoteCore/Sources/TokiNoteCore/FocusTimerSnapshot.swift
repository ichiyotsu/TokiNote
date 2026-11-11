import Foundation

public struct FocusTimerSnapshot: Equatable, Sendable {
    public var phase: FocusPhase
    public var mode: FocusMode
    public var indefinite: Bool
    public var totalMillis: Int64
    public var remainingMillis: Int64
    public var deadlineElapsed: Int64
    public var elapsedBaseMillis: Int64
    public var runStartedElapsed: Int64
    public var completedInCycle: Int
    public var segmentMinutes: Int
    public var focusMinutes: Int
    public var shortBreakMinutes: Int
    public var longBreakMinutes: Int
    public var sessionsBeforeLongBreak: Int
    public var autoStartNext: Bool
    public var savedAtElapsed: Int64
    public var savedAtWall: Int64

    public static let version = "1"

    public init(
        phase: FocusPhase,
        mode: FocusMode,
        indefinite: Bool,
        totalMillis: Int64,
        remainingMillis: Int64,
        deadlineElapsed: Int64,
        elapsedBaseMillis: Int64,
        runStartedElapsed: Int64,
        completedInCycle: Int,
        segmentMinutes: Int,
        focusMinutes: Int,
        shortBreakMinutes: Int,
        longBreakMinutes: Int,
        sessionsBeforeLongBreak: Int,
        autoStartNext: Bool,
        savedAtElapsed: Int64,
        savedAtWall: Int64
    ) {
        self.phase = phase
        self.mode = mode
        self.indefinite = indefinite
        self.totalMillis = totalMillis
        self.remainingMillis = remainingMillis
        self.deadlineElapsed = deadlineElapsed
        self.elapsedBaseMillis = elapsedBaseMillis
        self.runStartedElapsed = runStartedElapsed
        self.completedInCycle = completedInCycle
        self.segmentMinutes = segmentMinutes
        self.focusMinutes = focusMinutes
        self.shortBreakMinutes = shortBreakMinutes
        self.longBreakMinutes = longBreakMinutes
        self.sessionsBeforeLongBreak = sessionsBeforeLongBreak
        self.autoStartNext = autoStartNext
        self.savedAtElapsed = savedAtElapsed
        self.savedAtWall = savedAtWall
    }

    public func encode() -> String {
        [
            Self.version,
            phase.rawValue,
            mode.rawValue,
            indefinite ? "1" : "0",
            String(totalMillis),
            String(remainingMillis),
            String(deadlineElapsed),
            String(elapsedBaseMillis),
            String(runStartedElapsed),
            String(completedInCycle),
            String(segmentMinutes),
            String(focusMinutes),
            String(shortBreakMinutes),
            String(longBreakMinutes),
            String(sessionsBeforeLongBreak),
            autoStartNext ? "1" : "0",
            String(savedAtElapsed),
            String(savedAtWall),
        ].joined(separator: "\n")
    }

    public static func decode(_ raw: String) -> FocusTimerSnapshot? {
        let lines = raw.split(separator: "\n", omittingEmptySubsequences: false).map(String.init)
        guard lines.count >= 18, lines[0] == version else { return nil }
        guard
            let phase = FocusPhase(rawValue: lines[1]),
            let mode = FocusMode(rawValue: lines[2]),
            let totalMillis = Int64(lines[4]),
            let remainingMillis = Int64(lines[5]),
            let deadlineElapsed = Int64(lines[6]),
            let elapsedBaseMillis = Int64(lines[7]),
            let runStartedElapsed = Int64(lines[8]),
            let completedInCycle = Int(lines[9]),
            let segmentMinutes = Int(lines[10]),
            let focusMinutes = Int(lines[11]),
            let shortBreakMinutes = Int(lines[12]),
            let longBreakMinutes = Int(lines[13]),
            let sessionsBeforeLongBreak = Int(lines[14]),
            let savedAtElapsed = Int64(lines[16]),
            let savedAtWall = Int64(lines[17])
        else { return nil }
        return FocusTimerSnapshot(
            phase: phase,
            mode: mode,
            indefinite: lines[3] == "1",
            totalMillis: totalMillis,
            remainingMillis: remainingMillis,
            deadlineElapsed: deadlineElapsed,
            elapsedBaseMillis: elapsedBaseMillis,
            runStartedElapsed: runStartedElapsed,
            completedInCycle: completedInCycle,
            segmentMinutes: segmentMinutes,
            focusMinutes: focusMinutes,
            shortBreakMinutes: shortBreakMinutes,
            longBreakMinutes: longBreakMinutes,
            sessionsBeforeLongBreak: sessionsBeforeLongBreak,
            autoStartNext: lines[15] == "1",
            savedAtElapsed: savedAtElapsed,
            savedAtWall: savedAtWall
        )
    }
}
