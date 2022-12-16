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

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.trusted.Token;
import androidx.browser.trusted.TokenStore;
import androidx.browser.trusted.TrustedWebActivityCallbackRemote;
import androidx.browser.trusted.TrustedWebActivityService;

import java.util.ArrayList;
import java.util.List;

/**
 * An extension of {@link TrustedWebActivityService} that implements
 * {@link TrustedWebActivityService#getTokenStore()} using a
 * {@link SharedPreferencesTokenStore}.
 */
public class DelegationService extends TrustedWebActivityService {
    private final List<ExtraCommandHandler> mExtraCommandHandlers = new ArrayList<>();
    private SharedPreferencesTokenStore mTokenStore;

    public DelegationService() {
        registerExtraCommandHandler(new NotificationDelegationExtraCommandHandler());
    }

    @NonNull
    @Override
    @SuppressLint("WrongThread")
    // TokenStore#store wants to be called on a Worker thread for performance reasons, since
    // communication with the browser is async, this shouldn't be a problem.
    public TokenStore getTokenStore() {
        if (mTokenStore == null) {
            mTokenStore = new SharedPreferencesTokenStore(this);

            PackageManager pm = getPackageManager();
            if (ChromeOsSupport.isRunningOnArc(pm)) {
                // TWAs launched on ChromeOS may not always go through the normal launch flow
                // (LauncherActivity, TwaLauncher, etc), so setting the verified browser there
                // won't work. We must set it here instead.
                mTokenStore.store(Token.create(ChromeOsSupport.ARC_PAYMENT_APP, pm));
            }
        }

        return mTokenStore;
    }

    @Nullable
    @Override
    public Bundle onExtraCommand(
        @NonNull String commandName, @NonNull Bundle args, @Nullable TrustedWebActivityCallbackRemote callback) {
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
