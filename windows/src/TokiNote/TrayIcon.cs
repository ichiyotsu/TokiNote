using System.Runtime.InteropServices;

namespace TokiNote;

public sealed class TrayIcon : IDisposable
{
    private const uint NimAdd = 0;
    private const uint NimDelete = 2;
    private const uint NifMessage = 1;
    private const uint NifIcon = 2;
    private const uint NifTip = 4;
    private const uint TrayMessage = 0x8000 + 24;
    private const uint WmLButtonUp = 0x0202;
    private const uint WmLButtonDblClk = 0x0203;
    private const uint WmRButtonUp = 0x0205;
    private const uint MfString = 0;
    private const uint MfSeparator = 0x800;
    private const uint TpmReturnCmd = 0x0100;
    private const int WmNull = 0;

    private readonly MainWindow _window;
    private readonly AppHost _host;
    private readonly NativeMethods.SubclassProc _callback;
    private NativeMethods.NotifyIconData _data;
    private bool _added;

    public TrayIcon(MainWindow window, AppHost host)
    {
        _window = window;
        _host = host;
        _callback = Subclass;
        var hwnd = window.WindowHandle;
        if (!NativeMethods.SetWindowSubclass(hwnd, _callback, 1, 0))
        {
            return;
        }

        var iconPath = Path.Combine(AppContext.BaseDirectory, "Assets", "AppIcon.ico");
        var icon = NativeMethods.LoadImage(0, iconPath, 1, 16, 16, 0x10);
        _data = new NativeMethods.NotifyIconData
        {
            cbSize = Marshal.SizeOf<NativeMethods.NotifyIconData>(),
            hWnd = hwnd,
            uID = 1,
            uFlags = NifMessage | NifIcon | NifTip,
            uCallbackMessage = TrayMessage,
            hIcon = icon,
            szTip = "TokiNote",
        };
        _added = NativeMethods.Shell_NotifyIcon(NimAdd, ref _data);
    }

    public bool IsVisible => _added;

    public void Dispose()
    {
        if (_added)
        {
            NativeMethods.Shell_NotifyIcon(NimDelete, ref _data);
            _added = false;
        }

        NativeMethods.RemoveWindowSubclass(_window.WindowHandle, _callback, 1);
    }

    private nint Subclass(nint hwnd, uint message, nint wParam, nint lParam, nuint id, nuint data)
    {
        if (message == TrayMessage)
        {
            var mouse = (uint)lParam;
            if (mouse is WmLButtonUp or WmLButtonDblClk)
            {
                _window.DispatcherQueue.TryEnqueue(_window.Present);
                return 0;
            }

            if (mouse == WmRButtonUp)
            {
                ShowMenu(hwnd);
                return 0;
            }
        }

        return NativeMethods.DefSubclassProc(hwnd, message, wParam, lParam);
    }

    private void ShowMenu(nint hwnd)
    {
        var menu = NativeMethods.CreatePopupMenu();
        NativeMethods.AppendMenu(menu, MfString, 1, Strings.Get("tray_open"));
        var phase = _host.Engine.State.Phase;
        if (phase is FocusPhase.Running or FocusPhase.Paused)
        {
            NativeMethods.AppendMenu(menu, MfString, 2, phase == FocusPhase.Running ? Strings.Get("focus_pause") : Strings.Get("focus_play"));
        }

        NativeMethods.AppendMenu(menu, MfSeparator, 0, null);
        NativeMethods.AppendMenu(menu, MfString, 3, Strings.Get("tray_exit"));
        NativeMethods.GetCursorPos(out var point);
        NativeMethods.SetForegroundWindow(hwnd);
        var command = NativeMethods.TrackPopupMenu(menu, TpmReturnCmd, point.X, point.Y, 0, hwnd, 0);
        NativeMethods.PostMessage(hwnd, WmNull, 0, 0);
        NativeMethods.DestroyMenu(menu);
        _window.DispatcherQueue.TryEnqueue(() =>
        {
            switch (command)
            {
                case 1:
                    _window.Present();
                    break;
                case 2:
                    _host.Engine.Toggle();
                    break;
                case 3:
                    _window.ExitApplication();
                    break;
            }
        });
    }
}
