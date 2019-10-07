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

import android.content.pm.PackageManager;

import java.util.Arrays;
import java.util.List;

import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsService;

/**
 * The behaviour of the Trusted Web Activity Launcher changes based on what features are supported
 * by the browser the TWA is going to be launched in. The browser advertises what features it
 * supports by including categories (such as
 * {@link CustomTabsService#TRUSTED_WEB_ACTIVITY_CATEGORY}).
 *
 * Unfortunately when we first launched Trusted Web Activities, we didn't have the foresight to
 * include these categories in Chrome. This class includes Chrome specific code that can be used to
 * check whether a version of Chrome supports a feature even though it doesn't advertise that
 * support.
 *
 * For example, browsers can now use the {@link CustomTabsService#TRUSTED_WEB_ACTIVITY_CATEGORY} to
 * advertise that they support Trusted Web Activities, however Chrome 72 supports TWAs but does not
 * have that category.
 *
 * We hope to remove this class once the versions of Chrome that require special behaviour are no
 * longer in wide use.
 */
public class ChromeLegacyUtils {
    private static final String CHROME_LOCAL_BUILD_PACKAGE = "com.google.android.apps.chrome";
    private static final String CHROMIUM_LOCAL_BUILD_PACKAGE = "org.chromium.chrome";
    private static final String CHROME_CANARY_PACKAGE = "com.chrome.canary";
    private static final String CHROME_DEV_PACKAGE = "com.chrome.dev";
    private static final String CHROME_STABLE_PACKAGE = "com.android.chrome";
    private static final String CHROME_BETA_PACKAGE = "com.chrome.beta";

    private static final List<String> SUPPORTED_CHROME_PACKAGES = Arrays.asList(
            CHROME_LOCAL_BUILD_PACKAGE,
            CHROMIUM_LOCAL_BUILD_PACKAGE,
            CHROME_CANARY_PACKAGE,
            CHROME_DEV_PACKAGE,
            CHROME_BETA_PACKAGE,
            CHROME_STABLE_PACKAGE);

    /**
     * The versions of Chrome for which we should warn the user if they are out of date. We can't
     * check the version on local builds (the version code is 1) and we assume Canary and Dev users
     * update regularly.
     */
    static final List<String> VERSION_CHECK_CHROME_PACKAGES = Arrays.asList(
            CHROME_BETA_PACKAGE,
            CHROME_STABLE_PACKAGE);

    private static final List<String> LOCAL_BUILD_PACKAGES = Arrays.asList(
            CHROME_LOCAL_BUILD_PACKAGE,
            CHROMIUM_LOCAL_BUILD_PACKAGE);

    static final int VERSION_SUPPORTS_TRUSTED_WEB_ACTIVITIES = 362600000;
    private static final int VERSION_SUPPORTS_NO_PREWARM = 368300000;
    private static final int VERSION_76 = 380900000;
    private static final int VERSION_77 = 386500000;

    private ChromeLegacyUtils() {}

    /**
     * Whether the navbar of the launched Activity will be white.
     */
    public static boolean usesWhiteNavbar(String packageName) {
        // Prior to adding support for nav bar color customization, Chrome had always set
        // the white color.
        return SUPPORTED_CHROME_PACKAGES.contains(packageName);
    }

    /**
     * Chrome 76 supports navbar and color customization but doesn't advertise it.
     */
    public static boolean isChrome76(PackageManager pm, String packageName) {
        // Assume other browsers that support this feature will advertise it.
        if (!SUPPORTED_CHROME_PACKAGES.contains(packageName)) return false;

        return checkChromeVersion(pm, packageName, VERSION_76, VERSION_77);
    }

    /**
     * Whether the browser supports Trusted Web Activities but doesn't advertise it.
     */
    public static boolean supportsTrustedWebActivities(PackageManager pm, String packageName) {
        // Assume other browsers that support this feature will advertise it.
        if (!SUPPORTED_CHROME_PACKAGES.contains(packageName)) return false;

        return checkChromeVersion(pm, packageName, VERSION_SUPPORTS_TRUSTED_WEB_ACTIVITIES);
    }

    /**
     * Returns whether {@link CustomTabsClient#warmup} needs to be called prior to launching a
     * Trusted Web Activity. Starting from version 73 Chrome does not require warmup, which allows
     * to launch Trusted Web Activities faster.
     */
    public static boolean supportsLaunchWithoutWarmup(PackageManager pm, String packageName) {
        // Assume other browsers never had this requirement.
        if (!SUPPORTED_CHROME_PACKAGES.contains(packageName)) return true;

        return checkChromeVersion(pm, packageName, VERSION_SUPPORTS_NO_PREWARM);
    }

    /**
     * Returns true if the given {@code packageName} points to a version of Chrome greater than or
     * equal to {code version}.
     */
    private static boolean checkChromeVersion(PackageManager pm, String packageName, int version) {
        // We can't get the version of local builds of Chrome/Chromium, so we just assume they're
        // up to date.
        if (LOCAL_BUILD_PACKAGES.contains(packageName)) return true;

        return getVersionCode(pm, packageName) >= version;
    }

    /**
     * Returns true if the given {@code packageName} points to a version of Chrome between
     * minVersion and maxVersion.
     */
    private static boolean checkChromeVersion(PackageManager pm, String packageName,
            int minVersion, int maxVersion) {
        // Assume that these two-sided checks are only for past versions, so local build is newer.
        if (LOCAL_BUILD_PACKAGES.contains(packageName)) return false;

        int versionCode = getVersionCode(pm, packageName);
        return versionCode >= minVersion && versionCode < maxVersion;
    }

    static int getVersionCode(PackageManager pm, String packageName) {
        try {
            return pm.getPackageInfo(packageName, 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }
}
