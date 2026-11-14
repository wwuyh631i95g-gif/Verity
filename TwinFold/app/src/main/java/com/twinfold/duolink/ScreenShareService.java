package com.twinfold.duolink;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class ScreenShareService extends Service {
    public static final String ACTION_START = "com.twinfold.duolink.SCREEN_START";
    public static final String ACTION_STOP = "com.twinfold.duolink.SCREEN_STOP";
    public static final String ACTION_FRAME = "com.twinfold.duolink.SCREEN_FRAME";
    public static final String EXTRA_RESULT_CODE = "result_code";
    public static final String EXTRA_RESULT_DATA = "result_data";
    public static final String EXTRA_SIDE = "remote_side";
    public static final String EXTRA_FRAME = "jpeg_frame";

    private static final String CHANNEL = "twinfold_screen_capture";
    private static final int NOTIFICATION_ID = 7712;
    private MediaProjection projection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private HandlerThread workerThread;
    private Handler worker;
    private int width;
    private int height;
    private int side = 0;
    private long lastFrameAt = 0;
    private volatile boolean stopped = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        if (ACTION_STOP.equals(intent.getAction())) {
            stopCapture();
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!ACTION_START.equals(intent.getAction())) return START_NOT_STICKY;
        int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
        Intent resultData = intent.getParcelableExtra(EXTRA_RESULT_DATA);
        side = intent.getIntExtra(EXTRA_SIDE, 0);
        try {
            createChannel();
            Notification notification = buildNotification();
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
            MediaProjectionManager manager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            projection = manager.getMediaProjection(resultCode, resultData);
            if (projection == null) throw new IllegalStateException("MediaProjection unavailable");
            projection.registerCallback(new MediaProjection.Callback() {
                @Override public void onStop() {
                    stopCapture();
                    stopSelf();
                }
            }, new Handler(getMainLooper()));

            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            wm.getDefaultDisplay().getRealMetrics(metrics);
            width = metrics.widthPixels;
            height = metrics.heightPixels;
            int density = metrics.densityDpi;

            workerThread = new HandlerThread("TwinFoldScreenCapture");
            workerThread.start();
            worker = new Handler(workerThread.getLooper());
            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
            imageReader.setOnImageAvailableListener(this::onImageAvailable, worker);

            virtualDisplay = projection.createVirtualDisplay(
                    "TwinFoldSplitDisplay",
                    width,
                    height,
                    density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.getSurface(),
                    null,
                    worker
            );
        } catch (Exception e) {
            stopCapture();
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private void onImageAvailable(ImageReader reader) {
        if (stopped) return;
        Image image = null;
        Bitmap full = null;
        Bitmap crop = null;
        try {
            long now = System.currentTimeMillis();
            if (now - lastFrameAt < 360) {
                image = reader.acquireLatestImage();
                if (image != null) image.close();
                return;
            }
            image = reader.acquireLatestImage();
            if (image == null) return;
            lastFrameAt = now;

            Image.Plane[] planes = image.getPlanes();
            if (planes == null || planes.length == 0) return;
            ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * width;
            int paddedWidth = width + rowPadding / pixelStride;
            buffer.rewind();
            full = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888);
            full.copyPixelsFromBuffer(buffer);

            int cropX = 0, cropY = 0, cropW = width, cropH = height;
            if (side == 0) { cropX = width / 2; cropW = width - cropX; }       // right half
            else if (side == 1) { cropW = width / 2; }                         // left half
            else if (side == 2) { cropY = height / 2; cropH = height - cropY; }// bottom half
            else if (side == 3) { cropH = height / 2; }                        // top half

            crop = Bitmap.createBitmap(full, cropX, cropY, cropW, cropH);
            int maxSide = Math.max(crop.getWidth(), crop.getHeight());
            if (maxSide > 640) {
                float factor = 640f / maxSide;
                Bitmap scaled = Bitmap.createScaledBitmap(
                        crop,
                        Math.max(1, Math.round(crop.getWidth() * factor)),
                        Math.max(1, Math.round(crop.getHeight() * factor)),
                        true
                );
                if (scaled != crop) {
                    crop.recycle();
                    crop = scaled;
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream(48 * 1024);
            crop.compress(Bitmap.CompressFormat.JPEG, 48, out);
            byte[] jpeg = out.toByteArray();
            if (jpeg.length > 0 && !stopped) {
                Intent frame = new Intent(ACTION_FRAME);
                frame.setPackage(getPackageName());
                frame.putExtra(EXTRA_FRAME, jpeg);
                sendBroadcast(frame);
            }
        } catch (Exception ignored) {
        } finally {
            if (image != null) image.close();
            if (crop != null && !crop.isRecycled()) crop.recycle();
            if (full != null && !full.isRecycled()) full.recycle();
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL, "TwinFold screen sharing", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Visible notification while the screen is being shared.");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 7713, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL)
                : new Notification.Builder(this);
        return builder.setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentTitle("TwinFold — трансляция экрана активна")
                .setContentText("Выбранная часть экрана передаётся подключённому устройству.")
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    private void stopCapture() {
        if (stopped) return;
        stopped = true;
        try { if (virtualDisplay != null) virtualDisplay.release(); } catch (Exception ignored) {}
        virtualDisplay = null;
        try { if (imageReader != null) imageReader.close(); } catch (Exception ignored) {}
        imageReader = null;
        try { if (projection != null) projection.stop(); } catch (Exception ignored) {}
        projection = null;
        if (workerThread != null) {
            try { workerThread.quitSafely(); } catch (Exception ignored) {}
            workerThread = null;
        }
        try { stopForeground(true); } catch (Exception ignored) {}
    }

    @Override public void onDestroy() {
        stopCapture();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
