import Testing
@testable import TokiNoteCore

struct FocusTimerStateTests {
    private let settings = AppSettings(sessionsBeforeLongBreak: 4)

    @Test("early focus sessions lead to a short break")
    func earlyFocusSessionsLeadToAShortBreak() {
        let state = FocusTimerState(mode: .focus, completedInCycle: 0, settings: settings)
        #expect(state.nextMode == .shortBreak)
    }

    @Test("every fourth focus session leads to a long break")
    func everyFourthFocusSessionLeadsToALongBreak() {
        let state = FocusTimerState(mode: .focus, completedInCycle: 3, settings: settings)
        #expect(state.nextMode == .longBreak)
    }

    @Test("breaks always lead back to focus")
    func breaksAlwaysLeadBackToFocus() {
        let shortBreak = FocusTimerState(mode: .shortBreak, settings: settings)
        let longBreak = FocusTimerState(mode: .longBreak, settings: settings)
        #expect(shortBreak.nextMode == .focus)
        #expect(longBreak.nextMode == .focus)
    }

    @Test("progress reflects elapsed time")
    func progressReflectsElapsedTime() {
        let half = FocusTimerState(remainingSeconds: 750, totalSeconds: 1500)
        let fresh = FocusTimerState(remainingSeconds: 1500, totalSeconds: 1500)
        #expect(abs(half.progress - 0.5) < 0.001)
        #expect(abs(fresh.progress) < 0.001)
    }

    @Test("session index wraps inside the cycle")
    func sessionIndexWrapsInsideTheCycle() {
        #expect(FocusTimerState(completedInCycle: 0, settings: settings).sessionIndex == 1)
        #expect(FocusTimerState(completedInCycle: 3, settings: settings).sessionIndex == 4)
        #expect(FocusTimerState(completedInCycle: 4, settings: settings).sessionIndex == 1)
    }

    @Test("session dots reset when the next focus cycle starts")
    func sessionDotsResetWhenTheNextFocusCycleStarts() {
        #expect(filledSessionCount(completedInCycle: 4, cycleLength: 4, mode: .longBreak) == 4)
        #expect(filledSessionCount(completedInCycle: 4, cycleLength: 4, mode: .focus) == 0)
        #expect(filledSessionCount(completedInCycle: 1, cycleLength: 4, mode: .shortBreak) == 1)
        #expect(filledSessionCount(completedInCycle: 0, cycleLength: 4, mode: .focus) == 0)
    }

    @Test("zero length cycle does not divide by zero")
    func zeroLengthCycleDoesNotDivideByZero() {
        let state = FocusTimerState(settings: AppSettings(sessionsBeforeLongBreak: 0))
        #expect(state.nextMode == .longBreak)
        #expect(state.sessionIndex == 1)
    }
}
