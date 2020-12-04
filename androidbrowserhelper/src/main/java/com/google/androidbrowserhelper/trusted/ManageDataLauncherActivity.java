// Copyright 2018 Google Inc. All Rights Reserved.
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

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import static androidx.browser.customtabs.CustomTabsService.RELATION_HANDLE_ALL_URLS;
import static androidx.browser.customtabs.CustomTabsService.TRUSTED_WEB_ACTIVITY_CATEGORY;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.androidbrowserhelper.R;

import java.util.Collections;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsService;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;

/**
 * A convenience class for routing "Manage Space" clicks in the settings of apps hosting Trusted Web
 * Activities into a web browser's site settings.
 *
 * To use this activity you need to:
 * 1) Add it to the manifest. You might want to set a transparent theme to avoid seeing a white
 * background while the activity is launched (see the recommended theme in the javadoc for
 * {@link LauncherActivity}).
 * 2) Set your app's default url in the MANAGE_SPACE_URL metadata.
 * The provided url must belong to the origin associated with your app via the Digital Asset Links.
 * When the app has multiple origins associated with it, the providers show settings for all those
 * origins. Legacy versions of Chrome use MANAGE_SPACE_URL to pick the origin to show settings for.
 * 3) Specify this activity in manageSpaceActivity attribute of the application in the manifest [1].
 *
 * [1] https://developer.android.com/guide/topics/manifest/application-element#space
 */
public class ManageDataLauncherActivity extends Activity {
    private static final String TAG = "ManageDataLauncher";

    private static final String METADATA_MANAGE_SPACE_DEFAULT_URL =
            "android.support.customtabs.trusted.MANAGE_SPACE_URL";

    // TODO: move to AndroidX.
    public static final String ACTION_MANAGE_TRUSTED_WEB_ACTIVITY_DATA =
            "android.support.customtabs.action.ACTION_MANAGE_TRUSTED_WEB_ACTIVITY_DATA";

    public static final String SITE_SETTINGS_SHORTCUT_ID =
            "android.support.customtabs.action.SITE_SETTINGS_SHORTCUT";

    public static final String CATEGORY_LAUNCH_SITE_SETTINGS =
            "androidx.browser.trusted.category.LaunchSiteSettings";

    public static final String OVERRIDE_IC_SITE_SETTINGS_ID =
            "drawable/override_ic_site_settings";

    @Nullable
    private String mProviderPackage;

    @Nullable
    private CustomTabsServiceConnection mConnection;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mProviderPackage = new TwaSharedPreferencesManager(this).readLastLaunchedProviderPackageName();
        if (mProviderPackage == null) {
            handleTwaNeverLaunched();
            return;
        }
        if (!supportsTrustedWebActivities(mProviderPackage)) {
            handleNoSupportForManageSpace();
            return;
        }
        View loadingView = createLoadingView();
        if (loadingView != null) {
            setContentView(loadingView);
        }

        if (ChromeLegacyUtils.supportsManageSpaceWithoutWarmupAndValidation(getPackageManager(),
                mProviderPackage)) {
            mConnection = new Connection();
        } else {
            mConnection = new LegacyChromeConnection();
        }
        CustomTabsClient.bindCustomTabsService(this, mProviderPackage, mConnection);
    }

    /**
     * Returns the default url of the page for which the settings will be shown.
     * By default uses the url provided in metadata.
     * If null is returned, legacy Chrome versions may fail to show the settings.
     */
    @Nullable
    protected Uri getDefaultUrlForManagingSpace() {
        try {
            ActivityInfo info = getPackageManager()
                    .getActivityInfo(getComponentName(), PackageManager.GET_META_DATA);

            if (info.metaData != null
                    && info.metaData.containsKey(METADATA_MANAGE_SPACE_DEFAULT_URL)) {
                Uri uri = Uri.parse(info.metaData.getString(METADATA_MANAGE_SPACE_DEFAULT_URL));
                Log.d(TAG, "Using clean-up URL from Manifest (" + uri + ").");
                return uri;
            }
        } catch (PackageManager.NameNotFoundException e) {
            // Should never happen.
            onError(new RuntimeException(e));
        }
        return null;
    }

    /**
     * Override to customize loading view, or return null if not needed.
     */
    @Nullable
    protected View createLoadingView() {
        ProgressBar progressBar = new ProgressBar(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        progressBar.setLayoutParams(params);
        FrameLayout layout = new FrameLayout(this);
        layout.addView(progressBar);
        return layout;
    }

    /**
     * Override to implement custom error handling.
     */
    protected void onError(RuntimeException e) {
        throw e;
    }

    /**
     * Called if a TWA was never launched (the app itself may have been launched though).
     * The default behavior is to show a toast about not having browsing data.
     * Override to implement a different behavior.
     */
    protected void handleTwaNeverLaunched() {
        Toast.makeText(this, getString(R.string.manage_space_no_data_toast), Toast.LENGTH_LONG)
                .show();
        finish();
    }

    /**
     * Called if a TWA provider doesn't support manage space feature. The default behavior is to
     * show a toast telling the user where the data is stored. Override to implement other behavior.
     */
    protected void handleNoSupportForManageSpace() {
        String appName;
        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo(mProviderPackage, 0);
            appName = getPackageManager().getApplicationLabel(info).toString();
        } catch (PackageManager.NameNotFoundException e) {
            appName = mProviderPackage;
        }

        Toast.makeText(this, getString(R.string.manage_space_not_supported_toast, appName),
                Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mConnection != null) {
            unbindService(mConnection);
        }
        finish();
    }

    private void launchSettings(CustomTabsSession session) {
        boolean success = launchBrowserSiteSettings(ManageDataLauncherActivity.this,
                session, mProviderPackage, getDefaultUrlForManagingSpace());
        if (success) {
            finish();
        } else {
            handleNoSupportForManageSpace();
        }
    }

    // TODO: move to AndroidX.
    private static boolean launchBrowserSiteSettings(Activity activity, CustomTabsSession session,
            String packageName, Uri defaultUri) {
        // CustomTabsIntent builder is used just to put in the session extras.
        Intent intent = new CustomTabsIntent.Builder().setSession(session).build().intent;
        intent.setAction(ACTION_MANAGE_TRUSTED_WEB_ACTIVITY_DATA);
        intent.setPackage(packageName);
        intent.setData(defaultUri);
        try {
            activity.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }

    // TODO: move to feature detection in AndroidX.
    private boolean supportsTrustedWebActivities(String providerPackage) {
        if (ChromeLegacyUtils.supportsTrustedWebActivities(getPackageManager(), providerPackage)) {
            return true;
        }
        Intent intent = new Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION)
                .setPackage(providerPackage);
        List<ResolveInfo> services = getPackageManager().queryIntentServices(intent,
                PackageManager.GET_RESOLVED_FILTER);
        if (services.isEmpty()) {
            return false;
        }
        ResolveInfo resolveInfo = services.get(0);
        return resolveInfo.filter != null &&
                resolveInfo.filter.hasCategory(TRUSTED_WEB_ACTIVITY_CATEGORY);
    }

    private class Connection extends CustomTabsServiceConnection {
        @Override
        public void onCustomTabsServiceConnected(ComponentName componentName,
                CustomTabsClient client) {
            if (!isFinishing()) {
                launchSettings(client.newSession(null));
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {}
    }

    private class LegacyChromeConnection extends CustomTabsServiceConnection {

        private CustomTabsSession mSession;

        private CustomTabsCallback mCustomTabsCallback = new CustomTabsCallback() {
            @Override
            public void onRelationshipValidationResult(int relation, Uri requestedOrigin,
                    boolean validated, Bundle extras) {
                if (isFinishing()) {
                    return;
                }
                if (!validated) {
                    onError(new RuntimeException("Failed to validate origin " + requestedOrigin));
                    finish();
                    return;
                }
                launchSettings(mSession);
            }
        };

        @Override
        public void onCustomTabsServiceConnected(ComponentName componentName,
                CustomTabsClient client) {
            if (isFinishing()) {
                return;
            }
            Uri url = getDefaultUrlForManagingSpace();
            if (url == null) {
                onError(new RuntimeException("Can't launch settings without an url"));
                finish();
                return;
            }
            mSession = client.newSession(mCustomTabsCallback);
            boolean warmUpSuccessful = client.warmup(0);
            if (!warmUpSuccessful) {
                // Chrome can't warm up, because first run experience wasn't shown. That means
                // a TWA has never actually launched.
                handleTwaNeverLaunched();
                return;
            }
            mSession.validateRelationship(RELATION_HANDLE_ALL_URLS, url, null);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {}
    }

    /**
     * Checks whether the provided package name supports site settings. Returns true when
     * the package name belongs to a twa provider supporting the site settings category or
     * it is a legacy version of chrome that supports site settings but does not define
     * the site settings category yet.
     */
    public static boolean packageSupportsSiteSettings(String packageName,
            PackageManager packageManager) {
        if (packageName == null) return false;
        if (ChromeLegacyUtils.supportsSiteSettings(packageManager, packageName)) {
            // Legacy Chrome supports site settings but does not have the site settings category.
            return true;
        } else {
            Intent customTabsIntent = new Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION);
            customTabsIntent.addCategory(CATEGORY_LAUNCH_SITE_SETTINGS);
            customTabsIntent.setPackage(packageName);
            List<ResolveInfo> services = packageManager.queryIntentServices(customTabsIntent,
                    PackageManager.GET_RESOLVED_FILTER);
            return services.size() > 0;
        }
    }

    /**
     * Returns the {@link ShortcutInfo} for a dynamic shortcut into site settings,
     * provided that {@link ManageDataLauncherActivity} is present in the manifest
     * and an Intent for managing site settings is available.
     *
     * Otherwise returns null if {@link ManageDataLauncherActivity} is not launchable
     * or if shortcuts are not supported by the Android SDK version.
     *
     * The shortcut returned does not specify an activity. Thus when the shortcut is added,
     * the app's main activity will be used by default. This activity needs to define the
     * MAIN action and LAUNCHER category in order to attach the shortcut.
     */
    @Nullable
    static ShortcutInfo getSiteSettingsShortcutOrNull(Context context,
            PackageManager packageManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return null;

        Intent siteSettingsIntent = new Intent(context, ManageDataLauncherActivity.class);
        siteSettingsIntent.setAction(ACTION_MANAGE_TRUSTED_WEB_ACTIVITY_DATA);
        List<ResolveInfo> activities = packageManager.queryIntentActivities(siteSettingsIntent,
                PackageManager.MATCH_DEFAULT_ONLY);

        if(activities.size() == 0) return null;

        int shortcutIconId = context.getResources().getIdentifier(
                OVERRIDE_IC_SITE_SETTINGS_ID, "drawable", context.getPackageName());
        return new ShortcutInfo.Builder(context, SITE_SETTINGS_SHORTCUT_ID)
                .setShortLabel("Site Settings")
                .setLongLabel("Manage website notifications, permissions, etc.")
                .setIcon(Icon.createWithResource(context,
                        shortcutIconId != 0 ? shortcutIconId : R.drawable.ic_site_settings))
                .setIntent(siteSettingsIntent)
                .build();
    }

    /**
     * Adds dynamic shortcut to site settings if the twa provider and android version supports it.
     *
     * Removes previously added site settings shortcut if it is no longer supported, e.g. the user
     * changed their default browser.
     *
     * In order for addDynamicShortcuts() to work the android manifest must declare
     * an activity with the action MAIN and category LAUNCHER.
     */
    public static void addSiteSettingsShortcut(Context context, String packageName) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return;

        PackageManager packageManager = context.getPackageManager();
        ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);

        // Remove potentially existing shortcut if package does not support shortcuts.
        if (!packageSupportsSiteSettings(packageName, packageManager)) {
            shortcutManager.removeDynamicShortcuts(Collections.singletonList(ManageDataLauncherActivity
                    .SITE_SETTINGS_SHORTCUT_ID));
            return;
        }

        ShortcutInfo shortcut = getSiteSettingsShortcutOrNull(
                context, packageManager);

        // Remove potentially existing shortcut if shortcut can not be retrieved.
        if(shortcut == null) {
            shortcutManager.removeDynamicShortcuts(Collections.singletonList(ManageDataLauncherActivity
                    .SITE_SETTINGS_SHORTCUT_ID));
            return;
        }
        shortcutManager.addDynamicShortcuts(Collections.singletonList(shortcut));
    }
}
