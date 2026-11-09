namespace TokiNote;

internal static class Strings
{
    private static readonly Dictionary<string, string> Fallback = new()
    {
        ["app_name"] = "TokiNote",
        ["nav_notes"] = "笔记",
        ["nav_focus"] = "专注",
        ["nav_stats"] = "统计",
        ["nav_archive"] = "归档",
        ["nav_trash"] = "回收站",
        ["nav_settings"] = "设置",
        ["search_tokinote"] = "搜索 TokiNote",
        ["entry_new"] = "写日记",
        ["note_untitled"] = "无标题",
        ["title_hint"] = "标题",
        ["editor_hint"] = "写点什么…",
        ["action_pin"] = "置顶",
        ["action_unpin"] = "取消置顶",
        ["action_delete"] = "删除",
        ["action_cancel"] = "取消",
        ["action_save"] = "保存",
        ["preview_archive"] = "归档",
        ["archive_restore"] = "恢复",
        ["archive_delete_forever"] = "彻底删除",
        ["delete_note_title"] = "删除这条笔记？",
        ["delete_note_body"] = "笔记会移到回收站，保留 14 天。",
        ["section_pinned"] = "已置顶",
        ["archive_title"] = "归档",
        ["archive_subtitle"] = "归档的笔记不会出现在主页，且不可编辑",
        ["trash_title"] = "回收站",
        ["trash_subtitle"] = "保留 14 天，之后自动删除",
        ["archive_empty"] = "这里还没有内容",
        ["focus_title"] = "专注",
        ["focus_short_break"] = "短休息",
        ["focus_long_break"] = "长休息",
        ["focus_up_next"] = "接下来",
        ["focus_next_hint"] = "{0} · {1}",
        ["focus_indefinite"] = "不定时专注",
        ["focus_indefinite_hint"] = "正向计时中",
        ["focus_session"] = "第 {0} / {1} 段",
        ["focus_play"] = "开始",
        ["focus_pause"] = "暂停",
        ["focus_reset"] = "重置",
        ["focus_skip"] = "跳过",
        ["focus_summary"] = "专注总结",
        ["stats_today"] = "今天",
        ["stats_focus"] = "专注",
        ["stats_break"] = "休息",
        ["stats_last_week"] = "上周",
        ["stats_last_month"] = "上个月",
        ["stats_avg_focus"] = "日均专注",
        ["settings_appearance"] = "外观",
        ["settings_timer"] = "计时器",
        ["settings_data"] = "数据",
        ["settings_about"] = "关于",
        ["theme_mode"] = "主题模式",
        ["theme_system"] = "跟随系统",
        ["theme_light"] = "浅色",
        ["theme_dark"] = "深色",
        ["setting_amoled"] = "纯黑主题",
        ["setting_amoled_desc"] = "在深色模式下使用纯黑背景",
        ["setting_motion"] = "减弱动效",
        ["setting_motion_desc"] = "关闭页面转场与按压缩放动画",
        ["setting_show_time"] = "显示时间",
        ["setting_show_time_desc"] = "在日记卡片上显示具体时间",
        ["timer_sessions"] = "长休息间隔",
        ["timer_auto_start"] = "自动开始下一段",
        ["timer_auto_start_desc"] = "上一段结束后自动开始计时",
        ["data_notes"] = "笔记数量",
        ["data_archive"] = "归档",
        ["data_trash"] = "回收站",
        ["data_export"] = "备份 JSON",
        ["data_clear"] = "清空全部数据",
        ["data_clear_title"] = "清空所有数据？",
        ["data_clear_body"] = "所有笔记、标签和偏好设置都会被删除，且无法恢复。",
        ["about_check_update"] = "检查更新",
        ["about_latest"] = "已是最新",
        ["about_check_failed"] = "检查失败，点按重试",
        ["about_no_release"] = "当前通道暂无可用版本",
        ["about_github"] = "GitHub 主页",
        ["update_title"] = "发现新版本 {0}",
        ["update_current"] = "当前版本 {0}",
        ["update_download"] = "下载更新",
        ["update_downloading"] = "正在下载… {0}%",
        ["update_failed"] = "下载失败，请稍后再试",
        ["update_no_package"] = "这个版本没有可用的安装包",
        ["focus_notif_done_focus"] = "专注结束",
        ["focus_notif_done_break"] = "休息结束",
        ["focus_notif_done_focus_body"] = "已完成 {0}",
        ["focus_notif_done_break_body"] = "休息了 {0}",
        ["tray_open"] = "打开",
        ["tray_exit"] = "退出",
        ["notes_empty"] = "还没有笔记",
        ["focus_minutes"] = "专注时长",
        ["short_break_minutes"] = "短休息",
        ["long_break_minutes"] = "长休息",
    };

    private static bool _loaderFailed;
    private static Microsoft.Windows.ApplicationModel.Resources.ResourceLoader? _loader;

    public static string Get(string key)
    {
        if (!_loaderFailed)
        {
            try
            {
                _loader ??= new Microsoft.Windows.ApplicationModel.Resources.ResourceLoader();
                var value = _loader.GetString(key);
                if (!string.IsNullOrEmpty(value))
                {
                    return value;
                }
            }
            catch
            {
                _loaderFailed = true;
            }
        }

        return Fallback.TryGetValue(key, out var text) ? text : key;
    }

    public static string Format(string key, params object[] args) => string.Format(Get(key), args);
}
