import SwiftData
import SwiftUI

@main
struct TokiNoteApp: App {
    @State private var model: AppModel

    init() {
        let container = try! ModelContainer(for: NoteRecord.self, FocusSessionRecord.self, SettingsRecord.self)
        _model = State(initialValue: AppModel(container: container))
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(model)
                .modelContainer(model.container)
        }
        .commands { TokiCommands() }
    }
}
