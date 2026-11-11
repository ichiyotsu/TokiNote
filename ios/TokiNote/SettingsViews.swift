import SwiftData
import SwiftUI
import TokiNoteCore

enum SettingsRoute: Hashable {
    case trash
    case timer
}

struct SettingsView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.colorScheme) private var scheme
    @Query private var notes: [NoteRecord]
    @State private var confirmClear = false

    private var activeCount: Int { notes.filter { $0.deletedAt == nil }.count }
    private var trashCount: Int { notes.filter { $0.deletedAt != nil }.count }
    private var version: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "0.3.7"
    }

    var body: some View {
        @Bindable var settings = model.settings
        Form {
            Section(L10n.settingsAppearance) {
                Picker(L10n.themeMode, selection: themeBinding) {
                    Text(L10n.themeSystem).tag(ThemeChoice.system)
                    Text(L10n.themeLight).tag(ThemeChoice.light)
                    Text(L10n.themeDark).tag(ThemeChoice.dark)
                }
                Picker(L10n.colorPreset, selection: paletteBinding) {
                    ForEach(PaletteChoice.allCases) { choice in
                        Text(choice.title).tag(choice)
                    }
                }
                Toggle(isOn: $settings.amoled) {
                    VStack(alignment: .leading) {
                        Text(L10n.settingAmoled)
                        Text(L10n.settingAmoledDesc).font(.footnote).foregroundStyle(.secondary)
                    }
                }
                .onChange(of: settings.amoled) { _, _ in model.saveSettings() }
                Toggle(isOn: $settings.showTime) {
                    VStack(alignment: .leading) {
                        Text(L10n.settingShowTime)
                        Text(L10n.settingShowTimeDesc).font(.footnote).foregroundStyle(.secondary)
                    }
                }
                .onChange(of: settings.showTime) { _, _ in model.saveSettings() }
                Toggle(isOn: $settings.reduceMotion) {
                    VStack(alignment: .leading) {
                        Text(L10n.settingMotion)
                        Text(L10n.settingMotionDesc).font(.footnote).foregroundStyle(.secondary)
                    }
                }
                .onChange(of: settings.reduceMotion) { _, _ in model.saveSettings() }
            }
            Section(L10n.settingsTimer) {
                NavigationLink(value: SettingsRoute.timer) {
                    LabeledContent(L10n.settingsTimer, value: formatClock(settings.focusMinutes * 60))
                }
            }
            Section(L10n.settingsData) {
                LabeledContent(L10n.dataNotes, value: "\(activeCount)")
                NavigationLink(value: SettingsRoute.trash) {
                    LabeledContent(L10n.dataTrash, value: "\(trashCount)")
                }
                Button(L10n.dataClear, role: .destructive) { confirmClear = true }
            }
            Section(L10n.settingsAbout) {
                LabeledContent("TokiNote", value: version)
                Button {
                    Task { await model.checkForUpdate(manual: true) }
                } label: {
                    HStack {
                        Text(L10n.aboutCheckUpdate)
                        Spacer()
                        if model.checkingUpdate {
                            ProgressView()
                        } else if !model.updateStatus.isEmpty {
                            Text(model.updateStatus).foregroundStyle(.secondary)
                        }
                    }
                }
                Link(destination: URL(string: "https://github.com/ichiyotsu/TokiNote")!) {
                    VStack(alignment: .leading) {
                        Text(L10n.aboutGithub)
                        Text(L10n.aboutGithubDesc).font(.footnote).foregroundStyle(.secondary)
                    }
                }
            }
        }
        .navigationTitle(L10n.navSettings)
        .navigationDestination(for: SettingsRoute.self) { route in
            switch route {
            case .trash: TrashView()
            case .timer: TimerSettingsView()
            }
        }
        .confirmationDialog(L10n.dataClearTitle, isPresented: $confirmClear, titleVisibility: .visible) {
            Button(L10n.dataClear, role: .destructive) { model.clearAll() }
            Button(L10n.actionCancel, role: .cancel) {}
        } message: {
            Text(L10n.dataClearBody)
        }
        .tint(TokiPalette.primary(settings.palette, dark: scheme == .dark))
    }

    private var themeBinding: Binding<ThemeChoice> {
        Binding(
            get: { model.settings.theme },
            set: {
                model.settings.theme = $0
                model.saveSettings()
            }
        )
    }

    private var paletteBinding: Binding<PaletteChoice> {
        Binding(
            get: { model.settings.palette },
            set: {
                model.settings.palette = $0
                model.saveSettings()
            }
        )
    }
}

struct UpdateSheet: View {
    var release: IOSRelease
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL
    private var current: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "0.3.7"
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(L10n.updateCurrent(current))
                        .foregroundStyle(.secondary)
                    Text(release.notes.isEmpty ? L10n.updateNotes : release.notes)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    if release.ipaURL == nil {
                        Text(L10n.updateNoIPA)
                            .foregroundStyle(.secondary)
                    }
                }
                .padding(20)
            }
            .navigationTitle(L10n.updateTitle(release.version))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.actionClose) { dismiss() }
                }
            }
            .safeAreaBar(edge: .bottom) {
                VStack(spacing: 10) {
                    Button(L10n.updateDownload) { download() }
                        .buttonStyle(.glassProminent)
                        .disabled(release.ipaURL == nil)
                        .frame(maxWidth: .infinity)
                    Button(L10n.updateSideStore) { openSideStore() }
                        .buttonStyle(.glass)
                        .frame(maxWidth: .infinity)
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 8)
            }
        }
    }

    private func download() {
        guard let url = release.ipaURL else { return }
        openURL(url)
    }

    private func openSideStore() {
        guard let ipa = release.ipaURL, let sideStore = IOSReleaseIndex.sideStoreInstallURL(for: ipa) else {
            if let page = release.pageURL { openURL(page) }
            return
        }
        openURL(sideStore) { accepted in
            if !accepted, let page = release.pageURL {
                openURL(page)
            }
        }
    }
}
