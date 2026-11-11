import Testing
@testable import TokiNoteCore

struct FormatTests {
    @Test("clock formats minutes and seconds")
    func clockFormatsMinutesAndSeconds() {
        #expect(formatClock(1500) == "25:00")
        #expect(formatClock(9) == "00:09")
        #expect(formatClock(0) == "00:00")
    }

    @Test("elapsed shows hours once it passes sixty minutes")
    func elapsedShowsHoursOnceItPassesSixtyMinutes() {
        #expect(formatElapsed(1500) == "25:00")
        #expect(formatElapsed(3599) == "59:59")
        #expect(formatElapsed(3600) == "1:00:00")
        #expect(formatElapsed(3661) == "1:01:01")
    }

    @Test("minutes formats hours and minutes")
    func minutesFormatsHoursAndMinutes() {
        #expect(formatMinutes(25) == "25 分钟")
        #expect(formatMinutes(65) == "1 小时 5 分钟")
        #expect(formatMinutes(120) == "2 小时")
    }
}
