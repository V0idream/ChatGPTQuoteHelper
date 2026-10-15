# ChatGPT Text Selection Fix v0.2.0 (experimental)

This release adds a targeted LSPosed compatibility module to the existing ChatGPT Quote Helper project.

## Features

- Requests a first-level **Quote to GPT** action when selecting assistant response text in the ChatGPT Android app.
- Hides the built-in **Ask ChatGPT** selection action, while leaving Copy and Select all available.
- Sends selected text directly to the installed ChatGPT Quote Helper (`com.codex.chatgptquotehelper.debug`) using Android `ACTION_PROCESS_TEXT`.
- Does not modify ChatGPT's APK, intercept network traffic, or send chat messages.

## Installation

1. Install [ChatGPT Quote Helper v1.0.1](https://github.com/V0idream/ChatGPTQuoteHelper/releases/tag/v1.0.1) and configure its accessibility service.
2. Install `chatgpt-selection-fix-v0.2.0.apk` on a device with a functioning LSPosed setup.
3. Enable the module in LSPosed, scoped **only** to `com.openai.chatgpt`.
4. Restart ChatGPT and select a portion of an assistant reply. Tap **Quote to GPT**.

Android may place the item inside the overflow menu when horizontal space is limited. The module is intended for a specific text-selection implementation; compatibility with future versions is not guaranteed.

Test environment: OnePlus 13, Android 16 / ColorOS 16, ChatGPT 1.2026.279. Other versions were not thoroughly tested.

**SHA-256** (`chatgpt-selection-fix-v0.2.0.apk`):

```text
D6C17E0E84E398610D79277FAA2C48FCF30A71EF568289BDAC7B91E55D201D1D
```

The APK uses a debug signing certificate; third-party builds may have different signing certificates. The original Quote Helper source code remains unavailable; only the LSPosed module source is published here.
