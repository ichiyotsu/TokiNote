namespace TokiNote.Tests;

public class FocusTimerEngineTests
{
    private readonly AppSettings _settings = new()
    {
        FocusMinutes = 25,
        ShortBreakMinutes = 5,
        LongBreakMinutes = 15,
        SessionsBeforeLongBreak = 4,
        AutoStartNext = false,
    };

    [Fact]
    public void Countdown_follows_elapsed_time_instead_of_tick_count()
    {
        var clock = new ManualClock();
        using var engine = Create(clock);
        engine.Toggle();
        clock.Advance(10 * 60_000L);
        engine.Sync();

        Assert.Equal(15 * 60, engine.State.RemainingSeconds);
        Assert.Equal(900_000L, engine.State.RemainingMillis);
        Assert.True(engine.State.Running);
    }

    [Fact]
    public void Display_keeps_the_current_second_until_it_is_fully_consumed()
    {
        var clock = new ManualClock();
        using var engine = Create(clock);
        engine.Toggle();

        clock.Advance(999);
        engine.Sync();
        Assert.Equal(25 * 60, engine.State.RemainingSeconds);

        clock.Advance(1);
        engine.Sync();
        Assert.Equal(25 * 60 - 1, engine.State.RemainingSeconds);
    }

    [Fact]
    public void Pause_and_resume_keep_the_partial_second()
    {
        var clock = new ManualClock();
        using var engine = Create(clock);
        engine.Toggle();
        clock.Advance(1_500);
        engine.Pause();

        Assert.Equal(1_500_000L - 1_500L, engine.State.RemainingMillis);
        Assert.False(engine.State.Running);

        engine.Toggle();
        clock.Advance(500);
        engine.Sync();

        Assert.Equal(1_498_000L, engine.State.RemainingMillis);
        Assert.Equal(24 * 60 + 58, engine.State.RemainingSeconds);
    }

    [Fact]
    public void Background_gap_is_applied_on_the_next_sync()
    {
        var clock = new ManualClock();
        using var engine = Create(clock, _settings with { AutoStartNext = true });
        engine.Toggle();
        clock.Advance(25 * 60_000L - 1_000L);
        engine.Sync();
        Assert.True(engine.State.Running);
        Assert.Equal(FocusMode.Focus, engine.State.Mode);

        clock.Advance(1_000L);
        engine.Sync();

        Assert.Equal(FocusMode.ShortBreak, engine.State.Mode);
        Assert.True(engine.State.Running);
    }

    [Fact]
    public void Completion_records_the_planned_minutes_once()
    {
        var clock = new ManualClock();
        var recorded = new List<FocusSession>();
        using var engine = Create(clock, _settings with { AutoStartNext = true }, recorded);
        engine.Toggle();
        clock.Advance(25 * 60_000L);
        engine.Sync();
        engine.Sync();

        Assert.Equal([new FocusSession(clock.Wall, 25)], recorded);
        Assert.Equal(1, engine.State.CompletedInCycle);
        Assert.Equal(FocusMode.ShortBreak, engine.State.Mode);
    }

    [Fact]
    public void A_late_catch_up_does_not_invent_the_following_segments()
    {
        var clock = new ManualClock();
        var recorded = new List<FocusSession>();
        using var engine = Create(clock, _settings with { AutoStartNext = true }, recorded);
        engine.Toggle();
        clock.Advance(25 * 60_000L + 3_001L);
        engine.Sync();

        var session = Assert.Single(recorded);
        Assert.Equal(25, session.Minutes);
        Assert.Equal(FocusMode.ShortBreak, engine.State.Mode);
        Assert.False(engine.State.Running);
        Assert.Equal(FocusPhase.Idle, engine.State.Phase);
    }

    [Fact]
    public void Completion_inside_the_grace_window_still_auto_starts()
    {
        var clock = new ManualClock();
        using var engine = Create(clock, _settings with { AutoStartNext = true });
        engine.Toggle();
        clock.Advance(25 * 60_000L + 3_000L);
        engine.Sync();

        Assert.True(engine.State.Running);
        Assert.Equal(FocusMode.ShortBreak, engine.State.Mode);
    }

    [Fact]
    public void Finishing_a_break_returns_to_focus_and_records_the_break()
    {
        var clock = new ManualClock();
        var recorded = new List<FocusSession>();
        using var engine = Create(clock, recorder: recorded);
        engine.Toggle();
        clock.Advance(25 * 60_000L);
        engine.Sync();
        engine.Toggle();
        clock.Advance(5 * 60_000L);
        engine.Sync();

        Assert.Equal(new FocusSession(clock.Wall - 5 * 60_000L, 25), recorded[0]);
        Assert.Equal(new FocusSession(clock.Wall, 0, 5), recorded[1]);
        Assert.Equal(FocusMode.Focus, engine.State.Mode);
        Assert.False(engine.State.Running);
    }

    [Fact]
    public void Skip_uses_the_same_next_segment_as_the_on_screen_hint()
    {
        var clock = new ManualClock();
        using var engine = Create(clock);
        for (var index = 0; index < 3; index++)
        {
            engine.Toggle();
            clock.Advance(25 * 60_000L);
            engine.Sync();
            engine.Toggle();
            clock.Advance(5 * 60_000L);
            engine.Sync();
        }

        Assert.Equal(3, engine.State.CompletedInCycle);
        Assert.Equal(FocusMode.Focus, engine.State.Mode);
        Assert.Equal(FocusMode.LongBreak, engine.State.NextMode);

        engine.Skip();

        Assert.Equal(FocusMode.LongBreak, engine.State.Mode);
        Assert.False(engine.State.Indefinite);
        Assert.False(engine.State.Running);
        Assert.Equal(15 * 60, engine.State.RemainingSeconds);
    }

    [Fact]
    public void Skip_leaves_indefinite_mode()
    {
        var clock = new ManualClock();
        var recorded = new List<FocusSession>();
        using var engine = Create(clock, recorder: recorded);
        engine.StartIndefinite();
        clock.Advance(90_000);
        engine.Skip();

        Assert.False(engine.State.Indefinite);
        Assert.Equal(FocusMode.ShortBreak, engine.State.Mode);
        Assert.Empty(recorded);
    }

    [Fact]
    public void Indefinite_time_keeps_elapsed_time_across_pause()
    {
        var clock = new ManualClock();
        using var engine = Create(clock);
        engine.StartIndefinite();
        clock.Advance(5_000);
        engine.Sync();
        Assert.Equal(5, engine.State.ElapsedSeconds);

        clock.Advance(90_000);
        engine.Sync();
        Assert.Equal(95, engine.State.ElapsedSeconds);

        engine.Pause();
        var paused = engine.State.ElapsedSeconds;
        clock.Advance(30_000);
        engine.Sync();
        Assert.Equal(paused, engine.State.ElapsedSeconds);

        engine.Toggle();
        clock.Advance(2_000);
        engine.Sync();
        Assert.Equal(paused + 2, engine.State.ElapsedSeconds);
    }

    [Fact]
    public void Reset_returns_to_the_current_segment_and_drops_indefinite_time()
    {
        var clock = new ManualClock();
        using var engine = Create(clock);
        engine.StartIndefinite();
        clock.Advance(20_000);
        engine.Reset();

        var state = engine.State;
        Assert.False(state.Indefinite);
        Assert.False(state.Running);
        Assert.Equal(FocusMode.Focus, state.Mode);
        Assert.Equal(25 * 60, state.RemainingSeconds);
        Assert.Equal(0, state.ElapsedSeconds);
    }

    [Fact]
    public void Changing_unrelated_settings_does_not_wipe_a_paused_timer()
    {
        var clock = new ManualClock();
        using var engine = Create(clock);
        engine.Toggle();
        clock.Advance(10_000);
        engine.Pause();
        var remaining = engine.State.RemainingMillis;

        engine.Configure(_settings with { Amoled = true, FocusMinutes = 50 });

        Assert.Equal(remaining, engine.State.RemainingMillis);
        Assert.Equal(FocusPhase.Paused, engine.State.Phase);
        Assert.True(engine.State.Settings.Amoled);
    }

    [Fact]
    public void Changing_the_duration_updates_an_idle_timer()
    {
        using var engine = Create(new ManualClock());
        engine.Configure(_settings with { FocusMinutes = 45 });

        Assert.Equal(45 * 60, engine.State.RemainingSeconds);
        Assert.Equal(45 * 60, engine.State.TotalSeconds);
    }

    [Fact]
    public void Running_timer_keeps_its_original_length_when_settings_change()
    {
        var clock = new ManualClock();
        using var engine = Create(clock);
        engine.Toggle();
        clock.Advance(10_000);
        engine.Sync();

        engine.Configure(_settings with { FocusMinutes = 50 });

        Assert.Equal(1_490_000L, engine.State.RemainingMillis);
        Assert.Equal(25 * 60, engine.State.TotalSeconds);
    }

    [Fact]
    public void Process_death_restores_the_deadline()
    {
        var clock = new ManualClock();
        var persistence = new MemoryPersistence();
        using (var first = Create(clock, persistence: persistence))
        {
            first.Toggle();
        }

        clock.Advance(10_000);
        using var restored = Create(clock, persistence: persistence);
        Assert.True(restored.State.Running);
        Assert.Equal(1_490_000L, restored.State.RemainingMillis);
        Assert.Equal(25 * 60, restored.State.TotalSeconds);
    }

    [Fact]
    public void Process_death_restores_indefinite_elapsed_time_from_the_anchor()
    {
        var clock = new ManualClock();
        var persistence = new MemoryPersistence();
        using (var first = Create(clock, persistence: persistence))
        {
            first.StartIndefinite();
        }

        clock.Advance(12_000);
        using var restored = Create(clock, persistence: persistence);
        Assert.True(restored.State.Indefinite);
        Assert.Equal(12, restored.State.ElapsedSeconds);
    }

    [Fact]
    public void Reboot_shifts_the_deadline_onto_the_new_elapsed_clock()
    {
        var persistence = new MemoryPersistence();
        persistence.Save(new FocusTimerSnapshot(
            FocusPhase.Running,
            FocusMode.Focus,
            false,
            30_000L,
            30_000L,
            80_000L,
            0L,
            0L,
            2,
            25,
            25,
            5,
            15,
            4,
            false,
            50_000L,
            10_000_000L));
        var clock = new ManualClock(elapsed: 1_000L, wall: 10_010_000L);
        using var restored = Create(clock, persistence: persistence);

        Assert.Equal(20_000L, restored.State.RemainingMillis);
        Assert.Equal(2, restored.State.CompletedInCycle);
        Assert.True(restored.State.Running);
    }

    [Fact]
    public void Snapshot_codec_round_trips()
    {
        var snapshot = new FocusTimerSnapshot(
            FocusPhase.Paused,
            FocusMode.LongBreak,
            false,
            15 * 60_000L,
            8 * 60_000L,
            0L,
            0L,
            0L,
            4,
            15,
            25,
            5,
            15,
            4,
            true,
            99L,
            100L);

        Assert.Equal(snapshot, FocusTimerSnapshot.Decode(snapshot.Encode()));
        Assert.Null(FocusTimerSnapshot.Decode("nope"));
    }

    [Fact]
    public async Task Reset_cancels_a_pending_indefinite_start_and_the_ticker()
    {
        var clock = new ManualClock();
        var recorded = new List<FocusSession>();
        using var engine = Create(clock, recorder: recorded, enableTicker: true);
        engine.ScheduleIndefinite(10_000L);
        engine.Toggle();
        await Task.Delay(150);
        engine.Reset();
        clock.ArmedJump = 120_000L;
        await Task.Delay(1_200);

        Assert.False(engine.State.Running);
        Assert.False(engine.State.Indefinite);
        Assert.Empty(recorded);
    }

    private FocusTimerEngine Create(
        ManualClock clock,
        AppSettings? settings = null,
        List<FocusSession>? recorder = null,
        IFocusTimerPersistence? persistence = null,
        bool enableTicker = false) =>
        new(
            clock,
            persistence ?? new MemoryPersistence(),
            settings ?? _settings,
            recorder == null ? null : recorder.Add,
            enableTicker);
}

public sealed class ManualClock : IFocusClock
{
    private long _elapsed;
    private long _wall;

    public ManualClock(long elapsed = 0, long wall = 1_700_000_000_000L)
    {
        _elapsed = elapsed;
        _wall = wall;
    }

    public long Wall => _wall;

    public long ArmedJump { get; set; }

    public long ElapsedRealtime()
    {
        var jump = ArmedJump;
        if (jump != 0)
        {
            ArmedJump = 0;
            _elapsed += jump;
            _wall += jump;
        }

        return _elapsed;
    }

    public long WallClockMillis() => _wall;

    public void Advance(long millis)
    {
        _elapsed += millis;
        _wall += millis;
    }
}

public sealed class MemoryPersistence : IFocusTimerPersistence
{
    private FocusTimerSnapshot? _snapshot;

    public FocusTimerSnapshot? Load() => _snapshot;

    public void Save(FocusTimerSnapshot snapshot) => _snapshot = snapshot;

    public void Clear() => _snapshot = null;
}
