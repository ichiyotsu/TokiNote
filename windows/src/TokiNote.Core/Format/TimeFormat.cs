using System.Globalization;

namespace TokiNote;

public static class TimeFormat
{
    public static string FormatClock(int totalSeconds)
    {
        var safe = Math.Max(0, totalSeconds);
        return string.Format(CultureInfo.InvariantCulture, "{0:00}:{1:00}", safe / 60, safe % 60);
    }

    /// <summary>Count-up and the focus clock. Hours appear only after 60 minutes.</summary>
    public static string FormatElapsed(int totalSeconds)
    {
        var safe = Math.Max(0, totalSeconds);
        var hours = safe / 3600;
        if (hours > 0)
        {
            return string.Format(
                CultureInfo.InvariantCulture,
                "{0}:{1:00}:{2:00}",
                hours,
                (safe % 3600) / 60,
                safe % 60);
        }

        return string.Format(CultureInfo.InvariantCulture, "{0:00}:{1:00}", safe / 60, safe % 60);
    }

    public static string FormatMinutes(int minutes)
    {
        if (minutes < 60)
        {
            return $"{minutes} 分钟";
        }

        var hours = minutes / 60;
        var rest = minutes % 60;
        return rest == 0 ? $"{hours} 小时" : $"{hours} 小时 {rest} 分钟";
    }

    public static string FormatNoteDate(long epochMillis)
    {
        var local = DateTimeOffset.FromUnixTimeMilliseconds(epochMillis).ToLocalTime();
        return $"{local.Month}月{local.Day}日 {local:HH:mm}";
    }
}
