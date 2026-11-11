namespace TokiNote;

/// <summary>
/// Focus timer. The countdown is anchored to a monotonic deadline and indefinite
/// mode is anchored to a start time. Every refresh recomputes from the clock,
/// so sleep and background time do not drift. A completion more than
/// <see cref="LateGraceMs"/> late settles only the current segment.
/// </summary>
public sealed class FocusTimerEngine : IDisposable
{
    public const long LateGraceMs = 3_000;
    public const long RebootSkewMs = 1_000;
    public const long IndefiniteAnimationMs = 430;

    private readonly IFocusClock _clock;
    private readonly IFocusTimerPersistence _persistence;
    private readonly bool _enableTicker;
    private readonly object _gate = new();
    private readonly Action<FocusSession>? _sessionRecorder;

    private AppSettings _settings;
    private FocusPhase _phase = FocusPhase.Idle;
    private FocusMode _mode = FocusMode.Focus;
    private bool _indefinite;
    private long _totalMillis;
    private long _remainingMillis;
    private long _deadlineElapsed;
    private long _elapsedBaseMillis;
    private long _runStartedElapsed;
    private int _completedInCycle;
    private int _segmentMinutes;
    private int _scheduleGeneration;
    private int _tickerGeneration;
    private CancellationTokenSource? _tickerCts;
    private Task? _ticker;
    private CancellationTokenSource? _indefiniteCts;
    private volatile FocusState _state;

    public FocusTimerEngine(
        IFocusClock clock,
        IFocusTimerPersistence persistence,
        AppSettings? initialSettings = null,
        Action<FocusSession>? sessionRecorder = null,
        bool enableTicker = false)
    {
        _clock = clock;
        _persistence = persistence;
        _settings = initialSettings ?? new AppSettings();
        _sessionRecorder = sessionRecorder;
        _enableTicker = enableTicker;
        var saved = persistence.Load();
        if (saved != null)
        {
            ApplySnapshot(saved);
        }
        else
        {
            InstallIdleSegment(FocusMode.Focus);
        }

        _state = Derive(_clock.ElapsedRealtime());
    }

    public FocusState State => _state;

    public event Action<FocusTimerEvent>? Completed;

    public void Configure(AppSettings newSettings) => Mutate(_ =>
    {
        if (newSettings == _settings)
        {
            return;
        }

        var idle = _phase == FocusPhase.Idle && !_indefinite;
        _settings = newSettings;
        if (idle)
        {
            InstallIdleSegment(_mode);
        }

        CommitLocked();
    });

    public void Toggle() => Mutate(events =>
    {
        InvalidateScheduleLocked();
        if (_phase == FocusPhase.Running)
        {
            PauseLocked(events);
        }
        else
        {
            ResumeOrStartLocked();
        }
    });

    public void Pause() => Mutate(PauseLocked);

    public void Reset() => Mutate(_ =>
    {
        InvalidateScheduleLocked();
        StopTickerLocked();
        _indefinite = false;
        _elapsedBaseMillis = 0;
        _runStartedElapsed = 0;
        _deadlineElapsed = 0;
        _phase = FocusPhase.Idle;
        InstallIdleSegment(_mode);
        CommitLocked();
    });

    public void Skip() => Mutate(_ =>
    {
        InvalidateScheduleLocked();
        StopTickerLocked();
        _mode = FocusMath.NextFocusMode(_mode, _completedInCycle, _settings.SessionsBeforeLongBreak);
        _indefinite = false;
        _elapsedBaseMillis = 0;
        _runStartedElapsed = 0;
        _deadlineElapsed = 0;
        _phase = FocusPhase.Idle;
        InstallIdleSegment(_mode);
        CommitLocked();
    });

    public void StartIndefinite() => Mutate(_ => StartIndefiniteLocked());

    public void ScheduleIndefinite(long delayMs = IndefiniteAnimationMs)
    {
        lock (_gate)
        {
            _scheduleGeneration++;
            var generation = _scheduleGeneration;
            CancelIndefiniteJobLocked();
            if (delayMs <= 0 || !_enableTicker)
            {
                StartIndefiniteLocked();
                return;
            }

            var cts = new CancellationTokenSource();
            _indefiniteCts = cts;
            var token = cts.Token;
            _ = Task.Run(async () =>
            {
                try
                {
                    await Task.Delay(TimeSpan.FromMilliseconds(delayMs), token).ConfigureAwait(false);
                }
                catch (OperationCanceledException)
                {
                    return;
                }

                Mutate(_ =>
                {
                    if (generation != _scheduleGeneration)
                    {
                        return;
                    }

                    StartIndefiniteLocked();
                });
            }, token);
        }
    }

    /// <summary>Recompute from the clock. Call after resume and on every foreground.</summary>
    public void Sync()
    {
        var events = new List<FocusTimerEvent>();
        bool running;
        lock (_gate)
        {
            CatchUpLocked(_clock.ElapsedRealtime(), events);
            running = _phase == FocusPhase.Running;
        }

        Dispatch(events);
        if (running)
        {
            lock (_gate)
            {
                EnsureTickerLocked();
            }
        }
    }

    public void Dispose()
    {
        lock (_gate)
        {
            StopTickerLocked();
            CancelIndefiniteJobLocked();
        }
    }

    private void ApplySnapshot(FocusTimerSnapshot saved)
    {
        _settings = _settings with
        {
            FocusMinutes = Math.Max(1, saved.FocusMinutes),
            ShortBreakMinutes = Math.Max(1, saved.ShortBreakMinutes),
            LongBreakMinutes = Math.Max(1, saved.LongBreakMinutes),
            SessionsBeforeLongBreak = Math.Max(1, saved.SessionsBeforeLongBreak),
            AutoStartNext = saved.AutoStartNext,
        };
        _phase = saved.Phase;
        _mode = saved.Mode;
        _indefinite = saved.Indefinite;
        _totalMillis = Math.Max(0, saved.TotalMillis);
        _remainingMillis = Math.Max(0, saved.RemainingMillis);
        _elapsedBaseMillis = Math.Max(0, saved.ElapsedBaseMillis);
        _completedInCycle = Math.Max(0, saved.CompletedInCycle);
        _segmentMinutes = Math.Max(0, saved.SegmentMinutes);
        _deadlineElapsed = saved.DeadlineElapsed;
        _runStartedElapsed = saved.RunStartedElapsed;

        var nowElapsed = _clock.ElapsedRealtime();
        var nowWall = _clock.WallClockMillis();
        var rebooted = nowElapsed + RebootSkewMs < saved.SavedAtElapsed;
        if (!rebooted)
        {
            return;
        }

        if (_deadlineElapsed != 0)
        {
            var deadlineWall = saved.SavedAtWall + (saved.DeadlineElapsed - saved.SavedAtElapsed);
            _deadlineElapsed = nowElapsed + (deadlineWall - nowWall);
        }

        if (_runStartedElapsed != 0)
        {
            var startWall = saved.SavedAtWall + (saved.RunStartedElapsed - saved.SavedAtElapsed);
            _runStartedElapsed = nowElapsed + (startWall - nowWall);
        }
    }

    private void InstallIdleSegment(FocusMode target)
    {
        _mode = target;
        var minutes = Math.Max(1, FocusMath.MinutesOf(target, _settings));
        var total = minutes * 60_000L;
        _totalMillis = total;
        _remainingMillis = total;
        _segmentMinutes = minutes;
    }

    private void ResumeOrStartLocked()
    {
        var now = _clock.ElapsedRealtime();
        if (_indefinite)
        {
            _phase = FocusPhase.Running;
            _runStartedElapsed = now;
            CommitLocked();
            EnsureTickerLocked();
            return;
        }

        if (_phase == FocusPhase.Paused && _remainingMillis > 0)
        {
            _deadlineElapsed = now + _remainingMillis;
            _phase = FocusPhase.Running;
        }
        else
        {
            var minutes = Math.Max(1, FocusMath.MinutesOf(_mode, _settings));
            var total = minutes * 60_000L;
            _totalMillis = total;
            _remainingMillis = total;
            _segmentMinutes = minutes;
            _deadlineElapsed = now + total;
            _phase = FocusPhase.Running;
            _indefinite = false;
            _elapsedBaseMillis = 0;
            _runStartedElapsed = 0;
        }

        CommitLocked();
        EnsureTickerLocked();
    }

    private void PauseLocked(List<FocusTimerEvent> events)
    {
        if (_phase != FocusPhase.Running)
        {
            return;
        }

        var now = _clock.ElapsedRealtime();
        if (!_indefinite && _deadlineElapsed > 0 && now >= _deadlineElapsed)
        {
            CompleteLocked(now, events);
            return;
        }

        if (_indefinite)
        {
            _elapsedBaseMillis += Math.Max(0, now - _runStartedElapsed);
            _runStartedElapsed = 0;
        }
        else
        {
            _remainingMillis = Math.Max(0, _deadlineElapsed - now);
            _deadlineElapsed = 0;
        }

        _phase = FocusPhase.Paused;
        StopTickerLocked();
        CommitLocked();
    }

    private void StartIndefiniteLocked()
    {
        _scheduleGeneration++;
        CancelIndefiniteJobLocked();
        var now = _clock.ElapsedRealtime();
        _indefinite = true;
        _phase = FocusPhase.Running;
        _elapsedBaseMillis = 0;
        _runStartedElapsed = now;
        _deadlineElapsed = 0;
        _remainingMillis = 0;
        _totalMillis = 0;
        CommitLocked();
        EnsureTickerLocked();
    }

    private long CatchUpLocked(long now, List<FocusTimerEvent> events)
    {
        if (_phase != FocusPhase.Running)
        {
            return 0;
        }

        if (_indefinite)
        {
            PublishLocked();
            return IndefiniteDelay(now);
        }

        if (_deadlineElapsed <= 0)
        {
            return 0;
        }

        if (now >= _deadlineElapsed)
        {
            CompleteLocked(now, events);
            return _phase == FocusPhase.Running ? CountdownDelay(_clock.ElapsedRealtime()) : 0;
        }

        PublishLocked();
        return CountdownDelay(now);
    }

    private void CompleteLocked(long now, List<FocusTimerEvent> events)
    {
        var overtime = Math.Max(0, now - _deadlineElapsed);
        var endedAt = _clock.WallClockMillis() - overtime;
        var live = overtime <= LateGraceMs;
        var finishedMode = _mode;
        var minutes = _segmentMinutes;
        if (finishedMode == FocusMode.Focus)
        {
            _mode = FocusMath.NextFocusMode(FocusMode.Focus, _completedInCycle, _settings.SessionsBeforeLongBreak);
            _completedInCycle++;
            if (minutes > 0)
            {
                events.Add(new FocusTimerEvent.FocusCompleted(minutes, endedAt));
            }
        }
        else
        {
            _mode = FocusMode.Focus;
            if (minutes > 0)
            {
                events.Add(new FocusTimerEvent.BreakCompleted(minutes, endedAt));
            }
        }

        _indefinite = false;
        _elapsedBaseMillis = 0;
        _runStartedElapsed = 0;
        var nextMinutes = Math.Max(1, FocusMath.MinutesOf(_mode, _settings));
        var total = nextMinutes * 60_000L;
        _totalMillis = total;
        _remainingMillis = total;
        _segmentMinutes = nextMinutes;
        if (live && _settings.AutoStartNext)
        {
            _phase = FocusPhase.Running;
            _deadlineElapsed = now + total;
            EnsureTickerLocked();
        }
        else
        {
            _phase = FocusPhase.Idle;
            _deadlineElapsed = 0;
            StopTickerLocked();
        }

        CommitLocked();
    }

    private long CountdownDelay(long now)
    {
        var remaining = Math.Max(0, _deadlineElapsed - now);
        if (remaining == 0)
        {
            return 16;
        }

        var mod = remaining % 1000L;
        var until = mod == 0 ? 1000L : mod;
        return Math.Clamp(until, 16L, 1000L);
    }

    private long IndefiniteDelay(long now)
    {
        var elapsed = _elapsedBaseMillis + Math.Max(0, now - _runStartedElapsed);
        var mod = elapsed % 1000L;
        var until = mod == 0 ? 1000L : 1000L - mod;
        return Math.Clamp(until, 16L, 1000L);
    }

    private void EnsureTickerLocked()
    {
        if (!_enableTicker)
        {
            return;
        }

        if (_ticker is { IsCompleted: false })
        {
            return;
        }

        var generation = ++_tickerGeneration;
        _tickerCts?.Dispose();
        var cts = new CancellationTokenSource();
        _tickerCts = cts;
        var token = cts.Token;
        _ticker = Task.Run(async () =>
        {
            try
            {
                await TickerLoop(token, generation).ConfigureAwait(false);
            }
            finally
            {
                lock (_gate)
                {
                    if (_tickerGeneration == generation)
                    {
                        _ticker = null;
                        _tickerCts = null;
                    }
                }
            }
        }, token);
    }

    private async Task TickerLoop(CancellationToken token, int generation)
    {
        while (!token.IsCancellationRequested)
        {
            var events = new List<FocusTimerEvent>();
            long wait;
            var stop = false;
            lock (_gate)
            {
                if (token.IsCancellationRequested || generation != _tickerGeneration || _phase != FocusPhase.Running)
                {
                    stop = true;
                    wait = 0;
                }
                else
                {
                    wait = CatchUpLocked(_clock.ElapsedRealtime(), events);
                    stop = wait <= 0;
                }
            }

            Dispatch(events);
            if (stop)
            {
                break;
            }

            try
            {
                await Task.Delay(TimeSpan.FromMilliseconds(wait), token).ConfigureAwait(false);
            }
            catch (OperationCanceledException)
            {
                break;
            }
        }
    }

    private void StopTickerLocked()
    {
        _tickerGeneration++;
        _ticker = null;
        var cts = _tickerCts;
        _tickerCts = null;
        cts?.Cancel();
    }

    private void InvalidateScheduleLocked()
    {
        _scheduleGeneration++;
        CancelIndefiniteJobLocked();
    }

    private void CancelIndefiniteJobLocked()
    {
        var cts = _indefiniteCts;
        _indefiniteCts = null;
        cts?.Cancel();
    }

    private void CommitLocked()
    {
        _persistence.Save(Snapshot(_clock.ElapsedRealtime(), _clock.WallClockMillis()));
        PublishLocked();
    }

    private void PublishLocked() => _state = Derive(_clock.ElapsedRealtime());

    private FocusTimerSnapshot Snapshot(long nowElapsed, long nowWall)
    {
        var liveRemaining = _phase == FocusPhase.Running && !_indefinite && _deadlineElapsed > 0
            ? Math.Max(0, _deadlineElapsed - nowElapsed)
            : _remainingMillis;
        return new FocusTimerSnapshot(
            _phase,
            _mode,
            _indefinite,
            _totalMillis,
            liveRemaining,
            _phase == FocusPhase.Running && !_indefinite ? _deadlineElapsed : 0,
            _elapsedBaseMillis,
            _phase == FocusPhase.Running && _indefinite ? _runStartedElapsed : 0,
            _completedInCycle,
            _segmentMinutes,
            _settings.FocusMinutes,
            _settings.ShortBreakMinutes,
            _settings.LongBreakMinutes,
            _settings.SessionsBeforeLongBreak,
            _settings.AutoStartNext,
            nowElapsed,
            nowWall);
    }

    private FocusState Derive(long now)
    {
        var liveRemaining = _indefinite
            ? 0
            : _phase == FocusPhase.Running && _deadlineElapsed > 0
                ? Math.Max(0, _deadlineElapsed - now)
                : _remainingMillis;
        var liveElapsed = !_indefinite
            ? 0
            : _phase == FocusPhase.Running
                ? _elapsedBaseMillis + Math.Max(0, now - _runStartedElapsed)
                : _elapsedBaseMillis;
        return new FocusState(
            _mode,
            _phase == FocusPhase.Running,
            FocusMath.CeilSeconds(liveRemaining),
            (int)Math.Max(0, _totalMillis / 1000L),
            _completedInCycle,
            _settings,
            _indefinite,
            (int)Math.Max(0, liveElapsed / 1000L),
            _phase,
            liveRemaining,
            _totalMillis,
            _phase == FocusPhase.Running && !_indefinite ? _deadlineElapsed : 0,
            _indefinite ? _elapsedBaseMillis : 0,
            _phase == FocusPhase.Running && _indefinite ? _runStartedElapsed : 0);
    }

    private void Mutate(Action<List<FocusTimerEvent>> body)
    {
        var events = new List<FocusTimerEvent>();
        lock (_gate)
        {
            body(events);
        }

        Dispatch(events);
    }

    private void Dispatch(List<FocusTimerEvent> events)
    {
        if (events.Count == 0)
        {
            return;
        }

        foreach (var timerEvent in events)
        {
            _sessionRecorder?.Invoke(timerEvent.ToSession());
            Completed?.Invoke(timerEvent);
        }
    }
}
