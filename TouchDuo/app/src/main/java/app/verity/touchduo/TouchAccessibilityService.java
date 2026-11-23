package app.verity.touchduo;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

public class TouchAccessibilityService extends AccessibilityService {
    private WindowManager windowManager;
    private TextView floatingButton;
    private WindowManager.LayoutParams buttonParams;
    private boolean receiverRegistered;
    private boolean dispatching;

    private final BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateOverlay();
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        registerReceiver(refreshReceiver, new IntentFilter(MainActivity.ACTION_REFRESH));
        receiverRegistered = true;
        updateOverlay();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void updateOverlay() {
        SharedPreferences p = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE);
        if (!p.getBoolean("overlay_enabled", true)) {
            removeOverlay();
            return;
        }
        if (floatingButton != null || windowManager == null) return;

        floatingButton = new TextView(this);
        floatingButton.setText(p.getInt("point_count", 2) + "×");
        floatingButton.setTextColor(Color.WHITE);
        floatingButton.setTextSize(16);
        floatingButton.setGravity(Gravity.CENTER);
        floatingButton.setElevation(dp(6));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(34, 104, 220));
        background.setCornerRadius(dp(28));
        background.setStroke(dp(1), Color.WHITE);
        floatingButton.setBackground(background);

        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        int size = dp(58);
        buttonParams = new WindowManager.LayoutParams(
                size, size,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        buttonParams.gravity = Gravity.TOP | Gravity.LEFT;
        buttonParams.x = Math.max(0, metrics.widthPixels - size - dp(8));
        buttonParams.y = Math.max(dp(70), Math.min(dp(170), metrics.heightPixels - size - dp(8)));

        floatingButton.setOnTouchListener(new View.OnTouchListener() {
            private float downRawX;
            private float downRawY;
            private int startX;
            private int startY;
            private boolean moved;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downRawX = event.getRawX();
                        downRawY = event.getRawY();
                        startX = buttonParams.x;
                        startY = buttonParams.y;
                        moved = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - downRawX;
                        float dy = event.getRawY() - downRawY;
                        if (Math.abs(dx) > dp(5) || Math.abs(dy) > dp(5)) moved = true;
                        if (moved) {
                            DisplayMetrics dm = new DisplayMetrics();
                            windowManager.getDefaultDisplay().getRealMetrics(dm);
                            buttonParams.x = Math.max(0, Math.min(dm.widthPixels - buttonParams.width, startX + (int) dx));
                            buttonParams.y = Math.max(0, Math.min(dm.heightPixels - buttonParams.height, startY + (int) dy));
                            try { windowManager.updateViewLayout(floatingButton, buttonParams); } catch (Exception ignored) {}
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (!moved) sendMultiTouchGesture();
                        return true;
                }
                return true;
            }
        });

        try {
            windowManager.addView(floatingButton, buttonParams);
        } catch (Exception e) {
            floatingButton = null;
            buttonParams = null;
        }
    }

    private void sendMultiTouchGesture() {
        if (dispatching) return;
        SharedPreferences p = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE);
        int count = Math.max(2, Math.min(4, p.getInt("point_count", 2)));
        int duration = Math.max(80, Math.min(700, p.getInt("duration_ms", 180)));

        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        int width = Math.max(2, metrics.widthPixels);
        int height = Math.max(2, metrics.heightPixels);

        GestureDescription.Builder gesture = new GestureDescription.Builder();
        for (int i = 1; i <= count; i++) {
            float x = (width - 1) * (Math.max(0, Math.min(100, p.getInt("x" + i, i == 1 ? 18 : 82))) / 100.0f);
            float y = (height - 1) * (Math.max(0, Math.min(100, p.getInt("y" + i, 82))) / 100.0f);
            Path path = new Path();
            path.moveTo(x, y);
            path.lineTo(x < width - 1 ? x + 1.0f : x - 1.0f, y);
            gesture.addStroke(new GestureDescription.StrokeDescription(path, 0, duration));
        }

        dispatching = true;
        boolean accepted = dispatchGesture(gesture.build(), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                dispatching = false;
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                dispatching = false;
            }
        }, new Handler(Looper.getMainLooper()));
        if (!accepted) dispatching = false;
    }

    private void removeOverlay() {
        if (floatingButton != null && windowManager != null) {
            try { windowManager.removeView(floatingButton); } catch (Exception ignored) {}
        }
        floatingButton = null;
        buttonParams = null;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Intentionally does not inspect app content or accessibility node trees.
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        removeOverlay();
        if (receiverRegistered) {
            try { unregisterReceiver(refreshReceiver); } catch (Exception ignored) {}
            receiverRegistered = false;
        }
        super.onDestroy();
    }
}
