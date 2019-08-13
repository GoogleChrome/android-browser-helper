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

package com.google.androidbrowserhelper.trusted.splashscreens;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;

import com.google.androidbrowserhelper.trusted.ChromeLegacyUtils;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsService;
import androidx.browser.customtabs.TrustedWebUtils;
import androidx.browser.trusted.TrustedWebActivityIntentBuilder;

/**
 * Predicts system status bar and navigation bar colors that are about to be shown in a Trusted Web
 * Activity based on an instance of {@link TrustedWebActivityIntentBuilder}.
 */
class SystemBarColorPredictor {
    private Map<String, SupportedFeatures> mSupportedFeaturesCache = new HashMap<>();

    SystemBarColorPredictor() {}

    /**
     * Makes a best-effort guess about which status bar color will be used when the Trusted Web
     * Activity is launched. Returns null if not possible to predict.
     */
    @Nullable
    Integer getExpectedStatusBarColor(Context context, String providerPackage,
            TrustedWebActivityIntentBuilder builder) {
        Intent intent = builder.buildCustomTabsIntent().intent;
        if (providerSupportsColorSchemeParams(context, providerPackage)) {
            int colorScheme = getExpectedColorScheme(context, builder);
            CustomTabColorSchemeParams params = CustomTabsIntent.getColorSchemeParams(intent,
                    colorScheme);
            return params.toolbarColor;
        }
        Bundle extras = intent.getExtras();
        return extras == null ? null : (Integer) extras.get(CustomTabsIntent.EXTRA_TOOLBAR_COLOR);
    }

    /**
     * Makes a best-effort guess about which navigation bar color will be used when the Trusted Web
     * Activity is launched. Returns null if not possible to predict.
     */
    @Nullable
    Integer getExpectedNavbarColor(Context context, String providerPackage,
            TrustedWebActivityIntentBuilder builder) {
        Intent intent = builder.buildCustomTabsIntent().intent;
        if (providerSupportsNavBarColorCustomization(context, providerPackage)) {
            if (providerSupportsColorSchemeParams(context, providerPackage)) {
                int colorScheme = getExpectedColorScheme(context, builder);
                CustomTabColorSchemeParams params = CustomTabsIntent.getColorSchemeParams(intent,
                        colorScheme);
                return params.navigationBarColor;
            }
            Bundle extras = intent.getExtras();
            return extras == null ? null :
                    (Integer) extras.get(CustomTabsIntent.EXTRA_NAVIGATION_BAR_COLOR);
        }
        if (ChromeLegacyUtils.usesWhiteNavbar(providerPackage)) {
            return Color.WHITE;
        }
        return null;
    }

    private boolean providerSupportsNavBarColorCustomization(Context context,
            String providerPackage) {
        return getSupportedFeatures(context, providerPackage).navbarColorCustomization;
    }

    private boolean providerSupportsColorSchemeParams(Context context, String providerPackage) {
        return getSupportedFeatures(context, providerPackage).colorSchemeCustomization;
    }

    private SupportedFeatures getSupportedFeatures(Context context,
            String providerPackage) {
        SupportedFeatures cached = mSupportedFeaturesCache.get(providerPackage);
        if (cached != null) return cached;

        if (ChromeLegacyUtils.supportsNavbarAndColorCustomization(
                context.getPackageManager(), providerPackage)) {
            // Chrome 76 supports both features, but doesn't advertise it with categories.
            SupportedFeatures features = new SupportedFeatures(true, true);
            mSupportedFeaturesCache.put(providerPackage, features);
            return features;
        }

        Intent serviceIntent = new Intent()
                .setAction(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION)
                .setPackage(providerPackage);
        ResolveInfo resolveInfo = context.getPackageManager().resolveService(serviceIntent,
                PackageManager.GET_RESOLVED_FILTER);

        SupportedFeatures features = new SupportedFeatures(
                hasCategory(resolveInfo, CustomTabsService.CATEGORY_NAVBAR_COLOR_CUSTOMIZATION),
                hasCategory(resolveInfo, CustomTabsService.CATEGORY_COLOR_SCHEME_CUSTOMIZATION)
        );
        mSupportedFeaturesCache.put(providerPackage, features);
        return features;
    }

    private static boolean hasCategory(ResolveInfo info, String category) {
        return info != null && info.filter != null && info.filter.hasCategory(category);
    }

    private static int getExpectedColorScheme(Context context, TrustedWebActivityIntentBuilder builder) {
        Intent intent = builder.buildCustomTabsIntent().intent;
        Bundle extras = intent.getExtras();
        Integer scheme = extras == null ? null :
                (Integer) extras.get(CustomTabsIntent.EXTRA_COLOR_SCHEME);
        if (scheme != null && scheme != CustomTabsIntent.COLOR_SCHEME_SYSTEM) {
            return scheme;
        }
        boolean systemIsInDarkMode = (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        return systemIsInDarkMode ? CustomTabsIntent.COLOR_SCHEME_DARK :
                CustomTabsIntent.COLOR_SCHEME_LIGHT;
    }

    // This will be part of feature detection API soon.
    private static class SupportedFeatures {
        public final boolean navbarColorCustomization;
        public final boolean colorSchemeCustomization;

        private SupportedFeatures(boolean navbarColorCustomization,
                boolean colorSchemeCustomization) {
            this.navbarColorCustomization = navbarColorCustomization;
            this.colorSchemeCustomization = colorSchemeCustomization;
        }
    }
}