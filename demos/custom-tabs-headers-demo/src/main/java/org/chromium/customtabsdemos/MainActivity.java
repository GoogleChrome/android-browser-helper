package org.chromium.customtabsdemos;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsService;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;
import android.content.ComponentName;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Browser;

public class MainActivity extends AppCompatActivity {

    private CustomTabsSession mSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String url = "https://padr31.github.io/";

        findViewById(R.id.btnExtra).setOnClickListener(e -> {

            // Set up a callback that launches the cross origin intent after session validated.
            CustomTabsCallback callback = new CustomTabsCallback() {
                @Override
                public void onRelationshipValidationResult(int relation, @NonNull Uri requestedOrigin, boolean result, @Nullable Bundle extras) {
                    super.onRelationshipValidationResult(relation, requestedOrigin, result, extras);
                    // Launch custom tabs intent after session was validated as the same origin.
                    CustomTabsIntent intent = constructExtraHeadersIntent(mSession);
                    intent.launchUrl(MainActivity.this, Uri.parse(url));
                }
            };

            // Set up a connection that warms up and validates a session.
            CustomTabsServiceConnection connection = new CustomTabsServiceConnection() {
                @Override
                public void onCustomTabsServiceConnected(@NonNull ComponentName name, @NonNull CustomTabsClient client) {
                    // Create session after service connected.
                    mSession = client.newSession(callback);
                    client.warmup(0);
                    // Validate the session as the same origin to allow cross origin headers.
                    mSession.validateRelationship(CustomTabsService.RELATION_USE_AS_ORIGIN, Uri.parse(url), null);
                }
                @Override
                public void onServiceDisconnected(ComponentName componentName) { }
            };

            // Bind the custom tabs service connection.
            CustomTabsClient.bindCustomTabsService(this, CustomTabsClient.getPackageName(MainActivity.this, null), connection);
        });
    }

    private CustomTabsIntent constructExtraHeadersIntent(CustomTabsSession session) {
        CustomTabsIntent intent = new CustomTabsIntent.Builder(session).build();

        // Example non-cors-whitelisted headers.
        Bundle headers = new Bundle();
        headers.putString("bearer-token", "Some token");
        headers.putString("redirect-url", "Some redirect url");
        intent.intent.putExtra(Browser.EXTRA_HEADERS, headers);

        return intent;
    }
}