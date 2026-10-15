# ChatGPT Quote Helper

将 Android 文本选择菜单中选中的 ChatGPT 回复文字插入当前对话输入框的辅助工具。

## 配套项目：ChatGPT Text Selection Fix（LSPosed）

ChatGPT Android 部分版本的回复正文文字选择菜单不再显示第三方 `PROCESS_TEXT` 入口，导致本工具无法直接从回复中调用。为此，本仓库同时提供一个独立的 LSPosed 兼容模块。

两个组件分工如下：

| 组件 | 功能 | 依赖 |
| --- | --- | --- |
| **ChatGPT Quote Helper** | 接收选中文字，插入当前对话输入框 | 无障碍服务 |
| **ChatGPT Text Selection Fix** | 在 ChatGPT 回复文字选择工具栏中显示 **Quote to GPT**，隐藏原生 **Ask ChatGPT**，直接启动 Quote Helper | 已配置的 LSPosed 环境 |

**配套模块最新实验版：** [Text Selection Fix v0.2.0（GitHub Release 下载 APK）](https://github.com/V0idream/ChatGPTQuoteHelper/releases/tag/selection-fix-v0.2.0)。此前存放在仓库中的[早期调试包](lsposed-selection-fix/releases/ChatGPT-Text-Selection-Fix-v0.1.0-debug.apk)文件名带 `v0.1.0`，但 APK 内的 `versionName` 实际为 `0.2.0`；为避免混淆，建议使用 Release 附件。

**模块源码与构建说明：** [`lsposed-selection-fix/`](lsposed-selection-fix/)

安装顺序：先安装并配置 ChatGPT Quote Helper，再安装 LSPosed 模块，在 LSPosed 中启用模块且仅勾选 `com.openai.chatgpt` 作为作用域，最后强制停止并重新打开 ChatGPT。选中模型回复文字后点击 **Quote to GPT**。系统仍可能在工具栏宽度不足时将其收进溢出菜单。

模块已在 Android 16 / ColorOS 16、ChatGPT `1.2026.279` 上进行初步手动测试：菜单入口能够出现，引用工具可调用。该模块使用显式组件名调用 `com.codex.chatgptquotehelper.debug/com.codex.chatgptquotehelper.ProcessTextActivity`，目前仅兼容这一包名的引用工具版本；不适用于所有第三方文字处理应用。**模块属于实验版本**，不同 ChatGPT 版本可能失效。

两款 APK 都是 Debug 签名版本。LSPosed 模块的完整 Java 源码已提供，已通过 SDK 独立构建和 Gradle `assembleDebug` 编译验证；下文所述“原始源码已遗失”仅针对旧版 ChatGPT Quote Helper。模块 v0.2.0 的 SHA-256 参见 [校验文件](lsposed-selection-fix/SHA256SUMS.txt)。

## 下载与安装

从 [Releases](https://github.com/V0idream/ChatGPTQuoteHelper/releases/tag/v1.0.1) 下载 `ChatGPTQuoteHelper-v1.0.1-debug.apk`。

1. 安装 APK，打开 ChatGPT Quote Helper。
2. 在系统无障碍设置中启用该应用的服务。
3. 打开官方 ChatGPT Android 应用的当前对话。
4. 选中回复文字，在文本选择菜单或“更多”中选择“引用 GPT”。
5. 检查输入框中的引用，继续编辑并自行发送。

支持 Markdown、纯文本和自然语言引用格式。按钮位置由系统和目标应用决定，不能保证在菜单前排显示。

## 源码状态

**原 ChatGPT Quote Helper 的 Android Studio / Kotlin 源码已遗失，只有现存 APK 与发行说明。** 新增的 [LSPosed 修复模块](lsposed-selection-fix/)提供完整源码和可编译项目，两者的源码状态应分别理解。

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
