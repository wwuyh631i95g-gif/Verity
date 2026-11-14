package app.sketchforge;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
    private ModelView model;
    private TextView info;
    private static final int PICK_REF=12, SAVE_OBJ=13;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(18,24,38));
        getWindow().setNavigationBarColor(Color.rgb(18,24,38));
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(13,18,30));
        TextView title=new TextView(this); title.setText("SKETCHFORGE 3D  •  конструктор формы");
        title.setTextColor(Color.WHITE); title.setTextSize(17); title.setPadding(14,13,8,8); root.addView(title);
        info=new TextView(this); info.setText("Вращай модель пальцем. Добавляй детали, затем экспортируй OBJ.");
        info.setTextColor(0xffb7c4d8); info.setTextSize(12); info.setPadding(14,0,8,8); root.addView(info);
        model=new ModelView(this); LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,0,1); root.addView(model,mp);
        root.addView(row(new String[]{"Тело","Лапа","Зуб","Бугорок","Глаз","Хвост"},new Runnable[]{
            ()->model.addBody(),()->model.addPaw(),()->model.addTooth(),()->model.addBump(),()->model.addEye(),()->model.addTail()
        }));
        root.addView(row(new String[]{"Фото-референс","Отменить деталь","Очистить","Экспорт OBJ"},new Runnable[]{
            ()->pickReference(),()->model.removeLast(),()->model.reset(),()->saveObj()
        }));
        setContentView(root);
    }
    private View row(String[] labels,Runnable[] actions){
        HorizontalScrollView hsv=new HorizontalScrollView(this); hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout line=new LinearLayout(this); line.setPadding(6,4,6,8);
        for(int i=0;i<labels.length;i++){
            final Runnable action=actions[i]; Button b=new Button(this); b.setText(labels[i]); b.setTextSize(11);
            b.setAllCaps(false); b.setTextColor(Color.WHITE); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff31425f));
            b.setOnClickListener(v->action.run()); line.addView(b,new LinearLayout.LayoutParams(-2,48));
        }
        hsv.addView(line); return hsv;
    }
    private void pickReference(){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("image/*"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i,PICK_REF); }
    private void saveObj(){ Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("text/plain"); i.putExtra(Intent.EXTRA_TITLE,"my_character.obj"); startActivityForResult(i,SAVE_OBJ); }
    @Override protected void onActivityResult(int r,int c,Intent d){ super.onActivityResult(r,c,d); if(c!=RESULT_OK||d==null||d.getData()==null)return;
        if(r==PICK_REF){ try(InputStream in=getContentResolver().openInputStream(d.getData())){ Bitmap bm=BitmapFactory.decodeStream(in); model.setReference(bm); info.setText("Референс загружен. Добавляй геометрию, вращай и приближай модель."); }catch(Exception e){info.setText("Не удалось открыть изображение.");}}
        if(r==SAVE_OBJ){ try(OutputStream out=getContentResolver().openOutputStream(d.getData())){ out.write(model.toObj().getBytes("UTF-8")); Toast.makeText(this,"OBJ сохранён",Toast.LENGTH_LONG).show(); }catch(Exception e){Toast.makeText(this,"Ошибка экспорта: "+e.getMessage(),Toast.LENGTH_LONG).show();}}
    }

    static class Mesh { String name; float[] v; int[] f; int color; Mesh(String n,float[] a,int[] b,int c){name=n;v=a;f=b;color=c;} }
    static class Face { float[] x,y; float depth; int color; }
    class ModelView extends View {
        ArrayList<Mesh> meshes=new ArrayList<>(); Paint p=new Paint(3); float yaw=-0.55f,pitch=0.22f,zoom=1f,lastX,lastY; Bitmap reference;
        ModelView(Activity a){super(a); setLayerType(View.LAYER_TYPE_SOFTWARE,null); addBody();}
        void setReference(Bitmap b){reference=b;invalidate();}
        void reset(){meshes.clear();addBody();}
        void removeLast(){if(meshes.size()>1)meshes.remove(meshes.size()-1);invalidate();}
        void addBody(){meshes.add(ellipsoid("body",0,0,0,.82f,1.05f,.58f,0xff9b633d));invalidate();}
        void addPaw(){meshes.add(ellipsoid("paw_pad",-.03f,-.72f,.02f,.58f,.30f,.25f,0xffb8784a));
            for(int i=0;i<4;i++){float x=-.42f+i*.26f; float z=.08f+(i%2)*.05f; meshes.add(ellipsoid("toe_"+(i+1),x,-.48f,z,.17f,.22f,.18f,0xffca9468));} invalidate();}
        void addTooth(){meshes.add(cone("tooth",.28f,.72f,.08f,.26f,0xfff4eee0));invalidate();}
        void addBump(){meshes.add(ellipsoid("small_bump",.48f,.25f,.40f,.19f,.17f,.15f,0xffbd8156));invalidate();}
        void addEye(){meshes.add(ellipsoid("eye",-.25f,.36f,.52f,.12f,.12f,.07f,0xffe7f0f5)); meshes.add(ellipsoid("pupil",-.25f,.36f,.585f,.05f,.07f,.03f,0xff19212c));invalidate();}
        void addTail(){meshes.add(ellipsoid("tail",-.82f,-.26f,-.03f,.20f,.58f,.20f,0xff86502f));invalidate();}
        Mesh ellipsoid(String name,float cx,float cy,float cz,float rx,float ry,float rz,int color){
            int lat=14,lon=20; float[] v=new float[(lat+1)*(lon+1)*3]; int k=0;
            for(int a=0;a<=lat;a++){double th=Math.PI*a/lat; for(int b=0;b<=lon;b++){double ph=2*Math.PI*b/lon; v[k++]=cx+(float)(rx*Math.sin(th)*Math.cos(ph)); v[k++]=cy+(float)(ry*Math.cos(th)); v[k++]=cz+(float)(rz*Math.sin(th)*Math.sin(ph));}}
            int[] f=new int[lat*lon*6]; k=0; for(int a=0;a<lat;a++)for(int b=0;b<lon;b++){int q=a*(lon+1)+b; int n=q+lon+1; f[k++]=q;f[k++]=n;f[k++]=q+1; f[k++]=q+1;f[k++]=n;f[k++]=n+1;}
            return new Mesh(name,v,f,color);
        }
        Mesh cone(String name,float cx,float baseY,float cz,float h,int color){
            int n=24; float[] v=new float[(n+1)*3]; v[0]=cx;v[1]=baseY+h;v[2]=cz;
            for(int i=0;i<n;i++){double a=2*Math.PI*i/n; v[(i+1)*3]=cx+.18f*(float)Math.cos(a);v[(i+1)*3+1]=baseY;v[(i+1)*3+2]=cz+.18f*(float)Math.sin(a);}
            int[] f=new int[n*6]; int k=0; for(int i=0;i<n;i++){int j=(i+1)%n+1;f[k++]=0;f[k++]=i+1;f[k++]=j;f[k++]=1;f[k++]=j;f[k++]=i+1;}
            return new Mesh(name,v,f,color);
        }
        @Override protected void onDraw(Canvas c){
            super.onDraw(c); c.drawColor(0xff172238);
            p.setColor(0xff283954);p.setStrokeWidth(1);
            for(int i=0;i<getWidth();i+=42)c.drawLine(i,0,i,getHeight(),p);
            for(int j=0;j<getHeight();j+=42)c.drawLine(0,j,getWidth(),j,p);
            if(reference!=null){p.setColor(0xff101722);c.drawRoundRect(10,10,112,145,10,10,p);p.setAlpha(255);Rect src=new Rect(0,0,reference.getWidth(),reference.getHeight());RectF dst=new RectF(14,14,108,141);p.setFilterBitmap(true);c.drawBitmap(reference,src,dst,p);}
            ArrayList<Face> faces=new ArrayList<>(); float scale=Math.min(getWidth(),getHeight())*.29f*zoom;
            for(Mesh m:meshes) for(int i=0;i<m.f.length;i+=3){
                float[][] pts=new float[3][3]; float depth=0;
                for(int j=0;j<3;j++){int idx=m.f[i+j]*3;float x=m.v[idx],y=m.v[idx+1],z=m.v[idx+2];
                    float xx=(float)(x*Math.cos(yaw)-z*Math.sin(yaw));float zz=(float)(x*Math.sin(yaw)+z*Math.cos(yaw));
                    float yy=(float)(y*Math.cos(pitch)-zz*Math.sin(pitch));float z2=(float)(y*Math.sin(pitch)+zz*Math.cos(pitch));
                    pts[j][0]=getWidth()/2f+xx*scale;pts[j][1]=getHeight()*.55f-yy*scale;pts[j][2]=z2;depth+=z2/3f;
                }
                float shade=Math.max(.48f,Math.min(1.16f,.88f+depth*.10f)); Face f=new Face(); f.x=new float[]{pts[0][0],pts[1][0],pts[2][0]}; f.y=new float[]{pts[0][1],pts[1][1],pts[2][1]}; f.depth=depth;
                f.color=shade(m.color,shade); faces.add(f);
            }
            Collections.sort(faces,(a,b)->Float.compare(a.depth,b.depth));
            for(Face f:faces){Path path=new Path();path.moveTo(f.x[0],f.y[0]);path.lineTo(f.x[1],f.y[1]);path.lineTo(f.x[2],f.y[2]);path.close();p.setColor(f.color);p.setStyle(Paint.Style.FILL);c.drawPath(path,p);}
            p.setColor(0xffd5deea);p.setTextSize(12);c.drawText("Проведи пальцем — вращение • кнопки добавляют детали",12,getHeight()-14,p);
        }
        int shade(int color,float a){int r=Math.min(255,(int)(Color.red(color)*a)),g=Math.min(255,(int)(Color.green(color)*a)),b=Math.min(255,(int)(Color.blue(color)*a));return Color.rgb(r,g,b);}
        @Override public boolean onTouchEvent(android.view.MotionEvent e){ if(e.getAction()==MotionEvent.ACTION_DOWN){lastX=e.getX();lastY=e.getY();return true;}
            if(e.getAction()==MotionEvent.ACTION_MOVE){float dx=e.getX()-lastX,dy=e.getY()-lastY;yaw+=dx*.009f;pitch=Math.max(-1.15f,Math.min(1.15f,pitch+dy*.008f));lastX=e.getX();lastY=e.getY();invalidate();return true;}return true;}
        String toObj(){StringBuilder s=new StringBuilder("# SketchForge 3D model\n");int offset=1;
            for(Mesh m:meshes){s.append("o ").append(m.name.replace(' ','_')).append("\n");for(int i=0;i<m.v.length;i+=3)s.append(String.format(Locale.US,"v %.5f %.5f %.5f\n",m.v[i],m.v[i+1],m.v[i+2]));
                for(int i=0;i<m.f.length;i+=3)s.append("f ").append(m.f[i]+offset).append(" ").append(m.f[i+1]+offset).append(" ").append(m.f[i+2]+offset).append("\n");
                offset+=m.v.length/3;
            }return s.toString();}
    }
}
