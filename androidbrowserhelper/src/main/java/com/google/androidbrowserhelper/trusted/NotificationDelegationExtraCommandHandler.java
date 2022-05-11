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

import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.trusted.TrustedWebActivityCallbackRemote;

/**
 * Handles extra commands related to notification delegation such as checking and requesting permission.
 */
public class NotificationDelegationExtraCommandHandler implements ExtraCommandHandler {
    static final String COMMAND_CHECK_NOTIFICATION_PERMISSION =
            "checkNotificationPermission";
    private static final String COMMAND_GET_NOTIFICATION_PERMISSION_REQUEST_PENDING_INTENT =
            "getNotificationPermissionRequestPendingIntent";
    private static final String KEY_NOTIFICATION_CHANNEL_NAME = "notificationChannelName";
    private static final String KEY_NOTIFICATION_PERMISSION_REQUEST_PENDING_INTENT =
            "notificationPermissionRequestPendingIntent";

    @NonNull
    @Override
    public Bundle handleExtraCommand(Context context, @NonNull String commandName, @NonNull Bundle args,
            @Nullable TrustedWebActivityCallbackRemote callback) {
        Bundle commandResult = new Bundle();
        commandResult.putBoolean(EXTRA_COMMAND_SUCCESS, false);
        String channelName = args.getString(KEY_NOTIFICATION_CHANNEL_NAME);
        switch (commandName) {
            case COMMAND_CHECK_NOTIFICATION_PERMISSION:
                if (TextUtils.isEmpty(channelName)) break;

                boolean enabled = NotificationUtils.areNotificationsEnabled(context, channelName);
                @PermissionStatus int status = enabled ? PermissionStatus.ALLOW : PermissionStatus.BLOCK;
                if (status == PermissionStatus.BLOCK && !PrefUtils.hasRequestedNotificationPermission(context)) {
                    status = PermissionStatus.ASK;
                }
                commandResult.putInt(NotificationPermissionRequestActivity.KEY_PERMISSION_STATUS, status);
                commandResult.putBoolean(EXTRA_COMMAND_SUCCESS, true);
                break;
            case COMMAND_GET_NOTIFICATION_PERMISSION_REQUEST_PENDING_INTENT:
              if (TextUtils.isEmpty(channelName)) break;

              PendingIntent pendingIntent = NotificationPermissionRequestActivity.createPermissionRequestPendingIntent(
                    context, channelName);
              commandResult.putParcelable(KEY_NOTIFICATION_PERMISSION_REQUEST_PENDING_INTENT, pendingIntent);
              commandResult.putBoolean(EXTRA_COMMAND_SUCCESS, true);
              break;
        }
        return commandResult;
    }
}
