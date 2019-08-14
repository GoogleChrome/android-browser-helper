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

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.widget.Toast;

/**
 * Similar to {@link ChromeLegacyUtils}, behavior specific to Chrome
 */
public class ChromeUpdatePrompt {
    /**
     * The resource identifier to be passed to {@link Resources#getIdentifier} specifying the
     * resource name and type of the string to show if launching with an out of date version of
     * Chrome.
     */
    private static final String UPDATE_CHROME_MESSAGE_RESOURCE_ID = "string/update_chrome_toast";

    private ChromeUpdatePrompt() {}

    /**
     * If we are about to launch a TWA on Chrome Beta or Stable at a version before TWAs are
     * supported, display a Toast to the user asking them to update.
     * @param context {@link Context} to launch the Toast and access Resources and the
     *                PackageManager.
     * @param providerPackage Provider package we're about to use.
     */
    public static void promptIfNeeded(Context context, String providerPackage) {
        if (!ChromeLegacyUtils.VERSION_CHECK_CHROME_PACKAGES.contains(providerPackage)) return;
        if (!chromeNeedsUpdate(context.getPackageManager(), providerPackage)) {
            return;
        }

        showToastIfResourceExists(context, UPDATE_CHROME_MESSAGE_RESOURCE_ID);
    }

    private static boolean chromeNeedsUpdate(PackageManager pm, String chromePackage) {
        int versionCode = ChromeLegacyUtils.getVersionCode(pm, chromePackage);
        if (versionCode == 0) {
            // Do nothing - the user doesn't get prompted to update, but falling back to Custom
            // Tabs should still work.
            return false;
        }
        return versionCode < ChromeLegacyUtils.VERSION_SUPPORTS_TRUSTED_WEB_ACTIVITIES;
    }

    private static void showToastIfResourceExists(Context context, String resource) {
        int stringId = context.getResources().getIdentifier(resource, null,
                context.getPackageName());
        if (stringId == 0) return;

        Toast.makeText(context, stringId, Toast.LENGTH_LONG).show();
    }
}
