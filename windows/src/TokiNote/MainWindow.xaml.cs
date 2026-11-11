using Microsoft.UI.Composition.SystemBackdrops;
using Windows.UI;
using Microsoft.UI.Windowing;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;
using Microsoft.UI.Xaml.Media.Animation;

namespace TokiNote;

public sealed partial class MainWindow : Window
{
    private readonly AppHost _host;
    private bool _navReady;
    private bool _reallyExit;
    private TrayIcon? _tray;
    private string _currentTag = "";

    public MainWindow(AppHost host)
    {
        _host = host;
        InitializeComponent();
        var appFont = new FontFamily(AppFonts.FamilySource);
        Nav.FontFamily = appFont;
        AppTitle.FontFamily = appFont;
        ExtendsContentIntoTitleBar = true;
        SetTitleBar(TitleBar);
        AppWindow.Title = "TokiNote";
        var titleBar = AppWindow.TitleBar;
        titleBar.ButtonBackgroundColor = Color.FromArgb(0, 0, 0, 0);
        titleBar.ButtonInactiveBackgroundColor = Color.FromArgb(0, 0, 0, 0);
        AppWindow.Closing += OnClosing;
        Activated += (_, _) => _host.Engine.Sync();
        _navReady = true;
        Nav.SelectedItem = Nav.MenuItems[0];
        if (ContentFrame.Content == null)
        {
            _currentTag = "notes";
            ContentFrame.Navigate(typeof(NotesPage), _host);
        }
    }

    public bool IsPresented { get; private set; }

    public nint WindowHandle => WinRT.Interop.WindowNative.GetWindowHandle(this);

    public void Present()
    {
        ApplyTheme();
        Activate();
        IsPresented = true;
        _tray ??= new TrayIcon(this, _host);
    }

    public void ShowInfo(string message)
    {
        StatusInfo.Message = message;
        StatusInfo.IsOpen = true;
    }

    public void ApplyTheme()
    {
        var theme = ThemeHelper.Resolve(_host.Settings.ThemeMode);
        Root.RequestedTheme = theme;
        var dark = Root.ActualTheme == ElementTheme.Dark || theme == ElementTheme.Dark;
        if (_host.Settings.Amoled && dark)
        {
            SystemBackdrop = null;
            Root.Background = new SolidColorBrush(Color.FromArgb(255, 0, 0, 0));
            return;
        }

        Root.Background = new SolidColorBrush(Color.FromArgb(0, 0, 0, 0));
        try
        {
            if (MicaController.IsSupported())
            {
                SystemBackdrop = new MicaBackdrop { Kind = MicaKind.Base };
            }
            else if (DesktopAcrylicController.IsSupported())
            {
                SystemBackdrop = new DesktopAcrylicBackdrop();
            }
            else
            {
                SystemBackdrop = null;
                Root.Background = (Brush)Application.Current.Resources["ApplicationPageBackgroundThemeBrush"];
            }
        }
        catch
        {
            SystemBackdrop = null;
            Root.Background = (Brush)Application.Current.Resources["ApplicationPageBackgroundThemeBrush"];
        }
    }

    public void ExitApplication()
    {
        _reallyExit = true;
        _tray?.Dispose();
        Close();
    }

    private void OnClosing(AppWindow sender, AppWindowClosingEventArgs args)
    {
        if (_reallyExit || _tray is not { IsVisible: true })
        {
            _host.Dispose();
            return;
        }

        args.Cancel = true;
        NativeMethods.ShowWindow(WindowHandle, NativeMethods.SwHide);
    }

    private void Nav_SelectionChanged(NavigationView sender, NavigationViewSelectionChangedEventArgs args)
    {
        if (!_navReady || args.SelectedItem is not NavigationViewItem item)
        {
            return;
        }

        var tag = item.Tag as string ?? "";
        if (tag == _currentTag)
        {
            return;
        }

        _currentTag = tag;
        Type page = tag switch
        {
            "focus" => typeof(FocusPage),
            "stats" => typeof(StatsPage),
            "archive" => typeof(ArchivePage),
            "trash" => typeof(TrashPage),
            "settings" => typeof(SettingsPage),
            _ => typeof(NotesPage),
        };
        NavigationTransitionInfo transition = _host.Settings.ReduceMotion || !ThemeHelper.SystemAnimationsEnabled()
            ? new SuppressNavigationTransitionInfo()
            : new EntranceNavigationTransitionInfo();
        ContentFrame.Navigate(page, _host, transition);
    }
}
