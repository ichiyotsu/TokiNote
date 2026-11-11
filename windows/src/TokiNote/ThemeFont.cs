using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Media;

namespace TokiNote;

internal static class ThemeFont
{
    public static void Apply(ResourceDictionary resources)
    {
        var family = new FontFamily(AppFonts.FamilySource);
        resources["ContentControlThemeFontFamily"] = family;
        foreach (var merged in resources.MergedDictionaries)
        {
            Apply(merged);
        }

        foreach (var key in resources.ThemeDictionaries.Keys)
        {
            if (resources.ThemeDictionaries[key] is ResourceDictionary theme)
            {
                theme["ContentControlThemeFontFamily"] = family;
            }
        }
    }
}
