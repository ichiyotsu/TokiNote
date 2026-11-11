# Release 规范（待确认）

## 版本

- 正式版：`X.Y.Z`，例如 `0.1.0`
- 测试版：`X.Y.Z-bN`，例如 `0.1.1-b1`
- 发布顺序：`0.1.0` → `0.1.1-b1` → `0.1.1-b2` → … → `0.1.1`
- 测试版可以领先正式版多个版本
- 同一核心版本里正式版更高：`0.1.1-b1` < `0.1.1`
- `versionCode` 只递增。0.3.7 的 `versionCode` 是 27

## Tag

- 三个平台共用纯版本号标签：`0.3.7`。也可以用 `v` 前缀。带 `-` 的 tag 由 `.github/workflows/release.yml` 发成 prerelease，其余发成正式 release，并标成 Latest
- 同一次 Release 放上 Android APK / AAB、`focus-component-1.0.zip`、Windows `-win-x64.zip` 和 iOS IPA
- 应用内更新都读这个纯版本号。Android 只拿 `.apk`，Windows 只拿 `-win-x64.zip`，iOS 只拿 `.ipa`。没有对应文件的发布会被跳过

## 安装包

- Android：`TokiNote-0.3.7.apk`（另有 `TokiNote-0.3.7.aab`）
- iOS：`TokiNote-ios-0.3.7.ipa`（未签名）
- Windows：`TokiNote-Windows-0.3.7-win-x64.zip`

## Release Notes

```markdown
# 新增功能

## I. 功能名称

一句话说明用户现在能做什么。

## II. 功能名称

一句话说明用户现在能做什么。

# 设计特性

## I. 设计特性名称

一句话说明界面、动效或交互的变化。
```

规则：

- 只写用户能感知的内容
- 不写内部实现、提交记录、构建过程
- 功能用「新增功能」，视觉与交互用「设计特性」
- 没有对应内容时写「无」，不硬凑条目

## 当前版本

| 项目 | 正式版 | 测试版 | 状态 |
| --- | --- | --- | --- |
| TokiNote | `0.3.7`（Android versionCode 27；iOS build 2；Windows 0.3.7.0） | 无 | 已发布。三个平台共用标签 `0.3.7` |
| Luma | 无 | `0.1.0-beta1` | 已发布（历史 tag 例外） |
| TokiTodo | 无 | `0.1.0-beta.1` | 已发布 |

## 跨工作区同步

- TokiNote：已采用本规范
- Luma：已同步
- TokiTodo：已同步
