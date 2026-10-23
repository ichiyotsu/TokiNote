# TokiNote — 交接文档（HANDOFF）

> 用途：新会话开局先读本文件，即可无损接续。最后更新：2026-10-24（功能 1–5 实现并验证后）
>
> **工作区已迁移**：工程在 `D:\DSH\TokiNote\project`（git 仓库），工具在 `..\tools`，签名密钥副本在 `..\keys`。
> 旧的 `C:\Users\tokisaki\Documents\deepseek-harness\default-workspace\TokiNote` 与 `.tools` 已于 2026-10-24 删除。

## 1. 仓库与发布

- 仓库：https://github.com/ichiyotsu/TokiNote （public，默认分支 `main`）
- 已发布：**0.1.0** https://github.com/ichiyotsu/TokiNote/releases/tag/0.1.0
  - 附件 `app-release.apk`（14.3 MB，release keystore 签名）
  - 该版本是**本地签名构建 + GitHub API 上传**发的，不是 CI 产物
- 版本规则（用户明确要求，务必遵守）：
  - 正式版 `1.0.0` → tag `1.0.0`，标 Latest
  - Beta `1.4.0-beta` → tag `1.4.0-beta.1`（后续 `.2` `.3`），标 Pre-release
  - Alpha `1.5.5-alpha` → tag `1.5.5-a` + CI 构建号前四位，标 Pre-release
  - **每次发布前必须与用户核对「版本号 + 更新介绍」**，然后才打 tag
- 应用包名固定 `com.tokinote.app`（不可变，否则老用户无法覆盖安装）

## 2. 签名密钥（重要）

- 工作副本：`D:\DSH\TokiNote\keys\`（`release.jks` / `release.jks.b64` / `keystore-info.txt`）
- 原始副本：`C:\Users\tokisaki\TokiNote-keys\`（**不要删**，目前是唯一离线备份；建议再拷一份到 U 盘/网盘）
- 两个目录都已被 `.gitignore` 排除（`*.jks` / `*.keystore` / `*.b64`），**永远不要提交**
- 密码 / 别名在 `keystore-info.txt`（别名 `tokinote`，两个密码相同）
- GitHub Secrets 已由用户填好 4 个：`KEYSTORE_BASE64`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`

## 3. 环境与工具（新路径）

- 本机无 `git` / `gh`，用便携版：
  - `D:\DSH\TokiNote\tools\mingit\cmd\git.exe`
  - `D:\DSH\TokiNote\tools\gh\bin\gh.exe`（登录态在 Windows 凭据管理器，配置目录 `tools\gh-config`）
  - gh 需要 `GH_CONFIG_DIR` 与 `LOCALAPPDATA=tools\gh-la`（见 `D:\DSH\TokiNote\README.md`）
- 本地构建：`cd D:\DSH\TokiNote\project; .\gradlew.bat --offline assembleDebug`
  - JDK：`D:\Application\Android Studio\jbr`，ANDROID_HOME：`C:\Users\tokisaki\AppData\Local\Android\Sdk`
- 模拟器：AVD `Pixel_10_Pro`（1280×2856）；截图：`adb shell screencap -p /sdcard/s.png` + `adb pull`
- `local.properties` 里的 SDK 路径只在本机有效，换机器要改

## 4. CI 现状（仍未修完，根因已确认）

**根因**：`gradle/libs.versions.toml` 的 `agp = "9.4.1"` 是本地缓存里的内部版本，**公共 Maven 上不存在**，
所以 CI 必然报 `Plugin [id: 'com.android.application', version: '9.4.1'] was not found`。
本地能构建只是因为 `~/.gradle` 里有这个包。

已经修好并在 main 上的 CI 问题：
1. 移除 `android-actions/setup-android@v3`
2. `./gradlew Permission denied` → 改用 runner 预装 Gradle 9.8.1（`gradle` 命令）
3. Release 构建加 `-x lintVitalAnalyzeRelease -x lintVitalRelease`
4. wrapper 超时/重试参数调整（`networkTimeout=120000`、`retries=5`）

**下一步**：
- 查 Google Maven 可用的 AGP（需支持 compileSdk 37；大概率要降到 8.x 并同步调整 Kotlin/Compose 版本），
  然后本地 `./gradlew --offline` 与 CI 都要能过
- 或者接受「本地签名 + API 发布」为固定发版方式（0.1.0 已验证可行）

## 5. 代码结构

```
app/src/main/java/com/tokinote/app/
├── MainActivity.kt
├── data/  Note.kt AppSettings.kt TokiStore.kt UpdateChecker.kt AppViewModel.kt Attachments.kt Label.kt
└── ui/
    ├── TokiNoteApp.kt（导航栈、底栏、转场）
    ├── Dest.kt（Notes/Focus/Settings/Editor/Calendar/FocusSummary/Timer/Preview/MediaViewer/LocationPicker）
    ├── Motion.kt（LocalMotionEnabled、弹簧参数、pressScale）
    ├── Format.kt
    ├── components/（ExpressiveTopBar、FloatingNavBar、NavActionButton、PillGroup、SegmentedListItem）
    ├── notes/（TimelineScreen、JournalComponents、JournalEditorScreen、NoteSheets、CalendarScreen、DocumentParser、DocumentPreview）
    ├── focus/（FocusViewModel、FocusScreen、WavyRing、FocusSummaryScreen、TimerSettingsScreen）
    ├── map/（TencentMap + LocationPicker）
    ├── settings/（SettingsScreens：外观/数据/关于；ApkInstaller）
    └── theme/（Tomato 调色板、Google Sans Flex、Expressive 形状）
```

## 6. 本次会话完成的功能（2026-10-24，均已构建 + 模拟器截图验证）

1. **笔记页顶栏**
   - 「写日记」FAB 并入底栏：胶囊导航 + 右侧独立圆形「+」按钮
   - 「TokiNote」标题左对齐，点按展开搜索栏（自动聚焦）
   - 搜索结果：先给「N 条结果 / 标题命中 / 正文命中」容器化统计，再按日期分组放进大圆角容器
   - 底栏按钮随页面变化：笔记页「+」、专注页「调整时钟」（四方块图标）
2. **专注页**
   - 右上角改为 ⋮ 菜单 →「专注总结」/「计时器」
   - 总结页：本周大数字 + 7 天柱状图（当天高亮）+ 最近记录，可切换上周
   - 计时设置：三选一胶囊按钮组 +「自定义（铅笔）」→ 系统 TimePickerDialog；长休息间隔用段数胶囊 + 自定义滑杆
   - 计时环改为「波浪断点」：平滑轨道 + 正弦波浪粗线进度，中心暂停/开始按钮
3. **设置整理**
   - 设置页删除重复「计时器」入口（只剩 外观 / 数据 / 关于）
   - 所有二级页标题左对齐（ExpressiveTopBar 的 `alignStart`；日历页也换用它）
   - 关于页去掉大「T」，版本号独立成组，底部挂 GitHub 主页链接
4. **应用内更新**
   - `UpdateChecker`：`api.github.com/repos/ichiyotsu/TokiNote/releases`（正式版优先），语义化版本比较
   - 有新版 → 弹框展示 notes + 「取消 / 下载安装」，下载到 cache，`FileProvider` 交给系统安装器
   - 未授权时跳转「安装未知应用」设置；`REQUEST_INSTALL_PACKAGES` + `res/xml/file_paths.xml` 已配置
   - 已在模拟器上用 0.0.1 版实测：检查 → 弹框 → 下载 15 MB → 系统「Update this app?」
5. **动效系统**
   - `Motion.kt`：`LocalMotionEnabled`（由设置驱动）、弹簧参数、`Modifier.pressScale`
   - 页面转场按导航深度做「前进滑入 / 返回淡出缩放」；底栏、卡片、胶囊按钮均有按压缩放
   - 外观设置新增「减弱动效」开关，关闭后转场与按压缩放直接落终态

截图已更新到 `screenshots/`（journal / search / focus / focus-summary / timer / about / settings / calendar）。

## 7. 已知限制

- 地图瓦片需联网；离线只显示标记
- Office 预览是文本级（docx/pptx/xlsx 解析 OOXML），旧版 `.doc/.ppt/.xls` 仅可导入
- 计时器仅前台运行（未接前台 Service）
- 富文本为整条日记级字号/字重，非局部加粗
- 应用内更新依赖 GitHub 可达；无法安装时引导用户去系统设置授权

## 8. 继续工作的建议顺序

1. 修 CI 的 AGP 版本（见第 4 节）→ 让「打 tag 自动发版」跑通
2. 真机验证一次应用内更新（含下载、安装、覆盖升级）
3. 与用户核对版本号 + 更新文案，发 1.0.0（或下一个 beta）
4. 每完成一块，构建 + 模拟器截图验证，再提交
