package com.tokinote.app.ui.focus

/**
 * 计时用的两套时钟。
 *
 * [elapsedRealtime] 包含系统休眠，不受用户改时间影响，用来计算倒计时。
 * [wallClockMillis] 只在重启后 elapsedRealtime 归零时，把截止点换算回墙钟。
 */
interface FocusClock {
    fun elapsedRealtime(): Long
    fun wallClockMillis(): Long
}

object SystemFocusClock : FocusClock {
    override fun elapsedRealtime(): Long = android.os.SystemClock.elapsedRealtime()
    override fun wallClockMillis(): Long = System.currentTimeMillis()
}
