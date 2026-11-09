namespace TokiNote.Tests;

public class SplashAndFontTests
{
    [Fact]
    public void App_font_is_yahei_with_segoe_fallback_and_not_inter()
    {
        Assert.Equal("Microsoft YaHei UI, Segoe UI Variable, Segoe UI", AppFonts.FamilySource);
        Assert.DoesNotContain("Inter", AppFonts.FamilySource, StringComparison.OrdinalIgnoreCase);
        Assert.Equal("0.1.0", AppVersion.Current);
    }

    [Fact]
    public void Splash_waits_for_main_window_and_the_minimum_visible_time()
    {
        Assert.Equal(-1, SplashTiming.DelayBeforeExit(500, mainReady: false));
        Assert.Equal(1500, SplashTiming.DelayBeforeExit(500, mainReady: true));
        Assert.Equal(0, SplashTiming.DelayBeforeExit(2000, mainReady: true));
        Assert.Equal(0, SplashTiming.DelayBeforeExit(3500, mainReady: true));
        Assert.Equal(320, SplashTiming.ExitHoldMs(animationsEnabled: true));
        Assert.Equal(0, SplashTiming.ExitHoldMs(animationsEnabled: false));
    }

    [Fact]
    public void Per_pixel_transparency_starts_at_windows_11()
    {
        Assert.False(SplashTiming.UsePerPixelTransparency(17763));
        Assert.False(SplashTiming.UsePerPixelTransparency(19041));
        Assert.False(SplashTiming.UsePerPixelTransparency(20348));
        Assert.True(SplashTiming.UsePerPixelTransparency(22000));
        Assert.True(SplashTiming.UsePerPixelTransparency(26100));
    }

    [Fact]
    public void Xaml_uses_yahei_for_the_app_and_google_sans_flex_only_on_the_splash()
    {
        var appXaml = File.ReadAllText(FindRepoFile(Path.Combine("src", "TokiNote", "App.xaml")));
        var splashXaml = File.ReadAllText(FindRepoFile(Path.Combine("src", "TokiNote", "SplashWindow.xaml")));
        var mainXaml = File.ReadAllText(FindRepoFile(Path.Combine("src", "TokiNote", "MainWindow.xaml")));
        Assert.Contains("ContentControlThemeFontFamily", appXaml, StringComparison.Ordinal);
        Assert.Contains(AppFonts.FamilySource, appXaml, StringComparison.Ordinal);
        Assert.Contains("tokinote", splashXaml, StringComparison.Ordinal);
        Assert.Contains("GoogleSansFlex.ttf#Google Sans Flex", splashXaml, StringComparison.Ordinal);
        Assert.Contains("FontSize=\"56\"", splashXaml, StringComparison.Ordinal);
        Assert.DoesNotContain("GoogleSansFlex", appXaml, StringComparison.Ordinal);
        Assert.DoesNotContain("GoogleSansFlex", mainXaml, StringComparison.Ordinal);
        Assert.DoesNotContain("InterVariable", appXaml + splashXaml + mainXaml, StringComparison.Ordinal);

        var license = File.ReadAllText(FindRepoFile(Path.Combine("src", "TokiNote", "Assets", "Fonts", "OFL.txt")));
        Assert.Contains("SIL Open Font License", license, StringComparison.Ordinal);
        var font = FindRepoFile(Path.Combine("src", "TokiNote", "Assets", "Fonts", "GoogleSansFlex.ttf"));
        Assert.True(new FileInfo(font).Length > 100_000);
    }

    private static string FindRepoFile(string relative)
    {
        var dir = new DirectoryInfo(AppContext.BaseDirectory);
        while (dir != null)
        {
            var candidate = Path.Combine(dir.FullName, relative);
            if (File.Exists(candidate))
            {
                return candidate;
            }

            dir = dir.Parent;
        }

        throw new FileNotFoundException(relative);
    }
}
