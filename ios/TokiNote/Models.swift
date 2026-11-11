import Foundation
import SwiftData
import TokiNoteCore

enum ThemeChoice: String, Codable, CaseIterable, Identifiable {
    case system
    case light
    case dark
    var id: String { rawValue }
}

enum PaletteChoice: String, Codable, CaseIterable, Identifiable {
    case matcha
    case blue
    case purple
    case orange
    var id: String { rawValue }

    var title: String {
        switch self {
        case .matcha: return L10n.colorDefault
        case .blue: return L10n.colorBlue
        case .purple: return L10n.colorPurple
        case .orange: return L10n.colorOrange
        }
    }
}

@Model
final class NoteRecord {
    var id: UUID
    var title: String
    var body: String
    var pinned: Bool
    var createdAt: Date
    var updatedAt: Date
    var deletedAt: Date?

    init(
        id: UUID = UUID(),
        title: String = "",
        body: String = "",
        pinned: Bool = false,
        createdAt: Date = .now,
        updatedAt: Date = .now,
        deletedAt: Date? = nil
    ) {
        self.id = id
        self.title = title
        self.body = body
        self.pinned = pinned
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.deletedAt = deletedAt
    }

    var isBlank: Bool {
        title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
            body.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    var displayTitle: String {
        let trimmed = title.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? L10n.noteUntitled : trimmed
    }
}

@Model
final class FocusSessionRecord {
    var endedAt: Date
    var minutes: Int
    var breakMinutes: Int

    init(endedAt: Date, minutes: Int, breakMinutes: Int) {
        self.endedAt = endedAt
        self.minutes = minutes
        self.breakMinutes = breakMinutes
    }

    var session: FocusSession {
        FocusSession(endedAt: Int64(endedAt.timeIntervalSince1970 * 1000), minutes: minutes, breakMinutes: breakMinutes)
    }
}

@Model
final class SettingsRecord {
    var themeRaw: String
    var paletteRaw: String
    var amoled: Bool
    var reduceMotion: Bool
    var showTime: Bool
    var focusMinutes: Int
    var shortBreakMinutes: Int
    var longBreakMinutes: Int
    var sessionsBeforeLongBreak: Int
    var autoStartNext: Bool

    init() {
        themeRaw = ThemeChoice.system.rawValue
        paletteRaw = PaletteChoice.matcha.rawValue
        amoled = false
        reduceMotion = false
        showTime = true
        focusMinutes = 25
        shortBreakMinutes = 5
        longBreakMinutes = 15
        sessionsBeforeLongBreak = 4
        autoStartNext = true
    }

    var theme: ThemeChoice {
        get { ThemeChoice(rawValue: themeRaw) ?? .system }
        set { themeRaw = newValue.rawValue }
    }

    var palette: PaletteChoice {
        get { PaletteChoice(rawValue: paletteRaw) ?? .matcha }
        set { paletteRaw = newValue.rawValue }
    }

    var appSettings: AppSettings {
        AppSettings(
            amoled: amoled,
            focusMinutes: focusMinutes,
            shortBreakMinutes: shortBreakMinutes,
            longBreakMinutes: longBreakMinutes,
            sessionsBeforeLongBreak: sessionsBeforeLongBreak,
            autoStartNext: autoStartNext
        )
    }
}
