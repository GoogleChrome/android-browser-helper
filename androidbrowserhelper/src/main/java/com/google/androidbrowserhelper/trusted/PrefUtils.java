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

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;

/**
 * Helper for using application level {@link SharedPreferences} in a consistent way, with the same
 * file name and using the application context.
 */
public class PrefUtils {
    private PrefUtils() {}

    private static final String SHARED_PREFERENCES_NAME = "com.google.androidbrowserhelper";
    private static final String KEY_HAS_REQUESTED_NOTIFICATION_PERMISSION = "HAS_REQUESTED_NOTIFICATION_PERMISSION";
    private static final String NOTIFICATION_PERMISSION_DENIED_FILE_NAME =
            "com.google.androidbrowserhelper.NOTIFICATION_PERMISSION_DENIED";
    private static final String TAG = "PrefUtils";

    /**
     * Returns the application level {@link SharedPreferences} using the application context.
     */
    public static SharedPreferences getAppSharedPreferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE);
    }

    /**
     * @deprecated No longer used. The flag lived in backed up {@link SharedPreferences}, so a
     * reinstall restored it while the notification permission itself was reset.
     */
    @Deprecated
    public static boolean hasRequestedNotificationPermission(Context context) {
        return getAppSharedPreferences(context).getBoolean(KEY_HAS_REQUESTED_NOTIFICATION_PERMISSION, false);
    }

    /**
     * @deprecated No longer used, see {@link #hasRequestedNotificationPermission}.
     */
    @Deprecated
    public static void setHasRequestedNotificationPermission(Context context) {
        getAppSharedPreferences(context).edit().putBoolean(KEY_HAS_REQUESTED_NOTIFICATION_PERMISSION, true).apply();
    }

    /**
     * Whether the user denied the notification permission in a way that keeps Android from
     * showing the permission dialog again.
     */
    public static boolean isNotificationPermissionDenied(Context context) {
        return getNotificationPermissionDeniedFile(context).exists();
    }

    /**
     * Records whether the user denied the notification permission in a way that keeps Android
     * from showing the permission dialog again.
     */
    public static void setNotificationPermissionDenied(Context context, boolean denied) {
        File file = getNotificationPermissionDeniedFile(context);
        try {
            if (denied) {
                file.createNewFile();
            } else {
                file.delete();
            }
        } catch (IOException e) {
            Log.w(TAG, "Could not record the notification permission state.", e);
        }
    }

    // Android resets the permission on reinstall but restores backed up files, so this marker
    // lives where backups never reach.
    private static File getNotificationPermissionDeniedFile(Context context) {
        return new File(ContextCompat.getNoBackupFilesDir(context),
                NOTIFICATION_PERMISSION_DENIED_FILE_NAME);
    }
}
