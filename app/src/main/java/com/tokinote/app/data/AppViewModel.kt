package com.tokinote.app.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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

    private val _settings = MutableStateFlow(store.loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

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
    }

    fun toggleDone(id: String) {
        updateNotes { notes ->
            notes.map { if (it.id == id) it.copy(done = !it.done) else it }
        }
    }

    fun togglePin(id: String) {
        updateNotes { notes ->
            notes.map { if (it.id == id) it.copy(pinned = !it.pinned, updatedAt = System.currentTimeMillis()) else it }
        }
    }

    fun setEmoji(noteId: String, emoji: String) {
        updateNotes { notes ->
            notes.map { if (it.id == noteId) it.copy(emoji = emoji, updatedAt = System.currentTimeMillis()) else it }
        }
    }

    fun addMedia(noteId: String, paths: List<String>) {
        if (paths.isEmpty()) return
        updateNotes { notes ->
            notes.map {
                if (it.id == noteId) {
                    it.copy(media = it.media + paths, updatedAt = System.currentTimeMillis())
                } else {
                    it
                }
            }
        }
    }

    fun removeMedia(noteId: String, path: String) {
        updateNotes { notes ->
            notes.map {
                if (it.id == noteId) {
                    it.copy(media = it.media - path, updatedAt = System.currentTimeMillis())
                } else {
                    it
                }
            }
        }
    }

    fun addFiles(noteId: String, files: List<JournalFile>) {
        if (files.isEmpty()) return
        updateNotes { notes ->
            notes.map {
                if (it.id == noteId) {
                    it.copy(files = it.files + files, updatedAt = System.currentTimeMillis())
                } else {
                    it
                }
            }
        }
    }

    fun removeFile(noteId: String, path: String) {
        updateNotes { notes ->
            notes.map {
                if (it.id == noteId) {
                    it.copy(files = it.files.filterNot { file -> file.path == path }, updatedAt = System.currentTimeMillis())
                } else {
                    it
                }
            }
        }
    }

    fun setLocation(noteId: String, location: JournalLocation?) {
        updateNotes { notes ->
            notes.map {
                if (it.id == noteId) {
                    it.copy(location = location, updatedAt = System.currentTimeMillis())
                } else {
                    it
                }
            }
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

    /** 今天的专注分钟数。 */
    fun todayFocusMinutes(): Int {
        val today = LocalDate.now()
        return _sessions.value.sumOf { session ->
            val date = Instant.ofEpochMilli(session.endedAt).atZone(ZoneId.systemDefault()).toLocalDate()
            if (date == today) session.minutes else 0
        }
    }

    /* ----------------------------- data ----------------------------- */

    fun exportJson(): String = store.exportJson(_notes.value, _labels.value, _settings.value)

    fun clearAll() {
        store.clearAll()
        _notes.update { emptyList() }
        _labels.update { emptyList() }
        _sessions.update { emptyList() }
        _settings.update { AppSettings() }
    }

    private fun updateNotes(transform: (List<Note>) -> List<Note>) {
        val updated = transform(_notes.value)
        _notes.update { updated }
        store.saveNotes(updated)
    }
}
