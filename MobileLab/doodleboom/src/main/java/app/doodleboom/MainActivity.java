package app.doodleboom;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import android.media.ToneGenerator;
import android.media.AudioManager;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
    DrawingView board; TextView status;
    static final int SAVE=31;
    ToneGenerator tones;
    @Override public void onCreate(Bundle b){super.onCreate(b);
        getWindow().setStatusBarColor(0xff19223a);getWindow().setNavigationBarColor(0xff19223a);
        tones=new ToneGenerator(AudioManager.STREAM_MUSIC,45);
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(0xff202b46);
        TextView title=new TextView(this);title.setText("DOODLEBOOM  ✦  волшебная рисовалка");title.setTextColor(-1);title.setTextSize(18);title.setPadding(12,12,8,6);root.addView(title);
        status=new TextView(this);status.setText("Рисуй пальцем. Выбирай странные кисти и штампы.");status.setTextColor(0xffd5dded);status.setPadding(12,0,8,6);root.addView(status);
        root.addView(buttonRow(new String[]{"Карандаш","Неон","Радуга","Спрей","Ластик","Спираль"},new Runnable[]{
            ()->setTool(DrawingView.PENCIL),()->setTool(DrawingView.NEON),()->setTool(DrawingView.RAINBOW),
            ()->setTool(DrawingView.SPRAY),()->setTool(DrawingView.ERASER),()->setTool(DrawingView.SPIRAL)}));
        board=new DrawingView();root.addView(board,new LinearLayout.LayoutParams(-1,0,1));
        root.addView(buttonRow(new String[]{"⭐ Штамп","❤️ Штамп","☀ Штамп","↶ Отмена","Очистить","Сохранить PNG"},new Runnable[]{
            ()->board.setTool(DrawingView.STAR),()->board.setTool(DrawingView.HEART),()->board.setTool(DrawingView.SUN),
            ()->board.undo(),()->board.clearBoard(),()->savePng()}));
        root.addView(buttonRow(new String[]{"Красный","Оранжевый","Жёлтый","Зелёный","Синий","Фиолетовый","Чёрный"},new Runnable[]{
            ()->board.setColor(0xfff04452),()->board.setColor(0xffff923f),()->board.setColor(0xffffd84a),
            ()->board.setColor(0xff37c985),()->board.setColor(0xff3a9fff),()->board.setColor(0xffad79ff),()->board.setColor(0xff222638)}));
        setContentView(root);
    }
    void setTool(int t){board.setTool(t);tones.startTone(ToneGenerator.TONE_PROP_BEEP,70);status.setText("Инструмент выбран: "+board.toolName());}
    View buttonRow(String[] labels,Runnable[] acts){HorizontalScrollView h=new HorizontalScrollView(this);h.setHorizontalScrollBarEnabled(false);LinearLayout row=new LinearLayout(this);row.setPadding(4,2,4,4);
        for(int i=0;i<labels.length;i++){Runnable a=acts[i];Button b=new Button(this);b.setText(labels[i]);b.setAllCaps(false);b.setTextSize(10);b.setTextColor(-1);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff40547c));b.setOnClickListener(v->a.run());row.addView(b,new LinearLayout.LayoutParams(-2,46));}h.addView(row);return h;}
    void savePng(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/png");i.putExtra(Intent.EXTRA_TITLE,"DoodleBoom-art.png");startActivityForResult(i,SAVE);}
    @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r==SAVE&&c==RESULT_OK&&d!=null){try(OutputStream o=getContentResolver().openOutputStream(d.getData())){board.exportBitmap().compress(Bitmap.CompressFormat.PNG,100,o);Toast.makeText(this,"Картинка сохранена",Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(this,"Не удалось сохранить",Toast.LENGTH_SHORT).show();}}}
    @Override protected void onDestroy(){if(tones!=null)tones.release();super.onDestroy();}

    class DrawingView extends View {
        static final int PENCIL=0,NEON=1,RAINBOW=2,SPRAY=3,ERASER=4,SPIRAL=5,STAR=6,HEART=7,SUN=8;
        Bitmap bitmap;Canvas bitmapCanvas;Paint paint=new Paint(3);Path path=new Path();int tool=PENCIL,color=0xff18243b;float oldX,oldY;ArrayDeque<Bitmap> history=new ArrayDeque<>();Random random=new Random();
        DrawingView(){super(MainActivity.this);setBackgroundColor(-1);setLayerType(View.LAYER_TYPE_SOFTWARE,null);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);}
        @Override protected void onSizeChanged(int w,int h,int ow,int oh){super.onSizeChanged(w,h,ow,oh);if(w>0&&h>0&&bitmap==null){bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.WHITE);bitmapCanvas=new Canvas(bitmap);}}
        void saveUndo(){if(bitmap==null)return;history.push(bitmap.copy(Bitmap.Config.ARGB_8888,true));while(history.size()>12)history.removeLast();}
        void undo(){if(!history.isEmpty()){bitmap=history.pop();bitmapCanvas=new Canvas(bitmap);invalidate();}}
        void clearBoard(){saveUndo();bitmap.eraseColor(Color.WHITE);invalidate();}
        void setColor(int c){color=c;tool=PENCIL;invalidate();}
        void setTool(int t){tool=t;invalidate();}
        String toolName(){String[] n={"Карандаш","Неоновая кисть","Радуга","Спрей","Ластик","Спираль","Звёздочка","Сердце","Солнце"};return n[Math.max(0,Math.min(n.length-1,tool))];}
        Bitmap exportBitmap(){return bitmap;}
        @Override protected void onDraw(Canvas c){super.onDraw(c);if(bitmap!=null)c.drawBitmap(bitmap,0,0,null);
            if(tool==PENCIL||tool==NEON||tool==RAINBOW||tool==ERASER){configure(paint);c.drawPath(path,paint);}}
        void configure(Paint q){q.setStyle(Paint.Style.STROKE);q.setStrokeWidth(tool==ERASER?34:tool==NEON?19:tool==RAINBOW?15:6);q.setStrokeCap(Paint.Cap.ROUND);q.setStrokeJoin(Paint.Join.ROUND);
            if(tool==ERASER){q.setColor(Color.WHITE);q.setMaskFilter(null);q.setShader(null);}else if(tool==NEON){q.setColor(color);q.setShadowLayer(17,color,0,0xff000000);q.setMaskFilter(null);q.setShader(null);}else if(tool==RAINBOW){q.setColor(color);q.setShader(new LinearGradient(0,0,260,200,0xffff375f,0xff368bff,Shader.TileMode.MIRROR));q.setShadowLayer(3,0,0,0xffee88ff);}else{q.setColor(color);q.setShader(null);q.setShadowLayer(0,0,0,0);}}
        @Override public boolean onTouchEvent(android.view.MotionEvent e){float x=e.getX(),y=e.getY();
            if(e.getAction()==MotionEvent.ACTION_DOWN){saveUndo();oldX=x;oldY=y;path.reset();path.moveTo(x,y);
                if(tool==STAR||tool==HEART||tool==SUN){drawStamp(x,y,tool);invalidate();return true;}
                if(tool==SPIRAL){drawSpiral(x,y);invalidate();return true;}
                if(tool==SPRAY){spray(x,y);invalidate();return true;}return true;}
            if(e.getAction()==MotionEvent.ACTION_MOVE){if(tool==SPRAY){spray(x,y);}else if(tool==PENCIL||tool==NEON||tool==RAINBOW||tool==ERASER){path.quadTo(oldX,oldY,(x+oldX)/2,(y+oldY)/2);}oldX=x;oldY=y;invalidate();return true;}
            if(e.getAction()==MotionEvent.ACTION_UP){if(tool==PENCIL||tool==NEON||tool==RAINBOW||tool==ERASER){path.lineTo(x,y);configure(paint);bitmapCanvas.drawPath(path,paint);paint.clearShadowLayer();paint.setShader(null);}path.reset();invalidate();return true;}return true;}
        void spray(float x,float y){Paint q=new Paint(3);q.setColor(color);q.setStyle(Paint.Style.FILL);for(int i=0;i<34;i++){double a=random.nextDouble()*Math.PI*2;float r=(float)random.nextGaussian()*22;bitmapCanvas.drawCircle(x+(float)Math.cos(a)*r,y+(float)Math.sin(a)*r,1.2f+random.nextFloat()*2.3f,q);}}
        void drawStamp(float x,float y,int type){Paint q=new Paint(3);q.setColor(type==STAR?0xffffc940:type==HEART?0xfff04c74:0xffffad39);q.setStyle(Paint.Style.FILL);
            if(type==STAR){Path s=new Path();for(int i=0;i<10;i++){double a=-Math.PI/2+i*Math.PI/5;float r=i%2==0?34:15;float px=x+(float)Math.cos(a)*r,py=y+(float)Math.sin(a)*r;if(i==0)s.moveTo(px,py);else s.lineTo(px,py);}s.close();bitmapCanvas.drawPath(s,q);}
            else if(type==HEART){Path h=new Path();h.moveTo(x,y+28);h.cubicTo(x-55,y-5,x-24,y-48,x,y-20);h.cubicTo(x+26,y-50,x+55,y-5,x,y+28);h.close();bitmapCanvas.drawPath(h,q);}
            else{bitmapCanvas.drawCircle(x,y,25,q);q.setColor(0xffffdf70);for(int i=0;i<8;i++){double a=i*Math.PI/4;bitmapCanvas.drawLine(x+(float)Math.cos(a)*30,y+(float)Math.sin(a)*30,x+(float)Math.cos(a)*43,y+(float)Math.sin(a)*43,q);}}
        }
        void drawSpiral(float x,float y){Paint q=new Paint(3);q.setColor(color);q.setStyle(Paint.Style.STROKE);q.setStrokeWidth(5);Path s=new Path();for(int i=0;i<180;i++){double a=i*.13;float r=i*.23f;float px=x+(float)Math.cos(a)*r,py=y+(float)Math.sin(a)*r;if(i==0)s.moveTo(px,py);else s.lineTo(px,py);}bitmapCanvas.drawPath(s,q);q.setColor(0xffff5b9d);q.setStrokeWidth(2);bitmapCanvas.drawCircle(x,y,38,q);}
    }
}
