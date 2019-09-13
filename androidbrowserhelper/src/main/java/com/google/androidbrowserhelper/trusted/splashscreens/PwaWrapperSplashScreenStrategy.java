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

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.androidbrowserhelper.trusted.FeatureDetector;
import com.google.androidbrowserhelper.trusted.Utils;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.browser.customtabs.TrustedWebUtils;
import androidx.browser.trusted.TrustedWebActivityDisplayMode;
import androidx.browser.trusted.TrustedWebActivityDisplayMode.ImmersiveMode;
import androidx.browser.trusted.TrustedWebActivityIntentBuilder;
import androidx.browser.trusted.splashscreens.SplashScreenParamKey;
import androidx.browser.trusted.splashscreens.SplashScreenVersion;

/**
 * Implementation of {@link SplashScreenStrategy} suitable for apps that are PWA wrappers (i.e.
 * apps having no other UI outside of a TWA they launch).
 *
 * Shows splash screen in the client app before TWA is launched, then seamlessly transfers it into
 * the browser, which keeps it visible until the web page is loaded. The browser must support
 * {@link SplashScreenVersion#V1}.
 *
 * To use this you need to set up a FileProvider in AndroidManifest with the following paths:
 * <paths><files-path path="twa_splash/" name="twa_splash"/></paths>.
 *
 * **NB**: This class requires {@link #onActivityEnterAnimationComplete} to be called from
 * {@link Activity#onEnterAnimationComplete()}.
 */
public class PwaWrapperSplashScreenStrategy implements SplashScreenStrategy {

    private static final String TAG = "SplashScreenStrategy";

    private static final int IMMERSIVE_MODE_UI_FLAGS = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
            | View.SYSTEM_UI_FLAG_LOW_PROFILE
            | View.SYSTEM_UI_FLAG_IMMERSIVE;

    private static final int IMMERSIVE_STICKY_MODE_UI_FLAGS = IMMERSIVE_MODE_UI_FLAGS
            & ~View.SYSTEM_UI_FLAG_IMMERSIVE
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

    private final FeatureDetector mFeatureDetector = new FeatureDetector();
    private SystemBarColorPredictor mSystemBarColorPredictor =
            new SystemBarColorPredictor(mFeatureDetector);

    private final Activity mActivity;
    @DrawableRes
    private final int mDrawableId;
    @ColorInt
    private final int mBackgroundColor;
    private final ImageView.ScaleType mScaleType;
    @Nullable
    private final Matrix mTransformationMatrix;
    private final String mFileProviderAuthority;
    private final int mFadeOutDurationMillis;

    @Nullable
    private Bitmap mSplashImage;

    @Nullable
    private SplashImageTransferTask mSplashImageTransferTask;

    @Nullable
    private String mProviderPackage;

    private boolean mProviderSupportsSplashScreens;

    // Defaulting to true for pre-L because enter animations were introduced in L.
    private boolean mEnterAnimationComplete = Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP;

    @Nullable
    private Runnable mOnEnterAnimationCompleteRunnable;

    /**
     * @param activity {@link Activity} on top of which a TWA is going to be launched.
     * @param drawableId Resource id of the Drawable of an image (e.g. logo) displayed in the
     * splash screen.
     * @param backgroundColor Background color of the splash screen.
     * @param scaleType see {@link SplashScreenParamKey#SCALE_TYPE}
     * @param transformationMatrix see {@link SplashScreenParamKey#IMAGE_TRANSFORMATION_MATRIX}.
     * @param fadeOutDurationMillis see {@link SplashScreenParamKey#FADE_OUT_DURATION_MS}.
     * @param fileProviderAuthority Authority of a FileProvider used for transferring the splash
     * image to the browser.
     */
    public PwaWrapperSplashScreenStrategy(
            Activity activity,
            @DrawableRes int drawableId,
            @ColorInt int backgroundColor,
            ImageView.ScaleType scaleType,
            @Nullable Matrix transformationMatrix,
            int fadeOutDurationMillis,
            String fileProviderAuthority) {
        mDrawableId = drawableId;
        mBackgroundColor = backgroundColor;
        mScaleType = scaleType;
        mTransformationMatrix = transformationMatrix;
        mActivity = activity;
        mFileProviderAuthority = fileProviderAuthority;
        mFadeOutDurationMillis = fadeOutDurationMillis;
    }

    @Override
    public void onTwaLaunchInitiated(String providerPackage, TrustedWebActivityIntentBuilder builder) {
        mProviderPackage = providerPackage;
        mProviderSupportsSplashScreens = TrustedWebUtils.splashScreensAreSupported(mActivity,
                providerPackage, SplashScreenVersion.V1);

        if (!mProviderSupportsSplashScreens) {
            Log.w(TAG, "Provider " + providerPackage + " doesn't support splash screens");
            return;
        }

        showSplashScreen();
        if (mSplashImage != null) {
            setImmersiveModeDuringSplashScreen(providerPackage, builder);
            customizeStatusAndNavBarDuringSplashScreen(providerPackage, builder);
        }
    }

    /**
     * Splash screen is shown both before the Trusted Web Activity is launched - in this activity,
     * and for some time after that - in browser, on top of web page being loaded.
     * This method shows the splash screen in the LauncherActivity.
     */
    private void showSplashScreen() {
        mSplashImage = Utils.convertDrawableToBitmap(mActivity, mDrawableId);
        if (mSplashImage == null) {
            Log.w(TAG, "Failed to retrieve splash image from provided drawable id");
            return;
        }
        ImageView view = new ImageView(mActivity);
        view.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        view.setImageBitmap(mSplashImage);
        view.setBackgroundColor(mBackgroundColor);

        view.setScaleType(mScaleType);
        if (mScaleType == ImageView.ScaleType.MATRIX) {
            view.setImageMatrix(mTransformationMatrix);
        }

        mActivity.setContentView(view);
    }

    /**
     * Sets the colors of status and navigation bar to match the ones seen after the splash screen
     * is transferred to the browser.
     */
    private void customizeStatusAndNavBarDuringSplashScreen(
            String providerPackage, @Nullable TrustedWebActivityIntentBuilder builder) {
        Integer navbarColor = mSystemBarColorPredictor.getExpectedNavbarColor(mActivity,
                providerPackage, builder);
        if (navbarColor != null) {
            Utils.setNavigationBarColor(mActivity, navbarColor);
        }

        Integer statusBarColor = mSystemBarColorPredictor.getExpectedStatusBarColor(mActivity,
                providerPackage, builder);
        if (statusBarColor != null) {
            Utils.setStatusBarColor(mActivity, statusBarColor);
        }
    }

    /**
     * If the provider is going to set immersive mode, the client must set it as well while showing
     * the splash screen.
     */
    private void setImmersiveModeDuringSplashScreen(String providerPackage,
            TrustedWebActivityIntentBuilder builder) {
        if (!mFeatureDetector.providerSupportsImmersiveMode(mActivity, providerPackage)) {
            return;
        }
        TrustedWebActivityDisplayMode displayMode = builder.getDisplayMode();
        if (!(displayMode instanceof ImmersiveMode)) {
            return;
        }
        ImmersiveMode immersiveMode = (ImmersiveMode) displayMode;
        int flags = immersiveMode.isSticky ? IMMERSIVE_STICKY_MODE_UI_FLAGS
                : IMMERSIVE_MODE_UI_FLAGS;
        View decor = mActivity.getWindow().getDecorView();
        decor.setSystemUiVisibility(decor.getSystemUiVisibility() | flags);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mActivity.getWindow().getAttributes().layoutInDisplayCutoutMode =
                    immersiveMode.layoutInDisplayCutoutMode;
        }
    }

    @Override
    public void configureTwaBuilder(TrustedWebActivityIntentBuilder builder,
            CustomTabsSession session,
            Runnable onReadyCallback) {
        if (!mProviderSupportsSplashScreens || mSplashImage == null) {
            onReadyCallback.run();
            return;
        }
        if (TextUtils.isEmpty(mFileProviderAuthority)) {
            Log.w(TAG, "FileProvider authority not specified, can't transfer splash image.");
            onReadyCallback.run();
            return;
        }
        mSplashImageTransferTask = new SplashImageTransferTask(mActivity,
                mSplashImage, mFileProviderAuthority, session,
                mProviderPackage);

        mSplashImageTransferTask.execute(
                success -> onSplashImageTransferred(builder, success, onReadyCallback));
    }

    private void onSplashImageTransferred(TrustedWebActivityIntentBuilder builder, boolean success,
            Runnable onReadyCallback) {
        if (!success) {
            Log.w(TAG, "Failed to transfer splash image.");
            onReadyCallback.run();
            return;
        }
        builder.setSplashScreenParams(makeSplashScreenParamsBundle());

        runWhenEnterAnimationComplete(() -> {
            onReadyCallback.run();
            mActivity.overridePendingTransition(0, 0); // Avoid window animations during transition.
        });
    }

    private void runWhenEnterAnimationComplete(Runnable runnable) {
        if (mEnterAnimationComplete) {
            runnable.run();
        } else {
            mOnEnterAnimationCompleteRunnable = runnable;
        }
    }

    @NonNull
    private Bundle makeSplashScreenParamsBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(SplashScreenParamKey.VERSION, SplashScreenVersion.V1);
        bundle.putInt(SplashScreenParamKey.FADE_OUT_DURATION_MS, mFadeOutDurationMillis);
        bundle.putInt(SplashScreenParamKey.BACKGROUND_COLOR, mBackgroundColor);
        bundle.putInt(SplashScreenParamKey.SCALE_TYPE, mScaleType.ordinal());
        if (mTransformationMatrix != null) {
            float[] values = new float[9];
            mTransformationMatrix.getValues(values);
            bundle.putFloatArray(SplashScreenParamKey.IMAGE_TRANSFORMATION_MATRIX,
                    values);
        }
        return bundle;
    }

    /**
     * To be called from {@link Activity#onEnterAnimationComplete}.
     */
    public void onActivityEnterAnimationComplete() {
        mEnterAnimationComplete = true;
        if (mOnEnterAnimationCompleteRunnable != null) {
            mOnEnterAnimationCompleteRunnable.run();
            mOnEnterAnimationCompleteRunnable = null;
        }
    }

    /**
     * Performs clean-up.
     */
    public void destroy() {
        if (mSplashImageTransferTask != null) {
            mSplashImageTransferTask.cancel();
        }
    }
}