package app.verity.pocketwidgets;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class WidgetActionReceiver extends BroadcastReceiver {
    static final String ACTION_INCREMENT = "app.verity.pocketwidgets.INCREMENT";
    static final String ACTION_REFRESH = "app.verity.pocketwidgets.REFRESH";

    @Override public void onReceive(Context context, Intent intent) {
        if (ACTION_INCREMENT.equals(intent.getAction())) {
            int value = context.getSharedPreferences("pocket_widgets", Context.MODE_PRIVATE)
                    .getInt("tap_count", 0) + 1;
            context.getSharedPreferences("pocket_widgets", Context.MODE_PRIVATE)
                    .edit().putInt("tap_count", value).apply();
        }
        PocketWidgetProvider.updateAll(context);
    }
}
