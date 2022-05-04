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
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import android.util.Log;
import androidx.core.app.ActivityCompat;

/**
 * This is a simple transparent activity that will bring up the permission prompt. On either approve
 * or disapprove, this will send the result via the {@link Messenger} provided with the intent, and
 * then finish.
 */
public class PermissionRequestActivity extends Activity {
  private static final String TAG = "PermissionRequestActivity";

  static final String KEY_PERMISSION_STATUS = "permissionStatus";

  // TODO: Replace with Manifest.permission.POST_NOTIFICATIONS when targeting T.
  private static final String PERMISSION_POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS";

  private static final String EXTRA_MESSENGER = "messenger";
  private static final String EXTRA_PERMISSION = "permission";

  private String mRequestedPermission;
  private Messenger mMessenger;

  /**
   * Creates a {@link PendingIntent} for launching this activity to request the notification permission. It is mutable
   * so that a messenger extra can be added for returning the permission request result.
   */
  public static PendingIntent createNotificationPermissionRequestPendingIntent(Context context) {
    Intent intent = new Intent(context.getApplicationContext(), PermissionRequestActivity.class);
    intent.putExtra(EXTRA_PERMISSION, PERMISSION_POST_NOTIFICATIONS);
    return PendingIntent.getActivity(context.getApplicationContext(),0, intent, PendingIntent.FLAG_MUTABLE);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mRequestedPermission = getIntent().getStringExtra(EXTRA_PERMISSION);
    mMessenger = getIntent().getParcelableExtra(EXTRA_MESSENGER);
    if (mRequestedPermission == null || mMessenger == null) {
        Log.w(TAG, "PermissionRequestActivity.onCreate missing input");
        finish();
    }
    ActivityCompat.requestPermissions(this, new String[]{mRequestedPermission}, 0);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    Message message = Message.obtain();
    Bundle data = new Bundle();
    for (int i = 0; i < permissions.length; i++) {
          if (permissions[i].equals(mRequestedPermission)) {
              if (PERMISSION_POST_NOTIFICATIONS.equals(mRequestedPermission)) {
                PrefUtils.setHasRequestedNotificationPermission(this, true);
              }
              @PermissionStatus int status = grantResults[i] == PackageManager.PERMISSION_GRANTED ? PermissionStatus.ALLOW : PermissionStatus.BLOCK;
              data.putInt(KEY_PERMISSION_STATUS, status);
              break;
          }
    }
    message.setData(data);
    try {
        mMessenger.send(message);
    } catch (RemoteException e) {
        e.printStackTrace();
    } finally {
        finish();
    }
  }
}
