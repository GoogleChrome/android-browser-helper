package com.google.androidbrowserhelper.demos.twapostmessage;

import android.content.ComponentName;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;
import com.google.androidbrowserhelper.trusted.LauncherActivity;

public class MainActivity extends LauncherActivity {

  private CustomTabsClient mClient;
  private CustomTabsSession mSession;

  private Uri URL = Uri.parse("https://peconn.github.io/starters");

  private Uri SOURCE_ORIGIN = Uri.parse("my-app-origin-uri");
  private Uri TARGET_ORIGIN = Uri.parse("https://peconn.github.io");
  private boolean mValidated = false;

  private final String TAG = "TWA/CCT-PostMessageDemo";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    //No need to as for permission as the compileSDK is 31.
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

        // Listens for any navigation happens, we wait until the navigation finishes then we
        // request post message channel using
        // CustomTabsSession#requestPostMessageChannel(sourceUri, targetUri, extrasBundle)
        @Override
        public void onNavigationEvent(int navigationEvent, @Nullable Bundle extras) {
          if (navigationEvent != NAVIGATION_FINISHED) {
            return;
          }

          if (!mValidated) {
            Log.d(TAG, "Not starting PostMessage as validation didn't succeed.");
          }

          // If this fails:
          // - Have you included PostMessageService in your AndroidManifest.xml ?
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
    Log.d(TAG, "Binding to $packageName.");
    CustomTabsClient.bindCustomTabsService(this, packageName, new CustomTabsServiceConnection() {
      @Override
      public void onCustomTabsServiceConnected(@NonNull ComponentName name,
          @NonNull CustomTabsClient client) {
        mClient = client;

        // Note: validateRelationship requires warmup to have been called.
        client.warmup(0L);

        mSession = mClient.newSession(customTabsCallback);
        registerBroadcastReceiver();
      }

      @Override
      public void onServiceDisconnected(ComponentName componentName) {
        mClient = null;

      }
    });
  }

  private void registerBroadcastReceiver() {
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(PostMessageBroadcastReceiver.POST_MESSAGE_ACTION);
    registerReceiver(new PostMessageBroadcastReceiver(mSession), intentFilter);
  }
}