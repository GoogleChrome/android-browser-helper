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

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.trusted.TrustedWebActivityCallbackRemote;

/**
 * An interface for handling extraCommand in {@link androidx.browser.trusted.TrustedWebActivityService}.
 */
public interface ExtraCommandHandler {
    String EXTRA_COMMAND_SUCCESS = "success";

    /**
     * Handles the free form command from the browser. The return bundle should contain a boolean
     * {@code EXTRA_COMMAND_SUCCESS}, if the command is handled, set the value to {@code true};
     * otherwise, set to {@code false}.
     */
    @NonNull
    Bundle handleExtraCommand(Context context, String commandName, Bundle args,
                              @Nullable TrustedWebActivityCallbackRemote callback);
}
