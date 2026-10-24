package com.tokinote.app.ui

/** 应用内的目标页面。 */
sealed interface Dest {
    data object Notes : Dest
    data object Focus : Dest
    data object Settings : Dest
    data class Editor(val noteId: String) : Dest
    data class SettingsDetail(val page: SettingsPage) : Dest
    data object Calendar : Dest
    data object FocusSummary : Dest
    data object Timer : Dest
    data object Archive : Dest
    data object Trash : Dest
    data class MediaViewer(val noteId: String, val index: Int) : Dest
}

enum class SettingsPage {
    Appearance,
    Data,
    About,
}

fun Dest.isTopLevel(): Boolean = this is Dest.Notes || this is Dest.Focus || this is Dest.Settings

/** 用于判断转场方向：0 是一级页面，数字越大越深。 */
fun Dest.depth(): Int = when (this) {
    Dest.Notes, Dest.Focus, Dest.Settings -> 0
    else -> 1
}

/** 用于把返回栈保存到 SavedState，进程被回收后也能恢复。 */
fun Dest.encode(): String = when (this) {
    Dest.Notes -> "notes"
    Dest.Focus -> "focus"
    Dest.Settings -> "settings"
    is Dest.Editor -> "editor:$noteId"
    is Dest.SettingsDetail -> "settings:${page.name}"
    Dest.Calendar -> "calendar"
    Dest.FocusSummary -> "focus-summary"
    Dest.Timer -> "timer"
    Dest.Archive -> "archive"
    Dest.Trash -> "trash"
    is Dest.MediaViewer -> "media:$noteId:$index"
}

fun decodeDest(value: String): Dest? = when {
    value == "notes" -> Dest.Notes
    value == "focus" -> Dest.Focus
    value == "settings" -> Dest.Settings
    value == "calendar" -> Dest.Calendar
    value == "focus-summary" -> Dest.FocusSummary
    value == "timer" -> Dest.Timer
    value == "archive" -> Dest.Archive
    value == "trash" -> Dest.Trash
    value.startsWith("editor:") -> value.removePrefix("editor:").takeIf { it.isNotEmpty() }?.let(Dest::Editor)
    value.startsWith("settings:") -> runCatching {
        Dest.SettingsDetail(SettingsPage.valueOf(value.removePrefix("settings:")))
    }.getOrNull()

    value.startsWith("media:") -> {
        val rest = value.removePrefix("media:")
        val index = rest.substringAfterLast(':').toIntOrNull()
        val noteId = rest.substringBeforeLast(':', "")
        if (index != null && noteId.isNotEmpty()) Dest.MediaViewer(noteId, index) else null
    }

    else -> null
}
