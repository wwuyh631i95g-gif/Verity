package app.sandlens;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.hardware.camera2.*;
import android.os.*;
import android.view.*;
import android.graphics.SurfaceTexture;
import android.view.TextureView;
import android.widget.*;
import java.util.Collections;
import java.util.Locale;

public class MainActivity extends Activity implements TextureView.SurfaceTextureListener {
    private static final int CAMERA_PERMISSION=71;
    private FrameLayout root;
    private TextureView cameraView;
    private TerrainView terrain;
    private TextView status;
    private CameraDevice camera;
    private CameraCaptureSession session;
    private HandlerThread cameraThread;
    private Handler cameraHandler;
    private boolean cameraMode=false;
    private Runnable sampleTask;
    private float sensitivity=2.1f;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xff102535);
        getWindow().setNavigationBarColor(0xff102535);
        root=new FrameLayout(this);
        root.setBackgroundColor(0xff071d2e);

        cameraView=new TextureView(this);
        cameraView.setSurfaceTextureListener(this);
        cameraView.setVisibility(View.GONE);
        root.addView(cameraView,new FrameLayout.LayoutParams(-1,-1));

        terrain=new TerrainView();
        root.addView(terrain,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout panel=new LinearLayout(this);
        panel.setOrientation(1);
        panel.setPadding(10,7,10,7);
        panel.setBackgroundColor(0xb9162938);

        TextView title=new TextView(this);
        title.setText("SAND TABLE AR  •  карта высот");
        title.setTextSize(17);
        title.setTypeface(null,1);
        title.setTextColor(Color.WHITE);
        panel.addView(title);

        status=new TextView(this);
        status.setText("Поставь камеру над ровным песком и нажми «Калибровка 0». Затем построй горку.");
        status.setTextColor(0xffd8e8e9);
        status.setTextSize(11);
        panel.addView(status);

        LinearLayout row1=new LinearLayout(this);
        row1.setGravity(Gravity.CENTER_VERTICAL);
        row1.addView(btn("Камера",this::toggleCamera));
        row1.addView(btn("Калибровка 0",this::calibrate));
        row1.addView(btn("Чувствительность −",()->changeSensitivity(-.35f)));
        row1.addView(btn("+",()->changeSensitivity(.35f)));
        panel.addView(row1);

        LinearLayout row2=new LinearLayout(this);
        row2.setGravity(Gravity.CENTER_VERTICAL);
        row2.addView(btn("Поднять",()->{terrain.setBrushMode(false);setStatus("Режим холмов: веди пальцем по карте.");}));
        row2.addView(btn("Опустить",()->{terrain.setBrushMode(true);setStatus("Режим низин: веди пальцем по карте.");}));
        row2.addView(btn("Сброс",()->terrain.makeDemo()));
        row2.addView(btn("Изолинии",()->terrain.toggleContours()));
        panel.addView(row2);

        root.addView(panel,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));

        LinearLayout legend=new LinearLayout(this);
        legend.setOrientation(LinearLayout.VERTICAL);
        legend.setPadding(10,6,10,6);
        legend.setBackgroundColor(0xb9162938);
        TextView colorBar=new TextView(this);
        colorBar.setText("🔵 низина / условная вода     🟢 средняя     🟡 возвышенность     🔴 вершина");
        colorBar.setTextColor(Color.WHITE);
        colorBar.setTextSize(10);
        legend.addView(colorBar);
        TextView note=new TextView(this);
        note.setText("Цветная карта накладывается на изображение камеры на экране. Это оценка относительного рельефа, не измерение сантиметров и не проектор: телефон не может светить цветной картой на сам песок.");
        note.setTextColor(0xffffe5b2);
        note.setTextSize(10);
        legend.addView(note);
        root.addView(legend,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));

        setContentView(root);
        terrain.makeDemo();
        sampleTask=()->{
            if(cameraMode && cameraView!=null && cameraView.isAvailable()) {
                try {
                    Bitmap b=cameraView.getBitmap(TerrainView.GW,TerrainView.GH);
                    if(b!=null){terrain.updateFromCamera(b,sensitivity);b.recycle();}
                } catch(Exception ignored) {}
            }
            if(cameraHandler!=null) cameraHandler.postDelayed(sampleTask,260);
        };
    }

    private Button btn(String label,Runnable r) {
        Button b=new Button(this);
        b.setText(label);
        b.setTextSize(9);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setPadding(2,0,2,0);
        b.setMinWidth(0);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff32657a));
        b.setOnClickListener(v->r.run());
        return b;
    }
    private void setStatus(String s){if(status!=null)status.setText(s);}
    private void changeSensitivity(float d){sensitivity=Math.max(.6f,Math.min(4.5f,sensitivity+d));setStatus(String.format(Locale.US,"Чувствительность карты: %.1f. Наведи камеру на песок и проверь цвета.",sensitivity));}
    private void toggleCamera(){
        if(cameraMode){cameraMode=false;closeCamera();stopCameraThread();cameraView.setVisibility(View.GONE);terrain.setCameraOverlay(false);setStatus("Камера выключена. Виртуальный рельеф можно менять пальцем.");return;}
        if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.CAMERA},CAMERA_PERMISSION);return;}
        startCameraMode();
    }
    private void startCameraMode(){cameraMode=true;cameraView.setVisibility(View.VISIBLE);terrain.setCameraOverlay(true);startCameraThread();if(cameraView.isAvailable())openCamera();setStatus("Камера включена. Выровняй вид сверху; сначала нажми «Калибровка 0» на ровном песке.");}
    private void calibrate(){
        if(!cameraMode||!cameraView.isAvailable()){setStatus("Сначала включи камеру и наведи её на ровную поверхность песка.");return;}
        try{Bitmap b=cameraView.getBitmap(TerrainView.GW,TerrainView.GH);if(b==null){setStatus("Кадр камеры пока недоступен. Попробуй ещё раз.");return;}terrain.calibrateFlat(b);b.recycle();setStatus("Нулевая поверхность записана. Теперь строй горку: яркие/выступающие участки будут теплее по цвету.");}
        catch(Exception e){setStatus("Не удалось откалибровать кадр камеры.");}
    }
    private void startCameraThread(){if(cameraThread==null){cameraThread=new HandlerThread("SandTableCamera");cameraThread.start();cameraHandler=new Handler(cameraThread.getLooper());cameraHandler.postDelayed(sampleTask,350);}}
    private void stopCameraThread(){if(cameraThread!=null){cameraThread.quitSafely();try{cameraThread.join();}catch(InterruptedException ignored){}cameraThread=null;cameraHandler=null;}}
    private void openCamera(){
        if(!cameraMode||cameraHandler==null||!cameraView.isAvailable())return;
        try{
            CameraManager m=(CameraManager)getSystemService(CAMERA_SERVICE);String id=null;
            for(String candidate:m.getCameraIdList()){CameraCharacteristics cc=m.getCameraCharacteristics(candidate);Integer f=cc.get(CameraCharacteristics.LENS_FACING);if(f!=null&&f==CameraCharacteristics.LENS_FACING_BACK){id=candidate;break;}}
            if(id==null){setStatus("Основная камера не найдена.");return;}
            m.openCamera(id,new CameraDevice.StateCallback(){
                @Override public void onOpened(CameraDevice c){camera=c;preview();}
                @Override public void onDisconnected(CameraDevice c){c.close();camera=null;}
                @Override public void onError(CameraDevice c,int e){c.close();camera=null;runOnUiThread(()->setStatus("Ошибка камеры: "+e));}
            },cameraHandler);
        }catch(Exception e){setStatus("Не удалось открыть камеру: "+e.getClass().getSimpleName());}
    }
    private void preview(){
        try{
            SurfaceTexture st=cameraView.getSurfaceTexture();if(st==null)return;
            st.setDefaultBufferSize(640,480);
            Surface surface=new Surface(st);
            CaptureRequest.Builder b=camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            b.addTarget(surface);b.set(CaptureRequest.CONTROL_MODE,CameraMetadata.CONTROL_MODE_AUTO);
            camera.createCaptureSession(Collections.singletonList(surface),new CameraCaptureSession.StateCallback(){
                @Override public void onConfigured(CameraCaptureSession s){session=s;try{s.setRepeatingRequest(b.build(),null,cameraHandler);}catch(Exception ignored){}}
                @Override public void onConfigureFailed(CameraCaptureSession s){runOnUiThread(()->setStatus("Не удалось запустить предпросмотр камеры."));}
            },cameraHandler);
        }catch(Exception e){runOnUiThread(()->setStatus("Ошибка предпросмотра камеры."));}
    }
    private void closeCamera(){try{if(session!=null){session.close();session=null;}if(camera!=null){camera.close();camera=null;}}catch(Exception ignored){}}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==CAMERA_PERMISSION&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)startCameraMode();else setStatus("Разрешение камеры не выдано. Виртуальная песочница доступна без камеры.");}
    @Override public void onSurfaceTextureAvailable(SurfaceTexture s,int w,int h){if(cameraMode)openCamera();}
    @Override public void onSurfaceTextureSizeChanged(SurfaceTexture s,int w,int h){}
    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture s){closeCamera();return true;}
    @Override public void onSurfaceTextureUpdated(SurfaceTexture s){}
    @Override protected void onPause(){closeCamera();stopCameraThread();super.onPause();}
    @Override protected void onResume(){super.onResume();if(cameraMode){startCameraThread();if(cameraView.isAvailable())openCamera();}}

    private class TerrainView extends View {
        static final int GW=84,GH=112;
        float[][] h=new float[GH][GW];
        float[][] baseline=new float[GH][GW];
        boolean calibrated=false,drawContours=true,showCamera=false,brushLower=false;
        Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        TerrainView(){super(MainActivity.this);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        void setBrushMode(boolean lower){brushLower=lower;}
        void setCameraOverlay(boolean v){showCamera=v;invalidate();}
        void toggleContours(){drawContours=!drawContours;invalidate();}
        float clamp(float v){return Math.max(.025f,Math.min(.99f,v));}
        void makeDemo(){
            calibrated=false;
            for(int y=0;y<GH;y++)for(int x=0;x<GW;x++){
                float nx=(x-GW*.49f)/(GW*.39f),ny=(y-GH*.48f)/(GH*.39f);
                float d=(float)Math.sqrt(nx*nx+ny*ny);
                float hill=Math.max(0,1-d);
                float wave=(float)(Math.sin(x*.19+y*.11)*.035+Math.cos(y*.15-x*.07)*.028);
                float lake=(float)Math.sqrt(Math.pow((x-GW*.69f)/(GW*.15f),2)+Math.pow((y-GH*.70f)/(GH*.12f),2));
                h[y][x]=clamp(.18f+hill*.68f+wave-(lake<1?.22f*(1-lake):0));
            }
            invalidate();
        }
        float[][] readLuminance(Bitmap b){
            float[][] out=new float[GH][GW];
            for(int y=0;y<GH;y++)for(int x=0;x<GW;x++){
                int c=b.getPixel(Math.min(b.getWidth()-1,x*b.getWidth()/GW),Math.min(b.getHeight()-1,y*b.getHeight()/GH));
                out[y][x]=(Color.red(c)*.2126f+Color.green(c)*.7152f+Color.blue(c)*.0722f)/255f;
            }
            return out;
        }
        void calibrateFlat(Bitmap b){
            baseline=readLuminance(b);calibrated=true;
            for(int y=0;y<GH;y++)for(int x=0;x<GW;x++)h[y][x]=.16f;
            invalidate();
        }
        void updateFromCamera(Bitmap b,float strength){
            float[][] lum=readLuminance(b);
            float mean=0;
            for(int y=0;y<GH;y++)for(int x=0;x<GW;x++)mean+=lum[y][x];
            mean/=GW*GH;
            for(int y=1;y<GH-1;y++)for(int x=1;x<GW-1;x++){
                float dx=Math.abs(lum[y][x+1]-lum[y][x-1])*.5f;
                float dy=Math.abs(lum[y+1][x]-lum[y-1][x])*.5f;
                float edge=(dx+dy)*.5f;
                float relative=calibrated?(lum[y][x]-baseline[y][x]):(lum[y][x]-mean);
                float target=.17f+relative*strength+edge*1.75f;
                target=clamp(target);
                if(target<.22f)target=.08f+target*.38f;
                h[y][x]=h[y][x]*.45f+target*.55f;
            }
            // Keep borders stable rather than leaving uninitialised cells.
            for(int x=0;x<GW;x++){h[0][x]=h[1][x];h[GH-1][x]=h[GH-2][x];}
            for(int y=0;y<GH;y++){h[y][0]=h[y][1];h[y][GW-1]=h[y][GW-2];}
            postInvalidate();
        }
        int colorFor(float v){
            if(v<.22f){float t=v/.22f;return Color.rgb(4,(int)(48+84*t),(int)(130+105*t));}
            if(v<.42f){float t=(v-.22f)/.20f;return Color.rgb((int)(8+24*t),(int)(132+74*t),(int)(220-145*t));}
            if(v<.65f){float t=(v-.42f)/.23f;return Color.rgb((int)(32+92*t),(int)(206+14*t),(int)(75-18*t));}
            if(v<.82f){float t=(v-.65f)/.17f;return Color.rgb((int)(124+120*t),(int)(220-36*t),(int)(57-37*t));}
            float t=(v-.82f)/.18f;return Color.rgb(244,(int)(184-122*t),(int)(20+22*t));
        }
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            if(!showCamera)c.drawColor(0xff061d32);
            float cw=(float)getWidth()/GW,ch=(float)getHeight()/GH;
            int alpha=showCamera?142:255;
            paint.setStyle(Paint.Style.FILL);
            for(int y=0;y<GH;y++)for(int x=0;x<GW;x++){
                int col=colorFor(h[y][x]);
                paint.setColor(col);paint.setAlpha(alpha);
                c.drawRect(x*cw,y*ch,(x+1)*cw+1,(y+1)*ch+1,paint);
            }
            if(drawContours)drawContourLines(c,cw,ch,showCamera?188:220);
            drawHillShade(c,cw,ch,showCamera?60:76);
            paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);paint.setAlpha(235);paint.setTextSize(12);
            c.drawText(showCamera?(calibrated?"ОТНОСИТЕЛЬНАЯ КАРТА РЕЛЬЕФА":"КАРТА ПО КАДРУ • лучше откалибровать ровную поверхность"):"ДЕМО-КАРТА • проведи пальцем, чтобы менять высоту",10,getHeight()-13,paint);
        }
        void drawContourLines(Canvas c,float cw,float ch,int alpha){
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.15f);paint.setColor(0xfff1f7e4);paint.setAlpha(alpha);
            for(float level=.22f;level<.96f;level+=.065f){
                for(int y=0;y<GH-1;y++)for(int x=0;x<GW-1;x++){
                    float a=h[y][x],b=h[y][x+1],d=h[y+1][x],e=h[y+1][x+1];
                    float[] xs=new float[4],ys=new float[4];int n=0;
                    if((a<level)!=(b<level)){float t=(level-a)/(b-a);xs[n]=(x+t)*cw;ys[n]=y*ch;n++;}
                    if((b<level)!=(e<level)){float t=(level-b)/(e-b);xs[n]=(x+1)*cw;ys[n]=(y+t)*ch;n++;}
                    if((d<level)!=(e<level)){float t=(level-d)/(e-d);xs[n]=(x+t)*cw;ys[n]=(y+1)*ch;n++;}
                    if((a<level)!=(d<level)){float t=(level-a)/(d-a);xs[n]=x*cw;ys[n]=(y+t)*ch;n++;}
                    if(n==2)c.drawLine(xs[0],ys[0],xs[1],ys[1],paint);
                    else if(n==4){c.drawLine(xs[0],ys[0],xs[1],ys[1],paint);c.drawLine(xs[2],ys[2],xs[3],ys[3],paint);}
                }
            }
        }
        void drawHillShade(Canvas c,float cw,float ch,int alpha){
            paint.setStyle(Paint.Style.FILL);
            for(int y=1;y<GH-1;y+=2)for(int x=1;x<GW-1;x+=2){
                float dx=h[y][x]-h[y][x-1],dy=h[y][x]-h[y-1][x];
                if(Math.abs(dx+dy)<.025f)continue;
                paint.setColor(dx+dy>0?0xff000000:0xffffffff);paint.setAlpha(alpha);
                c.drawRect(x*cw,y*ch,(x+2)*cw,(y+2)*ch,paint);
            }
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getActionMasked()==MotionEvent.ACTION_DOWN||e.getActionMasked()==MotionEvent.ACTION_MOVE){sculpt(e.getX(),e.getY());invalidate();return true;}
            return true;
        }
        void sculpt(float px,float py){
            float gx=px/getWidth()*GW,gy=py/getHeight()*GH,r=8.5f;
            float delta=brushLower?-.026f:.026f;
            for(int y=Math.max(0,(int)(gy-r));y<Math.min(GH,(int)(gy+r+1));y++)
                for(int x=Math.max(0,(int)(gx-r));x<Math.min(GW,(int)(gx+r+1));x++){
                    float dx=x-gx,dy=y-gy,d=(float)Math.sqrt(dx*dx+dy*dy);
                    if(d<r){float influence=(float)Math.pow(1-d/r,2);h[y][x]=clamp(h[y][x]+delta*influence);}
                }
        }
    }
}