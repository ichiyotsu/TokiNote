namespace TokiNote.Tests;

public class WindowsUpdateCheckerTests
{
    [Fact]
    public void Patch_and_minor_bumps_are_newer()
    {
        Assert.True(WindowsUpdateChecker.IsNewer("1.0.1", "1.0.0"));
        Assert.True(WindowsUpdateChecker.IsNewer("1.1.0", "1.0.9"));
        Assert.True(WindowsUpdateChecker.IsNewer("v2.0.0", "1.9.9"));
    }

    [Fact]
    public void Same_version_is_not_newer()
    {
        Assert.False(WindowsUpdateChecker.IsNewer("1.0.0", "1.0.0"));
        Assert.False(WindowsUpdateChecker.IsNewer("1.0", "1.0.0"));
    }

    [Fact]
    public void Stable_beats_its_own_pre_release()
    {
        Assert.False(WindowsUpdateChecker.IsNewer("1.0.0-beta.2", "1.0.0"));
        Assert.True(WindowsUpdateChecker.IsNewer("1.0.0-beta.3", "1.0.0-beta.2"));
        Assert.True(WindowsUpdateChecker.IsNewer("1.0.1-beta", "1.0.0"));
    }

    [Fact]
    public void Windows_tags_ignore_android_releases_and_pick_the_newest()
    {
        var releases = new[]
        {
            Release("0.3.7"),
            Release("windows-0.1.0", "2026-01-01T00:00:00Z"),
            Release("windows-0.1.1", "2026-02-01T00:00:00Z"),
            Release("windows-0.2.0-beta", "2026-03-01T00:00:00Z"),
        };

        var latest = WindowsUpdateChecker.PickLatest(releases);
        Assert.Equal("0.2.0-beta", latest?.Version);
        Assert.Equal("windows-0.2.0-beta", latest?.Tag);
    }

    [Fact]
    public void Stable_windows_release_beats_same_core_beta()
    {
        var latest = WindowsUpdateChecker.PickLatest([
            Release("windows-0.1.0-beta"),
            Release("windows-0.1.0"),
        ]);
        Assert.Equal("0.1.0", latest?.Version);
    }

    [Fact]
    public void Asset_prefers_x64_exe_then_x64_zip()
    {
        var asset = WindowsUpdateChecker.PickAsset([
            new ReleaseAsset("TokiNote-win-x86.zip", "https://github.com/a.zip"),
            new ReleaseAsset("TokiNote-Windows-0.1.0-win-x64.zip", "https://github.com/b.zip"),
            new ReleaseAsset("notes.txt", "https://github.com/c.txt"),
        ]);
        Assert.Equal("TokiNote-Windows-0.1.0-win-x64.zip", asset?.Name);

        var exe = WindowsUpdateChecker.PickAsset([
            new ReleaseAsset("TokiNote-Windows-0.1.0-win-x64.zip", "https://github.com/b.zip"),
            new ReleaseAsset("TokiNote-Setup-win-x64.exe", "https://github.com/setup.exe"),
        ]);
        Assert.Equal("TokiNote-Setup-win-x64.exe", exe?.Name);
    }

    [Fact]
    public void Parse_skips_drafts_and_reads_windows_asset()
    {
        const string json = """
            [
              {"tag_name":"windows-0.1.0","draft":true,"body":"hidden","html_url":"https://github.com/hidden","assets":[]},
              {"tag_name":"0.3.5","draft":false,"body":"android","html_url":"https://github.com/android","assets":[{"name":"TokiNote.apk","browser_download_url":"https://github.com/a.apk"}]},
              {"tag_name":"windows-0.1.1","draft":false,"body":"hello","html_url":"https://github.com/win","published_at":"2026-04-01T00:00:00Z","assets":[{"name":"TokiNote-Windows-0.1.1-win-x64.zip","browser_download_url":"https://github.com/ichiyotsu/TokiNote/releases/download/windows-0.1.1/app.zip"}]}
            ]
            """;
        var latest = WindowsUpdateChecker.PickLatest(WindowsUpdateChecker.ParseReleases(json));
        Assert.Equal("0.1.1", latest?.Version);
        Assert.Equal("hello", latest?.Notes);
        Assert.Equal("TokiNote-Windows-0.1.1-win-x64.zip", latest?.Asset?.Name);
        Assert.True(WindowsUpdateChecker.IsNewer(latest!.Version, AppVersion.Current));
        Assert.False(WindowsUpdateChecker.IsNewer("0.1.0", AppVersion.Current));
    }

    [Fact]
    public void Download_urls_must_be_https_github()
    {
        Assert.True(WindowsUpdateChecker.IsAllowedDownloadUrl("https://github.com/ichiyotsu/TokiNote/releases/download/windows-0.1.0/app.zip"));
        Assert.True(WindowsUpdateChecker.IsAllowedDownloadUrl("https://objects.githubusercontent.com/a"));
        Assert.False(WindowsUpdateChecker.IsAllowedDownloadUrl("http://github.com/a.zip"));
        Assert.False(WindowsUpdateChecker.IsAllowedDownloadUrl("https://example.com/a.zip"));
    }

    private static WindowsRelease Release(string tag, string published = "2026-01-01T00:00:00Z") => new(
        tag,
        WindowsUpdateChecker.VersionFromTag(tag),
        "",
        "",
        DateTimeOffset.Parse(published),
        null);
}
