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

package com.google.androidbrowserhelper.demos.twa_notification_delegation;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;

import com.google.androidbrowserhelper.trusted.DelegationService;

public class NotificationDelegationService extends DelegationService {
    @Override
    public boolean onNotifyNotificationWithChannel(
            @NonNull String platformTag,
            int platformId,
            @NonNull Notification notification,
            @NonNull String channelName) {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Uri airhornUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                this.getPackageName() + "/" + R.raw.airhorn);

        // Notification.Builder.recoverBuilder() was introduce in Nougat, so we prefer it when
        // available.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Notification.Builder builder =
                    Notification.Builder.recoverBuilder(this, notification);

            // From Android O and above, the sound is set on the Channel instead of the notification.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setChannelId(channelName);

                // The first parameter of the call below is an ID for the channel and the second
                // is a human readable string for the name. We're using the same value for both
                // in this example.
                NotificationChannel channel = new NotificationChannel(
                        channelName, channelName, NotificationManager.IMPORTANCE_HIGH);
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build();
                channel.setSound(airhornUri, audioAttributes);

                // Warning: NotificationChannels are immutable. This call will create a notification
                // channel if one does not already exist, but it won't update an existing one.
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
