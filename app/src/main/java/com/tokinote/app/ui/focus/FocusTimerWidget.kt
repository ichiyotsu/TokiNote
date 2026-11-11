package com.tokinote.app.ui.focus

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.tokinote.app.MainActivity
import com.tokinote.app.R
import com.tokinote.app.TokiNoteApplication
import com.tokinote.app.ui.formatElapsed

/**
 * 桌面小组件。进行中的倒计时交给系统 Chronometer，进程被杀掉后数字仍按真实时间走；
 * 暂停、完成和模式切换时由引擎把新的 RemoteViews 推过来。
 */
class FocusTimerWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val state = (context.applicationContext as TokiNoteApplication).focusEngine.state.value
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, FocusWidgetUpdater.views(context, state))
        }
    }
}

object FocusWidgetUpdater {
    fun updateAll(context: Context, state: FocusState) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, FocusTimerWidget::class.java))
        if (ids.isEmpty()) return
        val views = views(context, state)
        ids.forEach { id -> manager.updateAppWidget(id, views) }
    }

    fun views(context: Context, state: FocusState): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_focus)
        val title = when {
            state.indefinite -> context.getString(R.string.focus_indefinite)
            state.mode == FocusMode.Focus -> context.getString(R.string.focus_title)
            state.mode == FocusMode.ShortBreak -> context.getString(R.string.focus_short_break)
            else -> context.getString(R.string.focus_long_break)
        }
        views.setTextViewText(R.id.widget_mode, title)
        val now = SystemClock.elapsedRealtime()
        if (state.running && !state.indefinite && state.deadlineElapsed > 0L) {
            val remaining = (state.deadlineElapsed - now).coerceAtLeast(0L)
            views.setViewVisibility(R.id.widget_clock, View.VISIBLE)
            views.setViewVisibility(R.id.widget_frozen, View.GONE)
            views.setChronometer(R.id.widget_clock, now + remaining, null, true)
            views.setChronometerCountDown(R.id.widget_clock, true)
        } else if (state.running && state.indefinite) {
            val elapsed = state.elapsedBaseMillis + (now - state.runStartedElapsed).coerceAtLeast(0L)
            views.setViewVisibility(R.id.widget_clock, View.VISIBLE)
            views.setViewVisibility(R.id.widget_frozen, View.GONE)
            views.setChronometer(R.id.widget_clock, now - elapsed, null, true)
            views.setChronometerCountDown(R.id.widget_clock, false)
        } else {
            val seconds = if (state.indefinite) state.elapsedSeconds else state.remainingSeconds
            views.setViewVisibility(R.id.widget_clock, View.GONE)
            views.setViewVisibility(R.id.widget_frozen, View.VISIBLE)
            views.setTextViewText(R.id.widget_frozen, formatElapsed(seconds))
        }
        views.setTextViewText(
            R.id.widget_toggle,
            context.getString(if (state.running) R.string.focus_pause else R.string.focus_play),
        )
        views.setOnClickPendingIntent(R.id.widget_toggle, FocusTimerReceiver.pending(context, FocusTimerReceiver.ACTION_TOGGLE))
        views.setOnClickPendingIntent(R.id.widget_root, openApp(context))
        return views
    }

    private fun openApp(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
