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
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

/**
 * Tests for {@link NotificationDelegationExtraCommandHandler}.
 */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(sdk = {Build.VERSION_CODES.TIRAMISU})
public class NotificationDelegationExtraCommandHandlerTest {
    private static final String CHANNEL_NAME = "General Notifications";

    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        NotificationManager notificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        shadowOf(notificationManager).setNotificationsEnabled(false);
    }

    private int checkPermission() {
        Bundle args = new Bundle();
        args.putString("notificationChannelName", CHANNEL_NAME);
        Bundle result = new NotificationDelegationExtraCommandHandler().handleExtraCommand(mContext,
                NotificationDelegationExtraCommandHandler.COMMAND_CHECK_NOTIFICATION_PERMISSION,
                args, null);
        assertTrue(result.getBoolean("success"));
        return result.getInt(NotificationPermissionRequestActivity.KEY_PERMISSION_STATUS);
    }

    @Test
    public void checkPermission_asksWhileNotDenied() {
        assertEquals(PermissionStatus.ASK, checkPermission());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void checkPermission_ignoresTheBackedUpFlag() {
        PrefUtils.setHasRequestedNotificationPermission(mContext);

        assertEquals(PermissionStatus.ASK, checkPermission());
    }

    @Test
    public void checkPermission_blocksAfterADenial() {
        PrefUtils.setNotificationPermissionDenied(mContext, true);

        assertEquals(PermissionStatus.BLOCK, checkPermission());

        PrefUtils.setNotificationPermissionDenied(mContext, false);

        assertEquals(PermissionStatus.ASK, checkPermission());
    }

    @Test
    public void checkPermission_allowsWhenEnabled() {
        PrefUtils.setNotificationPermissionDenied(mContext, true);
        NotificationManager notificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        shadowOf(notificationManager).setNotificationsEnabled(true);

        assertEquals(PermissionStatus.ALLOW, checkPermission());
    }
}
