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

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.ServiceWorkerController;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.Nullable;

public class WebViewActivity extends Activity {

    public static final String EXTRA_URL = "extra_url";

    private WebView mWebView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mWebView = findViewById(R.id.webview);
        setupWebView();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);

        String url = getIntent().getStringExtra(EXTRA_URL);
        if (url == null || url.isEmpty()) {
            finish();
            return;
        }

        clearAllWebData();

        // Disable service workers
        ServiceWorkerController swController = ServiceWorkerController.getInstance();
        swController.getServiceWorkerWebSettings().setBlockNetworkLoads(true);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                // Block JavaScript Web Locks API
                view.evaluateJavascript(
                        "Object.defineProperty(navigator, 'locks', { value: null });",
                        null
                );
            }
        });

        mWebView.loadUrl(url);
    }

    private void clearAllWebData() {

        // Clear cookies, cache, history and stored file data
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();

        mWebView.clearCache(true);
        mWebView.clearHistory();
        mWebView.clearFormData();
        mWebView.clearSslPreferences();
        mWebView.clearMatches();

        WebStorage.getInstance().deleteAllData();
        Utils.clearAppDirectory(this);
    }
}
