package com.google.androidbrowserhelper.demos.twapostmessage;
/*
 *    Copyright 2023 Google LLC
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

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.browser.trusted.TrustedWebActivityIntentBuilder;
import androidx.core.content.ContextCompat;

/**
 * A demo to showcase how to use the postMessage in CCT/TWA. Please note that the initialization has
 * to be from the app, the steps will be as following:
 *
 * 1. Bind CustomTabsService and wait for the CustomTabsClient to be ready. 2. Use this Client to
 * warmup and to create a new CustomTabsSession. 3. Listen for navigation events from
 * CustomTabsCallback#onNavigationEvent. 4. Upon receiving NAVIGATION_FINISHED, we request a new
 * PostMessageChannel from CustomTabsSession#requestPostMessageChannel. 5. When the channel is ready
 * we can initialize the communication by posting the first message using
 * CustomTabsSession#postMessage.
 *
 * Please note that requesting post message channel validates the relationship between your origin
 * and the application using CustomTabsSession#validateRelationship, with the relation as
 * Relation#RELATION_USE_AS_ORIGIN, please read the methods doc for more details.
 *
 * Validation with the origin doesn't necessarily mean that communication is exclusive to this
 * origin, but the DAL validation is required to provide MessageEvent.origin field.
 */
public class MainActivity extends AppCompatActivity {

    private CustomTabsClient mClient;
    private CustomTabsSession mSession;
    private Uri URL = Uri.parse("https://peconn.github.io/starters");

    // This origin is going to be validated via DAL, please see
    // (https://developer.chrome.com/docs/android/post-message-twa#add_the_app_to_web_validation),
    // it has to either start with http or https.
    private Uri SOURCE_ORIGIN = Uri.parse("https://sayedelabady.github.io/");
    private Uri TARGET_ORIGIN = Uri.parse("https://peconn.github.io");
    private boolean mValidated = false;

    private final String TAG = "PostMessageDemo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // No need to ask for permission as the compileSDK is 31.
        MessageNotificationHandler.createNotificationChannelIfNeeded(this);

        bindCustomTabsService();

    }

    private final CustomTabsCallback customTabsCallback =
        new CustomTabsCallback() {
            @Override
            public void onPostMessage(@NonNull String message, @Nullable Bundle extras) {
                super.onPostMessage(message, extras);
                if (message.contains("ACK")) {
                    return;
                }
                MessageNotificationHandler.showNotificationWithMessage(MainActivity.this, message);
                Log.d(TAG, "Got message: " + message);
            }

            @Override
            public void onRelationshipValidationResult(int relation, @NonNull Uri requestedOrigin,
                boolean result, @Nullable Bundle extras) {
                // If this fails:
                // - Have you called warmup?
                // - Have you set up Digital Asset Links correctly?
                // - Double check what browser you're using.
                Log.d(TAG, "Relationship result: " + result);
                mValidated = result;
            }

            // Listens for navigation, requests the postMessage channel when one completes.
            @Override
            public void onNavigationEvent(int navigationEvent, @Nullable Bundle extras) {
                if (navigationEvent != NAVIGATION_FINISHED) {
                    return;
                }

                if (!mValidated) {
                    Log.d(TAG, "Not starting PostMessage as validation didn't succeed.");
                }

                // If this fails:
                // - Have you included PostMessageService in your AndroidManifest.xml?
                boolean result = mSession.requestPostMessageChannel(SOURCE_ORIGIN, TARGET_ORIGIN,
                    new Bundle());
                Log.d(TAG, "Requested Post Message Channel: " + result);
            }

            @Override
            public void onMessageChannelReady(@Nullable Bundle extras) {
                Log.d(TAG, "Message channel ready.");

                int result = mSession.postMessage("First message", null);
                Log.d(TAG, "postMessage returned: " + result);
            }
        };


    private void bindCustomTabsService() {
        String packageName = CustomTabsClient.getPackageName(this, null);
        Toast.makeText(this, "Binding to " + packageName, Toast.LENGTH_SHORT).show();
        CustomTabsClient.bindCustomTabsService(this, packageName,
            new CustomTabsServiceConnection() {
                @Override
                public void onCustomTabsServiceConnected(@NonNull ComponentName name,
                    @NonNull CustomTabsClient client) {
                    mClient = client;

                    // Note: validateRelationship requires warmup to have been called.
                    client.warmup(0L);

                    mSession = mClient.newSession(customTabsCallback);

                    launch();
                    registerBroadcastReceiver();
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {
                    mClient = null;

                }
            });
    }


    // The demo should work for both CCT and TWA but here we are using TWA.
    private void launch() {
        new TrustedWebActivityIntentBuilder(URL).build(mSession)
            .launchTrustedWebActivity(MainActivity.this);
    }

    @SuppressLint("WrongConstant")
    private void registerBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PostMessageBroadcastReceiver.POST_MESSAGE_ACTION);
        ContextCompat.registerReceiver(this, new PostMessageBroadcastReceiver(mSession),
            intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }
}