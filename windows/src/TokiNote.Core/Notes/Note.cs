namespace TokiNote;

public sealed class Note
{
    public string Id { get; set; } = "";
    public string Title { get; set; } = "";
    public string Body { get; set; } = "";
    public string Emoji { get; set; } = "😀";
    public int ColorIndex { get; set; }
    public bool Pinned { get; set; }
    public bool Done { get; set; }
    public bool Archived { get; set; }
    public long? DeletedAt { get; set; }
    public long CreatedAt { get; set; }
    public long UpdatedAt { get; set; }
    public long Day { get; set; }

    public bool IsBlank => string.IsNullOrWhiteSpace(Title) && string.IsNullOrWhiteSpace(Body);

    public string Preview => Body.Trim();
}
