package app.sketchforge;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE=12, SAVE_OBJ=13, CAPTURE_SCREEN=14;
    private ModelView model;
    private TextView status;
    private SeekBar timeline;
    private boolean playing=false;
    private Handler handler=new Handler(Looper.getMainLooper());
    private Runnable playback;
    private static final String PREF="sketchforge_video";
    private int currentFrame=0;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(0xff111b2e);
        getWindow().setNavigationBarColor(0xff111b2e);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xff101828);

        TextView title=new TextView(this);
        title.setText("SKETCHFORGE 3D  •  анимация персонажей");
        title.setTextColor(Color.WHITE);title.setTextSize(17);title.setTypeface(null,1);
        title.setPadding(12,10,8,3);root.addView(title);

        status=new TextView(this);
        status.setText("Выбери форму, загрузи рисунок как наклейку на 3D-модель, двигай руки и ноги, добавляй кадры и записывай MP4.");
        status.setTextColor(0xffc0d0e4);status.setTextSize(11);status.setPadding(12,0,10,6);root.addView(status);

        model=new ModelView(this);
        root.addView(model,new LinearLayout.LayoutParams(-1,0,1));

        root.addView(row(new String[]{"Шар","Куб","Цилиндр","Треуг.","Звезда","Планета"},new Runnable[]{
            ()->model.setShape(0),()->model.setShape(1),()->model.setShape(2),()->model.setShape(3),()->model.setShape(4),()->model.setShape(5)
        }));
        root.addView(row(new String[]{"Мой рисунок","Очистить рисунок","Добавить хвост","Сохранить OBJ"},new Runnable[]{
            this::pickImage,()->model.setReference(null),()->model.addTail(),this::saveObj
        }));

        TextView arms=new TextView(this);arms.setText("Поза персонажа  •  настрой углы частей тела");arms.setTextColor(0xff9cbbe4);arms.setTextSize(11);arms.setPadding(12,3,8,0);root.addView(arms);
        root.addView(sliderRow("Рука Л",0));
        root.addView(sliderRow("Рука П",1));
        root.addView(sliderRow("Нога Л",2));
        root.addView(sliderRow("Нога П",3));

        LinearLayout timeRow=new LinearLayout(this);timeRow.setGravity(Gravity.CENTER_VERTICAL);timeRow.setPadding(8,0,8,0);
        TextView timeLabel=new TextView(this);timeLabel.setText("Таймлайн");timeLabel.setTextColor(Color.WHITE);timeLabel.setTextSize(11);timeRow.addView(timeLabel);
        timeline=new SeekBar(this);timeline.setMax(100);timeline.setProgress(0);timeRow.addView(timeline,new LinearLayout.LayoutParams(0,40,1));
        timeline.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int p,boolean user){if(user){currentFrame=p;model.setTime(p);}}
            public void onStartTrackingTouch(SeekBar s){}
            public void onStopTrackingTouch(SeekBar s){}
        });
        root.addView(timeRow);

        root.addView(row(new String[]{"＋ Кадр","▶ Плей","■ Стоп","Запись MP4"},new Runnable[]{
            this::saveKeyframe,this::togglePlayback,this::stopPlayback,this::recordVideo
        }));
        root.addView(row(new String[]{"Остановить запись","Поделиться видео"},new Runnable[]{
            this::stopRecording,this::shareLastVideo
        }));

        setContentView(root);
        buildPlaybackTask();
    }

    private View row(String[] labels,Runnable[] actions){
        HorizontalScrollView hsv=new HorizontalScrollView(this);hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout line=new LinearLayout(this);line.setPadding(5,2,5,4);
        for(int i=0;i<labels.length;i++){
            Button b=new Button(this);b.setText(labels[i]);b.setTextSize(10);b.setAllCaps(false);b.setTextColor(Color.WHITE);
            b.setPadding(4,0,4,0);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff2f4568));
            Runnable r=actions[i];b.setOnClickListener(v->r.run());
            line.addView(b,new LinearLayout.LayoutParams(-2,43));
        }
        hsv.addView(line);return hsv;
    }
    private View sliderRow(String label,int part){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(9,0,9,0);
        TextView name=new TextView(this);name.setText(label);name.setTextColor(0xffdbe7f5);name.setTextSize(10);row.addView(name,new LinearLayout.LayoutParams(55,32));
        SeekBar seek=new SeekBar(this);seek.setMax(160);seek.setProgress(80);row.addView(seek,new LinearLayout.LayoutParams(0,34,1));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int v,boolean user){if(user)model.setPose(part,(v-80)*.0125f);}
            public void onStartTrackingTouch(SeekBar s){}
            public void onStopTrackingTouch(SeekBar s){}
        });
        return row;
    }
    private void pickImage(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_IMAGE);}
    private void saveObj(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("text/plain");i.putExtra(Intent.EXTRA_TITLE,"sketchforge_character.obj");startActivityForResult(i,SAVE_OBJ);}
    private void saveKeyframe(){model.saveKeyframe(currentFrame);status.setText("Кадр "+currentFrame+" сохранён. Передвинь таймлайн, измени позу и добавь следующий кадр.");}
    private void togglePlayback(){if(playing){stopPlayback();return;}if(model.keyCount()==0)saveKeyframe();playing=true;status.setText("Воспроизведение анимации по ключевым кадрам.");handler.post(playback);}
    private void stopPlayback(){playing=false;if(playback!=null)handler.removeCallbacks(playback);status.setText("Анимация остановлена. Можно редактировать позу.");}
    private void buildPlaybackTask(){playback=()->{if(!playing)return;currentFrame=(currentFrame+1)%101;timeline.setProgress(currentFrame);model.setTime(currentFrame);handler.postDelayed(playback,35);};}
    private void recordVideo(){try{MediaProjectionManager m=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);startActivityForResult(m.createScreenCaptureIntent(),CAPTURE_SCREEN);}catch(Exception e){status.setText("Не удалось открыть системное окно записи экрана.");}}
    private void stopRecording(){Intent i=new Intent(this,ScreenRecorderService.class);i.setAction(ScreenRecorderService.ACTION_STOP);startService(i);status.setText("Команда остановки записи отправлена.");}
    private void shareLastVideo(){
        String saved=getSharedPreferences(PREF,0).getString("last_uri","");
        if(saved.isEmpty()){status.setText("Сначала запиши видео MP4, затем поделись им.");return;}
        try{Intent send=new Intent(Intent.ACTION_SEND);send.setType("video/mp4");send.putExtra(Intent.EXTRA_STREAM,Uri.parse(saved));send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(send,"Поделиться видео SketchForge"));}catch(Exception e){status.setText("Не удалось открыть отправку видео.");}
    }
    @Override protected void onActivityResult(int r,int c,Intent d){
        super.onActivityResult(r,c,d);if(c!=RESULT_OK||d==null)return;
        if(r==PICK_IMAGE&&d.getData()!=null){
            try{getContentResolver().takePersistableUriPermission(d.getData(),Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
            try(InputStream in=getContentResolver().openInputStream(d.getData())){Bitmap bm=BitmapFactory.decodeStream(in);if(bm!=null){model.setReference(cleanBackground(bm));status.setText("Рисунок наложен на поверхность выбранной 3D-формы. Вращай модель, чтобы посмотреть её с разных сторон.");}}catch(Exception e){status.setText("Не удалось открыть изображение.");}
        }
        if(r==SAVE_OBJ&&d.getData()!=null){
            try(OutputStream out=getContentResolver().openOutputStream(d.getData())){out.write(model.toObj().getBytes("UTF-8"));Toast.makeText(this,"OBJ сохранён",Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(this,"Ошибка экспорта OBJ",Toast.LENGTH_LONG).show();}
        }
        if(r==CAPTURE_SCREEN&&d.getData()!=null){
            Intent i=new Intent(this,ScreenRecorderService.class);i.setAction(ScreenRecorderService.ACTION_START);
            i.putExtra(ScreenRecorderService.EXTRA_RESULT_CODE,c);i.putExtra(ScreenRecorderService.EXTRA_DATA,d);
            try{if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);status.setText("Запись MP4 началась. На видео попадёт окно приложения вместе с анимацией.");}
            catch(Exception e){status.setText("Не удалось начать запись MP4. Проверь системное разрешение записи экрана.");}
        }
    }
    private Bitmap cleanBackground(Bitmap source){
        // Remove only corner-connected pixels close to the corner colour, keeping interior black line art.
        int w=source.getWidth(),h=source.getHeight();if(w>1200||h>1200){float scale=Math.min(1200f/w,1200f/h);source=Bitmap.createScaledBitmap(source,Math.max(1,(int)(w*scale)),Math.max(1,(int)(h*scale)),true);w=source.getWidth();h=source.getHeight();}
        Bitmap out=source.copy(Bitmap.Config.ARGB_8888,true);
        int bg=out.getPixel(0,0);int[] px=new int[w*h];out.getPixels(px,0,w,0,0,w,h);boolean[] seen=new boolean[px.length];int[] stack=new int[px.length];int top=0;
        for(int x=0;x<w;x++){top=pushEdge(x,px,seen,stack,top,bg);top=pushEdge((h-1)*w+x,px,seen,stack,top,bg);}
        for(int y=0;y<h;y++){top=pushEdge(y*w,px,seen,stack,top,bg);top=pushEdge(y*w+w-1,px,seen,stack,top,bg);}
        while(top>0){int idx=stack[--top];px[idx]=px[idx]&0x00ffffff;int x=idx%w,y=idx/w;if(x>0)top=pushEdge(idx-1,px,seen,stack,top,bg);if(x+1<w)top=pushEdge(idx+1,px,seen,stack,top,bg);if(y>0)top=pushEdge(idx-w,px,seen,stack,top,bg);if(y+1<h)top=pushEdge(idx+w,px,seen,stack,top,bg);}
        out.setPixels(px,0,w,0,0,w,h);return out;
    }
    private int pushEdge(int idx,int[] px,boolean[] seen,int[] stack,int top,int bg){
        if(idx<0||idx>=px.length||seen[idx])return top;
        int c=px[idx];int dist=Math.abs(Color.red(c)-Color.red(bg))+Math.abs(Color.green(c)-Color.green(bg))+Math.abs(Color.blue(c)-Color.blue(bg));
        if(dist<48){seen[idx]=true;stack[top++]=idx;}return top;
    }
    @Override protected void onDestroy(){stopPlayback();super.onDestroy();}

    static class Mesh{
        String name;float[] v;int[] f;int color;int swing=-1;float pivotX,pivotY,pivotZ;
        Mesh(String n,float[] a,int[] b,int c){name=n;v=a;f=b;color=c;}
    }
    static class Keyframe{int t;float[] pose;Keyframe(int t,float[] p){this.t=t;this.pose=p.clone();}}
    class ModelView extends View{
        ArrayList<Mesh> meshes=new ArrayList<>();
        ArrayList<Keyframe> keys=new ArrayList<>();
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        float yaw=-.52f,pitch=.18f,zoom=1f;
        float[] pose={0,0,0,0};float animationT=0;
        Bitmap texture;int shape=0;float downX,downY;ScaleGestureDetector scaleDetector;
        ModelView(Activity a){super(a);setLayerType(View.LAYER_TYPE_SOFTWARE,null);scaleDetector=new ScaleGestureDetector(a,new ScaleGestureDetector.SimpleOnScaleGestureListener(){public boolean onScale(ScaleGestureDetector s){zoom=Math.max(.55f,Math.min(1.8f,zoom*s.getScaleFactor()));invalidate();return true;}});buildModel();}
        void setShape(int value){shape=value;buildModel();}
        void setReference(Bitmap b){texture=b;invalidate();}
        void setPose(int part,float value){if(part>=0&&part<4){pose[part]=Math.max(-1.2f,Math.min(1.2f,value));invalidate();}}
        void setTime(int t){animationT=t;if(keys.size()>0){float[] v=interpolate(t);for(int i=0;i<4;i++)pose[i]=v[i];}invalidate();}
        int keyCount(){return keys.size();}
        void saveKeyframe(int t){for(int i=0;i<keys.size();i++)if(keys.get(i).t==t){keys.set(i,new Keyframe(t,pose));invalidate();return;}keys.add(new Keyframe(t,pose));Collections.sort(keys,(a,b)->Integer.compare(a.t,b.t));invalidate();}
        float[] interpolate(int t){if(keys.size()==1)return keys.get(0).pose.clone();Keyframe a=keys.get(0),b=keys.get(keys.size()-1);if(t<=a.t)return a.pose.clone();if(t>=b.t)return b.pose.clone();for(int i=0;i<keys.size()-1;i++){if(t>=keys.get(i).t&&t<=keys.get(i+1).t){a=keys.get(i);b=keys.get(i+1);break;}}float k=(t-a.t)/(float)Math.max(1,b.t-a.t);float[] out=new float[4];for(int i=0;i<4;i++)out[i]=a.pose[i]+(b.pose[i]-a.pose[i])*k;return out;}
        void buildModel(){
            meshes.clear();
            int bodyColor=shape==1?0xff9b704d:shape==2?0xff8c9cb2:shape==3?0xffb87942:shape==4?0xffe8bd38:shape==5?0xff426fb5:0xffa66a42;
            if(shape==0)meshes.add(ellipsoid("body_sphere",0,.0f,0,.77f,.82f,.55f,bodyColor));
            if(shape==1)meshes.add(box("body_cube",0,.0f,0,1.35f,1.45f,.82f,bodyColor));
            if(shape==2)meshes.add(cylinder("body_cylinder",0,.0f,0,.70f,1.3f,.70f,bodyColor));
            if(shape==3)meshes.add(cone("body_triangle",0,-.78f,0,1.55f,bodyColor));
            if(shape==4)meshes.add(star("body_star",0,.0f,0,1.0f,.46f,bodyColor));
            if(shape==5){meshes.add(ellipsoid("planet",0,.0f,0,.74f,.74f,.60f,bodyColor));meshes.add(ring("planet_ring",0,.0f,0,1.10f,.06f,0xffd1b56d));}
            // Head, ears, muzzle and small eyes.
            meshes.add(ellipsoid("head",0,1.18f,0,.58f,.58f,.50f,0xffb77c52));
            meshes.add(ellipsoid("ear_left",-.43f,1.62f,.02f,.20f,.22f,.18f,0xff805033));
            meshes.add(ellipsoid("ear_right",.43f,1.62f,.02f,.20f,.22f,.18f,0xff805033));
            meshes.add(ellipsoid("eye_left",-.22f,1.25f,.46f,.075f,.095f,.05f,0xff151a24));
            meshes.add(ellipsoid("eye_right",.22f,1.25f,.46f,.075f,.095f,.05f,0xff151a24));
            meshes.add(ellipsoid("nose",0,1.04f,.51f,.12f,.09f,.06f,0xff2b1d1a));
            // Swinging arms and legs. Each part rotates around the shoulder/hip pivot during posing.
            Mesh armL=box("arm_left",-.93f,.06f,0,.28f,.88f,.34f,0xff9b633e);armL.swing=0;armL.pivotX=-.76f;armL.pivotY=.52f;meshes.add(armL);
            Mesh armR=box("arm_right",.93f,.06f,0,.28f,.88f,.34f,0xff9b633e);armR.swing=1;armR.pivotX=.76f;armR.pivotY=.52f;meshes.add(armR);
            Mesh legL=box("leg_left",-.36f,-1.06f,.02f,.39f,.78f,.42f,0xff74452f);legL.swing=2;legL.pivotX=-.36f;legL.pivotY=-.69f;meshes.add(legL);
            Mesh legR=box("leg_right",.36f,-1.06f,.02f,.39f,.78f,.42f,0xff74452f);legR.swing=3;legR.pivotX=.36f;legR.pivotY=-.69f;meshes.add(legR);
            if(shape==5)meshes.add(ellipsoid("planet_moon",1.03f,.62f,0,.17f,.17f,.15f,0xffd5d7e8));
            invalidate();
        }
        Mesh ellipsoid(String name,float cx,float cy,float cz,float rx,float ry,float rz,int color){
            int lat=12,lon=18;float[] v=new float[(lat+1)*(lon+1)*3];int k=0;
            for(int a=0;a<=lat;a++){double th=Math.PI*a/lat;for(int b=0;b<=lon;b++){double ph=2*Math.PI*b/lon;v[k++]=cx+(float)(rx*Math.sin(th)*Math.cos(ph));v[k++]=cy+(float)(ry*Math.cos(th));v[k++]=cz+(float)(rz*Math.sin(th)*Math.sin(ph));}}
            int[] f=new int[lat*lon*6];k=0;for(int a=0;a<lat;a++)for(int b=0;b<lon;b++){int q=a*(lon+1)+b,n=q+lon+1;f[k++]=q;f[k++]=n;f[k++]=q+1;f[k++]=q+1;f[k++]=n;f[k++]=n+1;}
            return new Mesh(name,v,f,color);
        }
        Mesh box(String name,float cx,float cy,float cz,float w,float h,float d,int color){
            float x=w/2,y=h/2,z=d/2;
            float[] v={cx-x,cy-y,cz-z,cx+x,cy-y,cz-z,cx+x,cy+y,cz-z,cx-x,cy+y,cz-z,cx-x,cy-y,cz+z,cx+x,cy-y,cz+z,cx+x,cy+y,cz+z,cx-x,cy+y,cz+z};
            int[] f={0,1,2,0,2,3,5,4,7,5,7,6,4,0,3,4,3,7,1,5,6,1,6,2,3,2,6,3,6,7,4,5,1,4,1,0};
            return new Mesh(name,v,f,color);
        }
        Mesh cylinder(String name,float cx,float cy,float cz,float rx,float h,float rz,int color){
            int n=20;float[] v=new float[(n*2+2)*3];int k=0;
            for(int ring=0;ring<2;ring++)for(int i=0;i<n;i++){double a=2*Math.PI*i/n;v[k++]=cx+rx*(float)Math.cos(a);v[k++]=cy+(ring==0?-h/2:h/2);v[k++]=cz+rz*(float)Math.sin(a);}
            int bottom=n*2,top=n*2+1;v[bottom*3]=cx;v[bottom*3+1]=cy-h/2;v[bottom*3+2]=cz;v[top*3]=cx;v[top*3+1]=cy+h/2;v[top*3+2]=cz;
            int[] f=new int[n*12];k=0;for(int i=0;i<n;i++){int j=(i+1)%n;f[k++]=i;f[k++]=j;f[k++]=n+i;f[k++]=j;f[k++]=n+j;f[k++]=n+i;f[k++]=bottom;f[k++]=j;f[k++]=i;f[k++]=top;f[k++]=n+i;f[k++]=n+j;}return new Mesh(name,v,f,color);
        }
        Mesh cone(String name,float cx,float baseY,float cz,float h,int color){
            int n=28;float[] v=new float[(n+1)*3];v[0]=cx;v[1]=baseY+h;v[2]=cz;
            for(int i=0;i<n;i++){double a=2*Math.PI*i/n;v[(i+1)*3]=cx+.82f*(float)Math.cos(a);v[(i+1)*3+1]=baseY;v[(i+1)*3+2]=cz+.48f*(float)Math.sin(a);}
            int[] f=new int[n*3];int k=0;for(int i=0;i<n;i++){int j=(i+1)%n+1;f[k++]=0;f[k++]=i+1;f[k++]=j;}return new Mesh(name,v,f,color);
        }
        Mesh star(String name,float cx,float cy,float cz,float r,float depth,int color){
            int n=10;float[] v=new float[n*2*3];int k=0;for(int side=0;side<2;side++)for(int i=0;i<n;i++){double a=-Math.PI/2+i*Math.PI/n;float rad=(i%2==0?r:r*.46f);v[k++]=cx+rad*(float)Math.cos(a);v[k++]=cy+rad*(float)Math.sin(a);v[k++]=cz+(side==0?-depth:depth);}
            int[] f=new int[n*12];k=0;for(int i=0;i<n;i++){int j=(i+1)%n;f[k++]=i;f[k++]=j;f[k++]=n+i;f[k++]=j;f[k++]=n+j;f[k++]=n+i;f[k++]=0;f[k++]=i;f[k++]=j;f[k++]=n;f[k++]=n+j;f[k++]=n+i;}return new Mesh(name,v,f,color);
        }
        Mesh ring(String name,float cx,float cy,float cz,float radius,float width,int color){
            int n=44;float[] v=new float[n*2*3];int k=0;for(int r=0;r<2;r++)for(int i=0;i<n;i++){double a=2*Math.PI*i/n;float rr=radius+(r==0?-width:width);v[k++]=cx+rr*(float)Math.cos(a);v[k++]=cy+.17f*(float)Math.sin(a);v[k++]=cz+rr*.55f*(float)Math.sin(a);}
            int[] f=new int[n*6];k=0;for(int i=0;i<n;i++){int j=(i+1)%n;f[k++]=i;f[k++]=j;f[k++]=n+i;f[k++]=j;f[k++]=n+j;f[k++]=n+i;}return new Mesh(name,v,f,color);
        }
        void addTail(){meshes.add(ellipsoid("tail",-.88f,-.25f,-.05f,.20f,.48f,.20f,0xff8d542f));invalidate();}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);c.drawColor(0xff172238);
            p.setColor(0xff283954);p.setStrokeWidth(1);for(int x=0;x<getWidth();x+=42)c.drawLine(x,0,x,getHeight(),p);for(int y=0;y<getHeight();y+=42)c.drawLine(0,y,getWidth(),y,p);
            ArrayList<float[]> faces=new ArrayList<>();float scale=Math.min(getWidth(),getHeight())*.265f*zoom;
            for(Mesh m:meshes)for(int i=0;i<m.f.length;i+=3){
                float[][] pts=new float[3][3];float depth=0;
                for(int j=0;j<3;j++){
                    int idx=m.f[i+j]*3;float x=m.v[idx],y=m.v[idx+1],z=m.v[idx+2];
                    if(m.swing>=0){float angle=pose[m.swing];float dx=x-m.pivotX,dy=y-m.pivotY;float nx=dx*(float)Math.cos(angle)-dy*(float)Math.sin(angle);float ny=dx*(float)Math.sin(angle)+dy*(float)Math.cos(angle);x=m.pivotX+nx;y=m.pivotY+ny;}
                    float xx=(float)(x*Math.cos(yaw)-z*Math.sin(yaw));
                    float zz=(float)(x*Math.sin(yaw)+z*Math.cos(yaw));
                    float yy=(float)(y*Math.cos(pitch)-zz*Math.sin(pitch));
                    float z2=(float)(y*Math.sin(pitch)+zz*Math.cos(pitch));
                    pts[j][0]=getWidth()/2f+xx*scale;pts[j][1]=getHeight()*.57f-yy*scale;pts[j][2]=z2;depth+=z2/3f;
                }
                float shade=Math.max(.46f,Math.min(1.18f,.86f+depth*.09f));
                faces.add(new float[]{pts[0][0],pts[0][1],pts[1][0],pts[1][1],pts[2][0],pts[2][1],depth,shade,Color.red(m.color),Color.green(m.color),Color.blue(m.color)});
            }
            Collections.sort(faces,(a,b)->Float.compare(a[6],b[6]));
            for(float[] f:faces){Path path=new Path();path.moveTo(f[0],f[1]);path.lineTo(f[2],f[3]);path.lineTo(f[4],f[5]);path.close();p.setColor(Color.rgb(Math.min(255,(int)(f[8]*f[7])),Math.min(255,(int)(f[9]*f[7])),Math.min(255,(int)(f[10]*f[7]))));p.setStyle(Paint.Style.FILL);c.drawPath(path,p);}
            if(texture!=null)drawTextureDecal(c,scale);
            p.setColor(0xffdbe7f5);p.setTextSize(11);c.drawText("Вращение: палец  •  Масштаб: щипок  •  Картинка прикрепляется к передней поверхности",10,getHeight()-12,p);
        }
        void drawTextureDecal(Canvas c,float scale){
            float facing=(float)Math.cos(yaw);if(facing<-.05f)return;
            float cx=getWidth()/2f+(float)(.03f*Math.sin(yaw))*scale;
            float cy=getHeight()*.57f-(float)(.03f*Math.cos(pitch))*scale;
            float w=(shape==1?1.02f:shape==4?1.10f:.98f)*scale*Math.max(.15f,Math.abs(facing));
            float h=(shape==1?1.02f:shape==3?1.0f:1.08f)*scale*Math.max(.55f,(float)Math.cos(pitch));
            c.save();if(facing<0)c.scale(-1,1,cx,cy);
            p.setShader(new BitmapShader(texture,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP));
            RectF dst=new RectF(cx-w/2,cy-h/2,cx+w/2,cy+h/2);
            Path clip=new Path();
            if(shape==1){clip.addRoundRect(dst,18,18,Path.Direction.CW);}
            else if(shape==3){clip.moveTo(cx,cy-h/2);clip.lineTo(cx+w/2,cy+h/2);clip.lineTo(cx-w/2,cy+h/2);clip.close();}
            else if(shape==4){clip.addPath(starPath(cx,cy,w*.55f,h*.55f));}
            else{clip.addOval(dst,Path.Direction.CW);}
            c.save();c.clipPath(clip);c.drawBitmap(texture,null,dst,p);c.restore();
            p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(0x99ffffff);c.drawOval(dst,p);p.setStyle(Paint.Style.FILL);
            p.setColor(0x33ffffff);c.drawOval(new RectF(cx-w*.34f,cy-h*.43f,cx+w*.08f,cy-h*.19f),p);
            c.restore();
        }
        Path starPath(float cx,float cy,float rx,float ry){Path path=new Path();for(int i=0;i<10;i++){double a=-Math.PI/2+i*Math.PI/5;float r=i%2==0?1:.46f;float x=cx+(float)Math.cos(a)*rx*r,y=cy+(float)Math.sin(a)*ry*r;if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}path.close();return path;}
        @Override public boolean onTouchEvent(android.view.MotionEvent e){
            scaleDetector.onTouchEvent(e);
            if(e.getActionMasked()==MotionEvent.ACTION_DOWN){downX=e.getX();downY=e.getY();return true;}
            if(e.getActionMasked()==MotionEvent.ACTION_MOVE){float dx=e.getX()-downX,dy=e.getY()-downY;yaw+=dx*.008f;pitch=Math.max(-1.1f,Math.min(1.1f,pitch+dy*.007f));downX=e.getX();downY=e.getY();invalidate();return true;}return true;
        }
        String toObj(){
            StringBuilder s=new StringBuilder("# SketchForge 3D character model\n");int offset=1;
            for(Mesh m:meshes){s.append("o ").append(m.name.replace(' ','_')).append("\n");for(int i=0;i<m.v.length;i+=3)s.append(String.format(Locale.US,"v %.5f %.5f %.5f\n",m.v[i],m.v[i+1],m.v[i+2]));for(int i=0;i<m.f.length;i+=3)s.append("f ").append(m.f[i]+offset).append(" ").append(m.f[i+1]+offset).append(" ").append(m.f[i+2]+offset).append("\n");offset+=m.v.length/3;}
            return s.toString();
        }
    }
}