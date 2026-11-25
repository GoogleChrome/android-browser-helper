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
import android.view.WindowManager;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
    private WebViewClient mWebViewClient;
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
        if (this.mLaunchUrl != null) {
            this.mLaunchUrl = Uri.parse(this.mLaunchUrl.toString());
        }
        if (this.mLaunchUrl == null || !"https".equalsIgnoreCase(this.mLaunchUrl.getScheme())) {
            throw new IllegalArgumentException("launchUrl scheme must be 'https'");
        }

        if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        ) {
            if (getIntent().hasExtra(KEY_NAVIGATION_BAR_COLOR)) {
                int navigationBarColor = this.getIntent().getIntExtra(KEY_NAVIGATION_BAR_COLOR, 0);
                getWindow().setNavigationBarColor(navigationBarColor);
            }
        }

        if (getIntent().hasExtra(KEY_STATUS_BAR_COLOR)) {
            mStatusBarColor = this.getIntent().getIntExtra(KEY_STATUS_BAR_COLOR, 0);
            if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                    Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            ) {
                getWindow().setStatusBarColor(mStatusBarColor);
            }
        } else {
            if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                    Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            ) {
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
        mWebViewClient = createWebViewClient();
        mWebView.setWebViewClient(mWebViewClient);
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

    protected WebViewClient createWebViewClient() {
        return new FallbackWebViewClient();
    }

    protected class FallbackWebViewClient extends WebViewClient {
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
            mWebView.setWebViewClient(mWebViewClient != null ? mWebViewClient : this);
            WebSettings webSettings = mWebView.getSettings();
            WebViewFallbackActivity.this.setupWebSettings(webSettings);
            vg.addView(mWebView);

            // With the crash recovered, decide what to do next.
            // We are sending a toast and loading the origin
            // URL, in this example.
            Toast.makeText(view.getContext(), "Recovering from crash",
                    Toast.LENGTH_LONG).show();
            mWebView.loadUrl(mLaunchUrl.toString());
            return true;
        }

        protected boolean shouldOverrideUrlLoading(@NonNull Uri url, boolean isMainFrame) {
            if (url == null) {
                Log.w(TAG, "Blocked navigation to null URL in WebViewFallbackActivity");
                return true;
            }
            String scheme = url.getScheme();
            if (scheme == null) {
                Log.w(TAG, "Blocked navigation to URL with null scheme in "
                        + "WebViewFallbackActivity: " + url);
                return true;
            }
            String normalizedScheme = scheme.toLowerCase(Locale.ROOT);

            // URIs with the `data` scheme are handled in the WebView.
            // The "Demo" item in https://jakearchibald.github.io/svgomg/ is one example of this
            // usage
            if ("data".equals(normalizedScheme)) {
                return false;
            }

            if ("about".equals(normalizedScheme)) {
                String ssp = url.getSchemeSpecificPart();
                if ("blank".equals(ssp) || "srcdoc".equals(ssp)) {
                    return false;
                }
                Log.w(TAG, "Blocked navigation to disallowed about: URI in "
                        + "WebViewFallbackActivity: " + url);
                return true;
            }

            if ("blob".equals(normalizedScheme)) {
                String ssp = url.getSchemeSpecificPart();
                Uri inner = ssp != null ? Uri.parse(ssp) : null;
                if (inner != null && isTrustedOrigin(inner)) {
                    return false;
                }
                Log.w(TAG, "Blocked navigation to untrusted blob: URI in "
                        + "WebViewFallbackActivity: " + url);
                return true;
            }

            // Trusted https origins stay inside the WebView.
            if (isTrustedOrigin(url)) {
                return false;
            }

            // Untrusted http/https navigations are handed to an external Custom Tab from the
            // main frame only.
            if ("https".equals(normalizedScheme) || "http".equals(normalizedScheme)) {
                if (!isMainFrame) {
                    return true;
                }
                try {
                    CustomTabsIntent intent = new CustomTabsIntent.Builder()
                            .setToolbarColor(mStatusBarColor)
                            .build();
                    intent.launchUrl(WebViewFallbackActivity.this, url);
                } catch (ActivityNotFoundException | SecurityException ex) {
                    Log.e(TAG, String.format(
                            "Failed to launch external browser for '%s'", url), ex);
                }
                return true;
            }

            if ("file".equals(normalizedScheme)
                    || "content".equals(normalizedScheme)
                    || "javascript".equals(normalizedScheme)
                    || "intent".equals(normalizedScheme)) {
                Log.w(TAG, "Blocked navigation to disallowed scheme in WebViewFallbackActivity: "
                        + scheme);
                return true;
            }

            if (!isMainFrame) {
                return true;
            }
            try {
                Intent i = new Intent(Intent.ACTION_VIEW, url);
                i.addCategory(Intent.CATEGORY_BROWSABLE);
                startActivity(i);
            } catch (ActivityNotFoundException | SecurityException ex) {
                Log.e(TAG, String.format(
                        "Failed to launch external activity for '%s'", url), ex);
            }
            return true;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url == null) {
                return true;
            }
            return this.shouldOverrideUrlLoading(Uri.parse(url), true);
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            if (request == null || request.getUrl() == null) {
                return true;
            }
            return this.shouldOverrideUrlLoading(request.getUrl(), request.isForMainFrame());
        }

        protected boolean isTrustedOrigin(@Nullable Uri uri) {
            if (uri == null || !"https".equalsIgnoreCase(uri.getScheme())) {
                return false;
            }
            return uriOriginsMatch(uri, WebViewFallbackActivity.this.mLaunchUrl)
                    || matchExtraOrigins(uri);
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
            if (uriA == null || uriB == null
                    || uriA.getHost() == null || uriB.getHost() == null) {
                return false;
            }
            return Utils.isSameOrigin(uriA, uriB);
        }
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

                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                getWindow().addContentView(this.fullScreenView,
                        new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
            }

            @Override
            public void onHideCustomView() {
                // If we don't have a fullscreen then no-op.
                if (fullScreenView == null) {
                    return;
                }

                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                ((ViewGroup) fullScreenView.getParent()).removeView(fullScreenView);
                this.fullScreenView = null;
                setRequestedOrientation(this.originalOrientation);
            }
        };
    }

    @SuppressLint("SetJavaScriptEnabled")
    @SuppressWarnings("deprecation")
    protected void setupWebSettings(@NonNull WebSettings webSettings) {
        // Those settings are disabled by default.
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAllowFileAccess(false);
        webSettings.setAllowContentAccess(false);
        webSettings.setAllowFileAccessFromFileURLs(false);
        webSettings.setAllowUniversalAccessFromFileURLs(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            webSettings.setMediaPlaybackRequiresUserGesture(false);
        }
    }
}
