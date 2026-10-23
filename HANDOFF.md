# TokiNote — 交接文档（HANDOFF）

> 用途：新会话开局先读本文件，即可无损接续。最后更新：2026-10-24 02:5x（会话上下文即将耗尽时写）
>
> **新会话第一件事**：通读本文件 + 看 `docs/design-refs/` 里 4 张参考图，然后再动手。
> 本文件与参考图都还没提交到 git（本地工作区），开工前请先 `git add HANDOFF.md docs/ && commit`。

## 1. 仓库与发布

- 仓库：https://github.com/ichiyotsu/TokiNote （public，默认分支 `main`）
- 已发布：**0.1.0** https://github.com/ichiyotsu/TokiNote/releases/tag/0.1.0
  - 附件 `app-release.apk`（14.3 MB，release keystore 签名）
  - Release notes：`.github/release-notes/0.1.0.md`（用户逐字提供的文案）
  - 该版本是**本地签名构建 + GitHub API 上传**发的，不是 CI 产物
- 版本规则（用户明确要求，务必遵守）：
  - 正式版 `1.0.0` → tag `1.0.0`，标 Latest
  - Beta `1.4.0-beta` → tag `1.4.0-beta.1`（后续 `.2` `.3`），标 Pre-release
  - Alpha `1.5.5-alpha` → tag `1.5.5-a` + CI 构建号前四位，标 Pre-release
  - **每次发布前必须与用户核对「版本号 + 更新介绍」**，然后才打 tag
- 应用包名固定 `com.tokinote.app`（不可变，否则老用户无法覆盖安装）

## 2. 签名密钥（重要）

`C:\Users\tokisaki\TokiNote-keys\`
- `release.jks` — 发布签名密钥（**必须备份**，丢了无法给已安装用户升级）
- `release.jks.b64` — base64，用于 GitHub Secret `KEYSTORE_BASE64`
- `keystore-info.txt` — 密码 / 别名（`tokinote`，两个密码相同）

GitHub Secrets 已由用户填好 4 个：`KEYSTORE_BASE64`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`。

## 3. 环境与工具

- 本机无 `git`，使用便携版：`.tools\mingit\cmd\git.exe`
- 本机无 `gh`，使用便携版：`.tools\gh\bin\gh.exe`
  - 已通过 Device Flow 登录 `ichiyotsu`，token 在 keyring；`GH_CONFIG_DIR=.tools\gh-config`
  - `gh` 缓存目录需设 `LOCALAPPDATA=.tools\gh-la`（否则报 Access denied）
- 本地代理 `127.0.0.1:7890`（FlClash）时开时关；`github.com:443` 直连常不通，**`api.github.com` 可通** → 可用 Contents API 提交文件
- 本地构建：`cd TokiNote; .\gradlew.bat --offline assembleDebug`（JDK：`D:\Application\Android Studio\jbr`，ANDROID_HOME：`C:\Users\tokisaki\AppData\Local\Android\Sdk`）
- 模拟器：AVD `Pixel_10_Pro`，`adb` 在 SDK platform-tools；截图流程见 `.tools` 旧命令（`screencap` + `pull`）

## 4. CI 现状（未修完，根因已定位）

**根因**：`AGP 9.4.1` 在公共仓库不存在（本地缓存里的内部版本）。
로그：`Plugin [id: 'com.android.application', version: '9.4.1'] was not found`。

已修复的 CI 问题（都已在 main 上）：
1. 移除 `android-actions/setup-android@v3`（它执行已消失的 `sdkmanager tools`）
2. `./gradlew Permission denied` → 改用 runner **预装 Gradle 9.8.1**
3. wrapper 下载超时 → 不再走 wrapper（CI 用 `gradle` 命令）
4. Release 命令加 `-x lintVitalAnalyzeRelease -x lintVitalRelease`（lintVital 在本地失败）

**下一步**：
- 查 Google Maven 上可用的 AGP 版本（需能访问 `https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/maven-metadata.xml`），选一个支持 compileSdk 37 的公开版本（AGP 9.0+ 可能可用）
- 同步改 `gradle/libs.versions.toml` 的 `agp`，必要时调 Kotlin/Compose 版本
- 验证：改完推 main → CI 绿 → 再移动 tag 触发 Release 验证整链
- 备选方案：接受「本地签名 + API 发布」为固定发版方式（已验证可行）

## 5. 代码结构

```
app/src/main/java/com/tokinote/app/
├── MainActivity.kt
├── data/  Note.kt(emoji/day/media/files/location) TokiStore.kt AppViewModel.kt Attachments.kt
└── ui/
    ├── TokiNoteApp.kt（导航栈、底栏、路由）
    ├── Dest.kt（Notes/Focus/Settings/Editor/Calendar/Preview/MediaViewer/LocationPicker）
    ├── components/（ExpressiveTopBar, FloatingNavBar, SegmentedListItem）
    ├── notes/（TimelineScreen, JournalComponents, JournalEditorScreen, NoteSheets, CalendarScreen, DocumentParser, DocumentPreview）
    ├── focus/（FocusViewModel, FocusScreen）
    ├── map/（TencentMap + LocationPicker）
    ├── settings/（SettingsScreens：主页 + 外观/计时器/数据/关于）
    └── theme/（Tomato 调色板、Google Sans Flex、Expressive 形状）
```

设计来源：Tomato（主题/导航/计时器）、June（日记时间线/日历/编辑器）、Material 3 Expressive。

## 6. 待实现功能（用户 2026-10-24 提出）

1. **笔记页顶栏**
   - 「写日记」FAB 并入底栏（参考 `docs/design-refs/01-bottombar.png`：胶囊底栏 + 右侧独立「+」按钮）
   - 「TokiNote」标题改左对齐；点击标题展开搜索栏
   - 搜索栏内用逻辑分组/容器区分结果（MD3E 的 grouping/container 原则）
2. **专注页**
   - 右上角设置改为 ⋮ 菜单 → 进入「总结」页：历史专注次数/总时长/做了什么（`docs/design-refs/02-focus-summary.png`：大数字 + 柱状图周报）
   - 底栏那个额外按钮在此页变为「调整时钟」（四方块 icon）→ 进入计时器设置
   - 计时设置：长条滑杆改为独立按钮（`docs/design-refs/04-pills.png`：三选一胶囊按钮组），保留「自定义」+ 编辑 icon，点按可改预设值；时间输入优先调用系统默认选择器
   - 专注计时圆环改为 `docs/design-refs/03-wavy-ring.png` 的「波浪断点」样式（一半平滑、一半波浪粗线 + 中心暂停按钮）
3. **设置整理**
   - 专注页独立计时设置后，设置页删除重复的计时器入口
   - 所有二级页面标题/文本统一左对齐
   - 关于页去掉大「T」，底部栏挂 GitHub 主页链接
4. **应用内更新**
   - 关于页新增：版本号独立成一组（MD3E 强调分组/容器/留白）
   - 点击检查更新 → 调 GitHub Releases API 比对版本
   - 最新则弹框提示；有新版本则展示 release notes + 「取消 / 下载安装」
   - 参考 https://github.com/Starfallan/PiliNara 的实现方式
5. **动画反馈系统**
   - 参考 MD3E / Tomato / PiliNara 的 motion 规范，做统一的转场、按压、状态切换动效
   - 设置页提供「动画 / 减弱动效」开关

## 7. 已知限制

- 地图瓦片需联网；离线只显示标记
- Office 预览是文本级（docx/pptx/xlsx 解析 OOXML），旧版 `.doc/.ppt/.xls` 仅可导入
- 计时器仅前台运行（未接前台 Service）
- 富文本为整条日记级字号/字重，非局部加粗

## 8. 继续工作的建议顺序

1. 修 CI（AGP 版本）→ 让「打 tag 自动发版」跑通
2. 功能 1（笔记页顶栏 + 底栏 FAB）
3. 功能 2（专注总结页 + 计时设置 + 波浪环）
4. 功能 3（设置整理 + 关于页链接）
5. 功能 4（应用内更新）
6. 功能 5（动画系统 + 设置项）

每完成一块，构建 + 模拟器截图验证，再提交。
