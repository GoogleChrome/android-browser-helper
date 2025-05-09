// Copyright 2015 Google Inc. All Rights Reserved.
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

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;

/**
 * Opens Chrome Custom Tabs with a customized UI.
 */
public class CustomUIActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "CustChromeTabActivity";
    private static final int TOOLBAR_ITEM_ID = 1;

    private EditText mUrlEditText;
    private EditText mCustomTabColorEditText;
    private EditText mCustomTabSecondaryColorEditText;
    private CheckBox mShowActionButtonCheckbox;
    private CheckBox mAddMenusCheckbox;
    private CheckBox mShowTitleCheckBox;
    private CheckBox mCustomBackButtonCheckBox;
    private CheckBox mAutoHideAppBarCheckbox;
    private CheckBox mAddDefaultShareCheckbox;
    private CheckBox mToolbarItemCheckbox;
    private CustomTabActivityHelper mCustomTabActivityHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_ui);

        mCustomTabActivityHelper = new CustomTabActivityHelper();
        findViewById(R.id.start_custom_tab).setOnClickListener(this);

        mUrlEditText = findViewById(R.id.url);
        mCustomTabColorEditText = findViewById(R.id.custom_toolbar_color);
        mCustomTabSecondaryColorEditText = findViewById(R.id.custom_toolbar_secondary_color);
        mShowActionButtonCheckbox = findViewById(R.id.custom_show_action_button);
        mAddMenusCheckbox = findViewById(R.id.custom_add_menus);
        mShowTitleCheckBox = findViewById(R.id.show_title);
        mCustomBackButtonCheckBox = findViewById(R.id.custom_back_button);
        mAutoHideAppBarCheckbox = findViewById(R.id.auto_hide_checkbox);
        mAddDefaultShareCheckbox = findViewById(R.id.add_default_share);
        mToolbarItemCheckbox = findViewById(R.id.add_toolbar_item);
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
        if (viewId == R.id.start_custom_tab) {
            openCustomTab();
        }
    }

    private int getColor(EditText editText) {
        try {
            return Color.parseColor(editText.getText().toString());
        } catch (NumberFormatException ex) {
            Log.i(TAG, "Unable to parse Color: " + editText.getText());
            return Color.LTGRAY;
        }
    }

    private void openCustomTab() {
        String url = mUrlEditText.getText().toString();

        int color = getColor(mCustomTabColorEditText);
        int secondaryColor = getColor(mCustomTabSecondaryColorEditText);

        CustomTabsIntent.Builder intentBuilder = new CustomTabsIntent.Builder();
        CustomTabColorSchemeParams defaultColors = new CustomTabColorSchemeParams.Builder()
                .setToolbarColor(color)
                .setSecondaryToolbarColor(secondaryColor)
                .build();
        intentBuilder.setDefaultColorSchemeParams(defaultColors);

        if (mShowActionButtonCheckbox.isChecked()) {
            //Generally you do not want to decode bitmaps in the UI thread. Decoding it in the
            //UI thread to keep the example short.
            String actionLabel = getString(R.string.label_action);
            Bitmap icon = BitmapFactory.decodeResource(getResources(),
                    android.R.drawable.ic_menu_share);
            PendingIntent pendingIntent =
                    createPendingIntent(ActionBroadcastReceiver.ACTION_ACTION_BUTTON);
            intentBuilder.setActionButton(icon, actionLabel, pendingIntent);
        }

        if (mAddMenusCheckbox.isChecked()) {
            String menuItemTitle = getString(R.string.menu_item_title);
            PendingIntent menuItemPendingIntent =
                    createPendingIntent(ActionBroadcastReceiver.ACTION_MENU_ITEM);
            intentBuilder.addMenuItem(menuItemTitle, menuItemPendingIntent);
        }

        int shareState = mAddDefaultShareCheckbox.isChecked() ?
                CustomTabsIntent.SHARE_STATE_ON : CustomTabsIntent.SHARE_STATE_OFF;
        intentBuilder.setShareState(shareState);

        if (mToolbarItemCheckbox.isChecked()) {
            //Generally you do not want to decode bitmaps in the UI thread. Decoding it in the
            //UI thread to keep the example short.
            String actionLabel = getString(R.string.label_action);
            Bitmap icon = BitmapFactory.decodeResource(getResources(),
                    android.R.drawable.ic_menu_share);
            PendingIntent pendingIntent =
                    createPendingIntent(ActionBroadcastReceiver.ACTION_TOOLBAR);
            intentBuilder.addToolbarItem(TOOLBAR_ITEM_ID, icon, actionLabel, pendingIntent);
        }

        intentBuilder.setShowTitle(mShowTitleCheckBox.isChecked());

        intentBuilder.setUrlBarHidingEnabled(mAutoHideAppBarCheckbox.isChecked());

        if (mCustomBackButtonCheckBox.isChecked()) {
            intentBuilder.setCloseButtonIcon(toBitmap(getDrawable(R.drawable.ic_arrow_back)));
        }

        intentBuilder.setStartAnimations(this, R.anim.slide_in_right, R.anim.slide_out_left);
        intentBuilder.setExitAnimations(this, android.R.anim.slide_in_left,
                android.R.anim.slide_out_right);

        CustomTabActivityHelper.openCustomTab(
                this, intentBuilder.build(), Uri.parse(url), new WebviewFallback());
    }

    private PendingIntent createPendingIntent(int actionSourceId) {
        Intent actionIntent = new Intent(
                this.getApplicationContext(), ActionBroadcastReceiver.class);
        actionIntent.putExtra(ActionBroadcastReceiver.KEY_ACTION_SOURCE, actionSourceId);
        return PendingIntent.getBroadcast(
                getApplicationContext(), actionSourceId, actionIntent, PendingIntent.FLAG_MUTABLE);
    }

    /**
     * Return a Bitmap representation of the Drawable. Based on Android KTX.
     */
    private Bitmap toBitmap(Drawable drawable) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        Rect oldBounds = new Rect(drawable.getBounds());

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(new Canvas(bitmap));

        drawable.setBounds(oldBounds);
        return bitmap;
    }
}
