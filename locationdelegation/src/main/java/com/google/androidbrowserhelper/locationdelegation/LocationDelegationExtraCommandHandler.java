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

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.trusted.TrustedWebActivityCallbackRemote;

import com.google.androidbrowserhelper.trusted.ExtraCommandHandler;

public class LocationDelegationExtraCommandHandler implements ExtraCommandHandler {
    static final String CHECK_LOCATION_PERMISSION_COMMAND_NAME = "checkAndroidLocationPermission";
    private static final String START_LOCATION_COMMAND_NAME = "startLocation";
    private static final String STOP_LOCATION_COMMAND_NAME = "stopLocation";

    private LocationProvider mLocationProvider;

    public Bundle handleExtraCommand(Context context, String commandName, Bundle args,
            @Nullable TrustedWebActivityCallbackRemote callback) {
        TrustedWebActivityLocationCallback wrappedCallback = (callbackName, callbackArgs) -> {
            try {
                if (callback != null) {
                    callback.runExtraCallback(callbackName, callbackArgs);
                }
            } catch (RemoteException e) {
                // The remote app crashed/got shut down. Stop location provider.
                stopLocationProvider(context);
            }
        };

        Bundle result = new Bundle();
        result.putBoolean(EXTRA_COMMAND_SUCCESS, false);
        switch (commandName) {
            case CHECK_LOCATION_PERMISSION_COMMAND_NAME:
                if (callback == null) break;
                requestPermission(context, callback);
                result.putBoolean(EXTRA_COMMAND_SUCCESS, true);
                break;
            case START_LOCATION_COMMAND_NAME:
                if (callback == null) break;
                startLocationProvider(context, wrappedCallback, args.getBoolean("enableHighAccuracy"));

                result.putBoolean(EXTRA_COMMAND_SUCCESS, true);
                break;
            case STOP_LOCATION_COMMAND_NAME:
                stopLocationProvider(context);

                result.putBoolean(EXTRA_COMMAND_SUCCESS, true);
                break;
        }
        return result;
    }

    private void requestPermission(Context context,
            @NonNull TrustedWebActivityCallbackRemote callback) {
        PermissionRequestActivity.requestLocationPermission(context, callback);
    }

    private void startLocationProvider(
            Context context, @NonNull TrustedWebActivityLocationCallback locationChangeCallback,
            boolean enableHighAccuracy) {
        getLocationProvider(context).start(locationChangeCallback, enableHighAccuracy);
    }

    private void stopLocationProvider(Context context) {
        getLocationProvider(context).stop();
    }

    private LocationProvider getLocationProvider(Context context) {
        if (mLocationProvider == null){
            mLocationProvider = LocationProvider.create(context);
        }
        return mLocationProvider;
    }
}
