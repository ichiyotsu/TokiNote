# TokiNote — 交接文档（HANDOFF）

> 最后更新：2026-10-24（0.2.8 待发）
> 工作区：`D:\DSH\TokiNote\project`（git 仓库）；工具：`..\tools`；密钥：`..\keys`
> 新会话先读本文件，再读 `docs/design-refs/`。

## 1. 仓库与发布

- 仓库：https://github.com/ichiyotsu/TokiNote （public，main）
- 最新发布：**0.2.7**（release keystore 签名，versionCode 5）；**0.2.8 已构建**（versionCode 6，Haze 毛玻璃 + 标签底色），用户 2026-10-24 决定**暂缓发布**，等待视觉确认
- 版本规则：正式版 tag `x.y.z`；beta tag `x.y.z-beta.1`（Pre-release）；**发版前必须与用户核对版本号 + 文案**
- 包名固定 `com.tokinote.app`
- **CI 与 Release 工作流仍是红的**：GitHub runner 上 Gradle 解析 AGP 插件异常（本地完全正常）。
  发版方式固定为：本地 `assembleRelease`（带 VERSION_* / KEYSTORE_* 环境变量）→ `gh release create <tag> app-release.apk --notes-file ...`
  - 0.2.7 实际命令见第 3 节；tag 由 `gh release create --target main` 创建（提交 `7b0e0c3`）

## 2. 签名密钥（重要）

- `D:\DSH\TokiNote\keys\release.jks|.b64|keystore-info.txt`
- 原始备份：`C:\Users\tokisaki\TokiNote-keys`（唯一离线备份，务必再拷一份外部介质）
- 一律不进 git；密码见 `keystore-info.txt`

## 3. 环境

- 无系统 git/gh：`tools\mingit\cmd\git.exe`、`tools\gh\bin\gh.exe`
- gh：`GH_CONFIG_DIR=tools\gh-config`、`LOCALAPPDATA=tools\gh-la`（current dir 是 project，路径写成 `..\tools\...`）
- 构建：`$env:JAVA_HOME="D:\Application\Android Studio\jbr"; $env:ANDROID_HOME="C:\Users\tokisaki\AppData\Local\Android\Sdk"; .\gradlew.bat --offline assembleDebug`
- 发版构建（0.2.8 验证）：
  ```powershell
  $env:VERSION_NAME='0.2.8'; $env:VERSION_CODE='6'
  $env:KEYSTORE_FILE='D:\DSH\TokiNote\keys\release.jks'
  $env:KEYSTORE_PASSWORD='…'; $env:KEY_ALIAS='tokinote'; $env:KEY_PASSWORD='…'
  .\gradlew.bat --offline assembleRelease -x lintVitalAnalyzeRelease -x lintVitalRelease
  gh release create 0.2.8 app\build\outputs\apk\release\app-release.apk --title "TokiNote 0.2.8" --notes-file .github\release-notes\0.2.8.md --target main
  ```
- 模拟器 AVD `Pixel_10_Pro`（API 37）；**adb 手势注入对 Compose 不稳定**，长按/点击常无效，交互验证优先真机
- github.com:443 时不时机翻；`git push` 失败可改用 GitHub Git Data API

## 4. 技术栈（已全面升级）

- Kotlin **2.4.21**、Compose **1.12.1**、Material3 **1.5.0-beta02**、AGP 9.4.1、Gradle 9.6
- **minSdk 36（Android 16）**、compileSdk/targetSdk 37
- 新增依赖 `com.google.android.material:material:1.13.0`（Monet）、`dev.chrisbanes.haze:haze|haze-blur:2.0.1`（背景模糊）
- Compose 动态色：`dynamicLight/DarkColorScheme`（SDK 31+）；传统 View：`TokiNoteApplication` + `DynamicColors.applyToActivitiesIfAvailable`；`themes.xml` 继承 `Theme.Material3.DynamicColors.DayNight.NoActionBar`

## 5. 已完成的功能

- 笔记：Journal 卡片（图左文右、置顶/日常分区）、长按预览（打开/归档/删除，0.2.7 起任意卡片可用）、笔记内清单、日期/时间/标签合并面板、M3 日期与时间选择器、编辑页顶栏（关闭/删除/对勾）+ 顶部大图
- 首页分组：笔记页 Tab 下方「全部 / 分组 / +」；`+` 输入名称并多选笔记建组，长按分组可改名、增删笔记或删除；分组只存 noteId，不复制笔记，删除分组不影响笔记
- 毛玻璃（0.2.8）：笔记页顶栏 + 分组条 + 悬浮底栏用 Haze 做真实背景模糊；列表/日历从顶栏下方滚过，`headerHeight` 用 px→dp 测量后作为 LazyColumn 的 `topInset`
- 标签底色（0.2.8）：首页卡片显示彩色标签胶囊；编辑页标签面板选中项使用同一套 8 色 `LabelTints`，随深浅色和当前主色轻微调和
- 相册：三列网格 + 全屏查看器（缩放、下载、定位、信息）
- 专注：波浪环、主按钮长胶囊 + 圆形重置 + 窄药丸辅助键、垂直居中、长按/双击进入不定时正向计时（0.2.7 修复手势与 00:00 起计）
- 统计（笔记页第三栏）：年/月/周切换；年无日历只列全年，月显示月历，周显示 7 天条；整页 LazyColumn 可滚动；点日期只看当天，点范围标题回到范围并取消高亮
- 设置：外观（主题模式、莫奈取色预设、纯黑、动态取色、显示时间、减弱动效）、数据（笔记数量/归档/回收站一组；备份/清缓存/存储占用/清空一组）、关于（版本、检查更新 + Release/Beta 下拉、GitHub）
- 归档/回收站：归档不进主页且不可编辑；回收站保留 14 天自动清理
- 底栏：HorizontalFloatingToolbar + ToggleButton；设置页独立搜索按钮 + 设置内搜索
- 图标：Material Symbols `article-shortcut`，`#1E3C38` 底

## 6. 待办（下一会话优先）

1. 真机验证长按预览、不定时专注（模拟器 2026-10-24 已通过：长按卡片出预览；长按计时数字进入 00:03 正向计时；双指/双击手势在 adb 注入下未能确认）
2. 0.2.8 视觉确认后再发布（本地 release APK 已签名，见第 3 节）
3. 真机观察毛玻璃顶栏/底栏的滚动性能与浅色对比度（模拟器 0.2.8 已通过）
4. 未做：选中行独立字号（需要富文本 span 存储，单独一轮）
5. 可选：预设/动态色下的标签色调再微调（0.2.8 已做 8% 主色融合）

## 7. 已知坑

- PowerShell 5.1 写中文易乱码：含中文的 Kotlin 字符串优先用 `write`/`edit` 工具或 `[char]0xXXXX` 拼接
- 正则批量替换要确认没吞掉相邻 composable（本会话多次发生）
- 离线构建依赖已缓存；新增依赖需联网构建一次（Haze 2.0.1 已缓存）
- **C 盘（系统盘）容易满**：Gradle/Android 缓存都在 C，剩 0 字节时 `mergeExtDexRelease` 会报「磁盘空间不足」。清 `%LOCALAPPDATA%\Temp` 可立刻腾出空间
- **release 构建必须 `-x lintVitalAnalyzeRelease -x lintVitalRelease`**：离线缓存里没有 `com.android.tools.lint:lint-gradle:32.4.1`，否则任务失败
- **Haze 顶栏**：`onSizeChanged` 给的是 px，必须 `toDp()` 后再做 LazyColumn 的 top padding；直接 `.dp` 会把内容推下几百 dp。顶栏必须脱离布局流叠在列表上，否则没有内容可从背后滚过
- 提交信息里的 7 位短哈希（如 `7b0e0c3`）是 git commit SHA，提交时自动生成，不需要手写备注
