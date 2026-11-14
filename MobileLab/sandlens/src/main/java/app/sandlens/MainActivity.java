package app.sandlens;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.hardware.camera2.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceTexture;
import android.view.TextureView;
import android.view.View;
import android.view.MotionEvent;
import android.widget.*;
import java.util.Collections;
import java.util.Random;

/** Interactive topographic sand table based on the projected color-height-map effect shown in the supplied clip. */
public class MainActivity extends Activity implements TextureView.SurfaceTextureListener {
    private static final int CAMERA_PERMISSION=71;
    private FrameLayout frame; private TextureView cameraView; private TerrainView terrain; private TextView status;
    private CameraDevice camera; private CameraCaptureSession session; private HandlerThread cameraThread; private Handler cameraHandler;
    private boolean cameraMode=false, lowerMode=false, contours=true, water=true; private Runnable sampleTask;

    @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setStatusBarColor(0xff102535);getWindow().setNavigationBarColor(0xff102535);
        frame=new FrameLayout(this);frame.setBackgroundColor(0xff071d2e);
        cameraView=new TextureView(this);cameraView.setSurfaceTextureListener(this);cameraView.setVisibility(View.GONE);frame.addView(cameraView,new FrameLayout.LayoutParams(-1,-1));
        terrain=new TerrainView();frame.addView(terrain,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(10,7,10,7);panel.setBackgroundColor(0xb9162938);
        TextView title=new TextView(this);title.setText("SAND TABLE  •  интерактивный рельеф");title.setTextSize(16);title.setTypeface(null,1);title.setTextColor(Color.WHITE);panel.addView(title);
        status=new TextView(this);status.setText("Проводи пальцем по карте: создавай холмы и долины.");status.setTextColor(0xffd8e8e9);status.setTextSize(11);panel.addView(status);
        LinearLayout tools=new LinearLayout(this);tools.setGravity(Gravity.CENTER_VERTICAL);
        tools.addView(btn("Поднять",()->{lowerMode=false;terrain.setBrushMode(false);updateStatus();}));
        tools.addView(btn("Опустить",()->{lowerMode=true;terrain.setBrushMode(true);updateStatus();}));
        tools.addView(btn("Вода",()->{water=!water;terrain.setWater(water);updateStatus();}));
        tools.addView(btn("Изолинии",()->{contours=!contours;terrain.setContours(contours);updateStatus();}));
        panel.addView(tools);frame.addView(panel,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));
        LinearLayout bottom=new LinearLayout(this);bottom.setPadding(8,6,8,6);bottom.setGravity(Gravity.CENTER);bottom.setBackgroundColor(0xb9162938);
        bottom.addView(btn("Камера",()->toggleCamera()));bottom.addView(btn("Сбросить",()->terrain.resetTerrain()));bottom.addView(btn("Демо-рельеф",()->terrain.makeDemo()));
        frame.addView(bottom,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));
        TextView disclaimer=new TextView(this);disclaimer.setText("Цветная карта и линии высот — визуализация. Камера телефона не измеряет реальную глубину песка, а сам телефон не проецирует карту на песок.");disclaimer.setTextColor(0xffffe5b2);disclaimer.setTextSize(10);disclaimer.setPadding(9,5,9,5);disclaimer.setBackgroundColor(0xb9162938);FrameLayout.LayoutParams note=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);note.bottomMargin=58;frame.addView(disclaimer,note);
        setContentView(frame);terrain.makeDemo();
        sampleTask=()->{if(cameraMode&&cameraView.isAvailable()){try{Bitmap bm=cameraView.getBitmap(96,128);if(bm!=null){terrain.updateFromCamera(bm);bm.recycle();}}catch(Exception ignored){}}if(cameraHandler!=null)cameraHandler.postDelayed(sampleTask,450);};
    }
    private Button btn(String text,Runnable r){Button b=new Button(this);b.setText(text);b.setTextSize(10);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setPadding(3,0,3,0);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff32657a));b.setOnClickListener(v->r.run());return b;}
    private void updateStatus(){if(cameraMode)status.setText("Камера включена: цветная карта приблизительно следует яркости кадра. Можно корректировать пальцем.");else status.setText(lowerMode?"Режим долин: веди пальцем, чтобы понижать поверхность.":"Режим холмов: веди пальцем, чтобы поднимать поверхность.");}
    private void toggleCamera(){if(cameraMode){cameraMode=false;cameraView.setVisibility(View.GONE);closeCamera();terrain.setCameraOverlay(false);status.setText("Виртуальная песочница. Меняй высоту пальцем.");return;}if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.CAMERA},CAMERA_PERMISSION);return;}startCameraMode();}
    private void startCameraMode(){cameraMode=true;cameraView.setVisibility(View.VISIBLE);terrain.setCameraOverlay(true);startCameraThread();if(cameraView.isAvailable())openCamera();updateStatus();}
    private void startCameraThread(){if(cameraThread==null){cameraThread=new HandlerThread("SandTableCamera");cameraThread.start();cameraHandler=new Handler(cameraThread.getLooper());cameraHandler.postDelayed(sampleTask,500);}}
    private void stopCameraThread(){if(cameraThread!=null){cameraThread.quitSafely();try{cameraThread.join();}catch(InterruptedException ignored){}cameraThread=null;cameraHandler=null;}}
    private void openCamera(){if(!cameraMode||cameraHandler==null||!cameraView.isAvailable())return;try{CameraManager manager=(CameraManager)getSystemService(CAMERA_SERVICE);String id=null;for(String candidate:manager.getCameraIdList()){CameraCharacteristics c=manager.getCameraCharacteristics(candidate);Integer facing=c.get(CameraCharacteristics.LENS_FACING);if(facing!=null&&facing==CameraCharacteristics.LENS_FACING_BACK){id=candidate;break;}}if(id==null){status.setText("Основная камера не найдена.");return;}manager.openCamera(id,new CameraDevice.StateCallback(){@Override public void onOpened(CameraDevice c){camera=c;preview();}@Override public void onDisconnected(CameraDevice c){c.close();camera=null;}@Override public void onError(CameraDevice c,int e){c.close();camera=null;runOnUiThread(()->status.setText("Камера сообщила об ошибке: "+e));}},cameraHandler);}catch(Exception e){status.setText("Не удалось открыть камеру: "+e.getClass().getSimpleName());}}
    private void preview(){try{SurfaceTexture st=cameraView.getSurfaceTexture();if(st==null)return;st.setDefaultBufferSize(640,480);Surface surface=new Surface(st);CaptureRequest.Builder request=camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);request.addTarget(surface);request.set(CaptureRequest.CONTROL_MODE,CameraMetadata.CONTROL_MODE_AUTO);camera.createCaptureSession(Collections.singletonList(surface),new CameraCaptureSession.StateCallback(){@Override public void onConfigured(CameraCaptureSession s){session=s;try{s.setRepeatingRequest(request.build(),null,cameraHandler);}catch(Exception ignored){}}@Override public void onConfigureFailed(CameraCaptureSession s){runOnUiThread(()->status.setText("Не удалось запустить предпросмотр."));}},cameraHandler);}catch(Exception e){runOnUiThread(()->status.setText("Ошибка предпросмотра."));}}
    private void closeCamera(){try{if(session!=null){session.close();session=null;}if(camera!=null){camera.close();camera=null;}}catch(Exception ignored){}}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==CAMERA_PERMISSION&&results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)startCameraMode();else status.setText("Камера не включена. Виртуальная песочница продолжает работать.");}
    @Override public void onSurfaceTextureAvailable(SurfaceTexture surface,int width,int height){if(cameraMode)openCamera();}
    @Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface,int width,int height){}
    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface){closeCamera();return true;}
    @Override public void onSurfaceTextureUpdated(SurfaceTexture surface){}
    @Override protected void onPause(){closeCamera();stopCameraThread();super.onPause();}
    @Override protected void onResume(){super.onResume();if(cameraMode){startCameraThread();if(cameraView.isAvailable())openCamera();}}

    private class TerrainView extends View {
        static final int GW=84,GH=112;float[][] h=new float[GH][GW];Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);boolean drawContours=true,showWater=true,cameraOverlay=false,brushLower=false;float lastX=-1,lastY=-1;
        TerrainView(){super(MainActivity.this);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        void setBrushMode(boolean lower){brushLower=lower;}
        void setWater(boolean on){showWater=on;invalidate();}
        void setContours(boolean on){drawContours=on;invalidate();}
        void setCameraOverlay(boolean on){cameraOverlay=on;invalidate();}
        void resetTerrain(){makeDemo();}
        void makeDemo(){Random rnd=new Random(20261114L);for(int y=0;y<GH;y++)for(int x=0;x<GW;x++){float nx=(x-GW*.5f)/(GW*.40f),ny=(y-GH*.48f)/(GH*.40f);float d=(float)Math.sqrt(nx*nx+ny*ny);float hill=Math.max(0,1-d);float ridge=(float)(Math.sin(x*.21+y*.10)*.035+Math.cos(y*.17-x*.08)*.03);float lake=(float)Math.sqrt(((x-GW*.68f)/(GW*.14f))*((x-GW*.68f)/(GW*.14f))+((y-GH*.70f)/(GH*.12f))*((y-GH*.70f)/(GH*.12f)));h[y][x]=clamp(.18f+hill*.68f+ridge-(lake<1?.22f*(1-lake):0)+rnd.nextFloat()*.012f,.03f,.96f);}invalidate();}
        float clamp(float v,float min,float max){return Math.max(min,Math.min(max,v));}
        void updateFromCamera(Bitmap b){int w=Math.min(GW,b.getWidth()),he=Math.min(GH,b.getHeight());float[][] lum=new float[he][w];for(int y=0;y<he;y++)for(int x=0;x<w;x++){int c=b.getPixel(x,y);lum[y][x]=(Color.red(c)*.2126f+Color.green(c)*.7152f+Color.blue(c)*.0722f)/255f;}float min=1,max=0;for(int y=0;y<he;y++)for(int x=0;x<w;x++){float v=lum[y][x];min=Math.min(min,v);max=Math.max(max,v);}float span=Math.max(.08f,max-min);for(int y=0;y<GH;y++)for(int x=0;x<GW;x++){int xx=Math.min(w-1,x*w/GW),yy=Math.min(he-1,y*he/GH);float center=lum[yy][xx];float edge=0;if(xx+1<w&&yy+1<he)edge=(Math.abs(center-lum[yy][xx+1])+Math.abs(center-lum[yy+1][xx]))*.5f;float inferred=.14f+.72f*((center-min)/span)+edge*.28f;h[y][x]=clamp(h[y][x]*.28f+inferred*.72f,.025f,.97f);}postInvalidate();}
        int terrainColor(float v){if(v<.20f){float t=v/.20f;return Color.rgb(5,(int)(45+85*t),(int)(125+100*t));}if(v<.36f){float t=(v-.20f)/.16f;return Color.rgb((int)(20+35*t),(int)(130+75*t),(int)(115-35*t));}if(v<.62f){float t=(v-.36f)/.26f;return Color.rgb((int)(45+35*t),(int)(205-10*t),(int)(65-20*t));}if(v<.80f){float t=(v-.62f)/.18f;return Color.rgb((int)(85+150*t),(int)(195-10*t),(int)(55-30*t));}float t=(v-.80f)/.20f;return Color.rgb((int)(235-35*t),(int)(175-65*t),(int)(85-35*t));}
        @Override protected void onDraw(Canvas c){super.onDraw(c);if(!cameraOverlay)c.drawColor(0xff061d32);float cw=(float)getWidth()/GW,ch=(float)getHeight()/GH;int alpha=cameraOverlay?158:255;p.setStyle(Paint.Style.FILL);for(int y=0;y<GH;y++)for(int x=0;x<GW;x++){int col=terrainColor(h[y][x]);if(showWater&&h[y][x]<.205f)col=0xff0789d4;p.setColor(col);p.setAlpha(alpha);c.drawRect(x*cw,y*ch,(x+1)*cw+1,(y+1)*ch+1,p);}if(drawContours)drawContourLines(c,cw,ch,alpha);drawReliefShade(c,cw,ch);p.setStyle(Paint.Style.FILL);p.setColor(0xfff3f4ed);p.setAlpha(220);p.setTextSize(12);c.drawText(cameraOverlay?"КАРТА ПО ЯРКОСТИ КАДРА":"КАРТА ВЫСОТ • ВОДА • ИЗОЛИНИИ",12,getHeight()-15,p);}
        void drawContourLines(Canvas c,float cw,float ch,int alpha){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.25f);p.setColor(0xeaf5f1d8);p.setAlpha(Math.min(235,alpha));for(float level=.20f;level<.94f;level+=.055f){for(int y=0;y<GH-1;y++)for(int x=0;x<GW-1;x++){float a=h[y][x],b=h[y][x+1],d=h[y+1][x],e=h[y+1][x+1];float[] xs=new float[4],ys=new float[4];int n=0;if((a<level)!=(b<level)){float t=(level-a)/(b-a);xs[n]=(x+t)*cw;ys[n]=y*ch;n++;}if((b<level)!=(e<level)){float t=(level-b)/(e-b);xs[n]=(x+1)*cw;ys[n]=(y+t)*ch;n++;}if((d<level)!=(e<level)){float t=(level-d)/(e-d);xs[n]=(x+t)*cw;ys[n]=(y+1)*ch;n++;}if((a<level)!=(d<level)){float t=(level-a)/(d-a);xs[n]=x*cw;ys[n]=(y+t)*ch;n++;}if(n==2)c.drawLine(xs[0],ys[0],xs[1],ys[1],p);else if(n==4){c.drawLine(xs[0],ys[0],xs[1],ys[1],p);c.drawLine(xs[2],ys[2],xs[3],ys[3],p);}}}}
        void drawReliefShade(Canvas c,float cw,float ch){p.setStyle(Paint.Style.FILL);for(int y=1;y<GH-1;y+=2)for(int x=1;x<GW-1;x+=2){float dx=h[y][x]-h[y][x-1],dy=h[y][x]-h[y-1][x];int shade=(int)(Math.min(60,Math.abs(dx+dy)*220));if(shade<5)continue;p.setColor(dx+dy>0?0x33000000:0x22ffffff);c.drawRect(x*cw,y*ch,(x+2)*cw,(y+2)*ch,p);}}
        @Override public boolean onTouchEvent(MotionEvent e){float x=e.getX(),y=e.getY();if(e.getActionMasked()==MotionEvent.ACTION_DOWN||e.getActionMasked()==MotionEvent.ACTION_MOVE){sculpt(x,y);invalidate();return true;}return true;}
        void sculpt(float px,float py){float gx=px/getWidth()*GW,gy=py/getHeight()*GH;float radius=8.5f;float delta=brushLower?-.025f:.025f;for(int y=Math.max(0,(int)(gy-radius));y<Math.min(GH,(int)(gy+radius+1));y++)for(int x=Math.max(0,(int)(gx-radius));x<Math.min(GW,(int)(gx+radius+1));x++){float dx=x-gx,dy=y-gy;float d=(float)Math.sqrt(dx*dx+dy*dy);if(d<radius){float influence=(float)Math.pow(1-d/radius,2);h[y][x]=clamp(h[y][x]+delta*influence,.02f,.98f);}}}
    }
}