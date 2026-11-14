package app.sandlens;

import android.Manifest;
import android.app.Activity;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.hardware.camera2.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity implements TextureView.SurfaceTextureListener {
    TextureView cameraView; TerrainOverlay overlay; TextView status; CameraDevice camera; CameraCaptureSession session;
    HandlerThread thread; Handler handler; String cameraId; boolean front=false, mapOn=true, contours=true, water=true;
    Runnable sampler;
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(0xff111d31);getWindow().setNavigationBarColor(0xff111d31);
        FrameLayout frame=new FrameLayout(this);frame.setBackgroundColor(0xff14243a);
        cameraView=new TextureView(this);cameraView.setSurfaceTextureListener(this);frame.addView(cameraView,new FrameLayout.LayoutParams(-1,-1));
        overlay=new TerrainOverlay();frame.addView(overlay,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(8,8,8,8);
        panel.setBackgroundColor(0xaa111d31);
        TextView title=new TextView(this);title.setText("SANDLENS  •  карта рельефа с камеры");title.setTextColor(-1);title.setTextSize(16);panel.addView(title);
        status=new TextView(this);status.setText("Разреши камеру и направь её на песок или рельеф.");status.setTextColor(0xffdae6f7);status.setTextSize(11);panel.addView(status);
        LinearLayout controls=new LinearLayout(this);
        controls.addView(btn("Карта",()->{mapOn=!mapOn;overlay.mapOn=mapOn;overlay.invalidate();}));
        controls.addView(btn("Изолинии",()->{contours=!contours;overlay.contours=contours;overlay.invalidate();}));
        controls.addView(btn("Вода",()->{water=!water;overlay.water=water;overlay.invalidate();}));
        controls.addView(btn("Камера",()->{front=!front;closeCamera();openCameraIfReady();}));
        panel.addView(controls);
        FrameLayout.LayoutParams pp=new FrameLayout.LayoutParams(-1,-2,Gravity.TOP);frame.addView(panel,pp);
        TextView note=new TextView(this);note.setText("Оценка по светотени, не настоящий 3D-скан. Проекция на песок не выполняется.");note.setTextColor(0xfff5dfb1);note.setTextSize(11);note.setPadding(10,8,10,8);note.setBackgroundColor(0xaa111d31);
        FrameLayout.LayoutParams np=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);frame.addView(note,np);
        setContentView(frame);
        sampler=()->{if(cameraView!=null&&cameraView.isAvailable()){try{Bitmap bmap=cameraView.getBitmap(100,140);if(bmap!=null){overlay.setSource(bmap);bmap.recycle();}}catch(Exception ignored){}} if(handler!=null)handler.postDelayed(sampler,420);};
    }
    View btn(String text,Runnable r){Button b=new Button(this);b.setText(text);b.setTextSize(10);b.setAllCaps(false);b.setTextColor(-1);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff365779));b.setOnClickListener(v->r.run());return b;}
    @Override protected void onResume(){super.onResume();startThread();if(cameraView.isAvailable())openCameraIfReady();}
    @Override protected void onPause(){closeCamera();stopThread();super.onPause();}
    void startThread(){if(thread==null){thread=new HandlerThread("SandLensCamera");thread.start();handler=new Handler(thread.getLooper());handler.postDelayed(sampler,500);}}
    void stopThread(){if(thread!=null){thread.quitSafely();try{thread.join();}catch(InterruptedException ignored){}thread=null;handler=null;}}
    boolean hasPermission(){return checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED;}
    void openCameraIfReady(){if(!hasPermission()){requestPermissions(new String[]{Manifest.permission.CAMERA},70);status.setText("Нужно разрешение камеры.");return;}
        if(!cameraView.isAvailable()||handler==null)return;
        try{CameraManager m=(CameraManager)getSystemService(CAMERA_SERVICE);cameraId=null;
            for(String id:m.getCameraIdList()){CameraCharacteristics ch=m.getCameraCharacteristics(id);Integer facing=ch.get(CameraCharacteristics.LENS_FACING);if(facing!=null&&facing==(front?CameraCharacteristics.LENS_FACING_FRONT:CameraCharacteristics.LENS_FACING_BACK)){cameraId=id;break;}}
            if(cameraId==null){status.setText("Нужная камера не найдена.");return;}
            m.openCamera(cameraId,new CameraDevice.StateCallback(){
                @Override public void onOpened(CameraDevice c){camera=c;startPreview();}
                @Override public void onDisconnected(CameraDevice c){c.close();camera=null;}
                @Override public void onError(CameraDevice c,int e){c.close();camera=null;runOnUiThread(()->status.setText("Ошибка камеры: "+e));}
            },handler);
            status.setText(front?"Фронтальная камера • включена":"Основная камера • включена");
        }catch(Exception e){status.setText("Не удалось запустить камеру: "+e.getClass().getSimpleName());}
    }
    void startPreview(){try{SurfaceTexture st=cameraView.getSurfaceTexture();if(st==null)return;st.setDefaultBufferSize(640,480);Surface surface=new Surface(st);
        CaptureRequest.Builder req=camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);req.addTarget(surface);req.set(CaptureRequest.CONTROL_MODE,CameraMetadata.CONTROL_MODE_AUTO);
        camera.createCaptureSession(Collections.singletonList(surface),new CameraCaptureSession.StateCallback(){
            @Override public void onConfigured(CameraCaptureSession s){session=s;try{s.setRepeatingRequest(req.build(),null,handler);}catch(Exception ignored){}}
            @Override public void onConfigureFailed(CameraCaptureSession s){runOnUiThread(()->status.setText("Не удалось настроить предпросмотр."));}
        },handler);
    }catch(Exception e){runOnUiThread(()->status.setText("Ошибка предпросмотра."));}}
    void closeCamera(){try{if(session!=null){session.close();session=null;}if(camera!=null){camera.close();camera=null;}}catch(Exception ignored){}}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==70&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)openCameraIfReady();else status.setText("Доступ к камере не выдан.");}
    @Override public void onSurfaceTextureAvailable(SurfaceTexture s,int w,int h){openCameraIfReady();}
    @Override public void onSurfaceTextureSizeChanged(SurfaceTexture s,int w,int h){}
    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture s){closeCamera();return true;}
    @Override public void onSurfaceTextureUpdated(SurfaceTexture s){}

    class TerrainOverlay extends View {
        float[][] height=new float[140][100]; boolean hasData=false; boolean mapOn=true,contours=true,water=true; Paint p=new Paint(3);
        TerrainOverlay(){super(MainActivity.this);setWillNotDraw(false);}
        void setSource(Bitmap b){int w=b.getWidth(),h=b.getHeight();float[][] lum=new float[h][w];int min=255,max=0;
            for(int y=0;y<h;y++)for(int x=0;x<w;x++){int c=b.getPixel(x,y);int v=(int)(.2126*Color.red(c)+.7152*Color.green(c)+.0722*Color.blue(c));lum[y][x]=v;min=Math.min(min,v);max=Math.max(max,v);}
            float span=Math.max(35,max-min);
            for(int y=0;y<h;y++)for(int x=0;x<w;x++){float avg=0;int count=0;for(int yy=Math.max(0,y-1);yy<=Math.min(h-1,y+1);yy++)for(int xx=Math.max(0,x-1);xx<=Math.min(w-1,x+1);xx++){avg+=lum[yy][xx];count++;}
                float local=lum[y][x]-avg/count;float v=(lum[y][x]-min)/span;float estimate=clamp(.12f+v*.72f+local/110f,.02f,.98f);height[y][x]=estimate;}
            hasData=true;postInvalidate();
        }
        float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}
        int terrain(float h){if(h<.22f){float a=h/.22f;return Color.rgb(10,(int)(60+100*a),(int)(145+100*a));}
            if(h<.48f){float a=(h-.22f)/.26f;return Color.rgb((int)(20+40*a),(int)(150+75*a),(int)(90-15*a));}
            if(h<.72f){float a=(h-.48f)/.24f;return Color.rgb((int)(70+170*a),(int)(220-10*a),(int)(75-55*a));}
            float a=(h-.72f)/.28f;return Color.rgb((int)(185+55*a),(int)(170-85*a),(int)(95-70*a));}
        @Override protected void onDraw(Canvas c){super.onDraw(c);if(!mapOn||!hasData)return;int w=height[0].length,h=height.length;float cw=(float)getWidth()/w,ch=(float)getHeight()/h;
            p.setStyle(Paint.Style.FILL);
            for(int y=0;y<h;y++)for(int x=0;x<w;x++){float v=height[y][x];int col=terrain(v);if(water&&v<.18f)col=0xff168eff;p.setColor(col);p.setAlpha(138);c.drawRect(x*cw,y*ch,(x+1)*cw+1,(y+1)*ch+1,p);}
            if(contours){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.25f);p.setColor(0xddf8f7e9);
                for(int y=1;y<h-1;y++)for(int x=1;x<w-1;x++){float v=height[y][x];int band=(int)(v*12);if((int)(height[y+1][x]*12)!=band||(int)(height[y][x+1]*12)!=band){c.drawPoint((x+.5f)*cw,(y+.5f)*ch,p);}}}
            p.setStyle(Paint.Style.FILL);p.setColor(0xfff5f4ed);p.setAlpha(230);p.setTextSize(12);c.drawText("ПРИБЛИЗИТЕЛЬНАЯ ВЫСОТА",12,getHeight()-18,p);
        }
    }
}
