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
    private TextView comboButton;
    private WindowManager.LayoutParams buttonParams;
    private boolean receiverRegistered;
    private boolean dispatching;

    private final BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            updateOverlay();
        }
    };

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        windowManager = (WindowManager)getSystemService(WINDOW_SERVICE);
        registerReceiver(refreshReceiver, new IntentFilter(MainActivity.ACTION_REFRESH));
        receiverRegistered = true;
        updateOverlay();
    }

    private int dp(float value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable buttonBackground(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(18));
        d.setStroke(dp(2), Color.WHITE);
        return d;
    }

    private void updateOverlay() {
        SharedPreferences p = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE);
        if (!p.getBoolean("overlay_enabled", true)) {
            removeOverlay();
            return;
        }
        if (windowManager == null) return;
        if (comboButton != null) {
            comboButton.setText("A+B");
            return;
        }

        comboButton = new TextView(this);
        comboButton.setText("A+B");
        comboButton.setTextColor(Color.WHITE);
        comboButton.setTextSize(17);
        comboButton.setTypeface(null, 1);
        comboButton.setGravity(Gravity.CENTER);
        comboButton.setElevation(dp(5));
        comboButton.setBackground(buttonBackground(Color.rgb(27, 111, 205)));

        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        int size = dp(64);
        buttonParams = new WindowManager.LayoutParams(
                size, size,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        buttonParams.gravity = Gravity.TOP | Gravity.LEFT;
        buttonParams.x = Math.max(0, metrics.widthPixels - size - dp(8));
        buttonParams.y = Math.max(dp(70), Math.min(dp(160), metrics.heightPixels - size - dp(8)));

        comboButton.setOnTouchListener(new View.OnTouchListener() {
            private float downX, downY;
            private int startX, startY;
            private boolean moved;

            @Override public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getRawX();
                        downY = event.getRawY();
                        startX = buttonParams.x;
                        startY = buttonParams.y;
                        moved = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - downX;
                        float dy = event.getRawY() - downY;
                        if (Math.abs(dx) > dp(5) || Math.abs(dy) > dp(5)) moved = true;
                        if (moved) {
                            DisplayMetrics dm = new DisplayMetrics();
                            windowManager.getDefaultDisplay().getRealMetrics(dm);
                            buttonParams.x = Math.max(0, Math.min(dm.widthPixels - buttonParams.width,
                                    startX + (int)dx));
                            buttonParams.y = Math.max(0, Math.min(dm.heightPixels - buttonParams.height,
                                    startY + (int)dy));
                            try { windowManager.updateViewLayout(comboButton, buttonParams); }
                            catch (Exception ignored) {}
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (!moved) sendPair();
                        return true;
                }
                return true;
            }
        });

        try {
            windowManager.addView(comboButton, buttonParams);
        } catch (Exception error) {
            comboButton = null;
            buttonParams = null;
        }
    }

    // Two stationary strokes, same start time and duration, inside one gesture.
    // This is one simultaneous A+B input, never two sequential taps or a repeat loop.
    private void sendPair() {
        if (dispatching || windowManager == null) return;
        SharedPreferences p = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE);
        int duration = Math.max(100, Math.min(2000, p.getInt("duration_ms", 300)));

        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        int width = Math.max(2, metrics.widthPixels);
        int height = Math.max(2, metrics.heightPixels);

        float ax = (width - 1) * p.getInt("x1", 18) / 100.0f;
        float ay = (height - 1) * p.getInt("y1", 82) / 100.0f;
        float bx = (width - 1) * p.getInt("x2", 82) / 100.0f;
        float by = (height - 1) * p.getInt("y2", 82) / 100.0f;

        Path pointA = new Path();
        pointA.moveTo(ax, ay);
        Path pointB = new Path();
        pointB.moveTo(bx, by);

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(pointA, 0, duration));
        builder.addStroke(new GestureDescription.StrokeDescription(pointB, 0, duration));

        dispatching = true;
        if (comboButton != null) {
            comboButton.setText("…");
            comboButton.setBackground(buttonBackground(Color.rgb(24, 145, 101)));
        }

        boolean accepted = dispatchGesture(builder.build(), new GestureResultCallback() {
            @Override public void onCompleted(GestureDescription gesture) { finishDispatch(); }
            @Override public void onCancelled(GestureDescription gesture) { finishDispatch(); }
        }, new Handler(Looper.getMainLooper()));
        if (!accepted) finishDispatch();
    }

    private void finishDispatch() {
        dispatching = false;
        if (comboButton != null) {
            comboButton.setText("A+B");
            comboButton.setBackground(buttonBackground(Color.rgb(27, 111, 205)));
        }
    }

    private void removeOverlay() {
        if (comboButton != null && windowManager != null) {
            try { windowManager.removeView(comboButton); } catch (Exception ignored) {}
        }
        comboButton = null;
        buttonParams = null;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        // Does not inspect app content or accessibility nodes.
    }

    @Override public void onInterrupt() {}

    @Override public void onDestroy() {
        removeOverlay();
        if (receiverRegistered) {
            try { unregisterReceiver(refreshReceiver); } catch (Exception ignored) {}
            receiverRegistered = false;
        }
        super.onDestroy();
    }
}
