package com.google.androidbrowserhelper.paymentprovider;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

import android.net.Uri;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
    private static final Uri WEB_STORE = Uri.parse("https://beer.conn.dev");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.launch_browser).setOnClickListener(view ->
                new CustomTabsIntent.Builder().build().launchUrl(this, WEB_STORE));
    }
}
