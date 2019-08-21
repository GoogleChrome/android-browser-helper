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

package com.google.androidbrowserhelper.trusted;

import android.content.Intent;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.trusted.sharing.ShareData;
import androidx.browser.trusted.sharing.ShareTarget;

/**
 * Utils for preparing the share data to be sent into a Trusted Web Activity.
 */
public class SharingUtils {
    private SharingUtils() {}

    /**
     * Creates a {@link ShareData} object from an {@link Intent}. Returns null if the intent is not
     * a share intent, i.e. its action isn't {@link Intent#ACTION_SEND} or
     * {@link Intent#ACTION_SEND_MULTIPLE}.
     */
    @Nullable
    public static ShareData retrieveShareDataFromIntent(Intent intent) {
        String action = intent.getAction();
        if (!Intent.ACTION_SEND.equals(action) && !Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            return null;
        }
        List<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (uris == null) {
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (uri != null) {
                uris = Collections.singletonList(uri);
            }
        }
        return new ShareData(intent.getStringExtra(Intent.EXTRA_SUBJECT),
                intent.getStringExtra(Intent.EXTRA_TEXT), uris);
    }

    /**
     * Parses a {@link ShareTarget} from a json string. The json string is the "share_target" part
     * of the web manifest, as specified in https://wicg.github.io/web-share-target/level-2/.
     */
    @NonNull
    public static ShareTarget parseShareTargetJson(@NonNull String json) throws JSONException {
        JSONObject object = new JSONObject(json);
        String action = object.getString("action");
        String method = object.optString("method", null);
        String enctype = object.optString("enctype", null);
        JSONObject params = object.getJSONObject("params");
        String paramTitle = params.optString("title", "title");
        String paramText = params.optString("text", "text");
        List<ShareTarget.FileFormField> files = parseFiles(params.optJSONArray("files"));
        return new ShareTarget(action, method, enctype,
                new ShareTarget.Params(paramTitle, paramText, files));
    }

    @Nullable
    private static List<ShareTarget.FileFormField> parseFiles(@Nullable JSONArray filesJson)
            throws JSONException {
        if (filesJson == null) {
            return null;
        }
        List<ShareTarget.FileFormField> files = new ArrayList<>(filesJson.length());
        for (int i = 0; i < filesJson.length(); i++) {
            JSONObject fileJsonObject = filesJson.getJSONObject(i);
            String name = fileJsonObject.getString("name");
            List<String> acceptedTypes = parseAcceptedTypes(fileJsonObject.get("accept"));
            files.add(new ShareTarget.FileFormField(name, acceptedTypes));
        }
        return files;
    }

    @NonNull
    private static List<String> parseAcceptedTypes(@NonNull Object acceptedJson)
            throws JSONException {
        if (acceptedJson instanceof JSONArray) {
            JSONArray acceptJsonArray = (JSONArray) acceptedJson;
            List<String> acceptedTypes = new ArrayList<>(acceptJsonArray.length());
            for (int i = 0; i < acceptJsonArray.length(); i++) {
                acceptedTypes.add(acceptJsonArray.getString(i));
            }
            return acceptedTypes;
        }
        return Collections.singletonList(acceptedJson.toString());
    }
}
