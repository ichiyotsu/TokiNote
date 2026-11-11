using System.Globalization;

namespace TokiNote;

public sealed record FocusStatsSummary(
    int TodayFocus,
    int TodayBreak,
    IReadOnlyList<int> WeekFocus,
    IReadOnlyList<DateOnly> WeekDays,
    int WeekAverage,
    IReadOnlyList<int> MonthFocus,
    int MonthAverage);

public static class FocusStats
{
    public static DateOnly SessionDate(FocusSession session, TimeZoneInfo zone)
    {
        var utc = DateTimeOffset.FromUnixTimeMilliseconds(session.EndedAt);
        var local = TimeZoneInfo.ConvertTime(utc, zone);
        return DateOnly.FromDateTime(local.DateTime);
    }

    public static FocusStatsSummary Summarize(
        IReadOnlyList<FocusSession> sessions,
        DateOnly today,
        TimeZoneInfo zone)
    {
        int Sum(DateOnly day, bool focus) => sessions
            .Where(session => SessionDate(session, zone) == day)
            .Sum(session => focus ? session.Minutes : session.BreakMinutes);

        var weekDays = Enumerable.Range(0, 7).Select(index => today.AddDays(-6 + index)).ToArray();
        var week = weekDays.Select(day => Sum(day, true)).ToArray();
        var monthDays = Enumerable.Range(0, 30).Select(index => today.AddDays(-29 + index)).ToArray();
        var month = monthDays.Select(day => Sum(day, true)).ToArray();
        return new FocusStatsSummary(
            Sum(today, true),
            Sum(today, false),
            week,
            weekDays,
            week.Sum() / 7,
            month,
            month.Sum() / 30);
    }

    public static string DurationLabel(int minutes) =>
        minutes < 60 ? $"{minutes} 分" : $"{minutes / 60} 小时 {minutes % 60} 分";

    public static string NarrowDayLabel(DateOnly day)
    {
        var names = CultureInfo.GetCultureInfo("zh-CN").DateTimeFormat.ShortestDayNames;
        return names[(int)day.DayOfWeek];
    }
}
