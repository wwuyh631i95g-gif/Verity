package com.twinfold.duolink;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Path;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

public class TwinFoldAccessibilityService extends AccessibilityService {
    private static volatile TwinFoldAccessibilityService active;

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        active = this;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        // No screen text or window content is read. The service only injects approved gestures.
    }

    @Override public void onInterrupt() { }

    @Override public void onDestroy() {
        if (active == this) active = null;
        super.onDestroy();
    }

    public static boolean isEnabled(Context context) {
        String enabled = Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabled)) return false;
        ComponentName target = new ComponentName(context, TwinFoldAccessibilityService.class);
        String full = target.flattenToString();
        String shortName = target.flattenToShortString();
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabled);
        while (splitter.hasNext()) {
            String value = splitter.next();
            if (full.equalsIgnoreCase(value) || shortName.equalsIgnoreCase(value)) return true;
        }
        return false;
    }

    public static void dispatchRemoteGesture(Context context, float x1, float y1,
                                             float x2, float y2, long durationMs, int side) {
        TwinFoldAccessibilityService service = active;
        if (service == null || Build.VERSION.SDK_INT < 24) return;

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) service.getSystemService(WINDOW_SERVICE);
        if (wm == null) return;
        wm.getDefaultDisplay().getRealMetrics(metrics);
        float width = Math.max(1, metrics.widthPixels);
        float height = Math.max(1, metrics.heightPixels);

        float sx = clamp(x1);
        float sy = clamp(y1);
        float ex = clamp(x2);
        float ey = clamp(y2);
        if (side == 0) { // remote phone displays the right half
            sx = 0.5f + sx * 0.5f;
            ex = 0.5f + ex * 0.5f;
        } else if (side == 1) { // left half
            sx *= 0.5f;
            ex *= 0.5f;
        } else if (side == 2) { // bottom half
            sy = 0.5f + sy * 0.5f;
            ey = 0.5f + ey * 0.5f;
        } else { // top half
            sy *= 0.5f;
            ey *= 0.5f;
        }

        float startX = Math.max(1f, Math.min(width - 2f, sx * width));
        float startY = Math.max(1f, Math.min(height - 2f, sy * height));
        float endX = Math.max(1f, Math.min(width - 2f, ex * width));
        float endY = Math.max(1f, Math.min(height - 2f, ey * height));

        Path path = new Path();
        path.moveTo(startX, startY);
        path.lineTo(endX, endY);
        long duration = Math.max(50L, Math.min(1200L, durationMs));
        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, duration);
        GestureDescription gesture = new GestureDescription.Builder().addStroke(stroke).build();
        service.dispatchGesture(gesture, null, null);
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
