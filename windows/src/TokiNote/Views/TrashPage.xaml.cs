using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;

namespace TokiNote;

public sealed partial class TrashPage : Page
{
    private AppHost _host = null!;

    public TrashPage()
    {
        InitializeComponent();
        TitleText.Text = Strings.Get("trash_title");
        Subtitle.Text = Strings.Get("trash_subtitle");
    }

    protected override void OnNavigatedTo(Microsoft.UI.Xaml.Navigation.NavigationEventArgs e)
    {
        _host = (AppHost)e.Parameter;
        Reload();
    }

    private void Reload()
    {
        Items.Items.Clear();
        var notes = _host.Database.TrashedNotes();
        if (notes.Count == 0)
        {
            Items.Items.Add(new TextBlock { Text = Strings.Get("archive_empty"), Opacity = 0.7 });
            return;
        }

        foreach (var note in notes)
        {
            var id = note.Id;
            var restore = new Button { Content = Strings.Get("archive_restore") };
            var delete = new Button { Content = Strings.Get("archive_delete_forever"), Margin = new Thickness(8, 0, 0, 0) };
            restore.Click += (_, _) =>
            {
                _host.Database.Restore(id);
                Reload();
            };
            delete.Click += async (_, _) =>
            {
                var dialog = new ContentDialog
                {
                    Title = Strings.Get("archive_delete_forever"),
                    Content = Strings.Get("delete_note_body"),
                    PrimaryButtonText = Strings.Get("archive_delete_forever"),
                    CloseButtonText = Strings.Get("action_cancel"),
                    XamlRoot = XamlRoot,
                };
                if (await dialog.ShowAsync() == ContentDialogResult.Primary)
                {
                    _host.Database.DeletePermanently(id);
                    Reload();
                }
            };
            var row = new Grid { Margin = new Thickness(0, 6, 0, 6) };
            row.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            row.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
            var buttons = new StackPanel { Orientation = Orientation.Horizontal };
            buttons.Children.Add(restore);
            buttons.Children.Add(delete);
            Grid.SetColumn(buttons, 1);
            row.Children.Add(new TextBlock
            {
                Text = string.IsNullOrWhiteSpace(note.Title) ? Strings.Get("note_untitled") : note.Title,
                VerticalAlignment = VerticalAlignment.Center,
            });
            row.Children.Add(buttons);
            Items.Items.Add(row);
        }
    }
}
