package dev.voidream.chatgptselectionfix;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.app.Activity;
import android.graphics.Rect;
import android.text.Selection;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * ChatGPT-only compatibility layer for standard Android floating selection toolbars.
 * This module does not read clipboard contents outside an explicit user action.
 */
public final class SelectionFix implements IXposedHookLoadPackage {
    private static final String TARGET_PACKAGE = "com.openai.chatgpt";
    private static final String TAG = "ChatGPTSelectionFix";
    private static final int MENU_ID = 0x6F11F001;
    // Verified from the installed helper's ACTION_PROCESS_TEXT intent filter.
    private static final String HELPER_PACKAGE = "com.codex.chatgptquotehelper.debug";
    private static final String HELPER_ACTIVITY = "com.codex.chatgptquotehelper.ProcessTextActivity";
    private static final ThreadLocal<Capture> ACTIVE_CAPTURE = new ThreadLocal<>();
    private static volatile WeakReference<Activity> lastResumedActivity = new WeakReference<>(null);

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam param) {
        if (!TARGET_PACKAGE.equals(param.packageName)) return;

        hookSelectionToolbar(ActionMode.Callback.class, false);
        hookSelectionToolbar(ActionMode.Callback.class, true);
        hookForegroundActivity();
        hookClipboardCapture();
        log("Loaded in ChatGPT process; waiting for ActionMode callbacks");
    }

    private static void hookForegroundActivity() {
        try {
            XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Activity activity = (Activity) param.thisObject;
                    if (TARGET_PACKAGE.equals(activity.getPackageName())) {
                        lastResumedActivity = new WeakReference<>(activity);
                    }
                }
            });
        } catch (Throwable e) {
            log("Unable to track foreground Activity: " + e.getClass().getSimpleName());
        }
    }

    private static Activity findCallerActivity(View view) {
        Context context = view.getContext();
        // Compose and floating toolbars may wrap their Activity in multiple
        // ContextWrappers. The raw instanceof Activity check is insufficient.
        for (int i = 0; i < 20 && context != null; i++) {
            if (context instanceof Activity) {
                Activity activity = (Activity) context;
                if (!activity.isFinishing() && !activity.isDestroyed()) return activity;
                break;
            }
            if (!(context instanceof ContextWrapper)) break;
            Context next = ((ContextWrapper) context).getBaseContext();
            if (next == context) break;
            context = next;
        }
        // Views created with application context may not retain an Activity.
        // Use the most recently resumed ChatGPT Activity as the caller instead.
        Activity resumed = lastResumedActivity.get();
        return resumed != null && !resumed.isFinishing() && !resumed.isDestroyed()
                ? resumed : null;
    }

    private static void hookSelectionToolbar(Class<?> callbackType, boolean typed) {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (!(param.args[0] instanceof ActionMode.Callback)) return;
                if (param.args[0] instanceof DelegatingCallback) return;
                View view = (View) param.thisObject;
                param.args[0] = new DelegatingCallback((ActionMode.Callback) param.args[0], view);
            }
        };

        try {
            if (typed) {
                XposedHelpers.findAndHookMethod(View.class, "startActionMode", callbackType, int.class, hook);
            } else {
                XposedHelpers.findAndHookMethod(View.class, "startActionMode", callbackType, hook);
            }
        } catch (Throwable e) {
            log("Failed to hook startActionMode overload: " + e.getClass().getSimpleName());
        }
    }

    private static void hookClipboardCapture() {
        try {
            XposedHelpers.findAndHookMethod(ClipboardManager.class, "setPrimaryClip", ClipData.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        Capture capture = ACTIVE_CAPTURE.get();
                        if (capture == null) return;
                        ClipData data = (ClipData) param.args[0];
                        if (data != null && data.getItemCount() > 0) {
                            capture.value = data.getItemAt(0).coerceToText(capture.context);
                            // Suppress only this deliberate, synchronous synthetic Copy.
                            param.setResult(null);
                        }
                    }
                });
        } catch (Throwable e) {
            log("Clipboard capture unavailable: " + e.getClass().getSimpleName());
        }
    }

    private static final class Capture {
        final Context context;
        CharSequence value;
        Capture(Context context) { this.context = context; }
    }

    private static final class DelegatingCallback extends ActionMode.Callback2 {
        private final ActionMode.Callback original;
        private final WeakReference<View> source;

        DelegatingCallback(ActionMode.Callback original, View source) {
            this.original = original;
            this.source = new WeakReference<>(source);
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            boolean result = original.onCreateActionMode(mode, menu);
            if (result) {
                applyMenuChanges(menu);
                log("onCreateActionMode: modified menu, items=" + menu.size());
            }
            return result;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            boolean result = original.onPrepareActionMode(mode, menu);
            return applyMenuChanges(menu) || result;
        }

        private static boolean applyMenuChanges(Menu menu) {
            boolean changed = false;
            // Hide just the stock ChatGPT action, without removing items by
            // potentially shared menu IDs or touching Copy / Select all.
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                if (item.getItemId() == MENU_ID) continue;
                if (isAskChatGPTAction(item) && item.isVisible()) {
                    item.setVisible(false);
                    changed = true;
                    log("Hidden native Ask ChatGPT action");
                }
            }
            if (menu.findItem(MENU_ID) == null) {
                MenuItem quote = menu.add(Menu.NONE, MENU_ID, 0, "Quote to GPT");
                quote.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
                changed = true;
            }
            return changed;
        }

        private static boolean isAskChatGPTAction(MenuItem item) {
            CharSequence title = item.getTitle();
            if (title != null && "Ask ChatGPT".equalsIgnoreCase(title.toString().trim())) {
                return true;
            }
            // Narrow fallback for intent-backed variants of the native action.
            Intent intent = item.getIntent();
            ComponentName component = intent == null ? null : intent.getComponent();
            return component != null && TARGET_PACKAGE.equals(component.getPackageName())
                    && (TARGET_PACKAGE + ".TextProcessorActivity").equals(component.getClassName());
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() != MENU_ID) return original.onActionItemClicked(mode, item);
            View view = source.get();
            if (view == null) return true;
            try {
                CharSequence selection = selectedTextFromView(view);
                if (selection == null || selection.length() == 0) {
                    selection = captureUsingCopy(mode, view.getContext());
                }
                if (selection != null && selection.length() > 0) {
                    Activity caller = findCallerActivity(view);
                    if (caller == null) {
                        // Never introduce NEW_TASK here: it can switch to an
                        // already-running Quote Helper task and leave its
                        // MainActivity visible after the receiver finishes.
                        log("No usable ChatGPT Activity; skipping unsafe NEW_TASK launch");
                        return true;
                    }
                    Intent process = new Intent(Intent.ACTION_PROCESS_TEXT)
                            .setComponent(new ComponentName(HELPER_PACKAGE, HELPER_ACTIVITY))
                            .setType("text/plain")
                            .putExtra(Intent.EXTRA_PROCESS_TEXT, selection.toString())
                            .putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true);
                    caller.startActivity(process);
                    log("Launched quote helper in caller task; selected text length=" + selection.length());
                } else {
                    log("Could not recover selected text; no action launched");
                }
            } catch (Throwable t) {
                log("Process-text action failed: " + t.getClass().getSimpleName());
            }
            return true;
        }

        private CharSequence captureUsingCopy(ActionMode mode, Context context) {
            Menu menu = mode.getMenu();
            MenuItem copy = menu.findItem(android.R.id.copy);
            if (copy == null) {
                for (int i = 0; i < menu.size(); i++) {
                    MenuItem candidate = menu.getItem(i);
                    CharSequence title = candidate.getTitle();
                    if (title != null && ("copy".equalsIgnoreCase(title.toString().trim())
                            || "复制".equals(title.toString().trim()))) {
                        copy = candidate;
                        break;
                    }
                }
            }
            if (copy == null) {
                log("No existing Copy item found in ActionMode");
                return null;
            }

            Capture capture = new Capture(context);
            ACTIVE_CAPTURE.set(capture);
            try {
                original.onActionItemClicked(mode, copy);
                return capture.value;
            } finally {
                ACTIVE_CAPTURE.remove();
            }
        }

        private static CharSequence selectedTextFromView(View view) {
            if (view instanceof TextView) {
                CharSequence text = ((TextView) view).getText();
                int start = Selection.getSelectionStart(text);
                int end = Selection.getSelectionEnd(text);
                if (start >= 0 && end > start && end <= text.length()) {
                    return text.subSequence(start, end);
                }
            }
            try {
                AccessibilityNodeInfo info = view.createAccessibilityNodeInfo();
                if (info != null) {
                    try {
                        CharSequence text = info.getText();
                        int start = info.getTextSelectionStart();
                        int end = info.getTextSelectionEnd();
                        if (text != null && start >= 0 && end > start && end <= text.length()) {
                            return text.subSequence(start, end);
                        }
                    } finally {
                        info.recycle();
                    }
                }
            } catch (Throwable ignored) { /* Selection info may not be exposed by Compose. */ }
            return null;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            original.onDestroyActionMode(mode);
        }

        @Override
        public void onGetContentRect(ActionMode mode, View view, Rect outRect) {
            if (original instanceof ActionMode.Callback2) {
                ((ActionMode.Callback2) original).onGetContentRect(mode, view, outRect);
            } else {
                super.onGetContentRect(mode, view, outRect);
            }
        }
    }

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }
}
