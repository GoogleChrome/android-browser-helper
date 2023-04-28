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
    private static final String TAG = EngagementSignalsActivity.class.getSimpleName();
    private static final int INITIAL_HEIGHT_DEFAULT_PX = 600;

    private EditText mUrlEditText;
    private TextView mTextVerticalScroll;
    private TextView mTextGreatestPercentage;
    private TextView mTextSessionEnd;
    private TextView mTextNavigation;

    private ServiceConnection mConnection;
    private CustomTabsClient mClient;
    private CustomTabsSession mCustomTabsSession;

    private final ServiceConnectionCallback mServiceConnectionCallback = new ServiceConnectionCallback() {
        @Override
        public void onServiceConnected(CustomTabsClient client) {
            mClient = client;
            mCustomTabsSession = mClient.newSession(mCustomTabsCallback);
            try {
                boolean engagementSignalsApiAvailable = mCustomTabsSession.isEngagementSignalsApiAvailable(Bundle.EMPTY);
                if (!engagementSignalsApiAvailable) {
                    Log.d(TAG, "CustomTab Engagement signals not available, make sure to use the " +
                            "latest Chrome version and enable via chrome://flags/#cct-real-time-engagement-signals");
                    return;
                }
                mCustomTabsSession.setEngagementSignalsCallback(mEngagementSignalsCallback, Bundle.EMPTY);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed registering engagement signals callback", e);
                return;
            }
        }

        @Override
        public void onServiceDisconnected() {
            mClient = null;
            mConnection = null;
            mCustomTabsSession = null;
        }
    };

    private final EngagementSignalsCallback mEngagementSignalsCallback = new EngagementSignalsCallback() {
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

    private final CustomTabsCallback mCustomTabsCallback = new CustomTabsCallback() {
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
        if (packageName == null) return;
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
        startActivityForResult(customTabsIntent.intent, 1);
    }

}