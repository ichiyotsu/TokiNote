namespace TokiNote;

/// <summary>
/// Splash timing shared with the WinUI window. The splash and the main window
/// are created together; the splash stays up until the main window is ready
/// and at least <see cref="MinimumVisibleMs"/> have passed since it appeared.
/// </summary>
public static class SplashTiming
{
    public const int MinimumVisibleMs = 2000;
    public const int ExitDurationMs = 300;
    public const int CloseDelayMs = 320;
    public const int Windows11Build = 22000;

    public static int DelayBeforeExit(long elapsedSinceShownMs, bool mainReady)
    {
        if (!mainReady)
        {
            return -1;
        }

        return (int)Math.Max(0, MinimumVisibleMs - Math.Max(0, elapsedSinceShownMs));
    }

    public static int ExitHoldMs(bool animationsEnabled) =>
        animationsEnabled ? CloseDelayMs : 0;

    /// <summary>
    /// Per-pixel window transparency needs the Windows 11 DWM backdrop path.
    /// Windows 10 keeps an opaque rounded card.
    /// </summary>
    public static bool UsePerPixelTransparency(int windowsBuild) =>
        windowsBuild >= Windows11Build;
}
