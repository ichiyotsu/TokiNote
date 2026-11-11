# Release 规范（待确认）

## 版本

- 正式版：`X.Y.Z`，例如 `0.1.0`
- 测试版：`X.Y.Z-bN`，例如 `0.1.1-b1`
- 发布顺序：`0.1.0` → `0.1.1-b1` → `0.1.1-b2` → … → `0.1.1`
- 测试版可以领先正式版多个版本
- 同一核心版本里正式版更高：`0.1.1-b1` < `0.1.1`
- `versionCode` 只递增。0.3.7 的 `versionCode` 是 27

## Tag

- Android 用纯版本号：`0.3.7`。也可以用 `v` 前缀。带 `-` 的 tag 由 `.github/workflows/release.yml` 发成 prerelease，其余发成正式 release，并标成 Latest
- iOS / iPadOS 用 `ios-` 前缀：`ios-0.3.7`。工作流是 `.github/workflows/ios.yml`，不标成 Latest
- Windows 用 `windows-` 前缀：`windows-0.3.7`。工作流是 `.github/workflows/windows.yml`，不标成 Latest
- 应用内更新只认自己的前缀。Android 忽略 `ios-` 和 `windows-`
- 0.3.7 这一组是要保留的正式发布。发布时删除更早的 GitHub Release 和 tag

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
| TokiNote | `0.3.7`（Android versionCode 27；iOS build 2；Windows 0.3.7.0） | 无 | 已发布。标签 `0.3.7`、`ios-0.3.7`、`windows-0.3.7` |
| Luma | 无 | `0.1.0-beta1` | 已发布（历史 tag 例外） |
| TokiTodo | 无 | `0.1.0-beta.1` | 已发布 |

## 跨工作区同步

- TokiNote：已采用本规范
- Luma：已同步
- TokiTodo：已同步
