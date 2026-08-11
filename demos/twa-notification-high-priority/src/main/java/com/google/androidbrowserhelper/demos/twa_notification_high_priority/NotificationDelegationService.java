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
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.androidbrowserhelper.trusted.DelegationService;
import com.google.androidbrowserhelper.trusted.NotificationUtils;

/** Simple class to log notifications being sent from a higher
 *  priority channel, with debug logs.
 */
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
        Log.i(TAG, "Notification triggered for channel: " + channelName
                + " with importance: "
                + NotificationUtils.getNotificationImportance(this));
        return true;
    }
}
