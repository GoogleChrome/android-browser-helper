
package com.google.androidbrowserhelper.trusted.splashscreens;


import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;

class SystemBarBackgroundDrawable extends Drawable {

    private int mStatusBarPadding;
    private int mNavigationBarPadding;

    private int mStatusBarColor;
    private int mNavigationBarColor;

    public SystemBarBackgroundDrawable(@ColorInt int defaultColor) {
        mStatusBarColor = defaultColor;
        mNavigationBarColor = defaultColor;
    }

    public void setSystemBarPaddings(int statusBarPadding, int navigationBarPadding) {
        mStatusBarPadding = statusBarPadding;
        mNavigationBarPadding = navigationBarPadding;
    }
    public void setNavigationBarColor(@ColorInt int navigationBarColor) {
        this.mNavigationBarColor = navigationBarColor;
    }

    public void setStatusBarColor(@ColorInt int statusBarColor) {
        this.mStatusBarColor = statusBarColor;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(0); // Width of the rectangle's border

        paint.setColor(mStatusBarColor);
        Rect statusBarRect = new Rect(bounds.left, bounds.top, bounds.right, bounds.top + mStatusBarPadding);
        canvas.drawRect(statusBarRect, paint);

        paint.setColor(mNavigationBarColor);
        Rect navigationBarRect = new Rect(bounds.left, bounds.bottom - mNavigationBarPadding, bounds.right, bounds.bottom);
        canvas.drawRect(navigationBarRect, paint);
    }

    // These methods must be implemented
    @Override
    public void setAlpha(int alpha) {}

    @Override
    public void setColorFilter(ColorFilter colorFilter) {}

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}