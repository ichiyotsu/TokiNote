package com.tokinote.app.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 应用唯一的 ViewModel：笔记、标签、设置和专注记录都放在这里，
 * 每次修改立即写入 [TokiStore]。
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val store = TokiStore(application)

    private val _notes = MutableStateFlow(store.loadNotes())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val _labels = MutableStateFlow(store.loadLabels())
    val labels: StateFlow<List<Label>> = _labels.asStateFlow()

    private val _groups = MutableStateFlow(store.loadGroups())
    val groups: StateFlow<List<NoteGroup>> = _groups.asStateFlow()

    private val _folders = MutableStateFlow(store.loadFolders())
    val folders: StateFlow<List<AlbumFolder>> = _folders.asStateFlow()

    private val _settings = MutableStateFlow(store.loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    init {
        // 回收站保留 14 天，启动时清理过期笔记
        val cutoff = System.currentTimeMillis() - 14L * 24 * 60 * 60 * 1000
        val purged = _notes.value.filterNot { it.deletedAt != null && it.deletedAt < cutoff }
        if (purged.size != _notes.value.size) {
            _notes.value = purged
            store.saveNotes(purged)
        }
    }

    private val _sessions = MutableStateFlow(store.loadSessions())
    val sessions: StateFlow<List<FocusSession>> = _sessions.asStateFlow()

    /* ----------------------------- notes ----------------------------- */

    fun createNote(): Note {
        val note = Note(
            id = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis(),
            day = LocalDate.now().toEpochDay(),
            emoji = JournalEmojis.random(),
        )
        updateNotes { it + note }
        return note
    }

    fun note(id: String): Note? = _notes.value.firstOrNull { it.id == id }

    fun updateNote(updated: Note) {
        updateNotes { notes ->
            notes.map { if (it.id == updated.id) updated.copy(updatedAt = System.currentTimeMillis()) else it }
        }
    }

    fun deleteNote(id: String) {
        updateNotes { notes -> notes.filterNot { it.id == id } }
        if (_groups.value.any { id in it.noteIds }) {
            val updated = _groups.value.map { group ->
                if (id in group.noteIds) group.copy(noteIds = group.noteIds - id) else group
            }
            _groups.update { updated }
            store.saveGroups(updated)
        }
    }

    fun archive(id: String) {
        updateNotes { notes -> notes.map { if (it.id == id) it.copy(archived = true, pinned = false) else it } }
    }

    fun unarchive(id: String) {
        updateNotes { notes -> notes.map { if (it.id == id) it.copy(archived = false) else it } }
    }

    fun trash(id: String) {
        updateNotes { notes -> notes.map { if (it.id == id) it.copy(deletedAt = System.currentTimeMillis(), pinned = false) else it } }
    }

    fun restore(id: String) {
        updateNotes { notes -> notes.map { if (it.id == id) it.copy(deletedAt = null) else it } }
    }

    fun deletePermanently(id: String) {
        deleteNote(id)
    }

    fun toggleDone(id: String) {
        updateNotes { notes ->
            notes.map { if (it.id == id) it.copy(done = !it.done) else it }
        }
    }

    /** 删除空白笔记，避免用户点开编辑又退出时留下空记录。 */
    fun deleteIfBlank(id: String) {
        val note = note(id) ?: return
        if (note.isBlank) deleteNote(id)
    }

    /* ----------------------------- labels ----------------------------- */

    fun addLabel(name: String): Label? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        if (_labels.value.any { it.name.equals(trimmed, ignoreCase = true) }) return null
        val label = Label(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            colorIndex = _labels.value.size % 8,
        )
        val updated = _labels.value + label
        _labels.update { updated }
        store.saveLabels(updated)
        return label
    }

    fun deleteLabel(id: String) {
        val updated = _labels.value.filterNot { it.id == id }
        _labels.update { updated }
        store.saveLabels(updated)
        updateNotes { notes ->
            notes.map { if (id in it.labelIds) it.copy(labelIds = it.labelIds - id) else it }
        }
    }

    fun setNoteLabels(noteId: String, labelIds: Set<String>) {
        updateNotes { notes ->
            notes.map { if (it.id == noteId) it.copy(labelIds = labelIds, updatedAt = System.currentTimeMillis()) else it }
        }
    }

    /* ----------------------------- groups ----------------------------- */

    fun createGroup(name: String, noteIds: Set<String>): NoteGroup? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        if (_groups.value.any { it.name.equals(trimmed, ignoreCase = true) }) return null
        val group = NoteGroup(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            noteIds = noteIds,
        )
        val updated = _groups.value + group
        _groups.update { updated }
        store.saveGroups(updated)
        return group
    }

    fun updateGroup(id: String, name: String, noteIds: Set<String>): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        if (_groups.value.any { it.id != id && it.name.equals(trimmed, ignoreCase = true) }) return false
        val updated = _groups.value.map { group ->
            if (group.id == id) group.copy(name = trimmed, noteIds = noteIds) else group
        }
        _groups.update { updated }
        store.saveGroups(updated)
        return true
    }

    fun deleteGroup(id: String) {
        val updated = _groups.value.filterNot { it.id == id }
        _groups.update { updated }
        store.saveGroups(updated)
    }

    /* ----------------------------- album folders ----------------------------- */

    fun createFolder(name: String, paths: Set<String>): AlbumFolder? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        if (_folders.value.any { it.name.equals(trimmed, ignoreCase = true) }) return null
        val folder = AlbumFolder(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            paths = paths,
        )
        val updated = _folders.value + folder
        _folders.update { updated }
        store.saveFolders(updated)
        return folder
    }

    fun updateFolder(id: String, name: String, paths: Set<String>): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        if (_folders.value.any { it.id != id && it.name.equals(trimmed, ignoreCase = true) }) return false
        val updated = _folders.value.map { folder ->
            if (folder.id == id) folder.copy(name = trimmed, paths = paths) else folder
        }
        _folders.update { updated }
        store.saveFolders(updated)
        return true
    }

    fun deleteFolder(id: String) {
        val updated = _folders.value.filterNot { it.id == id }
        _folders.update { updated }
        store.saveFolders(updated)
    }

    /* ----------------------------- settings ----------------------------- */

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val updated = transform(_settings.value)
        _settings.update { updated }
        store.saveSettings(updated)
    }

    /* ----------------------------- focus ----------------------------- */

    fun recordFocusSession(minutes: Int) {
        if (minutes <= 0) return
        val updated = _sessions.value + FocusSession(System.currentTimeMillis(), minutes)
        _sessions.update { updated }
        store.saveSessions(updated)
    }

    fun recordBreakSession(minutes: Int) {
        if (minutes <= 0) return
        val updated = _sessions.value + FocusSession(System.currentTimeMillis(), 0, minutes)
        _sessions.update { updated }
        store.saveSessions(updated)
    }

    /* ----------------------------- data ----------------------------- */

    fun exportJson(): String = store.exportJson(
        _notes.value,
        _labels.value,
        _groups.value,
        _folders.value,
        _settings.value,
    )

    fun clearAll() {
        store.clearAll()
        _notes.update { emptyList() }
        _labels.update { emptyList() }
        _groups.update { emptyList() }
        _folders.update { emptyList() }
        _sessions.update { emptyList() }
        _settings.update { AppSettings() }
    }

    private fun updateNotes(transform: (List<Note>) -> List<Note>) {
        val updated = transform(_notes.value)
        _notes.update { updated }
        store.saveNotes(updated)
    }
}
