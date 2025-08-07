/*
 *    Copyright 2024 Google LLC
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

package com.google.androidbrowserhelper.demos.customtabsephemeralwithfallback;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;


public class MainActivity extends Activity {
    private static final String URL = "https://xchrdw.github.io/browsing-data/siteDataTester.html";
    private CustomTabsSession mSession;
    private CustomTabsServiceConnection mConnection;

    @Override
    protected void onStart() {
        super.onStart();

        mConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(@NonNull ComponentName name,
                                                     @NonNull CustomTabsClient client) {
                mSession = client.newSession(null);
                client.warmup(0);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
            }
        };

        String packageName = CustomTabsClient.getPackageName(this, null);
        if (packageName != null) {
            CustomTabsClient.bindCustomTabsService(this, packageName, mConnection);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button launchButton = findViewById(R.id.launch);
        launchButton.setOnClickListener(view -> {
            try {
                if (isEphemeralTabSupported()) {
                    launchEphemeralTab();
                } else {
                    launchFallbackWebView();
                }
            } catch (RemoteException e) {
                launchFallbackWebView();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mConnection == null) return;
        unbindService(mConnection);
        mConnection = null;
    }

    private void launchEphemeralTab() {
        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
                .setEphemeralBrowsingEnabled(true)
                .setUrlBarHidingEnabled(false)
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                .setCloseButtonIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_back_arrow))
                .setDefaultColorSchemeParams(new CustomTabColorSchemeParams.Builder()
                        .setToolbarColor(getColor(R.color.colorPrimary)).build())
                .build();
        customTabsIntent.launchUrl(this, Uri.parse(URL));
    }

    private void launchFallbackWebView() {
        Intent webIntent = new Intent(this, WebViewActivity.class);
        webIntent.putExtra(WebViewActivity.EXTRA_URL, URL);
        startActivity(webIntent);
    }

    private boolean isEphemeralTabSupported() throws RemoteException {
        String provider = CustomTabsClient.getPackageName(this, null);
        if (provider == null) {
            return false;
        } else {
            return CustomTabsClient.isEphemeralBrowsingSupported(this, provider);
        }
    }
}

