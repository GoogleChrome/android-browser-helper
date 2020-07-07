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

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.trusted.TokenStore;
import androidx.browser.trusted.TrustedWebActivityCallbackRemote;

import java.util.ArrayList;
import java.util.List;

/**
 * An extension of {@link androidx.browser.trusted.TrustedWebActivityService} that implements
 * {@link androidx.browser.trusted.TrustedWebActivityService#getTokenStore()} using a
 * {@link SharedPreferencesTokenStore}.
 */
public class DelegationService extends androidx.browser.trusted.TrustedWebActivityService {
    private final List<ExtraCommandHandler> mExtraCommandHandlers = new ArrayList<>();

    @NonNull
    @Override
    public TokenStore getTokenStore() {
        return new SharedPreferencesTokenStore(this);
    }

    @Nullable
    @Override
    public Bundle onExtraCommand(
            String commandName, Bundle args, @Nullable TrustedWebActivityCallbackRemote callback) {
        for (ExtraCommandHandler handler : mExtraCommandHandlers) {
            Bundle result = handler.handleExtraCommand(this, commandName, args, callback);
            if (result.getBoolean(ExtraCommandHandler.EXTRA_COMMAND_SUCCESS)) {
                return result;
            }
        }
        return Bundle.EMPTY;
    }

    public void registerExtraCommandHandler(ExtraCommandHandler handler) {
        mExtraCommandHandlers.add(handler);
    }
}
