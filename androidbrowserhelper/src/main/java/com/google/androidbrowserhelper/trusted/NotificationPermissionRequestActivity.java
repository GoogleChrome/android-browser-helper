// Copyright 2022 Google Inc. All Rights Reserved.
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
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import android.util.Log;
import androidx.core.app.ActivityCompat;

/**
 * A simple transparent activity for requesting the notification permission. On either approve or disapprove, this will
 * send the result via the {@link Messenger} provided with the intent, and then finish.
 */
public class NotificationPermissionRequestActivity extends Activity {
    private static final String TAG = "Notifications";

    static final String KEY_PERMISSION_STATUS = "permissionStatus";

    // TODO: Use Manifest.permission.POST_NOTIFICATIONS when it is released.
    private static final String PERMISSION_POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS";

    // TODO: Use Build.VERSION_CODES when it is released.
    private static final int VERSION_T = 33;

    private static final String EXTRA_NOTIFICATION_CHANNEL_NAME = "notificationChannelName";
    private static final String EXTRA_MESSENGER = "messenger";

    private String mChannelName;
    private Messenger mMessenger;

    /**
     * Creates a {@link PendingIntent} for launching this activity to request the notification permission. It is mutable
     * so that a messenger extra can be added for returning the permission request result.
     */
    public static PendingIntent createPermissionRequestPendingIntent(Context context, String channelName) {
        Intent intent = new Intent(context.getApplicationContext(), NotificationPermissionRequestActivity.class);
        intent.putExtra(EXTRA_NOTIFICATION_CHANNEL_NAME, channelName);
        // Starting with Build.VERSION_CODES.S it is required to explicitly specify the mutability of PendingIntents.
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0;
        return PendingIntent.getActivity(context.getApplicationContext(),0, intent, flags);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mChannelName = getIntent().getStringExtra(EXTRA_NOTIFICATION_CHANNEL_NAME);
        mMessenger = getIntent().getParcelableExtra(EXTRA_MESSENGER);
        if (mChannelName == null || mMessenger == null) {
            Log.w(TAG, "Finishing because no channel name or messenger for returning the result was provided.");
            finish();
            return;
        }

        // When running on T or greater, with the app targeting less than T, creating a channel for the first time will
        // trigger the permission dialog.
        if (Build.VERSION.SDK_INT >= VERSION_T && getApplicationContext().getApplicationInfo().targetSdkVersion < VERSION_T) {
            NotificationUtils.createNotificationChannel(this, mChannelName);
        }

        ActivityCompat.requestPermissions(this, new String[]{PERMISSION_POST_NOTIFICATIONS}, 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        boolean enabled = false;
        for (int i = 0; i < permissions.length; i++) {
            if (!permissions[i].equals(PERMISSION_POST_NOTIFICATIONS)) continue;

            PrefUtils.setHasRequestedNotificationPermission(this);
            enabled = grantResults[i] == PackageManager.PERMISSION_GRANTED;
            break;
        }

        // This method will only receive the notification permission and its grant result when running on and
        // targeting >= T. Check whether notifications are actually enabled, perhaps because the system displayed a
        // permission dialog after the first notification channel was created and the user approved it.
        if (!enabled) {
            enabled = NotificationUtils.areNotificationsEnabled(this, mChannelName);
        }

        sendPermissionMessage(mMessenger, enabled);
        finish();
    }

    /**
     * Sends a message to the messenger containing the permission status.
     */
    private static void sendPermissionMessage(Messenger messenger, boolean enabled) {
        Bundle data = new Bundle();
        @PermissionStatus int status = enabled ? PermissionStatus.ALLOW : PermissionStatus.BLOCK;
        data.putInt(KEY_PERMISSION_STATUS, status);
        Message message = Message.obtain();
        message.setData(data);

        try {
            messenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
