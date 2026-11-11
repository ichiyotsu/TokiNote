using Microsoft.UI.Xaml;
using Windows.UI.ViewManagement;

namespace TokiNote;

internal static class ThemeHelper
{
    public static ElementTheme Resolve(ThemeMode mode) => mode switch
    {
        ThemeMode.Light => ElementTheme.Light,
        ThemeMode.Dark => ElementTheme.Dark,
        _ => SystemTheme(),
    };

    public static bool SystemAnimationsEnabled()
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

    private static ElementTheme SystemTheme()
    {
        try
        {
            var background = new UISettings().GetColorValue(UIColorType.Background);
            var luminance = (background.R + background.G + background.B) / 3;
            return luminance < 128 ? ElementTheme.Dark : ElementTheme.Light;
        }
        catch
        {
            return ElementTheme.Light;
        }
    }
}
