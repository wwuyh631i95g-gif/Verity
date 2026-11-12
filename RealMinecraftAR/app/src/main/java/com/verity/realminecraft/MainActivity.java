package com.verity.realminecraft;

import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.SceneView;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;

import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.Sceneform;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends FragmentActivity
        implements ArFragment.OnViewCreatedListener,
        ArFragment.OnSessionConfigurationListener,
        com.google.ar.sceneform.ux.BaseArFragment.OnTapArPlaneListener {

    private static final String[] BLOCKS = {
        "Трава","Земля","Камень","Булыжник","Доски","Дуб","Листва","Песок",
        "Песчаник","Гравий","Кирпичи","Стекло","Белая шерсть","Красная шерсть",
        "Синяя шерсть","Зелёная шерсть","Чёрная шерсть","Жёлтая шерсть","Обсидиан",
        "Бедрок","Алмазный","Золотой","Железный","Изумрудный","Редстоун","Угольный",
        "Светокамень","Морской фонарь","ТНТ","Книжная полка","Верстак","Печь",
        "Сундук","Адский камень","Незерит. кирпич","Песок душ","Лёд","Снег","Глина","Мох"
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
        "Зомби","Скелет","Крипер","Эндермен","Паук",
        "Слизень","Свинья","Корова","Овца","Курица"
    };
    private static final int[] MOB_COLORS = {
        0xff4d9b55,0xffd5d1c5,0xff65b94d,0xff272332,0xff51413b,
        0xff72cf62,0xffed9da6,0xff8a6549,0xffe9e6dc,0xfff1eee5
    };

    private ArFragment arFragment;
    private FrameLayout arHost;
    private HudView hud;
    private ModelRenderable[] blockModels = new ModelRenderable[BLOCKS.length];
    private ModelRenderable[] mobCubes = new ModelRenderable[10];
    private ModelRenderable darkCube;
    private ModelRenderable eyeCube;
    private ModelRenderable whiteCube;
    private final List<PlacedBlock> placedBlocks = new ArrayList<>();
    private final List<Creature> creatures = new ArrayList<>();
    private final List<AnchorNode> friendAnchors = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean arReady = false;
    private boolean renderablesReady = false;
    private boolean warnedNoAr = false;
    private String friendName = "Друг";
    private long lastDamageAt = 0;
    private long lastTick = 0;

    private final Runnable gameLoop = new Runnable() {
        @Override public void run() {
            updateMobs();
            if (hud != null) hud.invalidate();
            handler.postDelayed(this, 50);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(0xff101913);
        getWindow().setNavigationBarColor(0xff101913);

        FrameLayout root = new FrameLayout(this);
        arHost = new FrameLayout(this);
        arHost.setId(View.generateViewId());
        root.addView(arHost, new FrameLayout.LayoutParams(-1,-1));
        hud = new HudView(this);
        hud.setCallbacks(new HudView.Callbacks() {
            @Override public void onFriendName() { editFriendName(); }
            @Override public void onClearFriends() { clearFriends(); }
            @Override public void onStatusRequest() { showTrackingHelp(); }
        });
        root.addView(hud, new FrameLayout.LayoutParams(-1,-1));
        setContentView(root);

        if (!Sceneform.isSupported(this)) {
            hud.setStatus("ARCore не поддерживается устройством");
            showArUnavailable();
            return;
        }

        arFragment = new ArFragment();
        getSupportFragmentManager().beginTransaction()
                .add(arHost.getId(), arFragment, "blockworld_ar")
                .commit();
        getSupportFragmentManager().executePendingTransactions();

        arFragment.setOnSessionConfigurationListener(this);
        arFragment.setOnViewCreatedListener(this);
        arFragment.setOnTapArPlaneListener(this);
        buildRenderables();
        handler.post(gameLoop);
    }

    private void showArUnavailable() {
        if (warnedNoAr) return;
        warnedNoAr = true;
        new AlertDialog.Builder(this)
            .setTitle("Нужна поддержка AR")
            .setMessage("Для закрепления блоков в реальном помещении требуется ARCore и поддерживаемые датчики движения. На этом телефоне ARCore может быть недоступен. Камера без AR не сможет удерживать объекты на месте.")
            .setPositiveButton("Понятно", null)
            .show();
    }

    private void buildRenderables() {
        for (int i=0;i<BLOCK_COLORS.length;i++) {
            final int index=i;
            MaterialFactory.makeOpaqueWithColor(this, new com.google.ar.sceneform.rendering.Color(BLOCK_COLORS[i]))
                .thenAccept(material -> blockModels[index] =
                    ShapeFactory.makeCube(new Vector3(1f,1f,1f), Vector3.zero(), material))
                .exceptionally(error -> { runOnUiThread(() -> hud.setStatus("Ошибка материалов")); return null; });
        }
        for (int i=0;i<MOB_COLORS.length;i++) {
            final int index=i;
            MaterialFactory.makeOpaqueWithColor(this, new com.google.ar.sceneform.rendering.Color(MOB_COLORS[i]))
                .thenAccept(material -> mobCubes[index] =
                    ShapeFactory.makeCube(new Vector3(1f,1f,1f), Vector3.zero(), material));
        }
        MaterialFactory.makeOpaqueWithColor(this, new com.google.ar.sceneform.rendering.Color(0xff24242a))
            .thenAccept(material -> darkCube=ShapeFactory.makeCube(new Vector3(1f,1f,1f),Vector3.zero(),material));
        MaterialFactory.makeOpaqueWithColor(this, new com.google.ar.sceneform.rendering.Color(0xfff5f0d8))
            .thenAccept(material -> whiteCube=ShapeFactory.makeCube(new Vector3(1f,1f,1f),Vector3.zero(),material));
        MaterialFactory.makeOpaqueWithColor(this, new com.google.ar.sceneform.rendering.Color(0xff181818))
            .thenAccept(material -> eyeCube=ShapeFactory.makeCube(new Vector3(1f,1f,1f),Vector3.zero(),material));

    }

    @Override public void onSessionConfiguration(Session session, Config config) {
        config.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL);
        if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
            config.setDepthMode(Config.DepthMode.AUTOMATIC);
        }
        config.setLightEstimationMode(Config.LightEstimationMode.AMBIENT_INTENSITY);
    }

    @Override public void onViewCreated(ArSceneView view) {
        arReady = true;
        view.setFrameRateFactor(SceneView.FrameRate.FULL);
        try { view.getPlaneRenderer().setVisible(false); } catch (Exception ignored) {}
        hud.setStatus("Наведи камеру на пол или стену");
        hud.invalidate();
    }

    @Override public void onTapPlane(HitResult hit, Plane plane, MotionEvent event) {
        if (!arReady || hud == null) return;
        if (!renderablesReady) {
            renderablesReady = allModelsReady();
            if (!renderablesReady) {
                Toast.makeText(this, "3D-модели ещё загружаются", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        int mode = hud.getMode();
        if (mode == 0) placeBlock(hit);
        else if (mode == 1) {
            if (plane.getType() != Plane.Type.HORIZONTAL_UPWARD_FACING) {
                Toast.makeText(this, "Мобов лучше ставить на пол", Toast.LENGTH_SHORT).show();
                return;
            }
            spawnMob(hit);
        } else placeFriendTag(hit);
    }

    private boolean allModelsReady() {
        for (ModelRenderable r:blockModels) if (r==null) return false;
        for (ModelRenderable r:mobCubes) if (r==null) return false;
        return darkCube!=null && eyeCube!=null && whiteCube!=null;
    }

    private void placeBlock(HitResult hit) {
        int type = hud.getSelectedBlock();
        ModelRenderable renderable = blockModels[type];
        if (renderable==null) return;
        Anchor anchor = hit.createAnchor();
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(arFragment.getArSceneView().getScene());
        Node cube = new Node();
        cube.setParent(anchorNode);
        cube.setRenderable(renderable);
        cube.setLocalScale(new Vector3(0.23f,0.23f,0.23f));
        cube.setLocalPosition(new Vector3(0f,0.115f,0f));
        PlacedBlock block = new PlacedBlock(type,anchor,anchorNode,cube);
        placedBlocks.add(block);
        cube.setOnTapListener((result,motion) -> breakBlock(block));
        hud.setStatus("Блок закреплён в реальном пространстве");
        hud.invalidate();
    }

    private void breakBlock(PlacedBlock block) {
        if (!placedBlocks.remove(block)) return;
        block.anchorNode.setParent(null);
        block.anchor.detach();
        hud.addXp(1);
        hud.setStatus("Блок разрушен");
        hud.invalidate();
    }

    private void spawnMob(HitResult hit) {
        int type = hud.getSelectedMob();
        Anchor anchor = hit.createAnchor();
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(arFragment.getArSceneView().getScene());
        Node mobRoot = new Node();
        mobRoot.setParent(anchorNode);
        Creature creature = new Creature(type,anchor,anchorNode,mobRoot);
        creatures.add(creature);
        buildMobShape(creature);
        hud.setStatus(MOBS[type]+" появился и идёт к тебе");
        hud.invalidate();
    }

    private void buildMobShape(Creature c) {
        int type=c.type;
        float height = type==3 ? 2.45f : (type==4 ? 0.45f : (type==5 ? 0.55f : (type==9 ? 0.55f : 0.95f)));
        float width = type==4 ? 0.95f : (type==3 ? 0.42f : (type==5 ? 0.55f : (type==9 ? 0.42f : 0.52f)));
        float depth = type==4 ? 0.72f : (type==5 ? 0.50f : 0.38f);
        ModelRenderable body = mobCubes[type];
        ModelRenderable light = type==1 || type==8 || type==9 ? whiteCube : body;

        if (type==4) {
            addPart(c,body,0f,0.15f,0f,0.48f,0.22f,0.38f);
            for(int i=0;i<4;i++) {
                float x=-0.36f+i*0.24f;
                addPart(c,body,x,0.07f,-0.23f,0.34f,0.07f,0.07f);
                addPart(c,body,x,0.07f,0.23f,0.34f,0.07f,0.07f);
            }
            addPart(c,eyeCube,-0.12f,0.20f,-0.20f,0.07f,0.07f,0.035f);
            addPart(c,eyeCube,0.12f,0.20f,-0.20f,0.07f,0.07f,0.035f);
        } else if (type==5) {
            addPart(c,body,0f,0.26f,0f,width,0.52f,depth);
            addPart(c,eyeCube,-0.13f,0.36f,-0.21f,0.07f,0.07f,0.03f);
            addPart(c,eyeCube,0.13f,0.36f,-0.21f,0.07f,0.07f,0.03f);
        } else if (type==2) {
            addPart(c,body,0f,0.53f,0f,0.38f,0.72f,0.34f);
            addPart(c,body,-0.12f,0.08f,0f,0.14f,0.18f,0.14f);
            addPart(c,body,0.12f,0.08f,0f,0.14f,0.18f,0.14f);
            addPart(c,body,-0.19f,0.42f,0f,0.12f,0.52f,0.12f);
            addPart(c,body,0.19f,0.42f,0f,0.12f,0.52f,0.12f);
            addEyes(c);
        } else {
            addPart(c,body,0f,height*0.52f,0f,width*0.66f,height*0.45f,depth);
            addPart(c,light,0f,height*0.84f,0f,width*0.56f,width*0.56f,width*0.55f);
            addPart(c,body,-width*0.42f,height*0.50f,0f,width*0.22f,height*0.44f,width*0.22f);
            addPart(c,body,width*0.42f,height*0.50f,0f,width*0.22f,height*0.44f,width*0.22f);
            addPart(c,body,-width*0.20f,height*0.17f,0f,width*0.22f,height*0.34f,width*0.22f);
            addPart(c,body,width*0.20f,height*0.17f,0f,width*0.22f,height*0.34f,width*0.22f);
            if(type==7 || type==8) {
                addPart(c,whiteCube,0f,height*0.56f,-depth*0.52f,width*0.18f,height*0.28f,0.06f);
            }
            if(type==9) {
                addPart(c,whiteCube,0f,height*0.34f,-depth*0.58f,0.10f,0.08f,0.08f);
                addPart(c,mobCubes[6],0f,height*1.10f,0f,0.08f,0.14f,0.08f);
            }
            if(type==6 || type==7 || type==8) {
                addPart(c,body,0f,height*0.79f,-depth*0.48f,width*0.26f,width*0.22f,0.09f);
            }
            addEyes(c);
        }
        c.root.setOnTapListener((result,motion) -> damageMob(c));
    }

    private void addEyes(Creature c) {
        float y = c.type==3 ? 1.98f : (c.type==9 ? 0.48f : 0.77f);
        addPart(c,eyeCube,-0.10f,y,-0.205f,0.055f,0.055f,0.035f);
        addPart(c,eyeCube,0.10f,y,-0.205f,0.055f,0.055f,0.035f);
    }

    private void addPart(Creature c, ModelRenderable model, float x,float y,float z,
                         float sx,float sy,float sz) {
        if (model==null) return;
        Node part = new Node();
        part.setParent(c.root);
        part.setRenderable(model);
        part.setLocalPosition(new Vector3(x,y,z));
        part.setLocalScale(new Vector3(sx,sy,sz));
        part.setOnTapListener((result,motion)->damageMob(c));
    }

    private void damageMob(Creature c) {
        if (!creatures.contains(c)) return;
        c.hp--;
        if (c.hp<=0) {
            creatures.remove(c);
            c.anchorNode.setParent(null);
            c.anchor.detach();
            hud.addXp(3);
            hud.setStatus(MOBS[c.type]+" побеждён");
        } else {
            hud.setStatus(MOBS[c.type]+"  ♥ "+c.hp+"/5");
        }
        hud.invalidate();
    }

    private void updateMobs() {
        if (!arReady || arFragment==null || arFragment.getArSceneView()==null || creatures.isEmpty()) return;
        Vector3 camera = arFragment.getArSceneView().getScene().getCamera().getWorldPosition();
        long now=System.currentTimeMillis();
        float dt=lastTick==0?0.05f:Math.min(0.12f,(now-lastTick)/1000f);
        lastTick=now;
        Iterator<Creature> iterator=creatures.iterator();
        while(iterator.hasNext()) {
            Creature m=iterator.next();
            Vector3 pos=m.root.getWorldPosition();
            float dx=camera.x-pos.x, dz=camera.z-pos.z;
            float dist=(float)Math.sqrt(dx*dx+dz*dz);
            if(dist>0.01f) {
                Vector3 direction=new Vector3(dx/dist,0f,dz/dist);
                m.root.setLookDirection(direction, new Vector3(0f,1f,0f));
                if(dist>0.85f) {
                    float speed=m.type==3?0.30f:(m.type==4?0.18f:0.23f);
                    m.root.setWorldPosition(new Vector3(pos.x+direction.x*speed*dt,pos.y,pos.z+direction.z*speed*dt));
                } else if(now-m.lastAttack>2200) {
                    m.lastAttack=now;
                    hud.damagePlayer(1);
                    hud.setStatus(MOBS[m.type]+" рядом — минус сердце");
                }
            }
        }
    }

    private void placeFriendTag(HitResult hit) {
        TextView tag = new TextView(this);
        tag.setText(friendName);
        tag.setTextColor(Color.WHITE);
        tag.setTextSize(14);
        tag.setTypeface(Typeface.DEFAULT_BOLD);
        tag.setGravity(Gravity.CENTER);
        tag.setPadding(18,8,18,8);
        tag.setBackgroundColor(0xdd17231b);
        ViewRenderable.builder().setView(this, tag).build().thenAccept(renderable -> {
            if (isFinishing() || arFragment==null || arFragment.getArSceneView()==null) return;
            Anchor anchor=hit.createAnchor();
            AnchorNode anchorNode=new AnchorNode(anchor);
            anchorNode.setParent(arFragment.getArSceneView().getScene());
            Node label=new Node();
            label.setParent(anchorNode);
            label.setRenderable(renderable);
            label.setLocalPosition(new Vector3(0f,0.42f,0f));
            label.setLocalScale(new Vector3(0.65f,0.65f,0.65f));
            friendAnchors.add(anchorNode);
            hud.setStatus("Метка «"+friendName+"» закреплена в пространстве");
        }).exceptionally(error -> {
            runOnUiThread(() -> Toast.makeText(this,"Не удалось создать метку",Toast.LENGTH_SHORT).show());
            return null;
        });
    }

    private void editFriendName() {
        EditText input=new EditText(this);
        input.setSingleLine(true);
        input.setText(friendName);
        input.setHint("Имя друга");
        new AlertDialog.Builder(this)
            .setTitle("Метка друга")
            .setMessage("Сохрани имя, затем коснись пола или стены, чтобы поставить 3D-метку.")
            .setView(input)
            .setNegativeButton("Отмена",null)
            .setPositiveButton("Сохранить",(dialog,which)->{
                friendName=input.getText().toString().trim();
                if(friendName.isEmpty()) friendName="Друг";
                hud.setFriendName(friendName);
                Toast.makeText(this,"Теперь коснись места для метки",Toast.LENGTH_SHORT).show();
            }).show();
    }

    private void clearFriends() {
        for(AnchorNode node:friendAnchors) {
            Anchor a=node.getAnchor();
            node.setParent(null);
            if(a!=null) a.detach();
        }
        friendAnchors.clear();
        hud.setStatus("3D-метки друзей очищены");
    }

    private void showTrackingHelp() {
        String msg="Плавно поворачивай телефон и медленно осматривай пол и стены. Когда ARCore распознает поверхность, коснись места для установки. Блоки и мобы остаются закреплёнными, пока работает AR-сеанс.";
        new AlertDialog.Builder(this).setTitle("Как закреплять объекты").setMessage(msg).setPositiveButton("Понятно",null).show();
    }

    @Override protected void onResume() {
        super.onResume();
        handler.removeCallbacks(gameLoop);
        handler.post(gameLoop);
    }
    @Override protected void onPause() {
        handler.removeCallbacks(gameLoop);
        super.onPause();
    }
    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        for(PlacedBlock b:placedBlocks) if(b.anchor!=null) b.anchor.detach();
        for(Creature m:creatures) if(m.anchor!=null) m.anchor.detach();
        for(AnchorNode n:friendAnchors) if(n.getAnchor()!=null) n.getAnchor().detach();
        super.onDestroy();
    }

    private static class PlacedBlock {
        final int type; final Anchor anchor; final AnchorNode anchorNode; final Node node;
        PlacedBlock(int t,Anchor a,AnchorNode an,Node n){type=t;anchor=a;anchorNode=an;node=n;}
    }
    private static class Creature {
        final int type; final Anchor anchor; final AnchorNode anchorNode; final Node root;
        int hp=5; long lastAttack=0;
        Creature(int t,Anchor a,AnchorNode an,Node r){type=t;anchor=a;anchorNode=an;root=r;}
    }

    static class HudView extends View {
        interface Callbacks { void onFriendName(); void onClearFriends(); void onStatusRequest(); }
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float d;
        private Callbacks callbacks;
        private int mode=0, selectedBlock=0, selectedMob=0, scroll=0, selectedSlot=0;
        private final int[] blockSlots={0,1,2,3,4,5,6,7,8};
        private final int[] mobSlots={0,1,2,3,4,5,6,7,8};
        private int health=10, hunger=10, xp=0;
        private String status="Запуск AR…", friendName="Друг";
        private float downX,downY;
        HudView(android.content.Context c){super(c);d=getResources().getDisplayMetrics().density;p.setTypeface(Typeface.DEFAULT_BOLD);setLayerType(View.LAYER_TYPE_HARDWARE,null);}
        void setCallbacks(Callbacks c){callbacks=c;}
        int getMode(){return mode;}
        int getSelectedBlock(){return selectedBlock;}
        int getSelectedMob(){return selectedMob;}
        void setStatus(String s){status=s;postInvalidate();}
        void setFriendName(String n){friendName=n;invalidate();}
        void addXp(int x){xp+=x;invalidate();}
        void damagePlayer(int amount){health=Math.max(0,health-amount);invalidate();}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);float w=getWidth(),h=getHeight();
            p.setColor(0xdd101913);c.drawRoundRect(10*d,8*d,w-10*d,70*d,14*d,14*d,p);
            text(c,"BLOCKWORLD AR  •  V2",22*d,25*d,10*d,0xffd3ead6,false);
            text(c,status,22*d,43*d,8.2f*d,0xffe7eee8,false);
            text(c,"XP "+xp,w-18*d,25*d,9*d,0xffffd66c,true);
            p.setTextAlign(Paint.Align.LEFT);p.setTextSize(17*d);p.setColor(0xffff5555);
            for(int i=0;i<10;i++)c.drawText(i<health?"♥":"♡",22*d+i*21*d,64*d,p);
            p.setTextSize(13*d);p.setColor(0xffffc76b);
            for(int i=0;i<10;i++)c.drawText(i<hunger?"◆":"◇",w-130*d+i*11.5f*d,62*d,p);

            float top=h-204*d;
            p.setColor(0xee111a14);c.drawRoundRect(7*d,top,w-7*d,h-7*d,15*d,15*d,p);
            String[] tabs={"БЛОКИ","МОБЫ","ДРУЗЬЯ"};
            float tw=(w-26*d)/3;
            for(int i=0;i<3;i++){
                float x=13*d+i*tw;p.setColor(mode==i?0xff397b43:0xff29372e);
                c.drawRoundRect(x,top+7*d,x+tw-4*d,top+39*d,8*d,8*d,p);
                text(c,tabs[i],x+(tw-4*d)/2,top+28*d,9*d,Color.WHITE,true);
            }
            float row=top+46*d;
            if(mode==2){
                p.setColor(0xff35483a);c.drawRoundRect(15*d,row,w-15*d,row+43*d,8*d,8*d,p);
                text(c,"＋ ИМЯ: "+friendName,w*.34f,row+27*d,10*d,Color.WHITE,true);
                text(c,"ОЧИСТИТЬ",w*.79f,row+27*d,9*d,0xffffc8a8,true);
                text(c,"Затем коснись реального места в кадре",w*.5f,row+61*d,8*d,0xffd6e0d8,true);
            }else{
                int count=Math.min(6,Math.max(1,(int)((w-28*d)/(53*d))));
                float cw=(w-26*d)/count;String[] items=mode==0?BLOCKS:MOBS;
                for(int i=0;i<count;i++){
                    int idx=scroll+i;if(idx>=items.length)break;
                    float x=13*d+i*cw;p.setColor((mode==0?idx==selectedBlock:idx==selectedMob)?0xff557d4e:0xff27352d);
                    c.drawRoundRect(x,row,x+cw-3*d,row+51*d,6*d,6*d,p);
                    if(mode==0)cubeIcon(c,x+cw/2,row+19*d,17*d,BLOCK_COLORS[idx]);
                    else mobIcon(c,x+cw/2,row+23*d,0.27f*d,MOB_COLORS[idx]);
                    text(c,items[idx].length()>8?items[idx].substring(0,7)+"…":items[idx],x+cw/2,row+44*d,7*d,0xfff3f5f1,true);
                }
                text(c,"‹",9*d,row+32*d,18*d,Color.WHITE,true);
                text(c,"›",w-9*d,row+32*d,18*d,Color.WHITE,true);
            }
            float hot=top+106*d,cw=(w-26*d)/9;
            for(int i=0;i<9;i++){
                float x=13*d+i*cw;p.setColor(i==selectedSlot?0xffcbb65e:0xff3a463e);
                c.drawRoundRect(x,hot,x+cw-2*d,hot+53*d,5*d,5*d,p);
                if(mode==0)cubeIcon(c,x+cw/2,hot+24*d,17*d,BLOCK_COLORS[blockSlots[i]]);
                else if(mode==1)mobIcon(c,x+cw/2,hot+25*d,0.23f*d,MOB_COLORS[mobSlots[i]]);
                else text(c,"★",x+cw/2,hot+32*d,15*d,0xffffd76a,true);
                text(c,""+(i+1),x+cw/2,hot+49*d,6.5f*d,0xffe7eee8,true);
            }
            if(health<=0)label(c,"ИГРОК ВЫБЫЛ",w/2,h*.42f,14*d,Color.WHITE,0xdd8e2727);
        }
        private void cubeIcon(Canvas c,float x,float y,float size,int color){
            float a=size*.42f;p.setColor(color);c.drawRect(x-a,y-a,x+a,y+a,p);
            p.setColor(0x55333333);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.2f*d);c.drawRect(x-a,y-a,x+a,y+a,p);p.setStyle(Paint.Style.FILL);
            p.setColor(0x55ffffff);c.drawLine(x-a,y-a,x+a,y-a,p);c.drawLine(x-a,y-a,x-a,y+a,p);
        }
        private void mobIcon(Canvas c,float x,float y,float s,int color){
            p.setColor(color);c.drawRect(x-7*s,y-7*s,x+7*s,y+6*s,p);c.drawRect(x-5*s,y-14*s,x+5*s,y-5*s,p);
            p.setColor(0xff202020);c.drawRect(x-3*s,y-11*s,x-1*s,y-9*s,p);c.drawRect(x+1*s,y-11*s,x+3*s,y-9*s,p);
            p.setColor(color);c.drawRect(x-5*s,y+6*s,x-2*s,y+11*s,p);c.drawRect(x+2*s,y+6*s,x+5*s,y+11*s,p);
        }
        private void text(Canvas c,String s,float x,float y,float size,int color,boolean center){p.setTextSize(size);p.setColor(color);p.setTextAlign(center?Paint.Align.CENTER:Paint.Align.LEFT);c.drawText(s,x,y,p);}
        private void label(Canvas c,String s,float x,float y,float size,int color,int bg){p.setTextSize(size);p.setTextAlign(Paint.Align.CENTER);float ww=p.measureText(s)+22*d;p.setColor(bg);c.drawRoundRect(x-ww/2,y-18*d,x+ww/2,y+6*d,8*d,8*d,p);p.setColor(color);c.drawText(s,x,y,p);}
        @Override public boolean onTouchEvent(MotionEvent e){
            float x=e.getX(),y=e.getY(),h=getHeight(),w=getWidth(),top=h-204*d;
            if(e.getAction()==MotionEvent.ACTION_DOWN){if(y<top)return false;downX=x;downY=y;return true;}
            if(e.getAction()!=MotionEvent.ACTION_UP)return y>=top;
            if(y<top)return false;
            if(y<top+43*d){mode=Math.max(0,Math.min(2,(int)((x-13*d)/((w-26*d)/3))));scroll=0;invalidate();return true;}
            if(mode==2&&y<top+100*d){
                if(x<w*.56f&&callbacks!=null)callbacks.onFriendName();
                else if(callbacks!=null)callbacks.onClearFriends();
                return true;
            }
            if(mode!=2&&y<top+103*d){
                int count=Math.min(6,Math.max(1,(int)((w-28*d)/(53*d))));
                float cw=(w-26*d)/count;
                String[] items=mode==0?BLOCKS:MOBS;
                if(x<17*d)scroll=Math.max(0,scroll-1);
                else if(x>w-17*d)scroll=Math.min(items.length-count,scroll+1);
                else{
                    int idx=scroll+(int)((x-13*d)/cw);
                    if(idx>=0&&idx<items.length){
                        if(mode==0){selectedBlock=idx;blockSlots[0]=idx;}
                        else{selectedMob=idx;mobSlots[0]=idx;}
                        selectedSlot=0;
                    }
                }
                invalidate();return true;
            }
            if(y>=top+105*d){
                int slot=Math.max(0,Math.min(8,(int)((x-13*d)/((w-26*d)/9))));
                selectedSlot=slot;if(mode==0)selectedBlock=blockSlots[slot];if(mode==1)selectedMob=mobSlots[slot];
                invalidate();return true;
            }
            return true;
        }
    }
}