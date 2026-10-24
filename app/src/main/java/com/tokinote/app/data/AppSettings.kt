package com.tokinote.app.data

enum class ThemeMode {
    System,
    Light,
    Dark,
}

/** 动态取色之外的几套系统预设配色。 */
enum class ColorPreset {
    Matcha,
    Blue,
    Purple,
    Orange,
}

/** 专注计时器的时长设置，默认值与 Tomato 一致（25 / 5 / 15 / 4）。 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = true,
    val amoled: Boolean = false,
    val colorPreset: ColorPreset = ColorPreset.Matcha,
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val sessionsBeforeLongBreak: Int = 4,
    val autoStartNext: Boolean = true,
    /** 列表卡片上是否显示具体时间。 */
    val showTime: Boolean = true,
    /** 减弱动效：关闭页面转场、按压回弹等动画。 */
    val reduceMotion: Boolean = false,
    /** release 或 beta：决定检查更新时拉取的通道。 */
    val updateChannel: String = "release",
    /** 个人资料：头像 emoji 与用户 ID。 */
    val userId: String = "tokinote",
    val avatarEmoji: String = "🌿",
)

/** 一段已完成的专注记录，用于统计。 */
data class FocusSession(
    val endedAt: Long,
    val minutes: Int,
    val breakMinutes: Int = 0,
)
