// Copyright 2026 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.androidbrowserhelper.trusted;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.browser.trusted.TrustedWebActivityIntent;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.TrustedWebUtils;
import androidx.browser.trusted.TrustedWebActivityIntentBuilder;

/**
 * A trampoline activity that handles Trusted Web Activity shortcuts.
 * It is defined as a noDisplay activity, meaning it finishes in {@link #onCreate}
 * before any layout is drawn.
 * <p>
 * When static shortcuts declared in {@code res/xml/shortcuts.xml} are invoked, Android's
 * {@code ShortcutService} automatically adds {@link Intent#FLAG_ACTIVITY_NEW_TASK} and
 * {@link Intent#FLAG_ACTIVITY_CLEAR_TASK} to the intent.
 * To prevent the system from destroying an already running TWA task (which uses the app's
 * default package {@code taskAffinity}), this activity declares {@code android:taskAffinity=""}
 * and {@code android:excludeFromRecents="true"} in the manifest.
 * <p>
 * On desktop environments (e.g. ChromeOS / Android PC), if no TWA task is running, a new task
 * rooted in the TWA package is started via {@link ColdShortcutActivity} (or a custom configured
 * activity) to prevent {@code DesktopModeCompatPolicy} translucent activity freezes and ensure
 * proper taskbar running-indicator attribution.
 * On mobile devices or when a TWA task is already running, the shortcut is routed directly via
 * {@link TwaLauncher}.
 */
public class ShortcutTrampolineActivity extends Activity {
    private static final String TAG = "ShortcutTrampoline";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Intent intent = getIntent();
            if (intent == null) {
                return;
            }

            Uri uri = intent.getData();
            if (uri == null) {
                return;
            }
            // Re-parse URI to prevent custom Parcelable Uri spoofing.
            uri = Uri.parse(uri.toString());

            LauncherActivityMetadata metadata = LauncherActivityMetadata.parse(this);
            if (!isTrusted(uri, metadata)) {
                Log.w(TAG, "Dropping untrusted shortcut URI: " + uri);
                return;
            }

            // Using getApplicationContext() is critical here because this Activity is going to
            // finish immediately, while the TwaLauncher will do asynchronous work (connecting
            // to Custom Tabs Service) and eventually launch the TWA.
            Context appContext = getApplicationContext();
            String coldShortcutClass = metadata.coldShortcutActivity;
            if (coldShortcutClass != null && coldShortcutClass.startsWith(".")) {
                coldShortcutClass = getPackageName() + coldShortcutClass;
            }
            Integer runningTaskId = findRunningTwaTaskId(
                    appContext, getTaskId(), metadata.launcherComponent, coldShortcutClass);

            PackageManager pm = appContext.getPackageManager();
            boolean isDesktop = ChromeOsSupport.isRunningOnArc(pm)
                    || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
                    && pm.hasSystemFeature(PackageManager.FEATURE_PC));

            if (isDesktop && runningTaskId == null) {
                // Cold launch on desktop: Start ColdShortcutActivity (or a custom configured
                // activity) in a new task. Because ColdShortcutActivity is opaque (Theme.NoTitleBar),
                // DesktopModeCompatPolicy does not trigger translucent exemptions, and the task is
                // rooted in the TWA package so the taskbar running-app indicator is correctly
                // attributed to the TWA icon.
                Intent coldLaunchIntent = new Intent();
                if (coldShortcutClass != null) {
                    coldLaunchIntent.setComponent(new ComponentName(this, coldShortcutClass));
                } else {
                    coldLaunchIntent.setClass(this, ColdShortcutActivity.class);
                }
                coldLaunchIntent.setData(uri);
                coldLaunchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(coldLaunchIntent);
                return;
            }

            Integer sessionId = SessionStore.makeSessionId(runningTaskId);
            TwaLauncher twaLauncher = new TwaLauncher(appContext, metadata.launchingBrowser, sessionId,
                    new SharedPreferencesTokenStore(appContext)) {
                @Override
                protected TrustedWebActivityIntent onPrepareIntent(TrustedWebActivityIntent intent) {
                    intent.getIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    return super.onPrepareIntent(intent);
                }
            };

            TrustedWebActivityIntentBuilder builder = new TrustedWebActivityIntentBuilder(uri);
            metadata.configureIntentBuilder(builder, appContext);

            twaLauncher.launch(
                    builder,
                    new QualityEnforcer(),
                    null /* splashScreenStrategy */,
                    () -> new Handler(Looper.getMainLooper()).post(twaLauncher::destroy),
                    new TwaLauncher.FallbackStrategy() {
                        @Override
                        public void launch(Context context, TrustedWebActivityIntentBuilder twaBuilder,
                                           @Nullable String providerPackage, @Nullable Runnable completionCallback) {
                            // Respect the metadata specified in the manifest instead of fallback.
                            if (metadata.launchingBrowser != null) {
                                Log.w(TAG, "Launching browser " + metadata.launchingBrowser + " is not available.");
                                if(completionCallback != null) {
                                    completionCallback.run();
                                }
                                return;
                            }

                            if ("webview".equalsIgnoreCase(metadata.fallbackStrategyType)) {
                                Intent fallbackIntent = WebViewFallbackActivity.createLaunchIntent(context,
                                        twaBuilder.getUri(), metadata);
                                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                try {
                                    context.startActivity(fallbackIntent);
                                } catch (ActivityNotFoundException e) {
                                    Log.e(TAG, "Failed to launch webview fallback: ", e);
                                }
                            } else {
                                // CustomTabs fallback
                                if (providerPackage == null) {
                                    providerPackage = CustomTabsClient.getPackageName(context, null);
                                }
                                CustomTabsIntent customTabsIntent = twaBuilder.buildCustomTabsIntent();
                                customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                if (providerPackage != null) {
                                    customTabsIntent.intent.setPackage(providerPackage);
                                }
                                if (ChromeOsSupport.isRunningOnArc(context.getPackageManager())) {
                                    customTabsIntent.intent.putExtra(TrustedWebUtils.EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY, true);
                                }
                                // Verify there is an app available to handle the intent before launching
                                customTabsIntent.intent.setData(twaBuilder.getUri());
                                if (customTabsIntent.intent.resolveActivity(context.getPackageManager()) != null) {
                                    context.startActivity(customTabsIntent.intent);
                                } else {
                                    Log.e(TAG, "No browser installed to handle Custom Tabs/Browser fallback.");
                                }
                            }
                            if (completionCallback != null) {
                                completionCallback.run();
                            }
                        }
                    }
            );

        } finally {
            // Must finish synchronously in onCreate() to satisfy android:noDisplay="true"
            finish();
        }
    }

    private static boolean isTrusted(Uri uri, LauncherActivityMetadata metadata) {
        if (uri == null) {
            return false;
        }
        if (metadata.defaultUrl != null) {
            Uri defaultUri = Uri.parse(metadata.defaultUrl);
            if (isSameOrigin(uri, defaultUri)) {
                return true;
            }
        }
        if (metadata.additionalTrustedOrigins != null) {
            for (String originStr : metadata.additionalTrustedOrigins) {
                Uri originUri = Uri.parse(originStr);
                if (isSameOrigin(uri, originUri)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isSameOrigin(Uri uri1, Uri uri2) {
        if (uri1 == null || uri2 == null) {
            return false;
        }
        String scheme1 = uri1.getScheme();
        String scheme2 = uri2.getScheme();
        String host1 = uri1.getHost();
        String host2 = uri2.getHost();
        if (scheme1 == null || scheme2 == null || host1 == null || host2 == null) {
            return false;
        }

        int port1 = uri1.getPort();
        int port2 = uri2.getPort();
        if (port1 == -1) {
            port1 = "https".equalsIgnoreCase(scheme1) ? 443 : ("http".equalsIgnoreCase(scheme1) ? 80 : -1);
        }
        if (port2 == -1) {
            port2 = "https".equalsIgnoreCase(scheme2) ? 443 : ("http".equalsIgnoreCase(scheme2) ? 80 : -1);
        }

        return scheme1.equalsIgnoreCase(scheme2) &&
                host1.equalsIgnoreCase(host2) &&
                port1 == port2;
    }

    private static @Nullable Integer findRunningTwaTaskId(Context context, int currentTaskId,
            @Nullable ComponentName twaLauncherComponent, @Nullable String coldShortcutClass) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return null;
        List<ActivityManager.AppTask> appTasks;
        try {
            appTasks = am.getAppTasks();
        } catch (Exception e) {
            return null;
        }
        if (appTasks == null) return null;
        for (ActivityManager.AppTask appTask : appTasks) {
            try {
                ActivityManager.RecentTaskInfo taskInfo = appTask.getTaskInfo();
                if (taskInfo == null || taskInfo.baseIntent == null) {
                    continue;
                }
                ComponentName component = taskInfo.baseIntent.getComponent();
                if (!isMatchingTwaComponent(
                        context, component, twaLauncherComponent, coldShortcutClass)) {
                    continue;
                }
                int taskId = taskInfo.id;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (taskInfo.taskId > 0) {
                        taskId = taskInfo.taskId;
                    }
                    if (taskId == currentTaskId || taskId <= 0) {
                        continue;
                    }
                    if (taskInfo.isRunning) {
                        return taskId;
                    }
                } else {
                    if (taskId == currentTaskId || taskId <= 0) {
                        continue;
                    }
                    return taskId;
                }
            } catch (IllegalArgumentException | SecurityException e) {
                // Ignore tasks that may no longer exist.
            }
        }
        return null;
    }

    private static boolean isMatchingTwaComponent(Context context,
            @Nullable ComponentName component, @Nullable ComponentName twaLauncherComponent,
            @Nullable String coldShortcutClass) {
        if (component == null) {
            return false;
        }
        String className = component.getClassName();

        // 1. Matches ColdShortcutActivity, or the custom activity configured via the
        // COLD_SHORTCUT_ACTIVITY metadata (i.e. a task started by a cold shortcut launch).
        if (ColdShortcutActivity.class.getName().equals(className)
                || className.equals(coldShortcutClass)) {
            return true;
        }

        // 2. Matches the resolved launcher component for this TWA
        if (twaLauncherComponent != null) {
            if (component.equals(twaLauncherComponent)) {
                return true;
            }
            // Handle activity-alias where baseIntent might refer to the target activity or alias
            try {
                PackageManager pm = context.getPackageManager();
                ActivityInfo info = pm.getActivityInfo(component, 0);
                if (info.targetActivity != null &&
                        info.targetActivity.equals(twaLauncherComponent.getClassName())) {
                    return true;
                }
                ActivityInfo launcherInfo = pm.getActivityInfo(twaLauncherComponent, 0);
                if (launcherInfo.targetActivity != null &&
                        launcherInfo.targetActivity.equals(className)) {
                    return true;
                }
            } catch (PackageManager.NameNotFoundException ignored) {}
        }

        // 3. Matches default LauncherActivity
        if (LauncherActivity.class.getName().equals(className)) {
            return true;
        }

        // 4. Fallback if launcher component was unresolvable: verify if class extends LauncherActivity
        if (twaLauncherComponent == null) {
            try {
                Class<?> cls = Class.forName(className, false, context.getClassLoader());
                if (LauncherActivity.class.isAssignableFrom(cls)) {
                    return true;
                }
            } catch (Throwable ignored) {}
        }

        return false;
    }
}
