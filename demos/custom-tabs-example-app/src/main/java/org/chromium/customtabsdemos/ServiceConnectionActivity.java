// Copyright 2015 Google Inc. All Rights Reserved.
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
package org.chromium.customtabsdemos;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

/**
 * This Activity connect to the Chrome Custom Tabs Service on startup, and allows you to decide
 * when to call mayLaunchUrl.
 */
public class ServiceConnectionActivity extends AppCompatActivity
        implements View.OnClickListener, CustomTabActivityHelper.ConnectionCallback {
    private EditText mUrlEditText;
    private View mMayLaunchUrlButton;
    private CustomTabActivityHelper customTabActivityHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_serviceconnection);

        mUrlEditText = findViewById(R.id.url);
        mMayLaunchUrlButton = findViewById(R.id.button_may_launch_url);
        mMayLaunchUrlButton.setEnabled(false);
        mMayLaunchUrlButton.setOnClickListener(this);

        findViewById(R.id.start_custom_tab).setOnClickListener(this);

        customTabActivityHelper = new CustomTabActivityHelper();
        customTabActivityHelper.setConnectionCallback(this);
        customTabActivityHelper.bindCustomTabsService(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        customTabActivityHelper.setConnectionCallback(null);
        customTabActivityHelper.unbindCustomTabsService(this);
    }

    @Override
    public void onCustomTabsConnected() {
        mMayLaunchUrlButton.setEnabled(true);
    }

    @Override
    public void onCustomTabsDisconnected() {
        mMayLaunchUrlButton.setEnabled(false);
    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        Uri uri  = Uri.parse(mUrlEditText.getText().toString());
        if (viewId == R.id.button_may_launch_url) {
            customTabActivityHelper.mayLaunchUrl(uri, null, null);
        } else if (viewId == R.id.start_custom_tab) {
            CustomTabsIntent customTabsIntent =
                    new CustomTabsIntent.Builder(customTabActivityHelper.getSession())
                            .build();
            CustomTabActivityHelper.openCustomTab(
                    this, customTabsIntent, uri, new WebviewFallback());
        }
    }
}
