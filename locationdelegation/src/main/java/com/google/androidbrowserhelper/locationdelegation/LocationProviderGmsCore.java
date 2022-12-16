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
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;

import androidx.annotation.Nullable;

/**
 * This is a LocationProvider using Google Play Services.
 * https://developers.google.com/android/reference/com/google/android/gms/location/package-summary
 */
public class LocationProviderGmsCore extends LocationProvider {
    private static final String TAG = "TWA_LocationProviderGms";

    // Values for the LocationRequest's setInterval for normal and high accuracy, respectively.
    private static final long UPDATE_INTERVAL_MS = 1000;
    private static final long UPDATE_INTERVAL_FAST_MS = 500;

    final FusedLocationProviderClient mLocationProviderClient;

    private boolean mIsRunning;
    private Context mContext;

    @Nullable
    private LocationRequest mLocationRequest;

    private final LocationCallback mLocationCallback =
            new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult result) {
                    onNewLocationAvailable(result.getLastLocation());
                }

                @Override
                // Called when there is a change in the availability of location data.
                public void onLocationAvailability(LocationAvailability availability) {
                    if (!availability.isLocationAvailable()) {
                        unregisterFromLocationUpdates();
                        notifyLocationErrorWithMessage("Location not available.");
                    }
                }
            };

    LocationProviderGmsCore(Context context, FusedLocationProviderClient locationClient) {
        mContext = context;
        mLocationProviderClient = locationClient;
    }

    public static boolean isGooglePlayServicesAvailable(Context context) {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
                == ConnectionResult.SUCCESS;
    }

    // LocationProvider implementations
    @Override
    void start(TrustedWebActivityLocationCallback callback, boolean enableHighAccuracy) {
        unregisterFromLocationUpdates();
        mCallback = callback;
        registerForLocationUpdates(enableHighAccuracy);
    }

    @Override
    void stop() {
        unregisterFromLocationUpdates();
    }

    @Override
    boolean isRunning() {
        return mIsRunning;
    }

    private void registerForLocationUpdates(boolean enableHighAccuracy) {
        mLocationRequest = LocationRequest.create();
        if (enableHighAccuracy && mContext.checkCallingOrSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // With enableHighAccuracy, request a faster update interval and configure the provider
            // for high accuracy mode.
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(UPDATE_INTERVAL_FAST_MS);
        } else {
            mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                    .setInterval(UPDATE_INTERVAL_MS);
        }

        mLocationProviderClient.getLastLocation().continueWith(task -> {
            onNewLocationAvailable(task.getResult());
            return null;
        });

        try {
            mLocationProviderClient.requestLocationUpdates(
                    mLocationRequest, mLocationCallback, Looper.getMainLooper()).continueWith(task -> {
                        mIsRunning = task.isSuccessful();
                        if (!mIsRunning) {
                            notifyLocationErrorWithMessage("Unable to request location updates.");
                        }
                        return null;
            });
        } catch (IllegalStateException e) {
            // IllegalStateException is thrown "If this method is executed in a thread that has not
            // called Looper.prepare()".
            Log.e(TAG, " mLocationProviderApi.requestLocationUpdates() " + e);
            notifyLocationErrorWithMessage(
                    "Error when requesting location updates: " + e.toString());
        }
    }

    private void unregisterFromLocationUpdates() {
        if (!mIsRunning) return;
        mIsRunning = false;
        mCallback = null;
        mLocationProviderClient.removeLocationUpdates(mLocationCallback);
    }
}
