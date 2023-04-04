package com.google.androidbrowserhelper.demos.twapostmessage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.browser.customtabs.CustomTabsSession;

public class PostMessageBroadcastReceiver extends BroadcastReceiver {

  private CustomTabsSession customTabsSession;

  public final static String POST_MESSAGE_ACTION = "com.example.postmessage.POST_MESSAGE_ACTION";

  public PostMessageBroadcastReceiver(CustomTabsSession session){
    customTabsSession = session;
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    customTabsSession.postMessage("Got it!", null);
  }
}
