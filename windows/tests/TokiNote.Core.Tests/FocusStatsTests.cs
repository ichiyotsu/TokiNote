namespace TokiNote.Tests;

public class FocusStatsTests
{
    [Fact]
    public void Summary_matches_the_android_today_week_and_month_windows()
    {
        var zone = TimeZoneInfo.Utc;
        var today = new DateOnly(2026, 11, 9);
        long At(DateOnly day, int hour) => new DateTimeOffset(day.ToDateTime(new TimeOnly(hour, 0)), TimeSpan.Zero).ToUnixTimeMilliseconds();
        var sessions = new List<FocusSession>
        {
            new(At(today, 9), 25),
            new(At(today, 10), 0, 5),
            new(At(today.AddDays(-1), 9), 50),
            new(At(today.AddDays(-10), 9), 10),
        };

        var summary = FocusStats.Summarize(sessions, today, zone);
        Assert.Equal(25, summary.TodayFocus);
        Assert.Equal(5, summary.TodayBreak);
        Assert.Equal(7, summary.WeekFocus.Count);
        Assert.Equal(30, summary.MonthFocus.Count);
        Assert.Equal(25, summary.WeekFocus[6]);
        Assert.Equal(50, summary.WeekFocus[5]);
        Assert.Equal((25 + 50) / 7, summary.WeekAverage);
        Assert.Equal("25 分", FocusStats.DurationLabel(25));
        Assert.Equal("2 小时 0 分", FocusStats.DurationLabel(120));
    }
}
