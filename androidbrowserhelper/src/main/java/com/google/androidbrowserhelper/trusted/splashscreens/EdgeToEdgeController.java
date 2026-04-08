package com.google.androidbrowserhelper.trusted.splashscreens;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.insets.ColorProtection;
import androidx.core.view.insets.ProtectionLayout;

import com.google.common.collect.ImmutableList;

/**
 * A helper class to control the color of system bars in edge-to-edge mode.
 * It sets up a {@link ProtectionLayout} which manages the drawables behind
 * the status and navigation bars.
 */
public class EdgeToEdgeController {
    private FrameLayout mRootView;
    private ProtectionLayout mProtectionLayout;
    private ColorProtection mStatusBarProtection;
    private ColorProtection mNavigationBarProtection;

    public EdgeToEdgeController(Activity activity, @ColorInt int defaultColor) {
        mStatusBarProtection = new ColorProtection(WindowInsetsCompat.Side.TOP, defaultColor);
        mNavigationBarProtection = new ColorProtection(WindowInsetsCompat.Side.BOTTOM, defaultColor);

        mRootView = new FrameLayout(activity);
        mRootView.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));

        mProtectionLayout = new ProtectionLayout(activity,
                ImmutableList.of(mStatusBarProtection, mNavigationBarProtection));
        mRootView.addView(mProtectionLayout);
        mProtectionLayout.setVisibility(View.VISIBLE);
    }

    public FrameLayout getWrapperView() {
        return mRootView;
    }

    public void addView(View originalView) {
        mProtectionLayout.addView(originalView);
    }

    public void setStatusBarColor(@ColorInt int color) {
        mStatusBarProtection.setColor(color);
    }

    public void setNavigationBarColor(@ColorInt int color) {
        mNavigationBarProtection.setColor(color);
    }
}
