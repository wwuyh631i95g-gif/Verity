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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class ScreenViewerActivity extends Activity {
    public static volatile Bitmap latestFrame;
    private static volatile ScreenViewerActivity active;
    private ImageView image;
    private boolean controlEnabled = false;
    private float downX;
    private float downY;
    private long downAt;
    private final LinkedHashMap<Integer, TouchTrack> activeTracks = new LinkedHashMap<>();
    private final ArrayList<TouchTrack> completedTracks = new ArrayList<>();

    private static final class TouchTrack {
        final long downAt;
        long upAt;
        final ArrayList<float[]> points = new ArrayList<>();
        TouchTrack(long time) { downAt = time; upAt = time; }
        void add(float x, float y, long time) {
            upAt = time;
            if (points.isEmpty()) {
                points.add(new float[]{x, y});
                return;
            }
            float[] last = points.get(points.size() - 1);
            float dx = x - last[0];
            float dy = y - last[1];
            if ((dx * dx + dy * dy) >= 0.000016f || points.size() == 1) {
                if (points.size() < 120) points.add(new float[]{x, y});
                else if ((points.size() % 3) == 0) points.set(points.size() - 1, new float[]{x, y});
            }
        }
    }

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
        final int action = event.getActionMasked();
        final long now = event.getEventTime();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                activeTracks.clear();
                completedTracks.clear();
                startTrack(event, event.getActionIndex(), now);
                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                updateExistingTracks(event, now);
                startTrack(event, event.getActionIndex(), now);
                return true;

            case MotionEvent.ACTION_MOVE:
                updateExistingTracks(event, now);
                return true;

            case MotionEvent.ACTION_POINTER_UP:
                updateExistingTracks(event, now);
                finishTrack(event.getPointerId(event.getActionIndex()), now);
                return true;

            case MotionEvent.ACTION_UP:
                updateExistingTracks(event, now);
                finishTrack(event.getPointerId(event.getActionIndex()), now);
                sendCompletedGesture();
                return true;

            case MotionEvent.ACTION_CANCEL:
                activeTracks.clear();
                completedTracks.clear();
                return true;

            default:
                return true;
        }
    }

    private void startTrack(MotionEvent event, int index, long time) {
        int id = event.getPointerId(index);
        TouchTrack track = new TouchTrack(time);
        track.add(clamp(event.getX(index) / Math.max(1f, image.getWidth())),
                clamp(event.getY(index) / Math.max(1f, image.getHeight())), time);
        activeTracks.put(id, track);
    }

    private void updateExistingTracks(MotionEvent event, long time) {
        for (int i = 0; i < event.getPointerCount(); i++) {
            int id = event.getPointerId(i);
            TouchTrack track = activeTracks.get(id);
            if (track != null) {
                track.add(clamp(event.getX(i) / Math.max(1f, image.getWidth())),
                        clamp(event.getY(i) / Math.max(1f, image.getHeight())), time);
            }
        }
    }

    private void finishTrack(int id, long time) {
        TouchTrack track = activeTracks.remove(id);
        if (track != null) {
            track.upAt = time;
            completedTracks.add(track);
        }
    }

    private void sendCompletedGesture() {
        if (completedTracks.isEmpty()) return;
        long gestureStart = Long.MAX_VALUE;
        for (TouchTrack track : completedTracks) gestureStart = Math.min(gestureStart, track.downAt);
        StringBuilder payload = new StringBuilder();
        int maxTracks = Math.min(completedTracks.size(), 10);
        for (int i = 0; i < maxTracks; i++) {
            TouchTrack track = completedTracks.get(i);
            if (i > 0) payload.append(';');
            long offset = Math.max(0L, track.downAt - gestureStart);
            long duration = Math.max(50L, Math.min(60000L - offset, track.upAt - track.downAt));
            payload.append(offset).append(',').append(duration);
            ArrayList<float[]> pts = track.points;
            for (int n = 0; n < pts.size(); n++) {
                float[] point = pts.get(n);
                payload.append(',').append(Math.round(clamp(point[0]) * 10000f));
                payload.append(',').append(Math.round(clamp(point[1]) * 10000f));
            }
        }
        MainActivity.sendSplitGestureBatch(payload.toString());
        completedTracks.clear();
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
