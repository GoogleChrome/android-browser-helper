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

package com.google.androidbrowserhelper.trusted;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebViewFallbackActivity extends Activity {
    private static final String TAG = WebViewFallbackActivity.class.getSimpleName();
    private static final String KEY_PREFIX =
            "com.google.browser.examples.twawebviewfallback.WebViewFallbackActivity.";
    private static final String KEY_LAUNCH_URI = KEY_PREFIX + "LAUNCH_URL";
    private static final String KEY_NAVIGATION_BAR_COLOR = KEY_PREFIX + "KEY_NAVIGATION_BAR_COLOR";
    private static final String KEY_STATUS_BAR_COLOR = KEY_PREFIX + "KEY_STATUS_BAR_COLOR";
    private static final String KEY_EXTRA_ORIGINS = KEY_PREFIX + "KEY_EXTRA_ORIGINS";

    private Uri mLaunchUrl;
    private int mStatusBarColor;
    private WebView mWebView;
    private List<Uri> mExtraOrigins = new ArrayList<>();

    public static Intent createLaunchIntent(
            Context context,
            Uri launchUrl,
            LauncherActivityMetadata launcherActivityMetadata) {
        Intent intent = new Intent(context, WebViewFallbackActivity.class);
        intent.putExtra(WebViewFallbackActivity.KEY_LAUNCH_URI, launchUrl);

        intent.putExtra(WebViewFallbackActivity.KEY_STATUS_BAR_COLOR,
                ContextCompat.getColor(context, launcherActivityMetadata.statusBarColorId));
        intent.putExtra(WebViewFallbackActivity.KEY_NAVIGATION_BAR_COLOR,
                ContextCompat.getColor(context, launcherActivityMetadata.navigationBarColorId));

        if (launcherActivityMetadata.additionalTrustedOrigins != null) {
            ArrayList<String> extraOrigins =
                    new ArrayList<>(launcherActivityMetadata.additionalTrustedOrigins);
            intent.putStringArrayListExtra(WebViewFallbackActivity.KEY_EXTRA_ORIGINS, extraOrigins);
        }
        return intent;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.mLaunchUrl = this.getIntent().getParcelableExtra(KEY_LAUNCH_URI);
        if (!"https".equals(this.mLaunchUrl.getScheme())) {
            throw new IllegalArgumentException("launchUrl scheme must be 'https'");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (getIntent().hasExtra(KEY_NAVIGATION_BAR_COLOR)) {
                int navigationBarColor = this.getIntent().getIntExtra(KEY_NAVIGATION_BAR_COLOR, 0);
                getWindow().setNavigationBarColor(navigationBarColor);
            }
        }

        if (getIntent().hasExtra(KEY_STATUS_BAR_COLOR)) {
            mStatusBarColor = this.getIntent().getIntExtra(KEY_STATUS_BAR_COLOR, 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(mStatusBarColor);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mStatusBarColor = getWindow().getStatusBarColor();
            } else {
                mStatusBarColor = Color.WHITE;
            }
        }

        if (getIntent().hasExtra(KEY_EXTRA_ORIGINS)) {
            List<String> extraOrigins = getIntent().getStringArrayListExtra(KEY_EXTRA_ORIGINS);
            if (extraOrigins != null) {
                for (String extraOrigin : extraOrigins) {
                    Uri extraOriginUri = Uri.parse(extraOrigin);
                    if (!"https".equalsIgnoreCase(extraOriginUri.getScheme())) {
                        Log.w(TAG, "Only 'https' origins are accepted. Ignoring extra origin: "
                                + extraOrigin);
                        continue;
                    }
                    mExtraOrigins.add(extraOriginUri);
                }
            }
        }

        mWebView = new WebView(this);
        mWebView.setWebViewClient(createWebViewClient());
        mWebView.setWebChromeClient(createWebViewChromeClient());

        WebSettings webSettings = mWebView.getSettings();
        setupWebSettings(webSettings);

        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        setContentView(mWebView, layoutParams);
        if (savedInstanceState != null) {
            mWebView.restoreState(savedInstanceState);
            return;
        }

        // Applications running in a Trusted Web Activity are supposed to have
        // android-app://<package-name> as the referrer.
        Map<String, String> headers = new HashMap<>();
        headers.put("Referer", "android-app://" + getPackageName() + "/");
        mWebView.loadUrl(mLaunchUrl.toString(), headers);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mWebView != null) {
            mWebView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mWebView != null) {
            mWebView.onResume();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mWebView != null) {
            mWebView.saveState(outState);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private WebViewClient createWebViewClient() {
        return new WebViewClient() {
            @Override
            public boolean onRenderProcessGone(
                    WebView view, RenderProcessGoneDetail detail) {
                ViewGroup vg = (ViewGroup) view.getParent();

                // Remove crashed WebView from the hierarchy
                // and ensure it is destroyed.
                vg.removeView(view);
                view.destroy();

                // Create a new instance, and ensure it also
                // handles crashes - in this case, re-using
                // the current WebViewClient
                mWebView = new WebView(view.getContext());
                mWebView.setWebViewClient(this);
                WebSettings webSettings = mWebView.getSettings();
                setupWebSettings(webSettings);
                vg.addView(mWebView);

                // With the crash recovered, decide what to do next.
                // We are sending a toast and loading the origin
                // URL, in this example.
                Toast.makeText(view.getContext(), "Recovering from crash",
                        Toast.LENGTH_LONG).show();
                mWebView.loadUrl(mLaunchUrl.toString());
                return true;
            }

            private boolean shouldOverrideUrlLoading(Uri navigationUrl) {
                Uri launchUrl = WebViewFallbackActivity.this.mLaunchUrl;
                // If the user is navigation to a different origin, use CCT to handle the navigation
                //
                // URIs with the `data` scheme are handled in the WebView.
                // The "Demo" item in https://jakearchibald.github.io/svgomg/ is one example of this
                // usage
                if (!"data".equals(navigationUrl.getScheme()) &&
                        !uriOriginsMatch(navigationUrl, launchUrl) &&
                        !matchExtraOrigins(navigationUrl)) {
                    // A Custom Tab is an ACTION_VIEW Intent with special extras that cause the
                    // handler for that Intent to change its behaviour. This Intent should be
                    // able to trigger both browsers or platform-specific apps that handle those
                    // Intents.
                    // However, some URLs, like the data:// schema must be handled by the
                    // WebView itself. If an URL can't be handled by any app we allow the WebView
                    // to try to handle it.
                    try {
                        CustomTabsIntent intent = new CustomTabsIntent.Builder()
                                .setToolbarColor(mStatusBarColor)
                                .build();
                        intent.launchUrl(WebViewFallbackActivity.this, navigationUrl);
                        return true;
                    } catch (ActivityNotFoundException ex) {
                        Log.e(TAG, String.format(
                                "ActivityNotFoundException while launching '%s'", navigationUrl));
                        return false;
                    }
                }

                return false;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return this.shouldOverrideUrlLoading(Uri.parse(url));
            }

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return this.shouldOverrideUrlLoading(request.getUrl());
            }

            private boolean matchExtraOrigins(Uri navigationUri) {
                for (Uri uri : mExtraOrigins) {
                    if (uriOriginsMatch(uri, navigationUri)) {
                        return true;
                    }
                }
                return false;
            }

            private boolean uriOriginsMatch(Uri uriA, Uri uriB) {
                return uriA.getScheme().equalsIgnoreCase(uriB.getScheme()) &&
                        uriA.getHost().equalsIgnoreCase(uriB.getHost()) &&
                        uriA.getPort() == uriB.getPort();
            }
        };
    }

    private WebChromeClient createWebViewChromeClient() {
        return new WebChromeClient() {
            private View fullScreenView;
            private int originalOrientation;

            @Override
            public void onShowCustomView(View paramView, CustomViewCallback paramCustomViewCallback) {
                // Make sure that we don't have another fullscreen shown already.
                if (this.fullScreenView != null) {
                    onHideCustomView();
                }
                // Save the fullscreen view in order to be able to remove it later when requested.
                this.fullScreenView = paramView;
                // Save the current orientation in order to be able to return the state after
                // exiting the fullscreen.
                this.originalOrientation = getRequestedOrientation();

                getWindow().addContentView(this.fullScreenView,
                        new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
                getWindow().addFlags(View.SYSTEM_UI_FLAG_FULLSCREEN);
            }

            @Override
            public void onHideCustomView() {
                // If we don't have a fullscreen then no-op.
                if (fullScreenView == null) {
                    return;
                }
                ((ViewGroup) fullScreenView.getParent()).removeView(fullScreenView);
                this.fullScreenView = null;
                getWindow().clearFlags(View.SYSTEM_UI_FLAG_FULLSCREEN);
                setRequestedOrientation(this.originalOrientation);
            }
        };
    }

    @SuppressLint("SetJavaScriptEnabled")
    private static void setupWebSettings(WebSettings webSettings) {
        // Those settings are disabled by default.
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
    }
}
