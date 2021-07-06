// Copyright 2020 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.androidbrowserhelper.playbilling.digitalgoods;

import android.os.Build;

import com.google.androidbrowserhelper.playbilling.provider.BillingWrapper;
import com.google.androidbrowserhelper.playbilling.provider.MockBillingWrapper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(sdk = {Build.VERSION_CODES.O_MR1})
public class ConnectedBillingWrapperTest {
    private final MockBillingWrapper mInnerBillingWrapper = new MockBillingWrapper();
    private final BillingWrapper mConnectedWrapper =
            new ConnectedBillingWrapper(mInnerBillingWrapper);

    @Test
    public void delays_getDetailsCall() {
        mConnectedWrapper.querySkuDetails("type", Collections.singletonList("id1"), null);
        assertNull(mInnerBillingWrapper.getQueriedSkuDetails());

        mInnerBillingWrapper.triggerConnected();
        assertEquals(mInnerBillingWrapper.getQueriedSkuDetails().get(0), "id1");
    }

    @Test
    public void delays_acknowledgeCall() {
        mConnectedWrapper.acknowledge("id1", null);
        assertNull(mInnerBillingWrapper.getAcknowledgeToken());

        mInnerBillingWrapper.triggerConnected();
        assertEquals(mInnerBillingWrapper.getAcknowledgeToken(), "id1");
    }

    @Test
    public void delays_consumeCall() {
        mConnectedWrapper.consume("id1", null);
        assertNull(mInnerBillingWrapper.getConsumeToken());

        mInnerBillingWrapper.triggerConnected();
        assertEquals(mInnerBillingWrapper.getConsumeToken(), "id1");
    }

    @Test
    public void forwards_getDetailsCall() {
        mConnectedWrapper.connect(null);
        mInnerBillingWrapper.triggerConnected();

        mConnectedWrapper.querySkuDetails("type", Collections.singletonList("id1"), null);
        assertEquals(mInnerBillingWrapper.getQueriedSkuDetails().get(0), "id1");
    }

    @Test
    public void forwards_acknowledge() {
        mConnectedWrapper.connect(null);
        mInnerBillingWrapper.triggerConnected();

        mConnectedWrapper.acknowledge("id1", null);
        assertEquals(mInnerBillingWrapper.getAcknowledgeToken(), "id1");
    }

    @Test
    public void forwards_consume() {
        mConnectedWrapper.connect(null);
        mInnerBillingWrapper.triggerConnected();

        mConnectedWrapper.consume("id1", null);
        assertEquals(mInnerBillingWrapper.getConsumeToken(), "id1");
    }

    @Test
    public void reconnects() {
        mConnectedWrapper.connect(null);
        mInnerBillingWrapper.triggerConnected();

        mConnectedWrapper.consume("id1", null);
        assertEquals(mInnerBillingWrapper.getConsumeToken(), "id1");

        mInnerBillingWrapper.triggerDisconnected();

        mConnectedWrapper.consume("id2", null);
        // The most recent call won't have got through yet, so we're still checking for the previous
        // id.
        assertEquals(mInnerBillingWrapper.getConsumeToken(), "id1");

        mInnerBillingWrapper.triggerConnected();
        assertEquals(mInnerBillingWrapper.getConsumeToken(), "id2");
    }
}
