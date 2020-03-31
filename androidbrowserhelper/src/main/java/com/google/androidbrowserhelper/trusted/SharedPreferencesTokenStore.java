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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Base64;

import androidx.annotation.Nullable;
import androidx.browser.trusted.Token;
import androidx.browser.trusted.TokenStore;

/**
 * Implements a {@link TokenStore} that uses {@link SharedPreferences} as the storage mechanism
 * for the {@link Token}.
 * Since it is backed by SharedPreferences, all instances of this class share state (if you call
 * {@link #setVerifiedProvider} on one, subsequent calls to {@link #load()} on other instances will
 * return the new value).
 */
public class SharedPreferencesTokenStore implements TokenStore {
    private static final String SHARED_PREFERENCES_NAME = "com.google.androidbrowserhelper";
    private static final String KEY_TOKEN =
            "SharedPreferencesTokenStore.TOKEN";

    private Context mContext;

    /**
     * Creates a new SharedPreferencesTokenStore.
     *
     * @param mContext The {@link Context} where the {@link SharedPreferences} will be stored.
     */
    public SharedPreferencesTokenStore(Context mContext) {
        this.mContext = mContext;
    }

    /**
     * This persists the given {@link Token} on a {@link SharedPreferences}.
     * Subsequent calls will overwrite the previously given {@link Token}.
     *
     * @param token The token to persist. It may be {@code null} to clear the storage.
     */
    @Override
    public void store(@Nullable Token token) {
        SharedPreferences preferences =
                mContext.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);

        // Clear the preference if the token is null
        if (token == null) {
            preferences.edit().remove(KEY_TOKEN).apply();
            return;
        }

        String encodedToken =
                Base64.encodeToString(token.serialize(), Base64.NO_WRAP | Base64.NO_PADDING);
        preferences.edit()
                .putString(KEY_TOKEN, encodedToken)
                .apply();
    }

    /**
     * This method returns the {@link Token} previously persisted by a call to {@link #store}.
     * @return The previously persisted {@link Token}, or {@code null} if none exist.
     *
     * This method will be called on a binder thread by {@link DelegationService}.
     */
    @Nullable
    @Override
    public Token load() {
        SharedPreferences preferences =
                mContext.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        String stringifiedToken = preferences.getString(KEY_TOKEN, null);
        if (stringifiedToken == null) {
            return null;
        }

        byte[] serializedToken =
                Base64.decode(stringifiedToken, Base64.NO_WRAP | Base64.NO_PADDING);
        return Token.deserialize(serializedToken);
    }

    public void setVerifiedProvider(String providerPackage, PackageManager packageManager) {
        Token token = Token.create(providerPackage, packageManager);
        this.store(token);
    }
}
