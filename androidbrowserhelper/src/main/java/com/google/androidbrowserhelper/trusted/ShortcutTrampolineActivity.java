// Copyright 2026 Google Inc. All Rights Reserved.
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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.browser.trusted.TrustedWebActivityIntent;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.TrustedWebUtils;
import androidx.browser.trusted.TrustedWebActivityIntentBuilder;

/**
 * A trampoline activity that handles Trusted Web Activity shortcuts.
 * It is defined as a noDisplay activity, meaning it finishes in onCreate()
 * before any layout is drawn.
 */
public class ShortcutTrampolineActivity extends Activity {
    private static final String TAG = "ShortcutTrampoline";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Intent intent = getIntent();
            if (intent == null) {
                return;
            }

            Uri uri = intent.getData();
            if (uri == null) {
                return;
            }
            // Re-parse URI to prevent custom Parcelable Uri spoofing.
            uri = Uri.parse(uri.toString());

            LauncherActivityMetadata metadata = LauncherActivityMetadata.parse(this);
            if (!isTrusted(uri, metadata)) {
                Log.w(TAG, "Dropping untrusted shortcut URI: " + uri);
                return;
            }

            // Using getApplicationContext() is critical here because this Activity is going to
            // finish immediately, while the TwaLauncher will do asynchronous work (connecting
            // to Custom Tabs Service) and eventually launch the TWA.
            Context appContext = getApplicationContext();
            TwaLauncher twaLauncher = new TwaLauncher(appContext, metadata.launchingBrowser) {
                @Override
                protected TrustedWebActivityIntent onPrepareIntent(TrustedWebActivityIntent intent) {
                    intent.getIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    return super.onPrepareIntent(intent);
                }
            };

            TrustedWebActivityIntentBuilder builder = new TrustedWebActivityIntentBuilder(uri);
            metadata.configureIntentBuilder(builder, appContext);

            twaLauncher.launch(
                    builder,
                    new QualityEnforcer(),
                    null /* splashScreenStrategy */,
                    () -> new Handler(Looper.getMainLooper()).post(twaLauncher::destroy),
                    new TwaLauncher.FallbackStrategy() {
                        @Override
                        public void launch(Context context, TrustedWebActivityIntentBuilder twaBuilder,
                                           @Nullable String providerPackage, @Nullable Runnable completionCallback) {
                            // Respect the metadata specified in the manifest instead of fallback.
                            if (metadata.launchingBrowser != null) {
                                Log.w(TAG, "Launching browser " + metadata.launchingBrowser + " is not available.");
                                if(completionCallback != null) {
                                    completionCallback.run();
                                }
                                return;
                            }

                            if ("webview".equalsIgnoreCase(metadata.fallbackStrategyType)) {
                                Intent fallbackIntent = WebViewFallbackActivity.createLaunchIntent(context,
                                        twaBuilder.getUri(), metadata);
                                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                try {
                                    context.startActivity(fallbackIntent);
                                } catch (ActivityNotFoundException e) {
                                    Log.e(TAG, "Failed to launch webview fallback: ", e);
                                }
                            } else {
                                // CustomTabs fallback
                                if (providerPackage == null) {
                                    providerPackage = CustomTabsClient.getPackageName(context, null);
                                }
                                CustomTabsIntent customTabsIntent = twaBuilder.buildCustomTabsIntent();
                                customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                if (providerPackage != null) {
                                    customTabsIntent.intent.setPackage(providerPackage);
                                }
                                if (ChromeOsSupport.isRunningOnArc(context.getPackageManager())) {
                                    customTabsIntent.intent.putExtra(TrustedWebUtils.EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY, true);
                                }
                                // Verify there is an app available to handle the intent before launching
                                customTabsIntent.intent.setData(twaBuilder.getUri());
                                if (customTabsIntent.intent.resolveActivity(context.getPackageManager()) != null) {
                                    context.startActivity(customTabsIntent.intent);
                                } else {
                                    Log.e(TAG, "No browser installed to handle Custom Tabs/Browser fallback.");
                                }
                            }
                            if (completionCallback != null) {
                                completionCallback.run();
                            }
                        }
                    }
            );

        } finally {
            // Must finish synchronously in onCreate() to satisfy android:noDisplay="true"
            finish();
        }
    }

    private static boolean isTrusted(Uri uri, LauncherActivityMetadata metadata) {
        if (uri == null) {
            return false;
        }
        if (metadata.defaultUrl != null) {
            Uri defaultUri = Uri.parse(metadata.defaultUrl);
            if (isSameOrigin(uri, defaultUri)) {
                return true;
            }
        }
        if (metadata.additionalTrustedOrigins != null) {
            for (String originStr : metadata.additionalTrustedOrigins) {
                Uri originUri = Uri.parse(originStr);
                if (isSameOrigin(uri, originUri)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isSameOrigin(Uri uri1, Uri uri2) {
        if (uri1 == null || uri2 == null) {
            return false;
        }
        String scheme1 = uri1.getScheme();
        String scheme2 = uri2.getScheme();
        String host1 = uri1.getHost();
        String host2 = uri2.getHost();
        if (scheme1 == null || scheme2 == null || host1 == null || host2 == null) {
            return false;
        }

        int port1 = uri1.getPort();
        int port2 = uri2.getPort();
        if (port1 == -1) {
            port1 = "https".equalsIgnoreCase(scheme1) ? 443 : ("http".equalsIgnoreCase(scheme1) ? 80 : -1);
        }
        if (port2 == -1) {
            port2 = "https".equalsIgnoreCase(scheme2) ? 443 : ("http".equalsIgnoreCase(scheme2) ? 80 : -1);
        }

        return scheme1.equalsIgnoreCase(scheme2) &&
                host1.equalsIgnoreCase(host2) &&
                port1 == port2;
    }
}
