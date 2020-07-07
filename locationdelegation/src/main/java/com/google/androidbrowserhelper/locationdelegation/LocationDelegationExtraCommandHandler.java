package com.google.androidbrowserhelper.locationdelegation;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.trusted.TrustedWebActivityCallbackRemote;

import com.google.androidbrowserhelper.trusted.ExtraCommandHandler;

public class LocationDelegationExtraCommandHandler implements ExtraCommandHandler {
    static final String CHECK_LOCATION_PERMISSION_COMMAND_NAME = "checkAndroidLocationPermission";
    private static final String START_LOCATION_COMMAND_NAME = "startLocation";
    private static final String STOP_LOCATION_COMMAND_NAME = "stopLocation";

    private boolean mIsProviderGmsCore;

    public Bundle handleExtraCommand(Context context, String commandName, Bundle args,
                                     @Nullable TrustedWebActivityCallbackRemote callback) {
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
                startLocationProvider(context, callback, args.getBoolean("enableHighAccuracy"));

                result.putBoolean(EXTRA_COMMAND_SUCCESS, true);
                break;
            case STOP_LOCATION_COMMAND_NAME:
                stopLocationProvider(context);

                result.putBoolean(EXTRA_COMMAND_SUCCESS, true);
                break;
        }
        return result;
    }

    private void requestPermission(Context context, @NonNull TrustedWebActivityCallbackRemote callback) {
        PermissionRequestActivity.requestLocationPermission(context, callback);
    }


    private void startLocationProvider(Context context,
                                       @NonNull TrustedWebActivityCallbackRemote locationChangeCallback,
            boolean enableHighAccuracy) {
        mIsProviderGmsCore = LocationProviderGmsCore.isGooglePlayServicesAvailable(context);
        if (mIsProviderGmsCore) {
            LocationProviderGmsCore.getInstance(context).start(
                    locationChangeCallback, enableHighAccuracy);
        } else {
            LocationProviderAndroid.getInstance().start(
                    context, locationChangeCallback, enableHighAccuracy);
        }
    }

    private void stopLocationProvider(Context context) {
        if (mIsProviderGmsCore) {
            LocationProviderGmsCore.getInstance(context).stop();
        } else {
            LocationProviderAndroid.getInstance().stop();
        }
    }
}
