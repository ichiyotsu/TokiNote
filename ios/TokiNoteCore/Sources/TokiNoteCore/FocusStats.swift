import Foundation

public struct FocusStatsSummary: Equatable, Sendable {
    public var todayFocus: Int
    public var todayBreak: Int
    public var weekFocus: [Int]
    public var weekAverage: Int
    public var monthFocus: [Int]
    public var monthAverage: Int
    public var weekDays: [Date]
    public var monthDays: [Date]
}

public enum FocusStats {
    public static func summarize(
        sessions: [FocusSession],
        today: Date,
        calendar: Calendar = .current
    ) -> FocusStatsSummary {
        let todayStart = calendar.startOfDay(for: today)
        let weekDays = (0..<7).reversed().map { offset in
            calendar.date(byAdding: .day, value: -offset, to: todayStart) ?? todayStart
        }
        let monthDays = (0..<30).reversed().map { offset in
            calendar.date(byAdding: .day, value: -offset, to: todayStart) ?? todayStart
        }
        let todayFocus = sum(sessions, on: todayStart, calendar: calendar, breakTime: false)
        let todayBreak = sum(sessions, on: todayStart, calendar: calendar, breakTime: true)
        let weekFocus = weekDays.map { sum(sessions, on: $0, calendar: calendar, breakTime: false) }
        let monthFocus = monthDays.map { sum(sessions, on: $0, calendar: calendar, breakTime: false) }
        return FocusStatsSummary(
            todayFocus: todayFocus,
            todayBreak: todayBreak,
            weekFocus: weekFocus,
            weekAverage: weekFocus.reduce(0, +) / 7,
            monthFocus: monthFocus,
            monthAverage: monthFocus.reduce(0, +) / 30,
            weekDays: weekDays,
            monthDays: monthDays
        )
    }

    private static func sum(_ sessions: [FocusSession], on day: Date, calendar: Calendar, breakTime: Bool) -> Int {
        sessions.reduce(0) { partial, session in
            let date = Date(timeIntervalSince1970: TimeInterval(session.endedAt) / 1000)
            guard calendar.isDate(date, inSameDayAs: day) else { return partial }
            return partial + (breakTime ? session.breakMinutes : session.minutes)
        }
    }
}
