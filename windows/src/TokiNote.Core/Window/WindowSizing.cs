namespace TokiNote;

/// <summary>
/// 主窗口的最小尺寸（逻辑单位）与 DPI 换算。
///
/// WinUI 3 没有给窗口设最小宽高的直接 API，最小拖拽尺寸要在
/// WM_GETMINMAXINFO 里按物理像素给出，所以把换算放在这里便于测试；
/// 换算方式和启动窗 PlaceWindow 那边保持一致（dpi / 96），
/// 这样高 DPI 下最小尺寸的实际大小也不会跟着变小。
///
/// 640×480 的取法：侧边栏（NavigationView，Auto 模式）在 641 以下
/// 会收成只剩菜单按钮，640 正好是还能正常用的下限；再小内容区
/// 就只剩一条缝了。
/// </summary>
public static class WindowSizing
{
    public const int MinimumWidthDip = 640;
    public const int MinimumHeightDip = 480;

    /// <summary>把最小尺寸按窗口当前 DPI 换算成物理像素；DPI 读不到（0）时按 96 处理。</summary>
    public static (int Width, int Height) MinimumTrackSize(uint dpi)
    {
        var effectiveDpi = dpi == 0 ? 96u : dpi;
        var scale = effectiveDpi / 96.0;
        return (
            (int)Math.Round(MinimumWidthDip * scale),
            (int)Math.Round(MinimumHeightDip * scale));
    }
}
