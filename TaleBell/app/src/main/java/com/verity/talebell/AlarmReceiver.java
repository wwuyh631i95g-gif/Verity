package com.verity.talebell;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "talebell_alarm_channel";
    private static final int NOTIFICATION_ID = 4109;

    @Override
    public void onReceive(Context context, Intent intent) {
        AlarmScheduler.scheduleNextDay(context);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Будильник сказок", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Напоминание включить новую сказку");
            channel.enableVibration(true);
            manager.createNotificationChannel(channel);
        }

        Intent open = new Intent(context, MainActivity.class);
        open.putExtra(MainActivity.EXTRA_ALARM, true);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent content = PendingIntent.getActivity(context, 4110, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, CHANNEL_ID) : new Notification.Builder(context);
        Notification notification = builder
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Пора слушать сказку")
                .setContentText("Сказочник готов придумать и озвучить новое произведение")
                .setCategory(Notification.CATEGORY_ALARM)
                .setPriority(Notification.PRIORITY_MAX)
                .setAutoCancel(true)
                .setContentIntent(content)
                .setFullScreenIntent(content, true)
                .build();

        manager.notify(NOTIFICATION_ID, notification);
    }
}
