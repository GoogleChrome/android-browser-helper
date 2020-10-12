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

package com.google.androidbrowserhelper.trusted;

import android.content.pm.PackageManager;

public class ChromeOsSupport {
    /** Feature name for ARC++ (App Runtime for Chrome running on Chrome OS). */
    public static final String ARC_FEATURE = "org.chromium.arc";

    /** The ARC++ package that quries for payments on behalf of TWAs on Chrome OS */
    public static final String ARC_PAYMENT_APP = "org.chromium.arc.payment_app";

    /**
     * Checks if the application is running on ARC++.
     *
     * @param pm A {@link PackageManager}.
     * @return True if running on ARC++.
     */
    public static boolean isRunningOnArc(PackageManager pm) {
        return pm.hasSystemFeature(ARC_FEATURE);
    }
}
