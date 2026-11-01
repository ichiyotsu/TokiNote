package com.tokinote.app.ui.focus

import android.content.Context

interface FocusTimerPersistence {
    fun load(): FocusTimerSnapshot?
    fun save(snapshot: FocusTimerSnapshot)
    fun clear()
}

/**
 * 进程被系统杀掉后仍能恢复截止时间。和笔记数据分开存，避免动到现有 schema。
 * 用 [commit] 而不是 apply，避免写完立刻被杀时丢锚点。
 */
class SharedPrefsFocusTimerPersistence(context: Context) : FocusTimerPersistence {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun load(): FocusTimerSnapshot? {
        val raw = prefs.getString(KEY, null) ?: return null
        return FocusTimerSnapshot.decode(raw)
    }

    override fun save(snapshot: FocusTimerSnapshot) {
        prefs.edit().putString(KEY, snapshot.encode()).commit()
    }

    override fun clear() {
        prefs.edit().remove(KEY).commit()
    }

    private companion object {
        const val PREFS = "focus_timer"
        const val KEY = "snapshot"
    }
}
