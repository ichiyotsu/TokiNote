import Foundation
import SwiftData
import SwiftUI
import TokiNoteCore
import UserNotifications
import WidgetKit

enum AppSection: String, CaseIterable, Hashable, Identifiable {
    case notes
    case focus
    case stats
    case settings

    var id: String { rawValue }

    var title: String {
        switch self {
        case .notes: return L10n.navNotes
        case .focus: return L10n.navFocus
        case .stats: return L10n.navStats
        case .settings: return L10n.navSettings
        }
    }

    var symbol: String {
        switch self {
        case .notes: return "note.text"
        case .focus: return "timer"
        case .stats: return "chart.bar"
        case .settings: return "gearshape"
        }
    }
}

struct TokiActions {
    var newNote: () -> Void
    var toggleFocus: () -> Void
    var resetFocus: () -> Void
    var skipFocus: () -> Void
    var showSection: (AppSection) -> Void
}

struct TokiActionsKey: FocusedValueKey {
    typealias Value = TokiActions
}

extension FocusedValues {
    var tokiActions: TokiActions? {
        get { self[TokiActionsKey.self] }
        set { self[TokiActionsKey.self] = newValue }
    }
}

@MainActor
@Observable
final class FocusController {
    private let engine: FocusTimerEngine
    private let clock = SystemFocusClock()
    private let notifications: FocusNotificationScheduler
    private let activities = FocusActivityCenter()
    private(set) var state: FocusTimerState
    var onSession: ((FocusSession) -> Void)?

    init(settings: AppSettings) {
        let group = UserDefaults(suiteName: FocusWidgetKind.appGroup).map {
            UserDefaultsFocusTimerPersistence(defaults: $0)
        }
        let persistence = MirroredFocusTimerPersistence(
            primary: UserDefaultsFocusTimerPersistence(),
            secondary: group
        )
        notifications = FocusNotificationScheduler(clock: clock)
        let engine = FocusTimerEngine(
            clock: clock,
            persistence: persistence,
            enableTasks: true,
            initialSettings: settings
        )
        self.engine = engine
        self.state = engine.state
        engine.sessionRecorder = { [weak self] session in
            Task { @MainActor in
                self?.onSession?(session)
            }
        }
        engine.onStateChange = { [weak self] newState in
            Task { @MainActor in
                self?.apply(newState)
            }
        }
        publish(engine.state)
    }

    func configure(_ settings: AppSettings) {
        engine.configure(settings)
        apply(engine.state)
    }

    func toggle() {
        engine.toggle()
        apply(engine.state)
    }

    func reset() {
        engine.reset()
        apply(engine.state)
    }

    func skip() {
        engine.skip()
        apply(engine.state)
    }

    func scheduleIndefinite(delayMs: Int64) {
        engine.scheduleIndefinite(delayMs: delayMs)
        apply(engine.state)
    }

    func sync() {
        engine.sync()
        apply(engine.state)
    }

    func snapshot(detail: String, nextText: String) -> FocusWidgetSnapshot {
        let now = clock.elapsedRealtime()
        return FocusPresentation.widgetSnapshot(
            state: state,
            nowElapsed: now,
            nowWall: clock.wallClockMillis(),
            modeTitle: state.indefinite ? L10n.focusIndefinite : L10n.modeTitle(state.mode),
            detail: detail,
            nextText: nextText
        )
    }

    var elapsedNow: Int64 { clock.elapsedRealtime() }

    private func apply(_ newState: FocusTimerState) {
        state = newState
        publish(newState)
    }

    private func publish(_ newState: FocusTimerState) {
        let detail = newState.indefinite
            ? L10n.focusIndefiniteHint
            : L10n.focusSession(newState.sessionIndex, max(newState.settings.sessionsBeforeLongBreak, 1))
        let next = L10n.focusNextHint(
            formatElapsed(newState.nextMode.minutes(newState.settings) * 60),
            L10n.modeTitle(newState.nextMode)
        )
        let snapshot = FocusPresentation.widgetSnapshot(
            state: newState,
            nowElapsed: clock.elapsedRealtime(),
            nowWall: clock.wallClockMillis(),
            modeTitle: newState.indefinite ? L10n.focusIndefinite : L10n.modeTitle(newState.mode),
            detail: detail,
            nextText: next
        )
        FocusWidgetStore.save(snapshot)
        WidgetCenter.shared.reloadTimelines(ofKind: FocusWidgetKind.home)
        activities.sync(snapshot)
        Task { await notifications.reschedule(newState) }
    }
}

@MainActor
@Observable
final class AppModel {
    let container: ModelContainer
    let context: ModelContext
    let focus: FocusController
    let notifications = NotificationPresenter()
    var settings: SettingsRecord
    var section: AppSection = .notes
    var selectedNoteID: UUID?
    var notesPath = NavigationPath()
    var pendingUpdate: IOSRelease?
    var updateStatus: String = ""
    var checkingUpdate = false
    private var didAutoCheck = false

    init(container: ModelContainer) {
        self.container = container
        let context = ModelContext(container)
        self.context = context
        let settings = Self.loadSettings(context)
        self.settings = settings
        let focus = FocusController(settings: settings.appSettings)
        self.focus = focus
        focus.onSession = { [weak self] session in
            self?.record(session)
        }
        purgeTrash()
        UNUserNotificationCenter.current().delegate = notifications
    }

    func createNote(pushOnPhone: Bool) {
        let note = NoteRecord()
        context.insert(note)
        try? context.save()
        section = .notes
        selectedNoteID = note.id
        if pushOnPhone {
            notesPath.append(note.id)
        }
    }

    func trash(_ note: NoteRecord) {
        note.deletedAt = .now
        note.pinned = false
        note.updatedAt = .now
        if selectedNoteID == note.id {
            selectedNoteID = nil
        }
        try? context.save()
    }

    func restore(_ note: NoteRecord) {
        note.deletedAt = nil
        note.updatedAt = .now
        try? context.save()
    }

    func deleteForever(_ note: NoteRecord) {
        context.delete(note)
        try? context.save()
    }

    func deleteIfBlank(_ note: NoteRecord) {
        guard note.deletedAt == nil, note.isBlank else { return }
        context.delete(note)
        if selectedNoteID == note.id {
            selectedNoteID = nil
        }
        try? context.save()
    }

    func purgeTrash() {
        let cutoff = Date().addingTimeInterval(-14 * 24 * 60 * 60)
        let notes = (try? context.fetch(FetchDescriptor<NoteRecord>())) ?? []
        var changed = false
        for note in notes {
            if let deleted = note.deletedAt, deleted < cutoff {
                context.delete(note)
                changed = true
            }
        }
        if changed { try? context.save() }
    }

    func saveSettings() {
        try? context.save()
        focus.configure(settings.appSettings)
    }

    func clearAll() {
        for note in (try? context.fetch(FetchDescriptor<NoteRecord>())) ?? [] {
            context.delete(note)
        }
        for session in (try? context.fetch(FetchDescriptor<FocusSessionRecord>())) ?? [] {
            context.delete(session)
        }
        settings.theme = .system
        settings.palette = .matcha
        settings.amoled = false
        settings.reduceMotion = false
        settings.showTime = true
        settings.focusMinutes = 25
        settings.shortBreakMinutes = 5
        settings.longBreakMinutes = 15
        settings.sessionsBeforeLongBreak = 4
        settings.autoStartNext = true
        try? context.save()
        focus.reset()
        focus.configure(settings.appSettings)
        selectedNoteID = nil
        notesPath = NavigationPath()
    }

    func checkForUpdate(manual: Bool) async {
        if checkingUpdate { return }
        if !manual && didAutoCheck { return }
        didAutoCheck = true
        checkingUpdate = true
        if manual { updateStatus = L10n.checking }
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "0.1.0"
        let result = await IOSReleaseIndex.check(currentVersion: version)
        checkingUpdate = false
        switch result {
        case .update(let release):
            pendingUpdate = release
            updateStatus = ""
        case .upToDate:
            if manual { updateStatus = L10n.aboutLatest }
        case .noRelease:
            if manual { updateStatus = L10n.aboutNoRelease }
        case .failed:
            if manual { updateStatus = L10n.aboutCheckFailed }
        }
    }

    private func record(_ session: FocusSession) {
        let record = FocusSessionRecord(
            endedAt: Date(timeIntervalSince1970: TimeInterval(session.endedAt) / 1000),
            minutes: session.minutes,
            breakMinutes: session.breakMinutes
        )
        context.insert(record)
        try? context.save()
    }

    private static func loadSettings(_ context: ModelContext) -> SettingsRecord {
        var descriptor = FetchDescriptor<SettingsRecord>()
        descriptor.fetchLimit = 1
        if let existing = try? context.fetch(descriptor).first {
            return existing
        }
        let created = SettingsRecord()
        context.insert(created)
        try? context.save()
        return created
    }
}
