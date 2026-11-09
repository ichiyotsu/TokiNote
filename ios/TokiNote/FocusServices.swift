import ActivityKit
import Foundation
import TokiNoteCore
import UserNotifications
import WidgetKit

@MainActor
final class FocusNotificationScheduler {
    static let requestID = "focus.segment.complete"
    private let clock: FocusClock

    init(clock: FocusClock) {
        self.clock = clock
    }

    func reschedule(_ state: FocusState) async {
        let center = UNUserNotificationCenter.current()
        center.removePendingNotificationRequests(withIdentifiers: [Self.requestID])
        guard state.running, !state.indefinite, state.deadlineElapsed > 0 else { return }
        let remain = state.deadlineElapsed - clock.elapsedRealtime()
        guard remain > 800 else { return }
        let minutes = max(Int(state.totalMillis / 60_000), 0)
        guard minutes > 0 else { return }
        var settings = await center.notificationSettings()
        if settings.authorizationStatus == .notDetermined {
            _ = try? await center.requestAuthorization(options: [.alert, .sound])
            settings = await center.notificationSettings()
        }
        guard settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional else { return }
        let content = UNMutableNotificationContent()
        if state.mode == .focus {
            content.title = L10n.notifDoneFocus
            content.body = L10n.notifDoneFocusBody(formatMinutes(minutes))
        } else {
            content.title = L10n.notifDoneBreak
            content.body = L10n.notifDoneBreakBody(formatMinutes(minutes))
        }
        content.sound = .default
        let interval = max(1, TimeInterval(remain) / 1000)
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: interval, repeats: false)
        let request = UNNotificationRequest(identifier: Self.requestID, content: content, trigger: trigger)
        try? await center.add(request)
    }
}

@MainActor
final class FocusActivityCenter {
    private var activity: Activity<FocusActivityAttributes>?

    func sync(_ snapshot: FocusWidgetSnapshot) {
        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return }
        let contentState = FocusActivityAttributes.ContentState(
            modeTitle: snapshot.modeTitle,
            indefinite: snapshot.indefinite,
            paused: snapshot.phase == FocusPhase.paused.rawValue,
            endDate: snapshot.endDate,
            countUpStart: snapshot.countUpStart,
            clockText: snapshot.clockText,
            detail: snapshot.detail
        )
        let visible = snapshot.phase != FocusPhase.idle.rawValue
        guard visible else {
            end()
            return
        }
        let content = ActivityContent(state: contentState, staleDate: snapshot.endDate)
        if let activity {
            Task { await activity.update(content) }
            return
        }
        let attributes = FocusActivityAttributes(name: "TokiNote")
        activity = try? Activity.request(attributes: attributes, content: content, pushType: nil)
    }

    func end() {
        let current = activity
        activity = nil
        Task { await current?.end(nil, dismissalPolicy: .immediate) }
    }
}

final class NotificationPresenter: NSObject, UNUserNotificationCenterDelegate {
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        []
    }
}
