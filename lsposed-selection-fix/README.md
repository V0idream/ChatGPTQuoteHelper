# ChatGPT Text Selection Fix

Experimental, ChatGPT-only LSPosed module for Android's `PROCESS_TEXT` menu.

## Installation / scope

1. Install `app/build/standalone/chatgpt-selection-fix-debug.apk` (already built and signed), or the equivalent Gradle Debug APK from `app/build/outputs/apk/debug/app-debug.apk`. Normal ADB app installation is sufficient; **no ADB root**.
2. In the LSPosed user interface, enable the module and set its scope to **ChatGPT** (`com.openai.chatgpt`) only.
3. Force-stop/restart ChatGPT *through the normal Android UI*, then select a portion of an assistant response.
4. Look for `Quote to GPT` directly on the floating selection toolbar, or inside overflow (`⋮`) if Android cannot fit it. The module also hides the `Ask ChatGPT` action from this toolbar to make room.
5. Tap it to launch the installed quote helper directly (`com.codex.chatgptquotehelper.debug/com.codex.chatgptquotehelper.ProcessTextActivity`) with the selected text.

The module works only if ChatGPT starts a standard Android `ActionMode`. On the tested ChatGPT build, this hook successfully restored an overflow action; the new release requests first-level placement and direct helper invocation. Android may still move it into overflow depending on available space.

## Privacy and compatibility

The module does not request network, storage, accessibility or root permissions. It does not modify ChatGPT's APK or private files. It intercepts a synthetic, synchronous Copy action only when the user explicitly taps `Quote to GPT`, so that it can forward the selected text to the preinstalled quote helper without intentionally changing the system clipboard. No selected text is logged.

If selecting `Quote to GPT` does not launch the helper, collect only module diagnostic messages from LSPosed logs. Do not provide a full logcat dump containing private content.

This is an experimental first build; it has not been validated on all ChatGPT text renderers. Disable its LSPosed scope to revert to the app's original behavior.

## Build

Requires JDK 17+, Android SDK Platform 35 and Android Gradle Plugin 8.7.3.

The bridge dependency is `de.robv.android.xposed:api:82`, used at compile time only. The Gradle build retrieves it from the configured Maven repositories; the standalone build expects `research/xposed-api-82.jar` (download this dependency separately). It must **not** be bundled into the APK.

Run `gradle :app:assembleDebug` from the project root.

If Gradle cannot download plugins, an independent Windows SDK build is also
available: `powershell -ExecutionPolicy Bypass -File tools/build-standalone.ps1`.
Its output is `app/build/standalone/chatgpt-selection-fix-debug.apk`.

## Distribution

The tested debug APK is archived at [`releases/ChatGPT-Text-Selection-Fix-v0.1.0-debug.apk`](releases/ChatGPT-Text-Selection-Fix-v0.1.0-debug.apk).

The module is a companion to [ChatGPT Quote Helper](../README.md); it does not contain or replace the helper app. It targets the exact helper package and activity shown above.
