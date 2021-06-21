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
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import androidx.browser.trusted.TrustedWebActivityCallbackRemote;

/**
 * This is a LocationProvider using Google Play Services.
 * https://developers.google.com/android/reference/com/google/android/gms/location/package-summary
 */
public class LocationProviderGmsCore extends LocationProvider
        implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener {
    private static final String TAG = "TWA_LocationProviderGms";

    // Values for the LocationRequest's setInterval for normal and high accuracy, respectively.
    private static final long UPDATE_INTERVAL_MS = 1000;
    private static final long UPDATE_INTERVAL_FAST_MS = 500;

    private static LocationProviderGmsCore sProvider;
    private final GoogleApiClient mGoogleApiClient;
    private final FusedLocationProviderApi mLocationProviderApi = LocationServices.FusedLocationApi;

    private boolean mEnableHighAccuracy;
    private LocationRequest mLocationRequest;

    LocationProviderGmsCore(Context context) {
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    public static boolean isGooglePlayServicesAvailable(Context context) {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
                == ConnectionResult.SUCCESS;
    }

    // ConnectionCallbacks implementation
    @Override
    public void onConnected(Bundle connectionHint) {
        mLocationRequest = LocationRequest.create();
        if (mEnableHighAccuracy
                && mGoogleApiClient.getContext().checkCallingOrSelfPermission(
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

        try {
            final Location location = mLocationProviderApi.getLastLocation(mGoogleApiClient);
            if (location != null) {
                onNewLocationAvailable(location);
            }

            mLocationProviderApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this, Looper.getMainLooper());
        } catch (IllegalStateException | SecurityException e) {
            // IllegalStateException is thrown "If this method is executed in a thread that has not
            // called Looper.prepare()". SecurityException is thrown if there is no permission.
            Log.e(TAG, " mLocationProviderApi.requestLocationUpdates() " + e);
            notifyLocationErrorWithMessage(
                    "Failed to request location updates: " + e.toString());
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
    }

    // OnConnectionFailedListener implementation
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        notifyLocationErrorWithMessage("Failed to connect to Google Play Services: " + result.toString());
    }

    // LocationProvider implementations
    @Override
    void start(TrustedWebActivityCallbackRemote callback, boolean enableHighAccuracy) {
        if (mGoogleApiClient.isConnected()) mGoogleApiClient.disconnect();
        mCallback = callback;
        mEnableHighAccuracy = enableHighAccuracy;

        mGoogleApiClient.connect(); // Should return via onConnected().
    }

    @Override
    void stop() {
        if (!mGoogleApiClient.isConnected()) return;

        mLocationProviderApi.removeLocationUpdates(mGoogleApiClient, this);

        mGoogleApiClient.disconnect();
        mCallback = null;
    }

    @Override
    boolean isRunning() {
        if (mGoogleApiClient == null) return false;
        return mGoogleApiClient.isConnecting() || mGoogleApiClient.isConnected();
    }

    // LocationListener implementation
    @Override
    public void onLocationChanged(Location location) {
        if (isRunning()) {
            onNewLocationAvailable(location);
        }
    }
}
