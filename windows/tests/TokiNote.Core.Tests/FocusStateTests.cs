namespace TokiNote.Tests;

public class FocusStateTests
{
    private readonly AppSettings _settings = new() { SessionsBeforeLongBreak = 4 };

    [Fact]
    public void Early_focus_sessions_lead_to_a_short_break()
    {
        var state = new FocusState(mode: FocusMode.Focus, completedInCycle: 0, settings: _settings);
        Assert.Equal(FocusMode.ShortBreak, state.NextMode);
    }

    [Fact]
    public void Every_fourth_focus_session_leads_to_a_long_break()
    {
        var state = new FocusState(mode: FocusMode.Focus, completedInCycle: 3, settings: _settings);
        Assert.Equal(FocusMode.LongBreak, state.NextMode);
    }

    [Fact]
    public void Breaks_always_lead_back_to_focus()
    {
        var shortBreak = new FocusState(mode: FocusMode.ShortBreak, settings: _settings);
        var longBreak = new FocusState(mode: FocusMode.LongBreak, settings: _settings);
        Assert.Equal(FocusMode.Focus, shortBreak.NextMode);
        Assert.Equal(FocusMode.Focus, longBreak.NextMode);
    }

    [Fact]
    public void Progress_reflects_elapsed_time()
    {
        var half = new FocusState(remainingSeconds: 750, totalSeconds: 1500);
        var fresh = new FocusState(remainingSeconds: 1500, totalSeconds: 1500);
        Assert.Equal(0.5f, half.Progress, 3);
        Assert.Equal(0f, fresh.Progress, 3);
    }

    [Fact]
    public void Session_index_wraps_inside_the_cycle()
    {
        Assert.Equal(1, new FocusState(completedInCycle: 0, settings: _settings).SessionIndex);
        Assert.Equal(4, new FocusState(completedInCycle: 3, settings: _settings).SessionIndex);
        Assert.Equal(1, new FocusState(completedInCycle: 4, settings: _settings).SessionIndex);
    }

    [Fact]
    public void Session_dots_reset_when_the_next_focus_cycle_starts()
    {
        Assert.Equal(4, FocusMath.FilledSessionCount(4, 4, FocusMode.LongBreak));
        Assert.Equal(0, FocusMath.FilledSessionCount(4, 4, FocusMode.Focus));
        Assert.Equal(1, FocusMath.FilledSessionCount(1, 4, FocusMode.ShortBreak));
        Assert.Equal(0, FocusMath.FilledSessionCount(0, 4, FocusMode.Focus));
    }

    [Fact]
    public void Zero_length_cycle_does_not_divide_by_zero()
    {
        var state = new FocusState(settings: new AppSettings { SessionsBeforeLongBreak = 0 });
        Assert.Equal(FocusMode.LongBreak, state.NextMode);
        Assert.Equal(1, state.SessionIndex);
    }
}
