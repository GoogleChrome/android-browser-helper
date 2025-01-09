// Copyright 2019 Google Inc. All Rights Reserved.
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

package com.google.androidbrowserhelper.trusted.testcomponents;

import android.app.Activity;
import android.os.Bundle;
import android.os.Looper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;

/**
 * A fake Browser that accepts browsable Intents.
 */
public class TestBrowser extends Activity {

    private final CountDownLatch mResumeLatch = new CountDownLatch(1);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mResumeLatch.countDown();
    }

    /**
     * Waits until onResume. Returns whether has reached onResume until timeout.
     * If already resumed, returns "true" immediately.
     */
    public boolean waitForResume(int timeoutMillis) {
        assert Thread.currentThread() != Looper.getMainLooper().getThread() : "Deadlock!";
        try {
            return mResumeLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }
}
