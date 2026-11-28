package com.verity.livingdesk;

import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

public final class SeasonWallpaperService extends WallpaperService {
    @Override public Engine onCreateEngine() {
        return new SeasonEngine();
    }

    private final class SeasonEngine extends Engine {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final SeasonRenderer renderer = new SeasonRenderer();
        private boolean visible = false;
        private long startedAt = System.currentTimeMillis();
        private long lastSettingsRead = 0L;

        private final Runnable drawRunner = new Runnable() {
            @Override public void run() {
                drawFrame();
            }
        };

        SeasonEngine() {
            setTouchEventsEnabled(false);
            updateSeason();
        }

        @Override public void onVisibilityChanged(boolean isVisible) {
            visible = isVisible;
            if (visible) drawFrame();
            else handler.removeCallbacks(drawRunner);
        }

        @Override public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            drawFrame();
        }

        @Override public void onSurfaceDestroyed(SurfaceHolder holder) {
            visible = false;
            handler.removeCallbacks(drawRunner);
            super.onSurfaceDestroyed(holder);
        }

        @Override public void onDestroy() {
            visible = false;
            handler.removeCallbacks(drawRunner);
            super.onDestroy();
        }

        private void updateSeason() {
            String season = getSharedPreferences("living_desk_settings", MODE_PRIVATE).getString("season", "auto");
            renderer.setSeason(season);
        }

        private void drawFrame() {
            handler.removeCallbacks(drawRunner);
            if (!visible) return;
            long now = System.currentTimeMillis();
            if (now - lastSettingsRead > 1500L) {
                lastSettingsRead = now;
                updateSeason();
            }
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    renderer.draw(canvas, canvas.getWidth(), canvas.getHeight(), now - startedAt);
                }
            } catch (IllegalArgumentException ignored) {
            } finally {
                if (canvas != null) {
                    try { holder.unlockCanvasAndPost(canvas); } catch (Exception ignored) { }
                }
            }
            if (visible) handler.postDelayed(drawRunner, 33L);
        }
    }
}
