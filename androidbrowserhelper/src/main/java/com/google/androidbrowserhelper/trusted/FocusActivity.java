package com.google.androidbrowserhelper.trusted;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;

/**
 * The purpose of this Activity is to allow this app's Task to be brought to the foreground.
 *
 * A PendingIntent to this Activity is provided to the browser. When the browser wants to bring
 * the app to the foreground (eg as the result of a web notification calling WindowClient.focus()),
 * the browser can launch that PendingIntent bringing this Activity to the foreground. This Activity
 * then finishes itself, revealing the other Activities in the app's Task. Hopefully the topmost
 * Activity will be the web browsing Activity.
 */
public class FocusActivity extends Activity {
    // This value should be moved into androidx.browser.
    private static final String EXTRA_FOCUS_INTENT =
            "androidx.browser.customtabs.extra.FOCUS_INTENT";

    private static Boolean mActivityExistsCached;

    public static void addToIntent(Intent containerIntent, Context context) {
        Intent focusIntent = new Intent(context, FocusActivity.class);

        // This class may not be included in app's manifest, don't add it in that case.
        if (mActivityExistsCached == null) {
            mActivityExistsCached =
                    focusIntent.resolveActivityInfo(context.getPackageManager(), 0) != null;
        }
        if (Boolean.FALSE.equals(mActivityExistsCached)) return;

        // This Intent will be launched from the background so we need NEW_TASK, however if an
        // existing task is suitable, that will be brought to the foreground despite the name of the
        // flag.
        focusIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Setting FLAG_MUTABLE or FLAG_IMMUTABLE is required from API Level 31 and above. However,
        // the flag is not available below API level 23.
        int flags =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
        containerIntent.putExtra(EXTRA_FOCUS_INTENT,
                PendingIntent.getActivity(context, 0, focusIntent, flags));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finish();
    }
}