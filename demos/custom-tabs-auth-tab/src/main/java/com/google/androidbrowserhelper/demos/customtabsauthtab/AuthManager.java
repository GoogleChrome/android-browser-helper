package com.google.androidbrowserhelper.demos.customtabsauthtab;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.browser.auth.AuthTabIntent;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;

import java.io.IOException;
import java.util.UUID;

/**
 * This class helps managing an authentication flow. It was created with the goal of demonstrating
 * how to use Custom Tabs Auth Tab to handle auth and is not meant as a complete implementation
 * of the OAuth protocol. We recommend checking out https://github.com/openid/AppAuth-Android for
 * a comprehensive implementation of the OAuth protocol.
 */

public class AuthManager {
    private static final String TAG = "OAuthManager";

    private final String mClientId;
    private final String mClientSecret;
    private final String mAuthorizationEndpoint;
    private final String mRedirectScheme;

    public interface OAuthCallback {
        void auth(String accessToken, String scope, String tokenType);
    }

    public AuthManager(String clientId, String clientSecret, String authorizationEndpoint,
                       String redirectScheme) {
        mClientId = clientId;
        mClientSecret = clientSecret;
        mAuthorizationEndpoint = authorizationEndpoint;
        mRedirectScheme = redirectScheme;
    }

    public void authorize(Context context, ActivityResultLauncher<Intent> launcher, String scope) {
        // Generate a random state.
        String state = UUID.randomUUID().toString();

        // Save the state so we can verify later.
        SharedPreferences preferences =
                context.getSharedPreferences("OAUTH_STORAGE", Context.MODE_PRIVATE);
        preferences.edit()
                .putString("OAUTH_STATE", state)
                .apply();

        // Create an authorization URI to the OAuth Endpoint.
        Uri uri = Uri.parse(mAuthorizationEndpoint)
                .buildUpon()
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("client_id", mClientId)
                .appendQueryParameter("scope", scope)
                .appendQueryParameter("state", state)
                .build();

        String packageName = CustomTabsClient.getPackageName(context, null);
        if (packageName == null) {
            Toast.makeText(context, "Can't find a Custom Tabs provider.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Open the Authorization URI in an Auth Tab if supported by the default browser.
        if (CustomTabsClient.isAuthTabSupported(context, packageName)) {
            AuthTabIntent authTabIntent = new AuthTabIntent.Builder().build();
            authTabIntent.launch(launcher, uri, mRedirectScheme);
        } else {
            // Fall back to a Custom Tab.
            CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
            customTabsIntent.intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
            customTabsIntent.launchUrl(context, uri);
        }
    }

    public void continueAuthFlow(@NonNull Context context, Uri uri, @NonNull OAuthCallback callback) {
        String code = uri.getQueryParameter("code");
        SharedPreferences preferences =
                context.getSharedPreferences("OAUTH_STORAGE", Context.MODE_PRIVATE);
        String state = preferences.getString("OAUTH_STATE", "");
        Uri tokenUri = Uri.parse("https://github.com/login/oauth/access_token")
                .buildUpon()
                .appendQueryParameter("client_id", mClientId)
                .appendQueryParameter("client_secret", mClientSecret)
                .appendQueryParameter("code", code)
                .appendQueryParameter("state", state)
                .build();

        // Run the network request off the UI thread.
        new Thread(() -> {
            try {
                String response = Utils.fetch(tokenUri);
                // The response is a query-string. We concatenate with a valid domain to be
                // able to easily parse and extract values.
                Uri responseUri = Uri.parse("http://example.com?" + response);
                String accessToken = responseUri.getQueryParameter("access_token");
                String tokenType = responseUri.getQueryParameter("token_type");
                String scope = responseUri.getQueryParameter("scope");

                // Invoke the callback in the main thread.
                new Handler(Looper.getMainLooper()).post(
                        () -> callback.auth(accessToken, scope, tokenType));

            } catch (IOException e) {
                Log.e(TAG, "Error requesting access token: " + e.getMessage());
            }
        }).start();
    }
}
