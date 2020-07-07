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
import android.os.Looper;
import android.os.RemoteException;
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
 * <p>
 * https://developers.google.com/android/reference/com/google/android/gms/location/package-summary
 */
public class LocationProviderGmsCore
        implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener {
    private static final String TAG = "TWA_LocationProviderGms";

    private static final String EXTRA_NEW_LOCATION_AVAILABLE_CALLBACK = "onNewLocationAvailable";
    private static final String EXTRA_NEW_ERROR_AVAILABLE_CALLBACK = "onNewErrorAvailable";

    // Values for the LocationRequest's setInterval for normal and high accuracy, respectively.
    private static final long UPDATE_INTERVAL_MS = 1000;
    private static final long UPDATE_INTERVAL_FAST_MS = 500;

    private static LocationProviderGmsCore sProvider;
    private final GoogleApiClient mGoogleApiClient;
    private FusedLocationProviderApi mLocationProviderApi = LocationServices.FusedLocationApi;

    private TrustedWebActivityCallbackRemote mCallback;

    private boolean mEnablehighAccuracy;
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

    public static LocationProviderGmsCore getInstance(Context context) {
        if (sProvider == null) {
            sProvider = new LocationProviderGmsCore(context);
        }
        return sProvider;
    }

    // ConnectionCallbacks implementation
    @Override
    public void onConnected(Bundle connectionHint) {
        mLocationRequest = LocationRequest.create();
        if (mEnablehighAccuracy) {
            // With enableHighAccuracy, request a faster update interval and configure the provider
            // for high accuracy mode.
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(UPDATE_INTERVAL_FAST_MS);
        } else {
            mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                    .setInterval(UPDATE_INTERVAL_MS);
        }

        final Location location = mLocationProviderApi.getLastLocation(mGoogleApiClient);
        if (location != null) {
            onNewLocationAvailable(location);
        }

        try {
            // Request updates on UI Thread replicating LocationProviderAndroid's behaviour.
            mLocationProviderApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this, Looper.getMainLooper());
        } catch (IllegalStateException | SecurityException e) {
            // IllegalStateException is thrown "If this method is executed in a thread that has not
            // called Looper.prepare()". SecurityException is thrown if there is no permission, see
            // https://crbug.com/731271.
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

    public void start(TrustedWebActivityCallbackRemote callback, boolean enableHighAccuracy) {
        if (mGoogleApiClient.isConnected()) mGoogleApiClient.disconnect();
        mCallback = callback;

        mEnablehighAccuracy = enableHighAccuracy;
        mGoogleApiClient.connect(); // Should return via onConnected().
    }

    public void stop() {
        if (!mGoogleApiClient.isConnected()) return;

        mLocationProviderApi.removeLocationUpdates(mGoogleApiClient, this);

        mGoogleApiClient.disconnect();
        mCallback = null;
    }

    public boolean isRunning() {
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

    private void onNewLocationAvailable(Location location) {
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
            Log.e(TAG,"Caught RemoteException sending location update callback." );
            stop();
        }
    }

    private void notifyLocationErrorWithMessage(String message) {
        try {
            Bundle locationResult = new Bundle();
            locationResult.putString("message", message);
            mCallback.runExtraCallback(EXTRA_NEW_ERROR_AVAILABLE_CALLBACK, locationResult);
        } catch (RemoteException e) {
            Log.e(TAG,"Caught RemoteException sending location error callback." );
            stop();
        }
    }
}
