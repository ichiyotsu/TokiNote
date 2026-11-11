# Release 规范（待确认）

## 版本

- 正式版：`X.Y.Z`，例如 `0.1.0`
- 测试版：`X.Y.Z-bN`，例如 `0.1.1-b1`
- 发布顺序：`0.1.0` → `0.1.1-b1` → `0.1.1-b2` → … → `0.1.1`
- 测试版可以领先正式版多个版本
- 同一核心版本里正式版更高：`0.1.1-b1` < `0.1.1`
- `versionCode` 只递增
- 已发布版本保留，不删除

## Tag

- tag 使用 `v` 前缀：`v0.1.0`、`v0.1.1-b1`
- 含 `-b` 的 tag 由 `.github/workflows/release.yml` 发成 prerelease，其余发成正式 release
- 已经发布的 tag 保持原名：`0.1.0`、`0.3.5`、`0.3.7-beta.2`、`0.3.7-beta.3`、`0.3.7-beta.4`
- `0.3.7-beta.N` 最接近的合规写法是 `0.3.7-bN`。这些 tag 不改
- Release 标题只写版本：`0.3.5`、`0.3.7 Beta 4`

## 安装包

- 已发布的 Android 包：`TokiNote-0.3.5.apk`、`TokiNote-0.3.7-beta.2.apk`、`TokiNote-0.3.7-beta.3.apk`、`TokiNote-0.3.7-beta.4.apk`。`0.1.0` 的文件是 `app-release.apk`。`0.3.5` 另有 `focus-component-1.0.zip`。
- `.github/workflows/release.yml` 上传 `app-release.apk` 和 `app-release.aab`，不改文件名。

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
| TokiNote | `0.3.5`（versionCode 22） | `0.3.7-beta.4`（versionCode 25；新测试版用 `X.Y.Z-bN`） | 已发布 |
| Luma | 无 | `0.1.0-beta1` | 已发布（历史 tag 例外） |
| TokiTodo | 无 | `0.1.0-beta.1` | 已发布 |

## 跨工作区同步

- TokiNote：已采用本规范
- Luma：已同步
- TokiTodo：已同步
