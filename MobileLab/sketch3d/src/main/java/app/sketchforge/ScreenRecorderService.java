package app.sketchforge;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.*;
import android.provider.MediaStore;
import android.net.Uri;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ScreenRecorderService extends Service {
    static final String ACTION_START="app.sketchforge.RECORD_START";
    static final String ACTION_STOP="app.sketchforge.RECORD_STOP";
    static final String EXTRA_RESULT_CODE="projection_result";
    static final String EXTRA_DATA="projection_data";
    private static final String CHANNEL="sketchforge_recording";
    private static final int NOTIFICATION_ID=8134;
    private MediaRecorder recorder;
    private MediaProjection projection;
    private VirtualDisplay display;
    private ParcelFileDescriptor pfd;
    private Uri outputUri;
    private File outputFile;
    private boolean stopping=false;

    @Override public void onCreate(){super.onCreate();createChannel();}
    private void createChannel(){
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel ch=new NotificationChannel(CHANNEL,"SketchForge video recording",NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Состояние записи видео SketchForge");
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
    }
    private Notification notification(String text){
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        return b.setSmallIcon(android.R.drawable.presence_video_online).setContentTitle("SketchForge").setContentText(text).setOngoing(true).build();
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null){stopSelf();return START_NOT_STICKY;}
        if(ACTION_STOP.equals(intent.getAction())){stopRecording(false);stopSelf();return START_NOT_STICKY;}
        if(ACTION_START.equals(intent.getAction()))startRecording(intent);
        return START_NOT_STICKY;
    }
    private void startRecording(Intent intent){
        try{
            if(Build.VERSION.SDK_INT>=29)startForeground(NOTIFICATION_ID,notification("Готовим запись MP4…"),ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
            else startForeground(NOTIFICATION_ID,notification("Готовим запись MP4…"));
            int result=intent.getIntExtra(EXTRA_RESULT_CODE,0);
            Intent data=intent.getParcelableExtra(EXTRA_DATA);
            if(data==null)throw new IllegalStateException("Нет разрешения записи экрана");
            MediaProjectionManager manager=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
            projection=manager.getMediaProjection(result,data);
            if(projection==null)throw new IllegalStateException("Не получен MediaProjection");

            android.util.DisplayMetrics metrics=new android.util.DisplayMetrics();
            ((WindowManager)getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(metrics);
            int width=Math.min(720,metrics.widthPixels);
            int height=Math.max(2,(int)(metrics.heightPixels*(width/(float)metrics.widthPixels)));
            width=Math.max(2,width-(width%2));height=Math.max(2,height-(height%2));

            prepareOutput();
            recorder=new MediaRecorder();
            recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            recorder.setVideoSize(width,height);
            recorder.setVideoFrameRate(30);
            recorder.setVideoEncodingBitRate(3500000);
            if(outputUri!=null&&pfd!=null)recorder.setOutputFile(pfd.getFileDescriptor());
            else recorder.setOutputFile(outputFile.getAbsolutePath());
            recorder.prepare();

            display=projection.createVirtualDisplay("SketchForgeRecorder",width,height,metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,recorder.getSurface(),null,null);
            projection.registerCallback(new MediaProjection.Callback(){
                @Override public void onStop(){stopRecording(true);stopSelf();}
            },new Handler(Looper.getMainLooper()));
            recorder.start();
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID,notification("Идёт запись MP4 · "+width+"×"+height));
        }catch(Exception e){
            stopRecording(false);
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID,notification("Не удалось начать запись: "+e.getClass().getSimpleName()));
            stopForeground(true);stopSelf();
        }
    }
    private void prepareOutput()throws Exception{
        String name="SketchForge_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date())+".mp4";
        if(Build.VERSION.SDK_INT>=29){
            ContentValues v=new ContentValues();
            v.put(MediaStore.Video.Media.DISPLAY_NAME,name);
            v.put(MediaStore.Video.Media.MIME_TYPE,"video/mp4");
            v.put(MediaStore.Video.Media.RELATIVE_PATH,"Movies/SketchForge");
            v.put(MediaStore.Video.Media.IS_PENDING,1);
            outputUri=getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,v);
            if(outputUri==null)throw new IllegalStateException("Не удалось создать MP4 в Movies");
            pfd=getContentResolver().openFileDescriptor(outputUri,"w");
            if(pfd==null)throw new IllegalStateException("Не удалось открыть видеофайл");
        }else{
            File dir=new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES),"SketchForge");
            if(!dir.exists()&&!dir.mkdirs())throw new IllegalStateException("Не удалось создать папку видео");
            outputFile=new File(dir,name);
        }
    }
    private void stopRecording(boolean projectionAlreadyStopped){
        if(stopping)return;stopping=true;
        try{if(recorder!=null){try{recorder.stop();}catch(Exception ignored){}recorder.reset();recorder.release();recorder=null;}}catch(Exception ignored){}
        try{if(display!=null){display.release();display=null;}}catch(Exception ignored){}
        try{if(projection!=null){if(!projectionAlreadyStopped)projection.stop();projection=null;}}catch(Exception ignored){}
        try{if(pfd!=null){pfd.close();pfd=null;}}catch(Exception ignored){}
        try{
            if(outputUri!=null){
                ContentValues done=new ContentValues();if(Build.VERSION.SDK_INT>=29)done.put(MediaStore.Video.Media.IS_PENDING,0);
                getContentResolver().update(outputUri,done,null,null);
                getSharedPreferences("sketchforge_video",0).edit().putString("last_uri",outputUri.toString()).apply();
                outputUri=null;
            }else if(outputFile!=null&&outputFile.exists()){
                getSharedPreferences("sketchforge_video",0).edit().putString("last_uri",Uri.fromFile(outputFile).toString()).apply();
                outputFile=null;
            }
        }catch(Exception ignored){}
        stopForeground(true);
        stopping=false;
    }
    @Override public void onDestroy(){stopRecording(false);super.onDestroy();}
    @Override public android.os.IBinder onBind(Intent i){return null;}
}