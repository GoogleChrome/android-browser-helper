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

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowNotificationManager;

/**
 * Tests for {@link NotificationUtils}.
 */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(sdk = {Build.VERSION_CODES.O_MR1})
public class NotificationUtilsTest {
    private Context mContext;
    private NotificationManager mNotificationManager;
    private ShadowNotificationManager mShadowNotificationManager;

    private static final String CHANNEL_NAME = "General Notifications";
    private static final String EXPECTED_CHANNEL_ID = "general_notifications_channel_id";

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mShadowNotificationManager = shadowOf(mNotificationManager);
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

    @Test
    public void getNotificationImportance_withNonServiceContext() {
        Context appContext = RuntimeEnvironment.application;
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT,
                NotificationUtils.getNotificationImportance(appContext));
        assertFalse(NotificationUtils.shouldUseHighPriorityNotifications(appContext));
    }
}
