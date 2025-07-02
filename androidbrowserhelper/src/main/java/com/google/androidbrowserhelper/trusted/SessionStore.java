package com.google.androidbrowserhelper.trusted;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Manages and persists session IDs for given task IDs.
 */
public class SessionStore {
    private static final Map<Integer, Integer> mTaskIdToSessionId = new HashMap<>();

    /**
     * Gets or creates a session ID for a given task ID. If the task ID is null,
     * returns {Integer.MAX_VALUE}.
     *
     * @param taskId The unique ID for the task, may be null.
     * @return The corresponding session ID, or {Integer.MAX_VALUE} if taskId is null.
     */
    public static Integer makeSessionId(@Nullable Integer taskId) {
        if(taskId == null) return Integer.MAX_VALUE;

        Integer sessionId = mTaskIdToSessionId.get(taskId);
        if(sessionId == null) {
            Random random = new Random();
            sessionId = random.nextInt(Integer.MAX_VALUE);
            mTaskIdToSessionId.put(taskId, sessionId);
        }

        return sessionId;
    }
}
