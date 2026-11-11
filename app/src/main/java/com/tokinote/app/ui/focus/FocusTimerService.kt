package com.tokinote.app.ui.focus

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tokinote.app.TokiNoteApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 前台服务只负责两件事：屏幕关闭时进程不被冻结，以及用系统计时器显示进度。
 * 时间本身仍由 [FocusTimerEngine] 按 elapsedRealtime 计算。
 */
class FocusTimerService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var observeJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val engine = (application as TokiNoteApplication).focusEngine
        val state = engine.state.value
        val notification = if (state.running) {
            FocusNotifications.ongoing(this, state)
        } else {
            FocusNotifications.placeholder(this)
        }
        val promoted = runCatching { promote(notification) }.isSuccess
        if (!promoted || !state.running) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        acquireWakeLock()
        observe(engine)
        return START_STICKY
    }

    override fun onDestroy() {
        observeJob?.cancel()
        serviceScope.cancel()
        releaseWakeLock()
        super.onDestroy()
    }

    private fun promote(notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(FocusNotifications.ONGOING_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(FocusNotifications.ONGOING_ID, notification)
        }
    }

    private fun observe(engine: FocusTimerEngine) {
        if (observeJob?.isActive == true) return
        observeJob = serviceScope.launch {
            launch {
                var lastKey = ""
                engine.state.collect { state ->
                    if (!state.running) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        return@collect
                    }
                    val key = anchorKey(state)
                    if (key == lastKey) return@collect
                    lastKey = key
                    NotificationManagerCompat.from(this@FocusTimerService)
                        .notify(FocusNotifications.ONGOING_ID, FocusNotifications.ongoing(this@FocusTimerService, state))
                }
            }
            launch {
                while (isActive) {
                    if (!engine.state.value.running) break
                    acquireWakeLock()
                    delay(WAKE_REFRESH_MS)
                }
            }
        }
    }

    private fun acquireWakeLock() {
        val lock = wakeLock ?: (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TokiNote:FocusTimer")
            .apply { setReferenceCounted(false) }
            .also { wakeLock = it }
        runCatching { lock.acquire(WAKE_TIMEOUT_MS) }
    }

    private fun releaseWakeLock() {
        val lock = wakeLock ?: return
        runCatching { if (lock.isHeld) lock.release() }
    }

    companion object {
        private const val TAG = "FocusTimerService"
        private const val WAKE_TIMEOUT_MS = 12L * 60L * 60L * 1000L
        private const val WAKE_REFRESH_MS = 60_000L

        fun start(context: Context) {
            val intent = Intent(context, FocusTimerService::class.java)
            runCatching {
                ContextCompat.startForegroundService(context, intent)
            }.onFailure { error ->
                Log.w(TAG, "Unable to start focus timer service", error)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FocusTimerService::class.java))
        }

        private fun anchorKey(state: FocusState): String =
            "${state.phase}|${state.mode}|${state.indefinite}|${state.deadlineElapsed}|${state.runStartedElapsed}|${state.elapsedBaseMillis}"
    }
}
