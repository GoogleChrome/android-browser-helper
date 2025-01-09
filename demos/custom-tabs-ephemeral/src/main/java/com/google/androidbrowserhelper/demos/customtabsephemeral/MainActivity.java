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

package com.google.androidbrowserhelper.demos.customtabsephemeral;

import androidx.annotation.OptIn;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.ExperimentalEphemeralBrowsing;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;

public class MainActivity extends Activity {
    private static final Uri URL = Uri.parse("https://xchrdw.github.io/browsing-data/siteDataTester.html");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button mLaunchButton = findViewById(R.id.launch);
        mLaunchButton.setOnClickListener(view -> {
            launchTab();
        });
    }

    @OptIn(markerClass = ExperimentalEphemeralBrowsing.class)
    private void launchTab() {
        Intent customTabsIntent = new CustomTabsIntent.Builder()
                .setEphemeralBrowsingEnabled(true)
                .build()
                .intent;
        customTabsIntent.setData(URL);
        startActivity(customTabsIntent);
    }
}

