package app.verity.pocketwidgets;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public final class WidgetActionReceiver extends BroadcastReceiver {
    static final String ACTION_INCREMENT = "app.verity.pocketwidgets.COUNTER_INCREMENT";
    static final String ACTION_REFRESH = "app.verity.pocketwidgets.COUNTER_REFRESH";

    @Override public void onReceive(Context context, Intent intent) {
        if (ACTION_INCREMENT.equals(intent.getAction())) {
            int value = context.getSharedPreferences("pocket_widgets", Context.MODE_PRIVATE)
                    .getInt("counter_67", 0) + 1;
            context.getSharedPreferences("pocket_widgets", Context.MODE_PRIVATE)
                    .edit().putInt("counter_67", value).apply();
        }
        refresh(context);
    }

    static void refresh(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, PocketWidgetProvider.class));
        for (int id : ids) PocketWidgetProvider.updateOne(context, manager, id);
    }
}
