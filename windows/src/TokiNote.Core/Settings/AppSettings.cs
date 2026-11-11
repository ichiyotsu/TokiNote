namespace TokiNote;

public enum ThemeMode
{
    System,
    Light,
    Dark,
}

/// <summary>
/// Timer defaults match the Android app (25 / 5 / 15 / 4, auto-start on).
/// </summary>
public sealed record AppSettings
{
    public ThemeMode ThemeMode { get; init; } = ThemeMode.System;
    public bool Amoled { get; init; }
    public bool ReduceMotion { get; init; }
    public bool ShowTime { get; init; } = true;
    public int FocusMinutes { get; init; } = 25;
    public int ShortBreakMinutes { get; init; } = 5;
    public int LongBreakMinutes { get; init; } = 15;
    public int SessionsBeforeLongBreak { get; init; } = 4;
    public bool AutoStartNext { get; init; } = true;
}
