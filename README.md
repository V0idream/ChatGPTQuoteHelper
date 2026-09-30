# ChatGPT Quote Helper

将 Android 文本选择菜单中选中的 ChatGPT 回复文字插入当前对话输入框的辅助工具。

## 下载与安装

从 [Releases](https://github.com/V0idream/ChatGPTQuoteHelper/releases/tag/v1.0.1) 下载 `ChatGPTQuoteHelper-v1.0.1-debug.apk`。

1. 安装 APK，打开 ChatGPT Quote Helper。
2. 在系统无障碍设置中启用该应用的服务。
3. 打开官方 ChatGPT Android 应用的当前对话。
4. 选中回复文字，在文本选择菜单或“更多”中选择“引用 GPT”。
5. 检查输入框中的引用，继续编辑并自行发送。

支持 Markdown、纯文本和自然语言引用格式。按钮位置由系统和目标应用决定，不能保证在菜单前排显示。

## 源码状态

**原始 Android Studio / Kotlin 源码已遗失。此仓库仅保存发行说明与现存 APK，不是可编译源码项目。**

本次未反编译 APK，也没有将重建代码冒充原始源码。将来如有重建项目，会明确标注其来源与验证状态。

## 版本与核验

| 项目 | 值 |
| --- | --- |
| APK 版本 | 1.0.1-debug |
| versionCode | 2 |
| 包名 | com.codex.chatgptquotehelper.debug |
| 最低 Android API | 28（Android 9） |
| targetSdk / compileSdk | 36 |
| 文件大小 | 2,473,467 bytes |
| APK 签名 | Android Debug 证书，v2 验证通过 |

SHA-256：

```text
1BE2BB3DA498ABC5CE110356CD92E63B6C98738F9249036E8C316CEF4EA1A27B
```

2026-09-30 对用户保留的 APK 执行了 `aapt dump badging`、`aapt dump permissions` 和 `apksigner verify`。文件哈希与此前交付的 v1.0.1 一致；Manifest 未声明任何 uses-permission，包括 INTERNET。

该版本设计使用 PROCESS_TEXT 与 AccessibilityService 完成本地输入框辅助，并由用户自行发送。此次发布未进行真机运行验证；Android 15/16 与当前 ChatGPT 稳定版的菜单、输入框定位和已有草稿保留行为仍需实际测试。

这是 Debug 安装包，未提供生产签名版本。无联网权限的核验不等同于完整代码审计。
