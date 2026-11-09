namespace TokiNote.Tests;

public class TimeFormatTests
{
    [Fact]
    public void Clock_formats_minutes_and_seconds()
    {
        Assert.Equal("25:00", TimeFormat.FormatClock(1500));
        Assert.Equal("00:09", TimeFormat.FormatClock(9));
        Assert.Equal("00:00", TimeFormat.FormatClock(0));
    }

    [Fact]
    public void Elapsed_shows_hours_once_it_passes_sixty_minutes()
    {
        Assert.Equal("25:00", TimeFormat.FormatElapsed(1500));
        Assert.Equal("59:59", TimeFormat.FormatElapsed(3599));
        Assert.Equal("1:00:00", TimeFormat.FormatElapsed(3600));
        Assert.Equal("1:01:01", TimeFormat.FormatElapsed(3661));
    }

    [Fact]
    public void Minutes_formats_hours_and_minutes()
    {
        Assert.Equal("25 分钟", TimeFormat.FormatMinutes(25));
        Assert.Equal("1 小时 5 分钟", TimeFormat.FormatMinutes(65));
        Assert.Equal("2 小时", TimeFormat.FormatMinutes(120));
    }
}
