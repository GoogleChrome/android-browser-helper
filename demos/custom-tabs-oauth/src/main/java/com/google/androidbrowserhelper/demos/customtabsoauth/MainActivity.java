/*
 *    Copyright 2020 Google LLC
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

package com.google.androidbrowserhelper.demos.customtabsoauth;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String AUTHORIZATION_ENDPOINT = "https://github.com/login/oauth/authorize";
    private static final String CLIENT_ID = "<github-client-id>";
    private static final String CLIENT_SECRET = "<github-client-secret>";
    private static final String REDIRECT_URI =
            "https://oauth-custom-tabs.glitch.me/oauth/auth-callback.html";

    private static final OAuthManager OAUTH_MANAGER =
            new OAuthManager(CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_ENDPOINT, REDIRECT_URI);

    private Button mLoginButton;
    private TextView mUserText;
    private ProgressBar mProgressBar;
    private boolean mLoggedIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLoginButton = findViewById(R.id.login_button);
        mUserText = findViewById(R.id.user_text);
        mProgressBar = findViewById(R.id.progress_bar);

        Intent intent = getIntent();
        if (intent != null) {
            Uri data = intent.getData();
            if (data != null && data.getPath() != null
                    && data.getPath().startsWith("/oauth/auth-callback.html")) {
                mProgressBar.setVisibility(View.VISIBLE);
                mLoginButton.setEnabled(false);
                handleAuthCallback(data);
            }
        }
    }

    private void handleAuthCallback(Uri uri) {
        OAUTH_MANAGER.handleAuthCallback(this, uri, (accessToken, scope, tokenType) -> {
            GithubApi.requestGithubUsername(accessToken, (username -> {
                mLoginButton.setText(R.string.logout);
                mLoginButton.setEnabled(true);
                mProgressBar.setVisibility(View.INVISIBLE);
                mUserText.setText(getString(R.string.logged_in, username));
                mLoggedIn = true;
            }));
        });
    }

    public void login(View v) {
        if (mLoggedIn) {
            mLoginButton.setText(R.string.login);
            mUserText.setText(R.string.logged_out);
            mLoggedIn = false;
        } else {
            OAUTH_MANAGER.authorize(this, "user");
        }
    }
}
