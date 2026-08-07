// Copyright 2024 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.androidbrowserhelper.demos.twa_notification_high_priority;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.androidbrowserhelper.trusted.DelegationService;
import com.google.androidbrowserhelper.trusted.NotificationUtils;

public class NotificationDelegationService extends DelegationService {
    private static final String TAG = "NotificationDelegation";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "NotificationDelegationService onCreate() triggered");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "NotificationDelegationService onDestroy() triggered");
    }

    @Override
    public boolean onNotifyNotificationWithChannel(
            @NonNull String platformTag,
            int platformId,
            @NonNull Notification notification,
            @NonNull String channelName) {
        int importance = NotificationUtils.getNotificationImportance(this);
        Log.i(TAG, "Notification triggered for channel: " + channelName
                + " (ID: " + NotificationUtils.channelNameToId(channelName) + ")"
                + " with importance: " + importance
                + " (HIGH=" + (importance == NotificationManager.IMPORTANCE_HIGH) + ")");

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Uri airhornUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                this.getPackageName() + "/" + R.raw.airhorn);

        // Notification.Builder.recoverBuilder() was introduced in Nougat, so we prefer it when
        // available.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Notification.Builder builder =
                    Notification.Builder.recoverBuilder(this, notification);

            // Ensure high priority is set on the builder for heads-up presentation
            builder.setPriority(Notification.PRIORITY_HIGH);

            // From Android O and above, importance and sound are set on the Channel.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String channelId = NotificationUtils.channelNameToId(channelName);
                builder.setChannelId(channelId);

                // Creates or updates the notification channel with configured importance, sound, and vibration
                NotificationChannel channel = new NotificationChannel(
                        channelId, channelName, importance);
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build();
                channel.setSound(airhornUri, audioAttributes);
                channel.enableVibration(true);

                mNotificationManager.createNotificationChannel(channel);
            }

            builder.setSound(airhornUri);
            notification = builder.build();
        } else {
            notification.sound = airhornUri;
        }

        mNotificationManager.notify(platformTag, platformId, notification);
        return true;
    }
}
