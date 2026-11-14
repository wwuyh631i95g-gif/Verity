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
        if (Math.abs(endX - startX) < 1f && Math.abs(endY - startY) < 1f) {
            endX = Math.min(width - 2f, startX + 1f);
        }
        path.lineTo(endX, endY);
        long duration = Math.max(50L, Math.min(1200L, durationMs));
        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, duration);
        GestureDescription gesture = new GestureDescription.Builder().addStroke(stroke).build();
        service.dispatchGesture(gesture, null, null);
    }

    public static void dispatchRemoteGestureBatch(Context context, String encodedTracks, int side) {
        TwinFoldAccessibilityService service = active;
        if (service == null || Build.VERSION.SDK_INT < 24 || encodedTracks == null || encodedTracks.isEmpty()) return;

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) service.getSystemService(WINDOW_SERVICE);
        if (wm == null) return;
        wm.getDefaultDisplay().getRealMetrics(metrics);
        float screenWidth = Math.max(1, metrics.widthPixels);
        float screenHeight = Math.max(1, metrics.heightPixels);

        GestureDescription.Builder builder = new GestureDescription.Builder();
        String[] tracks = encodedTracks.split(";");
        int count = Math.min(Math.min(tracks.length, 10), GestureDescription.getMaxStrokeCount());
        int added = 0;

        for (int i = 0; i < count; i++) {
            try {
                String[] values = tracks[i].split(",");
                if (values.length < 4 || (values.length & 1) != 0) continue;
                long startAt = Math.max(0L, Math.min(59000L, Long.parseLong(values[0])));
                long duration = Math.max(50L, Math.min(60000L - startAt, Long.parseLong(values[1])));
                if (duration < 1L) continue;

                float firstX = clamp(Integer.parseInt(values[2]) / 10000f);
                float firstY = clamp(Integer.parseInt(values[3]) / 10000f);
                float[] first = mapToSource(firstX, firstY, side, screenWidth, screenHeight);
                Path path = new Path();
                path.moveTo(first[0], first[1]);

                float lastX = first[0];
                float lastY = first[1];
                for (int n = 4; n + 1 < values.length; n += 2) {
                    float nx = clamp(Integer.parseInt(values[n]) / 10000f);
                    float ny = clamp(Integer.parseInt(values[n + 1]) / 10000f);
                    float[] point = mapToSource(nx, ny, side, screenWidth, screenHeight);
                    path.lineTo(point[0], point[1]);
                    lastX = point[0];
                    lastY = point[1];
                }

                if (values.length == 4) {
                    path.lineTo(Math.min(screenWidth - 2f, lastX + 0.5f), lastY);
                }
                GestureDescription.StrokeDescription stroke =
                        new GestureDescription.StrokeDescription(path, startAt, duration);
                builder.addStroke(stroke);
                added++;
            } catch (Exception ignored) {
                // Ignore malformed pointer tracks; other fingers can still be dispatched.
            }
        }

        if (added > 0) {
            service.dispatchGesture(builder.build(), new GestureResultCallback() {
                @Override public void onCompleted(GestureDescription gestureDescription) {
                    super.onCompleted(gestureDescription);
                }
                @Override public void onCancelled(GestureDescription gestureDescription) {
                    super.onCancelled(gestureDescription);
                }
            }, null);
        }
    }

    private static float[] mapToSource(float x, float y, int side,
                                       float width, float height) {
        float sx = clamp(x);
        float sy = clamp(y);
        if (side == 0) {
            sx = 0.5f + sx * 0.5f;
        } else if (side == 1) {
            sx *= 0.5f;
        } else if (side == 2) {
            sy = 0.5f + sy * 0.5f;
        } else {
            sy *= 0.5f;
        }
        float px = Math.max(1f, Math.min(width - 2f, sx * width));
        float py = Math.max(1f, Math.min(height - 2f, sy * height));
        return new float[]{px, py};
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
