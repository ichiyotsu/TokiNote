using System.Text.Json;
using Microsoft.Data.Sqlite;

namespace TokiNote;

public static class AppPaths
{
    public static string Root => Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "TokiNote");

    public static string Database => Path.Combine(Root, "tokinote.db");

    public static string TimerSnapshot => Path.Combine(Root, "focus-timer.txt");
}

public sealed class TokiDatabase : IDisposable
{
    public const long TrashRetentionMs = 14L * 24 * 60 * 60 * 1000;

    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        WriteIndented = true,
    };

    private readonly SqliteConnection _connection;
    private readonly object _gate = new();

    public TokiDatabase(string path)
    {
        var directory = Path.GetDirectoryName(path);
        if (!string.IsNullOrEmpty(directory))
        {
            Directory.CreateDirectory(directory);
        }

        var builder = new SqliteConnectionStringBuilder { DataSource = path };
        _connection = new SqliteConnection(builder.ToString());
        _connection.Open();
        Execute("PRAGMA journal_mode=WAL;");
        Execute("""
            CREATE TABLE IF NOT EXISTS notes (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                body TEXT NOT NULL,
                emoji TEXT NOT NULL,
                color_index INTEGER NOT NULL,
                pinned INTEGER NOT NULL,
                done INTEGER NOT NULL,
                archived INTEGER NOT NULL,
                deleted_at INTEGER NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                day INTEGER NOT NULL
            )
            """);
        Execute("""
            CREATE TABLE IF NOT EXISTS sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                ended_at INTEGER NOT NULL,
                minutes INTEGER NOT NULL,
                break_minutes INTEGER NOT NULL
            )
            """);
        Execute("""
            CREATE TABLE IF NOT EXISTS app_settings (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                json TEXT NOT NULL
            )
            """);
    }

    public Note CreateNote(long now, long epochDay)
    {
        var note = new Note
        {
            Id = Guid.NewGuid().ToString("D"),
            Emoji = PickEmoji(now),
            CreatedAt = now,
            UpdatedAt = now,
            Day = epochDay,
        };
        Upsert(note);
        return note;
    }

    public IReadOnlyList<Note> LoadNotes()
    {
        lock (_gate)
        {
            using var command = _connection.CreateCommand();
            command.CommandText = """
                SELECT id, title, body, emoji, color_index, pinned, done, archived,
                       deleted_at, created_at, updated_at, day
                FROM notes
                """;
            using var reader = command.ExecuteReader();
            var notes = new List<Note>();
            while (reader.Read())
            {
                notes.Add(ReadNote(reader));
            }

            return notes;
        }
    }

    public Note? Find(string id) => LoadNotes().FirstOrDefault(note => note.Id == id);

    public void Upsert(Note note)
    {
        lock (_gate)
        {
            using var command = _connection.CreateCommand();
            command.CommandText = """
                INSERT INTO notes (
                    id, title, body, emoji, color_index, pinned, done, archived,
                    deleted_at, created_at, updated_at, day)
                VALUES (
                    $id, $title, $body, $emoji, $color, $pinned, $done, $archived,
                    $deleted, $created, $updated, $day)
                ON CONFLICT(id) DO UPDATE SET
                    title = excluded.title,
                    body = excluded.body,
                    emoji = excluded.emoji,
                    color_index = excluded.color_index,
                    pinned = excluded.pinned,
                    done = excluded.done,
                    archived = excluded.archived,
                    deleted_at = excluded.deleted_at,
                    created_at = excluded.created_at,
                    updated_at = excluded.updated_at,
                    day = excluded.day
                """;
            Add(command, "$id", note.Id);
            Add(command, "$title", note.Title);
            Add(command, "$body", note.Body);
            Add(command, "$emoji", note.Emoji);
            Add(command, "$color", note.ColorIndex);
            Add(command, "$pinned", note.Pinned ? 1 : 0);
            Add(command, "$done", note.Done ? 1 : 0);
            Add(command, "$archived", note.Archived ? 1 : 0);
            Add(command, "$deleted", note.DeletedAt.HasValue ? note.DeletedAt.Value : DBNull.Value);
            Add(command, "$created", note.CreatedAt);
            Add(command, "$updated", note.UpdatedAt);
            Add(command, "$day", note.Day);
            command.ExecuteNonQuery();
        }
    }

    public void UpdateContent(Note note, long now)
    {
        note.UpdatedAt = now;
        Upsert(note);
    }

    public void Trash(string id, long now)
    {
        var note = Find(id);
        if (note == null)
        {
            return;
        }

        note.DeletedAt = now;
        note.Pinned = false;
        Upsert(note);
    }

    public void Restore(string id)
    {
        var note = Find(id);
        if (note == null)
        {
            return;
        }

        note.DeletedAt = null;
        Upsert(note);
    }

    public void Archive(string id)
    {
        var note = Find(id);
        if (note == null)
        {
            return;
        }

        note.Archived = true;
        note.Pinned = false;
        Upsert(note);
    }

    public void Unarchive(string id)
    {
        var note = Find(id);
        if (note == null)
        {
            return;
        }

        note.Archived = false;
        Upsert(note);
    }

    public void DeletePermanently(string id)
    {
        lock (_gate)
        {
            using var command = _connection.CreateCommand();
            command.CommandText = "DELETE FROM notes WHERE id = $id";
            Add(command, "$id", id);
            command.ExecuteNonQuery();
        }
    }

    public int PurgeExpiredTrash(long now)
    {
        var cutoff = now - TrashRetentionMs;
        lock (_gate)
        {
            using var command = _connection.CreateCommand();
            command.CommandText = "DELETE FROM notes WHERE deleted_at IS NOT NULL AND deleted_at < $cutoff";
            Add(command, "$cutoff", cutoff);
            return command.ExecuteNonQuery();
        }
    }

    public IReadOnlyList<Note> SearchActive(string query, bool ascending = false)
    {
        IEnumerable<Note> notes = LoadNotes().Where(note => note.DeletedAt == null && !note.Archived);
        if (!string.IsNullOrWhiteSpace(query))
        {
            notes = notes.Where(note =>
                note.Title.Contains(query, StringComparison.OrdinalIgnoreCase) ||
                note.Body.Contains(query, StringComparison.OrdinalIgnoreCase));
        }

        notes = ascending
            ? notes.OrderByDescending(note => note.Pinned).ThenBy(note => note.UpdatedAt)
            : notes.OrderByDescending(note => note.Pinned).ThenByDescending(note => note.UpdatedAt);
        return notes.ToList();
    }

    public IReadOnlyList<Note> ArchivedNotes() => LoadNotes()
        .Where(note => note.Archived && note.DeletedAt == null)
        .OrderByDescending(note => note.UpdatedAt)
        .ToList();

    public IReadOnlyList<Note> TrashedNotes() => LoadNotes()
        .Where(note => note.DeletedAt != null)
        .OrderByDescending(note => note.DeletedAt)
        .ToList();

    public void AddSession(FocusSession session)
    {
        lock (_gate)
        {
            using var command = _connection.CreateCommand();
            command.CommandText = """
                INSERT INTO sessions (ended_at, minutes, break_minutes)
                VALUES ($ended, $minutes, $break)
                """;
            Add(command, "$ended", session.EndedAt);
            Add(command, "$minutes", session.Minutes);
            Add(command, "$break", session.BreakMinutes);
            command.ExecuteNonQuery();
        }
    }

    public IReadOnlyList<FocusSession> LoadSessions()
    {
        lock (_gate)
        {
            using var command = _connection.CreateCommand();
            command.CommandText = "SELECT ended_at, minutes, break_minutes FROM sessions ORDER BY ended_at";
            using var reader = command.ExecuteReader();
            var sessions = new List<FocusSession>();
            while (reader.Read())
            {
                sessions.Add(new FocusSession(reader.GetInt64(0), reader.GetInt32(1), reader.GetInt32(2)));
            }

            return sessions;
        }
    }

    public AppSettings LoadSettings()
    {
        lock (_gate)
        {
            using var command = _connection.CreateCommand();
            command.CommandText = "SELECT json FROM app_settings WHERE id = 1";
            var raw = command.ExecuteScalar() as string;
            if (string.IsNullOrWhiteSpace(raw))
            {
                return new AppSettings();
            }

            try
            {
                return JsonSerializer.Deserialize<AppSettings>(raw) ?? new AppSettings();
            }
            catch
            {
                return new AppSettings();
            }
        }
    }

    public void SaveSettings(AppSettings settings)
    {
        lock (_gate)
        {
            using var command = _connection.CreateCommand();
            command.CommandText = """
                INSERT INTO app_settings (id, json) VALUES (1, $json)
                ON CONFLICT(id) DO UPDATE SET json = excluded.json
                """;
            Add(command, "$json", JsonSerializer.Serialize(settings, JsonOptions));
            command.ExecuteNonQuery();
        }
    }

    public string ExportJson()
    {
        var payload = new
        {
            notes = LoadNotes(),
            sessions = LoadSessions(),
            settings = LoadSettings(),
        };
        return JsonSerializer.Serialize(payload, JsonOptions);
    }

    public (int Notes, int Archived, int Trash) Counts()
    {
        var notes = LoadNotes();
        return (
            notes.Count(note => note.DeletedAt == null && !note.Archived),
            notes.Count(note => note.Archived && note.DeletedAt == null),
            notes.Count(note => note.DeletedAt != null));
    }

    public void ClearAll()
    {
        Execute("DELETE FROM notes");
        Execute("DELETE FROM sessions");
        Execute("DELETE FROM app_settings");
    }

    public void Dispose() => _connection.Dispose();

    private void Execute(string sql)
    {
        lock (_gate)
        {
            using var command = _connection.CreateCommand();
            command.CommandText = sql;
            command.ExecuteNonQuery();
        }
    }

    private static Note ReadNote(SqliteDataReader reader) => new()
    {
        Id = reader.GetString(0),
        Title = reader.GetString(1),
        Body = reader.GetString(2),
        Emoji = reader.GetString(3),
        ColorIndex = reader.GetInt32(4),
        Pinned = reader.GetInt32(5) != 0,
        Done = reader.GetInt32(6) != 0,
        Archived = reader.GetInt32(7) != 0,
        DeletedAt = reader.IsDBNull(8) ? null : reader.GetInt64(8),
        CreatedAt = reader.GetInt64(9),
        UpdatedAt = reader.GetInt64(10),
        Day = reader.GetInt64(11),
    };

    private static void Add(SqliteCommand command, string name, object value)
    {
        command.Parameters.AddWithValue(name, value);
    }

    private static string PickEmoji(long now)
    {
        string[] emojis = ["😀", "🌿", "☕️", "📚", "🌙", "✨"];
        var index = (int)(Math.Abs(now) % emojis.Length);
        return emojis[index];
    }
}
