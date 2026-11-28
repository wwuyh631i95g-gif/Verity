package com.verity.livingdesk;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Locale;

public final class NewYearWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) updateOne(context, manager, id);
    }

    private void updateOne(Context context, AppWidgetManager manager, int id) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime next = LocalDate.of(now.getYear() + 1, 1, 1).atStartOfDay(now.getZone());
        long seconds = Math.max(0L, Duration.between(now, next).getSeconds());
        long days = seconds / 86400L;
        long hours = (seconds % 86400L) / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long secs = seconds % 60L;

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_new_year);
        views.setTextViewText(R.id.widget_days, String.format(Locale.getDefault(), "%d дней", days));
        views.setTextViewText(R.id.widget_clock,
                String.format(Locale.getDefault(), "%02d:%02d:%02d до полуночи", hours, minutes, secs));
        Intent open = new Intent(context, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(context, 44, open,
                PendingIntent.FLAG_UPDATE_CURRENT | (android.os.Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        views.setOnClickPendingIntent(R.id.widget_root, pending);
        manager.updateAppWidget(id, views);
    }

    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(new ComponentName(context, NewYearWidgetProvider.class));
            for (int id : ids) updateOne(context, manager, id);
        }
    }
}
