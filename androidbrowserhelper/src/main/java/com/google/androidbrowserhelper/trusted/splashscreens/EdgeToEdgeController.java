package com.google.androidbrowserhelper.trusted.splashscreens;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class EdgeToEdgeController {
    private Activity mActivity;

    private SystemBarBackgroundDrawable mSystemBarBackgroundDrawable;

    public EdgeToEdgeController(Activity activity) {
        mActivity = activity;
    }

    public FrameLayout getWrapperView(@ColorInt int defaultColor) {
        FrameLayout rootView = new FrameLayout(mActivity);
        rootView.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        mSystemBarBackgroundDrawable = new SystemBarBackgroundDrawable(defaultColor);
        rootView.setBackground(mSystemBarBackgroundDrawable);

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            mSystemBarBackgroundDrawable.setSystemBarPaddings(systemBarInsets.top, systemBarInsets.bottom);
            v.setPadding(0, systemBarInsets.top, 0, systemBarInsets.bottom);
                        return WindowInsetsCompat.CONSUMED;
        });

        // This is required to make system bars transparent
        ViewCompat.setOnApplyWindowInsetsListener(mActivity.getWindow().getDecorView(), (v, insets) -> insets);

        return rootView;
    }

    public void setStatusBarColor(@ColorInt int color) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Call the old API for compatibility with older SDKs, in case edge to edge cannot be enabled
            mActivity.getWindow().setStatusBarColor(color);
        }
        mSystemBarBackgroundDrawable.setStatusBarColor(color);
            WindowInsetsControllerCompat windowInsetsController =
                    WindowCompat.getInsetsController(mActivity.getWindow(), mActivity.getWindow().getDecorView());
            if (windowInsetsController != null) {
            boolean shouldUseDarkIcons = shouldUseDarkIconsOnBackground(color);
            windowInsetsController.setAppearanceLightStatusBars(shouldUseDarkIcons);
        }
    }

    public void setNavigationBarColor(@ColorInt int color) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Call the old API for compatibility with older SDKs, in case edge to edge cannot be enabled
            mActivity.getWindow().setNavigationBarColor(color);
        }
        mSystemBarBackgroundDrawable.setNavigationBarColor(color);
            WindowInsetsControllerCompat windowInsetsController =
                    WindowCompat.getInsetsController(mActivity.getWindow(), mActivity.getWindow().getDecorView());
            if (windowInsetsController != null) {
            boolean shouldUseDarkIcons = shouldUseDarkIconsOnBackground(color);
            windowInsetsController.setAppearanceLightNavigationBars(shouldUseDarkIcons);
        }
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
}
