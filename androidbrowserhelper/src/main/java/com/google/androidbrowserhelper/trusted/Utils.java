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

package com.google.androidbrowserhelper.trusted;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

/**
 * Utilities used by helper classes that are setting up and launching Trusted Web Activities.
 */
public class Utils {

    /** 
     * Sets status bar color. Makes the icons dark if necessary. 
     * Deprecated - use EdgeToEdgeController instead.
    */
    @Deprecated
    public static void setStatusBarColor(Activity activity, @ColorInt int color) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
        activity.getWindow().setStatusBarColor(color);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && shouldUseDarkIconsOnBackground(color)) {
            addSystemUiVisibilityFlag(activity, View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    /**
     * Sets navigation bar color. Makes the icons dark if necessary.
     * Deprecated - use EdgeToEdgeController instead.
    */
    @Deprecated
    public static void setNavigationBarColor(Activity activity, @ColorInt int color) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;

        activity.getWindow().setNavigationBarColor(color);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && shouldUseDarkIconsOnBackground(color)) {
            addSystemUiVisibilityFlag(activity, View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
    }

    private static void addSystemUiVisibilityFlag(Activity activity, int flag) {
        View root = activity.getWindow().getDecorView().getRootView();
        int visibility = root.getSystemUiVisibility();
        visibility |= flag;
        root.setSystemUiVisibility(visibility);
    }

    /**
     * Determines whether to use dark icons on a background with given color by comparing the
     * contrast ratio (https://www.w3.org/TR/WCAG20/#contrast-ratiodef) to a threshold.
     * This criterion matches the one used by Chrome:
     * https://chromium.googlesource.com/chromium/src/+/90ac05ba6cb9ab5d5df75f0cef62c950be3716c3/chrome/android/java/src/org/chromium/chrome/browser/util/ColorUtils.java#215
     */
    private static boolean shouldUseDarkIconsOnBackground(@ColorInt int backgroundColor) {
        float luminance = 0.2126f * luminanceOfColorComponent(Color.red(backgroundColor))
                + 0.7152f * luminanceOfColorComponent(Color.green(backgroundColor))
                + 0.0722f * luminanceOfColorComponent(Color.blue(backgroundColor));
        float contrast = Math.abs((1.05f) / (luminance + 0.05f));
        return contrast < 3;
    }

    private static float luminanceOfColorComponent(float c) {
        c /= 255f;
        return (c < 0.03928f) ? c / 12.92f : (float) Math.pow((c + 0.055f) / 1.055f, 2.4f);
    }

    /**
     * Converts drawable located at given resource id into a Bitmap.
     */
    @Nullable
    public static Bitmap convertDrawableToBitmap(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (drawable == null) {
            return null;
        }
        drawable = DrawableCompat.wrap(drawable);

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private static final String TAG = "TWAUtils";

    /**
     * Checks whether the given URI shares an origin with the application's configured
     * {@code defaultUrl} or any of its {@code additionalTrustedOrigins}.
     */
    public static boolean isTrustedOrigin(
            @Nullable Uri uri, @NonNull LauncherActivityMetadata metadata) {
        if (uri == null) {
            return false;
        }
        if (metadata.defaultUrl != null) {
            Uri defaultUri = Uri.parse(metadata.defaultUrl);
            if (isSameOrigin(uri, defaultUri)) {
                return true;
            }
        }
        if (metadata.additionalTrustedOrigins != null) {
            for (String originStr : metadata.additionalTrustedOrigins) {
                Uri originUri = parseConfiguredOrigin(originStr);
                if (originUri != null && isSameOrigin(uri, originUri)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Parses an ADDITIONAL_TRUSTED_ORIGINS manifest entry. Full origins ("https://example.com") are
     * returned as-is; legacy scheme-less entries ("example.com", "example.com:8443") are normalised to
     * https with a warning. Returns null if the entry cannot be read as an origin at all.
     */
    @Nullable
    static Uri parseConfiguredOrigin(@Nullable String originStr) {
        if (originStr == null) return null;
        String trimmed = originStr.trim();
        if (trimmed.isEmpty()) return null;

        Uri parsed = Uri.parse(trimmed);
        if (parsed.getScheme() != null && parsed.getHost() != null) return parsed;

        // Scheme-less legacy entry. Note Uri.parse("example.com:8443") reads "example.com" as the
        // scheme, so re-parse with an explicit https:// prefix rather than inspecting `parsed`.
        Uri assumed = Uri.parse("https://" + trimmed);
        if (assumed.getHost() == null) {
            Log.w(TAG, "Ignoring unparseable ADDITIONAL_TRUSTED_ORIGINS entry: " + originStr);
            return null;
        }
        Log.w(TAG, "ADDITIONAL_TRUSTED_ORIGINS entry '" + originStr
                + "' has no scheme; assuming https. Declare full origins (e.g. "
                + "'https://example.com') - scheme-less entries are deprecated and are not "
                + "Digital-Asset-Links verified by the browser.");
        return assumed;
    }

    /**
     * Returns whether {@code uri} is trusted to be launched by {@code component} according to the
     * application's TWA configuration.
     *
     * <p>Semantics: {@code isTrustedOrigin(uri, metadata) || (component != null &&
     * matchesOwnIntentFilter(context, component, uri))}.
     *
     * <p>Both {@link LauncherActivity} and {@link ShortcutTrampolineActivity} pass their
     * {@link ComponentName} so that app shortcuts and inbound deep links enforce a single policy,
     * allowing {@code https} hosts declared in the app's own {@code BROWSABLE} manifest
     * {@code <intent-filter>}s in addition to {@code DEFAULT_URL} and
     * {@code ADDITIONAL_TRUSTED_ORIGINS}.
     */
    public static boolean isTrustedLaunchUrl(
            @NonNull Context context,
            @Nullable ComponentName component,
            @Nullable Uri uri,
            @NonNull LauncherActivityMetadata metadata) {
        return isTrustedOrigin(uri, metadata)
                || (component != null && matchesOwnIntentFilter(context, component, uri));
    }

    /**
     * Returns whether {@code uri} matches one of the {@code <intent-filter>} elements declared by
     * {@code component} (or its activity-alias target) in the app's own manifest, i.e. whether an
     * implicit BROWSABLE Intent for this URI would have been delivered here anyway.
     */
    public static boolean matchesOwnIntentFilter(
            @NonNull Context context, @NonNull ComponentName component, @Nullable Uri uri) {
        if (uri == null) return false;
        if (!context.getPackageName().equals(component.getPackageName())) return false;
        // ShortcutTrampolineActivity is a library-declared noDisplay trampoline whose
        // host domains are declared on the app's TWA LauncherActivity.
        if (ShortcutTrampolineActivity.class.getName().equals(component.getClassName())) {
            return matchesTwaLauncherIntentFilter(context, uri);
        }
        Intent probe = new Intent(Intent.ACTION_VIEW, uri)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setPackage(context.getPackageName());
        for (ResolveInfo info : context.getPackageManager().queryIntentActivities(
                probe, PackageManager.GET_RESOLVED_FILTER)) {
            if (info.activityInfo == null) continue;
            boolean isUs = component.getClassName().equals(info.activityInfo.name)
                    // activity-alias: the filter lives on the alias, we run as the target.
                    || component.getClassName().equals(info.activityInfo.targetActivity);
            if (!isUs) continue;
            // Reject scheme-only filters: they would allow any https origin and defeat the
            // purpose of the origin check. Require the app to have named a concrete host.
            if (info.filter == null || info.filter.countDataAuthorities() == 0) continue;
            return true;
        }
        return false;
    }

    /**
     * Returns whether {@code uri} matches one of the {@code BROWSABLE} {@code <intent-filter>}
     * elements with a concrete host authority declared on any TWA launcher activity (or its
     * activity-alias target) in {@code context}'s own manifest.
     */
    public static boolean matchesTwaLauncherIntentFilter(
            @NonNull Context context, @Nullable Uri uri) {
        if (uri == null) return false;
        Intent probe = new Intent(Intent.ACTION_VIEW, uri)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setPackage(context.getPackageName());
        for (ResolveInfo info : context.getPackageManager().queryIntentActivities(
                probe, PackageManager.GET_RESOLVED_FILTER)) {
            if (info.activityInfo == null) continue;
            if (!isTwaLauncherActivity(context, info.activityInfo)) continue;
            // Reject scheme-only filters: they would allow any https origin and defeat the
            // purpose of the origin check. Require the app to have named a concrete host.
            if (info.filter == null || info.filter.countDataAuthorities() == 0) continue;
            return true;
        }
        return false;
    }

    private static boolean isTwaLauncherActivity(
            @NonNull Context context, @NonNull ActivityInfo activityInfo) {
        if (LauncherActivity.class.getName().equals(activityInfo.name)
                || LauncherActivity.class.getName().equals(activityInfo.targetActivity)) {
            return true;
        }
        try {
            String targetName = activityInfo.targetActivity != null
                    ? activityInfo.targetActivity : activityInfo.name;
            ActivityInfo fullInfo =
                    context.getPackageManager().getActivityInfo(
                            new ComponentName(context.getPackageName(), targetName),
                            PackageManager.GET_META_DATA);
            return fullInfo.metaData != null
                    && fullInfo.metaData.containsKey(
                            "android.support.customtabs.trusted.DEFAULT_URL");
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Compares two URIs for same-origin equality (scheme, host, and normalized port).
     */
    public static boolean isSameOrigin(@Nullable Uri uri1, @Nullable Uri uri2) {
        if (uri1 == null || uri2 == null) {
            return false;
        }
        String scheme1 = uri1.getScheme();
        String scheme2 = uri2.getScheme();
        String host1 = uri1.getHost();
        String host2 = uri2.getHost();
        if (scheme1 == null || scheme2 == null || host1 == null || host2 == null) {
            return false;
        }

        int port1 = uri1.getPort();
        int port2 = uri2.getPort();
        if (port1 == -1) {
            port1 = "https".equalsIgnoreCase(scheme1)
                    ? 443 : ("http".equalsIgnoreCase(scheme1) ? 80 : -1);
        }
        if (port2 == -1) {
            port2 = "https".equalsIgnoreCase(scheme2)
                    ? 443 : ("http".equalsIgnoreCase(scheme2) ? 80 : -1);
        }

        return scheme1.equalsIgnoreCase(scheme2)
                && host1.equalsIgnoreCase(host2)
                && port1 == port2;
    }
}