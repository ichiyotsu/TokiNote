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
    public void Plain_version_tags_with_a_windows_zip_are_compared()
    {
        var latest = WindowsUpdateChecker.PickLatest([
            Release("0.3.7", zip: false),
            Release("0.1.0", "2026-01-01T00:00:00Z"),
            Release("0.1.1", "2026-02-01T00:00:00Z"),
            Release("0.2.0-beta", "2026-03-01T00:00:00Z"),
        ]);
        Assert.Equal("0.2.0-beta", latest?.Version);
        Assert.Equal("0.2.0-beta", latest?.Tag);
    }

    [Fact]
    public void Stable_windows_release_beats_same_core_beta()
    {
        var latest = WindowsUpdateChecker.PickLatest([
            Release("0.1.0-beta"),
            Release("0.1.0"),
        ]);
        Assert.Equal("0.1.0", latest?.Version);
    }

    [Fact]
    public void Asset_picks_the_win_x64_zip()
    {
        var asset = WindowsUpdateChecker.PickAsset([
            new ReleaseAsset("TokiNote-0.3.7.apk", "https://github.com/a.apk"),
            new ReleaseAsset("TokiNote-ios-0.3.7.ipa", "https://github.com/a.ipa"),
            new ReleaseAsset("TokiNote-win-x86.zip", "https://github.com/a.zip"),
            new ReleaseAsset("TokiNote-Windows-0.3.7-win-x64.zip", "https://github.com/b.zip"),
            new ReleaseAsset("TokiNote-Setup-win-x64.exe", "https://github.com/setup.exe"),
        ]);
        Assert.Equal("TokiNote-Windows-0.3.7-win-x64.zip", asset?.Name);
        Assert.Null(WindowsUpdateChecker.PickAsset([
            new ReleaseAsset("TokiNote-0.3.7.apk", "https://github.com/a.apk"),
            new ReleaseAsset("TokiNote-Setup-win-x64.exe", "https://github.com/setup.exe"),
        ]));
    }

    [Fact]
    public void Parse_skips_drafts_and_releases_without_a_windows_zip()
    {
        const string json = """
            [
              {"tag_name":"0.1.0","draft":true,"body":"hidden","html_url":"https://github.com/hidden","assets":[{"name":"TokiNote-Windows-0.1.0-win-x64.zip","browser_download_url":"https://github.com/hidden.zip"}]},
              {"tag_name":"0.3.5","draft":false,"body":"android","html_url":"https://github.com/android","assets":[{"name":"TokiNote.apk","browser_download_url":"https://github.com/a.apk"}]},
              {"tag_name":"0.1.1","draft":false,"body":"hello","html_url":"https://github.com/win","published_at":"2026-04-01T00:00:00Z","assets":[{"name":"TokiNote-Windows-0.1.1-win-x64.zip","browser_download_url":"https://github.com/ichiyotsu/TokiNote/releases/download/0.1.1/app.zip"}]}
            ]
            """;
        var latest = WindowsUpdateChecker.PickLatest(WindowsUpdateChecker.ParseReleases(json));
        Assert.Equal("0.1.1", latest?.Version);
        Assert.Equal("hello", latest?.Notes);
        Assert.Equal("TokiNote-Windows-0.1.1-win-x64.zip", latest?.Asset?.Name);
        Assert.True(WindowsUpdateChecker.IsNewer(latest!.Version, "0.1.0"));
        Assert.False(WindowsUpdateChecker.IsNewer("0.1.0", "0.1.0"));
    }

    [Fact]
    public void Version_0_3_8_is_newer_and_releases_without_a_windows_zip_are_ignored()
    {
        Assert.True(WindowsUpdateChecker.IsNewer("0.3.8", "0.3.7"));
        Assert.False(WindowsUpdateChecker.IsNewer("0.3.7", "0.3.8"));
        Assert.False(WindowsUpdateChecker.IsNewer("0.3.8", "0.3.8"));
        Assert.True(WindowsUpdateChecker.IsNewer("0.3.8", "0.3.8-beta.4"));
        Assert.Equal("0.3.8", WindowsUpdateChecker.VersionFromTag("0.3.8"));
        Assert.Equal("0.3.8", WindowsUpdateChecker.VersionFromTag("v0.3.8"));
        Assert.True(WindowsUpdateChecker.IsPlainVersionTag("0.3.8"));
        Assert.False(WindowsUpdateChecker.IsPlainVersionTag("ios-0.3.8"));
        Assert.False(WindowsUpdateChecker.IsPlainVersionTag("windows-0.3.8"));

        var latest = WindowsUpdateChecker.PickLatest([
            Release("0.3.7"),
            Release("0.3.8"),
            Release("0.3.9", zip: false),
            Release("ios-9.0.0"),
        ]);
        Assert.Equal("0.3.8", latest?.Tag);
        Assert.Equal("0.3.8", latest?.Version);
        Assert.Equal("0.3.8", AppVersion.Current);
        Assert.False(WindowsUpdateChecker.IsNewer(AppVersion.Current, "0.3.8"));
    }

    [Fact]
    public void Download_urls_must_be_https_github()
    {
        Assert.True(WindowsUpdateChecker.IsAllowedDownloadUrl("https://github.com/ichiyotsu/TokiNote/releases/download/0.3.7/TokiNote-Windows-0.3.7-win-x64.zip"));
        Assert.True(WindowsUpdateChecker.IsAllowedDownloadUrl("https://objects.githubusercontent.com/a"));
        Assert.False(WindowsUpdateChecker.IsAllowedDownloadUrl("http://github.com/a.zip"));
        Assert.False(WindowsUpdateChecker.IsAllowedDownloadUrl("https://example.com/a.zip"));
    }

    private static WindowsRelease Release(string tag, string published = "2026-01-01T00:00:00Z", bool zip = true)
    {
        var version = WindowsUpdateChecker.VersionFromTag(tag);
        return new WindowsRelease(
            tag,
            version,
            "",
            "",
            DateTimeOffset.Parse(published),
            zip ? new ReleaseAsset($"TokiNote-Windows-{version}-win-x64.zip", "https://github.com/b.zip") : null);
    }
}
