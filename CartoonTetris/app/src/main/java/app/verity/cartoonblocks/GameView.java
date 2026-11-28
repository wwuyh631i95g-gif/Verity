package app.verity.cartoonblocks;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.view.MotionEvent;
import android.view.View;
import java.util.Random;

public final class GameView extends View {
    private static final int C=10,R=20;
    private static final int[][][] SHAPES={
        {{1,1,1,1}},{{1,1},{1,1}},{{0,1,0},{1,1,1}},
        {{0,1,1},{1,1,0}},{{1,1,0},{0,1,1}},
        {{1,0,0},{1,1,1}},{{0,0,1},{1,1,1}}
    };
    private static final int[] COLORS={0xFF42D8F4,0xFFFFD35E,0xFFB68CFF,0xFF74E39D,0xFFFF779E,0xFF5F9FFF,0xFFFFA45C};
    private final Paint p=new Paint(3);
    private final Random random=new Random();
    private final ToneGenerator sound=new ToneGenerator(AudioManager.STREAM_MUSIC,35);
    private final int[][] board=new int[R][C];
    private final RectF[] controls=new RectF[5];
    private Piece current,next;
    private int score,lines,level=1;
    private boolean gameOver=false;
    private long lastDrop=0;
    private float scale=1,cell,boardX,boardY,boardW,boardH,downX,downY;
    private static final class Piece {
        int type,x,y; int[][] shape;
        Piece(int t){type=t;shape=new int[SHAPES[t].length][];for(int i=0;i<shape.length;i++)shape[i]=SHAPES[t][i].clone();x=C/2-shape[0].length/2;y=-1;}
    }
    public GameView(Context context){super(context);setLayerType(View.LAYER_TYPE_SOFTWARE,null);for(int i=0;i<5;i++)controls[i]=new RectF();newGame();}
    private void beep(int t){try{sound.startTone(t,60);}catch(Exception ignored){}}
    private void newGame(){for(int y=0;y<R;y++)for(int x=0;x<C;x++)board[y][x]=0;score=0;lines=0;level=1;gameOver=false;current=new Piece(random.nextInt(7));next=new Piece(random.nextInt(7));lastDrop=System.currentTimeMillis();invalidate();}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float w=getWidth(),h=getHeight();c.drawColor(0xFF090615);
        scale=Math.min(w/360f,h/650f);c.save();c.scale(scale,scale);float lw=w/scale,lh=h/scale;
        p.setColor(0xFF7DEBFA);p.setTextSize(10);p.setFakeBoldText(true);c.drawText("VERITY ARCADE / ORIGINAL BLOCKS",16,18,p);
        p.setColor(0xFFFFFFFF);p.setTextSize(22);c.drawText("CARTOON TETRIS",16,47,p);
        p.setColor(0xFFFFD56D);p.setTextSize(12);c.drawText("SCORE  "+score,16,75,p);
        p.setColor(0xFFB9C4DD);c.drawText("LINES  "+lines+"     LEVEL  "+level,16,92,p);
        p.setColor(0xFFB68CFF);p.setTextSize(9);c.drawText("NEXT",lw-64,28,p);
        cell=Math.min((lw-34)/C,(lh-230)/R);cell=Math.max(18,Math.min(31,cell));boardW=cell*C;boardH=cell*R;boardX=(lw-boardW)/2f;boardY=105;
        drawBoard(c);drawNext(c,lw);drawControls(c,lw,lh);
        if(gameOver){p.setColor(0xCC080713);c.drawRect(0,0,lw,lh,p);p.setColor(0xFFFF9AAB);p.setTextSize(25);p.setTextAlign(Paint.Align.CENTER);c.drawText("ИГРА ОКОНЧЕНА",lw/2,lh/2,p);p.setTextSize(12);p.setColor(-1);c.drawText("Нажми DROP, чтобы начать заново",lw/2,lh/2+24,p);p.setTextAlign(Paint.Align.LEFT);}
        c.restore();
        if(!gameOver){long now=System.currentTimeMillis();if(now-lastDrop>=Math.max(105,650-(level-1)*48)){stepDown();lastDrop=now;}postInvalidateDelayed(32);}
    }
    private void drawBoard(Canvas c){
        RectF frame=new RectF(boardX-7,boardY-7,boardX+boardW+7,boardY+boardH+7);
        p.setColor(0xFF25345D);p.setStyle(Paint.Style.FILL);c.drawRoundRect(frame,12,12,p);
        p.setColor(0xFF7CE6F2);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.5f);c.drawRoundRect(frame,12,12,p);p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF160D2C);c.drawRect(boardX,boardY,boardX+boardW,boardY+boardH,p);
        for(int y=0;y<R;y++)for(int x=0;x<C;x++){
            if(board[y][x]!=0)drawBlock(c,boardX+x*cell,boardY+y*cell,cell,board[y][x],true);
            else{p.setColor(0xFF211938);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.55f);c.drawRect(boardX+x*cell,boardY+y*cell,boardX+(x+1)*cell,boardY+(y+1)*cell,p);p.setStyle(Paint.Style.FILL);}
        }
        if(current!=null&&!gameOver){
            Piece ghost=new Piece(current.type);ghost.shape=current.shape;ghost.x=current.x;ghost.y=current.y;while(valid(ghost,ghost.x,ghost.y+1))ghost.y++;
            for(int y=0;y<ghost.shape.length;y++)for(int x=0;x<ghost.shape[y].length;x++)if(ghost.shape[y][x]!=0){float xx=boardX+(ghost.x+x)*cell,yy=boardY+(ghost.y+y)*cell;p.setColor(0x446EEAF5);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);c.drawRoundRect(xx+2,yy+2,xx+cell-2,yy+cell-2,4,4,p);p.setStyle(Paint.Style.FILL);}
            for(int y=0;y<current.shape.length;y++)for(int x=0;x<current.shape[y].length;x++)if(current.shape[y][x]!=0&&current.y+y>=0)drawBlock(c,boardX+(current.x+x)*cell,boardY+(current.y+y)*cell,cell,COLORS[current.type],true);
        }
    }
    private void drawBlock(Canvas c,float x,float y,float s,int color,boolean face){
        RectF r=new RectF(x+1.6f,y+1.6f,x+s-1.6f,y+s-1.6f);p.setColor(color);c.drawRoundRect(r,4,4,p);
        p.setColor(lighter(color));c.drawRoundRect(new RectF(r.left+2,r.top+2,r.right-2,r.top+s*.22f),3,3,p);
        p.setColor(0xAA14203A);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.05f);c.drawRoundRect(r,4,4,p);p.setStyle(Paint.Style.FILL);
        if(face&&s>=22){p.setColor(0xFF101628);float ey=y+s*.43f,rad=Math.max(1.25f,s*.055f);c.drawCircle(x+s*.36f,ey,rad,p);c.drawCircle(x+s*.64f,ey,rad,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,s*.045f));c.drawArc(x+s*.34f,y+s*.42f,x+s*.66f,y+s*.79f,15,150,false,p);p.setStyle(Paint.Style.FILL);}
    }
    private int lighter(int color){int a=color>>>24,r=(color>>>16)&255,g=(color>>>8)&255,b=color&255;return(a<<24)|(Math.min(255,r+(255-r)/5)<<16)|(Math.min(255,g+(255-g)/5)<<8)|Math.min(255,b+(255-b)/5);}
    private void drawNext(Canvas c,float w){if(next==null)return;float u=13,x=w-68,y=43;for(int r=0;r<next.shape.length;r++)for(int q=0;q<next.shape[r].length;q++)if(next.shape[r][q]!=0)drawBlock(c,x+q*u,y+r*u,u,COLORS[next.type],false);}
    private void drawControls(Canvas c,float w,float h){
        String[] names={"◀","▶","↻","▼","DROP"};float gap=7,left=12,bw=(w-24-gap*4)/5,y=h-61;
        for(int i=0;i<5;i++){float x=left+i*(bw+gap);controls[i].set(x,y,x+bw,y+46);p.setColor(i==4?0xFF304B70:0xFF22213D);c.drawRoundRect(controls[i],10,10,p);p.setColor(i==4?0xFF7CEAF4:0xFF655E91);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.2f);c.drawRoundRect(controls[i],10,10,p);p.setStyle(Paint.Style.FILL);p.setColor(-1);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(i==4?10:21);p.setFakeBoldText(true);c.drawText(names[i],controls[i].centerX(),controls[i].centerY()+(i==4?4:7),p);}
        p.setTextAlign(Paint.Align.CENTER);p.setTextSize(8);p.setColor(0xFF9CA8C1);c.drawText("СВАЙП ВЛЕВО/ВПРАВО · ВВЕРХ — ПОВОРОТ · ВНИЗ — ШАГ",w/2,h-9,p);p.setTextAlign(Paint.Align.LEFT);
    }
    private boolean valid(Piece a,int px,int py){for(int y=0;y<a.shape.length;y++)for(int x=0;x<a.shape[y].length;x++)if(a.shape[y][x]!=0){int bx=px+x,by=py+y;if(bx<0||bx>=C||by>=R)return false;if(by>=0&&board[by][bx]!=0)return false;}return true;}
    private void stepDown(){if(current==null||gameOver)return;if(valid(current,current.x,current.y+1))current.y++;else lockPiece();invalidate();}
    private void lockPiece(){
        for(int y=0;y<current.shape.length;y++)for(int x=0;x<current.shape[y].length;x++)if(current.shape[y][x]!=0){int by=current.y+y,bx=current.x+x;if(by<0){gameOver=true;beep(ToneGenerator.TONE_CDMA_ABBR_ALERT);return;}if(by<R&&bx>=0&&bx<C)board[by][bx]=COLORS[current.type];}
        int cleared=0;for(int y=R-1;y>=0;y--){boolean full=true;for(int x=0;x<C;x++)if(board[y][x]==0){full=false;break;}if(full){cleared++;for(int yy=y;yy>0;yy--)System.arraycopy(board[yy-1],0,board[yy],0,C);for(int x=0;x<C;x++)board[0][x]=0;y++;}}
        if(cleared>0){lines+=cleared;score+=new int[]{0,100,300,500,800}[cleared]*level;level=1+lines/10;beep(ToneGenerator.TONE_PROP_ACK);}else beep(ToneGenerator.TONE_PROP_BEEP);
        current=next;next=new Piece(random.nextInt(7));if(!valid(current,current.x,current.y))gameOver=true;
    }
    private void rotate(){if(current==null||gameOver)return;int h=current.shape.length,w=current.shape[0].length;int[][] a=new int[w][h];for(int y=0;y<h;y++)for(int x=0;x<w;x++)a[x][h-1-y]=current.shape[y][x];int[][] old=current.shape;current.shape=a;if(valid(current,current.x,current.y))beep(ToneGenerator.TONE_PROP_ACK);else if(valid(current,current.x-1,current.y))current.x--;else if(valid(current,current.x+1,current.y))current.x++;else current.shape=old;invalidate();}
    private void move(int dx){if(current!=null&&!gameOver&&valid(current,current.x+dx,current.y)){current.x+=dx;beep(ToneGenerator.TONE_DTMF_2);invalidate();}}
    private void hardDrop(){if(gameOver){newGame();return;}while(valid(current,current.x,current.y+1))current.y++;lockPiece();beep(ToneGenerator.TONE_PROP_ACK);lastDrop=System.currentTimeMillis();invalidate();}
    @Override public boolean dispatchTouchEvent(MotionEvent e){
        if(e.getAction()==MotionEvent.ACTION_DOWN){downX=e.getX();downY=e.getY();return true;}
        if(e.getAction()==MotionEvent.ACTION_UP){
            float x=e.getX()/scale,y=e.getY()/scale,lh=getHeight()/scale;
            if(y>lh-75){if(controls[0].contains(x,y))move(-1);else if(controls[1].contains(x,y))move(1);else if(controls[2].contains(x,y)){if(gameOver)newGame();else rotate();}else if(controls[3].contains(x,y))stepDown();else if(controls[4].contains(x,y))hardDrop();return true;}
            if(gameOver){newGame();return true;}
            float dx=e.getX()-downX,dy=e.getY()-downY;
            if(Math.abs(dx)>28)move(dx>0?1:-1);else if(dy>32)stepDown();else rotate();return true;
        }return true;
    }
    @Override protected void onDetachedFromWindow(){try{sound.release();}catch(Exception ignored){}super.onDetachedFromWindow();}
}