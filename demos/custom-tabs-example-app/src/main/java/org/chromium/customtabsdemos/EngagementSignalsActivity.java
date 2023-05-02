// Copyright 2023 Google Inc. All Rights Reserved.
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

import static androidx.browser.customtabs.CustomTabsIntent.ACTIVITY_HEIGHT_ADJUSTABLE;

import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.browser.customtabs.EngagementSignalsCallback;

public class EngagementSignalsActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "EngagementSignals";
    private static final int INITIAL_HEIGHT_DEFAULT_PX = 600;

    private EditText mUrlEditText;
    private TextView mTextVerticalScroll;
    private TextView mTextGreatestPercentage;
    private TextView mTextSessionEnd;
    private TextView mTextNavigation;

    @Nullable
    private ServiceConnection mConnection;
    @Nullable
    private CustomTabsClient mClient;
    @Nullable
    private CustomTabsSession mCustomTabsSession;

    private ServiceConnectionCallback mServiceConnectionCallback = new ServiceConnectionCallback() {
        @Override
        public void onServiceConnected(CustomTabsClient client) {
            mClient = client;
            mCustomTabsSession = mClient.newSession(mCustomTabsCallback);
            try {
                boolean engagementSignalsApiAvailable = mCustomTabsSession.isEngagementSignalsApiAvailable(Bundle.EMPTY);
                if (!engagementSignalsApiAvailable) {
                    Log.d(TAG, "CustomTab Engagement signals not available, make sure to use the " +
                            "latest Chrome version");
                    return;
                }
                boolean signalsCallback = mCustomTabsSession.setEngagementSignalsCallback(mEngagementSignalsCallback, Bundle.EMPTY);
                if (!signalsCallback) {
                    Log.w(TAG, "Could not set EngagementSignalsCallback");
                }
            } catch (RemoteException e) {
                Log.w(TAG, "The Service died while responding to the request.", e);
            } catch (UnsupportedOperationException e) {
                Log.w(TAG, "Engagement Signals API isn't supported by the browser.", e);
            }
        }

        @Override
        public void onServiceDisconnected() {
            mClient = null;
            mConnection = null;
            mCustomTabsSession = null;
        }
    };

    private EngagementSignalsCallback mEngagementSignalsCallback = new EngagementSignalsCallback() {
        @Override
        public void onVerticalScrollEvent(boolean isDirectionUp, @NonNull Bundle extras) {
            Log.d(TAG, "onVerticalScrollEvent (isDirectionUp=" + isDirectionUp + ')');
            mTextVerticalScroll.setText("vertical scroll " + (isDirectionUp ? "UP️" : "DOWN"));
        }

        @Override
        public void onGreatestScrollPercentageIncreased(int scrollPercentage, @NonNull Bundle extras) {
            Log.d(TAG, "scroll percentage: " + scrollPercentage + "%");
            mTextGreatestPercentage.setText("scroll percentage: " + scrollPercentage + "%");
        }

        @Override
        public void onSessionEnded(boolean didUserInteract, @NonNull Bundle extras) {
            Log.d(TAG, "onSessionEnded (didUserInteract=" + didUserInteract + ')');
            mTextSessionEnd.setText(didUserInteract ? "session ended with user interaction" : "session ended without user interaction");
        }
    };

    private CustomTabsCallback mCustomTabsCallback = new CustomTabsCallback() {
        @Override
        public void onNavigationEvent(int navigationEvent, @Nullable Bundle extras) {
            String event;
            switch (navigationEvent) {
                case CustomTabsCallback.NAVIGATION_ABORTED:
                    event = "NAVIGATION_ABORTED";
                    break;
                case CustomTabsCallback.NAVIGATION_FAILED:
                    event = "NAVIGATION_FAILED";
                    break;
                case CustomTabsCallback.NAVIGATION_FINISHED:
                    event = "NAVIGATION_FINISHED";
                    break;
                case CustomTabsCallback.NAVIGATION_STARTED:
                    event = "NAVIGATION_STARTED";
                    // Scroll percentage and direction should be reset
                    mTextVerticalScroll.setText("vertical scroll: n/a");
                    mTextGreatestPercentage.setText("scroll percentage: n/a");
                    break;
                case CustomTabsCallback.TAB_SHOWN:
                    event = "TAB_SHOWN";
                    break;
                case CustomTabsCallback.TAB_HIDDEN:
                    event = "TAB_HIDDEN";
                    break;
                default:
                    event = String.valueOf(navigationEvent);
            }
            Log.d(TAG, "onNavigationEvent (navigationEvent=" + event + ')');
            mTextNavigation.setText("onNavigationEvent " + event);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_engagement_signals);
        findViewById(R.id.start_custom_tab).setOnClickListener(this);

        mUrlEditText = findViewById(R.id.url);
        mTextGreatestPercentage = findViewById(R.id.label_event_greatest_percentage);
        mTextNavigation = findViewById(R.id.label_event_navigation);
        mTextSessionEnd = findViewById(R.id.label_event_session_ended);
        mTextVerticalScroll = findViewById(R.id.label_event_vertical_scroll);
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindCustomTabsService();
    }

    private void bindCustomTabsService() {
        String packageName = CustomTabsHelper.getPackageNameToUse(this);
        if (packageName == null) {
            Log.w(TAG, packageName + " does not support a Custom Tab Service connection");
            return;
        }
        mConnection = new ServiceConnection(mServiceConnectionCallback);
        CustomTabsClient.bindCustomTabsService(this, packageName, mConnection);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindCustomTabsService();
    }

    private void unbindCustomTabsService() {
        if (mConnection == null) return;
        unbindService(mConnection);
        mClient = null;
        mCustomTabsSession = null;
        mConnection = null;
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        switch (viewId) {
            case R.id.start_custom_tab:
                openCustomTab();
                break;
            default:
                // Unknown View Clicked
        }
    }

    private void openCustomTab() {
        String url = mUrlEditText.getText().toString();
        CustomTabsIntent.Builder intentBuilder = new CustomTabsIntent.Builder(mCustomTabsSession);
        intentBuilder.setInitialActivityHeightPx(INITIAL_HEIGHT_DEFAULT_PX);
        CustomTabsIntent customTabsIntent = intentBuilder.build();
        customTabsIntent.launchUrl(this, Uri.parse(url));
    }

}
