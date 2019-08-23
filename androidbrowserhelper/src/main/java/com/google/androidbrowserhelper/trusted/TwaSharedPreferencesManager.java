package com.google.androidbrowserhelper.trusted;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

/**
 * Manages shared preferences for {@link LauncherActivity} and related infrastructure.
 */
public class TwaSharedPreferencesManager {
    private static final String PREFS_NAME = "TrustedWebActivityLauncherPrefs";
    private static final String KEY_PROVIDER_PACKAGE = "KEY_PROVIDER_PACKAGE";
    private final SharedPreferences mSharedPreferences;

    public TwaSharedPreferencesManager(Context context) {
        mSharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Writes the package name of the provider in which a TWA was launched the last time.
     */
    public void writeLastLaunchedProviderPackageName(String packageName) {
        mSharedPreferences.edit().putString(KEY_PROVIDER_PACKAGE, packageName).apply();
    }

    /**
     * Reads the package name of the provider in which a TWA was launched the last time.
     */
    @Nullable
    public String readLastLaunchedProviderPackageName() {
        return mSharedPreferences.getString(KEY_PROVIDER_PACKAGE, null);
    }
}
