package com.google.androidbrowserhelper.demos.customtabsoauth;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class GithubApi {
    private static final String TAG = "GithubAPI";
    private static final String API_ENDPOINT = "https://api.github.com/user";
    private static final String AUTH_HEADER_KEY = "Authorization";

    public interface UserCallback {
        void onUserData(String username);
    }

    public static void requestGithubUsername(String token, UserCallback callback) {
        new Thread(() -> {
            try {
                Uri uri = Uri.parse(API_ENDPOINT);
                Map<String, String> headers =
                        Collections.singletonMap(AUTH_HEADER_KEY, "token " + token);
                String response = Utils.fetch(uri, headers);
                JSONObject user = new JSONObject(response);
                String username = user.getString("name");

                // Invoke the callback in the main thread.
                new Handler(Looper.getMainLooper()).post(() -> {
                    callback.onUserData(username);
                });
            } catch (IOException | JSONException ex) {
                Log.e(TAG, "Error fetching GitHub user: " + ex.getMessage());
            }
        }).start();
    }
}
