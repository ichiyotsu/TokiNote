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

/** 整体设计语言；MD3E 为现有 Expressive，OneUI 为三星 One UI 风格。 */
enum class DesignStyle {
    MaterialExpressive,
    OneUI,
}

/** 字体预设；Auto 跟随当前设计风格。 */
enum class AppFont {
    Auto,
    Samsung,
    Md3e,
    PingFang,
    Custom,
}

/** 发布开关：0.3.5 固定 MD3E，设计风格入口暂不可用。 */
object TokiFeatures {
    const val DESIGN_STYLE_SWITCH_ENABLED = false
}

/** 用户导入的自定义字体。 */
data class CustomFont(
    val name: String,
    val path: String,
)

/** 专注计时器的时长设置，默认值与 Tomato 一致（25 / 5 / 15 / 4）。 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val designStyle: DesignStyle = DesignStyle.MaterialExpressive,
    val font: AppFont = AppFont.Auto,
    val customFontPath: String = "",
    val customFonts: List<CustomFont> = emptyList(),
    /** SAF 授权的 Google Drive / OneDrive 同步文件夹。 */
    val syncFolderUri: String = "",
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
    /** 个人资料：头像 emoji、用户 ID、名称、邮箱与相册头像路径。 */
    val userId: String = "tokinote",
    val avatarEmoji: String = "🌿",
    val userName: String = "ichiyo tokisaki",
    val userEmail: String = "ichiyo@tokinote.app",
    val avatarPath: String = "",
    /** 可选的专注组件是否启用；组件未安装时始终视为未启用。 */
    val focusComponentEnabled: Boolean = false,
)

/** 一段已完成的专注记录，用于统计。 */
data class FocusSession(
    val endedAt: Long,
    val minutes: Int,
    val breakMinutes: Int = 0,
)
