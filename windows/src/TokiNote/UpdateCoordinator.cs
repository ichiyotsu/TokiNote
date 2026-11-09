using System.IO.Compression;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;

namespace TokiNote;

internal static class UpdateCoordinator
{
    public static async Task CheckAsync(MainWindow window, bool interactive)
    {
        var queue = window.DispatcherQueue;
        var (status, release) = await WindowsUpdateChecker.FetchAsync().ConfigureAwait(false);
        var shown = new TaskCompletionSource<object?>();
        if (!queue.TryEnqueue(async () =>
        {
            try
            {
                await PresentAsync(window, interactive, status, release);
            }
            finally
            {
                shown.TrySetResult(null);
            }
        }))
        {
            return;
        }

        await shown.Task.ConfigureAwait(false);
    }

    private static async Task PresentAsync(MainWindow window, bool interactive, UpdateCheckStatus status, WindowsRelease? release)
    {
        if (status == UpdateCheckStatus.Failed)
        {
            if (interactive)
            {
                window.ShowInfo(Strings.Get("about_check_failed"));
            }

            return;
        }

        if (release == null || !WindowsUpdateChecker.IsNewer(release.Version, AppVersion.Current))
        {
            if (interactive)
            {
                window.ShowInfo(release == null ? Strings.Get("about_no_release") : Strings.Get("about_latest"));
            }

            return;
        }

        var notes = string.IsNullOrWhiteSpace(release.Notes) ? release.Tag : release.Notes;
        var dialog = new ContentDialog
        {
            Title = Strings.Format("update_title", release.Version),
            Content = new ScrollViewer
            {
                MaxHeight = 280,
                Content = new TextBlock
                {
                    Text = Strings.Format("update_current", AppVersion.Current) + "\n\n" + notes,
                    TextWrapping = TextWrapping.Wrap,
                },
            },
            PrimaryButtonText = Strings.Get("update_download"),
            CloseButtonText = Strings.Get("action_cancel"),
            DefaultButton = ContentDialogButton.Primary,
            XamlRoot = window.Content.XamlRoot,
        };
        if (release.Asset == null || !WindowsUpdateChecker.IsAllowedDownloadUrl(release.Asset.Url))
        {
            dialog.PrimaryButtonText = "";
            ((TextBlock)((ScrollViewer)dialog.Content).Content).Text += "\n\n" + Strings.Get("update_no_package");
            await dialog.ShowAsync();
            return;
        }

        if (await dialog.ShowAsync() != ContentDialogResult.Primary)
        {
            return;
        }

        var progress = new ProgressBar { Minimum = 0, Maximum = 100, IsIndeterminate = true };
        var label = new TextBlock { Text = Strings.Format("update_downloading", 0) };
        var panel = new StackPanel { Spacing = 12, Children = { label, progress } };
        var downloading = new ContentDialog
        {
            Title = Strings.Format("update_title", release.Version),
            Content = panel,
            XamlRoot = window.Content.XamlRoot,
        };
        var shown = downloading.ShowAsync();
        var ext = release.Asset.Name.EndsWith(".exe", StringComparison.OrdinalIgnoreCase) ? ".exe" : ".zip";
        var destination = Path.Combine(Path.GetTempPath(), "TokiNote-update" + ext);
        try
        {
            await UpdateInstaller.DownloadAsync(release.Asset.Url, destination, new Progress<int>(value =>
            {
                progress.IsIndeterminate = false;
                progress.Value = value;
                label.Text = Strings.Format("update_downloading", value);
            }));
            downloading.Hide();
            UpdateInstaller.InstallAndRelaunch(destination, release.Asset.Name);
        }
        catch
        {
            downloading.Hide();
            window.ShowInfo(Strings.Get("update_failed"));
        }

        try
        {
            await shown;
        }
        catch
        {
            // Hide completes the dialog.
        }
    }
}

internal static class UpdateInstaller
{
    public static async Task DownloadAsync(string url, string destination, IProgress<int>? progress, CancellationToken cancellationToken = default)
    {
        using var http = new HttpClient { Timeout = TimeSpan.FromMinutes(10) };
        http.DefaultRequestHeaders.UserAgent.ParseAdd(WindowsUpdateChecker.UserAgent);
        using var response = await http.GetAsync(url, HttpCompletionOption.ResponseHeadersRead, cancellationToken).ConfigureAwait(false);
        response.EnsureSuccessStatusCode();
        var total = response.Content.Headers.ContentLength ?? -1;
        await using var input = await response.Content.ReadAsStreamAsync(cancellationToken).ConfigureAwait(false);
        Directory.CreateDirectory(Path.GetDirectoryName(destination)!);
        await using var output = File.Create(destination);
        var buffer = new byte[64 * 1024];
        long read = 0;
        var last = -1;
        while (true)
        {
            var count = await input.ReadAsync(buffer, cancellationToken).ConfigureAwait(false);
            if (count <= 0)
            {
                break;
            }

            await output.WriteAsync(buffer.AsMemory(0, count), cancellationToken).ConfigureAwait(false);
            read += count;
            if (total > 0)
            {
                var percent = (int)Math.Clamp(read * 100 / total, 0, 100);
                if (percent != last)
                {
                    last = percent;
                    progress?.Report(percent);
                }
            }
        }
    }

    public static void InstallAndRelaunch(string packagePath, string assetName)
    {
        var installDir = AppContext.BaseDirectory.TrimEnd(Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar);
        if (assetName.EndsWith(".exe", StringComparison.OrdinalIgnoreCase))
        {
            System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo(packagePath) { UseShellExecute = true });
            App.Current.Host.Dispose();
            Application.Current.Exit();
            return;
        }

        var stage = Path.Combine(Path.GetTempPath(), "TokiNote-update-stage");
        if (Directory.Exists(stage))
        {
            Directory.Delete(stage, true);
        }

        Directory.CreateDirectory(stage);
        ZipFile.ExtractToDirectory(packagePath, stage, overwriteFiles: true);
        var exe = Directory.EnumerateFiles(stage, "TokiNote.exe", SearchOption.AllDirectories).FirstOrDefault()
            ?? throw new FileNotFoundException("TokiNote.exe");
        var source = Path.GetDirectoryName(exe)!;
        var script = Path.Combine(Path.GetTempPath(), "TokiNote-update.ps1");
        var pid = Environment.ProcessId;
        var content = $$"""
            $ErrorActionPreference = 'Stop'
            try { Wait-Process -Id {{pid}} -Timeout 90 -ErrorAction SilentlyContinue } catch {}
            Start-Sleep -Seconds 1
            try {
              Copy-Item -Path (Join-Path '{{Escape(source)}}' '*') -Destination '{{Escape(installDir)}}' -Recurse -Force
              Start-Process -FilePath '{{Escape(Path.Combine(installDir, "TokiNote.exe"))}}'
            } catch {
              Add-Type -AssemblyName PresentationFramework
              [System.Windows.MessageBox]::Show($_.Exception.Message, 'TokiNote')
            }
            """;
        File.WriteAllText(script, content, new System.Text.UTF8Encoding(encoderShouldEmitUTF8Identifier: true));
        System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo
        {
            FileName = "powershell.exe",
            Arguments = $"-NoProfile -ExecutionPolicy Bypass -File \"{script}\"",
            UseShellExecute = false,
            CreateNoWindow = true,
        });
        App.Current.Host.Dispose();
        Application.Current.Exit();
    }

    private static string Escape(string path) => path.Replace("'", "''");
}
