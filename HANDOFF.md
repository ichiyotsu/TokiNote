# TokiNote — 交接文档（HANDOFF）

> 最后更新：2026-10-24（0.2.6 已发布，0.2.7 待发）
> 工作区：`D:\DSH\TokiNote\project`（git 仓库）；工具：`..\tools`；密钥：`..\keys`
> 新会话先读本文件，再读 `docs/design-refs/`。

## 1. 仓库与发布

- 仓库：https://github.com/ichiyotsu/TokiNote （public，main）
- 最新发布：**0.2.6**（release keystore 签名，versionCode 4）；0.2.5、0.2.0、0.1.0 均在 Releases
- 版本规则：正式版 tag `x.y.z`；beta tag `x.y.z-beta.1`（Pre-release）；**发版前必须与用户核对版本号 + 文案**
- 包名固定 `com.tokinote.app`
- **CI 与 Release 工作流仍是红的**：GitHub runner 上 Gradle 解析 AGP 插件异常（本地完全正常）。
  发版方式固定为：本地 `assembleRelease`（带 KEYSTORE_* 环境变量）→ `gh release create <tag> app-release.apk --notes-file ...`

## 2. 签名密钥（重要）

- `D:\DSH\TokiNote\keys\release.jks|.b64|keystore-info.txt`
- 原始备份：`C:\Users\tokisaki\TokiNote-keys`（唯一离线备份，务必再拷一份外部介质）
- 一律不进 git；密码见 `keystore-info.txt`

## 3. 环境

- 无系统 git/gh：`tools\mingit\cmd\git.exe`、`tools\gh\bin\gh.exe`
- gh：`GH_CONFIG_DIR=tools\gh-config`、`LOCALAPPDATA=tools\gh-la`
- 构建：`$env:JAVA_HOME="D:\Application\Android Studio\jbr"; $env:ANDROID_HOME="C:\Users\tokisaki\AppData\Local\Android\Sdk"; .\gradlew.bat --offline assembleDebug`
- 模拟器 AVD `Pixel_10_Pro`（API 37）；**adb 手势注入对 Compose 不稳定**，长按/点击常无效，交互验证优先真机
- github.com:443 时不时机翻；`git push` 失败可改用 GitHub Git Data API

## 4. 技术栈（已全面升级）

- Kotlin **2.4.21**、Compose **1.12.1**、Material3 **1.5.0-beta02**、AGP 9.4.1、Gradle 9.6
- **minSdk 36（Android 16）**、compileSdk/targetSdk 37
- 新增依赖 `com.google.android.material:material:1.13.0`（Monet）
- Compose 动态色：`dynamicLight/DarkColorScheme`（SDK 31+）；传统 View：`TokiNoteApplication` + `DynamicColors.applyToActivitiesIfAvailable`；`themes.xml` 继承 `Theme.Material3.DynamicColors.DayNight.NoActionBar`

## 5. 已完成的功能

- 笔记：Journal 卡片（图左文右、置顶/日常分区）、长按预览（打开/归档/删除）、笔记内清单、日期/时间/标签合并面板、M3 日期与时间选择器、编辑页顶栏（关闭/删除/对勾）+ 顶部大图
- 相册：三列网格 + 全屏查看器（缩放、下载、定位、信息）
- 专注：波浪环、主按钮长胶囊 + 圆形重置 + 窄药丸辅助键、垂直居中、长按/双击进入不定时正向计时
- 统计（笔记页第三栏）：年/月/周切换；年无日历只列全年，月显示月历，周显示 7 天条；整页 LazyColumn 可滚动；点日期只看当天，点范围标题回到范围并取消高亮
- 设置：外观（主题模式、莫奈取色预设、纯黑、动态取色、显示时间、减弱动效）、数据（笔记数量/归档/回收站一组；备份/清缓存/存储占用/清空一组）、关于（版本、检查更新 + Release/Beta 下拉、GitHub）
- 归档/回收站：归档不进主页且不可编辑；回收站保留 14 天自动清理
- 底栏：HorizontalFloatingToolbar + ToggleButton；设置页独立搜索按钮 + 设置内搜索
- 图标：Material Symbols `article-shortcut`，`#1E3C38` 底

## 6. 待办（下一会话优先）

1. **首页分组**：顶栏 `全部 / 2026 / +`，`+` 弹出笔记多选加入分组；分组过滤列表（建议复用 labels 或新增 groups）
2. 核对预设/动态色下的背景与标签底色（本轮已给预设 surface 混色）
3. 发 **0.2.7**（日历修复、检查更新通道下拉、动态色/预设背景、设置搜索）
4. 真机验证长按预览、不定时专注
5. 未做：真模糊（顶栏/底栏 backdrop blur）、选中行独立字号

## 7. 已知坑

- PowerShell 5.1 写中文易乱码：含中文的 Kotlin 字符串优先用 `write`/`edit` 工具或 `[char]0xXXXX` 拼接
- 正则批量替换要确认没吞掉相邻 composable（本会话多次发生）
- 离线构建依赖已缓存；新增依赖需联网构建一次
