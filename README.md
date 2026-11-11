# tokinote

Tokinote 是 Android 上的本地笔记和专注计时应用，界面里的名字是 TokiNote。笔记、图片、标签、分组、相册、个人资料和专注记录写在本机。可以导出 JSON，导入或导出 DOCX 正文，也可以把一份快照写进用户选定的云盘文件夹。检查更新和可选的专注组件包会访问 GitHub。

应用 ID 是 `com.tokinote.app`。源码默认 `versionName` 0.2.8、`versionCode` 1。发布时用环境变量 `VERSION_NAME`、`VERSION_CODE` 覆盖。

| 平台 | 版本 |
| --- | --- |
| Android | `app/build.gradle.kts` 的 `versionName` 0.2.8，`versionCode` 1。调试包是 `app-debug.apk`。release 构建产出 `app-release.apk` 和 `app-release.aab`。 |

## 版本规则

`X.Y.Z`（如 `0.1.0`）是正式版。`X.Y.Z-bN`（如 `0.1.1-b1`）是测试版。发布顺序是 `0.1.0`，然后 `0.1.1-b1`、`0.1.1-b2`，直到 `0.1.1`。测试版版本号领先正式版几个是正常的。tag 加 `v` 前缀，例如 `v0.1.0`、`v0.1.1-b1`。发布工作流里，tag 含 `-b` 时发成 prerelease，其余发成正式 release。同一核心版本里测试版更旧：`0.1.1-b1` < `0.1.1`。`versionCode` 只递增。

## 已发布版本

已发布并仍保留的有 [0.1.0](https://github.com/ichiyotsu/Tokinote/releases/tag/0.1.0)、[0.3.5](https://github.com/ichiyotsu/Tokinote/releases/tag/0.3.5)（Latest）、[0.3.7-beta.2](https://github.com/ichiyotsu/Tokinote/releases/tag/0.3.7-beta.2)、[0.3.7-beta.3](https://github.com/ichiyotsu/Tokinote/releases/tag/0.3.7-beta.3)、[0.3.7-beta.4](https://github.com/ichiyotsu/Tokinote/releases/tag/0.3.7-beta.4)。这些 tag 没有 `v` 前缀。`0.3.7-beta.2`、`0.3.7-beta.3`、`0.3.7-beta.4` 不是 `X.Y.Z-bN`。按现在的规则，最接近的写法是 `0.3.7-b2`、`0.3.7-b3`、`0.3.7-b4`。已发布的 tag 和 Release 仍用原来的名字。

安装包文件名：`0.1.0` 是 `app-release.apk`。`0.3.5` 是 `TokiNote-0.3.5.apk`，同一次发布还有 `focus-component-1.0.zip`。`0.3.7-beta.2` 是 `TokiNote-0.3.7-beta.2.apk`。`0.3.7-beta.3` 是 `TokiNote-0.3.7-beta.3.apk`。`0.3.7-beta.4` 是 `TokiNote-0.3.7-beta.4.apk`。`HANDOFF.md` 记录 0.3.5 的 `versionCode` 是 22，0.3.7-beta.4 是 25。源码默认的 `versionCode` 仍是 1。

## 安装已发布的版本

从 [Releases](https://github.com/ichiyotsu/Tokinote/releases) 下载 APK，用系统安装器安装。应用内「检查更新」也可以下载 APK 并交给安装器。没有「安装未知应用」权限时会打开系统设置。

## 从源码构建

```bash
gradle testDebugUnitTest
gradle assembleDebug
gradle assembleRelease
```

仓库里有 `gradlew`，wrapper 指向 Gradle 9.6.0。CI 直接用预装 Gradle，不用 wrapper。runner 上 `downloads.gradle.org` 会拒绝连接，wrapper 拉不到 9.6.0。

`VERSION_NAME` 和 `VERSION_CODE` 不设时，用 0.2.8 和 1。`COMPILE_SDK`、`TARGET_SDK` 不设时，用 37。

签名钥匙不在这个仓库里。设置 `KEYSTORE_FILE`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD` 后，release 用该钥匙签名。没有 `KEYSTORE_FILE` 时，release 使用 debug 签名。

`.github/workflows/release.yml` 在推送 `v` 开头的 tag 时构建 `app-release.apk` 和 `app-release.aab`，并创建 GitHub Release。tag 里有 `-b` 则标成 prerelease，否则标成正式 release。说明文件读 `.github/release-notes/<版本号>.md`，没有文件时用一句占位说明。

## Android 说明

`minSdk` 是 31（Android 12）。`compileSdk` / `targetSdk` 默认 37。工作流安装 `platforms;android-37.0` 和 `build-tools;36.0.0`。

数据在 `TokiStore` 的 SharedPreferences JSON 里。回收站保留 14 天，启动时删掉过期笔记。归档笔记不进主页，也不能编辑。DOCX 只处理正文段落。云同步通过 SAF 读写用户选中文件夹里的 `tokinote-sync.json`，应用不登录网盘。

专注计时在 `FocusViewModel` 里用协程倒数，没有前台 Service。底栏的专注入口要先下载并安装 `focus-component-1.0.zip`（来自 [0.3.5](https://github.com/ichiyotsu/Tokinote/releases/tag/0.3.5)），再打开组件开关。未安装时不显示这个入口。

外观有 `MaterialExpressive` 和 `OneUI` 两套。`TokiFeatures.DESIGN_STYLE_SWITCH_ENABLED` 为 true，外观页可以切换。手机用底部导航。`smallestScreenWidthDp >= 600` 且横屏时，设置改用侧栏。

检查更新请求 `https://api.github.com/repos/ichiyotsu/Tokinote/releases`。Release 通道只匹配 `X.Y.Z`。Beta 通道同时看正式版和 `X.Y.Z-bN`，并仍认已经发布的 `X.Y.Z-beta`、`X.Y.Z-beta.N`，取更高的一个。同一核心版本的正式版高于测试版。`INTERNET` 用于这次检查，以及下载专注组件包。

## 许可

仓库根目录的 `LICENSE` 是 GNU GPL-3.0。应用图标是 Material Symbols 的 `article-shortcut`，Apache-2.0，说明在 `docs/icon-sources.md`。
