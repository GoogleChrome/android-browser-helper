package com.google.androidbrowserhelper.playbilling.provider;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import com.google.androidbrowserhelper.playbilling.IIsReadyToPayService;
import com.google.androidbrowserhelper.playbilling.IIsReadyToPayServiceCallback;
import com.google.androidbrowserhelper.trusted.SharedPreferencesTokenStore;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ServiceTestRule;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for {@link IsReadyToPayService}.
 *
 */
@RunWith(AndroidJUnit4.class)
public class IsReadyToPayServiceTest {
    @Rule
    public ServiceTestRule mServiceTestRule = new ServiceTestRule();

    private Context mContext;
    private IIsReadyToPayService mService;
    private SharedPreferencesTokenStore mTokenStore;

    private CountDownLatch mServiceIsReadyToPayLatch = new CountDownLatch(1);
    private CountDownLatch mServiceIsNotReadyToPayLatch = new CountDownLatch(1);
    private IIsReadyToPayServiceCallback mServiceCallback =
            new IIsReadyToPayServiceCallback.Stub() {
        @Override
        public void handleIsReadyToPay(boolean isReadyToPay) throws RemoteException {
            if (isReadyToPay) {
                mServiceIsReadyToPayLatch.countDown();
            } else {
                mServiceIsNotReadyToPayLatch.countDown();
            }
        }
    };

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mTokenStore = new SharedPreferencesTokenStore(mContext);

        Intent intent = new Intent();
        intent.setClass(mContext, IsReadyToPayService.class);

        try {
            mService = IIsReadyToPayService.Stub.asInterface(mServiceTestRule.bindService(intent));
        } catch (TimeoutException e) {
            fail();
        }
    }

    @After
    public void tearDown() {
        mServiceTestRule.unbindService();
        mTokenStore.store(null);
    }

    @Test
    public void isReadyToPayForVerifiedProvider() throws RemoteException, InterruptedException {
        mTokenStore.setVerifiedProvider(mContext.getPackageName(), mContext.getPackageManager());

        mService.isReadyToPay(mServiceCallback);
        assertCalled(mServiceIsReadyToPayLatch);
    }

    @Test
    public void isNotReadyToPayForUnverifiedProvider() throws RemoteException, InterruptedException {
        mTokenStore.store(null);

        mService.isReadyToPay(mServiceCallback);
        assertCalled(mServiceIsNotReadyToPayLatch);
    }

    private static void assertCalled(CountDownLatch latch) throws InterruptedException {
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
