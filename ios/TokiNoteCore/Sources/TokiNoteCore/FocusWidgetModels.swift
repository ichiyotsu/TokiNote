import Foundation

public enum FocusWidgetKind {
    public static let home = "FocusWidget"
    public static let appGroup = "group.com.tokinote.ios"
    public static let snapshotKey = "focus_widget_snapshot"
}

public struct FocusWidgetSnapshot: Codable, Equatable, Sendable {
    public var phase: String
    public var indefinite: Bool
    public var modeTitle: String
    public var clockText: String
    public var endDate: Date?
    public var countUpStart: Date?
    public var detail: String
    public var nextText: String

    public init(
        phase: String,
        indefinite: Bool,
        modeTitle: String,
        clockText: String,
        endDate: Date?,
        countUpStart: Date?,
        detail: String,
        nextText: String
    ) {
        self.phase = phase
        self.indefinite = indefinite
        self.modeTitle = modeTitle
        self.clockText = clockText
        self.endDate = endDate
        self.countUpStart = countUpStart
        self.detail = detail
        self.nextText = nextText
    }

    public static let placeholder = FocusWidgetSnapshot(
        phase: FocusPhase.idle.rawValue,
        indefinite: false,
        modeTitle: "专注",
        clockText: "25:00",
        endDate: nil,
        countUpStart: nil,
        detail: "打开 TokiNote 开始",
        nextText: ""
    )
}

public enum FocusWidgetStore {
    public static func save(_ snapshot: FocusWidgetSnapshot, defaults: UserDefaults = .standard, suite: UserDefaults? = UserDefaults(suiteName: FocusWidgetKind.appGroup)) {
        guard let data = try? JSONEncoder().encode(snapshot) else { return }
        defaults.set(data, forKey: FocusWidgetKind.snapshotKey)
        suite?.set(data, forKey: FocusWidgetKind.snapshotKey)
        defaults.synchronize()
        suite?.synchronize()
    }

    public static func load(defaults: UserDefaults = .standard, suite: UserDefaults? = UserDefaults(suiteName: FocusWidgetKind.appGroup)) -> FocusWidgetSnapshot? {
        let data = suite?.data(forKey: FocusWidgetKind.snapshotKey) ?? defaults.data(forKey: FocusWidgetKind.snapshotKey)
        guard let data else { return nil }
        return try? JSONDecoder().decode(FocusWidgetSnapshot.self, from: data)
    }
}

public enum FocusPresentation {
    public static func widgetSnapshot(
        state: FocusState,
        nowElapsed: Int64,
        nowWall: Int64,
        modeTitle: String,
        detail: String,
        nextText: String
    ) -> FocusWidgetSnapshot {
        let seconds = displaySeconds(state, now: nowElapsed)
        var endDate: Date?
        var countUpStart: Date?
        if state.running && !state.indefinite && state.deadlineElapsed > 0 {
            let remain = state.deadlineElapsed - nowElapsed
            endDate = Date(timeIntervalSince1970: TimeInterval(nowWall + remain) / 1000)
        }
        if state.indefinite {
            let elapsed = liveElapsedMillis(state, now: nowElapsed)
            countUpStart = Date(timeIntervalSince1970: TimeInterval(nowWall - elapsed) / 1000)
        }
        return FocusWidgetSnapshot(
            phase: state.phase.rawValue,
            indefinite: state.indefinite,
            modeTitle: modeTitle,
            clockText: formatElapsed(seconds),
            endDate: endDate,
            countUpStart: countUpStart,
            detail: detail,
            nextText: nextText
        )
    }
}

#if canImport(ActivityKit)
import ActivityKit

public struct FocusActivityAttributes: ActivityAttributes {
    public struct ContentState: Codable, Hashable, Sendable {
        public var modeTitle: String
        public var indefinite: Bool
        public var paused: Bool
        public var endDate: Date?
        public var countUpStart: Date?
        public var clockText: String
        public var detail: String

        public init(
            modeTitle: String,
            indefinite: Bool,
            paused: Bool,
            endDate: Date?,
            countUpStart: Date?,
            clockText: String,
            detail: String
        ) {
            self.modeTitle = modeTitle
            self.indefinite = indefinite
            self.paused = paused
            self.endDate = endDate
            self.countUpStart = countUpStart
            self.clockText = clockText
            self.detail = detail
        }
    }

    public var name: String

    public init(name: String) {
        self.name = name
    }
}
#endif
