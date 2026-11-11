using System.Globalization;
using System.Net.Http.Headers;
using System.Text.Json;

namespace TokiNote;

public sealed record ReleaseAsset(string Name, string Url);

public sealed record WindowsRelease(
    string Tag,
    string Version,
    string Notes,
    string PageUrl,
    DateTimeOffset PublishedAt,
    ReleaseAsset? Asset);

public enum UpdateCheckStatus
{
    Failed,
    None,
    Found,
}

public static class WindowsUpdateChecker
{
    public const string ReleasesUrl = "https://api.github.com/repos/ichiyotsu/TokiNote/releases?per_page=100";
    public const string UserAgent = "TokiNote-Windows";

    public static bool IsWindowsTag(string? tag) =>
        !string.IsNullOrWhiteSpace(tag) &&
        tag.Trim().StartsWith("windows-", StringComparison.OrdinalIgnoreCase);

    public static string VersionFromTag(string tag)
    {
        var value = tag.Trim();
        const string prefix = "windows-";
        if (value.StartsWith(prefix, StringComparison.OrdinalIgnoreCase))
        {
            value = value[prefix.Length..];
        }

        if (value.Length > 1 && (value[0] == 'v' || value[0] == 'V') && char.IsDigit(value[1]))
        {
            value = value[1..];
        }

        return value;
    }

    public static bool IsNewer(string remote, string current)
    {
        var left = ParsedVersion.Parse(remote);
        var right = ParsedVersion.Parse(current);
        var core = CompareCore(left.Core, right.Core);
        if (core > 0)
        {
            return true;
        }

        if (core < 0)
        {
            return false;
        }

        if (left.PreRelease.Length == 0)
        {
            return right.PreRelease.Length > 0;
        }

        if (right.PreRelease.Length == 0)
        {
            return false;
        }

        return string.CompareOrdinal(left.PreRelease, right.PreRelease) > 0;
    }

    public static WindowsRelease? PickLatest(IEnumerable<WindowsRelease> releases)
    {
        WindowsRelease? best = null;
        foreach (var release in releases)
        {
            if (!IsWindowsTag(release.Tag))
            {
                continue;
            }

            if (best == null || IsNewer(release.Version, best.Version))
            {
                best = release;
                continue;
            }

            if (!IsNewer(best.Version, release.Version) && release.PublishedAt > best.PublishedAt)
            {
                best = release;
            }
        }

        return best;
    }

    public static ReleaseAsset? PickAsset(IEnumerable<ReleaseAsset> assets)
    {
        ReleaseAsset? best = null;
        var bestScore = int.MinValue;
        foreach (var asset in assets)
        {
            var score = Score(asset.Name);
            if (score > bestScore)
            {
                best = asset;
                bestScore = score;
            }
        }

        return bestScore > 0 ? best : null;
    }

    public static bool IsAllowedDownloadUrl(string url)
    {
        if (!Uri.TryCreate(url, UriKind.Absolute, out var uri))
        {
            return false;
        }

        if (!string.Equals(uri.Scheme, "https", StringComparison.OrdinalIgnoreCase))
        {
            return false;
        }

        var host = uri.Host;
        return host.Equals("github.com", StringComparison.OrdinalIgnoreCase)
            || host.Equals("objects.githubusercontent.com", StringComparison.OrdinalIgnoreCase)
            || host.Equals("release-assets.githubusercontent.com", StringComparison.OrdinalIgnoreCase)
            || host.EndsWith(".githubusercontent.com", StringComparison.OrdinalIgnoreCase);
    }

    public static IReadOnlyList<WindowsRelease> ParseReleases(string json)
    {
        using var document = JsonDocument.Parse(json);
        if (document.RootElement.ValueKind != JsonValueKind.Array)
        {
            return Array.Empty<WindowsRelease>();
        }

        var releases = new List<WindowsRelease>();
        foreach (var item in document.RootElement.EnumerateArray())
        {
            if (item.TryGetProperty("draft", out var draft) && draft.ValueKind == JsonValueKind.True)
            {
                continue;
            }

            var tag = ReadString(item, "tag_name");
            if (string.IsNullOrWhiteSpace(tag))
            {
                continue;
            }

            var assets = new List<ReleaseAsset>();
            if (item.TryGetProperty("assets", out var assetArray) && assetArray.ValueKind == JsonValueKind.Array)
            {
                foreach (var asset in assetArray.EnumerateArray())
                {
                    var name = ReadString(asset, "name");
                    var url = ReadString(asset, "browser_download_url");
                    if (!string.IsNullOrWhiteSpace(name) && !string.IsNullOrWhiteSpace(url))
                    {
                        assets.Add(new ReleaseAsset(name, url));
                    }
                }
            }

            var published = DateTimeOffset.MinValue;
            var publishedRaw = ReadString(item, "published_at");
            if (!string.IsNullOrWhiteSpace(publishedRaw) &&
                DateTimeOffset.TryParse(publishedRaw, CultureInfo.InvariantCulture, DateTimeStyles.RoundtripKind, out var parsed))
            {
                published = parsed;
            }

            releases.Add(new WindowsRelease(
                tag,
                VersionFromTag(tag),
                ReadString(item, "body"),
                ReadString(item, "html_url"),
                published,
                PickAsset(assets)));
        }

        return releases;
    }

    public static async Task<(UpdateCheckStatus Status, WindowsRelease? Release)> FetchAsync(
        HttpClient? httpClient = null,
        CancellationToken cancellationToken = default)
    {
        var ownsClient = httpClient == null;
        var http = httpClient ?? new HttpClient { Timeout = TimeSpan.FromSeconds(20) };
        try
        {
            var url = ReleasesUrl;
            var all = new List<WindowsRelease>();
            for (var page = 0; page < 5 && !string.IsNullOrWhiteSpace(url); page++)
            {
                using var request = new HttpRequestMessage(HttpMethod.Get, url);
                request.Headers.UserAgent.ParseAdd(UserAgent);
                request.Headers.Accept.Add(new MediaTypeWithQualityHeaderValue("application/vnd.github+json"));
                using var response = await http.SendAsync(request, cancellationToken).ConfigureAwait(false);
                if (!response.IsSuccessStatusCode)
                {
                    return all.Count == 0
                        ? (UpdateCheckStatus.Failed, null)
                        : ResultOf(all);
                }

                var json = await response.Content.ReadAsStringAsync(cancellationToken).ConfigureAwait(false);
                all.AddRange(ParseReleases(json));
                url = NextLink(response);
            }

            return ResultOf(all);
        }
        catch
        {
            return (UpdateCheckStatus.Failed, null);
        }
        finally
        {
            if (ownsClient)
            {
                http.Dispose();
            }
        }
    }

    private static (UpdateCheckStatus Status, WindowsRelease? Release) ResultOf(List<WindowsRelease> releases)
    {
        var latest = PickLatest(releases);
        return latest == null ? (UpdateCheckStatus.None, null) : (UpdateCheckStatus.Found, latest);
    }

    private static string? NextLink(HttpResponseMessage response)
    {
        if (!response.Headers.TryGetValues("Link", out var values))
        {
            return null;
        }

        foreach (var value in values)
        {
            foreach (var part in value.Split(','))
            {
                var pieces = part.Split(';');
                if (pieces.Length < 2 || !pieces[1].Contains("rel=\"next\"", StringComparison.OrdinalIgnoreCase))
                {
                    continue;
                }

                var link = pieces[0].Trim().TrimStart('<').TrimEnd('>');
                if (Uri.TryCreate(link, UriKind.Absolute, out _))
                {
                    return link;
                }
            }
        }

        return null;
    }

    private static int Score(string name)
    {
        var lower = name.ToLowerInvariant();
        var exe = lower.EndsWith(".exe", StringComparison.Ordinal);
        var zip = lower.EndsWith(".zip", StringComparison.Ordinal);
        if (!exe && !zip)
        {
            return 0;
        }

        var x64 = lower.Contains("x64", StringComparison.Ordinal) || lower.Contains("win64", StringComparison.Ordinal);
        if (exe && x64)
        {
            return 4;
        }

        if (zip && x64)
        {
            return 3;
        }

        return exe ? 2 : 1;
    }

    private static string ReadString(JsonElement element, string name)
    {
        if (!element.TryGetProperty(name, out var value) || value.ValueKind != JsonValueKind.String)
        {
            return "";
        }

        return value.GetString() ?? "";
    }

    private static int CompareCore(IReadOnlyList<int> left, IReadOnlyList<int> right)
    {
        for (var index = 0; index < Math.Max(left.Count, right.Count); index++)
        {
            var a = index < left.Count ? left[index] : 0;
            var b = index < right.Count ? right[index] : 0;
            if (a != b)
            {
                return a.CompareTo(b);
            }
        }

        return 0;
    }

    private readonly record struct ParsedVersion(int[] Core, string PreRelease)
    {
        public static ParsedVersion Parse(string raw)
        {
            var trimmed = raw.Trim();
            if (trimmed.Length > 1 && (trimmed[0] == 'v' || trimmed[0] == 'V') && char.IsDigit(trimmed[1]))
            {
                trimmed = trimmed[1..];
            }

            var dash = trimmed.IndexOf('-');
            var corePart = dash < 0 ? trimmed : trimmed[..dash];
            var pre = dash < 0 ? "" : trimmed[(dash + 1)..];
            var numbers = corePart.Split('.', StringSplitOptions.RemoveEmptyEntries);
            var core = new int[3];
            for (var index = 0; index < core.Length; index++)
            {
                core[index] = index < numbers.Length && int.TryParse(numbers[index], NumberStyles.Integer, CultureInfo.InvariantCulture, out var value)
                    ? value
                    : 0;
            }

            return new ParsedVersion(core, pre);
        }
    }
}
