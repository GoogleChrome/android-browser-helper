package com.google.androidbrowserhelper;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.androidbrowserhelper.demo.R;
import com.google.androidbrowserhelper.launchtwa.LaunchTwaActivity;

public class DemoListActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo_list);
    }

    public void launchTwaDemos(View view) {
        startActivity(new Intent(this, LaunchTwaActivity.class));
    }
}
