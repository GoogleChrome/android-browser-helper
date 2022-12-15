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

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationManagerCompat;
import java.util.Locale;

/**
 * Helper for interacting with the notification manager and channels.
 */
public class NotificationUtils {
    private NotificationUtils() {}

    /**
     * Returns true if notifications are enabled and either the channel does not exist or it has not been disabled.
     */
    public static boolean areNotificationsEnabled(Context context, String channelName) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true;

        NotificationChannel channel =
                    NotificationManagerCompat.from(context).getNotificationChannel(channelNameToId(channelName));
        return channel == null || channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
    }

    /**
     * Creates a notification channel using the given channel name.
     */
    public static void createNotificationChannel(Context context, String channelName) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationChannel channel = new NotificationChannel(channelNameToId(channelName),
                channelName, NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManagerCompat.from(context).createNotificationChannel(channel);
    }

    /**
     * Generates a notification channel id from a channel name.
     * TODO: Remove this when we can use the method defined in AndroidX instead.
     */
    private static String channelNameToId(String name) {
        return name.toLowerCase(Locale.ROOT).replace(' ', '_') + "_channel_id";
    }
}
