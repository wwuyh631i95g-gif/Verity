package com.verity.rastaponio;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

public class MainActivity extends Activity {
    private final Handler handler = new Handler();
    private FrameLayout root;
    private VideoView video;
    private ImageView shadow;
    private View gameOver;
    private int lane = 1;
    private boolean stopped;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!stopped && video != null && video.isPlaying()
                    && hit(video.getCurrentPosition(), lane)) {
                endGame();
                return;
            }
            handler.postDelayed(this, 70);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideBars();

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);

        video = new VideoView(this);
        root.addView(video, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        video.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.traffic_clip));
        video.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            mp.setVolume(0f, 0f);
            video.start();
            handler.removeCallbacks(tick);
            handler.post(tick);
        });

        shadow = new ImageView(this);
        shadow.setImageResource(R.drawable.shadow_player);
        shadow.setScaleType(ImageView.ScaleType.FIT_CENTER);
        FrameLayout.LayoutParams shadowParams =
                new FrameLayout.LayoutParams(dp(72), dp(142), Gravity.TOP | Gravity.LEFT);
        root.addView(shadow, shadowParams);
        root.post(this::placeShadow);

        button("‹", true);
        button("›", false);

        TextView title = new TextView(this);
        title.setText("RASTAPONIO");
        title.setTextColor(Color.WHITE);
        title.setTextSize(13);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setLetterSpacing(.14f);
        title.setShadowLayer(5f, 0f, 2f, Color.BLACK);
        FrameLayout.LayoutParams titleParams =
                new FrameLayout.LayoutParams(-2, dp(34), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        titleParams.topMargin = dp(16);
        root.addView(title, titleParams);
    }

    private void hideBars() {
        Window window = getWindow();
        window.setStatusBarColor(Color.BLACK);
        window.setNavigationBarColor(Color.BLACK);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        if (focused) hideBars();
    }

    private void button(String label, boolean left) {
        TextView control = new TextView(this);
        control.setText(label);
        control.setTextColor(Color.WHITE);
        control.setTextSize(39);
        control.setGravity(Gravity.CENTER);
        control.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        control.setBackgroundColor(Color.argb(195, 14, 22, 30));
        control.setElevation(dp(8));
        control.setOnClickListener(view -> {
            if (stopped) return;
            lane = Math.max(0, Math.min(2, lane + (left ? -1 : 1)));
            placeShadow();
        });

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(82), dp(70),
                left ? Gravity.BOTTOM | Gravity.LEFT : Gravity.BOTTOM | Gravity.RIGHT);
        params.leftMargin = dp(28);
        params.rightMargin = dp(28);
        params.bottomMargin = dp(34);
        root.addView(control, params);
    }

    private void placeShadow() {
        if (root == null || shadow == null) return;
        int width = root.getWidth();
        int height = root.getHeight();
        if (width <= 0 || height <= 0) return;
        float[] laneX = {.27f, .50f, .73f};
        shadow.setX(laneX[lane] * width - dp(36));
        shadow.setY(height * .515f - dp(71));
    }

    // Collision windows are aligned to the visible cars crossing the shadow's row
    // in the supplied clip. The player can move between the three lanes.
    private boolean hit(int ms, int selectedLane) {
        if (ms >= 4650 && ms <= 5550 && selectedLane == 1) return true;
        if (ms >= 5750 && ms <= 7150 && (selectedLane == 0 || selectedLane == 2)) return true;
        if (ms >= 7350 && ms <= 8250 && (selectedLane == 0 || selectedLane == 2)) return true;
        return ms >= 8650 && ms <= 9800 && selectedLane == 0;
    }

    private void endGame() {
        stopped = true;
        video.pause();
        shadow.setVisibility(View.INVISIBLE);

        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.argb(190, 0, 0, 0));
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
        gameOver = overlay;

        TextView collision = new TextView(this);
        collision.setText("СТОЛКНОВЕНИЕ!");
        collision.setTextColor(Color.WHITE);
        collision.setTextSize(24);
        collision.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        collision.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams collisionParams =
                new FrameLayout.LayoutParams(-1, dp(64), Gravity.CENTER);
        collisionParams.bottomMargin = dp(90);
        overlay.addView(collision, collisionParams);

        TextView restart = new TextView(this);
        restart.setText("ЗАНОВО");
        restart.setTextColor(Color.WHITE);
        restart.setTextSize(20);
        restart.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        restart.setGravity(Gravity.CENTER);
        restart.setBackgroundColor(Color.rgb(210, 43, 53));
        overlay.addView(restart, new FrameLayout.LayoutParams(dp(220), dp(62), Gravity.CENTER));
        restart.setOnClickListener(view -> restartGame());
    }

    private void restartGame() {
        stopped = false;
        lane = 1;
        if (gameOver != null) root.removeView(gameOver);
        gameOver = null;
        shadow.setVisibility(View.VISIBLE);
        placeShadow();
        video.seekTo(0);
        video.start();
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, 70);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
        if (video != null) video.pause();
    }

    @Override protected void onResume() {
        super.onResume();
        if (video != null && !stopped) {
            video.start();
            handler.removeCallbacks(tick);
            handler.post(tick);
        }
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (video != null) video.stopPlayback();
        super.onDestroy();
    }
}
