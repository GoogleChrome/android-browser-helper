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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Instrumentation test that discharges the V1 ranking-signal check for {@link TwaProviderPicker}.
 *
 * <p>Unlike {@code adb shell} queries (which execute under the {@code shell} UID and are exempt
 * from Android 11+ package visibility filtering), this test runs inside the test application's own
 * UID. Because {@code androidbrowserhelper} targets {@code targetSdkVersion 31} (i.e. API &gt;= 30),
 * Android package visibility filtering is genuinely active for this APK rather than vacuously
 * disabled.
 *
 * <p>Note: this is an {@code androidTest} suite and is <b>not</b> run by the baseline
 * {@code testDebugUnitTest} command; execute it on a connected device via
 * {@code :androidbrowserhelper:connectedDebugAndroidTest}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class TwaProviderPickerInstallSourceTest {
    private static final String TAG = "TWAInstallSourceTest";
    private static final String CHROME_PACKAGE = "com.android.chrome";
    private static final String PLAY_STORE_PACKAGE = "com.android.vending";

    private Context mContext;
    private PackageManager mPackageManager;

    @Before
    public void setUp() {
        TwaProviderPicker.restrictToPackageForTesting(null);
        mContext = ApplicationProvider.getApplicationContext();
        mPackageManager = mContext.getPackageManager();
    }

    @After
    public void tearDown() {
        TwaProviderPicker.restrictToPackageForTesting(null);
    }

    /**
     * Queries the exact {@link Intent#ACTION_VIEW} + {@link Intent#CATEGORY_BROWSABLE} + {@code http}
     * intent used by {@link TwaProviderPicker#pickProvider(PackageManager)}, then verifies that every
     * returned candidate is resolvable via {@link PackageManager#getApplicationInfo} and
     * {@link PackageManager#getInstallerPackageName} from the app's own UID. Skips (does not fail)
     * when no browser is installed on the target device.
     */
    @Test
    public void browserCandidates_resolveInstallSourceFromAppUid() throws Exception {
        Set<String> candidates = queryBrowserCandidatePackages();
        assumeFalse("No browser candidates installed on device; skipping.", candidates.isEmpty());

        for (String packageName : candidates) {
            ApplicationInfo appInfo = mPackageManager.getApplicationInfo(packageName, 0);
            assertNotNull("getApplicationInfo returned null for " + packageName, appInfo);

            // Must not throw IllegalArgumentException / SecurityException from the app's UID.
            String installer = mPackageManager.getInstallerPackageName(packageName);
            boolean isSystem = (appInfo.flags & (ApplicationInfo.FLAG_SYSTEM
                    | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
            boolean isPlayInstalled = PLAY_STORE_PACKAGE.equals(installer);

            Log.i(TAG, "Candidate " + packageName
                    + ": flags=0x" + Integer.toHexString(appInfo.flags)
                    + " isSystem=" + isSystem
                    + " installer=" + installer
                    + " privileged=" + (isSystem || isPlayInstalled));
        }

        TwaProviderPicker.Action action = TwaProviderPicker.pickProvider(mPackageManager);
        assertNotNull(action);
    }

    /**
     * Discharges the V1 ranking-signal check when retail Chrome ({@code com.android.chrome}) is
     * present among the visible browser candidates: verifies that {@code com.android.chrome} is
     * classified as privileged via {@link ApplicationInfo#FLAG_SYSTEM},
     * {@link ApplicationInfo#FLAG_UPDATED_SYSTEM_APP}, or a {@code com.android.vending}
     * installer of record. Skips when {@code com.android.chrome} is not installed.
     */
    @Test
    public void chromeCandidate_isClassifiedAsSystemOrStoreInstalledWhenPresent() throws Exception {
        Set<String> candidates = queryBrowserCandidatePackages();
        assumeTrue("com.android.chrome not installed as a browser candidate; skipping.",
                candidates.contains(CHROME_PACKAGE));

        ApplicationInfo appInfo = mPackageManager.getApplicationInfo(CHROME_PACKAGE, 0);
        String installer = mPackageManager.getInstallerPackageName(CHROME_PACKAGE);
        boolean isSystem = (appInfo.flags & (ApplicationInfo.FLAG_SYSTEM
                | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
        boolean isPlayInstalled = PLAY_STORE_PACKAGE.equals(installer);

        assertTrue("Expected com.android.chrome to be privileged (FLAG_SYSTEM/UPDATED_SYSTEM_APP "
                        + "or installer=com.android.vending), but got flags=0x"
                        + Integer.toHexString(appInfo.flags) + " and installer=" + installer,
                isSystem || isPlayInstalled);
    }

    private Set<String> queryBrowserCandidatePackages() {
        Intent queryBrowsersIntent = new Intent()
                .setAction(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setData(Uri.fromParts("http", "", null));

        List<ResolveInfo> possibleProviders = new ArrayList<>(
                mPackageManager.queryIntentActivities(
                        queryBrowsersIntent, PackageManager.MATCH_DEFAULT_ONLY));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            possibleProviders.addAll(
                    mPackageManager.queryIntentActivities(
                            queryBrowsersIntent, PackageManager.MATCH_ALL));
        }

        Set<String> candidates = new LinkedHashSet<>();
        String selfPackage = mContext.getPackageName();
        for (ResolveInfo info : possibleProviders) {
            if (info != null && info.activityInfo != null
                    && info.activityInfo.packageName != null
                    && !selfPackage.equals(info.activityInfo.packageName)) {
                candidates.add(info.activityInfo.packageName);
            }
        }
        return candidates;
    }
}
