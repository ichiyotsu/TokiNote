namespace TokiNote;

public enum FocusPhase
{
    Idle,
    Running,
    Paused,
}

public enum FocusMode
{
    Focus,
    ShortBreak,
    LongBreak,
}

public abstract record FocusTimerEvent(int Minutes, long EndedAt)
{
    public sealed record FocusCompleted(int Minutes, long EndedAt) : FocusTimerEvent(Minutes, EndedAt);
    public sealed record BreakCompleted(int Minutes, long EndedAt) : FocusTimerEvent(Minutes, EndedAt);

    public FocusSession ToSession() => this switch
    {
        FocusCompleted focus => new FocusSession(focus.EndedAt, focus.Minutes, 0),
        BreakCompleted breakEvent => new FocusSession(breakEvent.EndedAt, 0, breakEvent.Minutes),
        _ => throw new InvalidOperationException(),
    };
}

public sealed record FocusSession(long EndedAt, int Minutes, int BreakMinutes = 0);

public interface IFocusClock
{
    long ElapsedRealtime();
    long WallClockMillis();
}

public sealed class SystemFocusClock : IFocusClock
{
    public long ElapsedRealtime() => Environment.TickCount64;

    public long WallClockMillis() => DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
}

public interface IFocusTimerPersistence
{
    FocusTimerSnapshot? Load();
    void Save(FocusTimerSnapshot snapshot);
    void Clear();
}

public sealed class FileFocusTimerPersistence : IFocusTimerPersistence
{
    private readonly string _path;

    public FileFocusTimerPersistence(string path) => _path = path;

    public FocusTimerSnapshot? Load()
    {
        if (!File.Exists(_path))
        {
            return null;
        }

        return FocusTimerSnapshot.Decode(File.ReadAllText(_path));
    }

    public void Save(FocusTimerSnapshot snapshot)
    {
        var directory = Path.GetDirectoryName(_path);
        if (!string.IsNullOrEmpty(directory))
        {
            Directory.CreateDirectory(directory);
        }

        var temp = _path + ".tmp";
        File.WriteAllText(temp, snapshot.Encode());
        File.Move(temp, _path, overwrite: true);
    }

    public void Clear()
    {
        if (File.Exists(_path))
        {
            File.Delete(_path);
        }
    }
}

/// <summary>
/// Restorable timer anchors. Elapsed time is the monotonic clock; after a reboot
/// that clock resets and the wall clock converts the anchors.
/// </summary>
public sealed record FocusTimerSnapshot(
    FocusPhase Phase,
    FocusMode Mode,
    bool Indefinite,
    long TotalMillis,
    long RemainingMillis,
    long DeadlineElapsed,
    long ElapsedBaseMillis,
    long RunStartedElapsed,
    int CompletedInCycle,
    int SegmentMinutes,
    int FocusMinutes,
    int ShortBreakMinutes,
    int LongBreakMinutes,
    int SessionsBeforeLongBreak,
    bool AutoStartNext,
    long SavedAtElapsed,
    long SavedAtWall)
{
    public const string Version = "1";

    public string Encode() => string.Join('\n',
        Version,
        Phase.ToString(),
        Mode.ToString(),
        Indefinite ? "1" : "0",
        TotalMillis.ToString(),
        RemainingMillis.ToString(),
        DeadlineElapsed.ToString(),
        ElapsedBaseMillis.ToString(),
        RunStartedElapsed.ToString(),
        CompletedInCycle.ToString(),
        SegmentMinutes.ToString(),
        FocusMinutes.ToString(),
        ShortBreakMinutes.ToString(),
        LongBreakMinutes.ToString(),
        SessionsBeforeLongBreak.ToString(),
        AutoStartNext ? "1" : "0",
        SavedAtElapsed.ToString(),
        SavedAtWall.ToString());

    public static FocusTimerSnapshot? Decode(string raw)
    {
        try
        {
            var lines = raw.Replace("\r", "").Split('\n');
            if (lines.Length < 18 || lines[0] != Version)
            {
                return null;
            }

            if (!Enum.TryParse(lines[1], out FocusPhase phase) || !Enum.TryParse(lines[2], out FocusMode mode))
            {
                return null;
            }

            return new FocusTimerSnapshot(
                phase,
                mode,
                lines[3] == "1",
                long.Parse(lines[4]),
                long.Parse(lines[5]),
                long.Parse(lines[6]),
                long.Parse(lines[7]),
                long.Parse(lines[8]),
                int.Parse(lines[9]),
                int.Parse(lines[10]),
                int.Parse(lines[11]),
                int.Parse(lines[12]),
                int.Parse(lines[13]),
                int.Parse(lines[14]),
                lines[15] == "1",
                long.Parse(lines[16]),
                long.Parse(lines[17]));
        }
        catch
        {
            return null;
        }
    }
}

public static class FocusMath
{
    public static int CeilSeconds(long millis)
    {
        if (millis <= 0)
        {
            return 0;
        }

        var value = (millis + 999L) / 1000L;
        return (int)Math.Min(value, int.MaxValue);
    }

    public static int MinutesOf(FocusMode mode, AppSettings settings) => mode switch
    {
        FocusMode.Focus => settings.FocusMinutes,
        FocusMode.ShortBreak => settings.ShortBreakMinutes,
        FocusMode.LongBreak => settings.LongBreakMinutes,
        _ => settings.FocusMinutes,
    };

    public static FocusMode NextFocusMode(FocusMode mode, int completedInCycle, int sessionsBeforeLongBreak)
    {
        var cycle = Math.Max(1, sessionsBeforeLongBreak);
        return mode switch
        {
            FocusMode.Focus => (completedInCycle + 1) % cycle == 0 ? FocusMode.LongBreak : FocusMode.ShortBreak,
            _ => FocusMode.Focus,
        };
    }

    public static int FilledSessionCount(int completedInCycle, int cycleLength, FocusMode mode)
    {
        var cycle = Math.Max(1, cycleLength);
        if (mode != FocusMode.Focus && completedInCycle > 0 && completedInCycle % cycle == 0)
        {
            return cycle;
        }

        var remainder = completedInCycle % cycle;
        return remainder < 0 ? remainder + cycle : remainder;
    }
}

public sealed class FocusState
{
    public FocusState(
        FocusMode mode = FocusMode.Focus,
        bool running = false,
        int remainingSeconds = 25 * 60,
        int totalSeconds = 25 * 60,
        int completedInCycle = 0,
        AppSettings? settings = null,
        bool indefinite = false,
        int elapsedSeconds = 0,
        FocusPhase? phase = null,
        long? remainingMillis = null,
        long? totalMillis = null,
        long deadlineElapsed = 0,
        long? elapsedBaseMillis = null,
        long runStartedElapsed = 0)
    {
        Mode = mode;
        Running = running;
        RemainingSeconds = remainingSeconds;
        TotalSeconds = totalSeconds;
        CompletedInCycle = completedInCycle;
        Settings = settings ?? new AppSettings();
        Indefinite = indefinite;
        ElapsedSeconds = elapsedSeconds;
        Phase = phase ?? (running ? FocusPhase.Running : FocusPhase.Idle);
        RemainingMillis = remainingMillis ?? remainingSeconds * 1000L;
        TotalMillis = totalMillis ?? totalSeconds * 1000L;
        DeadlineElapsed = deadlineElapsed;
        ElapsedBaseMillis = elapsedBaseMillis ?? elapsedSeconds * 1000L;
        RunStartedElapsed = runStartedElapsed;
    }

    public FocusMode Mode { get; }
    public bool Running { get; }
    public int RemainingSeconds { get; }
    public int TotalSeconds { get; }
    public int CompletedInCycle { get; }
    public AppSettings Settings { get; }
    public bool Indefinite { get; }
    public int ElapsedSeconds { get; }
    public FocusPhase Phase { get; }
    public long RemainingMillis { get; }
    public long TotalMillis { get; }
    public long DeadlineElapsed { get; }
    public long ElapsedBaseMillis { get; }
    public long RunStartedElapsed { get; }

    public float Progress => TotalMillis <= 0
        ? 0f
        : Math.Clamp(1f - (float)RemainingMillis / TotalMillis, 0f, 1f);

    public FocusMode NextMode => FocusMath.NextFocusMode(Mode, CompletedInCycle, Settings.SessionsBeforeLongBreak);

    public int SessionIndex
    {
        get
        {
            var cycle = Math.Max(1, Settings.SessionsBeforeLongBreak);
            var mod = CompletedInCycle % cycle;
            if (mod < 0)
            {
                mod += cycle;
            }

            return mod + 1;
        }
    }
}

public static class FocusDisplay
{
    public static long LiveRemainingMillis(FocusState state, long now)
    {
        if (state.Running && !state.Indefinite && state.DeadlineElapsed > 0)
        {
            return Math.Max(0, state.DeadlineElapsed - now);
        }

        return state.RemainingMillis;
    }

    public static long LiveElapsedMillis(FocusState state, long now)
    {
        if (!state.Indefinite)
        {
            return 0;
        }

        if (state.Running)
        {
            return state.ElapsedBaseMillis + Math.Max(0, now - state.RunStartedElapsed);
        }

        return state.ElapsedBaseMillis;
    }

    public static int ShowSeconds(FocusState state, long now) =>
        state.Indefinite
            ? (int)(LiveElapsedMillis(state, now) / 1000L)
            : FocusMath.CeilSeconds(LiveRemainingMillis(state, now));
}
