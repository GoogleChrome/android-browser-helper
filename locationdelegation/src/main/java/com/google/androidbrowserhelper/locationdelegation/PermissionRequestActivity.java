// Copyright 2020 Google Inc. All Rights Reserved.
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

package com.google.androidbrowserhelper.locationdelegation;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.TextUtils;

import java.util.Arrays;

import androidx.browser.trusted.TrustedWebActivityCallbackRemote;
import androidx.core.app.ActivityCompat;

/**
 * This is a simple transparent activity that will bring up the permission prompt. On either approve
 * or disapprove, this will send the result via the {@link Messenger} provided with the intent, and
 * then finish.
 */
public class PermissionRequestActivity extends Activity {
    private static final String LOCATION_PERMISSION_RESULT = "locationPermissionResult";

    private static final String EXTRA_RESULT_RECEIVER = "EXTRA_RESULT_RECEIVER";
    private static final String EXTRA_PERMISSIONS = "EXTRA_PERMISSIONS";
    private static final String EXTRA_GRANT_RESULTS = "EXTRA_GRANTED_RESULT";

    private static final String[] LOCATION_PERMISSION =
            {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    private Messenger mMessenger;

    /**
     * Launches the {@link PermissionRequestActivity} to request location permission, and then sends
     * the granted result back to browser via {@link TrustedWebActivityCallbackRemote}.
     */
    public static void requestLocationPermission(
            Context context, TrustedWebActivityCallbackRemote callback) {
        Handler handler = new Handler(Looper.getMainLooper(), message -> {
            Bundle data = message.getData();
            notifyLocationPermissionResult(callback, data.getStringArray(EXTRA_PERMISSIONS),
                    data.getIntArray(EXTRA_GRANT_RESULTS));
            return true;
        });

        final Messenger messenger = new Messenger(handler);
        Intent intent = new Intent(context, PermissionRequestActivity.class);
        intent.putExtra(EXTRA_PERMISSIONS, LOCATION_PERMISSION);
        intent.putExtra(EXTRA_RESULT_RECEIVER, messenger);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private static void notifyLocationPermissionResult(
            TrustedWebActivityCallbackRemote callback, String[] permissions, int[] grantedResult) {
        Bundle result = new Bundle();
        for (int i = 0; i < permissions.length; i++) {
            if (Arrays.asList(LOCATION_PERMISSION).contains(permissions[i])) {
                result.putBoolean(LOCATION_PERMISSION_RESULT,
                        grantedResult[i] == PackageManager.PERMISSION_GRANTED);
            }
        }
        try {
            callback.runExtraCallback(
                    LocationDelegationExtraCommandHandler.CHECK_LOCATION_PERMISSION_COMMAND_NAME,
                    result);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String[] permissions = getIntent().getStringArrayExtra(EXTRA_PERMISSIONS);
        mMessenger = getIntent().getParcelableExtra(EXTRA_RESULT_RECEIVER);
        ActivityCompat.requestPermissions(this, permissions, 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
        Message message = new Message();
        Bundle data = new Bundle();
        data.putStringArray(EXTRA_PERMISSIONS, permissions);
        data.putIntArray(EXTRA_GRANT_RESULTS, grantResults);
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
