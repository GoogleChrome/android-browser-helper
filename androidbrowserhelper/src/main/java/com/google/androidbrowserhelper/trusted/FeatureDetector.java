package com.google.androidbrowserhelper.trusted;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.HashMap;
import java.util.Map;

import androidx.browser.customtabs.CustomTabsService;

/**
 * Finds which features the given provider supports by querying PackageManager.
 * To be replaced with an AndroidX API at some point.
 */
public class FeatureDetector {
    private Map<String, SupportedFeatures> mSupportedFeaturesCache = new HashMap<>();

    public boolean providerSupportsNavBarColorCustomization(Context context,
            String providerPackage) {
        return getSupportedFeatures(context, providerPackage).navbarColorCustomization;
    }

    public boolean providerSupportsColorSchemeParams(Context context, String providerPackage) {
        return getSupportedFeatures(context, providerPackage).colorSchemeCustomization;
    }

    public boolean providerSupportsImmersiveMode(Context context, String providerPackage) {
        return getSupportedFeatures(context, providerPackage).immersiveMode;
    }

    private SupportedFeatures getSupportedFeatures(Context context,
            String providerPackage) {
        SupportedFeatures cached = mSupportedFeaturesCache.get(providerPackage);
        if (cached != null) return cached;

        if (ChromeLegacyUtils.isChrome76(context.getPackageManager(), providerPackage)) {
            // Chrome 76 has a mismatch between features actually supported and declared.
            SupportedFeatures features = new SupportedFeatures(true, true, true, false);
            mSupportedFeaturesCache.put(providerPackage, features);
            return features;
        }

        Intent serviceIntent = new Intent()
                .setAction(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION)
                .setPackage(providerPackage);
        ResolveInfo resolveInfo = context.getPackageManager().resolveService(serviceIntent,
                PackageManager.GET_RESOLVED_FILTER);

        // Feature detection categories were introduced after TWAs: Chrome 72-74 supports TWAs
        // without declaring it.
        boolean supportsTrustedWebActivities =
                ChromeLegacyUtils.supportsTrustedWebActivities(context.getPackageManager(),
                        providerPackage)
                || hasCategory(resolveInfo, CustomTabsService.TRUSTED_WEB_ACTIVITY_CATEGORY);
        SupportedFeatures features = new SupportedFeatures(
                supportsTrustedWebActivities,
                hasCategory(resolveInfo, CustomTabsService.CATEGORY_NAVBAR_COLOR_CUSTOMIZATION),
                hasCategory(resolveInfo, CustomTabsService.CATEGORY_COLOR_SCHEME_CUSTOMIZATION),
                hasCategory(resolveInfo,
                        CustomTabsService.CATEGORY_TRUSTED_WEB_ACTIVITY_IMMERSIVE_MODE));
        mSupportedFeaturesCache.put(providerPackage, features);
        return features;
    }

    private static boolean hasCategory(ResolveInfo info, String category) {
        return info != null && info.filter != null && info.filter.hasCategory(category);
    }

    private static class SupportedFeatures {
        public final boolean trustedWebActivities;
        public final boolean navbarColorCustomization;
        public final boolean colorSchemeCustomization;
        public final boolean immersiveMode;

        private SupportedFeatures(boolean trustedWebActivities,
                boolean navbarColorCustomization,
                boolean colorSchemeCustomization,
                boolean immersiveMode) {
            this.trustedWebActivities = trustedWebActivities;
            this.navbarColorCustomization = navbarColorCustomization;
            this.colorSchemeCustomization = colorSchemeCustomization;
            this.immersiveMode = immersiveMode;
        }
    }
}
