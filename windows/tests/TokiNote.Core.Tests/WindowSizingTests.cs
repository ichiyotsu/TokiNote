namespace TokiNote.Tests;

public class WindowSizingTests
{
    [Fact]
    public void Minimum_track_size_is_640_by_480_at_100_percent()
    {
        Assert.Equal((640, 480), WindowSizing.MinimumTrackSize(96));
    }

    [Fact]
    public void Minimum_track_size_scales_with_dpi()
    {
        Assert.Equal((800, 600), WindowSizing.MinimumTrackSize(120));
        Assert.Equal((960, 720), WindowSizing.MinimumTrackSize(144));
        Assert.Equal((1280, 960), WindowSizing.MinimumTrackSize(192));
    }

    [Fact]
    public void Minimum_track_size_falls_back_to_96_dpi_when_unknown()
    {
        Assert.Equal((640, 480), WindowSizing.MinimumTrackSize(0));
    }

    [Fact]
    public void Main_window_navigation_pane_collapses_automatically()
    {
        var mainXaml = File.ReadAllText(FindRepoFile(Path.Combine("src", "TokiNote", "MainWindow.xaml")));
        Assert.Contains("PaneDisplayMode=\"Auto\"", mainXaml, StringComparison.Ordinal);
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
