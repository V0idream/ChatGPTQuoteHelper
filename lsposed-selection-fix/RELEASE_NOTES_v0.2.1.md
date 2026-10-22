# ChatGPT Text Selection Fix v0.2.1

This release corrects the text processor's Android task-stack behavior, fixing a navigation bug where tapping **Quote to GPT** opened Quote Helper and left its main activity on screen. The original user tested the fix on-device and confirmed quoting now works normally without manually returning to ChatGPT.

## Changes since v0.2.0

- Unwrap `ContextWrapper` instances to find the actual ChatGPT `Activity` associated with the selected text.
- Track the most recently resumed ChatGPT `Activity` as a weak-reference fallback when a selection view does not retain its host Activity.
- Start Quote Helper's `ProcessTextActivity` from the ChatGPT Activity, **without** adding `FLAG_ACTIVITY_NEW_TASK`.
- Abort a quote action safely rather than launching into an unrelated task when no valid caller Activity is available.
- Preserve **Quote to GPT**, hiding the redundant **Ask ChatGPT** menu action, and retaining Copy and Select all.

## Installation

1. Install [ChatGPT Quote Helper v1.0.1](https://github.com/V0idream/ChatGPTQuoteHelper/releases/tag/v1.0.1), or a compatible newer build retaining `com.codex.chatgptquotehelper.debug/com.codex.chatgptquotehelper.ProcessTextActivity`. Configure its accessibility service.
2. Install `chatgpt-selection-fix-v0.2.1.apk` over v0.2.0. The APK uses the same signing certificate as the published v0.2.0 module, so an in-place upgrade is supported.
3. In LSPosed, enable the module scoped **only** to ChatGPT (`com.openai.chatgpt`).
4. Force-stop and restart ChatGPT, select text in an assistant reply, and tap **Quote to GPT**. The quote should be inserted without having to return from Quote Helper manually.

Tested on OnePlus 13, Android 16 / ColorOS 16, ChatGPT Android 1.2026.279. Other versions and devices may differ. No Root permission needs to be granted to ADB Shell to install the APK; the existing LSPosed environment handles the module.

**SHA-256** (`chatgpt-selection-fix-v0.2.1.apk`):

```text
441DC8DB6088159DF7A558113B23B9C1FCC7B5D1747811FAE9E57A41A4F784F4
```

The original Quote Helper is a separate app. This release only updates the LSPosed selection fix module.
