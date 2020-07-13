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
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;

import androidx.browser.trusted.TrustedWebActivityCallbackRemote;

/**
 * This is a LocationProvider using Android APIs [1]. It is a separate class for clarity
 * so that it can manage all processing completely on the UI thread. The container class
 * ensures that the start/stop calls into this class are done on the UI thread.
 * <p>
 * [1] https://developer.android.com/reference/android/location/package-summary.html
 */
public class LocationProviderAndroid implements LocationListener {
    private static final String TAG = "TWA_LocationProvider";

    private static final String EXTRA_NEW_LOCATION_AVAILABLE_CALLBACK = "onNewLocationAvailable";
    private static final String EXTRA_NEW_LOCATION_ERROR_CALLBACK= "onNewLocationError";

    private LocationManager mLocationManager;
    private boolean mIsRunning;
    private TrustedWebActivityCallbackRemote mCallback;
    private static LocationProviderAndroid sProvider;

    public static LocationProviderAndroid getInstance() {
        if (sProvider == null) {
            sProvider = new LocationProviderAndroid();
        }
        return sProvider;
    }

    public void start(
            Context context, TrustedWebActivityCallbackRemote callback, boolean enableHighAccuracy) {
        unregisterFromLocationUpdates();
        mCallback = callback;
        registerForLocationUpdates(context, enableHighAccuracy);
    }

    public void stop() {
        unregisterFromLocationUpdates();
    }

    public boolean isRunning() {
        return mIsRunning;
    }

    @Override
    public void onLocationChanged(Location location) {
        // Callbacks from the system location service are queued to this thread, so it's
        // possible that we receive callbacks after unregistering. At this point, the
        // native object will no longer exist.
        if (mIsRunning) {
            onNewLocationAvailable(location);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    private void createLocationManagerIfNeeded(Context context) {
        if (mLocationManager != null) return;
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (mLocationManager == null) {
            Log.e(TAG, "Could not get location manager.");
        }
    }

    /**
     * Registers this object with the location service.
     */
    private void registerForLocationUpdates(Context context, boolean enableHighAccuracy) {
        createLocationManagerIfNeeded(context);
        if (usePassiveOneShotLocation()) return;

        assert !mIsRunning;
        mIsRunning = true;

        // We're running on the main thread. The C++ side is responsible to
        // bounce notifications to the Geolocation thread as they arrive in the mainLooper.
        try {
            Criteria criteria = new Criteria();
            if (enableHighAccuracy) criteria.setAccuracy(Criteria.ACCURACY_FINE);
            mLocationManager.requestLocationUpdates(0, 0, criteria, this, Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e(TAG,
                    "Caught security exception while registering for location updates "
                            + "from the system. The application does not have sufficient "
                            + "geolocation permissions.");
            unregisterFromLocationUpdates();
            // Propagate an error to JavaScript, this can happen in case of WebView
            // when the embedding app does not have sufficient permissions.
            notifyLocationErrorWithMessage("Application does not have sufficient geolocation permissions.");
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Caught IllegalArgumentException registering for location updates.");
            unregisterFromLocationUpdates();
        }
    }

    /**
     * Unregisters this object from the location service.
     */
    private void unregisterFromLocationUpdates() {
        if (!mIsRunning) return;
        mIsRunning = false;
        mLocationManager.removeUpdates(this);
    }

    private boolean usePassiveOneShotLocation() {
        if (!isOnlyPassiveLocationProviderEnabled()) {
            return false;
        }

        // Do not request a location update if the only available location provider is
        // the passive one. Make use of the last known location and call
        // onNewLocationAvailable directly.
        final Location location =
                mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        if (location != null) {
            // should called on main thread!!!
            onNewLocationAvailable(location);
        }
        return true;
    }

    /*
     * Checks if the passive location provider is the only provider available
     * in the system.
     */
    private boolean isOnlyPassiveLocationProviderEnabled() {
        final List<String> providers = mLocationManager.getProviders(true);
        return providers != null && providers.size() == 1
                && providers.get(0).equals(LocationManager.PASSIVE_PROVIDER);
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
            mCallback.runExtraCallback(EXTRA_NEW_LOCATION_ERROR_CALLBACK, locationResult);
        } catch (RemoteException e) {
            Log.e(TAG,"Caught RemoteException sending location error callback." );
            stop();
        }
    }
}
