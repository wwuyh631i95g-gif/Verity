package com.verity.talebell;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Calendar;

final class AlarmScheduler {
    static final String PREFS = "tale_alarm";
    static final String ACTION_ALARM = "com.verity.talebell.ACTION_STORY_ALARM";
    private static final int REQUEST_CODE = 4107;

    private AlarmScheduler() {}

    static boolean canSchedule(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms();
    }

    static void schedule(Context context, int hour, int minute, boolean daily) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putBoolean("enabled", true).putInt("hour", hour)
                .putInt("minute", minute).putBoolean("daily", daily).apply();

        Calendar trigger = Calendar.getInstance();
        trigger.set(Calendar.HOUR_OF_DAY, hour);
        trigger.set(Calendar.MINUTE, minute);
        trigger.set(Calendar.SECOND, 0);
        trigger.set(Calendar.MILLISECOND, 0);
        if (trigger.getTimeInMillis() <= System.currentTimeMillis()) trigger.add(Calendar.DAY_OF_YEAR, 1);
        setAlarm(context, trigger.getTimeInMillis());
    }

    static void cancel(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        manager.cancel(alarmPendingIntent(context));
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean("enabled", false).putBoolean("pending", false).apply();
    }

    static void rescheduleIfNeeded(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean("enabled", false) || !canSchedule(context)) return;
        schedule(context, prefs.getInt("hour", 8), prefs.getInt("minute", 0),
                prefs.getBoolean("daily", false));
    }

    static void scheduleNextDay(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean("enabled", false) || !prefs.getBoolean("daily", false)) {
            prefs.edit().putBoolean("enabled", false).apply();
            return;
        }
        Calendar trigger = Calendar.getInstance();
        trigger.set(Calendar.HOUR_OF_DAY, prefs.getInt("hour", 8));
        trigger.set(Calendar.MINUTE, prefs.getInt("minute", 0));
        trigger.set(Calendar.SECOND, 0);
        trigger.set(Calendar.MILLISECOND, 0);
        trigger.add(Calendar.DAY_OF_YEAR, 1);
        setAlarm(context, trigger.getTimeInMillis());
    }

    private static void setAlarm(Context context, long when) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pending = alarmPendingIntent(context);
        Intent showIntent = new Intent(context, MainActivity.class);
        PendingIntent show = PendingIntent.getActivity(context, 4108, showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(when, show);
        manager.setAlarmClock(info, pending);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putLong("next_trigger", when).putBoolean("pending", false).apply();
    }

    private static PendingIntent alarmPendingIntent(Context context) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(ACTION_ALARM);
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
