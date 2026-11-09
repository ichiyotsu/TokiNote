using Microsoft.UI.Dispatching;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Input;
using Microsoft.UI.Xaml.Media;
using Microsoft.UI.Xaml.Shapes;

namespace TokiNote;

public sealed partial class FocusPage : Page
{
    private AppHost _host = null!;
    private DispatcherQueueTimer? _timer;
    private DispatcherQueueTimer? _holdTimer;
    private long _lastClick;
    private readonly IFocusClock _clock = new SystemFocusClock();

    public FocusPage()
    {
        InitializeComponent();
        ResetButton.Content = Strings.Get("focus_reset");
        SkipButton.Content = Strings.Get("focus_skip");
        IndefiniteButton.Content = Strings.Get("focus_indefinite");
        UpNextLabel.Text = Strings.Get("focus_up_next");
    }

    protected override void OnNavigatedTo(Microsoft.UI.Xaml.Navigation.NavigationEventArgs e)
    {
        _host = (AppHost)e.Parameter;
        _host.Engine.Sync();
        _timer = DispatcherQueue.GetForCurrentThread().CreateTimer();
        _timer.Interval = TimeSpan.FromMilliseconds(50);
        _timer.IsRepeating = true;
        _timer.Tick += (_, _) => Render();
        _timer.Start();
        Render();
    }

    protected override void OnNavigatedFrom(Microsoft.UI.Xaml.Navigation.NavigationEventArgs e) => _timer?.Stop();

    private void Render()
    {
        var state = _host.Engine.State;
        var now = _clock.ElapsedRealtime();
        var seconds = FocusDisplay.ShowSeconds(state, now);
        ClockText.Text = TimeFormat.FormatElapsed(seconds);
        ModeText.Text = state.Indefinite ? Strings.Get("focus_indefinite") : ModeLabel(state.Mode);
        SessionText.Text = state.Indefinite
            ? Strings.Get("focus_indefinite_hint")
            : Strings.Format("focus_session", state.SessionIndex, Math.Max(1, state.Settings.SessionsBeforeLongBreak));
        ToggleButton.Content = state.Running ? Strings.Get("focus_pause") : Strings.Get("focus_play");
        var nextMinutes = Math.Max(1, FocusMath.MinutesOf(state.NextMode, state.Settings));
        NextText.Text = Strings.Format("focus_next_hint", TimeFormat.FormatElapsed(nextMinutes * 60), ModeLabel(state.NextMode));
        var progress = state.Indefinite || state.TotalMillis <= 0
            ? 0
            : Math.Clamp(1 - FocusDisplay.LiveRemainingMillis(state, now) / (double)state.TotalMillis, 0, 1);
        var diameter = Ring.Width - Ring.StrokeThickness;
        var dash = Math.PI * diameter / Ring.StrokeThickness;
        Ring.StrokeDashArray = new DoubleCollection { dash * progress, dash + 0.01 };
        var total = Math.Max(1, state.Settings.SessionsBeforeLongBreak);
        var filled = FocusMath.FilledSessionCount(state.CompletedInCycle, total, state.Mode);
        if (Dots.Children.Count != total)
        {
            Dots.Children.Clear();
            for (var index = 0; index < total; index++)
            {
                Dots.Children.Add(new Ellipse { Width = 8, Height = 8 });
            }
        }

        for (var index = 0; index < Dots.Children.Count; index++)
        {
            var dot = (Ellipse)Dots.Children[index];
            dot.Fill = new SolidColorBrush(index < filled
                ? Windows.UI.Color.FromArgb(255, 0x4C, 0x66, 0x2B)
                : Windows.UI.Color.FromArgb(90, 128, 128, 128));
        }
    }

    private void ToggleButton_Click(object sender, RoutedEventArgs e) => _host.Engine.Toggle();

    private void ResetButton_Click(object sender, RoutedEventArgs e) => _host.Engine.Reset();

    private void SkipButton_Click(object sender, RoutedEventArgs e) => _host.Engine.Skip();

    private void IndefiniteButton_Click(object sender, RoutedEventArgs e) => _host.Engine.ScheduleIndefinite();

    private void ClockHost_PointerPressed(object sender, PointerRoutedEventArgs e)
    {
        var state = _host.Engine.State;
        if (state.Running || state.Indefinite || state.Mode != FocusMode.Focus)
        {
            return;
        }

        _holdTimer = DispatcherQueue.GetForCurrentThread().CreateTimer();
        _holdTimer.Interval = TimeSpan.FromMilliseconds(500);
        _holdTimer.Tick += (_, _) =>
        {
            _holdTimer.Stop();
            _host.Engine.ScheduleIndefinite();
        };
        _holdTimer.Start();
    }

    private void ClockHost_PointerReleased(object sender, PointerRoutedEventArgs e)
    {
        _holdTimer?.Stop();
        var state = _host.Engine.State;
        if (state.Running || state.Indefinite || state.Mode != FocusMode.Focus)
        {
            return;
        }

        var now = Environment.TickCount64;
        if (now - _lastClick < 400)
        {
            _host.Engine.ScheduleIndefinite();
            _lastClick = 0;
            return;
        }

        _lastClick = now;
    }

    private static string ModeLabel(FocusMode mode) => mode switch
    {
        FocusMode.ShortBreak => Strings.Get("focus_short_break"),
        FocusMode.LongBreak => Strings.Get("focus_long_break"),
        _ => Strings.Get("focus_title"),
    };
}
