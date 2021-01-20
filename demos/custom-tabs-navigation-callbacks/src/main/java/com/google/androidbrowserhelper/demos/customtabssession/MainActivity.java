/*
 *    Copyright 2021 Google LLC
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.google.androidbrowserhelper.demos.customtabssession;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;

import android.app.Activity;
import android.content.ComponentName;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String TAG = "CustomTabsDemo";

    private static final Uri URL = Uri.parse("https://peconn.github.io/starters/");

    private CustomTabsSession mSession;
    private CustomTabsServiceConnection mConnection;

    private Button mLaunchButton;
    private TextView mLogView;

    private final StringBuilder mLogs = new StringBuilder();

    private void appendToLog(String log) {
        Log.d(TAG, log);

        mLogs.append(log);
        mLogs.append("\n");

        mLogView.setText(mLogs.toString());
    }

    @Override
    protected void onStart() {
        super.onStart();

        CustomTabsCallback callback = new CustomTabsCallback() {
            @Override
            public void onNavigationEvent(int navigationEvent, @Nullable Bundle extras) {
                appendToLog(eventToString(navigationEvent) + ": " + bundleToString(extras));
            }

            @Override
            public void extraCallback(@NonNull String callbackName, @Nullable Bundle args) {
                appendToLog("Extra: " + callbackName + ": " + bundleToString(args));
            }
        };

        mConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(@NonNull ComponentName name,
                    @NonNull CustomTabsClient client) {
                mSession = client.newSession(callback);
                client.warmup(0);
                mLaunchButton.setEnabled(true);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) { }
        };

        String packageName = CustomTabsClient.getPackageName(MainActivity.this, null);
        if (packageName == null) {
            Toast.makeText(this, "Can't find a Custom Tabs provider.", Toast.LENGTH_SHORT).show();
            return;
        }

        CustomTabsClient.bindCustomTabsService(this, packageName, mConnection);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLogView = findViewById(R.id.logs);
        mLaunchButton = findViewById(R.id.launch);
        mLaunchButton.setOnClickListener(view -> {
            CustomTabsIntent intent = new CustomTabsIntent.Builder(mSession).build();
            intent.launchUrl(MainActivity.this, URL);
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mConnection == null) return;
        unbindService(mConnection);
        mConnection = null;
        mLaunchButton.setEnabled(false);
    }

    private static String eventToString(int navigationEvent) {
        switch(navigationEvent) {
            case CustomTabsCallback.NAVIGATION_STARTED: return "Navigation Started";
            case CustomTabsCallback.NAVIGATION_FINISHED: return "Navigation Finished";
            case CustomTabsCallback.NAVIGATION_FAILED: return "Navigation Failed";
            case CustomTabsCallback.NAVIGATION_ABORTED: return "Navigation Aborted";
            case CustomTabsCallback.TAB_SHOWN: return "Tab Shown";
            case CustomTabsCallback.TAB_HIDDEN: return "Tab Hidden";
            default: return "Unknown Event";
        }
    }

    private static String bundleToString(Bundle bundle) {
        StringBuilder b = new StringBuilder();

        b.append("{");

        if (bundle != null) {
            boolean first = true;

            for (String key : bundle.keySet()) {
                if (!first) {
                    b.append(", ");
                }
                first = false;

                b.append(key);
                b.append(": ");
                b.append(bundle.get(key));
            }
        }

        b.append("}");

        return b.toString();
    }
}

