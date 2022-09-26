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

/**
 * Helper for using application level {@link SharedPreferences} in a consistent way, with the same
 * file name and using the application context.
 */
public class PrefUtils {
    private PrefUtils() {}

    private static final String SHARED_PREFERENCES_NAME = "com.google.androidbrowserhelper";
    private static final String KEY_HAS_REQUESTED_NOTIFICATION_PERMISSION = "HAS_REQUESTED_NOTIFICATION_PERMISSION";

    /**
     * Returns the application level {@link SharedPreferences} using the application context.
     */
    public static SharedPreferences getAppSharedPreferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE);
    }

    public static boolean hasRequestedNotificationPermission(Context context) {
        return getAppSharedPreferences(context).getBoolean(KEY_HAS_REQUESTED_NOTIFICATION_PERMISSION, false);
    }

    public static void setHasRequestedNotificationPermission(Context context) {
        getAppSharedPreferences(context).edit().putBoolean(KEY_HAS_REQUESTED_NOTIFICATION_PERMISSION, true).apply();
    }
}
