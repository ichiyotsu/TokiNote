package com.tokinote.app.ui.focus

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tokinote.app.MainActivity
import com.tokinote.app.R
import com.tokinote.app.ui.formatMinutes

internal object FocusUiVisibility {
    @Volatile
    var started: Boolean = false
}

object FocusNotifications {
    const val CHANNEL_RUNNING = "focus_running"
    const val CHANNEL_DONE = "focus_done"
    const val ONGOING_ID = 42
    const val DONE_ID = 43

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val running = NotificationChannel(
            CHANNEL_RUNNING,
            context.getString(R.string.focus_notif_channel),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.focus_notif_channel_desc)
            setShowBadge(false)
        }
        val done = NotificationChannel(
            CHANNEL_DONE,
            context.getString(R.string.focus_notif_done_channel),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.focus_notif_done_channel_desc)
        }
        manager.createNotificationChannel(running)
        manager.createNotificationChannel(done)
    }

    fun ongoing(context: Context, state: FocusState): android.app.Notification {
        ensureChannels(context)
        val nowElapsed = SystemClock.elapsedRealtime()
        val nowWall = System.currentTimeMillis()
        val countingUp = state.indefinite
        val whenMillis = if (countingUp && state.running) {
            val elapsed = state.elapsedBaseMillis + (nowElapsed - state.runStartedElapsed).coerceAtLeast(0L)
            nowWall - elapsed
        } else if (state.running && state.deadlineElapsed > 0L) {
            nowWall + (state.deadlineElapsed - nowElapsed).coerceAtLeast(0L)
        } else {
            nowWall + state.remainingMillis.coerceAtLeast(0L)
        }
        val title = when {
            state.indefinite -> context.getString(R.string.focus_notif_indefinite)
            state.mode == FocusMode.Focus -> context.getString(R.string.focus_notif_running_focus)
            state.mode == FocusMode.ShortBreak -> context.getString(R.string.focus_notif_running_short)
            else -> context.getString(R.string.focus_notif_running_long)
        }
        val body = if (state.indefinite) {
            context.getString(R.string.focus_indefinite_hint)
        } else {
            context.getString(
                R.string.focus_session,
                state.sessionIndex,
                state.settings.sessionsBeforeLongBreak.coerceAtLeast(1),
            )
        }
        return NotificationCompat.Builder(context, CHANNEL_RUNNING)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(title)
            .setContentText(body)
            .setWhen(whenMillis)
            .setUsesChronometer(true)
            .setChronometerCountDown(!countingUp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openAppIntent(context))
            .addAction(
                R.drawable.ic_stat_timer,
                context.getString(R.string.focus_pause),
                FocusTimerReceiver.pending(context, FocusTimerReceiver.ACTION_PAUSE),
            )
            .addAction(
                R.drawable.ic_stat_timer,
                context.getString(R.string.focus_reset),
                FocusTimerReceiver.pending(context, FocusTimerReceiver.ACTION_RESET),
            )
            .build()
    }

    fun placeholder(context: Context): android.app.Notification {
        ensureChannels(context)
        return NotificationCompat.Builder(context, CHANNEL_RUNNING)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(context.getString(R.string.focus_title))
            .setSilent(true)
            .setOngoing(true)
            .build()
    }

    fun showCompleted(context: Context, event: FocusTimerEvent) {
        if (FocusUiVisibility.started) return
        ensureChannels(context)
        val focus = event is FocusTimerEvent.FocusCompleted
        val title = context.getString(
            if (focus) R.string.focus_notif_done_focus else R.string.focus_notif_done_break,
        )
        val body = context.getString(
            if (focus) R.string.focus_notif_done_focus_body else R.string.focus_notif_done_break_body,
            formatMinutes(event.minutes),
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_DONE)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAppIntent(context))
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(DONE_ID, notification)
        }
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
