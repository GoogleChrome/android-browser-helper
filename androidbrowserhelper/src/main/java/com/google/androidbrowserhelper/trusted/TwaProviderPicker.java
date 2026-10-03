// Copyright 2019 Google Inc. All Rights Reserved.
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

import static androidx.browser.customtabs.CustomTabsService.TRUSTED_WEB_ACTIVITY_CATEGORY;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsService;
import androidx.browser.customtabs.TrustedWebUtils;

/**
 * Chooses a Browser/Custom Tabs/Trusted Web Activity provider and appropriate launch mode.
 * All browsers on the users device are considered, in order of Android's preference (so the user's
 * default will come first). We look for browsers that support Trusted Web Activities first, then
 * ones that support Custom Tabs falling back to any browser if neither of those criteria are
 * matched.
 *
 * A browser is an app that has an Activity that answers the following Intent:
 * <pre>
 * new Intent()
 *         .setAction(Intent.ACTION_VIEW)
 *         .addCategory(Intent.CATEGORY_BROWSABLE)
 *         .setData(Uri.parse("http://"));
 * </pre>
 *
 * A Custom Tabs provider is an app that has a Service that answers the following Intent:
 * <pre>
 * new Intent()
 *         .setAction(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION);
 * </pre>
 *
 * A Trusted Web Activity provider is an app that has a Service that answers the following Intent:
 * <pre>
 * new Intent()
 *         .setAction(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION)
 *         .addCategory(CustomTabsService.TRUSTED_WEB_ACTIVITY_CATEGORY);
 * </pre>
 */
public class TwaProviderPicker {
    private static final String TAG = "TWAProviderPicker";
    private static final String PLAY_STORE_PACKAGE = "com.android.vending";
    private static String sPackageNameForTesting;

    @IntDef({LaunchMode.TRUSTED_WEB_ACTIVITY, LaunchMode.CUSTOM_TAB, LaunchMode.BROWSER})
    @Retention(RetentionPolicy.SOURCE)
    public @interface LaunchMode {
        /** The webapp should be launched as a Trusted Web Activity. */
        int TRUSTED_WEB_ACTIVITY = 0;
        /** The webapp should be launched as a simple (no Session) Custom Tab. */
        int CUSTOM_TAB = 1;
        /** The webapp should be opened in the browser. */
        int BROWSER = 2;
    }

    /**
     * The result of {@link #pickProvider}, holding the launch mode and package (which may be null
     * when the launchMode is BROWSER).
     */
    public static class Action {
        /** How the webapp should be launched. */
        @LaunchMode
        public final int launchMode;
        /** The provider package name, may be null when {@code launchMode == BROWSER}. */
        @Nullable
        public final String provider;

        /** Creates this object with the given parameters. */
        public Action(@LaunchMode int launchMode, @Nullable String provider) {
            this.launchMode = launchMode;
            this.provider = provider;
        }
    }

    /**
     * Chooses an appropriate provider (see class description) and the launch mode that browser
     * supports.
     */
    public static Action pickProvider(PackageManager pm) {
        // Setting the Intent Data as seen at
        // https://cs.android.com/android/platform/superproject/+/fd994cf9ef8207ad03dc3a1d831e9263ddfd4469:packages/apps/PermissionController/src/com/android/packageinstaller/role/model/BrowserRoleBehavior.java
        Intent queryBrowsersIntent = new Intent()
                .setAction(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setData(Uri.fromParts("http", "", null));
        if (sPackageNameForTesting != null) {
            queryBrowsersIntent.setPackage(sPackageNameForTesting);
        }

        String bestCctProvider = null;
        boolean bestCctProviderIsPrivileged = false;
        String bestBrowserProvider = null;
        boolean bestBrowserProviderIsPrivileged = false;

        // These packages will be in order of Android's preference.
        List<ResolveInfo> possibleProviders
                = pm.queryIntentActivities(queryBrowsersIntent, PackageManager.MATCH_DEFAULT_ONLY);

        // Entries before this index came from the MATCH_DEFAULT_ONLY query. Everything after it
        // came from MATCH_ALL, which is explicitly NOT ordered by preference -- see the comment
        // below.
        final int defaultOrderedCount = possibleProviders.size();

        // MATCH_DEFAULT_ONLY returns a single entry when the user has an actual default browser. If it
        // returns more than one, no default is set and the head is as unordered as the MATCH_ALL tail --
        // require system or Play Store installation rather than taking an arbitrary first arrival.
        final boolean headIsAuthoritative = defaultOrderedCount == 1;

        // According to the documentation, the flag we want to use above is MATCH_DEFAULT_ONLY.
        // This would match all the browsers installed on the user's system whose intent handler
        // contains the category Intent.CATEGORY_DEFAULT. However, in Android M the behavior of
        // the PackageManager changed to only return the default browser unless the MATCH_ALL is
        // passed (this is specific to querying browsers - if you query for any other type of
        // package, MATCH_DEFAULT_ONLY will work as documented). This flag did not exist on Android
        // versions before M, so we only use it in that case.
        //
        // Additionally we add the result of the call with MATCH_ALL onto the end of the result of
        // MATCH_DEFAULT_ONLY (instead of calling queryIntentActivities just once, with MATCH_ALL)
        // because (again, as opposed to the documentation) when MATCH_ALL is used the results are
        // not returned in order of Android's preference.
        //
        // This will result in the user's default browser being in the list twice, however that
        // shouldn't affect the correctness of the following code.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            possibleProviders.addAll(pm.queryIntentActivities(queryBrowsersIntent,
                    PackageManager.MATCH_ALL));
        }

        Map<String, Integer> customTabsServices = getLaunchModesForCustomTabsServices(pm);

        for (int i = 0; i < possibleProviders.size(); i++) {
            ResolveInfo possibleProvider = possibleProviders.get(i);
            String providerName = possibleProvider.activityInfo.packageName;

            @LaunchMode int launchMode = customTabsServices.containsKey(providerName)
                    ? customTabsServices.get(providerName) : LaunchMode.BROWSER;

            switch (launchMode) {
                case LaunchMode.TRUSTED_WEB_ACTIVITY:
                    if (i == 0 && headIsAuthoritative) {
                        // The user's default browser (or sole default-category browser). Authoritative:
                        // take it and stop, as before.
                        Log.d(TAG, "Found TWA provider, finishing search: " + providerName);
                        return new Action(LaunchMode.TRUSTED_WEB_ACTIVITY, providerName);
                    }
                    // No single default browser is set (multi-entry head) or we are in the
                    // unordered MATCH_ALL tail: require system or Play Store installation for
                    // TRUSTED_WEB_ACTIVITY mode, and downgrade unprivileged candidates to
                    // CUSTOM_TAB.
                    if (isSystemOrStoreInstalled(pm, providerName)) {
                        Log.d(TAG, "Found privileged TWA provider, finishing search: "
                                + providerName);
                        return new Action(LaunchMode.TRUSTED_WEB_ACTIVITY, providerName);
                    } else if (bestCctProvider == null) {
                        bestCctProvider = providerName;
                        bestCctProviderIsPrivileged = false;
                    }
                    break;
                case LaunchMode.CUSTOM_TAB:
                    Log.d(TAG, "Found Custom Tabs provider: " + providerName);
                    if (i == 0 && headIsAuthoritative) {
                        bestCctProvider = providerName;
                        bestCctProviderIsPrivileged = true;
                    } else if (bestCctProvider == null) {
                        bestCctProvider = providerName;
                        bestCctProviderIsPrivileged = isSystemOrStoreInstalled(pm, providerName);
                    } else if (!bestCctProviderIsPrivileged
                            && isSystemOrStoreInstalled(pm, providerName)) {
                        bestCctProvider = providerName;
                        bestCctProviderIsPrivileged = true;
                    }
                    break;
                case LaunchMode.BROWSER:
                    Log.d(TAG, "Found browser: " + providerName);
                    if (i == 0 && headIsAuthoritative) {
                        bestBrowserProvider = providerName;
                        bestBrowserProviderIsPrivileged = true;
                    } else if (bestBrowserProvider == null) {
                        bestBrowserProvider = providerName;
                        bestBrowserProviderIsPrivileged =
                                isSystemOrStoreInstalled(pm, providerName);
                    } else if (!bestBrowserProviderIsPrivileged
                            && isSystemOrStoreInstalled(pm, providerName)) {
                        bestBrowserProvider = providerName;
                        bestBrowserProviderIsPrivileged = true;
                    }
                    break;
            }
        }

        if (bestCctProvider != null) {
            Log.d(TAG, "Found no TWA providers, using first Custom Tabs provider: "
                    + bestCctProvider);
            return new Action(LaunchMode.CUSTOM_TAB, bestCctProvider);
        }

        Log.d(TAG, "Found no TWA providers, using first browser: " + bestBrowserProvider);
        return new Action(LaunchMode.BROWSER, bestBrowserProvider);
    }

    /**
     * Whether {@code packageName} was preinstalled on the system image or installed by the Play
     * Store. Used to gate Trusted Web Activity candidates when no single default browser is set or
     * when scanning the unordered MATCH_ALL tail: non-default unprivileged candidates are excluded
     * from {@link LaunchMode#TRUSTED_WEB_ACTIVITY} and downgraded to
     * {@link LaunchMode#CUSTOM_TAB}.
     *
     * <p>Note that the installer of record is forgeable with {@code adb install -i}, though an
     * attacker with ADB access has stronger primitives.
     *
     * <p>Returns false for any package we cannot resolve (and logs a warning, as candidates
     * returned by {@link PackageManager#queryIntentActivities} are visible by construction).
     * Callers must treat "unknown" and "not privileged" identically.
     */
    private static boolean isSystemOrStoreInstalled(PackageManager pm, String packageName) {
        boolean resolved = false;
        try {
            ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            if (info != null) {
                resolved = true;
                if ((info.flags & (ApplicationInfo.FLAG_SYSTEM
                        | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0) {
                    return true;
                }
            }
        } catch (PackageManager.NameNotFoundException | RuntimeException e) {
            // Fall through: an unresolvable package is simply not privileged.
        }

        try {
            if (PLAY_STORE_PACKAGE.equals(pm.getInstallerPackageName(packageName))) return true;
        } catch (RuntimeException e) {
            // getInstallerPackageName throws IllegalArgumentException for unknown packages on AOSP,
            // but the thrown type is not guaranteed across OEM PackageManager implementations. This
            // runs on the cold-start launch path and the method's contract is already "false when we
            // cannot resolve", so swallow broadly rather than risk failing the launch.
            resolved = false;
        }

        if (!resolved) {
            // The candidate came out of queryIntentActivities(), so it is visible to us and should
            // resolve here. If it does not, the candidate is treated as neither system- nor
            // Play-installed. Loud on purpose: this is the one assumption in the install-source
            // check that no unit test can cover.
            Log.w(TAG, "Could not resolve install source for browser candidate " + packageName
                    + "; treating it as neither system- nor Play-installed.");
        }
        return false;
    }

    /**
     * Restricts the logic to only consider providers from the given package. For use in testing.
     * Pass in {@code null} to reset.
     */
    static void restrictToPackageForTesting(@Nullable String packageName) {
        sPackageNameForTesting = packageName;
    }

    /** Returns a map from package name to LaunchMode for all available Custom Tabs Services. */
    private static Map<String, Integer> getLaunchModesForCustomTabsServices(PackageManager pm) {
        List<ResolveInfo> services = pm.queryIntentServices(
                new Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION),
                PackageManager.GET_RESOLVED_FILTER);

        Map<String, Integer> customTabsServices = new HashMap<>();
        for (ResolveInfo service : services) {
            String packageName = service.serviceInfo.packageName;

            if (ChromeLegacyUtils.supportsTrustedWebActivities(pm, packageName)) {
                // Chrome 72-74 support Trusted Web Activites but don't yet have the TWA category on
                // their CustomTabsService.
                customTabsServices.put(packageName, LaunchMode.TRUSTED_WEB_ACTIVITY);
                continue;
            }

            boolean supportsTwas = service.filter != null &&
                    service.filter.hasCategory(TRUSTED_WEB_ACTIVITY_CATEGORY);

            customTabsServices.put(packageName,
                    supportsTwas ? LaunchMode.TRUSTED_WEB_ACTIVITY : LaunchMode.CUSTOM_TAB);
        }
        return customTabsServices;
    }
}