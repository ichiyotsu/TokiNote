package com.tokinote.app.ui.focus

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tokinote.app.TokiNoteApplication

class FocusTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val engine = (context.applicationContext as TokiNoteApplication).focusEngine
        when (intent?.action) {
            ACTION_PAUSE -> engine.pause()
            ACTION_TOGGLE -> engine.toggle()
            ACTION_RESET -> engine.reset()
        }
    }

    companion object {
        const val ACTION_PAUSE = "com.tokinote.app.focus.PAUSE"
        const val ACTION_TOGGLE = "com.tokinote.app.focus.TOGGLE"
        const val ACTION_RESET = "com.tokinote.app.focus.RESET"

        fun pending(context: Context, action: String): PendingIntent {
            val intent = Intent(context, FocusTimerReceiver::class.java).setAction(action)
            return PendingIntent.getBroadcast(
                context,
                action.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
