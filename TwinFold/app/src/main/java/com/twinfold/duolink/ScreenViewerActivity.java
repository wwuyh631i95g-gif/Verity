package com.twinfold.duolink;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class ScreenViewerActivity extends Activity {
    public static volatile Bitmap latestFrame;
    private static volatile ScreenViewerActivity active;
    private ImageView image;
    private boolean controlEnabled = false;
    private float downX;
    private float downY;
    private long downAt;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        controlEnabled = getIntent().getBooleanExtra("split_mode", false);
        showViewer();
    }

    private void showViewer() {
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.FIT_XY);
        FrameLayout.LayoutParams imageLp = new FrameLayout.LayoutParams(-1, -1);
        imageLp.topMargin = dp(44);
        root.addView(image, imageLp);

        TextView label = new TextView(this);
        label.setText(controlEnabled
                ? "TWINFOLD  •  ВТОРАЯ ПОЛОВИНА  •  касания передаются на первый телефон  •  нажмите здесь для выхода"
                : "TWINFOLD  •  ПРОСМОТР ПЕРЕДАВАЕМОГО ЭКРАНА  •  нажмите здесь для выхода");
        label.setTextColor(Color.WHITE);
        label.setTextSize(10);
        label.setGravity(Gravity.CENTER);
        label.setPadding(dp(10), 0, dp(10), 0);
        label.setBackgroundColor(0xB3000000);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, dp(44), Gravity.TOP);
        root.addView(label, lp);

        label.setOnClickListener(v -> closeViewer());
        image.setOnTouchListener((v, event) -> handleTouch(event));
        setContentView(root);
        active = this;
        if (latestFrame != null) image.setImageBitmap(latestFrame);
    }

    private boolean handleTouch(MotionEvent event) {
        if (!controlEnabled) return false;
        float width = Math.max(1f, image.getWidth());
        float height = Math.max(1f, image.getHeight());
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = clamp(event.getX() / width);
                downY = clamp(event.getY() / height);
                downAt = event.getEventTime();
                return true;
            case MotionEvent.ACTION_MOVE:
                return true;
            case MotionEvent.ACTION_UP:
                float x = clamp(event.getX() / width);
                float y = clamp(event.getY() / height);
                long duration = Math.max(50L, Math.min(1200L, event.getEventTime() - downAt));
                MainActivity.sendSplitGesture(downX, downY, x, y, duration);
                return true;
            case MotionEvent.ACTION_CANCEL:
                return true;
            default:
                return true;
        }
    }

    private float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private void closeViewer() {
        if (controlEnabled) MainActivity.leaveSplitViewer();
        finish();
    }

    public static void updateIfVisible(Bitmap bitmap) {
        ScreenViewerActivity viewer = active;
        if (viewer != null && viewer.image != null) {
            viewer.runOnUiThread(() -> {
                if (viewer.image != null) viewer.image.setImageBitmap(bitmap);
            });
        }
    }

    public static void finishFromPeer() {
        ScreenViewerActivity viewer = active;
        if (viewer != null) viewer.runOnUiThread(viewer::finish);
    }

    private int dp(int value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        controlEnabled = intent.getBooleanExtra("split_mode", false);
        if (controlEnabled) recreate();
    }

    @Override public void onBackPressed() {
        closeViewer();
    }

    @Override protected void onDestroy() {
        if (active == this) active = null;
        super.onDestroy();
    }
}
