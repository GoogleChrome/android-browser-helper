/*
 *    Copyright 2024 Google LLC
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.google.androidbrowserhelper.demos.customtabsephemeralwithfallback;

import android.content.Context;
import android.util.Log;

import java.io.File;

public class Utils {
    private static final String TAG = "Utils";
    private static final String APP_WEB_VIEW_DIRECTORY = "app_webview";

    public static void clearAppDirectory(Context context) {
        File appDirectory = new File(context.getCacheDir().getParent(), APP_WEB_VIEW_DIRECTORY);
        if (appDirectory.exists() && appDirectory.isDirectory()) {
            delete(appDirectory);
        }
    }

    private static void delete(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File innerFile : files) {
                    delete(innerFile);
                }
            }
        }

        if (!file.delete()) {
            Log.d(TAG, "Could not remove file: " + file.getName());
        }
    }
}
