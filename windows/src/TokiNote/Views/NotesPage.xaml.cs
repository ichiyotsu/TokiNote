using Microsoft.UI.Dispatching;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;

namespace TokiNote;

public sealed class NoteListItem
{
    public string Id { get; init; } = "";
    public string Title { get; init; } = "";
    public string Preview { get; init; } = "";
    public string When { get; init; } = "";
}

public sealed partial class NotesPage : Page
{
    private AppHost _host = null!;
    private string? _selectedId;
    private bool _loadingEditor;
    private DispatcherQueueTimer? _saveTimer;

    public NotesPage()
    {
        InitializeComponent();
        NewButton.Content = Strings.Get("entry_new");
        SearchBox.PlaceholderText = Strings.Get("search_tokinote");
        TitleBox.PlaceholderText = Strings.Get("title_hint");
        BodyBox.PlaceholderText = Strings.Get("editor_hint");
        ArchiveButton.Content = Strings.Get("preview_archive");
        DeleteButton.Content = Strings.Get("action_delete");
        EmptyLabel.Text = Strings.Get("notes_empty");
        EmojiBox.TextChanged += Editor_TextChanged;
    }

    protected override void OnNavigatedTo(Microsoft.UI.Xaml.Navigation.NavigationEventArgs e)
    {
        _host = (AppHost)e.Parameter;
        _saveTimer = DispatcherQueue.GetForCurrentThread().CreateTimer();
        _saveTimer.Interval = TimeSpan.FromMilliseconds(300);
        _saveTimer.Tick += (_, _) =>
        {
            _saveTimer.Stop();
            SaveSelected(refreshList: false);
        };
        Reload(null);
    }

    protected override void OnNavigatedFrom(Microsoft.UI.Xaml.Navigation.NavigationEventArgs e)
    {
        _saveTimer?.Stop();
        SaveSelected(refreshList: false);
        DeleteIfBlank();
    }

    private void Reload(string? selectId)
    {
        var notes = _host.Database.SearchActive(SearchBox.Text);
        NoteList.ItemsSource = notes.Select(note => new NoteListItem
        {
            Id = note.Id,
            Title = string.IsNullOrWhiteSpace(note.Title) ? (string.IsNullOrWhiteSpace(note.Emoji) ? Strings.Get("note_untitled") : note.Emoji + "  " + Strings.Get("note_untitled")) : (note.Pinned ? "📌 " : "") + note.Title,
            Preview = note.Preview,
            When = _host.Settings.ShowTime ? TimeFormat.FormatNoteDate(note.UpdatedAt) : "",
        }).ToList();
        EmptyLabel.Visibility = notes.Count == 0 && _selectedId == null ? Visibility.Visible : Visibility.Collapsed;
        if (selectId != null)
        {
            NoteList.SelectedItem = ((List<NoteListItem>)NoteList.ItemsSource).FirstOrDefault(item => item.Id == selectId);
        }
    }

    private void SearchBox_TextChanged(object sender, TextChangedEventArgs e) => Reload(_selectedId);

    private void NewButton_Click(object sender, RoutedEventArgs e)
    {
        SaveSelected(refreshList: false);
        DeleteIfBlank();
        var now = DateTimeOffset.Now.ToUnixTimeMilliseconds();
        var day = (long)(DateTime.Today - new DateTime(1970, 1, 1)).TotalDays;
        var note = _host.Database.CreateNote(now, day);
        _selectedId = note.Id;
        Reload(note.Id);
        ShowEditor(note);
        TitleBox.Focus(FocusState.Programmatic);
    }

    private void NoteList_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        if (NoteList.SelectedItem is not NoteListItem item || item.Id == _selectedId)
        {
            return;
        }

        SaveSelected(refreshList: false);
        DeleteIfBlank();
        _selectedId = item.Id;
        var note = _host.Database.Find(item.Id);
        if (note != null)
        {
            ShowEditor(note);
        }
    }

    private void ShowEditor(Note note)
    {
        _loadingEditor = true;
        Editor.Visibility = Visibility.Visible;
        EmptyLabel.Visibility = Visibility.Collapsed;
        EmojiBox.Text = note.Emoji;
        TitleBox.Text = note.Title;
        BodyBox.Text = note.Body;
        PinButton.Content = note.Pinned ? Strings.Get("action_unpin") : Strings.Get("action_pin");
        _loadingEditor = false;
    }

    private void Editor_TextChanged(object sender, TextChangedEventArgs e)
    {
        if (_loadingEditor || _saveTimer == null)
        {
            return;
        }

        _saveTimer.Stop();
        _saveTimer.Start();
    }

    private void SaveSelected(bool refreshList)
    {
        if (_selectedId == null)
        {
            return;
        }

        var note = _host.Database.Find(_selectedId);
        if (note == null || note.Archived || note.DeletedAt != null)
        {
            return;
        }

        note.Emoji = EmojiBox.Text;
        note.Title = TitleBox.Text;
        note.Body = BodyBox.Text;
        _host.Database.UpdateContent(note, DateTimeOffset.Now.ToUnixTimeMilliseconds());
        if (refreshList)
        {
            Reload(_selectedId);
        }
    }

    private void DeleteIfBlank()
    {
        if (_selectedId == null)
        {
            return;
        }

        var note = _host.Database.Find(_selectedId);
        if (note != null && note.IsBlank && note.DeletedAt == null)
        {
            _host.Database.DeletePermanently(note.Id);
            _selectedId = null;
            Editor.Visibility = Visibility.Collapsed;
        }
    }

    private void PinButton_Click(object sender, RoutedEventArgs e)
    {
        if (_selectedId == null)
        {
            return;
        }

        SaveSelected(refreshList: false);
        var note = _host.Database.Find(_selectedId);
        if (note == null)
        {
            return;
        }

        note.Pinned = !note.Pinned;
        _host.Database.Upsert(note);
        ShowEditor(note);
        Reload(note.Id);
    }

    private void ArchiveButton_Click(object sender, RoutedEventArgs e)
    {
        if (_selectedId == null)
        {
            return;
        }

        SaveSelected(refreshList: false);
        var id = _selectedId;
        _host.Database.Archive(id);
        _selectedId = null;
        Editor.Visibility = Visibility.Collapsed;
        Reload(null);
    }

    private async void DeleteButton_Click(object sender, RoutedEventArgs e)
    {
        if (_selectedId == null)
        {
            return;
        }

        var dialog = new ContentDialog
        {
            Title = Strings.Get("delete_note_title"),
            Content = Strings.Get("delete_note_body"),
            PrimaryButtonText = Strings.Get("action_delete"),
            CloseButtonText = Strings.Get("action_cancel"),
            XamlRoot = XamlRoot,
        };
        if (await dialog.ShowAsync() != ContentDialogResult.Primary)
        {
            return;
        }

        _host.Database.Trash(_selectedId, DateTimeOffset.Now.ToUnixTimeMilliseconds());
        _selectedId = null;
        Editor.Visibility = Visibility.Collapsed;
        Reload(null);
    }
}
