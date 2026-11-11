namespace TokiNote;

public sealed class AppHost : IDisposable
{
    private readonly FileFocusTimerPersistence _timerStore;

    public AppHost()
    {
        Directory.CreateDirectory(AppPaths.Root);
        Database = new TokiDatabase(AppPaths.Database);
        Database.PurgeExpiredTrash(DateTimeOffset.Now.ToUnixTimeMilliseconds());
        Settings = Database.LoadSettings();
        _timerStore = new FileFocusTimerPersistence(AppPaths.TimerSnapshot);
        Engine = new FocusTimerEngine(
            new SystemFocusClock(),
            _timerStore,
            Settings,
            Database.AddSession,
            enableTicker: true);
    }

    public TokiDatabase Database { get; }
    public FocusTimerEngine Engine { get; }
    public AppSettings Settings { get; private set; }
    public event Action<FocusTimerEvent>? SegmentCompleted;

    public void Start()
    {
        Engine.Completed += OnCompleted;
        Engine.Sync();
        Settings = Engine.State.Settings;
        Database.SaveSettings(Settings);
    }

    public void UpdateSettings(AppSettings settings)
    {
        Settings = settings;
        Database.SaveSettings(settings);
        Engine.Configure(settings);
        Settings = Engine.State.Settings;
    }

    public void Dispose()
    {
        Engine.Completed -= OnCompleted;
        Engine.Dispose();
        Database.Dispose();
    }

    private void OnCompleted(FocusTimerEvent timerEvent) => SegmentCompleted?.Invoke(timerEvent);
}
