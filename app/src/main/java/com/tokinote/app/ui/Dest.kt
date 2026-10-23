package com.tokinote.app.ui

/** 应用内的目标页面。 */
sealed interface Dest {
    data object Notes : Dest
    data object Focus : Dest
    data object Settings : Dest
    data class Editor(val noteId: String) : Dest
    data class SettingsDetail(val page: SettingsPage) : Dest
    data object Calendar : Dest
    data class Preview(val path: String) : Dest
    data class MediaViewer(val noteId: String, val index: Int) : Dest
    data class LocationPicker(val noteId: String) : Dest
}

enum class SettingsPage {
    Appearance,
    Timer,
    Data,
    About,
}

fun Dest.isTopLevel(): Boolean = this is Dest.Notes || this is Dest.Focus || this is Dest.Settings

/** 用于把返回栈保存到 SavedState，进程被回收后也能恢复。 */
fun Dest.encode(): String = when (this) {
    Dest.Notes -> "notes"
    Dest.Focus -> "focus"
    Dest.Settings -> "settings"
    is Dest.Editor -> "editor:$noteId"
    is Dest.SettingsDetail -> "settings:${page.name}"
    Dest.Calendar -> "calendar"
    is Dest.Preview -> "preview:$path"
    is Dest.MediaViewer -> "media:$noteId:$index"
    is Dest.LocationPicker -> "location:$noteId"
}

fun decodeDest(value: String): Dest? = when {
    value == "notes" -> Dest.Notes
    value == "focus" -> Dest.Focus
    value == "settings" -> Dest.Settings
    value == "calendar" -> Dest.Calendar
    value.startsWith("editor:") -> value.removePrefix("editor:").takeIf { it.isNotEmpty() }?.let(Dest::Editor)
    value.startsWith("settings:") -> runCatching {
        Dest.SettingsDetail(SettingsPage.valueOf(value.removePrefix("settings:")))
    }.getOrNull()

    value.startsWith("preview:") -> Dest.Preview(value.removePrefix("preview:"))
    value.startsWith("media:") -> {
        val rest = value.removePrefix("media:")
        val index = rest.substringAfterLast(':').toIntOrNull()
        val noteId = rest.substringBeforeLast(':', "")
        if (index != null && noteId.isNotEmpty()) Dest.MediaViewer(noteId, index) else null
    }

    value.startsWith("location:") -> value.removePrefix("location:").takeIf { it.isNotEmpty() }?.let(Dest::LocationPicker)
    else -> null
}
