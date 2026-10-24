# TokiNote

本地优先的笔记 + 专注应用。设计语言以 [Tomato](https://github.com/nsh07/Tomato) 为基准，
遵循 Material 3 Expressive：大圆角、胶囊控件、悬浮导航栏、分段列表与 Google Sans Flex。

| 日志时间线 | 相册 | 图片查看器 |
| --- | --- | --- |
| ![](screenshots/journal.png) | ![](screenshots/album.png) | ![](screenshots/viewer.png) |

| 编辑器 | 专注 | 统计 |
| --- | --- | --- |
| ![](screenshots/editor.png) | ![](screenshots/focus.png) | ![](screenshots/focus-summary.png) |

| 计时设置 | 外观 | 数据 | 关于 |
| --- | --- | --- | --- |
| ![](screenshots/timer.png) | ![](screenshots/appearance.png) | ![](screenshots/data.png) | ![](screenshots/about.png) |

## 笔记

- 顶部是一条半透明磨砂搜索胶囊：菜单、搜索 TokiNote、网格/列表切换、排序、头像；不再有独立 Note 标题和主页 Tab 行
- 底栏是 One UI 风格的单层毛玻璃胶囊（30dp 模糊 + 轻叠色 + 边缘高光）与小圆角矩形按钮：列表 / 日历 / 搜索结果从下方滚过时会真实模糊背后内容
- Expressive 外形：不对称圆角卡片、加粗大标题、细描边的彩色标签
- 笔记列表：支持「已置顶 / 全部笔记」小标题、两列网格与单列列表；卡片有图时图片顶部铺满，无图时不渲染图片容器
- 左侧抽屉：笔记 / 相册 / 统计三个入口，选中高亮；笔记分组与相册文件夹是可折叠小节（旋转箭头 + 伸缩动画），抽屉左边距 16dp、宽度 68%、大阴影
- 相册文件夹：支持命名、多选照片、查看创建时间、编辑/删除；点顶栏头像打开 Google 账户风格「个人中心」，可展开编辑用户名 / 邮箱、从相册换头像、查看 App 存储占用并跳系统应用详情；功能卡与存储卡按设置页样式贴成一张分组卡片
- 全屏沉浸式：状态栏 / 导航栏透明，顶部有一层渐变 scrim，状态栏图标随深浅色自动切换
- 日志：按天分组的卡片，含 emoji、标题、正文预览、描边标签、图片拼贴与时间（可在外观设置里关闭时间）
- 长按卡片会从卡片实际位置膨胀成屏幕中央的预览窗，背景压暗，可直接打开 / 归档 / 删除
- 归档的笔记不进首页且不可编辑；回收站保留 14 天，可恢复或彻底删除
- 相册：当月图片三列网格；点开进入全屏查看器
  - 双指缩放、双击放大
  - 底部悬浮操作条：保存到系统相册 / 查看原笔记 / 图片信息（大小、时间、路径）
- 日历页：月视图，有日记的日子显示当天 emoji，选中日直接列出当天日记
- 编辑器：首图提取主色，驱动背景 / 标题 / 保存按钮 / 底部工具岛；正文与头部全屏滚动、顶栏和工具岛悬浮；向下滚动时标题缩小、日期标签 Spring 收起，回顶弹回；
  图片横幅 + 大标题留白；底部是默认收起、点击后弹性展开的悬浮工具岛；顶部「更多」打开笔记信息面板（创建 / 编辑时间、标签、归档 / 删除）

## 专注

- 专注 25 / 短休息 5 / 长休息 15，长休息间隔可调
- 「波浪断点」计时环 + 中心暂停按钮
- 不定时专注：长按或双击计时数字进入正向计时，重置回到原计时
- 重置 / 跳过 / 计时器设置三个按钮，右下角独立按钮直接进入统计
- 统计页照 Tomato：今天专注与休息卡片、上周柱状图、上个月迷你柱状图
- 计时设置照 Tomato：大号时间预览 + 场景切换 + 预设胶囊 + 自定义（系统时间选择器）

## 设置

- 外观：主题模式、配色预设（抹茶 / 晴空 / 紫藤 / 暖橙）、动态取色、纯黑、显示时间、减弱动效
- 数据：笔记数量、备份 JSON、清空全部数据（同为分段列表项）
- 关于：只保留「基于 Material Design 3 Expressive 设计」、版本、检查更新、GitHub 主页；检查更新右侧有 Release / Beta 下拉通道（M3E 大圆角菜单 + Spring 箭头）

## 应用内更新

「检查更新」读取 GitHub Releases API 比对版本；有新版时展示 release notes，
可下载 APK 并交给系统安装器（未授权「安装未知应用」时自动跳转设置）。

## 数据存储

所有数据保存在设备本地（`SharedPreferences` + JSON）：`notes` / `labels` / `groups` / `settings` / `sessions`。
只有「检查更新 / 下载 APK」会访问 `api.github.com` 与 GitHub 下载地址。

## 构建

```bash
# 需要 JDK 17+ 与 Android SDK（compileSdk 37）
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 工程结构

```
app/src/main/java/com/tokinote/app/
├── MainActivity.kt            入口，主题（深色 / 动态取色 / 配色预设 / 纯黑）
├── data/
│   ├── Note.kt                笔记模型
│   ├── NoteGroup.kt           首页分组模型
│   ├── AppSettings.kt         设置、配色预设与专注记录
│   ├── TokiStore.kt           SharedPreferences + JSON 持久化
│   ├── UpdateChecker.kt       GitHub Releases 检查与 APK 下载
│   └── AppViewModel.kt        单一状态源
└── ui/
    ├── TokiNoteApp.kt         导航栈 + 底栏 + 转场
    ├── Dest.kt                页面路由
    ├── Motion.kt              动效开关、弹簧参数、按压缩放
    ├── components/            顶栏、悬浮导航栏、胶囊按钮组、分段列表
    ├── notes/                 时间线、分组、相册网格、编辑器、图片查看器、日历
    ├── focus/                 番茄钟、波浪环、统计、计时设置
    └── settings/              外观 / 数据 / 关于、APK 安装器
```

## 设计来源与许可

- 视觉基准：[Tomato](https://github.com/nsh07/Tomato)（GPL-3.0）
- 字体：[Google Sans Flex](https://fonts.google.com/specimen/Google+Sans+Flex) 与 [Micro 5 Charted](https://fonts.google.com/specimen/Micro+5+Charted)，随应用打包
- 组件：Jetpack Compose 1.12.1 + Material 3 1.5.0 + Haze 2.0.1（毛玻璃）

## 已知限制

- 计时器仅前台运行（未接前台 Service）
- 富文本是整条日记级的字号/字重，不是局部加粗
- 应用内更新依赖 GitHub 可达；无法安装时会引导用户去系统设置授权
