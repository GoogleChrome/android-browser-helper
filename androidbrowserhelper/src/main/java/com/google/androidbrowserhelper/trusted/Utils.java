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
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.util.Log;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Utilities used by helper classes that are setting up and launching Trusted Web Activities.
 */
public class Utils {
    private static final String TAG = "TWAUtils";

    static final String METADATA_DEFAULT_URL =
            "android.support.customtabs.trusted.DEFAULT_URL";

    private static final Set<String> sWarnedSchemelessOrigins =
            Collections.synchronizedSet(new HashSet<>());

    @VisibleForTesting
    static void resetWarnedOriginsForTesting() {
        sWarnedSchemelessOrigins.clear();
    }

    /** Full URI in debuggable builds; only scheme://host[:port] otherwise, since URIs can carry tokens. */
    static String uriForLog(@NonNull Context context, @Nullable Uri uri) {
        if (uri == null) return "null";
        if ((context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            return uri.toString();
        }
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null) return "<redacted>";
        int port = uri.getPort();
        return scheme + "://" + host + (port == -1 ? "" : ":" + port) + "/...";
    }

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

    /**
     * Checks whether the given URI is an {@code https} URI that shares an origin with the
     * application's configured {@code defaultUrl} or any of its {@code additionalTrustedOrigins}.
     */
    static boolean isTrustedOrigin(
            @Nullable Uri uri, @NonNull LauncherActivityMetadata metadata) {
        if (uri == null || !"https".equalsIgnoreCase(uri.getScheme())) {
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
        if (sWarnedSchemelessOrigins.add(trimmed)) {
            Log.w(TAG, "ADDITIONAL_TRUSTED_ORIGINS entry '" + originStr
                    + "' has no scheme; assuming https. Declare full origins (e.g. "
                    + "'https://example.com') - scheme-less entries are deprecated and are not "
                    + "Digital-Asset-Links verified by the browser.");
        }
        return assumed;
    }

    @FunctionalInterface
    private interface ActivityMatcher { boolean matches(ActivityInfo info); }

    private static boolean matchesBrowsableFilter(
            Context context, @Nullable Uri uri, ActivityMatcher matcher) {
        if (uri == null) {
            return false;
        }
        // IntentFilter/PackageManager compare hosts with compareToIgnoreCase(), which folds e.g.
        // U+0131 to 'i'; browsers apply IDNA instead. Only ASCII hosts are compared.
        if (!isAscii(uri.getScheme()) || !isAscii(uri.getHost())) {
            return false;
        }
        Uri normalized = uri.normalizeScheme(); // IntentFilter scheme matching is case-sensitive
        Intent probe = new Intent(Intent.ACTION_VIEW, normalized)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setPackage(context.getPackageName());
        for (ResolveInfo info : context.getPackageManager().queryIntentActivities(
                probe, PackageManager.GET_RESOLVED_FILTER | PackageManager.GET_META_DATA)) {
            if (info.activityInfo == null || !matcher.matches(info.activityInfo)) continue;
            if (hasConcreteMatchingAuthority(info.filter, normalized)) return true;
        }
        return false;
    }

    // A filter must name a concrete host that matches the URI. The wildcard host "*" matches every
    // origin, so it never counts. Subdomain wildcards such as "*.example.com" are still accepted.
    private static boolean hasConcreteMatchingAuthority(@Nullable IntentFilter filter, Uri uri) {
        if (filter == null) return false;
        Iterator<IntentFilter.AuthorityEntry> it = filter.authoritiesIterator();
        if (it == null) return false;
        while (it.hasNext()) {
            IntentFilter.AuthorityEntry entry = it.next();
            if (!"*".equals(entry.getHost()) && entry.match(uri) >= 0) return true;
        }
        return false;
    }

    /**
     * Returns whether {@code uri} matches one of the {@code <intent-filter>} elements declared by
     * {@code component} (or its activity-alias target) in the app's own manifest, i.e. whether an
     * implicit BROWSABLE Intent for this URI would have been delivered here anyway. Called by
     * {@link LauncherActivity}.
     */
    static boolean matchesOwnIntentFilter(
            @NonNull Context context, @NonNull ComponentName component, @Nullable Uri uri) {
        if (!context.getPackageName().equals(component.getPackageName())) return false;
        return matchesBrowsableFilter(context, uri, info ->
                component.getClassName().equals(info.name)
                        // activity-alias: the filter lives on the alias, we run as the target.
                        || component.getClassName().equals(info.targetActivity));
    }

    /**
     * Returns whether {@code uri} matches one of the {@code BROWSABLE} {@code <intent-filter>}
     * elements with a concrete host authority declared on any TWA launcher activity (or its
     * activity-alias target) in {@code context}'s own manifest. Called by
     * {@link ShortcutTrampolineActivity}.
     */
    static boolean matchesTwaLauncherIntentFilter(
            @NonNull Context context, @Nullable Uri uri) {
        return matchesBrowsableFilter(context, uri, info -> isTwaLauncherActivity(context, info));
    }

    private static boolean isTwaLauncherActivity(
            @NonNull Context context, @NonNull ActivityInfo info) {
        if (LauncherActivity.class.getName().equals(info.name)
                || LauncherActivity.class.getName().equals(info.targetActivity)) {
            return true;
        }
        if (info.metaData != null && info.metaData.containsKey(METADATA_DEFAULT_URL)) {
            return true;
        }
        if (info.targetActivity == null) {
            return false;
        }
        try {
            ActivityInfo targetInfo =
                    context.getPackageManager().getActivityInfo(
                            new ComponentName(context.getPackageName(), info.targetActivity),
                            PackageManager.GET_META_DATA);
            return targetInfo.metaData != null
                    && targetInfo.metaData.containsKey(METADATA_DEFAULT_URL);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /** Whether {@code s} is non-null and contains only ASCII characters. */
    private static boolean isAscii(@Nullable String s) {
        if (s == null) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) > 0x7F) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compares two URIs for same-origin equality (scheme, host, and normalized port). Scheme and
     * host are compared ASCII-case-insensitively; non-ASCII schemes or hosts never match.
     */
    static boolean isSameOrigin(@Nullable Uri uri1, @Nullable Uri uri2) {
        if (uri1 == null || uri2 == null) {
            return false;
        }
        String scheme1 = uri1.getScheme();
        String scheme2 = uri2.getScheme();
        String host1 = uri1.getHost();
        String host2 = uri2.getHost();
        if (!isAscii(scheme1) || !isAscii(scheme2) || !isAscii(host1) || !isAscii(host2)) {
            return false;
        }

        String s1 = scheme1.toLowerCase(Locale.ROOT);
        String s2 = scheme2.toLowerCase(Locale.ROOT);
        int port1 = uri1.getPort();
        int port2 = uri2.getPort();
        if (port1 == -1) {
            port1 = "https".equals(s1) ? 443 : ("http".equals(s1) ? 80 : -1);
        }
        if (port2 == -1) {
            port2 = "https".equals(s2) ? 443 : ("http".equals(s2) ? 80 : -1);
        }

        return s1.equals(s2)
                && host1.toLowerCase(Locale.ROOT).equals(host2.toLowerCase(Locale.ROOT))
                && port1 == port2;
    }

    private static volatile OwnAuthoritiesCache sOwnAuthoritiesCache;

    private static final class OwnAuthoritiesCache {
        final String packageName;
        final Set<String> authorities;

        OwnAuthoritiesCache(
                @Nullable String packageName,
                Set<String> authorities) {
            this.packageName = packageName;
            this.authorities = authorities;
        }
    }

    @VisibleForTesting
    static void resetOwnProviderAuthoritiesCacheForTesting() {
        sOwnAuthoritiesCache = null;
    }

    private static Set<String> getOwnProviderAuthorities(@NonNull Context context) {
        String hostPackage = context.getPackageName();
        OwnAuthoritiesCache cached = sOwnAuthoritiesCache;
        if (cached != null && Objects.equals(cached.packageName, hostPackage)) {
            return cached.authorities;
        }

        Context appContext = context.getApplicationContext();
        PackageManager pm = (appContext != null ? appContext : context).getPackageManager();

        Set<String> authorities = new HashSet<>();
        String[] packages = pm.getPackagesForUid(Process.myUid());
        if (packages == null || packages.length == 0) {
            packages = hostPackage != null ? new String[]{hostPackage} : new String[0];
        } else if (hostPackage != null) {
            boolean foundHost = false;
            for (String pkg : packages) {
                if (hostPackage.equals(pkg)) {
                    foundHost = true;
                    break;
                }
            }
            if (!foundHost) {
                String[] expanded = Arrays.copyOf(packages, packages.length + 1);
                expanded[packages.length] = hostPackage;
                packages = expanded;
            }
        }

        for (String pkg : packages) {
            if (pkg == null) {
                continue;
            }
            try {
                PackageInfo pkgInfo =
                        pm.getPackageInfo(pkg, PackageManager.GET_PROVIDERS);
                if (pkgInfo != null && pkgInfo.providers != null) {
                    for (ProviderInfo provider : pkgInfo.providers) {
                        if (provider == null || provider.authority == null) {
                            continue;
                        }
                        for (String part : provider.authority.split(";")) {
                            String trimmed = part.trim();
                            if (!trimmed.isEmpty()) {
                                authorities.add(trimmed.toLowerCase(Locale.ROOT));
                            }
                        }
                    }
                }
            } catch (PackageManager.NameNotFoundException ignored) {
                // Ignore packages that cannot be queried.
            }
        }

        Set<String> unmodifiable = Collections.unmodifiableSet(authorities);
        sOwnAuthoritiesCache = new OwnAuthoritiesCache(hostPackage, unmodifiable);
        return unmodifiable;
    }

    /**
     * Returns {@code authority} without Android's cross-user {@code "<userId>@"} prefix (added by
     * Intent#fixUris for cross-profile launches), or null if the prefix is malformed. Splits on the
     * last '@', matching ContentProvider#getAuthorityWithoutUserId.
     */
    @VisibleForTesting
    @Nullable
    static String stripContentUserId(@NonNull String authority) {
        int at = authority.lastIndexOf('@');
        if (at == -1) {
            return authority;
        }
        String userId = authority.substring(0, at);
        String bare = authority.substring(at + 1);
        if (userId.isEmpty() || bare.isEmpty()) {
            return null;
        }
        for (int i = 0; i < userId.length(); i++) {
            char c = userId.charAt(i);
            if (c < '0' || c > '9') {
                return null;
            }
        }
        try {
            Integer.parseInt(userId);
        } catch (NumberFormatException e) {
            return null;
        }
        return bare;
    }

    /**
     * Verifies that {@code uri} is a {@code content://} URI backed by an external ContentProvider
     * (not owned by {@code context}'s package or UID, preventing confused-deputy re-grants of
     * internal FileProviders, including cross-profile {@code content://<userId>@<authority>/...}
     * URIs) and that this process holds a valid URI grant for {@code modeFlags}.
     */
    static boolean isSafeExternalContentUri(
            @NonNull Context context, @Nullable Uri uri, int modeFlags) {
        if (uri == null) {
            return false;
        }
        Uri canonicalUri = Uri.parse(uri.toString());
        if (!"content".equalsIgnoreCase(canonicalUri.getScheme())) {
            return false;
        }

        String authority = canonicalUri.getAuthority();
        if (authority == null) {
            return false;
        }
        String bareAuthority = stripContentUserId(authority);
        if (bareAuthority == null || bareAuthority.indexOf(':') != -1) {
            return false;
        }

        if (getOwnProviderAuthorities(context).contains(
                bareAuthority.toLowerCase(Locale.ROOT))) {
            return false;
        }

        // Best-effort extra rejection when visible; never fail closed on null because Android 11+
        // package visibility filtering causes resolveContentProvider() to return null for external
        // senders not declared in <queries>.
        ProviderInfo providerInfo =
                context.getPackageManager().resolveContentProvider(bareAuthority, 0);
        if (providerInfo != null) {
            if (context.getPackageName().equals(providerInfo.packageName)) {
                return false;
            }
            if (providerInfo.applicationInfo != null
                    && providerInfo.applicationInfo.uid == Process.myUid()) {
                return false;
            }
        }

        // Pass the full URI (including any userId@ prefix) because ContextImpl resolves the user id
        // from it.
        return context.checkUriPermission(
                canonicalUri, Process.myPid(), Process.myUid(), modeFlags)
                == PackageManager.PERMISSION_GRANTED;
    }
}
