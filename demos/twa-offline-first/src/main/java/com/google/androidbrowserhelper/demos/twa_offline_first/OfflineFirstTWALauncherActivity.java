package com.google.androidbrowserhelper.demos.twa_offline_first;

import com.google.androidbrowserhelper.trusted.LauncherActivity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class OfflineFirstTWALauncherActivity extends LauncherActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.displayScreen();
    }

    @Override
    protected boolean shouldLaunchImmediately() {
        return false;
    }

    private void displayScreen() {
        if(isOnline()) {
            launchTwa();
        } else {
            launchCustomOfflineScreen();
        }
    }

    private void launchCustomOfflineScreen() {
        setContentView(R.layout.activity_offline_first_twa);

        final Button retryBtn = this.findViewById(R.id.retry_btn);
        retryBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                displayScreen();
            }
        });
    }

    private boolean isOnline() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}