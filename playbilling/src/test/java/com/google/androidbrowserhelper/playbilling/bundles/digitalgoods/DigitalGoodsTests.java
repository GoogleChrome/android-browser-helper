package com.google.androidbrowserhelper.playbilling.bundles.digitalgoods;

import android.os.Bundle;

import com.google.androidbrowserhelper.playbilling.digitalgoods.DigitalGoodsRequestHandler;
import com.google.androidbrowserhelper.playbilling.digitalgoods.GetDetailsCall;
import com.google.androidbrowserhelper.playbilling.provider.BillingWrapperFactory;
import com.google.androidbrowserhelper.playbilling.provider.MockBillingWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(manifest = Config.NONE)
public class DigitalGoodsTests {
    private final static DigitalGoodsRequestHandler.Callback EMPTY_CALLBACK = (name, args) -> {};

    private final MockBillingWrapper mBillingWrapper = new MockBillingWrapper();
    private DigitalGoodsRequestHandler mHandler;

    @Before
    public void setUp() {
        BillingWrapperFactory.setBillingWrapperForTesting(mBillingWrapper);
        mHandler = new DigitalGoodsRequestHandler(null);
    }

    @Test
    public void getDetailsCall_unknownCommand() {
        assertFalse(mHandler.handle("unknown", new Bundle(), EMPTY_CALLBACK));
    }

    @Test
    public void getDetailsCall_wrongArgs() {
        assertFalse(mHandler.handle(GetDetailsCall.COMMAND_NAME, new Bundle(), EMPTY_CALLBACK));
    }

    @Test
    public void getDetailsCall() {
        String commandName = GetDetailsCall.COMMAND_NAME;
        Bundle args = GetDetailsCall.createBundleForTesting("id1");
        DigitalGoodsRequestHandler.Callback callback = (name, bundle) -> {};

        assertTrue(mHandler.handle(commandName, args, callback));
    }
}
