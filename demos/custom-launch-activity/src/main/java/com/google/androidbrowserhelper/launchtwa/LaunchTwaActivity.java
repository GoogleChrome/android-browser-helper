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

package com.google.androidbrowserhelper.launchtwa;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.androidbrowserhelper.demo.R;
import com.google.androidbrowserhelper.trusted.TwaLauncher;
import com.google.androidbrowserhelper.trusted.TwaProviderPicker;

import java.util.Arrays;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.browser.trusted.TrustedWebActivityIntentBuilder;

public class LaunchTwaActivity extends AppCompatActivity {
    private static final Uri LAUNCH_URI =
            Uri.parse("https://github.com/GoogleChrome/android-browser-helper");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch_twa);
    }

    public void launch(View view) {
        new TwaLauncher(this).launch(LAUNCH_URI);
    }

    public void launchWithCustomColors(View view) {
        TrustedWebActivityIntentBuilder builder = new TrustedWebActivityIntentBuilder(LAUNCH_URI)
                        .setNavigationBarColor(Color.RED)
                        .setToolbarColor(Color.BLUE);

        new TwaLauncher(this).launch(builder, null, null);
    }

    public void launcherWithMultipleOrigins(View view) {
        List<String> origins = Arrays.asList(
                "https://www.wikipedia.org/",
                "https://www.example.com/"
        );

        TrustedWebActivityIntentBuilder builder = new TrustedWebActivityIntentBuilder(LAUNCH_URI)
                .setAdditionalTrustedOrigins(origins);

        new TwaLauncher(this).launch(builder, null, null);
    }

    public void launchWithCustomReferrer(View view) {
        // The ergonomics will be improved here, since we're basically replicating the work of
        // TwaLauncher, see https://github.com/GoogleChrome/android-browser-helper/issues/13.
        TrustedWebActivityIntentBuilder builder = new TrustedWebActivityIntentBuilder(LAUNCH_URI);

        TwaProviderPicker.Action action = TwaProviderPicker.pickProvider(getPackageManager());
        CustomTabsClient.bindCustomTabsService(this, action.provider,
                new CustomTabsServiceConnection() {
            CustomTabsSession mSession;
            private final static int SESSION_ID = 45;  // An arbitrary constant.

            @Override
            public void onCustomTabsServiceConnected(ComponentName name,
                    CustomTabsClient client) {
                mSession = client.newSession(null, SESSION_ID);

                if (mSession == null) {
                    Toast.makeText(LaunchTwaActivity.this,
                            "Couldn't get session from provider.", Toast.LENGTH_LONG).show();
                }

                Intent intent = builder.build(mSession);
                intent.putExtra(Intent.EXTRA_REFERRER,
                        Uri.parse("android-app://com.google.androidbrowserhelper?twa=true"));
                startActivity(intent);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mSession = null;
            }
        });
    }
}
