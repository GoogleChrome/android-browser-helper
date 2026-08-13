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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(sdk = {Build.VERSION_CODES.O_MR1})
public class ShortcutTrampolineActivityTest {
    private Context mContext;
    private ShadowPackageManager mShadowPackageManager;

    private static final String DEFAULT_URL = "https://www.example.com/twa/home";

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mShadowPackageManager = shadowOf(mContext.getPackageManager());

        // Set up the package info with metadata on a dummy LauncherActivity
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = mContext.getPackageName();

        ActivityInfo dummyLauncherActivity = new ActivityInfo();
        dummyLauncherActivity.packageName = mContext.getPackageName();
        dummyLauncherActivity.name = LauncherActivity.class.getName();
        dummyLauncherActivity.metaData = new Bundle();
        dummyLauncherActivity.metaData.putString("android.support.customtabs.trusted.DEFAULT_URL", DEFAULT_URL);

        ActivityInfo trampolineActivity = new ActivityInfo();
        trampolineActivity.packageName = mContext.getPackageName();
        trampolineActivity.name = ShortcutTrampolineActivity.class.getName();

        packageInfo.activities = new ActivityInfo[]{dummyLauncherActivity, trampolineActivity};
        mShadowPackageManager.addPackage(packageInfo);
    }

    @Test
    public void activityFinishesSynchronously() {
        Intent intent = new Intent(Intent.ACTION_VIEW)
                .setData(Uri.parse("https://www.example.com/twa/shortcut"));

        ActivityController<ShortcutTrampolineActivity> controller =
                Robolectric.buildActivity(ShortcutTrampolineActivity.class, intent);

        controller.create();

        assertTrue(controller.get().isFinishing());
    }

    @Test
    public void launchesTwaForTrustedUri() {
        Uri trustedUri = Uri.parse("https://www.example.com/twa/shortcut");
        Intent intent = new Intent(Intent.ACTION_VIEW).setData(trustedUri);

        ActivityController<ShortcutTrampolineActivity> controller =
                Robolectric.buildActivity(ShortcutTrampolineActivity.class, intent);

        controller.create();

        // The activity should finish immediately.
        assertTrue(controller.get().isFinishing());

        // Since we didn't set up custom tabs service, it will use the fallback strategy.
        // The fallback strategy uses the application context to start the intent, which
        // should be registered in the shadow application.
        Intent launchedIntent = shadowOf(RuntimeEnvironment.application).getNextStartedActivity();
        assertNotNull(launchedIntent);
        assertEquals(Intent.ACTION_VIEW, launchedIntent.getAction());
        assertEquals(trustedUri, launchedIntent.getData());
        
        // Ensure FLAG_ACTIVITY_NEW_TASK is attached
        int flags = launchedIntent.getFlags();
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, flags & Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    @Test
    public void dropsUntrustedUri() {
        Uri untrustedUri = Uri.parse("https://www.evil.com/twa/shortcut");
        Intent intent = new Intent(Intent.ACTION_VIEW).setData(untrustedUri);

        ActivityController<ShortcutTrampolineActivity> controller =
                Robolectric.buildActivity(ShortcutTrampolineActivity.class, intent);

        controller.create();

        // The activity should finish immediately.
        assertTrue(controller.get().isFinishing());

        // No activity should be launched because the URI is untrusted.
        Intent launchedIntent = shadowOf(RuntimeEnvironment.application).getNextStartedActivity();
        assertNull(launchedIntent);
    }
}
