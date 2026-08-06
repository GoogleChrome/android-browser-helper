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
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.NotificationManagerCompat;
import java.util.Locale;

/**
 * Helper for interacting with the notification manager and channels.
 */
public class NotificationUtils {
    /**
     * Metadata key to specify whether notification channels should be created with high importance.
     */
    public static final String METADATA_USE_HIGH_PRI_NOTIFICATIONS =
            "android.support.customtabs.trusted.USE_HIGH_PRI_NOTIFICATIONS";

    /**
     * AndroidX alias for {@link #METADATA_USE_HIGH_PRI_NOTIFICATIONS}.
     */
    public static final String METADATA_USE_HIGH_PRI_NOTIFICATIONS_ANDROIDX =
            "androidx.browser.trusted.USE_HIGH_PRI_NOTIFICATIONS";

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
     * Checks if high-priority notifications are configured in the manifest metadata.
     */
    public static boolean shouldUseHighPriorityNotifications(Context context) {
        if (context == null) return false;
        try {
            Bundle metaData = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA).metaData;
            if (metaData != null && isHighPriorityInBundle(metaData)) {
                return true;
            }

            if (context instanceof Activity) {
                Activity activity = (Activity) context;
                ActivityInfo activityInfo = activity.getPackageManager().getActivityInfo(
                        activity.getComponentName(), PackageManager.GET_META_DATA);
                if (activityInfo != null && activityInfo.metaData != null
                        && isHighPriorityInBundle(activityInfo.metaData)) {
                    return true;
                }
            } else if (context instanceof Service) {
                Service service = (Service) context;
                ServiceInfo serviceInfo = service.getPackageManager().getServiceInfo(
                        new ComponentName(service, service.getClass()), PackageManager.GET_META_DATA);
                if (serviceInfo != null && serviceInfo.metaData != null
                        && isHighPriorityInBundle(serviceInfo.metaData)) {
                    return true;
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            // Package or component not found; fallback to default.
        }
        return false;
    }

    private static boolean isHighPriorityInBundle(Bundle metaData) {
        if (metaData.containsKey(METADATA_USE_HIGH_PRI_NOTIFICATIONS)) {
            Object val = metaData.get(METADATA_USE_HIGH_PRI_NOTIFICATIONS);
            if (parseBooleanValue(val)) return true;
        }
        if (metaData.containsKey(METADATA_USE_HIGH_PRI_NOTIFICATIONS_ANDROIDX)) {
            Object val = metaData.get(METADATA_USE_HIGH_PRI_NOTIFICATIONS_ANDROIDX);
            if (parseBooleanValue(val)) return true;
        }
        return false;
    }

    private static boolean parseBooleanValue(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return false;
    }

    /**
     * Returns the notification importance level resolved from the application manifest metadata.
     */
    public static int getNotificationImportance(Context context) {
        return shouldUseHighPriorityNotifications(context)
                ? NotificationManager.IMPORTANCE_HIGH
                : NotificationManager.IMPORTANCE_DEFAULT;
    }

    /**
     * Creates a notification channel using the given channel name and the importance level resolved
     * from manifest metadata.
     */
    public static void createNotificationChannel(Context context, String channelName) {
        createNotificationChannel(context, channelName, getNotificationImportance(context));
    }

    /**
     * Creates a notification channel using the given channel name and explicit importance level.
     */
    public static void createNotificationChannel(Context context, String channelName, int importance) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationChannel channel = new NotificationChannel(channelNameToId(channelName),
                channelName, importance);
        NotificationManagerCompat.from(context).createNotificationChannel(channel);
    }

    /**
     * Generates a notification channel id from a channel name.
     * TODO: Remove this when we can use the method defined in AndroidX instead.
     */
    static String channelNameToId(String name) {
        return name.toLowerCase(Locale.ROOT).replace(' ', '_') + "_channel_id";
    }
}
