import Foundation

public protocol FocusTimerPersistence: AnyObject {
    func load() -> FocusTimerSnapshot?
    func save(_ snapshot: FocusTimerSnapshot)
    func clear()
}

public final class UserDefaultsFocusTimerPersistence: FocusTimerPersistence {
    private let defaults: UserDefaults
    private let key: String

    public init(defaults: UserDefaults = .standard, key: String = "focus_timer_snapshot") {
        self.defaults = defaults
        self.key = key
    }

    public func load() -> FocusTimerSnapshot? {
        guard let raw = defaults.string(forKey: key) else { return nil }
        return FocusTimerSnapshot.decode(raw)
    }

    public func save(_ snapshot: FocusTimerSnapshot) {
        defaults.set(snapshot.encode(), forKey: key)
        defaults.synchronize()
    }

    public func clear() {
        defaults.removeObject(forKey: key)
        defaults.synchronize()
    }
}

/// Writes the process-local snapshot and, when the App Group is available, the
/// copy the widget reads. A missing group must not block the in-app timer.
public final class MirroredFocusTimerPersistence: FocusTimerPersistence {
    private let primary: FocusTimerPersistence
    private let secondary: FocusTimerPersistence?

    public init(primary: FocusTimerPersistence, secondary: FocusTimerPersistence?) {
        self.primary = primary
        self.secondary = secondary
    }

    public func load() -> FocusTimerSnapshot? {
        primary.load() ?? secondary?.load()
    }

    public func save(_ snapshot: FocusTimerSnapshot) {
        primary.save(snapshot)
        secondary?.save(snapshot)
    }

    public func clear() {
        primary.clear()
        secondary?.clear()
    }
}
