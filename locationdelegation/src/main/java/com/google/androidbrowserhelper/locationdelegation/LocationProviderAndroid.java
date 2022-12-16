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
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import java.util.List;

/**
 * This is a LocationProvider using Android APIs [1]. It is a separate class for clarity so that it
 * can manage all processing completely on the UI thread. The container class ensures that the
 * start/stop calls into this class are done on the UI thread.
 * [1] https://developer.android.com/reference/android/location/package-summary.html
 */
public class LocationProviderAndroid extends LocationProvider implements LocationListener {
    private static final String TAG = "TWA_LocationAndroid";

    private LocationManager mLocationManager;
    private boolean mIsRunning;
    private Context mContext;

    LocationProviderAndroid(Context context) {
        mContext = context;
    }

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

    @Override
    public void onLocationChanged(Location location) {
        // Callbacks from the system location service are queued to this thread, so it's possible
        // that we receive callbacks after unregistering.
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

    private void createLocationManagerIfNeeded() {
        if (mLocationManager != null) return;
        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        if (mLocationManager == null) {
            Log.e(TAG, "Could not get location manager.");
        }
    }

    /**
     * Registers this object with the location service.
     */
    private void registerForLocationUpdates(boolean enableHighAccuracy) {
        createLocationManagerIfNeeded();
        if (usePassiveOneShotLocation()) return;

        assert !mIsRunning;
        mIsRunning = true;

        try {
            Criteria criteria = new Criteria();
            if (enableHighAccuracy &&
                    mContext.checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                criteria.setAccuracy(Criteria.ACCURACY_FINE);
            }
            mLocationManager.requestLocationUpdates(0, 0, criteria,
                    this, Looper.getMainLooper());
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

        // Do not request a location update if the only available location provider is the passive
        // one. Make use of the last known location and call onNewLocationAvailable directly.
        Location location = null;
        try {
            location = mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        } catch (SecurityException | IllegalArgumentException e) {
            notifyLocationErrorWithMessage(
                    "Failed to request location updates: " + e.toString());
        }

        if (location != null) {
            // should called on main thread!!!
            onNewLocationAvailable(location);
        }
        return true;
    }

    /*
     * Checks if the passive location provider is the only provider available in the system.
     */
    private boolean isOnlyPassiveLocationProviderEnabled() {
        final List<String> providers = mLocationManager.getProviders(true);
        return providers != null && providers.size() == 1
                && providers.get(0).equals(LocationManager.PASSIVE_PROVIDER);
    }
}
