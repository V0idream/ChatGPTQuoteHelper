# ChatGPT Text Selection Fix

Experimental LSPosed compatibility module for the official ChatGPT Android application (`com.openai.chatgpt`). It restores a direct **Quote to GPT** text-selection action for use with the separate [ChatGPT Quote Helper](https://github.com/V0idream/ChatGPTQuoteHelper).

## What it does

When a user selects part of an assistant reply, the module attempts to:

1. Hook the standard Android `View.startActionMode(...)` selection callbacks inside ChatGPT.
2. Hide the built-in **Ask ChatGPT** action, if it is present in the intercepted menu.
3. Insert **Quote to GPT**, requesting placement in the first-level floating toolbar.
4. On tap, extract only the current selection and pass it through `ACTION_PROCESS_TEXT` to `com.codex.chatgptquotehelper.debug/com.codex.chatgptquotehelper.ProcessTextActivity`.

The Android floating toolbar can still move the action into overflow (`...`) if there is insufficient space. The module does not change other selection actions, such as Copy and Select all.

The code first tries to read selection ranges from `TextView` / view accessibility metadata. If those are unavailable, it attempts to capture the result of the app's existing Copy action synchronously, without intentionally modifying the system clipboard. This fallback is experimental and may depend on the ChatGPT renderer.

### Fix in v0.2.1: Return to ChatGPT automatically

Earlier builds could start the text processor with `FLAG_ACTIVITY_NEW_TASK` if a floating toolbar's view held a wrapped `Context` rather than a direct Android `Activity`. Android could then resume an existing Quote Helper task, leaving its main page visible even after the temporary text processor called `finish()`.

v0.2.1 unwraps `ContextWrapper` chains to locate the originating ChatGPT `Activity`, falling back to a weak reference to the most recently resumed ChatGPT `Activity` when necessary. It starts `ProcessTextActivity` from that `Activity` **without** `NEW_TASK`, preserving the expected return to ChatGPT. If no viable ChatGPT activity can be found, it skips the unsafe launch. This behavior was confirmed on the user's device.

## Requirements

- Android 12L+ (API 32+) and an LSPosed-compatible rooted environment.
- Official ChatGPT Android application (`com.openai.chatgpt`).
- **ChatGPT Quote Helper 1.0.1-debug**, installed separately (or a compatible build retaining the same package name and Activity component).

Tested with ChatGPT Android **1.2026.279** on OnePlus 13 / Android 16 / ColorOS 16, including the v0.2.1 return-to-chat behavior. Other versions have not been comprehensively tested.

## Install

1. Install the APK attached to [Text Selection Fix v0.2.1](https://github.com/V0idream/ChatGPTQuoteHelper/releases/tag/selection-fix-v0.2.1).
2. In LSPosed, enable this module with **only ChatGPT** (`com.openai.chatgpt`) in its scope.
3. Force-stop and restart ChatGPT.
4. Select text in an assistant response and tap **Quote to GPT**.

Do **not** grant ADB Shell superuser access. APK installation through ordinary `adb install` does not require `su` or `adb root`. LSPosed must already be installed and managed separately.

To undo the change, disable this module for ChatGPT and restart the ChatGPT app. Neither component sends messages automatically.

## Build from source

Requires JDK 17+, Android SDK Platform 35, Android SDK Build Tools 35.0.0, and (for the Gradle route) Gradle 8.13 or a compatible Gradle version.

With a configured Android SDK, run:

```powershell
gradle :app:assembleDebug
```

Alternatively, on Windows, build without Android Gradle Plugin:

```powershell
powershell -ExecutionPolicy Bypass -File tools/build-standalone.ps1
```

The standalone script downloads the **compile-only** Xposed API 82 JAR from an Aliyun Maven mirror if absent and checks its SHA-256. Android, D8, and signing tools come from the installed SDK. `research/` and the local debug signing key are excluded from version control.

Build outputs:

- Gradle: `app/build/outputs/apk/debug/app-debug.apk`
- Standalone: `app/build/standalone/chatgpt-selection-fix-debug.apk`

Locally generated debug APKs normally use a **different signing certificate** from the published APK. To upgrade an existing installation without uninstalling it, use a build signed by the same key as the installed module.

## Privacy and limitations

- No Internet, Accessibility Service, or storage permissions are requested by this module.
- No chat contents or selected text are written to logs. Only generic events and selected-text length may be logged for diagnostics.
- No ChatGPT APK modification, authentication hooks, network interception, or automated sending.
- A custom selection renderer that never starts Android `ActionMode` may bypass these hooks.
- This build is experimental; the user has confirmed normal quoting and automatic return to ChatGPT on their device, but full cross-device regression testing has not been performed.

For the original Quote Helper APK archive, see the [repository root README](../README.md). The helper's original Kotlin source code is not included because it has been lost.
