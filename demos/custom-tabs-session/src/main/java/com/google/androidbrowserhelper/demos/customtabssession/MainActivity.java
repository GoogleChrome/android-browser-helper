/*
 *    Copyright 2020 Google LLC
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
import androidx.browser.customtabs.CustomTabsService;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.core.app.NotificationCompat;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Browser;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final Uri URL = Uri.parse("https://peconn.github.io/starters/");
    private static final Uri UPDATED_URL =
            Uri.parse("https://peconn.github.io/starters/?updated=true");

    private CustomTabsSession mSession;
    private CustomTabsServiceConnection mConnection;

    private Button mExtraButton;

    @Override
    protected void onStart() {
        super.onStart();

        mConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(@NonNull ComponentName name,
                    @NonNull CustomTabsClient client) {
                mSession = client.newSession(null);
                client.warmup(0);
                mExtraButton.setEnabled(true);
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

        mExtraButton = findViewById(R.id.launch);
        mExtraButton.setOnClickListener(view -> {
            CustomTabsIntent intent = new CustomTabsIntent.Builder(mSession).build();
            intent.launchUrl(MainActivity.this, URL);

            new Handler(Looper.myLooper()).postDelayed(() -> {
                CustomTabsIntent updateIntent = new CustomTabsIntent.Builder(mSession).build();
                updateIntent.launchUrl(MainActivity.this, UPDATED_URL);
            }, 5000);
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mConnection == null) return;
        unbindService(mConnection);
        mConnection = null;
        mExtraButton.setEnabled(false);
    }
}

