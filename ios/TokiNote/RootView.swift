import SwiftUI
import TokiNoteCore

struct RootView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.horizontalSizeClass) private var sizeClass
    @Environment(\.colorScheme) private var scheme
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        @Bindable var model = model
        Group {
            if sizeClass == .regular {
                pad
            } else {
                phone
            }
        }
        .tint(TokiPalette.primary(model.settings.palette, dark: scheme == .dark))
        .preferredColorScheme(colorScheme)
        .background {
            TokiScreenBackground(palette: model.settings.palette, amoled: model.settings.amoled)
        }
        .sheet(item: updateBinding) { release in
            UpdateSheet(release: release)
        }
        .onAppear {
            Task { await model.checkForUpdate(manual: false) }
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active {
                model.focus.sync()
                model.purgeTrash()
            }
        }
        .focusedSceneValue(\.tokiActions, TokiActions(
            newNote: { model.createNote(pushOnPhone: sizeClass != .regular) },
            toggleFocus: { model.focus.toggle() },
            resetFocus: { model.focus.reset() },
            skipFocus: { model.focus.skip() },
            showSection: { model.section = $0 }
        ))
    }

    private var colorScheme: ColorScheme? {
        switch model.settings.theme {
        case .system: return nil
        case .light: return .light
        case .dark: return .dark
        }
    }

    private var updateBinding: Binding<IOSRelease?> {
        Binding(
            get: { model.pendingUpdate },
            set: { model.pendingUpdate = $0 }
        )
    }

    private var phone: some View {
        @Bindable var model = model
        return TabView(selection: $model.section) {
            Tab(value: AppSection.notes) {
                NavigationStack(path: $model.notesPath) {
                    NotesList(usesSelection: false)
                        .navigationDestination(for: UUID.self) { NoteEditor(id: $0) }
                }
            } label: {
                Label(L10n.navNotes, systemImage: "note.text")
            }
            Tab(value: AppSection.focus) {
                NavigationStack {
                    FocusScreen()
                }
            } label: {
                Label(L10n.navFocus, systemImage: "timer")
            }
            Tab(value: AppSection.stats) {
                NavigationStack {
                    FocusSummaryScreen()
                }
            } label: {
                Label(L10n.navStats, systemImage: "chart.bar")
            }
            Tab(value: AppSection.settings) {
                NavigationStack {
                    SettingsView()
                }
            } label: {
                Label(L10n.navSettings, systemImage: "gearshape")
            }
        }
        .tabBarMinimizeBehavior(.onScrollDown)
    }

    private var pad: some View {
        @Bindable var model = model
        let selection = Binding<AppSection?>(
            get: { model.section },
            set: { model.section = $0 ?? model.section }
        )
        return NavigationSplitView {
            List(selection: selection) {
                ForEach(AppSection.allCases) { section in
                    Label(section.title, systemImage: section.symbol)
                        .tag(section)
                        .hoverEffect(.highlight)
                }
            }
            .navigationTitle("TokiNote")
            .listStyle(.sidebar)
        } content: {
            if model.section == .notes {
                NotesList(usesSelection: true)
            } else {
                ContentUnavailableView(model.section.title, systemImage: model.section.symbol)
            }
        } detail: {
            NavigationStack {
                switch model.section {
                case .notes:
                    if let id = model.selectedNoteID {
                        NoteEditor(id: id)
                    } else {
                        ContentUnavailableView(L10n.notesPlaceholder, systemImage: "note.text", description: Text(L10n.newNote))
                    }
                case .focus:
                    FocusScreen()
                case .stats:
                    FocusSummaryScreen()
                case .settings:
                    SettingsView()
                }
            }
        }
        .navigationSplitViewStyle(.balanced)
    }
}

struct TokiCommands: Commands {
    @FocusedValue(\.tokiActions) private var actions

    var body: some Commands {
        CommandGroup(replacing: .newItem) {
            Button(L10n.newNote) { actions?.newNote() }
                .keyboardShortcut("n", modifiers: .command)
        }
        CommandMenu(L10n.focusCommand) {
            Button(L10n.commandToggle) { actions?.toggleFocus() }
                .keyboardShortcut(.space, modifiers: [])
            Button(L10n.focusReset) { actions?.resetFocus() }
                .keyboardShortcut("r", modifiers: [.command, .shift])
            Button(L10n.focusSkip) { actions?.skipFocus() }
                .keyboardShortcut(".", modifiers: [.command, .shift])
            Divider()
            Button(L10n.navNotes) { actions?.showSection(.notes) }
                .keyboardShortcut("1", modifiers: .command)
            Button(L10n.navFocus) { actions?.showSection(.focus) }
                .keyboardShortcut("2", modifiers: .command)
            Button(L10n.navStats) { actions?.showSection(.stats) }
                .keyboardShortcut("3", modifiers: .command)
            Button(L10n.navSettings) { actions?.showSection(.settings) }
                .keyboardShortcut("4", modifiers: .command)
        }
    }
}
