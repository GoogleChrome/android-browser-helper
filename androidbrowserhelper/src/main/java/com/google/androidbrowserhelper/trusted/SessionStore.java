// Copyright 2026 Google Inc. All Rights Reserved.
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

import androidx.annotation.Nullable;

/**
 * Utility class to map Android task IDs to Trusted Web Activity session IDs.
 */
public class SessionStore {

    private SessionStore() {}

    /**
     * Maps a given task ID to a session ID. If {@code taskId} is non-null, returns the task ID
     * directly as the session ID to deterministically bind the session to the task; otherwise
     * returns {@link Integer#MAX_VALUE}.
     *
     * @param taskId The unique ID for the task, may be null.
     * @return The corresponding session ID, or {@link Integer#MAX_VALUE} if {@code taskId} is null.
     */
    public static Integer makeSessionId(@Nullable Integer taskId) {
        if (taskId == null) return Integer.MAX_VALUE;
        return taskId;
    }
}
