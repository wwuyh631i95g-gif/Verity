package app.doodleboom;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Random;

/** Retro-inspired drawing studio based on the visible drawing tools and effects in the supplied video. */
public class MainActivity extends Activity {
    private static final int PICK_IMAGE=41, SAVE_IMAGE=42;
    private ArtView art;
    private TextView hint;
    private int selectedStamp=0;
    private final String[] stampNames={"Цветок","Шарик","Нота","Звезда","Смайлик","Игрушка","Планета","Бабочка","Сердце","Гриб"};
    private final String[] stampGlyphs={"🌸","🎈","♫","★","☺","🧸","🪐","🦋","♥","🍄"};
    private final int[] palette={0xff161616,0xff777777,0xffffffff,0xffe53935,0xffff8f00,0xffffca28,0xff43a047,0xff00a7a7,0xff1e88e5,0xff3949ab,0xff8e24aa,0xffec407a,0xff795548,0xff90a4ae};

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(0xff243047);
        getWindow().setNavigationBarColor(0xff243047);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(0xffd8d6cb);

        LinearLayout titleBar=new LinearLayout(this); titleBar.setGravity(Gravity.CENTER_VERTICAL); titleBar.setPadding(10,5,8,5); titleBar.setBackgroundColor(0xff263750);
        TextView title=new TextView(this); title.setText("KID PIX STUDIO  ✦  творческая мастерская"); title.setTextColor(Color.WHITE); title.setTextSize(15); title.setTypeface(null,1);
        titleBar.addView(title,new LinearLayout.LayoutParams(0,42,1));
        Button studio=smallButton("СТУДИЯ",0xff45617e); studio.setOnClickListener(v->showStudioMenu()); titleBar.addView(studio);
        root.addView(titleBar);

        LinearLayout actions=new LinearLayout(this); actions.setPadding(4,4,4,4); actions.setBackgroundColor(0xffb8c0bd);
        addAction(actions,"↶","Отменить",()->{art.undo();});
        addAction(actions,"🧽","Ластик",()->art.setTool(ArtView.ERASER));
        addAction(actions,"✦","Штампы",()->showStamps());
        addAction(actions,"🖼","Фото",()->pickImage());
        addAction(actions,"▣","Сохранить",()->saveImage());
        addAction(actions,"⌫","Очистить",()->confirmClear());
        root.addView(actions);

        LinearLayout work=new LinearLayout(this); work.setOrientation(LinearLayout.HORIZONTAL); work.setBackgroundColor(0xffc8c8bf);
        ScrollView toolScroll=new ScrollView(this); toolScroll.setFillViewport(false); toolScroll.setBackgroundColor(0xffaeb7b5);
        LinearLayout rail=new LinearLayout(this); rail.setOrientation(LinearLayout.VERTICAL); rail.setPadding(3,3,3,3);
        addTool(rail,"✎","Карандаш",ArtView.PENCIL); addTool(rail,"🖌","Кисть",ArtView.BRUSH); addTool(rail,"╱","Линия",ArtView.LINE);
        addTool(rail,"▭","Прямоуг.",ArtView.RECT); addTool(rail,"◯","Овал",ArtView.OVAL); addTool(rail,"▨","Заливка",ArtView.FILL);
        addTool(rail,"✣","Брызги",ArtView.SPRAY); addTool(rail,"❉","Снежинки",ArtView.SNOW); addTool(rail,"○","Пузыри",ArtView.BUBBLES);
        addTool(rail,"〰","Волна",ArtView.WAVE); addTool(rail,"🌀","Спираль",ArtView.SPIRAL); addTool(rail,"🌈","Радуга",ArtView.RAINBOW);
        addTool(rail,"▤","Штамп-кисть",ArtView.STAMP_BRUSH); addTool(rail,"A","Текст",ArtView.TEXT); addTool(rail,"▱","Валик",ArtView.ROLLER);
        toolScroll.addView(rail); work.addView(toolScroll,new LinearLayout.LayoutParams(58,-1));
        art=new ArtView(); work.addView(art,new LinearLayout.LayoutParams(0,-1,1));
        root.addView(work,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout bottom=new LinearLayout(this); bottom.setOrientation(LinearLayout.VERTICAL); bottom.setPadding(4,4,4,4); bottom.setBackgroundColor(0xffaeb7b5);
        hint=new TextView(this); hint.setText("Коснись холста и рисуй. Для штампов выбери рисунок в меню «Штампы»."); hint.setTextSize(11); hint.setTextColor(0xff243047); hint.setPadding(5,0,5,3); bottom.addView(hint);
        HorizontalScrollView colors=new HorizontalScrollView(this); colors.setHorizontalScrollBarEnabled(false); LinearLayout swatches=new LinearLayout(this); swatches.setGravity(Gravity.CENTER_VERTICAL);
        for(int color:palette){ View s=new View(this); GradientDrawable g=new GradientDrawable();g.setShape(GradientDrawable.OVAL);g.setColor(color);g.setStroke(2,0xff45505b);s.setBackground(g); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(34,34);lp.setMargins(3,1,3,1);swatches.addView(s,lp);s.setOnClickListener(v->{art.setColor(color);hint.setText("Цвет выбран. Теперь рисуй на холсте.");}); }
        colors.addView(swatches); bottom.addView(colors); root.addView(bottom);
        setContentView(root);
        showStudioMenu(false);
    }

    private Button smallButton(String txt,int bg){Button b=new Button(this);b.setText(txt);b.setTextSize(10);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setPadding(3,0,3,0);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bg));return b;}
    private void addAction(LinearLayout row,String glyph,String label,Runnable action){LinearLayout cell=new LinearLayout(this);cell.setOrientation(LinearLayout.VERTICAL);cell.setGravity(Gravity.CENTER);Button b=smallButton(glyph,0xff425a72);b.setTextSize(16);cell.addView(b,new LinearLayout.LayoutParams(42,38));TextView t=new TextView(this);t.setText(label);t.setTextSize(9);t.setTextColor(0xff263547);cell.addView(t);b.setOnClickListener(v->action.run());row.addView(cell,new LinearLayout.LayoutParams(0,-2,1));}
    private void addTool(LinearLayout rail,String glyph,String label,int tool){Button b=smallButton(glyph,0xffd8dedb);b.setTextColor(0xff263547);b.setTextSize(18);rail.addView(b,new LinearLayout.LayoutParams(48,46));TextView t=new TextView(this);t.setText(label);t.setTextColor(0xff293747);t.setTextSize(8);t.setGravity(Gravity.CENTER);rail.addView(t,new LinearLayout.LayoutParams(48,17));b.setOnClickListener(v->{art.setTool(tool);hint.setText("Инструмент: "+label);});}

    private void showStudioMenu(){showStudioMenu(true);}
    private void showStudioMenu(boolean showToast){
        String[] modes={"Рисовать на холсте","Коллекция штампов","Кисти и эффекты","Открыть картинку"};
        new AlertDialog.Builder(this).setTitle("Kid Pix Studio • мастерская").setItems(modes,(d,which)->{
            if(which==0){art.setTool(ArtView.PENCIL);hint.setText("Режим рисования. Выбирай инструменты слева.");}
            else if(which==1)showStamps(); else if(which==2)showBrushGuide(); else pickImage();
        }).setNegativeButton("Продолжить",(d,w)->{}).show();
    }
    private void showStamps(){new AlertDialog.Builder(this).setTitle("Выбери штамп").setItems(stampNames,(d,w)->{selectedStamp=w;art.setStamp(w);art.setTool(ArtView.STAMP);hint.setText("Штамп «"+stampNames[w]+"»: касайся холста или веди пальцем.");}).show();}
    private void showBrushGuide(){String[] effects={"Спираль","Пузыри","Снежинки","Брызги-конфетти","Волнистая кисть","Радужный след","Штамп-кисть","Валик-узор"};int[] tools={ArtView.SPIRAL,ArtView.BUBBLES,ArtView.SNOW,ArtView.SPRAY,ArtView.WAVE,ArtView.RAINBOW,ArtView.STAMP_BRUSH,ArtView.ROLLER};new AlertDialog.Builder(this).setTitle("Необычные кисти и эффекты").setItems(effects,(d,w)->{art.setTool(tools[w]);hint.setText("Эффект выбран: "+effects[w]);}).show();}
    private void confirmClear(){new AlertDialog.Builder(this).setMessage("Очистить рисунок?").setNegativeButton("Нет",null).setPositiveButton("Очистить",(d,w)->art.clearArt()).show();}
    private void pickImage(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");startActivityForResult(i,PICK_IMAGE);}
    private void saveImage(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/png");i.putExtra(Intent.EXTRA_TITLE,"KidPix-picture.png");startActivityForResult(i,SAVE_IMAGE);}
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;try{if(req==PICK_IMAGE){try(InputStream in=getContentResolver().openInputStream(data.getData())){Bitmap bm=BitmapFactory.decodeStream(in);if(bm!=null){art.insertImage(bm);hint.setText("Картинка добавлена на холст.");}}}else if(req==SAVE_IMAGE){try(OutputStream out=getContentResolver().openOutputStream(data.getData())){art.export().compress(Bitmap.CompressFormat.PNG,100,out);hint.setText("PNG сохранён.");Toast.makeText(this,"Рисунок сохранён",Toast.LENGTH_SHORT).show();}}}catch(Exception e){Toast.makeText(this,"Не удалось выполнить действие с файлом",Toast.LENGTH_LONG).show();}}

    private class ArtView extends View {
        static final int PENCIL=0,BRUSH=1,LINE=2,RECT=3,OVAL=4,FILL=5,ERASER=6,SPRAY=7,SNOW=8,BUBBLES=9,WAVE=10,SPIRAL=11,RAINBOW=12,STAMP=13,STAMP_BRUSH=14,ROLLER=15,TEXT=16;
        Bitmap bitmap; Canvas paintCanvas; Path live=new Path(); int tool=PENCIL,color=0xff161616,stamp=0; float sx,sy,lx,ly; final Random rnd=new Random(); ArrayDeque<Bitmap> undo=new ArrayDeque<>();
        ArtView(){super(MainActivity.this);setLayerType(View.LAYER_TYPE_SOFTWARE,null);setBackgroundColor(Color.WHITE);}
        @Override protected void onSizeChanged(int w,int h,int oldW,int oldH){super.onSizeChanged(w,h,oldW,oldH);if(w<=0||h<=0)return;Bitmap next=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(next);c.drawColor(Color.WHITE);if(bitmap!=null)c.drawBitmap(bitmap,null,new Rect(0,0,w,h),new Paint(Paint.FILTER_BITMAP_FLAG));bitmap=next;paintCanvas=c;}
        void snapshot(){if(bitmap==null)return;undo.push(bitmap.copy(Bitmap.Config.ARGB_8888,true));while(undo.size()>7)undo.removeLast();}
        void undo(){if(undo.isEmpty())return;bitmap=undo.pop();paintCanvas=new Canvas(bitmap);invalidate();}
        void clearArt(){snapshot();bitmap.eraseColor(Color.WHITE);invalidate();}
        void setTool(int t){tool=t;invalidate();}
        void setColor(int c){color=c;if(tool==ERASER)tool=PENCIL;invalidate();}
        void setStamp(int s){stamp=s;tool=STAMP;}
        Bitmap export(){return bitmap;}
        void insertImage(Bitmap src){snapshot();Paint q=new Paint(Paint.FILTER_BITMAP_FLAG|Paint.ANTI_ALIAS_FLAG);float scale=Math.min((float)getWidth()/src.getWidth(),(float)getHeight()/src.getHeight())*.72f;float w=src.getWidth()*scale,h=src.getHeight()*scale;RectF dest=new RectF((getWidth()-w)/2f,(getHeight()-h)/2f,(getWidth()+w)/2f,(getHeight()+h)/2f);paintCanvas.drawBitmap(src,null,dest,q);invalidate();}
        @Override protected void onDraw(Canvas c){super.onDraw(c);if(bitmap==null)return;c.drawColor(Color.WHITE);c.drawBitmap(bitmap,0,0,null);if(!live.isEmpty()){Paint q=makePaint();if(tool==LINE){c.drawLine(sx,sy,lx,ly,q);}else if(tool==RECT){c.drawRect(sx,sy,lx,ly,q);}else if(tool==OVAL){c.drawOval(sx,sy,lx,ly,q);}else c.drawPath(live,q);}}
        Paint makePaint(){Paint q=new Paint(Paint.ANTI_ALIAS_FLAG);q.setColor(color);q.setStrokeCap(Paint.Cap.ROUND);q.setStrokeJoin(Paint.Join.ROUND);q.setStyle(Paint.Style.STROKE);q.setStrokeWidth(tool==PENCIL?3.2f:tool==BRUSH?9f:tool==ERASER?28f:tool==RAINBOW?13f:tool==WAVE?8f:5f);if(tool==ERASER){q.setColor(Color.WHITE);q.setStrokeWidth(30);}if(tool==BRUSH){q.setStrokeWidth(10);q.setAlpha(210);}if(tool==RAINBOW)q.setShader(new LinearGradient(0,0,240,180,new int[]{0xffff3b70,0xffffd740,0xff55c95a,0xff34aaff,0xffa46bff},null,Shader.TileMode.MIRROR));return q;}
        @Override public boolean onTouchEvent(MotionEvent e){float x=e.getX(),y=e.getY();switch(e.getActionMasked()){
            case MotionEvent.ACTION_DOWN:sx=lx=x;sy=ly=y;snapshot();live.reset();live.moveTo(x,y);if(tool==FILL){bucket((int)x,(int)y,color);invalidate();return true;}if(tool==TEXT){textAt(x,y);return true;}if(tool==STAMP){drawStamp(paintCanvas,x,y,stamp);invalidate();return true;}if(tool==SPIRAL){drawSpiral(x,y);invalidate();return true;}if(tool==SPRAY){spray(x,y);invalidate();return true;}if(tool==SNOW||tool==BUBBLES||tool==STAMP_BRUSH||tool==ROLLER){scatter(x,y,tool);invalidate();return true;}return true;
            case MotionEvent.ACTION_MOVE:if(tool==SPRAY)spray(x,y);else if(tool==SNOW||tool==BUBBLES||tool==STAMP_BRUSH||tool==ROLLER)scatter(x,y,tool);else if(tool==STAMP){if(Math.hypot(x-lx,y-ly)>24)drawStamp(paintCanvas,x,y,stamp);}else if(tool==PENCIL||tool==BRUSH||tool==ERASER||tool==RAINBOW||tool==WAVE){if(tool==WAVE){float mx=(lx+x)/2f;live.quadTo(lx,ly,mx,ly+(float)Math.sin(x*.06)*13);}else live.quadTo(lx,ly,(lx+x)/2f,(ly+y)/2f);}lx=x;ly=y;invalidate();return true;
            case MotionEvent.ACTION_UP:lx=x;ly=y;if(tool==LINE||tool==RECT||tool==OVAL){Paint q=makePaint();if(tool==LINE)paintCanvas.drawLine(sx,sy,x,y,q);else if(tool==RECT)paintCanvas.drawRect(sx,sy,x,y,q);else paintCanvas.drawOval(Math.min(sx,x),Math.min(sy,y),Math.max(sx,x),Math.max(sy,y),q);}else if(tool==PENCIL||tool==BRUSH||tool==ERASER||tool==RAINBOW||tool==WAVE){live.lineTo(x,y);paintCanvas.drawPath(live,makePaint());}live.reset();invalidate();return true;
        }return true;}
        void textAt(float x,float y){final EditText edit=new EditText(MainActivity.this);edit.setSingleLine(true);new AlertDialog.Builder(MainActivity.this).setTitle("Надпись на рисунке").setView(edit).setNegativeButton("Отмена",null).setPositiveButton("Добавить",(d,w)->{String t=edit.getText().toString();if(!t.isEmpty()){Paint q=new Paint(Paint.ANTI_ALIAS_FLAG);q.setColor(color);q.setTextSize(32);q.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));paintCanvas.drawText(t,x,y,q);invalidate();}}).show();}
        void drawSpiral(float x,float y){Paint q=new Paint(Paint.ANTI_ALIAS_FLAG);q.setColor(color);q.setStyle(Paint.Style.STROKE);q.setStrokeWidth(5);Path s=new Path();for(int i=0;i<260;i++){double a=i*.105;float r=i*.31f,px=x+(float)Math.cos(a)*r,py=y+(float)Math.sin(a)*r;if(i==0)s.moveTo(px,py);else s.lineTo(px,py);}paintCanvas.drawPath(s,q);}
        void spray(float x,float y){Paint q=new Paint(Paint.ANTI_ALIAS_FLAG);q.setColor(color);for(int i=0;i<44;i++){double a=rnd.nextDouble()*Math.PI*2;float r=(float)Math.sqrt(rnd.nextDouble())*32;paintCanvas.drawCircle(x+(float)Math.cos(a)*r,y+(float)Math.sin(a)*r,1.2f+rnd.nextFloat()*2.6f,q);}}
        void scatter(float x,float y,int mode){Paint q=new Paint(Paint.ANTI_ALIAS_FLAG);int n=mode==STAMP_BRUSH?4:mode==ROLLER?15:12;for(int i=0;i<n;i++){float dx=(rnd.nextFloat()-.5f)*(mode==ROLLER?42:34),dy=(rnd.nextFloat()-.5f)*(mode==ROLLER?42:34);if(mode==SNOW){q.setColor(0xff5275bd);drawSnow(paintCanvas,x+dx,y+dy,5+rnd.nextFloat()*4,q);}else if(mode==BUBBLES){q.setColor(color);q.setStyle(Paint.Style.STROKE);q.setStrokeWidth(1.8f);paintCanvas.drawCircle(x+dx,y+dy,4+rnd.nextFloat()*10,q);}else if(mode==STAMP_BRUSH){drawStamp(paintCanvas,x+dx,y+dy,stamp);}else{q.setColor(color);q.setStyle(Paint.Style.FILL);paintCanvas.drawCircle(x+dx,y+dy,2+rnd.nextFloat()*4,q);}}}
        void drawSnow(Canvas c,float x,float y,float r,Paint q){q.setStyle(Paint.Style.STROKE);q.setStrokeWidth(1.4f);for(int i=0;i<6;i++){double a=i*Math.PI/3;float ex=x+(float)Math.cos(a)*r,ey=y+(float)Math.sin(a)*r;c.drawLine(x,y,ex,ey,q);float bx=x+(float)Math.cos(a)*r*.65f,by=y+(float)Math.sin(a)*r*.65f;for(int side=-1;side<=1;side+=2)c.drawLine(bx,by,bx+(float)Math.cos(a+side*.7)*r*.35f,by+(float)Math.sin(a+side*.7)*r*.35f,q);}}
        void drawStamp(Canvas c,float x,float y,int index){Paint q=new Paint(Paint.ANTI_ALIAS_FLAG);q.setTextAlign(Paint.Align.CENTER);q.setTextSize(36);q.setShadowLayer(2,1,2,0x55000000);q.setColor(color);c.drawText(stampGlyphs[index%stampGlyphs.length],x,y+13,q);q.clearShadowLayer();}
        void bucket(int px,int py,int fill){if(bitmap==null||px<0||py<0||px>=bitmap.getWidth()||py>=bitmap.getHeight())return;int w=bitmap.getWidth(),h=bitmap.getHeight();int[] pixels=new int[w*h];bitmap.getPixels(pixels,0,w,0,0,w,h);int first=py*w+px;int start=pixels[first];if(start==fill)return;int tolerance=24;ArrayDeque<Integer> stack=new ArrayDeque<>();boolean[] seen=new boolean[w*h];stack.push(first);seen[first]=true;while(!stack.isEmpty()){int id=stack.pop();if(!similar(pixels[id],start,tolerance))continue;pixels[id]=fill;int xx=id%w,yy=id/w;if(xx>0&&!seen[id-1]){seen[id-1]=true;stack.push(id-1);}if(xx<w-1&&!seen[id+1]){seen[id+1]=true;stack.push(id+1);}if(yy>0&&!seen[id-w]){seen[id-w]=true;stack.push(id-w);}if(yy<h-1&&!seen[id+w]){seen[id+w]=true;stack.push(id+w);}}bitmap.setPixels(pixels,0,w,0,0,w,h);}
        boolean similar(int a,int b,int t){return Math.abs(Color.red(a)-Color.red(b))<t&&Math.abs(Color.green(a)-Color.green(b))<t&&Math.abs(Color.blue(a)-Color.blue(b))<t;}
    }
}