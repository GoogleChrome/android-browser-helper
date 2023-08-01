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

package com.google.androidbrowserhelper.locationdelegation;

import static com.google.androidbrowserhelper.locationdelegation.LocationProvider.EXTRA_NEW_LOCATION_AVAILABLE_CALLBACK;
import static com.google.androidbrowserhelper.locationdelegation.LocationProvider.EXTRA_NEW_ERROR_AVAILABLE_CALLBACK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import androidx.test.core.app.ApplicationProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Tasks;
import com.google.common.collect.ImmutableList;

/** Tests for {@link LocationProviderGmsCore}. */
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@DoNotInstrument
@Config(sdk = {Build.VERSION_CODES.O_MR1})
public class LocationProviderGmsCoreTest {
    private LocationProviderGmsCore mLocationProvider;

    private FusedLocationProviderClient mMockLocationClient;
    private LocationCallback mPendingCallback;

    @Before
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        mMockLocationClient = spy(LocationServices.getFusedLocationProviderClient(context));

        doAnswer(
                invocation -> {
                    mPendingCallback = invocation.getArgument(/* index= */ 1);
                    return Tasks.forResult(null);
                })
                .when(mMockLocationClient)
                .requestLocationUpdates(
                        any(), any(LocationCallback.class), any());
        doAnswer(
                invocation -> {
                    mPendingCallback = null;
                    return Tasks.forResult(null);
                })
                .when(mMockLocationClient)
                .removeLocationUpdates(any(LocationCallback.class));
        setLastLocation(null);

        mLocationProvider = new LocationProviderGmsCore(context, mMockLocationClient);
    }

    private Location createMockLocation() {
        Location location = new Location("mock");
        location.setLatitude(45.6);
        location.setLongitude(-128.4);
        return location;
    }

    private void createLocationUpdates() {
        LocationResult result = LocationResult.create(ImmutableList.of(createMockLocation()));
        if (mPendingCallback != null) {
            mPendingCallback.onLocationResult(result);
        }
    }

    private void setLastLocation(Location lastLocation) {
        when(mMockLocationClient.getLastLocation()).thenReturn(Tasks.forResult(lastLocation));
    }

    @Test
    public void testGetLocationUpdates() throws Exception {
        CountDownLatch callbackTriggered = new CountDownLatch(1);
        TrustedWebActivityLocationCallback locationCallback = (callbackName, args) -> {
            assertEquals(callbackName, EXTRA_NEW_LOCATION_AVAILABLE_CALLBACK);
            callbackTriggered.countDown();
            mLocationProvider.stop();
        };

        mLocationProvider.start(locationCallback, false);
        createLocationUpdates();

        assertTrue(callbackTriggered.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testGetsLastLocationFromProvider() throws Exception {
        CountDownLatch callbackTriggered = new CountDownLatch(1);
        TrustedWebActivityLocationCallback locationCallback = (callbackName, args) -> {
            assertEquals(callbackName, EXTRA_NEW_LOCATION_AVAILABLE_CALLBACK);
            callbackTriggered.countDown();
            mLocationProvider.stop();
        };

        setLastLocation(createMockLocation());
        mLocationProvider.start(locationCallback, false);

        assertTrue(callbackTriggered.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testMultipleUpdates() throws Exception {
        CountDownLatch callbackTriggered = new CountDownLatch(3);
        TrustedWebActivityLocationCallback locationCallback = (callbackName, args) -> {
            assertEquals(callbackName, EXTRA_NEW_LOCATION_AVAILABLE_CALLBACK);
            callbackTriggered.countDown();
        };

        setLastLocation(createMockLocation());
        mLocationProvider.start(locationCallback, false);
        createLocationUpdates();
        createLocationUpdates();

        assertTrue(mLocationProvider.isRunning());
        assertTrue(callbackTriggered.await(1, TimeUnit.SECONDS));

        mLocationProvider.stop();
        assertFalse(mLocationProvider.isRunning());
    }

    @Test
    public void testStopLocationUpdates() throws Exception {
        TrustedWebActivityLocationCallback locationCallback =
                Mockito.mock(TrustedWebActivityLocationCallback.class);

        mLocationProvider.start(locationCallback, false);
        mLocationProvider.stop();
        assertFalse(mLocationProvider.isRunning());

        createLocationUpdates();
        verify(locationCallback, never()).run(any(), any());
    }

    @Test
    public void testStopAndReturnWhenError() throws Exception {
        CountDownLatch callbackTriggered = new CountDownLatch(1);
        TrustedWebActivityLocationCallback locationCallback = (callbackName, args) -> {
            assertEquals(callbackName, EXTRA_NEW_ERROR_AVAILABLE_CALLBACK);
            callbackTriggered.countDown();
        };
        doThrow(new IllegalStateException())
                .when(mMockLocationClient).requestLocationUpdates(any(), any(LocationCallback.class),any());

        mLocationProvider.start(locationCallback, false);

        assertTrue(callbackTriggered.await(1, TimeUnit.SECONDS));
        assertFalse(mLocationProvider.isRunning());
    }
}


