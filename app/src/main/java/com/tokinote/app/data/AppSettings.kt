package com.tokinote.app.data

enum class ThemeMode {
    System,
    Light,
    Dark,
}

/** 专注计时器的时长设置，默认值与 Tomato 一致（25 / 5 / 15 / 4）。 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = false,
    val amoled: Boolean = false,
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val sessionsBeforeLongBreak: Int = 4,
    val autoStartNext: Boolean = true,
)

/** 一段已完成的专注记录，用于「今日专注」统计。 */
data class FocusSession(
    val endedAt: Long,
    val minutes: Int,
)
