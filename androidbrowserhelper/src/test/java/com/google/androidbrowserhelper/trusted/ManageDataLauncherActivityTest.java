// Copyright 2020 Google Inc. All Rights Reserved.
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

import static com.google.androidbrowserhelper.trusted.ManageDataLauncherActivity.ACTION_MANAGE_TRUSTED_WEB_ACTIVITY_DATA;
import static com.google.androidbrowserhelper.trusted.ManageDataLauncherActivity.CATEGORY_LAUNCH_SITE_SETTINGS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import static androidx.browser.customtabs.CustomTabsService.TRUSTED_WEB_ACTIVITY_CATEGORY;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowPackageManager;

import androidx.browser.customtabs.CustomTabsService;
import androidx.test.InstrumentationRegistry;

/**
 * Tests for {@link TwaProviderPicker}.
 */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(sdk = {Build.VERSION_CODES.O_MR1})
public class ManageDataLauncherActivityTest {
    private PackageManager mPackageManager;
    private ShadowPackageManager mShadowPackageManager;

    private static final String TWA_PROVIDER_PACKAGE = "com.trustedweb.one";
    private static final String CHROME = "com.android.chrome";
    private static final int CHROME_72_VERSION = 362600000;
    private static final int CHROME_71_VERSION = 357800000;

    @Before
    public void setUp() {
        mPackageManager = RuntimeEnvironment.application.getPackageManager();
        mShadowPackageManager = shadowOf(mPackageManager);
    }

    @Test
    public void chrome72SupportsSiteSettings() {
        installChrome(CHROME_72_VERSION);
        TwaProviderPicker.Action action = TwaProviderPicker.pickProvider(mPackageManager);

        assertTrue(ManageDataLauncherActivity
                .packageSupportsSiteSettings(action.provider, mPackageManager));
    }

    @Test
    public void chrome71DoesNotSupportsSiteSettings() {
        installChrome(CHROME_71_VERSION);
        TwaProviderPicker.Action action = TwaProviderPicker.pickProvider(mPackageManager);

        assertFalse(ManageDataLauncherActivity
                .packageSupportsSiteSettings(action.provider,mPackageManager));
    }

    @Test
    public void browserWithCategorySupportsSiteSettings() {
        installTrustedWebActivityProviderWithSiteSettingsCategory(TWA_PROVIDER_PACKAGE);

        TwaProviderPicker.Action action = TwaProviderPicker.pickProvider(mPackageManager);

        assertTrue(ManageDataLauncherActivity
                .packageSupportsSiteSettings(action.provider, mPackageManager));
    }

    @Test
    public void browserWithoutCategoryDoesNotSupportSiteSettings() {
        installTrustedWebActivityProvider(TWA_PROVIDER_PACKAGE);

        TwaProviderPicker.Action action = TwaProviderPicker.pickProvider(mPackageManager);

        assertFalse(ManageDataLauncherActivity
                .packageSupportsSiteSettings(action.provider, mPackageManager));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.N_MR1)
    public void returnsSiteSettingsShortcut() {
        installTrustedWebActivityProviderWithManageDataAction(TWA_PROVIDER_PACKAGE);

        assertNotNull(ManageDataLauncherActivity
                .getSiteSettingsShortcutOrNull(mContext, mPackageManager));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.N)
    public void returnsNullSiteSettingsShortcutWithLowSDK() {
        installTrustedWebActivityProviderWithManageDataAction(TWA_PROVIDER_PACKAGE);
        assertNull(ManageDataLauncherActivity
                .getSiteSettingsShortcutOrNull(mContext, mPackageManager));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.N_MR1)
    public void returnsNullSiteSettingsShortcutWhenMissingManageDataLauncherActivity() {
        assertNull(ManageDataLauncherActivity
                .getSiteSettingsShortcutOrNull(mContext, mPackageManager));
    }

    private void installBrowser(String packageName) {
        Intent intent = new Intent()
                .setData(Uri.fromParts("http", "", null))
                .setAction(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE);

        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = new ActivityInfo();
        resolveInfo.activityInfo.packageName = packageName;

        mShadowPackageManager.addResolveInfoForIntent(intent, resolveInfo);
    }

    private void installCustomTabsProvider(String packageName) {
        installBrowser(packageName);

        Intent intent = new Intent()
                .setAction(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION);

        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = new ServiceInfo();
        resolveInfo.serviceInfo.packageName = packageName;

        mShadowPackageManager.addResolveInfoForIntent(intent, resolveInfo);
    }

    private void installTrustedWebActivityProvider(String packageName) {
        installBrowser(packageName);

        Intent intent = new Intent()
                .setAction(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION);

        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = new ServiceInfo();
        resolveInfo.serviceInfo.packageName = packageName;
        resolveInfo.filter = Mockito.mock(IntentFilter.class);
        when(resolveInfo.filter.hasCategory(eq(TRUSTED_WEB_ACTIVITY_CATEGORY))).thenReturn(true);

        mShadowPackageManager.addResolveInfoForIntent(intent, resolveInfo);
    }

    private void installTrustedWebActivityProviderWithSiteSettingsCategory(String packageName) {
        installBrowser(packageName);

        Intent intent = new Intent()
                .setAction(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION);
        intent.setPackage(packageName);
        intent.addCategory(CATEGORY_LAUNCH_SITE_SETTINGS);

        mShadowPackageManager.addResolveInfoForIntent(intent, new ResolveInfo());
    }

    private Context mContext = InstrumentationRegistry.getContext();

    private void installTrustedWebActivityProviderWithManageDataAction(String packageName) {
        installBrowser(packageName);

        Intent intent = new Intent(mContext, ManageDataLauncherActivity.class)
                .setAction(ACTION_MANAGE_TRUSTED_WEB_ACTIVITY_DATA);

        mShadowPackageManager.addResolveInfoForIntent(intent, new ResolveInfo());
    }

    private void installChrome(int version) {
        // Chrome was still a Custom Tabs provider before Chrome 72.
        installCustomTabsProvider(CHROME);

        PackageInfo packageInfo = new PackageInfo();
        packageInfo.versionCode = version;
        packageInfo.packageName = CHROME;

        mShadowPackageManager.addPackage(packageInfo);
    }
}