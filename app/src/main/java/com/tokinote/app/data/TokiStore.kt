package com.tokinote.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 本地存储：SharedPreferences + JSON。
 *
 * 笔记、标签、设置和专注记录全部写在这里，不依赖网络，也没有云端同步，
 * 所以卸载应用或清除数据之前它们都会一直存在。
 */
class TokiStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /* ----------------------------- notes ----------------------------- */

    fun loadNotes(): List<Note> {
        val raw = prefs.getString(KEY_NOTES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    add(array.getJSONObject(index).toNote())
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveNotes(notes: List<Note>) {
        val array = JSONArray()
        notes.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_NOTES, array.toString()).apply()
    }

    /* ----------------------------- labels ----------------------------- */

    fun loadLabels(): List<Label> {
        val raw = prefs.getString(KEY_LABELS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.getJSONObject(index)
                    add(
                        Label(
                            id = json.getString("id"),
                            name = json.getString("name"),
                            colorIndex = json.optInt("colorIndex", 0),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveLabels(labels: List<Label>) {
        val array = JSONArray()
        labels.forEach { label ->
            array.put(
                JSONObject().apply {
                    put("id", label.id)
                    put("name", label.name)
                    put("colorIndex", label.colorIndex)
                },
            )
        }
        prefs.edit().putString(KEY_LABELS, array.toString()).apply()
    }

    /* ----------------------------- groups ----------------------------- */

    fun loadGroups(): List<NoteGroup> {
        val raw = prefs.getString(KEY_GROUPS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.getJSONObject(index)
                    add(
                        NoteGroup(
                            id = json.getString("id"),
                            name = json.getString("name"),
                            noteIds = buildSet {
                                val ids = json.optJSONArray("noteIds") ?: JSONArray()
                                for (i in 0 until ids.length()) add(ids.getString(i))
                            },
                            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveGroups(groups: List<NoteGroup>) {
        val array = JSONArray()
        groups.forEach { group ->
            array.put(
                JSONObject().apply {
                    put("id", group.id)
                    put("name", group.name)
                    put("noteIds", JSONArray(group.noteIds.sorted()))
                    put("createdAt", group.createdAt)
                },
            )
        }
        prefs.edit().putString(KEY_GROUPS, array.toString()).apply()
    }

    /* ----------------------------- album folders ----------------------------- */

    fun loadFolders(): List<AlbumFolder> {
        val raw = prefs.getString(KEY_FOLDERS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.getJSONObject(index)
                    add(
                        AlbumFolder(
                            id = json.getString("id"),
                            name = json.getString("name"),
                            paths = buildSet {
                                val paths = json.optJSONArray("paths") ?: JSONArray()
                                for (i in 0 until paths.length()) add(paths.getString(i))
                            },
                            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveFolders(folders: List<AlbumFolder>) {
        val array = JSONArray()
        folders.forEach { folder ->
            array.put(
                JSONObject().apply {
                    put("id", folder.id)
                    put("name", folder.name)
                    put("paths", JSONArray(folder.paths.sorted()))
                    put("createdAt", folder.createdAt)
                },
            )
        }
        prefs.edit().putString(KEY_FOLDERS, array.toString()).apply()
    }

    /* ----------------------------- settings ----------------------------- */

    fun loadSettings(): AppSettings {
        val raw = prefs.getString(KEY_SETTINGS, null) ?: return AppSettings()
        return runCatching {
            val json = JSONObject(raw)
            AppSettings(
                themeMode = runCatching {
                    ThemeMode.valueOf(json.optString("themeMode", ThemeMode.System.name))
                }.getOrDefault(ThemeMode.System),
                designStyle = runCatching {
                    DesignStyle.valueOf(json.optString("designStyle", DesignStyle.MaterialExpressive.name))
                }.getOrDefault(DesignStyle.MaterialExpressive),
                dynamicColor = json.optBoolean("dynamicColor", true),
                colorPreset = runCatching {
                    ColorPreset.valueOf(json.optString("colorPreset", ColorPreset.Matcha.name))
                }.getOrDefault(ColorPreset.Matcha),
                amoled = json.optBoolean("amoled", false),
                focusMinutes = json.optInt("focusMinutes", 25).coerceIn(1, 180),
                shortBreakMinutes = json.optInt("shortBreakMinutes", 5).coerceIn(1, 60),
                longBreakMinutes = json.optInt("longBreakMinutes", 15).coerceIn(1, 90),
                sessionsBeforeLongBreak = json.optInt("sessionsBeforeLongBreak", 4).coerceIn(1, 12),
                autoStartNext = json.optBoolean("autoStartNext", true),
                showTime = json.optBoolean("showTime", true),
                reduceMotion = json.optBoolean("reduceMotion", false),
                updateChannel = json.optString("updateChannel", "release"),
                userId = json.optString("userId", "tokinote"),
                avatarEmoji = json.optString("avatarEmoji", "🌿"),
                userName = json.optString("userName", "ichiyo tokisaki"),
                userEmail = json.optString("userEmail", "ichiyo@tokinote.app"),
                avatarPath = json.optString("avatarPath", ""),
                focusComponentEnabled = json.optBoolean("focusComponentEnabled", false),
            )
        }.getOrDefault(AppSettings())
    }

    fun saveSettings(settings: AppSettings) {
        val json = JSONObject().apply {
            put("themeMode", settings.themeMode.name)
            put("designStyle", settings.designStyle.name)
            put("dynamicColor", settings.dynamicColor)
            put("colorPreset", settings.colorPreset.name)
            put("amoled", settings.amoled)
            put("focusMinutes", settings.focusMinutes)
            put("shortBreakMinutes", settings.shortBreakMinutes)
            put("longBreakMinutes", settings.longBreakMinutes)
            put("sessionsBeforeLongBreak", settings.sessionsBeforeLongBreak)
            put("autoStartNext", settings.autoStartNext)
            put("showTime", settings.showTime)
            put("reduceMotion", settings.reduceMotion)
            put("updateChannel", settings.updateChannel)
            put("userId", settings.userId)
            put("avatarEmoji", settings.avatarEmoji)
            put("userName", settings.userName)
            put("userEmail", settings.userEmail)
            put("avatarPath", settings.avatarPath)
            put("focusComponentEnabled", settings.focusComponentEnabled)
        }
        prefs.edit().putString(KEY_SETTINGS, json.toString()).apply()
    }

    /* ----------------------------- sessions ----------------------------- */

    fun loadSessions(): List<FocusSession> {
        val raw = prefs.getString(KEY_SESSIONS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.getJSONObject(index)
                    add(
                        FocusSession(
                            endedAt = json.optLong("endedAt", 0L),
                            minutes = json.optInt("minutes", 0),
                            breakMinutes = json.optInt("breakMinutes", 0),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveSessions(sessions: List<FocusSession>) {
        val array = JSONArray()
        sessions.forEach { session ->
            array.put(
                JSONObject().apply {
                    put("endedAt", session.endedAt)
                    put("minutes", session.minutes)
                    put("breakMinutes", session.breakMinutes)
                },
            )
        }
        prefs.edit().putString(KEY_SESSIONS, array.toString()).apply()
    }

    /* ----------------------------- backup ----------------------------- */

    fun exportJson(
        notes: List<Note>,
        labels: List<Label>,
        groups: List<NoteGroup>,
        folders: List<AlbumFolder>,
        settings: AppSettings,
    ): String {
        val notesArray = JSONArray().apply { notes.forEach { put(it.toJson()) } }
        val labelsArray = JSONArray().apply {
            labels.forEach { label ->
                put(
                    JSONObject().apply {
                        put("id", label.id)
                        put("name", label.name)
                        put("colorIndex", label.colorIndex)
                    },
                )
            }
        }
        val groupsArray = JSONArray().apply {
            groups.forEach { group ->
                put(
                    JSONObject().apply {
                        put("id", group.id)
                        put("name", group.name)
                        put("noteIds", JSONArray(group.noteIds.sorted()))
                        put("createdAt", group.createdAt)
                    },
                )
            }
        }
        val foldersArray = JSONArray().apply {
            folders.forEach { folder ->
                put(
                    JSONObject().apply {
                        put("id", folder.id)
                        put("name", folder.name)
                        put("paths", JSONArray(folder.paths.sorted()))
                        put("createdAt", folder.createdAt)
                    },
                )
            }
        }
        return JSONObject().apply {
            put("app", "TokiNote")
            put("version", 3)
            put("exportedAt", System.currentTimeMillis())
            put("notes", notesArray)
            put("labels", labelsArray)
            put("groups", groupsArray)
            put("folders", foldersArray)
            put("settings", JSONObject().apply {
                put("themeMode", settings.themeMode.name)
                put("colorPreset", settings.colorPreset.name)
                put("focusMinutes", settings.focusMinutes)
                put("shortBreakMinutes", settings.shortBreakMinutes)
                put("longBreakMinutes", settings.longBreakMinutes)
                put("sessionsBeforeLongBreak", settings.sessionsBeforeLongBreak)
                put("autoStartNext", settings.autoStartNext)
                put("showTime", settings.showTime)
                put("reduceMotion", settings.reduceMotion)
                put("updateChannel", settings.updateChannel)
                put("userId", settings.userId)
                put("avatarEmoji", settings.avatarEmoji)
                put("userName", settings.userName)
                put("userEmail", settings.userEmail)
                put("avatarPath", settings.avatarPath)
            })
        }.toString(2)
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    /* ----------------------------- mapping ----------------------------- */

    private fun Note.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("body", body)
        put("emoji", emoji)
        put("colorIndex", colorIndex)
        put("pinned", pinned)
        put("done", done)
        put("archived", archived)
        put("deletedAt", deletedAt)
        put(
            "checklist",
            JSONArray().apply {
                checklist.forEach { item ->
                    put(JSONObject().apply { put("text", item.text); put("done", item.done) })
                }
            },
        )
        put("labelIds", JSONArray(labelIds.sorted()))
        put("titleSize", titleSize.name)
        put("bodySize", bodySize.name)
        put("bodyWeight", bodyWeight.name)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
        put("day", day)
        put("media", JSONArray(media))
        put(
            "files",
            JSONArray().apply {
                files.forEach { file ->
                    put(
                        JSONObject().apply {
                            put("name", file.name)
                            put("path", file.path)
                            put("mime", file.mime)
                            put("size", file.size)
                        },
                    )
                }
            },
        )
        put(
            "location",
            location?.let { loc ->
                JSONObject().apply {
                    put("lat", loc.lat)
                    put("lng", loc.lng)
                    put("name", loc.name)
                }
            },
        )
    }

    private fun JSONObject.toNote(): Note = Note(
        id = getString("id"),
        title = optString("title", ""),
        body = optString("body", ""),
        emoji = optString("emoji", "😀"),
        colorIndex = optInt("colorIndex", 0).coerceIn(0, 7),
        pinned = optBoolean("pinned", false),
        done = optBoolean("done", false),
        archived = optBoolean("archived", false),
        deletedAt = if (has("deletedAt")) optLong("deletedAt", 0L) else null,
        checklist = buildList {
            val array = optJSONArray("checklist") ?: JSONArray()
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(ChecklistItem(item.optString("text", ""), item.optBoolean("done", false)))
            }
        },
        labelIds = buildSet {
            val ids = optJSONArray("labelIds") ?: JSONArray()
            for (index in 0 until ids.length()) add(ids.getString(index))
        },
        titleSize = enumOrDefault(optString("titleSize"), TextSize.Medium),
        bodySize = enumOrDefault(optString("bodySize"), TextSize.Medium),
        bodyWeight = enumOrDefault(optString("bodyWeight"), TextWeight.Normal),
        createdAt = optLong("createdAt", System.currentTimeMillis()),
        updatedAt = optLong("updatedAt", System.currentTimeMillis()),
        day = optLong("day", 0L),
        media = buildList {
            val array = optJSONArray("media") ?: JSONArray()
            for (index in 0 until array.length()) add(array.getString(index))
        },
        files = buildList {
            val array = optJSONArray("files") ?: JSONArray()
            for (index in 0 until array.length()) {
                val json = array.getJSONObject(index)
                add(
                    JournalFile(
                        name = json.optString("name", "文件"),
                        path = json.optString("path", ""),
                        mime = json.optString("mime", ""),
                        size = json.optLong("size", 0L),
                    ),
                )
            }
        },
        location = optJSONObject("location")?.let { json ->
            JournalLocation(
                lat = json.optDouble("lat", 0.0),
                lng = json.optDouble("lng", 0.0),
                name = json.optString("name", "位置"),
            )
        },
    )

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, fallback: T): T =
        runCatching { enumValueOf<T>(name.orEmpty()) }.getOrDefault(fallback)

    private companion object {
        const val PREFS_NAME = "tokinote_prefs"
        const val KEY_NOTES = "notes"
        const val KEY_LABELS = "labels"
        const val KEY_GROUPS = "groups"
        const val KEY_FOLDERS = "folders"
        const val KEY_SETTINGS = "settings"
        const val KEY_SESSIONS = "sessions"
    }
}
