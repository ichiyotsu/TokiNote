using System.Runtime.InteropServices;

namespace TokiNote;

internal static class NativeMethods
{
    public const int SwHide = 0;
    public const int SwShow = 5;
    public const int WmNcLButtonDown = 0x00A1;
    public const int HtCaption = 2;

    [DllImport("user32.dll")]
    public static extern uint GetDpiForWindow(nint hwnd);

    [DllImport("user32.dll")]
    public static extern bool ShowWindow(nint hwnd, int command);

    [DllImport("user32.dll")]
    public static extern bool SetForegroundWindow(nint hwnd);

    [DllImport("user32.dll")]
    public static extern bool ReleaseCapture();

    [DllImport("user32.dll")]
    public static extern nint SendMessage(nint hwnd, int message, nint wParam, nint lParam);

    [DllImport("dwmapi.dll")]
    public static extern int DwmSetWindowAttribute(nint hwnd, int attribute, ref int value, int size);

    [DllImport("gdi32.dll")]
    public static extern nint CreateRoundRectRgn(int left, int top, int right, int bottom, int widthEllipse, int heightEllipse);

    [DllImport("user32.dll")]
    public static extern int SetWindowRgn(nint hwnd, nint region, bool redraw);

    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    public static extern nint LoadImage(nint instance, string path, uint type, int cx, int cy, uint load);

    [DllImport("shell32.dll", CharSet = CharSet.Unicode)]
    public static extern bool Shell_NotifyIcon(uint message, ref NotifyIconData data);

    [DllImport("user32.dll")]
    public static extern bool GetCursorPos(out NativePoint point);

    [DllImport("user32.dll")]
    public static extern nint CreatePopupMenu();

    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    public static extern bool AppendMenu(nint menu, uint flags, nuint id, string? text);

    [DllImport("user32.dll")]
    public static extern int TrackPopupMenu(nint menu, uint flags, int x, int y, int reserved, nint hwnd, nint rect);

    [DllImport("user32.dll")]
    public static extern bool DestroyMenu(nint menu);

    [DllImport("user32.dll")]
    public static extern bool PostMessage(nint hwnd, int message, nint wParam, nint lParam);

    [DllImport("comctl32.dll")]
    public static extern bool SetWindowSubclass(nint hwnd, SubclassProc callback, nuint id, nuint data);

    [DllImport("comctl32.dll")]
    public static extern bool RemoveWindowSubclass(nint hwnd, SubclassProc callback, nuint id);

    [DllImport("comctl32.dll")]
    public static extern nint DefSubclassProc(nint hwnd, uint message, nint wParam, nint lParam);

    [DllImport("ntdll.dll")]
    private static extern int RtlGetVersion(ref OsVersionInfo info);

    public delegate nint SubclassProc(nint hwnd, uint message, nint wParam, nint lParam, nuint id, nuint data);

    public static int WindowsBuild()
    {
        var info = new OsVersionInfo { dwOSVersionInfoSize = Marshal.SizeOf<OsVersionInfo>() };
        return RtlGetVersion(ref info) == 0 ? info.dwBuildNumber : Environment.OSVersion.Version.Build;
    }

    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
    private struct OsVersionInfo
    {
        public int dwOSVersionInfoSize;
        public int dwMajorVersion;
        public int dwMinorVersion;
        public int dwBuildNumber;
        public int dwPlatformId;
        [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 128)]
        public string szCSDVersion;
    }

    [StructLayout(LayoutKind.Sequential)]
    public struct NativePoint
    {
        public int X;
        public int Y;
    }

    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
    public struct NotifyIconData
    {
        public int cbSize;
        public nint hWnd;
        public uint uID;
        public uint uFlags;
        public uint uCallbackMessage;
        public nint hIcon;
        [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 128)]
        public string szTip;
        public uint dwState;
        public uint dwStateMask;
        [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 256)]
        public string szInfo;
        public uint uTimeoutOrVersion;
        [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 64)]
        public string szInfoTitle;
        public uint dwInfoFlags;
        public Guid guidItem;
        public nint hBalloonIcon;
    }
}
