package com.verity.realminecraft;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@SuppressWarnings("deprecation")
public class MainActivity extends Activity implements SurfaceHolder.Callback {
    private static final int CAMERA_REQUEST = 41;
    private SurfaceView cameraSurface;
    private SurfaceHolder cameraHolder;
    private Camera camera;
    private boolean surfaceReady = false;
    private GameOverlay gameOverlay;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.rgb(12, 20, 16));
        getWindow().setNavigationBarColor(Color.rgb(12, 20, 16));

        FrameLayout root = new FrameLayout(this);
        cameraSurface = new SurfaceView(this);
        cameraSurface.setZOrderMediaOverlay(false);
        cameraHolder = cameraSurface.getHolder();
        cameraHolder.addCallback(this);
        root.addView(cameraSurface, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        gameOverlay = new GameOverlay(this);
        gameOverlay.setCallbacks(new GameOverlay.Callbacks() {
            @Override public void requestFriendName() { showFriendNameDialog(); }
            @Override public void clearFriendTags() {
                gameOverlay.clearFriends();
                Toast.makeText(MainActivity.this, "Метки друзей очищены", Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(gameOverlay, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);

        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            gameOverlay.setCameraGranted(true);
            if (surfaceReady) startCamera();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);
        }
    }

    private void showFriendNameDialog() {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Например: Alex");
        input.setText(gameOverlay.getFriendName());
        new AlertDialog.Builder(this)
                .setTitle("Имя друга")
                .setMessage("Это локальная метка: поставь её касанием в нужное место камеры.")
                .setView(input)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) name = "Друг";
                    gameOverlay.setFriendName(name);
                    Toast.makeText(this, "Теперь коснись места для метки", Toast.LENGTH_SHORT).show();
                }).show();
    }

    @Override public void surfaceCreated(SurfaceHolder holder) {
        surfaceReady = true;
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCamera();
    }

    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (camera != null) {
            try {
                camera.stopPreview();
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (Exception ignored) { releaseCamera(); }
        }
    }

    @Override public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceReady = false;
        releaseCamera();
    }

    private void startCamera() {
        if (!surfaceReady || camera != null ||
                checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return;
        try {
            camera = Camera.open();
            Camera.Parameters params = camera.getParameters();
            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes != null && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            } else if (focusModes != null && focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
            camera.setParameters(params);
            camera.setDisplayOrientation(90);
            camera.setPreviewDisplay(cameraHolder);
            camera.startPreview();
            gameOverlay.setCameraGranted(true);
        } catch (Exception error) {
            releaseCamera();
            gameOverlay.setCameraGranted(false);
            Toast.makeText(this, "Не удалось открыть камеру. Проверь разрешение и перезапусти приложение.", Toast.LENGTH_LONG).show();
        }
    }

    private void releaseCamera() {
        if (camera != null) {
            try { camera.stopPreview(); } catch (Exception ignored) {}
            try { camera.release(); } catch (Exception ignored) {}
            camera = null;
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == CAMERA_REQUEST) {
            boolean granted = results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED;
            gameOverlay.setCameraGranted(granted);
            if (granted) startCamera();
            else Toast.makeText(this, "Без доступа к камере нельзя показать блоки поверх реального изображения.", Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (surfaceReady && checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCamera();
    }

    @Override protected void onPause() {
        releaseCamera();
        super.onPause();
    }

    static class GameOverlay extends View {
        interface Callbacks {
            void requestFriendName();
            void clearFriendTags();
        }

        private static final String[] BLOCKS = {
            "Трава", "Земля", "Камень", "Булыжник", "Доски", "Дуб", "Листва", "Песок",
            "Песчаник", "Гравий", "Кирпичи", "Стекло", "Белая шерсть", "Красная шерсть",
            "Синяя шерсть", "Зелёная шерсть", "Чёрная шерсть", "Жёлтая шерсть", "Обсидиан",
            "Бедрок", "Алмазный", "Золотой", "Железный", "Изумрудный", "Редстоун",
            "Угольный", "Светокамень", "Морской фонарь", "ТНТ", "Книжная полка",
            "Верстак", "Печь", "Сундук", "Адский камень", "Незерит. кирпич", "Песок душ",
            "Лёд", "Снег", "Глина", "Мох"
        };
        private static final int[] BLOCK_COLORS = {
            0xff65a94c,0xff98613d,0xff858585,0xff777777,0xffb98a50,0xff8a6038,0xff477b36,
            0xffe4d49a,0xffd9c28d,0xffa49e91,0xffa34d40,0xffa6dfe0,0xffe9e9e9,0xffc84343,
            0xff466cc3,0xff4d9149,0xff303039,0xffe5cc43,0xff29213b,0xff4b4b51,0xff43c6d4,
            0xffe4b83d,0xffaeb4b9,0xff38ad68,0xffa93636,0xff373737,0xffe6be54,0xffa7dfe3,
            0xffd74335,0xff895f3f,0xffa27b4c,0xff797777,0xff986d3e,0xff6b3533,0xff68504b,
            0xff80bfe8,0xffe9f2f6,0xffaaa5a0,0xffb6b8aa,0xff638a50
        };
        private static final String[] MOBS = {
            "Зомби", "Скелет", "Крипер", "Эндермен", "Паук",
            "Слизень", "Свинья", "Корова", "Овца", "Курица"
        };
        private static final int[] MOB_COLORS = {
            0xff4d9b55,0xffd5d1c5,0xff65b94d,0xff272332,0xff29252a,
            0xff72cf62,0xffed9da6,0xff8a6549,0xffe9e6dc,0xfff1eee5
        };

        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final List<PlacedBlock> blocks = new ArrayList<>();
        private final List<Creature> creatures = new ArrayList<>();
        private final List<FriendTag> friends = new ArrayList<>();
        private final Random random = new Random();
        private final float d;
        private Callbacks callbacks;
        private boolean cameraGranted = false;
        private int tab = 0;
        private int selectedBlock = 0;
        private int selectedMob = 0;
        private int selectedSlot = 0;
        private int[] blockHotbar = {0,1,2,3,4,5,6,7,8};
        private int[] mobHotbar = {0,1,2,3,4,5,6,7,8};
        private int inventoryScroll = 0;
        private int hearts = 10;
        private int hunger = 10;
        private int xp = 0;
        private long lastHungerTick = System.currentTimeMillis();
        private float downX, downY;
        private long downTime;
        private String friendName = "Друг";

        GameOverlay(Context context) {
            super(context);
            d = getResources().getDisplayMetrics().density;
            p.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD));
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        void setCallbacks(Callbacks c) { callbacks = c; }
        void setCameraGranted(boolean value) { cameraGranted = value; invalidate(); }
        String getFriendName() { return friendName; }
        void setFriendName(String value) { friendName = value; invalidate(); }
        void clearFriends() { friends.clear(); invalidate(); }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float w = getWidth(), h = getHeight();
            drawHud(c, w);
            drawWorldObjects(c);
            drawBottomPanel(c, w, h);
            if (!cameraGranted) {
                drawLabel(c, "РАЗРЕШИ ДОСТУП К КАМЕРЕ", w * .5f, h * .34f, 13*d, 0xffffffff, 0xdd18231d);
            }
            if (hearts <= 0) {
                drawLabel(c, "ИГРОК ВЫБЫЛ — КОСНИСЬ, ЧТОБЫ ВОССТАНОВИТЬ HP", w*.5f, h*.48f, 10*d, 0xffffffff, 0xdd7b2525);
            }
            updateCreatures();
            postInvalidateDelayed(350);
        }

        private void drawHud(Canvas c, float w) {
            p.setColor(0xdd101913); c.drawRoundRect(12*d, 10*d, w-12*d, 65*d, 15*d, 15*d, p);
            p.setTextSize(11*d); p.setColor(0xffc5e8c8); p.setTextAlign(Paint.Align.LEFT);
            c.drawText("BLOCKWORLD  •  V1", 23*d, 27*d, p);
            p.setTextSize(19*d); p.setColor(0xffff5555);
            for (int i=0;i<5;i++) c.drawText(i*2 < hearts ? "♥" : (i*2+1 < hearts ? "♥" : "♡"), 22*d+i*25*d, 53*d, p);
            p.setTextSize(18*d); p.setColor(0xffffb74d);
            for (int i=0;i<5;i++) c.drawText(i*2 < hunger ? "◆" : (i*2+1 < hunger ? "◆" : "◇"), w-145*d+i*24*d, 51*d, p);
            p.setTextSize(9*d); p.setColor(0xffd9eadb); p.setTextAlign(Paint.Align.RIGHT);
            c.drawText("XP "+xp, w-22*d, 27*d, p);
        }

        private void drawBottomPanel(Canvas c, float w, float h) {
            float top = h-202*d;
            p.setColor(0xee101913); c.drawRoundRect(8*d, top, w-8*d, h-8*d, 16*d, 16*d, p);
            String[] tabs = {"БЛОКИ", "МОБЫ", "ДРУЗЬЯ"};
            float tabW = (w-28*d)/3f;
            for (int i=0;i<3;i++) {
                float x = 14*d+i*tabW;
                p.setColor(tab==i ? 0xff397b42 : 0xff26332b);
                c.drawRoundRect(x, top+8*d, x+tabW-4*d, top+42*d, 9*d, 9*d, p);
                drawText(c, tabs[i], x+(tabW-4*d)/2, top+30*d, 10*d, 0xffffffff, true);
            }

            float rowY = top+53*d;
            if (tab == 2) {
                p.setColor(0xff33463a); c.drawRoundRect(16*d,rowY,w-16*d,rowY+56*d,10*d,10*d,p);
                drawText(c, "＋ ИМЯ ДРУГА", w*.34f, rowY+33*d, 12*d, 0xffffffff, true);
                drawText(c, "ОЧИСТИТЬ", w*.78f, rowY+33*d, 11*d, 0xffffc8a8, true);
                drawText(c, "Коснись места в кадре, чтобы поставить метку", w*.5f, rowY+73*d, 9*d, 0xffd1ded4, true);
            } else {
                String[] list = tab == 0 ? BLOCKS : MOBS;
                int count = Math.min(8, Math.max(1, (int)((w-30*d)/(58*d))));
                float cellW = (w-28*d)/count;
                for (int i=0;i<count;i++) {
                    int item = inventoryScroll+i;
                    if (item >= list.length) break;
                    float x = 14*d+i*cellW;
                    p.setColor((tab==0 ? item==selectedBlock : item==selectedMob) ? 0xff527f4d : 0xff27352d);
                    c.drawRoundRect(x,rowY,x+cellW-3*d,rowY+58*d,7*d,7*d,p);
                    if (tab==0) drawCube(c,x+cellW*.5f,rowY+23*d,19*d,BLOCK_COLORS[item]);
                    else drawMob(c,x+cellW*.5f,rowY+31*d,0.34f,item,0);
                    drawText(c, shortName(list[item]), x+cellW*.5f, rowY+51*d, 7.2f*d, 0xfff2f5f1, true);
                }
                drawText(c, "‹", 9*d, rowY+35*d, 21*d, 0xffdbe8dc, true);
                drawText(c, "›", w-9*d, rowY+35*d, 21*d, 0xffdbe8dc, true);
            }

            float hotY = top+120*d;
            float slotW = (w-28*d)/9f;
            for (int i=0;i<9;i++) {
                float x = 14*d+i*slotW;
                p.setColor(i==selectedSlot ? 0xffd4c46b : 0xff3b443e);
                c.drawRoundRect(x,hotY,x+slotW-2*d,hotY+56*d,6*d,6*d,p);
                if (tab==0) drawCube(c,x+slotW*.5f,hotY+27*d,22*d,BLOCK_COLORS[blockHotbar[i]%BLOCK_COLORS.length]);
                else if (tab==1) drawMob(c,x+slotW*.5f,hotY+35*d,0.36f,mobHotbar[i]%MOBS.length,0);
                else drawText(c, "★", x+slotW*.5f, hotY+35*d, 18*d, 0xffffd76a, true);
                drawText(c, ""+(i+1), x+slotW*.5f, hotY+52*d, 7*d, 0xffe4e7e3, true);
            }
        }

        private String shortName(String s) {
            if (s.length() <= 9) return s;
            return s.substring(0,8)+"…";
        }

        private void drawWorldObjects(Canvas c) {
            for (PlacedBlock b : blocks) {
                drawCube(c,b.x,b.y,b.size,BLOCK_COLORS[b.type]);
                drawText(c, BLOCKS[b.type], b.x, b.y+b.size*.75f, 8*d, 0xffffffff, true);
            }
            for (Creature m : creatures) {
                drawMob(c,m.x,m.y,m.scale,m.type,m.hp);
                drawLabel(c,MOBS[m.type]+"  "+m.hp+"/5",m.x,m.y-m.scale*54*d,8*d,0xffffffff,0xbb101913);
            }
            for (FriendTag f : friends) {
                p.setColor(0xfff2cf56); c.drawCircle(f.x,f.y,5*d,p);
                drawLabel(c,f.name,f.x,f.y-17*d,10*d,0xffffffff,0xdd29352c);
            }
        }

        private void updateCreatures() {
            long now = System.currentTimeMillis();
            if (now-lastHungerTick > 45000 && hunger>0) {
                hunger--; lastHungerTick=now;
            }
            float cx=getWidth()*.5f, cy=(getHeight()-202*d)*.48f;
            for (Creature m : creatures) {
                m.x += (random.nextFloat()-.5f)*1.5f*d;
                m.y += (random.nextFloat()-.5f)*1.2f*d;
                float dx=m.x-cx, dy=m.y-cy;
                if (dx*dx+dy*dy < 62*d*62*d && now-m.lastAttack>3500) {
                    m.lastAttack=now;
                    if (hearts>0) hearts=Math.max(0,hearts-1);
                }
            }
        }

        private void drawCube(Canvas c, float x, float y, float size, int color) {
            float half=size*.52f, topY=y-size*.55f, midY=y-size*.20f, bottomY=y+size*.34f;
            int dark=shade(color,.66f), light=shade(color,1.18f);
            Path top=new Path(); top.moveTo(x,topY); top.lineTo(x+half,midY-size*.18f);
            top.lineTo(x,midY+size*.18f); top.lineTo(x-half,midY-size*.18f); top.close();
            Path left=new Path(); left.moveTo(x-half,midY-size*.18f); left.lineTo(x,midY+size*.18f);
            left.lineTo(x,bottomY); left.lineTo(x-half,bottomY-size*.36f); left.close();
            Path right=new Path(); right.moveTo(x,midY+size*.18f); right.lineTo(x+half,midY-size*.18f);
            right.lineTo(x+half,bottomY-size*.36f); right.lineTo(x,bottomY); right.close();
            p.setColor(dark); c.drawPath(left,p); p.setColor(color); c.drawPath(right,p);
            p.setColor(light); c.drawPath(top,p);
            p.setColor(0x55302b25); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1*d);
            c.drawPath(top,p); c.drawPath(left,p); c.drawPath(right,p); p.setStyle(Paint.Style.FILL);
            p.setColor(0x22ffffff);
            for(int i=0;i<3;i++) c.drawRect(x-half*.55f+i*size*.13f,midY+size*.02f,x-half*.40f+i*size*.13f,midY+size*.12f,p);
        }

        private void drawMob(Canvas c, float x, float y, float scale, int type, int hp) {
            float s=d*scale;
            int base=MOB_COLORS[type%MOB_COLORS.length];
            float bodyW=26*s, bodyH=27*s, head=22*s;
            if (type==4) { // spider
                p.setColor(base);
                c.drawRoundRect(x-18*s,y-12*s,x+18*s,y+8*s,5*s,5*s,p);
                for(int i=0;i<4;i++) {
                    float xx=x-14*s+i*9*s;
                    c.drawLine(xx,y+1*s,xx-13*s,y+16*s,p);
                    c.drawLine(xx,y+1*s,xx+13*s,y+16*s,p);
                }
                p.setColor(0xffff3434); c.drawCircle(x-7*s,y-4*s,2*s,p); c.drawCircle(x+7*s,y-4*s,2*s,p);
                return;
            }
            if (type==5) { // slime
                p.setColor(0xaa65cf60); c.drawRoundRect(x-16*s,y-25*s,x+16*s,y+14*s,5*s,5*s,p);
                p.setColor(0xff245d2d); c.drawRect(x-9*s,y-12*s,x-5*s,y-7*s,p); c.drawRect(x+5*s,y-12*s,x+9*s,y-7*s,p);
                return;
            }
            // Simple home-made voxel silhouettes; no game texture files are used.
            p.setColor(shade(base,.72f));
            c.drawRect(x-8*s,y+14*s,x-2*s,y+32*s,p);
            c.drawRect(x+3*s,y+14*s,x+9*s,y+32*s,p);
            p.setColor(base);
            if(type==2) { // creeper body
                c.drawRect(x-12*s,y-5*s,x+12*s,y+19*s,p);
            } else if(type==6 || type==7 || type==8 || type==9) {
                c.drawRect(x-13*s,y-2*s,x+13*s,y+17*s,p);
            } else {
                c.drawRect(x-bodyW/2,y-4*s,x+bodyW/2,y+19*s,p);
            }
            int headColor=type==3?0xff191622:(type==1?0xffd8d5c9:base);
            p.setColor(headColor);
            c.drawRect(x-head/2,y-28*s,x+head/2,y-5*s,p);
            p.setColor(type==3?0xffb67cff:0xff202326);
            if(type==1) { p.setColor(0xff252525); }
            c.drawRect(x-7*s,y-20*s,x-3*s,y-16*s,p);
            c.drawRect(x+3*s,y-20*s,x+7*s,y-16*s,p);
            if(type==0 || type==2 || type==3) {
                p.setColor(0xff252525); c.drawRect(x-5*s,y-10*s,x+5*s,y-8*s,p);
            }
            if(type==6 || type==7 || type==8 || type==9) {
                p.setColor(0xff36302a);
                c.drawRect(x-9*s,y+30*s,x-2*s,y+34*s,p);
                c.drawRect(x+3*s,y+30*s,x+10*s,y+34*s,p);
            }
            if(type==7) {
                p.setColor(0xffeee7d6); c.drawRect(x-4*s,y-26*s,x+4*s,y-6*s,p);
            }
            if(type==9) {
                p.setColor(0xffd94e4e); c.drawRect(x-3*s,y-33*s,x+3*s,y-25*s,p);
            }
            if(hp>0 && hp<5) {
                p.setColor(0xffff5a52); c.drawRect(x-14*s,y-34*s,x-14*s+(28*s*hp/5f),y-31*s,p);
            }
        }

        private int shade(int color,float factor) {
            int r=Math.min(255,(int)(Color.red(color)*factor));
            int g=Math.min(255,(int)(Color.green(color)*factor));
            int b=Math.min(255,(int)(Color.blue(color)*factor));
            return Color.rgb(r,g,b);
        }

        private void drawText(Canvas c,String value,float x,float y,float size,int color,boolean center) {
            p.setTextSize(size); p.setColor(color); p.setTextAlign(center?Paint.Align.CENTER:Paint.Align.LEFT);
            c.drawText(value,x,y,p);
        }

        private void drawLabel(Canvas c,String value,float x,float y,float size,int textColor,int bg) {
            p.setTextSize(size); p.setTextAlign(Paint.Align.CENTER);
            float width=p.measureText(value)+14*d, height=size+12*d;
            p.setColor(bg); c.drawRoundRect(x-width/2,y-height/2,x+width/2,y+height/2,7*d,7*d,p);
            p.setColor(textColor); c.drawText(value,x,y+size*.34f,p);
        }

        @Override public boolean onTouchEvent(android.view.MotionEvent e) {
            float x=e.getX(), y=e.getY(), h=getHeight(), w=getWidth();
            float panelTop=h-202*d;
            if(e.getAction()==android.view.MotionEvent.ACTION_DOWN) {
                downX=x; downY=y; downTime=System.currentTimeMillis(); return true;
            }
            if(e.getAction()!=android.view.MotionEvent.ACTION_UP) return true;
            if(y>=panelTop) {
                if(y<panelTop+48*d) {
                    int selected=Math.max(0,Math.min(2,(int)((x-14*d)/((w-28*d)/3f))));
                    tab=selected; inventoryScroll=0; invalidate(); return true;
                }
                if(tab==2 && y<panelTop+116*d) {
                    if(x<w*.55f && callbacks!=null) callbacks.requestFriendName();
                    else if(callbacks!=null) callbacks.clearFriendTags();
                    return true;
                }
                if(tab!=2 && y<panelTop+115*d) {
                    int count=Math.min(8,Math.max(1,(int)((w-30*d)/(58*d))));
                    float cellW=(w-28*d)/count;
                    if(x<17*d) inventoryScroll=Math.max(0,inventoryScroll-1);
                    else if(x>w-17*d) inventoryScroll=Math.min((tab==0?BLOCKS.length:MOBS.length)-count,inventoryScroll+1);
                    else {
                        int index=inventoryScroll+(int)((x-14*d)/cellW);
                        if(tab==0 && index>=0 && index<BLOCKS.length) {
                            selectedBlock=index; blockHotbar[0]=index; selectedSlot=0;
                        } else if(tab==1 && index>=0 && index<MOBS.length) {
                            selectedMob=index; mobHotbar[0]=index; selectedSlot=0;
                        }
                    }
                    invalidate(); return true;
                }
                if(y>=panelTop+118*d && y<panelTop+184*d) {
                    int slot=Math.max(0,Math.min(8,(int)((x-14*d)/((w-28*d)/9f))));
                    selectedSlot=slot;
                    if(tab==0) selectedBlock=blockHotbar[slot];
                    if(tab==1) selectedMob=mobHotbar[slot];
                    invalidate(); return true;
                }
                return true;
            }
            if(hearts<=0) { hearts=10; hunger=10; invalidate(); return true; }
            long held=System.currentTimeMillis()-downTime;
            if(tab==0) {
                PlacedBlock hit=findBlock(x,y,34*d);
                if(hit!=null) blocks.remove(hit);
                else if(held>500) {
                    PlacedBlock nearest=findBlock(x,y,68*d);
                    if(nearest!=null) blocks.remove(nearest);
                } else {
                    if(blocks.size()<100) blocks.add(new PlacedBlock(selectedBlock,x,y,38*d));
                }
            } else if(tab==1) {
                Creature hit=findCreature(x,y,42*d);
                if(hit!=null) {
                    hit.hp--;
                    if(hit.hp<=0) { creatures.remove(hit); xp+=2; }
                } else if(creatures.size()<20) {
                    creatures.add(new Creature(selectedMob,x,y,1.0f));
                }
            } else {
                friends.add(new FriendTag(friendName,x,y));
            }
            invalidate(); return true;
        }

        private PlacedBlock findBlock(float x,float y,float radius) {
            for(int i=blocks.size()-1;i>=0;i--) {
                PlacedBlock b=blocks.get(i);
                if(Math.abs(x-b.x)<radius && Math.abs(y-b.y)<radius) return b;
            }
            return null;
        }
        private Creature findCreature(float x,float y,float radius) {
            for(int i=creatures.size()-1;i>=0;i--) {
                Creature m=creatures.get(i);
                if(Math.abs(x-m.x)<radius && Math.abs(y-m.y)<radius) return m;
            }
            return null;
        }

        static class PlacedBlock {
            int type; float x,y,size;
            PlacedBlock(int t,float px,float py,float s){type=t;x=px;y=py;size=s;}
        }
        static class Creature {
            int type,hp=5; float x,y,scale; long lastAttack=0;
            Creature(int t,float px,float py,float sc){type=t;x=px;y=py;scale=sc;}
        }
        static class FriendTag {
            String name; float x,y;
            FriendTag(String n,float px,float py){name=n;x=px;y=py;}
        }
    }
}