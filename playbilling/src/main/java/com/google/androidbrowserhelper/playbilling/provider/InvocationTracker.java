// Copyright 2022 Google Inc. All Rights Reserved.
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

package com.google.androidbrowserhelper.playbilling.provider;

import androidx.annotation.Nullable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A helper class for {@link MockBillingWrapper} that helps keep track of which methods were called
 * and the arguments they were called with.
 */
class InvocationTracker<Argument, Callback> {
    private final CountDownLatch mCalledLatch = new CountDownLatch(1);

    @Nullable private Callback mCallback;
    @Nullable private Argument mArgument;

    /** Pretend that the method was called with the given argument and callback. */
    public void call(Argument argument, Callback callback) {
        mCallback = callback;
        mArgument = argument;
        mCalledLatch.countDown();
    }

    /** Returns the argument that the method was previously called with, if any. */
    public @Nullable Argument getArgument() {
        return mArgument;
    }

    /** Returns the callback that the method was previously called with, if any. */
    public @Nullable Callback getCallback() {
        return mCallback;
    }

    /** Wait until the method was called, returns {@code false} if the wait timed out. */
    public boolean waitUntilCalled() throws InterruptedException {
        return mCalledLatch.await(5, TimeUnit.SECONDS);
    }
}
