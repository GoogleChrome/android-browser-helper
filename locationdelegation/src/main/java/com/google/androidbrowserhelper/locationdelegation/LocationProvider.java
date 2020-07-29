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
import android.location.Location;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import androidx.browser.trusted.TrustedWebActivityCallbackRemote;

/**
 * Abstract LocationProvider class for start and stop getting location updates.
 */
public abstract class LocationProvider {
    private static final String TAG = "TWA_LocationProvider";
    private static final String EXTRA_NEW_LOCATION_AVAILABLE_CALLBACK = "onNewLocationAvailable";
    private static final String EXTRA_NEW_ERROR_AVAILABLE_CALLBACK = "onNewErrorAvailable";

    protected TrustedWebActivityCallbackRemote mCallback;

    static LocationProvider create(Context context) {
        if (LocationProviderGmsCore.isGooglePlayServicesAvailable(context)) {
            return new LocationProviderGmsCore(context);
        }
        return new LocationProviderAndroid(context);
    }

    /**
     * Start listening for location updates. Calling several times before stop() is interpreted as
     * restart.
     *
     * @param callback           Callback to provide location updates.
     * @param enableHighAccuracy Whether or not to enable high accuracy location.
     */
    abstract void start(TrustedWebActivityCallbackRemote callback, boolean enableHighAccuracy);

    /**
     * Stop listening for location updates.
     */
    abstract void stop();

    /**
     * Returns true if we are currently listening for location updates, false if not.
     */
    abstract boolean isRunning();

    protected void onNewLocationAvailable(Location location) {
        Bundle locationResult = new Bundle();
        locationResult.putDouble("latitude", location.getLatitude());
        locationResult.putDouble("longitude", location.getLongitude());
        locationResult.putLong("timeStamp", location.getTime());
        if (location.hasAltitude()) {
            locationResult.putDouble("altitude", location.getAltitude());
        }
        if (location.hasAccuracy()) {
            locationResult.putDouble("accuracy", location.getAccuracy());
        }
        if (location.hasBearing()) {
            locationResult.putDouble("bearing", location.getBearing());
        }
        if (location.hasSpeed()) {
            locationResult.putDouble("speed", location.getSpeed());
        }

        try {
            mCallback.runExtraCallback(EXTRA_NEW_LOCATION_AVAILABLE_CALLBACK, locationResult);
        } catch (RemoteException e) {
            Log.e(TAG, "Caught RemoteException sending location update callback.");
            stop();
        }
    }

    protected void notifyLocationErrorWithMessage(String message) {
        try {
            Bundle locationResult = new Bundle();
            locationResult.putString("message", message);
            mCallback.runExtraCallback(EXTRA_NEW_ERROR_AVAILABLE_CALLBACK, locationResult);
        } catch (RemoteException e) {
            Log.e(TAG, "Caught RemoteException sending location error callback.");
            stop();
        }
    }
}
