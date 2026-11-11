using Microsoft.UI.Composition;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Media;
using Windows.UI;

namespace TokiNote;

/// <summary>
/// Clears the window to per-pixel transparency so the splash shadow can sit
/// on the desktop. Used on Windows 11, where system backdrop brushes work.
/// </summary>
public sealed class TransparentBackdrop : SystemBackdrop
{
    protected override void OnTargetConnected(ICompositionSupportsSystemBackdrop connectedTarget, XamlRoot xamlRoot)
    {
        connectedTarget.SystemBackdrop = xamlRoot.Compositor.CreateColorBrush(Color.FromArgb(0, 0, 0, 0));
    }

    protected override void OnTargetDisconnected(ICompositionSupportsSystemBackdrop connectedTarget)
    {
        connectedTarget.SystemBackdrop = null;
    }
}
