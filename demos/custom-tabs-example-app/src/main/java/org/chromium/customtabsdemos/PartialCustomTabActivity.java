// Copyright 2022 Google Inc. All Rights Reserved.
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
package org.chromium.customtabsdemos;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsSession;

/**
 * Opens Partial-height Chrome Custom Tab.
 *
 * <p>As of androidx.browser:browser:1.5.0-alpha01, following APIs for UI customization
 *    are supported:
 *
 * <h3> Initial activity height </h3>
 * <ul>
 *   <li> Prior to Chrome M107, this can be customized with a legacy intent extra flag
 *     "androidx.browser.customtabs.extra.INITIAL_ACTIVITY_HEIGHT_IN_PIXEL". </li>
 *   <li> From M107, {@link CustomTabIntent.Builder#setInitialActivityHeightPx()} is supported.</li>
 * </ul>
 *
 * <h3> Fixed-height tab </h3>
 * <ul>
 *   <li> The tab height is not fixed i.e. resizable by default. </li>
 *   <li> From M107, this can be set to fixed with an intent extra flag
 *     CustomTabsIntent.EXTRA_ACTIVITY_RESIZE_BEHAVIOR set to
 *     CustomTabsIntent.ACTIVITY_HEIGHT_FIXED. </li>
 *   <li> From M107, {@link CustomTabIntent.Builder#setInitialActivityHeightPx(int, int)}
 *     is also supported to specify the height and resize behavior. </li>
 * </ul>
 *
 * <h3> Start/End animation </h3>
 * <ul>
 *   <li> Start animation is always slide-up from bottom. This will be enforced from M109. </li>
 *   <li> End animation is always slide-down. This is not customizable. </li>
 * </ul>
 *
 * <h3> Toolbar corner radius </h3>
 * <ul>
 *   <li> The maximum radius is 16dp, also the default value. </li>
 *   <li> Prior to M107, this can be customized with a legacy intent extra flag
 *     "androidx.browser.customtabs.extra.TOOLBAR_CORNER_RADIUS_IN_PIXEL" </li>
 *   <li> From M107, {@link CustomTabIntent.Builder#setToolbarCornerRadiusDp()} is supported. </li>
 * </ul>
 *
 * <h3> Background app interaction </h3>
 * <ul>
 *   <li> Background app is interactable by default. </li>
 *   <li> Interaction can be disabled from M109 with an intent extra flag
 *     "androix.browser.customtabs.extra.ENABLE_BACKGROUND_INTERACTION" </li>
 *   <li> Builder API will be provided in the future. </li>
 * </ul>
 */
public class PartialCustomTabActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "PartialCustomTabActivity";
    private static final int INITIAL_HEIGHT_DEFAULT_PX = 600;
    private static final int CORNER_RADIUS_MAX_DP = 16;
    private static final int CORNER_RADIUS_DEFAULT_DP = CORNER_RADIUS_MAX_DP;
    private static final int BACKGROUND_INTERACT_OFF_VALUE = 2;

    private EditText mUrlEditText;
    private CheckBox mFixedHeightCheckbox;
    private CheckBox mBackgroundAppCheckbox;
    private SeekBar mToolbarCornerRadiusSlider;
    private TextView mToolbarCornerRadiusLabel;
    private CustomTabActivityHelper mCustomTabActivityHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_partial_custom);

        mCustomTabActivityHelper = new CustomTabActivityHelper();
        findViewById(R.id.start_custom_tab).setOnClickListener(this);

        mUrlEditText = findViewById(R.id.url);
        mFixedHeightCheckbox = findViewById(R.id.fixed_height);
        mBackgroundAppCheckbox = findViewById(R.id.background_app);

        mToolbarCornerRadiusLabel = findViewById(R.id.radius_dp_label);
        mToolbarCornerRadiusSlider = findViewById(R.id.corner_radius_slider);
        mToolbarCornerRadiusSlider.setMax(CORNER_RADIUS_MAX_DP);
        mToolbarCornerRadiusSlider.setProgress(CORNER_RADIUS_DEFAULT_DP);

        int dp = mToolbarCornerRadiusSlider.getProgress();
        mToolbarCornerRadiusLabel.setText(getString(R.string.dp_template, dp));
        mToolbarCornerRadiusSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                String strDp = getString(R.string.dp_template, progress);
                mToolbarCornerRadiusLabel.setText(strDp);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mCustomTabActivityHelper.bindCustomTabsService(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCustomTabActivityHelper.unbindCustomTabsService(this);
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        switch (viewId) {
            case R.id.start_custom_tab:
                openCustomTab();
                break;
            default:
                // Unknown View Clicked
        }
    }

    private void openCustomTab() {
        String url = mUrlEditText.getText().toString();

        // Uses the established session to build a PCCT intent.
        CustomTabsSession session = mCustomTabActivityHelper.getSession();
        CustomTabsIntent.Builder intentBuilder = new CustomTabsIntent.Builder(session);
        int resizeBehavior = mFixedHeightCheckbox.isChecked()
                ? CustomTabsIntent.ACTIVITY_HEIGHT_FIXED
                : CustomTabsIntent.ACTIVITY_HEIGHT_DEFAULT;

        intentBuilder.setInitialActivityHeightPx(INITIAL_HEIGHT_DEFAULT_PX, resizeBehavior);
        int toolbarCornerRadiusDp = mToolbarCornerRadiusSlider.getProgress();
        intentBuilder.setToolbarCornerRadiusDp(toolbarCornerRadiusDp);

        CustomTabsIntent customTabsIntent = intentBuilder.build();

        customTabsIntent.intent.putExtra(
                "androidx.browser.customtabs.extra.INITIAL_ACTIVITY_HEIGHT_IN_PIXEL",
                INITIAL_HEIGHT_DEFAULT_PX);
        int toolbarCornerRadiusPx =
                Math.round(toolbarCornerRadiusDp * getResources().getDisplayMetrics().density);
        customTabsIntent.intent.putExtra(
                "androidx.browser.customtabs.extra.TOOLBAR_CORNER_RADIUS_IN_PIXEL",
                toolbarCornerRadiusPx);
        if (resizeBehavior != CustomTabsIntent.ACTIVITY_HEIGHT_DEFAULT) {
            customTabsIntent.intent.putExtra(
                    CustomTabsIntent.EXTRA_ACTIVITY_HEIGHT_RESIZE_BEHAVIOR, resizeBehavior);
        }
        if (!mBackgroundAppCheckbox.isChecked()) {
            customTabsIntent.intent.putExtra(
                    "androix.browser.customtabs.extra.ENABLE_BACKGROUND_INTERACTION",
                    BACKGROUND_INTERACT_OFF_VALUE);
        }

        CustomTabActivityHelper.openCustomTab(
                this, customTabsIntent, Uri.parse(url), new WebviewFallback());
    }
}
