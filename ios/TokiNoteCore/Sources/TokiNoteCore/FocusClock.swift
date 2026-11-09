import Darwin
import Foundation

/// Two clocks, matching the Android focus engine.
///
/// `elapsedRealtime` includes time the device spends asleep and ignores manual
/// clock changes. `wallClockMillis` is only used to re-anchor a deadline after
/// reboot, when the elapsed clock goes back to zero.
public protocol FocusClock: AnyObject {
    func elapsedRealtime() -> Int64
    func wallClockMillis() -> Int64
}

public final class SystemFocusClock: FocusClock {
    public init() {}

    public func elapsedRealtime() -> Int64 {
        Int64(clock_gettime_nsec_np(CLOCK_MONOTONIC) / 1_000_000)
    }

    public func wallClockMillis() -> Int64 {
        Int64(Date().timeIntervalSince1970 * 1000)
    }
}
