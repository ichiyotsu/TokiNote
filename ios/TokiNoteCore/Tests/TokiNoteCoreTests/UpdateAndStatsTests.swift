import Foundation
import Testing
@testable import TokiNoteCore

struct UpdateCheckerTests {
    @Test("patch and minor bumps are newer")
    func patchAndMinorBumpsAreNewer() {
        #expect(IOSReleaseIndex.isNewer("1.0.1", than: "1.0.0"))
        #expect(IOSReleaseIndex.isNewer("1.1.0", than: "1.0.9"))
        #expect(IOSReleaseIndex.isNewer("v2.0.0", than: "1.9.9"))
    }

    @Test("same version is not newer")
    func sameVersionIsNotNewer() {
        #expect(!IOSReleaseIndex.isNewer("1.0.0", than: "1.0.0"))
        #expect(!IOSReleaseIndex.isNewer("1.0", than: "1.0.0"))
    }

    @Test("stable beats its own pre-release")
    func stableBeatsItsOwnPreRelease() {
        #expect(!IOSReleaseIndex.isNewer("1.0.0-beta.2", than: "1.0.0"))
        #expect(IOSReleaseIndex.isNewer("1.0.0-beta.3", than: "1.0.0-beta.2"))
        #expect(IOSReleaseIndex.isNewer("1.0.1-beta", than: "1.0.0"))
    }

    @Test("ios tags ignore android releases and drafts")
    func iosTagsIgnoreAndroidReleasesAndDrafts() throws {
        let json = """
        [
          {"tag_name":"0.3.7","draft":false,"body":"android","html_url":"https://example.com/a","assets":[{"name":"TokiNote-0.3.7.apk","browser_download_url":"https://example.com/a.apk"}]},
          {"tag_name":"ios-0.1.0","draft":false,"body":"first","html_url":"https://example.com/ios","published_at":"2026-11-01T00:00:00Z","assets":[{"name":"TokiNote-ios-0.1.0.ipa","browser_download_url":"https://example.com/ios.ipa"},{"name":"notes.txt","browser_download_url":"https://example.com/n.txt"}]},
          {"tag_name":"ios-0.2.0-beta","draft":false,"body":"beta","html_url":"https://example.com/beta","published_at":"2026-11-02T00:00:00Z","assets":[{"name":"TokiNote-ios-0.2.0-beta.ipa","browser_download_url":"https://example.com/beta.ipa"}]},
          {"tag_name":"ios-9.0.0","draft":true,"body":"hidden","html_url":"https://example.com/draft","assets":[{"name":"TokiNote-ios-9.0.0.ipa","browser_download_url":"https://example.com/draft.ipa"}]},
          {"tag_name":"ios-0.2.0","draft":false,"body":"stable","html_url":"https://example.com/stable","published_at":"2026-11-03T00:00:00Z","assets":[{"name":"extra.ipa","browser_download_url":"https://example.com/extra.ipa"},{"name":"TokiNote-ios-0.2.0.ipa","browser_download_url":"https://example.com/stable.ipa"}]}
        ]
        """.data(using: .utf8)!
        let releases = try #require(IOSReleaseIndex.parse(data: json))
        #expect(releases.map(\.tag) == ["ios-0.1.0", "ios-0.2.0-beta", "ios-0.2.0"])
        let newest = try #require(IOSReleaseIndex.newest(releases))
        #expect(newest.version == "0.2.0")
        #expect(newest.ipaURL?.absoluteString == "https://example.com/stable.ipa")
        #expect(IOSReleaseIndex.check(data: json, currentVersion: "0.1.0") == .update(newest))
        #expect(IOSReleaseIndex.check(data: json, currentVersion: "0.2.0") == .upToDate)
        let sideStore = try #require(IOSReleaseIndex.sideStoreInstallURL(for: newest.ipaURL!))
        let parts = try #require(URLComponents(url: sideStore, resolvingAgainstBaseURL: false))
        #expect(parts.scheme == "sidestore")
        #expect(parts.host == "install")
        #expect(parts.queryItems?.first?.name == "url")
        #expect(parts.queryItems?.first?.value == newest.ipaURL?.absoluteString)
    }

    @Test("0.3.7 is newer and other platform tags are ignored")
    func version037IgnoresOtherPlatforms() throws {
        #expect(IOSReleaseIndex.isNewer("0.3.7", than: "0.3.6"))
        #expect(!IOSReleaseIndex.isNewer("0.3.6", than: "0.3.7"))
        #expect(!IOSReleaseIndex.isNewer("0.3.7", than: "0.3.7"))
        #expect(IOSReleaseIndex.isNewer("0.3.7", than: "0.3.7-beta.4"))
        #expect(IOSReleaseIndex.version(fromTag: "ios-0.3.7") == "0.3.7")
        #expect(IOSReleaseIndex.version(fromTag: "0.3.7") == nil)
        #expect(IOSReleaseIndex.version(fromTag: "windows-0.3.7") == nil)
        let json = """
        [
          {"tag_name":"0.3.7","draft":false,"body":"android","assets":[{"name":"TokiNote-0.3.7.apk","browser_download_url":"https://example.com/a.apk"}]},
          {"tag_name":"windows-0.3.7","draft":false,"body":"windows","assets":[{"name":"TokiNote-Windows-0.3.7-win-x64.zip","browser_download_url":"https://example.com/w.zip"}]},
          {"tag_name":"ios-0.3.6","draft":false,"body":"old","published_at":"2026-11-01T00:00:00Z","assets":[{"name":"TokiNote-ios-0.3.6.ipa","browser_download_url":"https://example.com/old.ipa"}]},
          {"tag_name":"ios-0.3.7","draft":false,"body":"new","published_at":"2026-11-11T00:00:00Z","assets":[{"name":"TokiNote-ios-0.3.7.ipa","browser_download_url":"https://example.com/new.ipa"}]}
        ]
        """.data(using: .utf8)!
        let newest = try #require(IOSReleaseIndex.newest(try #require(IOSReleaseIndex.parse(data: json))))
        #expect(newest.tag == "ios-0.3.7")
        #expect(newest.version == "0.3.7")
        #expect(newest.ipaURL?.absoluteString == "https://example.com/new.ipa")
        #expect(IOSReleaseIndex.check(data: json, currentVersion: "0.3.6") == .update(newest))
        #expect(IOSReleaseIndex.check(data: json, currentVersion: "0.3.7") == .upToDate)
    }

    @Test("no ios release is reported separately from failure")
    func noIOSRelease() throws {
        let json = """
        [{"tag_name":"0.3.5","draft":false,"body":"","assets":[]}]
        """.data(using: .utf8)!
        #expect(IOSReleaseIndex.check(data: json, currentVersion: "0.1.0") == .noRelease)
        #expect(IOSReleaseIndex.parse(data: Data("not-json".utf8)) == nil)
    }
}

struct FocusStatsTests {
    @Test("today week and month use the android windows")
    func todayWeekAndMonth() {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(secondsFromGMT: 0)!
        let today = calendar.date(from: DateComponents(year: 2026, month: 11, day: 9, hour: 15))!
        func millis(_ day: Int, _ hour: Int = 12) -> Int64 {
            let date = calendar.date(from: DateComponents(year: 2026, month: 11, day: day, hour: hour))!
            return Int64(date.timeIntervalSince1970 * 1000)
        }
        let sessions = [
            FocusSession(endedAt: millis(9), minutes: 25),
            FocusSession(endedAt: millis(9, 18), minutes: 0, breakMinutes: 5),
            FocusSession(endedAt: millis(8), minutes: 50),
            FocusSession(endedAt: millis(3), minutes: 10),
        ]
        let summary = FocusStats.summarize(sessions: sessions, today: today, calendar: calendar)
        #expect(summary.todayFocus == 25)
        #expect(summary.todayBreak == 5)
        #expect(summary.weekFocus.count == 7)
        #expect(summary.weekFocus.last == 25)
        #expect(summary.weekFocus.first == 10)
        #expect(summary.weekAverage == (25 + 50 + 10) / 7)
        #expect(summary.monthFocus.count == 30)
        #expect(summary.monthAverage == (25 + 50 + 10) / 30)
        #expect(formatDurationLabel(0) == "0 分")
        #expect(formatDurationLabel(125) == "2 小时 5 分")
    }
}
