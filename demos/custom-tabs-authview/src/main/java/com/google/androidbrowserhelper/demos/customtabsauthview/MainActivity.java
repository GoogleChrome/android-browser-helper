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

package com.google.androidbrowserhelper.demos.customtabsauthview;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.auth.AuthTabIntent;
import androidx.browser.auth.ExperimentalAuthTab;

@OptIn(markerClass = ExperimentalAuthTab.class)
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private static final String AUTHORIZATION_ENDPOINT = "https://github.com/login/oauth/authorize";
    private static final String CLIENT_ID = "<github-client-id>";
    private static final String CLIENT_SECRET = "<github-client-secret>";
    private static final String REDIRECT_SCHEME = "auth";

    private static final AuthManager O_AUTH_MANAGER =
            new AuthManager(CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_ENDPOINT, REDIRECT_SCHEME);

    private final ActivityResultLauncher<Intent> mLauncher =
            AuthTabIntent.registerActivityResultLauncher(this, this::handleAuthResult);

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
            if (data != null && data.getHost() != null
                    && data.getHost().startsWith("callback")) {
                mProgressBar.setVisibility(View.VISIBLE);
                mLoginButton.setEnabled(false);
                completeAuth(data);
            }
        }
    }

    public void login(View v) {
        if (mLoggedIn) {
            mLoginButton.setText(R.string.login);
            mUserText.setText(R.string.logged_out);
            mLoggedIn = false;
        } else {
            O_AUTH_MANAGER.authorize(this, mLauncher, "user");
        }
    }

    private void handleAuthResult(AuthTabIntent.AuthResult result) {
        if (result.resultCode == AuthTabIntent.RESULT_OK) {
            completeAuth(result.resultUri);
        }
    }

    private void completeAuth(Uri uri) {
        O_AUTH_MANAGER.continueAuthFlow(this, uri, (accessToken, scope, tokenType) -> {
            GithubApi.requestGithubUsername(accessToken, (username -> {
                mLoginButton.setText(R.string.logout);
                mLoginButton.setEnabled(true);
                mProgressBar.setVisibility(View.INVISIBLE);
                mUserText.setText(getString(R.string.logged_in, username));
                mLoggedIn = true;
            }));
        });
    }
}