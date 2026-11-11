import Foundation
import TokiNoteCore

enum L10n {
    static var navNotes: String { String(localized: "nav_notes", defaultValue: "笔记") }
    static var navFocus: String { String(localized: "nav_focus", defaultValue: "专注") }
    static var navStats: String { String(localized: "nav_stats", defaultValue: "统计") }
    static var navSettings: String { String(localized: "nav_settings", defaultValue: "设置") }
    static var noteUntitled: String { String(localized: "note_untitled", defaultValue: "无标题") }
    static var actionBack: String { String(localized: "action_back", defaultValue: "返回") }
    static var actionPin: String { String(localized: "action_pin", defaultValue: "置顶") }
    static var actionUnpin: String { String(localized: "action_unpin", defaultValue: "取消置顶") }
    static var actionDelete: String { String(localized: "action_delete", defaultValue: "删除") }
    static var actionCancel: String { String(localized: "action_cancel", defaultValue: "取消") }
    static var actionDone: String { String(localized: "action_done", defaultValue: "完成") }
    static var actionClose: String { String(localized: "action_close", defaultValue: "关闭") }
    static var deleteNoteTitle: String { String(localized: "delete_note_title", defaultValue: "删除这条笔记？") }
    static var deleteNoteBody: String { String(localized: "delete_note_body", defaultValue: "笔记会移到回收站，保留 14 天。") }
    static var editorHint: String { String(localized: "editor_hint", defaultValue: "写点什么…") }
    static var titleHint: String { String(localized: "title_hint", defaultValue: "标题") }
    static var searchPrompt: String { String(localized: "search_tokinote", defaultValue: "搜索 TokiNote") }
    static var sectionPinned: String { String(localized: "section_pinned", defaultValue: "已置顶") }
    static var sectionAll: String { String(localized: "section_all", defaultValue: "全部笔记") }
    static var notesEmpty: String { String(localized: "notes_empty", defaultValue: "还没有笔记") }
    static var notesEmptySearch: String { String(localized: "notes_empty_search", defaultValue: "没有匹配的笔记") }
    static var notesPlaceholder: String { String(localized: "notes_placeholder", defaultValue: "选择一条笔记") }
    static var selectSection: String { String(localized: "select_section", defaultValue: "选择一项") }
    static var newNote: String { String(localized: "new_note", defaultValue: "新建笔记") }
    static var focusTitle: String { String(localized: "focus_title", defaultValue: "专注") }
    static var focusShortBreak: String { String(localized: "focus_short_break", defaultValue: "短休息") }
    static var focusLongBreak: String { String(localized: "focus_long_break", defaultValue: "长休息") }
    static var focusUpNext: String { String(localized: "focus_up_next", defaultValue: "接下来") }
    static var focusIndefiniteHint: String { String(localized: "focus_indefinite_hint", defaultValue: "正向计时中") }
    static var focusIndefinite: String { String(localized: "focus_indefinite", defaultValue: "不定时专注") }
    static var focusPlay: String { String(localized: "focus_play", defaultValue: "开始") }
    static var focusPause: String { String(localized: "focus_pause", defaultValue: "暂停") }
    static var focusReset: String { String(localized: "focus_reset", defaultValue: "重置") }
    static var focusSkip: String { String(localized: "focus_skip", defaultValue: "跳过") }
    static var focusSummary: String { String(localized: "focus_summary", defaultValue: "专注总结") }
    static var focusSettings: String { String(localized: "focus_settings", defaultValue: "计时器设置") }
    static var settingsAppearance: String { String(localized: "settings_appearance", defaultValue: "外观") }
    static var settingsTimer: String { String(localized: "settings_timer", defaultValue: "计时器") }
    static var settingsData: String { String(localized: "settings_data", defaultValue: "数据") }
    static var settingsAbout: String { String(localized: "settings_about", defaultValue: "关于") }
    static var themeMode: String { String(localized: "theme_mode", defaultValue: "主题模式") }
    static var themeSystem: String { String(localized: "theme_system", defaultValue: "跟随系统") }
    static var themeLight: String { String(localized: "theme_light", defaultValue: "浅色") }
    static var themeDark: String { String(localized: "theme_dark", defaultValue: "深色") }
    static var settingAmoled: String { String(localized: "setting_amoled", defaultValue: "纯黑主题") }
    static var settingAmoledDesc: String { String(localized: "setting_amoled_desc", defaultValue: "在深色模式下使用纯黑背景") }
    static var colorPreset: String { String(localized: "color_preset", defaultValue: "莫奈取色") }
    static var colorDefault: String { String(localized: "color_default", defaultValue: "抹茶") }
    static var colorBlue: String { String(localized: "color_blue", defaultValue: "晴空") }
    static var colorPurple: String { String(localized: "color_purple", defaultValue: "紫藤") }
    static var colorOrange: String { String(localized: "color_orange", defaultValue: "暖橙") }
    static var timerSessions: String { String(localized: "timer_sessions", defaultValue: "长休息间隔") }
    static var timerAutoStart: String { String(localized: "timer_auto_start", defaultValue: "自动开始下一段") }
    static var timerAutoStartDesc: String { String(localized: "timer_auto_start_desc", defaultValue: "上一段结束后自动开始计时") }
    static var timerCustom: String { String(localized: "timer_custom", defaultValue: "自定义") }
    static var timerPickDuration: String { String(localized: "timer_pick_duration", defaultValue: "选择时长") }
    static var timerConfirm: String { String(localized: "timer_confirm", defaultValue: "确定") }
    static var dataNotes: String { String(localized: "data_notes", defaultValue: "笔记数量") }
    static var dataTrash: String { String(localized: "data_trash", defaultValue: "回收站") }
    static var trashTitle: String { String(localized: "trash_title", defaultValue: "回收站") }
    static var trashSubtitle: String { String(localized: "trash_subtitle", defaultValue: "保留 14 天，之后自动删除") }
    static var trashEmpty: String { String(localized: "trash_empty", defaultValue: "这里还没有内容") }
    static var restore: String { String(localized: "archive_restore", defaultValue: "恢复") }
    static var deleteForever: String { String(localized: "archive_delete_forever", defaultValue: "彻底删除") }
    static var deleteForeverTitle: String { String(localized: "delete_forever_title", defaultValue: "彻底删除这条笔记？") }
    static var deleteForeverBody: String { String(localized: "delete_forever_body", defaultValue: "删除后无法恢复。") }
    static var dataClear: String { String(localized: "data_clear", defaultValue: "清空全部数据") }
    static var dataClearTitle: String { String(localized: "data_clear_title", defaultValue: "清空所有数据？") }
    static var dataClearBody: String { String(localized: "data_clear_body", defaultValue: "所有笔记、专注记录和偏好设置都会被删除，且无法恢复。") }
    static var settingMotion: String { String(localized: "setting_motion", defaultValue: "减弱动效") }
    static var settingMotionDesc: String { String(localized: "setting_motion_desc", defaultValue: "关闭页面转场与按压缩放动画") }
    static var settingShowTime: String { String(localized: "setting_show_time", defaultValue: "显示时间") }
    static var settingShowTimeDesc: String { String(localized: "setting_show_time_desc", defaultValue: "在笔记上显示具体时间") }
    static var aboutCheckUpdate: String { String(localized: "about_check_update", defaultValue: "检查更新") }
    static var aboutLatest: String { String(localized: "about_latest", defaultValue: "已是最新") }
    static var aboutCheckFailed: String { String(localized: "about_check_failed", defaultValue: "检查失败，点按重试") }
    static var aboutNoRelease: String { String(localized: "about_no_release", defaultValue: "当前通道暂无可用版本") }
    static var aboutGithub: String { String(localized: "about_github", defaultValue: "GitHub 主页") }
    static var aboutGithubDesc: String { String(localized: "about_github_desc", defaultValue: "github.com/ichiyotsu/TokiNote") }
    static var updateDownload: String { String(localized: "update_download", defaultValue: "下载 IPA") }
    static var updateSideStore: String { String(localized: "update_sidestore", defaultValue: "用 SideStore 打开") }
    static var updateNoIPA: String { String(localized: "update_no_ipa", defaultValue: "这个版本没有可用的 IPA") }
    static var updateNotes: String { String(localized: "update_notes", defaultValue: "更新公告") }
    static var statsToday: String { String(localized: "stats_today", defaultValue: "今天") }
    static var statsFocus: String { String(localized: "stats_focus", defaultValue: "专注") }
    static var statsBreak: String { String(localized: "stats_break", defaultValue: "休息") }
    static var statsLastWeek: String { String(localized: "stats_last_week", defaultValue: "上周") }
    static var statsLastMonth: String { String(localized: "stats_last_month", defaultValue: "上个月") }
    static var statsAvgFocus: String { String(localized: "stats_avg_focus", defaultValue: "日均专注") }
    static var sortAscending: String { String(localized: "sort_ascending", defaultValue: "切换为升序") }
    static var sortDescending: String { String(localized: "sort_descending", defaultValue: "切换为降序") }
    static var focusCommand: String { String(localized: "focus_command", defaultValue: "专注") }
    static var commandToggle: String { String(localized: "command_toggle", defaultValue: "开始或暂停") }
    static var notifDoneFocus: String { String(localized: "focus_notif_done_focus", defaultValue: "专注结束") }
    static var notifDoneBreak: String { String(localized: "focus_notif_done_break", defaultValue: "休息结束") }
    static var checking: String { String(localized: "update_checking", defaultValue: "正在检查…") }

    static func focusSession(_ index: Int, _ total: Int) -> String {
        String(format: String(localized: "focus_session", defaultValue: "第 %1$d / %2$d 段"), locale: Locale(identifier: "zh_CN"), index, total)
    }

    static func focusNextHint(_ time: String, _ mode: String) -> String {
        String(format: String(localized: "focus_next_hint", defaultValue: "%1$@ · %2$@"), locale: Locale(identifier: "zh_CN"), time, mode)
    }

    static func sessionsValue(_ count: Int) -> String {
        String(format: String(localized: "sessions_value", defaultValue: "%1$d 段"), locale: Locale(identifier: "zh_CN"), count)
    }

    static func aboutVersion(_ version: String) -> String {
        String(format: String(localized: "settings_about_sub", defaultValue: "版本 %1$@"), locale: Locale(identifier: "zh_CN"), version)
    }

    static func updateTitle(_ version: String) -> String {
        String(format: String(localized: "update_title", defaultValue: "发现新版本 %1$@"), locale: Locale(identifier: "zh_CN"), version)
    }

    static func updateCurrent(_ version: String) -> String {
        String(format: String(localized: "update_current", defaultValue: "当前版本 %1$@"), locale: Locale(identifier: "zh_CN"), version)
    }

    static func notifDoneFocusBody(_ duration: String) -> String {
        String(format: String(localized: "focus_notif_done_focus_body", defaultValue: "已完成 %1$@"), locale: Locale(identifier: "zh_CN"), duration)
    }

    static func notifDoneBreakBody(_ duration: String) -> String {
        String(format: String(localized: "focus_notif_done_break_body", defaultValue: "休息了 %1$@"), locale: Locale(identifier: "zh_CN"), duration)
    }

    static func modeTitle(_ mode: FocusMode) -> String {
        switch mode {
        case .focus: return focusTitle
        case .shortBreak: return focusShortBreak
        case .longBreak:         return focusLongBreak
        }
    }
}
