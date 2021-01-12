package com.google.androidbrowserhelper.demos.playbilling;

import android.util.Log;

import com.google.androidbrowserhelper.playbilling.digitalgoods.DigitalGoodsRequestHandler;
import com.google.androidbrowserhelper.trusted.DelegationService;

public class ExtraFeaturesService extends DelegationService {
    @Override
    public void onCreate() {
        super.onCreate();

        Log.d("DGDebug", "Creating Service.");

        registerExtraCommandHandler(new DigitalGoodsRequestHandler(getApplicationContext()));
    }
}
