package com.google.androidbrowserhelper.demos.twapostmessage;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class MessageNotificationHandler {

  private final static String CHANNEL_ID = "TWA-postmessage-channel-id";


  public static void showNotificationWithMessage(Context context, String message) {
    Intent intent = new Intent();
      intent.setAction(PostMessageBroadcastReceiver.POST_MESSAGE_ACTION);

    PendingIntent pendingIntent =
        PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

    NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_background)
        .setContentTitle("Received a message")
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .addAction(R.drawable.ic_launcher_background, "Reply back", pendingIntent)
        .setAutoCancel(true);

    NotificationManagerCompat.from(context).notify(1, builder.build());

  }

  public static void createNotificationChannelIfNeeded(Context context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      String name = "TWA-postMessage-Demo";
      String descriptionText = "A channel to send post message demo notification";
      int importance = NotificationManager.IMPORTANCE_HIGH;
      NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
      channel.setDescription(descriptionText);

      NotificationManager notificationManager =
          (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

      notificationManager.createNotificationChannel(channel);
    }
  }
}
