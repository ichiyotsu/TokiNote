package com.tokinote.app.ui.settings

import android.content.Context
import java.io.File

/**
 * Keeps a downloaded update for the next app start. The system installer still
 * owns the actual install confirmation; this only decides when it is opened.
 */
object PendingUpdate {
    private const val PREFS = "pending_update"
    private const val KEY_PATH = "apk_path"

    fun schedule(context: Context, path: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PATH, path)
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PATH)
            .apply()
    }

    /** Opens the installer once on a later launch. Returns true if it was launched. */
    fun installIfPending(context: Context): Boolean {
        val path = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PATH, null)
            ?: return false
        val file = File(path)
        if (!file.exists()) {
            clear(context)
            return false
        }
        if (!ApkInstaller.canInstall(context)) return false
        val installed = ApkInstaller.install(context, file)
        if (installed) clear(context)
        return installed
    }
}
