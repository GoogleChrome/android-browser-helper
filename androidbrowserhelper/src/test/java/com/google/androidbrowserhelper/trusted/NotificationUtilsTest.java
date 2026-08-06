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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.ParameterizedRobolectricTestRunner.Parameter;
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowNotificationManager;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.Arrays;
import java.util.Collection;

/**
 * Parameterized tests for {@link NotificationUtils}.
 */
@RunWith(ParameterizedRobolectricTestRunner.class)
@DoNotInstrument
@Config(sdk = {Build.VERSION_CODES.O_MR1})
public class NotificationUtilsTest {
    @Parameter(0)
    public String mTestName;

    @Parameter(1)
    public String mMetadataKey;

    @Parameter(2)
    public Object mMetadataValue;

    @Parameter(3)
    public int mExpectedImportance;

    @Parameter(4)
    public boolean mExpectedShouldUseHighPriority;

    private Context mContext;
    private PackageManager mPackageManager;
    private ShadowPackageManager mShadowPackageManager;
    private NotificationManager mNotificationManager;
    private ShadowNotificationManager mShadowNotificationManager;

    private static final String CHANNEL_NAME = "General Notifications";
    private static final String EXPECTED_CHANNEL_ID = "general_notifications_channel_id";

    @Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {
                        "default_unspecified",
                        null,
                        null,
                        NotificationManager.IMPORTANCE_DEFAULT,
                        false
                },
                {
                        "customtabs_boolean_true",
                        NotificationUtils.METADATA_USE_HIGH_PRI_NOTIFICATIONS,
                        true,
                        NotificationManager.IMPORTANCE_HIGH,
                        true
                },
                {
                        "customtabs_string_true",
                        NotificationUtils.METADATA_USE_HIGH_PRI_NOTIFICATIONS,
                        "true",
                        NotificationManager.IMPORTANCE_HIGH,
                        true
                },
                {
                        "customtabs_string_true_uppercase",
                        NotificationUtils.METADATA_USE_HIGH_PRI_NOTIFICATIONS,
                        "TRUE",
                        NotificationManager.IMPORTANCE_HIGH,
                        true
                },
                {
                        "androidx_boolean_true",
                        NotificationUtils.METADATA_USE_HIGH_PRI_NOTIFICATIONS_ANDROIDX,
                        true,
                        NotificationManager.IMPORTANCE_HIGH,
                        true
                },
                {
                        "androidx_string_true",
                        NotificationUtils.METADATA_USE_HIGH_PRI_NOTIFICATIONS_ANDROIDX,
                        "true",
                        NotificationManager.IMPORTANCE_HIGH,
                        true
                },
                {
                        "customtabs_boolean_false",
                        NotificationUtils.METADATA_USE_HIGH_PRI_NOTIFICATIONS,
                        false,
                        NotificationManager.IMPORTANCE_DEFAULT,
                        false
                },
                {
                        "customtabs_string_false",
                        NotificationUtils.METADATA_USE_HIGH_PRI_NOTIFICATIONS,
                        "false",
                        NotificationManager.IMPORTANCE_DEFAULT,
                        false
                },
                {
                        "customtabs_string_invalid",
                        NotificationUtils.METADATA_USE_HIGH_PRI_NOTIFICATIONS,
                        "invalid_string",
                        NotificationManager.IMPORTANCE_DEFAULT,
                        false
                },
        });
    }

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mPackageManager = mContext.getPackageManager();
        mShadowPackageManager = shadowOf(mPackageManager);
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mShadowNotificationManager = shadowOf(mNotificationManager);

        if (mMetadataKey != null && mMetadataValue != null) {
            setAppMetadata(mMetadataKey, mMetadataValue);
        }
    }

    @Test
    public void channelNameToId_replacesSpacesAndAppendsSuffix() {
        assertEquals("general_notifications_channel_id",
                NotificationUtils.channelNameToId("General Notifications"));
        assertEquals("chat_channel_id",
                NotificationUtils.channelNameToId("Chat"));
        assertEquals("test_channel_name_channel_id",
                NotificationUtils.channelNameToId("Test Channel Name"));
    }

    @Test
    public void getNotificationImportance_matchesExpectedImportance() {
        assertEquals(mExpectedImportance, NotificationUtils.getNotificationImportance(mContext));
    }

    @Test
    public void shouldUseHighPriorityNotifications_matchesExpectedBoolean() {
        assertEquals(mExpectedShouldUseHighPriority,
                NotificationUtils.shouldUseHighPriorityNotifications(mContext));
    }

    @Test
    public void createNotificationChannel_resolvesImportanceCorrectly() {
        NotificationUtils.createNotificationChannel(mContext, CHANNEL_NAME);

        NotificationChannel channel = mNotificationManager.getNotificationChannel(EXPECTED_CHANNEL_ID);
        assertNotNull(channel);
        assertEquals(CHANNEL_NAME, channel.getName().toString());
        assertEquals(mExpectedImportance, channel.getImportance());
    }

    @Test
    public void createNotificationChannel_explicitImportanceOverridesMetadata() {
        NotificationUtils.createNotificationChannel(mContext, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);

        NotificationChannel channel = mNotificationManager.getNotificationChannel(EXPECTED_CHANNEL_ID);
        assertNotNull(channel);
        assertEquals(CHANNEL_NAME, channel.getName().toString());
        assertEquals(NotificationManager.IMPORTANCE_LOW, channel.getImportance());
    }

    @Test
    public void areNotificationsEnabled_returnsTrueForNewAndEnabledChannels() {
        assertTrue(NotificationUtils.areNotificationsEnabled(mContext, CHANNEL_NAME));

        NotificationUtils.createNotificationChannel(mContext, CHANNEL_NAME);
        assertTrue(NotificationUtils.areNotificationsEnabled(mContext, CHANNEL_NAME));
    }

    @Test
    public void areNotificationsEnabled_returnsFalseForBlockedChannel() {
        NotificationChannel channel = new NotificationChannel(
                EXPECTED_CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE);
        mNotificationManager.createNotificationChannel(channel);

        assertFalse(NotificationUtils.areNotificationsEnabled(mContext, CHANNEL_NAME));
    }

    @Test
    public void areNotificationsEnabled_returnsFalseWhenNotificationsDisabledGlobally() {
        mShadowNotificationManager.setNotificationsEnabled(false);
        assertFalse(NotificationUtils.areNotificationsEnabled(mContext, CHANNEL_NAME));
    }

    @Test
    public void getNotificationImportance_withNullContext() {
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT,
                NotificationUtils.getNotificationImportance(null));
        assertFalse(NotificationUtils.shouldUseHighPriorityNotifications(null));
    }

    private void setAppMetadata(String key, Object value) {
        PackageInfo packageInfo = mShadowPackageManager.getInternalMutablePackageInfo(mContext.getPackageName());
        if (packageInfo == null) {
            packageInfo = new PackageInfo();
            packageInfo.packageName = mContext.getPackageName();
            packageInfo.applicationInfo = new ApplicationInfo();
            packageInfo.applicationInfo.packageName = mContext.getPackageName();
            mShadowPackageManager.addPackage(packageInfo);
        }
        if (packageInfo.applicationInfo.metaData == null) {
            packageInfo.applicationInfo.metaData = new Bundle();
        }
        if (value instanceof Boolean) {
            packageInfo.applicationInfo.metaData.putBoolean(key, (Boolean) value);
        } else if (value instanceof String) {
            packageInfo.applicationInfo.metaData.putString(key, (String) value);
        } else if (value instanceof Integer) {
            packageInfo.applicationInfo.metaData.putInt(key, (Integer) value);
        }
    }
}
