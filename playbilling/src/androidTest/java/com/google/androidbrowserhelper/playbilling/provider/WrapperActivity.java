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

package com.google.androidbrowserhelper.playbilling.provider;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;

/**
 * An Activity that launches another Activity with {@link #startActivityForResult}.
 *
 * Originally I'd attempted to have this Activity launched by an ActivityTestRule, but that seemed
 * unable to handle an Activity that immediately launched another Activity.
 *
 * I agree that having statics all over the place is ugly, but it's test code and it works.
 */
public class WrapperActivity extends Activity {
    // Can't use 0, 1 or -1 as they're valid values.
    private static final int INVALID_RESULT_CODE = 666;
    private static int sResultCode = INVALID_RESULT_CODE;

    private static CountDownLatch sLaunchLatch = new CountDownLatch(1);
    private static CountDownLatch sFinishLatch = new CountDownLatch(1);

    public static final String EXTRA_INNER_INTENT = "inner_intent";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sResultCode = INVALID_RESULT_CODE;

        Intent innerIntent = getIntent().getParcelableExtra(EXTRA_INNER_INTENT);
        startActivityForResult(innerIntent, 0);

        sLaunchLatch.countDown();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        sResultCode = resultCode;
        finish();
        sFinishLatch.countDown();
    }

    static int getLastResultCode() {
        return sResultCode;
    }

    static void reset() {
        sLaunchLatch = new CountDownLatch(1);
        sFinishLatch = new CountDownLatch(1);
    }

    static boolean waitForLaunch() throws InterruptedException {
        return sLaunchLatch.await(5, TimeUnit.SECONDS);
    }

    static boolean waitForFinish() throws InterruptedException {
        return sFinishLatch.await(5, TimeUnit.SECONDS);
    }
}
