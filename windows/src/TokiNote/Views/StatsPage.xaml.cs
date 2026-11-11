using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;
using Windows.UI;

namespace TokiNote;

public sealed partial class StatsPage : Page
{
    public StatsPage()
    {
        InitializeComponent();
        TitleText.Text = Strings.Get("focus_summary");
        TodayLabel.Text = Strings.Get("stats_today");
        FocusLabel.Text = Strings.Get("stats_focus");
        BreakLabel.Text = Strings.Get("stats_break");
        WeekLabel.Text = Strings.Get("stats_last_week");
        MonthLabel.Text = Strings.Get("stats_last_month");
        WeekCaption.Text = Strings.Get("stats_avg_focus");
        MonthCaption.Text = Strings.Get("stats_avg_focus");
    }

    protected override void OnNavigatedTo(Microsoft.UI.Xaml.Navigation.NavigationEventArgs e)
    {
        var host = (AppHost)e.Parameter;
        var today = DateOnly.FromDateTime(DateTime.Now);
        var summary = FocusStats.Summarize(host.Database.LoadSessions(), today, TimeZoneInfo.Local);
        TodayFocus.Text = FocusStats.DurationLabel(summary.TodayFocus);
        TodayBreak.Text = FocusStats.DurationLabel(summary.TodayBreak);
        WeekAverage.Text = FocusStats.DurationLabel(summary.WeekAverage);
        MonthAverage.Text = FocusStats.DurationLabel(summary.MonthAverage);
        FillBars(WeekBars, summary.WeekFocus, summary.WeekDays.Select(FocusStats.NarrowDayLabel).ToArray(), 18);
        FillBars(MonthBars, summary.MonthFocus, null, 6);
    }

    private static void FillBars(StackPanel panel, IReadOnlyList<int> values, IReadOnlyList<string>? labels, double width)
    {
        panel.Children.Clear();
        var max = Math.Max(1, values.Count == 0 ? 1 : values.Max());
        for (var index = 0; index < values.Count; index++)
        {
            var height = 8 + 100.0 * values[index] / max;
            var column = new StackPanel { Width = width, VerticalAlignment = VerticalAlignment.Bottom };
            column.Children.Add(new Border
            {
                Height = height,
                CornerRadius = new CornerRadius(3),
                Background = new SolidColorBrush(Color.FromArgb(255, 0x4C, 0x66, 0x2B)),
                VerticalAlignment = VerticalAlignment.Bottom,
            });
            if (labels != null)
            {
                column.Children.Add(new TextBlock
                {
                    Text = labels[index],
                    HorizontalAlignment = HorizontalAlignment.Center,
                    FontSize = 12,
                    Margin = new Thickness(0, 6, 0, 0),
                });
            }

            panel.Children.Add(column);
        }
    }
}
