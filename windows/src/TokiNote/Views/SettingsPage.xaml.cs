using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Windows.Storage;
using Windows.Storage.Pickers;

namespace TokiNote;

public sealed partial class SettingsPage : Page
{
    private AppHost _host = null!;
    private bool _loading = true;

    public SettingsPage()
    {
        InitializeComponent();
        AppearanceTitle.Text = Strings.Get("settings_appearance");
        ThemeLabel.Text = Strings.Get("theme_mode");
        ThemeSystem.Content = Strings.Get("theme_system");
        ThemeLight.Content = Strings.Get("theme_light");
        ThemeDark.Content = Strings.Get("theme_dark");
        AmoledSwitch.Header = Strings.Get("setting_amoled");
        AmoledSwitch.OffContent = Strings.Get("setting_amoled_desc");
        MotionSwitch.Header = Strings.Get("setting_motion");
        TimeSwitch.Header = Strings.Get("setting_show_time");
        TimerTitle.Text = Strings.Get("settings_timer");
        FocusLabel.Text = Strings.Get("focus_minutes");
        ShortLabel.Text = Strings.Get("short_break_minutes");
        LongLabel.Text = Strings.Get("long_break_minutes");
        SessionsLabel.Text = Strings.Get("timer_sessions");
        AutoSwitch.Header = Strings.Get("timer_auto_start");
        DataTitle.Text = Strings.Get("settings_data");
        ExportButton.Content = Strings.Get("data_export");
        ClearButton.Content = Strings.Get("data_clear");
        AboutTitle.Text = Strings.Get("settings_about");
        UpdateButton.Content = Strings.Get("about_check_update");
        GitHubButton.Content = Strings.Get("about_github");
    }

    protected override void OnNavigatedTo(Microsoft.UI.Xaml.Navigation.NavigationEventArgs e)
    {
        _host = (AppHost)e.Parameter;
        _loading = true;
        var settings = _host.Settings;
        ThemeSystem.IsChecked = settings.ThemeMode == ThemeMode.System;
        ThemeLight.IsChecked = settings.ThemeMode == ThemeMode.Light;
        ThemeDark.IsChecked = settings.ThemeMode == ThemeMode.Dark;
        AmoledSwitch.IsOn = settings.Amoled;
        MotionSwitch.IsOn = settings.ReduceMotion;
        TimeSwitch.IsOn = settings.ShowTime;
        FocusBox.Value = settings.FocusMinutes;
        ShortBox.Value = settings.ShortBreakMinutes;
        LongBox.Value = settings.LongBreakMinutes;
        SessionsBox.Value = settings.SessionsBeforeLongBreak;
        AutoSwitch.IsOn = settings.AutoStartNext;
        VersionText.Text = Strings.Format("update_current", AppVersion.Current);
        var counts = _host.Database.Counts();
        CountsText.Text = $"{Strings.Get("data_notes")} {counts.Notes} · {Strings.Get("data_archive")} {counts.Archived} · {Strings.Get("data_trash")} {counts.Trash}";
        _loading = false;
    }

    private void Theme_Checked(object sender, RoutedEventArgs e) => Save();

    private void Setting_Toggled(object sender, RoutedEventArgs e) => Save();

    private void Timer_Changed(NumberBox sender, NumberBoxValueChangedEventArgs args) => Save();

    private void Save()
    {
        if (_loading || _host == null)
        {
            return;
        }

        var theme = ThemeDark.IsChecked == true ? ThemeMode.Dark
            : ThemeLight.IsChecked == true ? ThemeMode.Light
            : ThemeMode.System;
        _host.UpdateSettings(_host.Settings with
        {
            ThemeMode = theme,
            Amoled = AmoledSwitch.IsOn,
            ReduceMotion = MotionSwitch.IsOn,
            ShowTime = TimeSwitch.IsOn,
            FocusMinutes = (int)FocusBox.Value,
            ShortBreakMinutes = (int)ShortBox.Value,
            LongBreakMinutes = (int)LongBox.Value,
            SessionsBeforeLongBreak = (int)SessionsBox.Value,
            AutoStartNext = AutoSwitch.IsOn,
        });
        App.Current.Shell?.ApplyTheme();
    }

    private async void ExportButton_Click(object sender, RoutedEventArgs e)
    {
        var picker = new FileSavePicker();
        var window = App.Current.Shell;
        if (window == null)
        {
            return;
        }

        WinRT.Interop.InitializeWithWindow.Initialize(picker, window.WindowHandle);
        picker.SuggestedFileName = "TokiNote-backup";
        picker.FileTypeChoices.Add("JSON", new List<string> { ".json" });
        var file = await picker.PickSaveFileAsync();
        if (file == null)
        {
            return;
        }

        await FileIO.WriteTextAsync(file, _host.Database.ExportJson());
    }

    private async void ClearButton_Click(object sender, RoutedEventArgs e)
    {
        var dialog = new ContentDialog
        {
            Title = Strings.Get("data_clear_title"),
            Content = Strings.Get("data_clear_body"),
            PrimaryButtonText = Strings.Get("data_clear"),
            CloseButtonText = Strings.Get("action_cancel"),
            XamlRoot = XamlRoot,
        };
        if (await dialog.ShowAsync() != ContentDialogResult.Primary)
        {
            return;
        }

        _host.Engine.Reset();
        _host.Database.ClearAll();
        _host.UpdateSettings(new AppSettings());
        _loading = true;
        ThemeSystem.IsChecked = true;
        ThemeLight.IsChecked = false;
        ThemeDark.IsChecked = false;
        AmoledSwitch.IsOn = false;
        MotionSwitch.IsOn = false;
        TimeSwitch.IsOn = true;
        FocusBox.Value = 25;
        ShortBox.Value = 5;
        LongBox.Value = 15;
        SessionsBox.Value = 4;
        AutoSwitch.IsOn = true;
        CountsText.Text = $"{Strings.Get("data_notes")} 0 · {Strings.Get("data_archive")} 0 · {Strings.Get("data_trash")} 0";
        _loading = false;
        App.Current.Shell?.ApplyTheme();
    }
}
