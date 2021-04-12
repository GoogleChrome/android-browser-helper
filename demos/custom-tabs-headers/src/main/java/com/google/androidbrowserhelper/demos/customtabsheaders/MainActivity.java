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

package com.google.androidbrowserhelper.demos.customtabsheaders;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsService;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;

import android.app.Activity;
import android.content.ComponentName;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Browser;
import android.widget.Button;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity {
    // This project is using a demo page hosted at Glitch. However, the demo will only work if the
    // Digital Asset Links validation is successful. So, when testing on your computer, remix the
    // project at https://glitch.com/edit/#!/custom-tabs-custom-he, edit the file under
    // `public/.well-known/assetlinks.json` with your own SHA-256 fingerprint (use Tools > Terminal
    // to find and edit the file), and update the URL below to the new project.
    private static final Uri URL = Uri.parse("https://custom-tabs-custom-he.glitch.me/");

    private CustomTabsSession mSession;
    private CustomTabsServiceConnection mConnection;

    private Button mExtraButton;

    @Override
    protected void onStart() {
        super.onStart();

        // Set up a callback that launches the intent after session was validated.
        CustomTabsCallback callback = new CustomTabsCallback() {
            @Override
            public void onRelationshipValidationResult(int relation, @NonNull Uri requestedOrigin,
                                                       boolean result, @Nullable Bundle extras) {
                // Can launch custom tabs intent after session was validated as the same origin.
                mExtraButton.setEnabled(true);
            }
        };

        // Set up a connection that warms up and validates a session.
        mConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(@NonNull ComponentName name,
                                                     @NonNull CustomTabsClient client) {
                // Create session after service connected.
                mSession = client.newSession(callback);
                client.warmup(0);
                // Validate the session as the same origin to allow cross origin headers.
                mSession.validateRelationship(CustomTabsService.RELATION_USE_AS_ORIGIN,
                        URL, null);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
            }
        };

        //Add package names for other browsers that support Custom Tabs and custom headers.
        List<String> packageNames = Arrays.asList(
                "com.google.android.apps.chrome",
                "com.chrome.canary",
                "com.chrome.dev",
                "com.chrome.beta",
                "com.android.chrome"
        );
        String packageName =
                CustomTabsClient.getPackageName(MainActivity.this, packageNames, false);
        if (packageName == null) {
            Toast.makeText(getApplicationContext(), "Package name is null.", Toast.LENGTH_SHORT)
                    .show();
        } else {
            // Bind the custom tabs service connection.
            CustomTabsClient.bindCustomTabsService(this, packageName, mConnection);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mExtraButton = findViewById(R.id.btn_extra);
        mExtraButton.setOnClickListener(view -> {
            CustomTabsIntent intent = constructExtraHeadersIntent(mSession);
            intent.launchUrl(MainActivity.this, URL);
        });
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Unbind from the service if we connected successfully and clear the session.
        if (mSession != null) {
            unbindService(mConnection);
            mConnection = null;
            mSession = null;
        }
        mExtraButton.setEnabled(false);
    }

    private CustomTabsIntent constructExtraHeadersIntent(CustomTabsSession session) {
        CustomTabsIntent intent = new CustomTabsIntent.Builder(session).build();

        // Example non-cors-whitelisted headers.
        Bundle headers = new Bundle();
        headers.putString("bearer-token", "Some token");
        headers.putString("redirect-url", "Some redirect url");
        intent.intent.putExtra(Browser.EXTRA_HEADERS, headers);

        return intent;
    }
}

