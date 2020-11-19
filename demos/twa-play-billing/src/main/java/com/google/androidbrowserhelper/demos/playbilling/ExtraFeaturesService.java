package com.google.androidbrowserhelper.demos.playbilling;

import android.content.pm.PackageManager;
import android.util.Log;

import com.google.androidbrowserhelper.playbilling.digitalgoods.DigitalGoodsRequestHandler;
import com.google.androidbrowserhelper.trusted.ChromeOsSupport;
import com.google.androidbrowserhelper.trusted.DelegationService;
import com.google.androidbrowserhelper.trusted.SharedPreferencesTokenStore;

import androidx.browser.trusted.Token;
import androidx.browser.trusted.TokenStore;

public class ExtraFeaturesService extends DelegationService {
    @Override
    public void onCreate() {
        super.onCreate();

        Log.d("ChrOS", "Service created.");

        checkTokens();

        registerExtraCommandHandler(new DigitalGoodsRequestHandler(getApplicationContext()));
    }

    private void checkTokens() {
        PackageManager pm = getPackageManager();
        if (!ChromeOsSupport.isRunningOnArc(pm)) return;

        Log.d("ChrOS", "Service running on ARC++.");

        TokenStore store = new SharedPreferencesTokenStore(this);
        Token token = store.load();

        if (token == null) {
            Log.d("ChrOS", "No token found.");
        } else if (token.matches(ChromeOsSupport.ARC_PAYMENT_APP, pm)) {
            Log.d("ChrOS", "Token check passed.");
            return;
        } else {
            Log.d("ChrOS", "Token check failed.");
        }

        Log.d("ChrOS", "Overwriting token to hackily force things to work.");
        Token newToken = Token.create(ChromeOsSupport.ARC_PAYMENT_APP, pm);

        if (newToken == null) {
            Log.d("ChrOS", "Could not create an ARC++ token (for some reason).");
        } else {
            store.store(newToken);
            Log.d("ChrOS", "Token overwritten.");
        }

    }
}
