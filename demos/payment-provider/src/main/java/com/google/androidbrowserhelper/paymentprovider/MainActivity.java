package com.google.androidbrowserhelper.paymentprovider;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;

import android.net.Uri;
import android.os.Bundle;

import com.google.androidbrowserhelper.trusted.SharedPreferencesTokenStore;

public class MainActivity extends AppCompatActivity {
    private static final Uri WEB_STORE = Uri.parse("https://beer.conn.dev");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String providerName = CustomTabsClient.getPackageName(this, null);
        SharedPreferencesTokenStore store = new SharedPreferencesTokenStore(this);

        // TODO: Add a button to clear the verified provider.

        findViewById(R.id.launch_browser).setOnClickListener(view -> {
            CustomTabsIntent intent = new CustomTabsIntent.Builder().build();

            intent.intent.setPackage(providerName);
            store.setVerifiedProvider(providerName, getPackageManager());

            intent.launchUrl(this, WEB_STORE);
        });
    }
}
