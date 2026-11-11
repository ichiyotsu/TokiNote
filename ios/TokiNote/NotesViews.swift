import SwiftData
import SwiftUI
import TokiNoteCore

struct NotesList: View {
    var usesSelection: Bool
    @Environment(AppModel.self) private var model
    @Query(sort: \NoteRecord.updatedAt, order: .reverse) private var notes: [NoteRecord]
    @State private var query = ""
    @State private var ascending = false

    private var visible: [NoteRecord] {
        notes.filter { note in
            guard note.deletedAt == nil else { return false }
            let needle = query.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !needle.isEmpty else { return true }
            if note.title.localizedStandardContains(needle) || note.body.localizedStandardContains(needle) {
                return true
            }
            return note.title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
                L10n.noteUntitled.localizedStandardContains(needle)
        }
    }

    private var ordered: [NoteRecord] {
        visible.sorted { lhs, rhs in
            if lhs.pinned != rhs.pinned { return lhs.pinned && !rhs.pinned }
            return ascending ? lhs.updatedAt < rhs.updatedAt : lhs.updatedAt > rhs.updatedAt
        }
    }

    var body: some View {
        let pinned = ordered.filter(\.pinned)
        let rest = ordered.filter { !$0.pinned }
        List {
            if !pinned.isEmpty {
                Section(L10n.sectionPinned) {
                    rows(pinned)
                }
            }
            Section(pinned.isEmpty ? "" : L10n.sectionAll) {
                rows(rest)
            }
        }
        .listStyle(.insetGrouped)
        .searchable(text: $query, prompt: L10n.searchPrompt)
        .navigationTitle(L10n.navNotes)
        .overlay {
            if ordered.isEmpty {
                ContentUnavailableView(
                    query.isEmpty ? L10n.notesEmpty : L10n.notesEmptySearch,
                    systemImage: query.isEmpty ? "note.text" : "magnifyingglass",
                    description: Text(query.isEmpty ? L10n.editorHint : L10n.notesEmptySearch)
                )
            }
        }
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button(ascending ? L10n.sortDescending : L10n.sortAscending, systemImage: ascending ? "arrow.up" : "arrow.down") {
                    ascending.toggle()
                }
            }
            ToolbarItem(placement: .primaryAction) {
                Button(L10n.newNote, systemImage: "square.and.pencil") {
                    model.createNote(pushOnPhone: !usesSelection)
                }
                .keyboardShortcut("n", modifiers: .command)
            }
        }
    }

    @ViewBuilder
    private func rows(_ items: [NoteRecord]) -> some View {
        ForEach(items) { note in
            if usesSelection {
                Button {
                    model.selectedNoteID = note.id
                } label: {
                    NoteRow(note: note, showTime: model.settings.showTime)
                }
                .buttonStyle(.plain)
                .listRowBackground(model.selectedNoteID == note.id ? Color.accentColor.opacity(0.18) : nil)
                .hoverEffect(.highlight)
                .swipeActions(edge: .trailing) { trashButton(note) }
                .swipeActions(edge: .leading) { pinButton(note) }
                .contextMenu { pinButton(note); trashButton(note) }
            } else {
                NavigationLink(value: note.id) {
                    NoteRow(note: note, showTime: model.settings.showTime)
                }
                .hoverEffect(.highlight)
                .swipeActions(edge: .trailing) { trashButton(note) }
                .swipeActions(edge: .leading) { pinButton(note) }
                .contextMenu { pinButton(note); trashButton(note) }
            }
        }
    }

    private func pinButton(_ note: NoteRecord) -> some View {
        Button(note.pinned ? L10n.actionUnpin : L10n.actionPin, systemImage: note.pinned ? "pin.slash" : "pin") {
            note.pinned.toggle()
            note.updatedAt = .now
            try? model.context.save()
        }
        .tint(.orange)
    }

    private func trashButton(_ note: NoteRecord) -> some View {
        Button(L10n.actionDelete, systemImage: "trash", role: .destructive) {
            model.trash(note)
        }
    }
}

struct NoteRow: View {
    var note: NoteRecord
    var showTime: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(note.displayTitle)
                .font(.headline)
                .lineLimit(1)
            if !note.body.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                Text(note.body.trimmingCharacters(in: .whitespacesAndNewlines))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }
            if showTime {
                Text(formatNoteDate(note.updatedAt))
                    .font(.caption)
                    .foregroundStyle(.tertiary)
            }
        }
        .padding(.vertical, 4)
        .accessibilityElement(children: .combine)
    }
}

struct NoteEditor: View {
    var id: UUID
    @Environment(AppModel.self) private var model
    @Environment(\.dismiss) private var dismiss
    @Query private var notes: [NoteRecord]
    @State private var confirmTrash = false

    private var note: NoteRecord? { notes.first { $0.id == id } }

    var body: some View {
        Group {
            if let note {
                editor(note)
            } else {
                ContentUnavailableView(L10n.notesPlaceholder, systemImage: "note.text")
            }
        }
        .navigationTitle(note?.displayTitle ?? L10n.noteUntitled)
        .navigationBarTitleDisplayMode(.inline)
    }

    private func editor(_ note: NoteRecord) -> some View {
        @Bindable var note = note
        return VStack(spacing: 0) {
            TextField(L10n.titleHint, text: $note.title)
                .font(.title2.weight(.semibold))
                .padding(.horizontal, 20)
                .padding(.top, 12)
            TextEditor(text: $note.body)
                .font(.body)
                .scrollContentBackground(.hidden)
                .padding(.horizontal, 14)
                .overlay(alignment: .topLeading) {
                    if note.body.isEmpty {
                        Text(L10n.editorHint)
                            .foregroundStyle(.tertiary)
                            .padding(.horizontal, 20)
                            .padding(.top, 8)
                            .allowsHitTesting(false)
                    }
                }
        }
        .background(Color.clear)
        .onChange(of: note.title) { _, _ in note.updatedAt = .now }
        .onChange(of: note.body) { _, _ in note.updatedAt = .now }
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button(note.pinned ? L10n.actionUnpin : L10n.actionPin, systemImage: note.pinned ? "pin.slash" : "pin") {
                    note.pinned.toggle()
                }
            }
            ToolbarItem(placement: .destructiveAction) {
                Button(L10n.actionDelete, systemImage: "trash", role: .destructive) {
                    confirmTrash = true
                }
            }
        }
        .confirmationDialog(L10n.deleteNoteTitle, isPresented: $confirmTrash, titleVisibility: .visible) {
            Button(L10n.actionDelete, role: .destructive) {
                model.trash(note)
                dismiss()
            }
            Button(L10n.actionCancel, role: .cancel) {}
        } message: {
            Text(L10n.deleteNoteBody)
        }
        .onDisappear {
            model.deleteIfBlank(note)
        }
    }
}

struct TrashView: View {
    @Environment(AppModel.self) private var model
    @Query(sort: \NoteRecord.deletedAt, order: .reverse) private var notes: [NoteRecord]
    @State private var pending: NoteRecord?

    private var trashed: [NoteRecord] { notes.filter { $0.deletedAt != nil } }

    var body: some View {
        List {
            if trashed.isEmpty {
                ContentUnavailableView(L10n.trashEmpty, systemImage: "trash", description: Text(L10n.trashSubtitle))
            } else {
                Section {
                    ForEach(trashed) { note in
                        VStack(alignment: .leading, spacing: 4) {
                            Text(note.displayTitle).font(.headline)
                            if let deleted = note.deletedAt {
                                Text(formatNoteDate(deleted))
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        .swipeActions {
                            Button(L10n.restore, systemImage: "arrow.uturn.backward") { model.restore(note) }
                                .tint(.green)
                            Button(L10n.deleteForever, systemImage: "trash", role: .destructive) { pending = note }
                        }
                    }
                } footer: {
                    Text(L10n.trashSubtitle)
                }
            }
        }
        .navigationTitle(L10n.trashTitle)
        .onAppear { model.purgeTrash() }
        .confirmationDialog(L10n.deleteForeverTitle, isPresented: Binding(
            get: { pending != nil },
            set: { if !$0 { pending = nil } }
        ), titleVisibility: .visible) {
            Button(L10n.deleteForever, role: .destructive) {
                if let pending { model.deleteForever(pending) }
                pending = nil
            }
            Button(L10n.actionCancel, role: .cancel) { pending = nil }
        } message: {
            Text(L10n.deleteForeverBody)
        }
    }
}
