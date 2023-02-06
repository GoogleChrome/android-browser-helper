// Copyright 2018 Google Inc. All Rights Reserved.
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
import android.content.Intent;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.trusted.TrustedWebActivityDisplayMode;
import androidx.browser.trusted.TrustedWebActivityIntentBuilder;
import androidx.browser.trusted.TrustedWebActivityService;
import androidx.browser.trusted.sharing.ShareData;
import androidx.browser.trusted.sharing.ShareTarget;
import androidx.core.content.ContextCompat;

import com.google.androidbrowserhelper.trusted.splashscreens.PwaWrapperSplashScreenStrategy;

import org.json.JSONException;

/**
 * A convenience class to make using Trusted Web Activities easier. You can extend this class for
 * basic modifications to the behaviour.
 *
 * If you just want to wrap a website in a Trusted Web Activity you should:
 * 1) Copy the manifest for the svgomg project.
 * 2) Set up Digital Asset Links [1] for your site and app.
 * 3) Set the DEFAULT_URL metadata in the manifest and the browsable intent filter to point to your
 *    website.
 *
 * You can skip (2) if you just want to try out TWAs but not on your own website, but you must
 * add the {@code --disable-digital-asset-link-verification-for-url=https://svgomg.firebaseapp.com}
 * to Chrome for this to work [2].
 *
 * You may also go beyond this and add notification delegation, which causes push notifications to
 * be shown by your app instead of Chrome. This is detailed in the javadoc for
 * {@link TrustedWebActivityService}.
 *
 * If you just want default behaviour your Trusted Web Activity client app doesn't even need any
 * Java code - you just set everything up in the Android Manifest!
 *
 * This activity also supports showing a splash screen while the Trusted Web Activity provider is
 * warming up and is loading the page in Trusted Web Activity. This is supported in Chrome 75+.
 *
 * Splash screens support in Chrome is based on transferring the splash screen via FileProvider [3].
 * To set up splash screens, you need to:
 * 1) Set up a FileProvider in the Manifest as described in [3]. The file provider paths should be
 * as follows: <paths><files-path path="twa_splash/" name="twa_splash"/></paths>
 * 2) Provide splash-screen related metadata (see descriptions in {@link LauncherActivityMetadata}),
 * including the authority of your FileProvider.
 *
 * Splash screen is first shown here in LauncherActivity, then seamlessly moved onto the browser.
 * Showing splash screen in the app first is optional, but highly recommended, because on slow
 * devices (e.g. Android Go) it can take seconds to boot up a browser.
 *
 * Recommended theme for this Activity is:
 * <pre>{@code
 * <style name="LauncherActivityTheme" parent="Theme.AppCompat.NoActionBar">
 *     <item name="android:windowIsTranslucent">true</item>
 *     <item name="android:windowBackground">@android:color/transparent</item>
 *     <item name="android:statusBarColor">@android:color/transparent</item>
 *     <item name="android:navigationBarColor">@android:color/transparent</item>
 *     <item name="android:backgroundDimEnabled">false</item>
 * </style>
 * }</pre>
 *
 * Note that even with splash screen enabled, it is still recommended to use a transparent theme.
 * That way the Activity can gracefully fall back to being a transparent "trampoline" activity in
 * the following cases:
 * - Splash screens are not supported by the picked browser.
 * - The TWA is already running, and LauncherActivity merely needs to deliver a new Intent to it.
 *
 * [1] https://developers.google.com/digital-asset-links/v1/getting-started
 * [2] https://www.chromium.org/developers/how-tos/run-chromium-with-flags#TOC-Setting-Flags-for-Chrome-on-Android
 * [3] https://developer.android.com/reference/android/support/v4/content/FileProvider
 */
public class LauncherActivity extends Activity {
    private static final String TAG = "TWALauncherActivity";

    private static final String BROWSER_WAS_LAUNCHED_KEY =
            "android.support.customtabs.trusted.BROWSER_WAS_LAUNCHED_KEY";

    private static final String FALLBACK_TYPE_WEBVIEW = "webview";

    /** We only want to show the update prompt once per instance of this application. */
    private static boolean sChromeVersionChecked;

    /** See comment in onCreate. */
    private static int sLauncherActivitiesAlive;

    private LauncherActivityMetadata mMetadata;

    private boolean mBrowserWasLaunched;

    @Nullable
    private PwaWrapperSplashScreenStrategy mSplashScreenStrategy;

    @Nullable
    private TwaLauncher mTwaLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sLauncherActivitiesAlive++;
        boolean twaAlreadyRunning = sLauncherActivitiesAlive > 1;
        boolean intentHasData = getIntent().getData() != null;
        boolean intentHasShareData = SharingUtils.isShareIntent(getIntent());
        if (twaAlreadyRunning && !intentHasData && !intentHasShareData) {
            // If there's another LauncherActivity alive, that means that the TWA is already
            // running. If we attempt to launch it again, we will trigger a browser navigation. For
            // the case where an Intent comes in from a BROWSABLE Intent, a notification or with
            // share data, that is the desired behaviour.

            // However, if the TWA was originally started by a BROWSABLE Intent and the user then
            // clicks on the Launcher icon, Android launches this Activity anew (instead of just
            // bringing the Task to the foreground). In this case we don't want to launch the TWA
            // again and trigger the navigation. Since launching this Activity will have brought the
            // TWA to the foreground, we can just finish and everything will work fine.
            finish();
            return;
        }

        if (restartInNewTask()) {
            finish();
            return;
        }

        if (savedInstanceState != null && savedInstanceState.getBoolean(BROWSER_WAS_LAUNCHED_KEY)) {
            // This activity died in the background after launching Trusted Web Activity, then
            // the user closed the Trusted Web Activity and ended up here.
            finish();
            return;
        }

        mMetadata = LauncherActivityMetadata.parse(this);

        if (splashScreenNeeded()) {
            mSplashScreenStrategy = new PwaWrapperSplashScreenStrategy(this,
                    mMetadata.splashImageDrawableId,
                    getColorCompat(mMetadata.splashScreenBackgroundColorId),
                    getSplashImageScaleType(),
                    getSplashImageTransformationMatrix(),
                    mMetadata.splashScreenFadeOutDurationMillis,
                    mMetadata.fileProviderAuthority);
        }

        if (shouldLaunchImmediately()) {
            launchTwa();
        }
    }

    /**
     * Signals if {@link LauncherActivity} should automatically launch the Trusted Web Activity on
     * {@linke #onCreate()}. Return {@code false} when a subclass needs to perform an asynchronous
     * task before launching the Trusted Web Activity. The subclass will then be responsible for
     * calling {@link #launchTwa()} itself once the asynchronous task is finished.
     */
    protected boolean shouldLaunchImmediately() {
        return true;
    }

    /**
     * Launches the Trusted Web Activity. This methods should only be called when
     * {@link #shouldLaunchImmediately()} returns {@code false}.
     */
    protected void launchTwa() {
        // When launching asynchronously, developers should check if the Activity is finishing
        // before calling launchTwa(). We double check the condition here and prevent the launch
        // if that's the case.
        if (isFinishing()) {
            Log.d(TAG, "Aborting launchTwa() as Activity is finishing");
            return;
        }

        CustomTabColorSchemeParams darkModeColorScheme = new CustomTabColorSchemeParams.Builder()
                .setToolbarColor(getColorCompat(mMetadata.statusBarColorDarkId))
                .setNavigationBarColor(getColorCompat(mMetadata.navigationBarColorDarkId))
                .setNavigationBarDividerColor(
                        getColorCompat(mMetadata.navigationBarDividerColorDarkId))
                .build();

        TrustedWebActivityIntentBuilder twaBuilder =
                new TrustedWebActivityIntentBuilder(getLaunchingUrl())
                        .setToolbarColor(getColorCompat(mMetadata.statusBarColorId))
                        .setNavigationBarColor(getColorCompat(mMetadata.navigationBarColorId))
                        .setNavigationBarDividerColor(
                                getColorCompat(mMetadata.navigationBarDividerColorId))
                        .setColorScheme(CustomTabsIntent.COLOR_SCHEME_SYSTEM)
                        .setColorSchemeParams(
                                CustomTabsIntent.COLOR_SCHEME_DARK, darkModeColorScheme)
                        .setDisplayMode(getDisplayMode())
                        .setScreenOrientation(mMetadata.screenOrientation);

        if (mMetadata.additionalTrustedOrigins != null) {
            twaBuilder.setAdditionalTrustedOrigins(mMetadata.additionalTrustedOrigins);
        }

        addShareDataIfPresent(twaBuilder);

        mTwaLauncher = createTwaLauncher();
        mTwaLauncher.launch(twaBuilder,
                getCustomTabsCallback(),
                mSplashScreenStrategy,
                () -> mBrowserWasLaunched = true,
                getFallbackStrategy());

        if (!sChromeVersionChecked) {
            ChromeUpdatePrompt.promptIfNeeded(this, mTwaLauncher.getProviderPackage());
            sChromeVersionChecked = true;
        }

        if (ChromeOsSupport.isRunningOnArc(getApplicationContext().getPackageManager())) {
            new TwaSharedPreferencesManager(this)
                    .writeLastLaunchedProviderPackageName(ChromeOsSupport.ARC_PAYMENT_APP);
        } else {
            new TwaSharedPreferencesManager(this)
                    .writeLastLaunchedProviderPackageName(mTwaLauncher.getProviderPackage());
        }

        ManageDataLauncherActivity.addSiteSettingsShortcut(this,
                mTwaLauncher.getProviderPackage());
    }

    protected CustomTabsCallback getCustomTabsCallback() {
        return new QualityEnforcer();
    }

    protected TwaLauncher createTwaLauncher() {
        return new TwaLauncher(this);
    }

    private boolean splashScreenNeeded() {
        // Splash screen was not requested.
        if (mMetadata.splashImageDrawableId == 0) return false;

        // If this activity isn't task root, then a TWA is already running in this task. This can
        // happen if a VIEW intent (without Intent.FLAG_ACTIVITY_NEW_TASK) is being handled after
        // launching a TWA. In that case we're only passing a new intent into existing TWA, and
        // don't show the splash screen.
        return isTaskRoot();
    }

    private void addShareDataIfPresent(TrustedWebActivityIntentBuilder twaBuilder) {
        ShareData shareData = SharingUtils.retrieveShareDataFromIntent(getIntent());
        if (shareData == null) {
            return;
        }
        if (mMetadata.shareTarget == null) {
            Log.d(TAG, "Failed to share: share target not defined in the AndroidManifest");
            return;
        }
        try {
            ShareTarget shareTarget = SharingUtils.parseShareTargetJson(mMetadata.shareTarget);
            twaBuilder.setShareParams(shareTarget, shareData);
        } catch (JSONException e) {
            Log.d(TAG, "Failed to parse share target json: " + e.toString());
        }
    }

    /**
     * Override to set a custom scale type for the image displayed on a splash screen.
     * See {@link ImageView.ScaleType}.
     */
    @NonNull
    protected ImageView.ScaleType getSplashImageScaleType() {
        return ImageView.ScaleType.CENTER;
    }

    /**
     * Override to set a transformation matrix for the image displayed on a splash screen.
     * See {@link ImageView#setImageMatrix}.
     * Has any effect only if {@link #getSplashImageScaleType()} returns {@link
     * ImageView.ScaleType#MATRIX}.
     */
    @Nullable
    protected Matrix getSplashImageTransformationMatrix() {
        return null;
    }

    private int getColorCompat(int splashScreenBackgroundColorId) {
        return ContextCompat.getColor(this, splashScreenBackgroundColorId);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (mBrowserWasLaunched) {
            finish(); // The user closed the Trusted Web Activity and ended up here.
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        sLauncherActivitiesAlive--;

        if (mTwaLauncher != null) {
            mTwaLauncher.destroy();
        }
        if (mSplashScreenStrategy != null) {
            mSplashScreenStrategy.destroy();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(BROWSER_WAS_LAUNCHED_KEY, mBrowserWasLaunched);
    }

    @Override
    public void onEnterAnimationComplete() {
        super.onEnterAnimationComplete();
        if (mSplashScreenStrategy != null) {
            mSplashScreenStrategy.onActivityEnterAnimationComplete();
        }
    }

    /**
     * Returns the URL that the Trusted Web Activity should be launched to. By default this
     * implementation checks to see if the Activity was launched with an Intent with data, if so
     * attempt to launch to that URL. If not, read the
     * "android.support.customtabs.trusted.DEFAULT_URL" metadata from the manifest.
     *
     * Override this for special handling (such as ignoring or sanitising data from the Intent).
     */
    protected Uri getLaunchingUrl() {
        Uri uri = getIntent().getData();
        if (uri != null) {
            Log.d(TAG, "Using URL from Intent (" + uri + ").");
            return uri;
        }

        if (mMetadata.defaultUrl != null) {
            Log.d(TAG, "Using URL from Manifest (" + mMetadata.defaultUrl + ").");
            return Uri.parse(mMetadata.defaultUrl);
        }

        return Uri.parse("https://www.example.com/");
    }

    /**
     * Returns the fallback strategy to be used if there's no Trusted Web Activity support on the
     * device. By default, used the "android.support.customtabs.trusted.DEFAULT_URL" metadata from
     * the manifest. If the value is not present, it uses a CustomTabs fallback.
     *
     * Override this for creating a custom fallback approach, such as launching a different WebView
     * fallback implementation ot starting a native Activity.
     */
    protected TwaLauncher.FallbackStrategy getFallbackStrategy() {
        if (FALLBACK_TYPE_WEBVIEW.equalsIgnoreCase(mMetadata.fallbackStrategyType)) {
            return TwaLauncher.WEBVIEW_FALLBACK_STRATEGY;
        }
        return TwaLauncher.CCT_FALLBACK_STRATEGY;
    }

    /**
     * Returns the display mode the TrustedWebWebActivity should be launched with. Defaults to the
     * "android.support.customtabs.trusted.DISPLAY_MODE" metadata from the manifest or the "default"
     * mode if the metadata is not present.
     *
     * Override this for starting the Trusted Web Activity with different display mode, with special
     * handling of screen cut-outs, for instance.
     */
    protected TrustedWebActivityDisplayMode getDisplayMode() {
        return this.mMetadata.displayMode;
    }

    private boolean restartInNewTask() {
        boolean hasNewTask = (getIntent().getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0;
        boolean hasNewDocument = (getIntent().getFlags() & Intent.FLAG_ACTIVITY_NEW_DOCUMENT) != 0;

        if (hasNewTask && !hasNewDocument) return false;

        // The desired behaviour of TWAs is that there's only one running in the user's Recent
        // Apps. This reflects how many other Android Apps work and corresponds to ensuring that
        // the TWA only exists in a single Task.

        // If the TWA was implemented as single Activity, we could do this with
        // launchMode=singleTask (or singleInstance), however since the TWA consists of a
        // LauncherActivity which then starts a browser Activity, things get a bit more
        // complicated.

        // If we used singleTask on LauncherActivity then whenever a TWA was running and a new
        // Intent was fired, the browser Activity on top would get clobbered.

        // Therefore, we always ensure that LauncherActivity is launched with New Task. This
        // means that if the TWA is already running a *new* LauncherActivity will be created on
        // top of the Browser Activity. The browser then launches an Intent with CLEAR_TOP to
        // the existing Browser Activity, killing the temporary LauncherActivity and focusing
        // the TWA.

        // We also need to clear NEW_DOCUMENT here as well otherwise Intents created with
        // NEW_DOCUMENT will launch us in a new Task, separate from an existing instance.
        // Setting documentLaunchMode="never" didn't stop this behaviour.
        Intent newIntent = new Intent(getIntent());

        int flags = getIntent().getFlags();
        flags |= Intent.FLAG_ACTIVITY_NEW_TASK;
        flags &= ~Intent.FLAG_ACTIVITY_NEW_DOCUMENT;
        newIntent.setFlags(flags);

        startActivity(newIntent);
        return true;
    }
}
