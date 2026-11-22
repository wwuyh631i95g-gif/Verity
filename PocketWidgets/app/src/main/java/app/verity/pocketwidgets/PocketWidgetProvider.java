package app.verity.pocketwidgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public final class PocketWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) updateOne(context, manager, id);
    }

    static void updateOne(Context context, AppWidgetManager manager, int id) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.pocket_widget);
        int value = context.getSharedPreferences("pocket_widgets", Context.MODE_PRIVATE).getInt("counter_67", 0);
        views.setTextViewText(R.id.widget_count, "6–7 · " + value);
        Intent increment = new Intent(context, WidgetActionReceiver.class);
        increment.setAction(WidgetActionReceiver.ACTION_INCREMENT);
        PendingIntent pi = PendingIntent.getBroadcast(context, id + 17, increment,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_add, pi);
        manager.updateAppWidget(id, views);
    }
}
