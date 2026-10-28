# TokiNote — 交接文档（HANDOFF）

> 最后更新：2026-10-28（0.3.5 正式版发布）
> 工作区：`D:\DSH\TokiNote\project`（git 仓库）；工具：`..\tools`；密钥：`..\keys`
> 新会话先读本文件，再读 `docs/design-refs/`。

## 1. 仓库与发布

- 仓库：https://github.com/ichiyotsu/TokiNote （public，main）
- 当前 Release：正式版 **0.3.5**（Latest，versionCode 22）、Beta **0.3.7-beta.2**（versionCode 21）、**0.1.0**
- 发布策略：正式版与 Beta 轮流迭代；**从 0.3.5 起不再删除旧 Release，全部保留历史**
- 正式版只从 Beta 验证后的内容收敛；Beta 可领先正式版多个版本
- 发布开关：`TokiFeatures.DESIGN_STYLE_SWITCH_ENABLED = false`，0.3.5 固定 MD3E，外观不显示设计风格入口
- 版本规则：
  - 正式版 `x.y.z`：完整、稳定
  - Beta `x.y.z-beta` / `x.y.z-beta.N`：可领先正式版多个版本，也可能包含不同功能
  - Beta 通道 = beta + 正式版中版本更高者；同版本正式版优先
  - Release 通道只读正式版
- 不再使用 Alpha 通道
- 发版前与用户确认版本号和文案
- 包名固定 `com.tokinote.app`
- **CI 与 Release 工作流仍是红的**：GitHub runner 上 Gradle 解析 AGP 插件异常（本地完全正常）。
  发版方式固定为：本地 `assembleRelease`（带 VERSION_* / KEYSTORE_* 环境变量）→ 将 APK 命名为 `TokiNote-<versionName>.apk` → `gh release create <tag> TokiNote-<versionName>.apk --notes-file ...`
  - 0.2.7 实际命令见第 3 节；tag 由 `gh release create --target main` 创建（提交 `7b0e0c3`）

## 2. 签名密钥（重要）

- `D:\DSH\TokiNote\keys\release.jks|.b64|keystore-info.txt`
- 原始备份：`C:\Users\tokisaki\TokiNote-keys`（唯一离线备份，务必再拷一份外部介质）
- 一律不进 git；密码见 `keystore-info.txt`

## 3. 环境

- 无系统 git/gh：`tools\mingit\cmd\git.exe`、`tools\gh\bin\gh.exe`
- gh：`GH_CONFIG_DIR=tools\gh-config`、`LOCALAPPDATA=tools\gh-la`（current dir 是 project，路径写成 `..\tools\...`）
- 构建：`$env:JAVA_HOME="D:\Application\Android Studio\jbr"; $env:ANDROID_HOME="C:\Users\tokisaki\AppData\Local\Android\Sdk"; .\gradlew.bat --offline assembleDebug`
- 发版构建（0.3.5 示例）：
  ```powershell
  $env:VERSION_NAME='0.3.5'; $env:VERSION_CODE='22'
  $env:KEYSTORE_FILE='D:\DSH\TokiNote\keys\release.jks'
  $env:KEYSTORE_PASSWORD='…'; $env:KEY_ALIAS='tokinote'; $env:KEY_PASSWORD='…'
  .\gradlew.bat --offline assembleRelease -x lintVitalAnalyzeRelease -x lintVitalRelease
  Copy-Item app\build\outputs\apk\release\app-release.apk build\TokiNote-0.3.5.apk
  gh release create 0.3.5 build\TokiNote-0.3.5.apk --title "0.3.5" --notes-file .github\release-notes\0.3.5.md --target main
  ```
- 模拟器 AVD `Pixel_10_Pro`（API 37）；**adb 手势注入对 Compose 不稳定**，长按/点击常无效，交互验证优先真机
- github.com:443 时不时机翻；`git push` 失败可改用 GitHub Git Data API

## 4. 技术栈（已全面升级）

- **隐私边界（硬约束）**：除「检查更新」「公告拉取」和用户主动选择的云盘文件夹同步外，App 不发起网络请求；不接入统计、账号上报，笔记与个人资料只留在本机或用户选定的云盘
- Kotlin **2.4.21**、Compose **1.12.1**、Material3 **1.5.0-beta02**、AGP 9.4.1、Gradle 9.6
- **minSdk 31（Android 12）**，compileSdk/targetSdk 37；向下覆盖 Android 12–15；Haze 在 API 31+ 使用 RenderEffect，低版本保留 fallback
- Android 12–15 兼容：显式 `enableEdgeToEdge` + `setDecorFitsSystemWindows(false)`，状态栏透明、导航栏对比度关闭；手势导航保持透明，3-button 导航栏沿用系统不透明处理；已在 Android 12（API 31）与 API 37 模拟器验证启动、编辑页 IME、抽屉玻璃和 R8 release
- 新增依赖 `com.google.android.material:material:1.13.0`（Monet）、`dev.chrisbanes.haze:haze|haze-blur:2.0.1`（背景模糊）
- Compose 动态色：`dynamicLight/DarkColorScheme`（SDK 31+）；传统 View：`TokiNoteApplication` + `DynamicColors.applyToActivitiesIfAvailable`；`themes.xml` 继承 `Theme.Material3.DynamicColors.DayNight.NoActionBar`
- **Release 已开启 R8**：`isMinifyEnabled = true`、`isShrinkResources = true`；Compose/Haze 使用 consumer rules，`proguard-rules.pro` 只保留注解与可选注解的 warning 规则
- **玻璃统一入口**：`ui/theme/Glass.kt` 的 `Modifier.tokiGlass()` 统一 Haze 输入、模糊半径、tint 和 fallback；底栏、顶栏与抽屉 30dp 走同一条链路
- **代码瘦身**：删除 `WavyRing`、旧 `JournalTopBar` / `JournalTabs` / `SearchResultRow` / `NoteGroupBar`、未使用的 ViewModel 方法，以及 107 条废弃字符串；当前 release APK 约 6.5 MB
- **Pad 横屏适配**：抽屉宽度限制 260dp；笔记网格自适应 2–4 列；专注页时钟左 / 操作右（约 6:4）；设置首页两列，二级页左侧 3:7 快捷栏
- **设计风格**：外观新增 MD3E / One UI；One UI 采用悬浮玻璃 Dock、全宽列表行、圆形 FAB、线性图标、Samsung Sans、#0381FE、26dp focus block、24dp 边距与低饱和蓝粉渐变背景
- **字体**：外观可选跟随风格、Samsung Sans、GoogleSans、苹方，或导入本地 TTF；导入的字体进入下拉列表并可确认删除；苹方使用设备字体，缺字时回退
- **可选专注组件**：设置 → 组件；从 0.3.4 Release 的 `focus-component-1.0.zip` 下载并校验 SHA-256，安装到 `files/components/focus`；启用后底栏才显示专注，可关闭开关或删除组件
- **云同步**：数据页选择 Google Drive / OneDrive 文件夹（SAF），上传快照 / 从云端恢复；快照含笔记和专注统计
- **DOCX**：数据页支持导入 DOCX（归入「导入」分组，标题取文件名）和导出全部笔记

## 5. 已完成的功能

- 笔记：Journal 卡片（图左文右、置顶/日常分区）、长按预览（打开/归档/删除）、笔记内清单、日期/时间/标签合并面板、M3 日期与时间选择器、编辑页顶栏（关闭/更多/对勾）+ 顶部大图
- 首页分组：笔记页左侧抽屉（三条杠）内展示「全部 / 分组 / +」；分组只存 noteId，不复制笔记，删除分组不影响笔记；抽屉标题「 TokiNote 」使用 Google Fonts `Micro 5 Charted`，左边距 16dp、宽度 68%（max 320dp）、四角 34dp、阴影 24dp
- Keep 风格主页（0.2.8）：顶部独立搜索控制区（导航 / 搜索 / 网格列表切换 / 排序 / 头像，滚动时整条玻璃底出现）；「已置顶 / 全部笔记」小标题；两列网格与单列列表；卡片有图时图片顶部铺满、无图时不渲染图片容器
- 抽屉导航（0.2.8）：笔记 / 相册 / 统计从主页迁入抽屉；分组与相册文件夹做成可折叠小节；相册文件夹支持命名、多选照片、创建时间、编辑/删除；头像打开「我的」设置 emoji 与用户 ID
- 我的（0.2.8）：Google 账户风格，用户卡片可展开编辑用户名/邮箱（实时写入设置），头像可从相册选择并同步到顶栏；功能卡与存储卡合并成一张贴紧的分段卡片（分割线，无间隙），存储卡显示 APK+files/cache 占用并提供系统应用详情跳转
- 沉浸式状态栏（0.2.8）：`enableEdgeToEdge` + 透明 status/nav bar；MainActivity 里根据深浅色切 status bar icon 外观；0.3.1 起编辑页顶栏把状态栏区域一起绘制，保证同色
- 更新通道（0.2.8）：关于页检查更新右侧 Release/Beta 下拉，22dp 圆角菜单、Spring 箭头、选中胶囊 + 对勾
- 多 Tab 联动：三条杠只在笔记 Tab 显示；切到相册 / 统计时用 Spring 收起，`Note` 标题向左补位，切回时弹回
- 沉浸式编辑页（0.2.8）：正文和头部作为底层滚动内容，顶栏与工具岛 Overlay；滚动超过阈值时大标题缩小、日期标签 Spring 收起，顶栏显示折叠标题，回到顶部弹回；顶栏使用随 `collapseProgress` 加深的 tonal scrim（编辑页没有用 Haze，避免嵌套滚动下捕获不稳定）
- Expressive 外形（0.2.8）：卡片 / 分组 / FAB 用不对称圆角；标题加大加粗、时间与标签缩小降噪、卡片 padding 加宽；首页标签改细描边；FAB 用高饱和主色
- 单层玻璃底栏（0.2.8）：不再用 HorizontalFloatingToolbar，改成自绘胶囊 Row + Haze 30dp 模糊 + smoke/frost 叠色 + 1dp 边缘高光；选中项是胶囊内部实色，右侧 + 独立
- 长按预览（0.2.8）：从卡片 `boundsInRoot` 到屏幕中央目标 bounds 做共享式膨胀，Emphasized easing、背景压暗、底栏自动收起；见 `NotePreviewOverlay.kt`
- 编辑页（0.2.8）：首图取色（`NotePalette`）驱动背景 / 标题 / 保存按钮 / 工具岛；图片横幅 156dp（键盘弹出时 96dp）；底部悬浮工具岛默认只显示 +，弹性展开四个工具；顶部更多打开笔记信息面板（创建/编辑时间、标签、归档/删除）；正文输入区铺满剩余高度，空白可聚焦，长文内部滚动
- 标签底色（0.2.8）：首页卡片显示描边标签胶囊；编辑页标签面板选中项使用 8 色 `LabelTints`，随深浅色和当前主色轻微调和
- 相册：三列网格 + 全屏查看器（缩放、下载、定位、信息）
- 专注：波浪环、主按钮长胶囊 + 圆形重置 + 窄药丸辅助键、垂直居中、长按/双击进入不定时正向计时（0.2.7 修复手势与 00:00 起计）
- 统计（笔记页第三栏）：年/月/周切换；年无日历只列全年，月显示月历，周显示 7 天条；整页 LazyColumn 可滚动；点日期只看当天，点范围标题回到范围并取消高亮
- 设置：外观（主题模式、莫奈取色预设、纯黑、动态取色、显示时间、减弱动效）、数据（笔记数量/归档/回收站一组；备份/清缓存/存储占用/清空一组）、关于（版本、检查更新 + Release/Beta 下拉、GitHub）
- 归档/回收站：归档不进主页且不可编辑；回收站保留 14 天自动清理
- 底栏：HorizontalFloatingToolbar + ToggleButton；设置页独立搜索按钮 + 设置内搜索
- 图标：Material Symbols `article-shortcut`，`#1E3C38` 底

## 6. 待办（下一会话优先）

1. 真机验证长按预览、不定时专注（模拟器 2026-10-24 已通过：长按卡片出预览；长按计时数字进入 00:03 正向计时；双指/双击手势在 adb 注入下未能确认）
2. 当前为 0.3.5 正式版 + 0.3.7-beta.2 + 0.1.0；后续正式版/Beta 轮流迭代，旧 Release 全部保留；0.3.5 固定 MD3E；APK 命名 `TokiNote-<version>.apk`
3. 真机观察毛玻璃、长按膨胀、编辑页滚动折叠、抽屉的开合性能
4. 未做：选中行独立字号（需要富文本 span 存储，单独一轮）
5. 待验证：编辑页长文输入时光标是否会自动滚动跟随（目前依赖 BasicTextField 在 verticalScroll 中的 bring-into-view 行为）
6. 可选：编辑页顶栏当前是 tonal scrim，不是真模糊；若 Haze 能在嵌套滚动里稳定捕获，可再换回

## 7. 已知坑

- PowerShell 5.1 写中文易乱码：含中文的 Kotlin 字符串优先用 `write`/`edit` 工具或 `[char]0xXXXX` 拼接
- 正则批量替换要确认没吞掉相邻 composable（本会话多次发生）
- 离线构建依赖已缓存；新增依赖需联网构建一次（Haze 2.0.1 已缓存）
- **C 盘（系统盘）容易满**：Gradle/Android 缓存都在 C，剩 0 字节时 `mergeExtDexRelease` 会报「磁盘空间不足」。清 `%LOCALAPPDATA%\Temp` 可立刻腾出空间
- **release 构建必须 `-x lintVitalAnalyzeRelease -x lintVitalRelease`**：离线缓存里没有 `com.android.tools.lint:lint-gradle:32.4.1`，否则任务失败
- **Haze 顶栏**：`onSizeChanged` 给的是 px，必须 `toDp()` 后再做 LazyColumn 的 top padding；直接 `.dp` 会把内容推下几百 dp。顶栏必须脱离布局流叠在列表上，否则没有内容可从背后滚过
- **毛玻璃不要叠层**：底栏/顶栏容器保持透明，tint 放进 `HazeBlurStyle` 的 `colorEffects` / `fallbackColorEffect`，让模糊本身就是背景；不要再额外加 `surface.copy(alpha=…)` 的 Surface 或 `.background()`，否则会变成两层
- **长按预览**：source bounds 用 `onGloballyPositioned { boundsInRoot() }`，目标 bounds 也要在 `onGloballyPositioned` 里量；缩放用等比，避免 scaleX/scaleY 独立导致文字挤压；预览打开时通过回调让 TokiNoteApp 收起底栏
- **编辑页取色**：`BitmapFactory` 用 `inSampleSize = 8` 只采样 48×48 网格，避免主线程解码大图；主题色、工具岛、标题分别用 `animateColorAsState` 过渡
- **正文铺满**：`BoxWithConstraints` 里 header 用 `heightIn(max = maxHeight - 130.dp)` + 独立滚动，正文 `weight(1f)` 占满剩余；不要给正文固定 `height(280.dp)`，否则空白区点不到。键盘弹出时用 `maxHeight < 560.dp` 把横幅收成 96dp
- **底栏减重**：模糊半径 30dp、smoke 0.16 + frost 0.08，1dp 边缘高光；不要再套 HorizontalFloatingToolbar 或第二层 Surface，`FloatingNavBar` 已经重写成单层 Row
- **编辑页顶栏**：内容在 `verticalScroll` 里，顶栏/工具岛是 Box overlay；顶栏背景用 `surface.copy(alpha = 0.28 + 0.70 * collapseProgress)` 的 tonal scrim，不叠 Haze，避免滚动时文字穿透和双层感
- **左侧抽屉**：`GroupDrawer` 在 TimelineScreen 根 Box 之外，必须传 `topInset = contentPadding.calculateTopPadding()`，否则会盖住状态栏；打开时通过 `onDrawerVisibilityChange` 收起底栏，面板底部用 `navigationBarsPadding` 留出安全区
- **Keep 主页**：顶部胶囊的 Haze source 在整个内容 Box 上，不要把 source 放到 `verticalScroll` 内部；无图卡片不要保留 Image 容器，`NoteCard` 只在 `note.media.firstOrNull() != null` 时渲染图片
- **相册文件夹**：`AlbumFolder` 保存图片路径（不是 noteId），删除文件夹不删照片；`AlbumFolderSheet` 用 3 列多选，抽屉行显示创建日期
- **账号面板**：`ProfileSheet` 的 `avatarPath` 存的是 `Attachments.copyImage` 复制到 app files 的路径；存储占用要把 `applicationInfo.sourceDir`（APK）算进去，否则只有几百 KB
- **状态栏**：`enableEdgeToEdge` 用透明 SystemBarStyle；icon 明暗在 MainActivity 的 `SideEffect` 里用 `WindowCompat.getInsetsController` 设置；编辑页把状态栏区域并入顶栏背景，避免透明状态栏与顶栏出现色差
- **更新通道**：`DropdownMenu` 的 `shape/containerColor/tonalElevation/shadowElevation` 参数在 M3 1.5 可用；菜单项用 `clip(CircleShape)` + 选中背景，不要在 `DropdownMenu` 外再包一层背景
- 提交信息里的 7 位短哈希（如 `7b0e0c3`）是 git commit SHA，提交时自动生成，不需要手写备注
