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

package com.google.androidbrowserhelper.trusted;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.trusted.TokenStore;
import androidx.browser.trusted.TrustedWebActivityCallbackRemote;

/**
 * An extension of {@link androidx.browser.trusted.TrustedWebActivityService} that implements
 * {@link androidx.browser.trusted.TrustedWebActivityService#getTokenStore()} using a
 * {@link SharedPreferencesTokenStore}.
 */
public class DelegationService extends androidx.browser.trusted.TrustedWebActivityService {
    static final String CHECK_LOCATION_PERMISSION_COMMAND_NAME = "checkAndroidLocationPermission";
    private static final String START_LOCATION_COMMAND_NAME = "startLocation";
    private static final String STOP_LOCATION_COMMAND_NAME = "stopLocation";
    private static final String EXTRA_COMMAND_SUCCESS = "success";

    private boolean mIsProviderGmsCore;

    @NonNull
    @Override
    public TokenStore getTokenStore() {
        return new SharedPreferencesTokenStore(this);
    }

    @Nullable
    @Override
    public Bundle onExtraCommand(
            String commandName, Bundle args, @Nullable TrustedWebActivityCallbackRemote callback) {
        Bundle result = new Bundle();
        result.putBoolean(EXTRA_COMMAND_SUCCESS, false);
        switch (commandName) {
            case CHECK_LOCATION_PERMISSION_COMMAND_NAME:
                if (callback == null) break;
                requestPermission(callback);

                result.putBoolean(EXTRA_COMMAND_SUCCESS, true);
                break;
            case START_LOCATION_COMMAND_NAME:
                if (callback == null) break;
                startLocationProvider(callback, args.getBoolean("enableHighAccuracy"));

                result.putBoolean(EXTRA_COMMAND_SUCCESS, true);
                break;
            case STOP_LOCATION_COMMAND_NAME:
                stopLocationProvider();

                result.putBoolean(EXTRA_COMMAND_SUCCESS, true);
                break;
        }
        return result;
    }

    private void requestPermission(@NonNull TrustedWebActivityCallbackRemote callback) {
        PermissionRequestActivity.requestLocationPermission(this, callback);
    }


    private void startLocationProvider(
            @NonNull TrustedWebActivityCallbackRemote locationChangeCallback,
            boolean enableHighAccuracy) {
        mIsProviderGmsCore = LocationProviderGmsCore.isGooglePlayServicesAvailable(this);
        if (mIsProviderGmsCore) {
            LocationProviderGmsCore.getInstance(this).start(
                    locationChangeCallback, enableHighAccuracy);
        } else {
            LocationProviderAndroid.getInstance().start(
                    this, locationChangeCallback, enableHighAccuracy);
        }
    }

    private void stopLocationProvider() {
        if (mIsProviderGmsCore) {
            LocationProviderGmsCore.getInstance(this).stop();
        } else {
            LocationProviderAndroid.getInstance().stop();
        }
    }
}
