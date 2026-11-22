package app.verity.pocketwidgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public final class PocketWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) updateOne(context, manager, id);
    }

    static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, PocketWidgetProvider.class));
        for (int id : ids) updateOne(context, manager, id);
    }

    static void updateOne(Context context, AppWidgetManager manager, int id) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.pocket_widget);
        int count = context.getSharedPreferences("pocket_widgets", Context.MODE_PRIVATE).getInt("tap_count", 0);
        views.setTextViewText(R.id.widget_count, "Нажатий: " + count);

        Intent add = new Intent(context, WidgetActionReceiver.class);
        add.setAction(WidgetActionReceiver.ACTION_INCREMENT);
        PendingIntent addPi = PendingIntent.getBroadcast(context, id * 10 + 1, add,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_add, addPi);

        Intent open = new Intent(context, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPi = PendingIntent.getActivity(context, id * 10 + 2, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_open, openPi);
        manager.updateAppWidget(id, views);
    }
}
