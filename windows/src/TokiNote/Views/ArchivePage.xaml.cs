using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;

namespace TokiNote;

public sealed partial class ArchivePage : Page
{
    private AppHost _host = null!;

    public ArchivePage()
    {
        InitializeComponent();
        TitleText.Text = Strings.Get("archive_title");
        Subtitle.Text = Strings.Get("archive_subtitle");
    }

    protected override void OnNavigatedTo(Microsoft.UI.Xaml.Navigation.NavigationEventArgs e)
    {
        _host = (AppHost)e.Parameter;
        Reload();
    }

    private void Reload()
    {
        Items.Items.Clear();
        var notes = _host.Database.ArchivedNotes();
        if (notes.Count == 0)
        {
            Items.Items.Add(new TextBlock { Text = Strings.Get("archive_empty"), Opacity = 0.7 });
            return;
        }

        foreach (var note in notes)
        {
            var id = note.Id;
            var button = new Button { Content = Strings.Get("archive_restore"), Tag = id };
            button.Click += (_, _) =>
            {
                _host.Database.Unarchive(id);
                Reload();
            };
            var row = new Grid { ColumnSpacing = 12, Margin = new Thickness(0, 6, 0, 6) };
            row.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            row.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
            var title = new TextBlock
            {
                Text = string.IsNullOrWhiteSpace(note.Title) ? Strings.Get("note_untitled") : note.Title,
                VerticalAlignment = VerticalAlignment.Center,
            };
            Grid.SetColumn(button, 1);
            row.Children.Add(title);
            row.Children.Add(button);
            Items.Items.Add(row);
        }
    }
}
