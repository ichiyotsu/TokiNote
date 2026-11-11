using Microsoft.UI.Dispatching;
using Microsoft.UI.Xaml;

namespace TokiNote;

public partial class App : Application
{
    private MainWindow? _main;
    private string? _pendingInfo;
    private DispatcherQueue? _queue;

    public App()
    {
        InitializeComponent();
        ThemeFont.Apply(Resources);
    }

    public static new App Current => (App)Application.Current;

    public AppHost Host { get; private set; } = null!;

    public MainWindow? Shell => _main;

    protected override void OnLaunched(LaunchActivatedEventArgs args)
    {
        _queue = DispatcherQueue.GetForCurrentThread();
        Host = new AppHost();
        Host.SegmentCompleted += OnSegmentCompleted;
        var theme = ThemeHelper.Resolve(Host.Settings.ThemeMode);
        var animations = !Host.Settings.ReduceMotion && ThemeHelper.SystemAnimationsEnabled();
        SplashWindow? splash = null;
        var shownAt = Environment.TickCount64;
        try
        {
            splash = new SplashWindow(theme, animations);
            shownAt = Environment.TickCount64;
            splash.ShowSplash();
        }
        catch
        {
            splash = null;
        }

        Host.Start();
        _main = new MainWindow(Host);
        if (splash == null)
        {
            _main.Present();
            _ = UpdateCoordinator.CheckAsync(_main, interactive: false);
            return;
        }

        var wait = SplashTiming.DelayBeforeExit(Environment.TickCount64 - shownAt, mainReady: true);
        splash.ScheduleExit(wait, () =>
        {
            splash.Close();
            _main.Present();
            if (!string.IsNullOrEmpty(_pendingInfo))
            {
                _main.ShowInfo(_pendingInfo);
                _pendingInfo = null;
            }

            _ = UpdateCoordinator.CheckAsync(_main, interactive: false);
        });
    }

    private void OnSegmentCompleted(FocusTimerEvent timerEvent)
    {
        var focus = timerEvent is FocusTimerEvent.FocusCompleted;
        var title = Strings.Get(focus ? "focus_notif_done_focus" : "focus_notif_done_break");
        var body = Strings.Format(
            focus ? "focus_notif_done_focus_body" : "focus_notif_done_break_body",
            TimeFormat.FormatMinutes(timerEvent.Minutes));
        var line = $"{title} · {body}";
        _queue?.TryEnqueue(() =>
        {
            try
            {
                ToastService.Show(title, body);
            }
            catch
            {
                // Notifications are best-effort for unpackaged runs.
            }

            if (_main != null && _main.IsPresented)
            {
                _main.ShowInfo(line);
            }
            else
            {
                _pendingInfo = line;
            }
        });
    }
}
