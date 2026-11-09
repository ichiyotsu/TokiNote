namespace TokiNote.Tests;

public class NoteRepositoryTests
{
    [Fact]
    public void Search_trash_archive_and_purge_match_the_note_model()
    {
        var path = Path.Combine(Path.GetTempPath(), "tokinote-tests", Guid.NewGuid().ToString("N"), "notes.db");
        using var database = new TokiDatabase(path);
        var now = 1_700_000_000_000L;
        var first = database.CreateNote(now, 19_000);
        first.Title = "买菜";
        first.Body = "番茄和牛奶";
        database.UpdateContent(first, now + 10);
        var second = database.CreateNote(now + 20, 19_000);
        second.Title = "会议";
        second.Body = "下午三点";
        second.Pinned = true;
        database.UpdateContent(second, now + 30);

        var results = database.SearchActive("番茄");
        var found = Assert.Single(results);
        Assert.Equal(first.Id, found.Id);

        var ordered = database.SearchActive("");
        Assert.Equal(second.Id, ordered[0].Id);

        database.Archive(first.Id);
        Assert.DoesNotContain(database.SearchActive(""), note => note.Id == first.Id);
        Assert.Equal(first.Id, Assert.Single(database.ArchivedNotes()).Id);

        database.Trash(second.Id, now + 40);
        Assert.Empty(database.SearchActive(""));
        Assert.Equal(second.Id, Assert.Single(database.TrashedNotes()).Id);
        Assert.False(database.Find(second.Id)!.Pinned);

        database.Restore(second.Id);
        Assert.Equal(second.Id, Assert.Single(database.SearchActive("")).Id);

        database.Trash(second.Id, now);
        var removed = database.PurgeExpiredTrash(now + TokiDatabase.TrashRetentionMs + 1);
        Assert.Equal(1, removed);
        Assert.Null(database.Find(second.Id));
    }

    [Fact]
    public void Settings_and_sessions_round_trip()
    {
        var path = Path.Combine(Path.GetTempPath(), "tokinote-tests", Guid.NewGuid().ToString("N"), "notes.db");
        using var database = new TokiDatabase(path);
        database.SaveSettings(new AppSettings { FocusMinutes = 40, ThemeMode = ThemeMode.Dark, ReduceMotion = true });
        var loaded = database.LoadSettings();
        Assert.Equal(40, loaded.FocusMinutes);
        Assert.Equal(ThemeMode.Dark, loaded.ThemeMode);
        Assert.True(loaded.ReduceMotion);

        database.AddSession(new FocusSession(10, 25));
        database.AddSession(new FocusSession(20, 0, 5));
        Assert.Equal(2, database.LoadSessions().Count);
    }
}
