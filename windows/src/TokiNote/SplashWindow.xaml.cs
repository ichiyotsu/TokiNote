using Microsoft.UI.Dispatching;
using Microsoft.UI.Windowing;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Hosting;
using Microsoft.UI.Xaml.Input;
using Microsoft.UI.Xaml.Media;
using Microsoft.UI.Xaml.Media.Animation;
using Windows.Graphics;
using Windows.UI;
using Windows.UI.ViewManagement;

namespace TokiNote;

public sealed partial class SplashWindow : Window
{
    private readonly bool _animations;
    private bool _transparent;
    private readonly DispatcherQueueTimer _exitTimer;
    private bool _exitStarted;

    public SplashWindow(ElementTheme theme, bool animations)
    {
        InitializeComponent();
        _animations = animations && SystemAnimationsEnabled();
        _transparent = SplashTiming.UsePerPixelTransparency(NativeMethods.WindowsBuild());
        Root.RequestedTheme = theme == ElementTheme.Default ? ElementTheme.Dark : theme;
        ApplyPalette(Root.RequestedTheme);
        if (!_animations)
        {
            Card.Opacity = 1;
            CardScale.ScaleX = 1;
            CardScale.ScaleY = 1;
            Wordmark.Opacity = 1;
            TitleShift.Y = 0;
        }

        _exitTimer = DispatcherQueue.CreateTimer();
        _exitTimer.IsRepeating = false;
        ConfigurePresenter();
        PlaceWindow();
        if (_transparent)
        {
            try
            {
                SystemBackdrop = new TransparentBackdrop();
            }
            catch
            {
                UseOpaqueCard();
            }
        }
        else
        {
            UseOpaqueCard();
        }

        Root.Loaded += (_, _) =>
        {
            if (_transparent)
            {
                AttachShadow();
            }

            if (_animations)
            {
                PlayEntrance();
            }
        };
    }

    public void ShowSplash()
    {
        Activate();
        var hwnd = WinRT.Interop.WindowNative.GetWindowHandle(this);
        RemoveSystemShadow(hwnd);
        if (!_transparent)
        {
            ApplyRoundedRegion(hwnd);
        }
    }

    public void ScheduleExit(int delayMs, Action closed)
    {
        _exitTimer.Interval = TimeSpan.FromMilliseconds(Math.Max(1, delayMs));
        _exitTimer.Tick += (_, _) =>
        {
            _exitTimer.Stop();
            BeginExit(closed);
        };
        _exitTimer.Start();
    }

    private void BeginExit(Action closed)
    {
        if (_exitStarted)
        {
            return;
        }

        _exitStarted = true;
        if (!_animations)
        {
            closed();
            return;
        }

        PlayExit();
        var hold = DispatcherQueue.CreateTimer();
        hold.Interval = TimeSpan.FromMilliseconds(SplashTiming.ExitHoldMs(true));
        hold.IsRepeating = false;
        hold.Tick += (_, _) =>
        {
            hold.Stop();
            closed();
        };
        hold.Start();
    }

    private void UseOpaqueCard()
    {
        _transparent = false;
        Card.Margin = new Thickness(0);
        Root.Background = CardFill.Background;
    }

    private void ApplyPalette(ElementTheme theme)
    {
        var dark = theme == ElementTheme.Dark;
        var background = dark
            ? Color.FromArgb(255, 0x1C, 0x1B, 0x1F)
            : Color.FromArgb(255, 0xFD, 0xF7, 0xFF);
        var foreground = dark
            ? Color.FromArgb(255, 0xFF, 0xFF, 0xFF)
            : Color.FromArgb(255, 0x1D, 0x1B, 0x20);
        CardFill.Background = new SolidColorBrush(background);
        Wordmark.Foreground = new SolidColorBrush(foreground);
    }

    private void ConfigurePresenter()
    {
        var presenter = OverlappedPresenter.Create();
        presenter.SetBorderAndTitleBar(false, false);
        presenter.IsResizable = false;
        presenter.IsMaximizable = false;
        presenter.IsMinimizable = false;
        presenter.IsAlwaysOnTop = true;
        AppWindow.SetPresenter(presenter);
        AppWindow.IsShownInSwitchers = true;
        AppWindow.Title = "tokinote";
    }

    private void PlaceWindow()
    {
        var hwnd = WinRT.Interop.WindowNative.GetWindowHandle(this);
        var dpi = NativeMethods.GetDpiForWindow(hwnd);
        if (dpi == 0)
        {
            dpi = 96;
        }

        var scale = dpi / 96.0;
        var width = (int)Math.Round(384 * scale);
        var height = (int)Math.Round(256 * scale);
        var display = DisplayArea.GetFromWindowId(AppWindow.Id, DisplayAreaFallback.Primary) ?? DisplayArea.Primary;
        var work = display?.WorkArea ?? new RectInt32(0, 0, 1920, 1080);
        var x = work.X + Math.Max(0, (work.Width - width) / 2);
        var y = work.Y + Math.Max(0, (work.Height - height) / 2);
        AppWindow.MoveAndResize(new RectInt32(x, y, width, height));
    }

    private static void RemoveSystemShadow(nint hwnd)
    {
        var disabled = 1;
        NativeMethods.DwmSetWindowAttribute(hwnd, 2, ref disabled, sizeof(int));
        var doNotRound = 1;
        NativeMethods.DwmSetWindowAttribute(hwnd, 33, ref doNotRound, sizeof(int));
    }

    private void ApplyRoundedRegion(nint hwnd)
    {
        var dpi = NativeMethods.GetDpiForWindow(hwnd);
        if (dpi == 0)
        {
            dpi = 96;
        }

        var scale = dpi / 96.0;
        var width = (int)Math.Round(384 * scale);
        var height = (int)Math.Round(256 * scale);
        var radius = (int)Math.Round(36 * scale);
        var region = NativeMethods.CreateRoundRectRgn(0, 0, width + 1, height + 1, radius * 2, radius * 2);
        NativeMethods.SetWindowRgn(hwnd, region, true);
    }

    private void AttachShadow()
    {
        var visual = ElementCompositionPreview.GetElementVisual(ShadowHost);
        var compositor = visual.Compositor;
        var size = new System.Numerics.Vector2((float)ShadowHost.ActualWidth, (float)ShadowHost.ActualHeight);
        if (size.X < 1 || size.Y < 1)
        {
            size = new System.Numerics.Vector2(368, 240);
        }

        var sprite = compositor.CreateSpriteVisual();
        sprite.Size = size;
        sprite.Brush = compositor.CreateColorBrush(Color.FromArgb(255, 0, 0, 0));
        var shadow = compositor.CreateDropShadow();
        shadow.BlurRadius = 30;
        shadow.Offset = new System.Numerics.Vector3(0, 10, 0);
        shadow.Color = Color.FromArgb(255, 0, 0, 0);
        shadow.Opacity = 0.28f;
        sprite.Shadow = shadow;
        ElementCompositionPreview.SetElementChildVisual(ShadowHost, sprite);
    }

    private void PlayEntrance()
    {
        var storyboard = new Storyboard();
        storyboard.Children.Add(Spline(Card, "Opacity", 0, 1, 550, 0.2, 0, 0, 1, TimeSpan.Zero));
        storyboard.Children.Add(Spline(CardScale, "ScaleX", 0.86, 1, 550, 0.2, 0, 0, 1, TimeSpan.Zero));
        storyboard.Children.Add(Spline(CardScale, "ScaleY", 0.86, 1, 550, 0.2, 0, 0, 1, TimeSpan.Zero));
        storyboard.Children.Add(Spline(Wordmark, "Opacity", 0, 1, 800, 0.05, 0.7, 0.1, 1, TimeSpan.FromMilliseconds(100)));
        storyboard.Children.Add(Spline(TitleShift, "Y", 10, 0, 800, 0.05, 0.7, 0.1, 1, TimeSpan.FromMilliseconds(100)));
        storyboard.Begin();
    }

    private void PlayExit()
    {
        var storyboard = new Storyboard();
        storyboard.Children.Add(Spline(Card, "Opacity", Card.Opacity, 0, SplashTiming.ExitDurationMs, 0.3, 0, 0.8, 0.15, TimeSpan.Zero));
        storyboard.Children.Add(Spline(CardScale, "ScaleX", CardScale.ScaleX, 0.94, SplashTiming.ExitDurationMs, 0.3, 0, 0.8, 0.15, TimeSpan.Zero));
        storyboard.Children.Add(Spline(CardScale, "ScaleY", CardScale.ScaleY, 0.94, SplashTiming.ExitDurationMs, 0.3, 0, 0.8, 0.15, TimeSpan.Zero));
        storyboard.Begin();
    }

    private static DoubleAnimationUsingKeyFrames Spline(
        DependencyObject target,
        string property,
        double from,
        double to,
        int milliseconds,
        double x1,
        double y1,
        double x2,
        double y2,
        TimeSpan begin)
    {
        var animation = new DoubleAnimationUsingKeyFrames
        {
            BeginTime = begin,
            Duration = TimeSpan.FromMilliseconds(milliseconds),
            FillBehavior = FillBehavior.HoldEnd,
        };
        animation.KeyFrames.Add(new SplineDoubleKeyFrame
        {
            KeyTime = KeyTime.FromTimeSpan(TimeSpan.Zero),
            Value = from,
        });
        animation.KeyFrames.Add(new SplineDoubleKeyFrame
        {
            KeyTime = KeyTime.FromTimeSpan(TimeSpan.FromMilliseconds(milliseconds)),
            Value = to,
            KeySpline = new KeySpline
            {
                ControlPoint1 = new Windows.Foundation.Point(x1, y1),
                ControlPoint2 = new Windows.Foundation.Point(x2, y2),
            },
        });
        Storyboard.SetTarget(animation, target);
        Storyboard.SetTargetProperty(animation, property);
        return animation;
    }

    private void Root_PointerPressed(object sender, PointerRoutedEventArgs e)
    {
        if (!e.GetCurrentPoint(Root).Properties.IsLeftButtonPressed)
        {
            return;
        }

        var hwnd = WinRT.Interop.WindowNative.GetWindowHandle(this);
        NativeMethods.ReleaseCapture();
        NativeMethods.SendMessage(hwnd, NativeMethods.WmNcLButtonDown, NativeMethods.HtCaption, 0);
    }

    private static bool SystemAnimationsEnabled()
    {
        try
        {
            return new UISettings().AnimationsEnabled;
        }
        catch
        {
            return true;
        }
    }
}
