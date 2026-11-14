package app.notifydash;

import android.app.*;
import android.content.*;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.RemoteViews;

public class GameReceiver extends BroadcastReceiver {
    static final String CHANNEL="arcade_notification_games";
    static final int NOTIFICATION_ID=7138;
    static final String ACTION_LEFT="app.notifydash.LEFT";
    static final String ACTION_ACT="app.notifydash.ACT";
    static final String ACTION_RIGHT="app.notifydash.RIGHT";
    static final String ACTION_RESET="app.notifydash.RESET";
    static final String ACTION_END="app.notifydash.END";
    static final String PREF="arcade_notification_state";

    static final String[] NAMES={
        "GEOMETRY DASH","FLAPPY HOP","SNAKE","MINI PONG","SPACE SHOOTER","BREAKOUT",
        "DOODLE JUMP","MOLE TAP","2048","MEMORY","RHYTHM BEAT","MAZE RUN",
        "COIN RUN","BRIDGE BUILDER","SUBMARINE","ROCKET LANDER",
        "COLOR SWITCH","FISHING","MINI TETRIS","ORBIT DASH"
    };

    // Each challenge has exactly one correct command: LEFT, ACTION or RIGHT.
    // No button is a free point: a wrong command costs a heart.
    static final String[][] CHALLENGES={
        {"ШИП ▲ впереди! Нажми ПРЫЖОК.","Низкий потолок ▰ — нажми ПРИСЕСТЬ.","ЯМА ▱ — перепрыгни!"},
        {"Труба сверху ▥ — опустись влево.","Нижняя труба ▥ — взмахни вверх!","Узкий проход справа — смести птицу вправо."},
        {"Поворот налево!","Еда впереди — шагни прямо.","Поворот направо!"},
        {"Мяч летит влево — двигай ракетку влево.","Мяч по центру — удар!","Мяч уходит вправо — двигай ракетку вправо."},
        {"Астероид слева ☄ — уклонись влево.","Враг по центру — ОГОНЬ!","Астероид справа ☄ — уклонись вправо."},
        {"Шар у левого края — ракетку влево.","Шар над центром — отбей!","Шар у правого края — ракетку вправо."},
        {"Платформа слева ▱ — смести героя влево.","Прыжок на платформу!","Платформа справа ▱ — смести героя вправо."},
        {"Крот в левой норе — бей влево.","Крот выскочил в центре — УДАР!","Крот в правой норе — бей вправо."},
        {"Плитка слева: сдвинь влево.","Слияние в центре — ДЕЙСТВИЕ!","Плитка справа: сдвинь вправо."},
        {"Открой карту слева.","Открой центральную карту.","Открой карту справа."},
        {"Нота слева — нажми в ритм.","Ударный бит — ДЕЙСТВИЕ!","Нота справа — нажми в ритм."},
        {"Коридор поворачивает налево.","Прямой проход — шаг вперёд.","Коридор поворачивает направо."},
        {"Препятствие слева — смени дорожку влево.","Барьер впереди — ПРЫЖОК!","Препятствие справа — смени дорожку вправо."},
        {"Опора слева — баланс влево.","Положи доску на опору!","Опора справа — баланс вправо."},
        {"Мина слева 💣 — уйди влево.","Сканируй свободный проход!","Мина справа 💣 — уйди вправо."},
        {"Ракету кренит влево — выровняй влево.","Включи тягу!","Ракету кренит вправо — выровняй вправо."},
        {"Кольцо требует красный — выбери слева.","Цвет совпал — пройди прямо!","Кольцо требует синий — выбери справа."},
        {"Поплавок уходит влево — подстрой леску.","Поклёвка! Подсекай!","Поплавок уходит вправо — подстрой леску."},
        {"Фигура смещается влево.","Поверни фигуру!","Фигура смещается вправо."},
        {"Астероид слева — манёвр влево.","Включи щит в центре!","Астероид справа — манёвр вправо."}
    };

    static void ensureChannel(Context c){
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel ch=new NotificationChannel(CHANNEL,"Arcade games in notifications",NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("Интерактивные мини-игры с препятствиями и управлением в уведомлении");
            ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
    }

    static void start(Context c,int game){
        ensureChannel(c);
        int g=Math.max(0,Math.min(19,game));
        c.getSharedPreferences(PREF,0).edit()
            .putBoolean("active",true).putInt("game",g).putInt("score",0)
            .putInt("lives",3).putInt("step",0).putInt("lane",1).putInt("combo",0)
            .putLong("shownAt",System.currentTimeMillis())
            .putString("message","Раунд начался! Реагируй быстро: правильная команда даёт очки, ошибка отнимает ♥.")
            .apply();
        show(c);
    }

    @Override public void onReceive(Context c,Intent i){
        String a=i.getAction();
        SharedPreferences p=c.getSharedPreferences(PREF,0);
        if(ACTION_END.equals(a)){
            p.edit().putBoolean("active",false).apply();
            ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(NOTIFICATION_ID);
            return;
        }
        if(ACTION_RESET.equals(a)){start(c,p.getInt("game",0));return;}
        if(!p.getBoolean("active",false))return;

        int game=p.getInt("game",0),step=p.getInt("step",0),score=p.getInt("score",0);
        int lives=p.getInt("lives",3),lane=p.getInt("lane",1),combo=p.getInt("combo",0);
        int command=ACTION_LEFT.equals(a)?0:ACTION_RIGHT.equals(a)?2:1;
        int expected=expectedCommand(game,step);
        long now=System.currentTimeMillis();
        long reaction=Math.max(0,now-p.getLong("shownAt",now));
        boolean correct=command==expected;

        if(correct){
            combo++;
            int speedBonus=reaction<850?2:reaction<1600?1:0;
            score+=1+speedBonus+(combo%5==0?2:0);
            p.edit().putString("message",reaction<850?"Молниеносно! +"+(1+speedBonus)+" очка · серия ×"+combo:
                "Верно! +"+(1+speedBonus)+" очка · серия ×"+combo).apply();
            lane=command==0?Math.max(0,lane-1):command==2?Math.min(2,lane+1):lane;
        }else{
            lives--;
            combo=0;
            p.edit().putString("message","ОШИБКА! Нужно: "+commandName(expected)+". Потеряно ♥.").apply();
        }

        step++;
        boolean active=lives>0;
        p.edit().putInt("score",score).putInt("lives",lives).putInt("step",step)
            .putInt("lane",lane).putInt("combo",combo).putBoolean("active",active)
            .putLong("shownAt",now).apply();
        if(!active)p.edit().putString("message","ИГРА ОКОНЧЕНА. Очки: "+score+". Нажми ↻, чтобы начать заново.").apply();
        else if(step%10==0)p.edit().putString("message","Этап "+(step/10+1)+"! Скорость растёт — серия ×"+combo+".").apply();
        show(c);
    }

    private static int expectedCommand(int game,int step){
        // A deterministic but varied obstacle sequence. 0=left, 1=action, 2=right.
        int[][] sequences={
            {1,0,1,2,1,0,1,2}, {0,1,2,1,0,1,2,1}, {0,1,2,1,2,0,1,2},
            {0,1,2,0,1,2,1,0}, {0,1,2,1,2,0,1,2}, {0,1,2,1,0,2,1,2},
            {0,1,2,1,2,0,1,2}, {0,1,2,1,0,2,1,2}, {0,2,1,0,1,2,0,1},
            {0,1,2,1,2,0,1,2}, {0,1,2,1,0,2,1,2}, {0,1,2,0,1,2,1,0},
            {0,1,2,1,2,0,1,2}, {0,1,2,1,0,2,1,2}, {0,1,2,1,2,0,1,2},
            {0,1,2,1,0,2,1,2}, {0,1,2,1,2,0,1,2}, {0,1,2,1,0,2,1,2},
            {0,1,2,1,2,0,1,2}, {0,1,2,1,0,2,1,2}
        };
        int[] s=sequences[game];
        return s[Math.floorMod(step+s.length*(game%3),s.length)];
    }

    private static String commandName(int cmd){
        return cmd==0?"◀ ЛЕВО":cmd==2?"ПРАВО ▶":"● ДЕЙСТВИЕ";
    }

    private static String board(int game,int step,int lane){
        int expected=expectedCommand(game,step);
        String left=expected==0?"✦":"·",center=expected==1?"✦":"·",right=expected==2?"✦":"·";
        switch(game){
            case 0:return "ПОЛОСА:  "+left+"  "+center+"  "+right+"\n"+(expected==1?"       ▲ ШИП — ПРЫГАЙ":"       ▰ / ▱ — МАНЁВР");
            case 1:return "ТРУБЫ:   "+left+"  "+center+"  "+right+"\n       🐤  ПРОЛЕТИ В ПРОХОД";
            case 2:return "ЗМЕЙКА:  "+left+"  ●  "+right+"\n       ПОВОРОТ / ЕДА";
            case 3:return "МЯЧ:     "+left+"  ●  "+right+"\n       РАКЕТКА ДОЛЖНА ПОПАСТЬ";
            case 4:return "КОСМОС:  "+left+"  ☄  "+right+"\n       🚀 УКЛОНЯЙСЯ / СТРЕЛЯЙ";
            case 5:return "БЛОКИ:   "+left+"  ▣  "+right+"\n       ОТБЕЙ МЯЧ РАКЕТКОЙ";
            case 6:return "ПЛАТФОРМЫ: "+left+"  ▱  "+right+"\n       🐸 ВЫБЕРИ ПЛАТФОРМУ";
            case 7:return "НОРЫ:    "+left+"  "+center+"  "+right+"\n       🐹 ЦЕЛЬ ВЫСКОЧИЛА!";
            case 8:return "ПЛИТКИ:  "+left+"  [2]  "+right+"\n       СДВИНЬ НУЖНУЮ ПЛИТКУ";
            case 9:return "КАРТЫ:   "+left+"  ▣  "+right+"\n       НАЙДИ ПАРУ";
            case 10:return "РИТМ:    "+left+"  ♪  "+right+"\n       ПОПАДИ В БИТ";
            case 11:return "ПУТЬ:    "+left+"  ┃  "+right+"\n       ВЫБЕРИ КОРИДОР";
            case 12:return "ЗАБЕГ:   "+left+"  ▲  "+right+"\n       ПРЕПЯТСТВИЕ ВПЕРЕДИ";
            case 13:return "МОСТ:    "+left+"  ═  "+right+"\n       ПОЙМАЙ ТОЧКУ ОПОРЫ";
            case 14:return "МОРЕ:    "+left+"  💣  "+right+"\n       🚤 ОБОЙДИ МИНУ";
            case 15:return "РАКЕТА:  "+left+"  🚀  "+right+"\n       ВЫРОВНЯЙ / ТЯГА";
            case 16:return "ЦВЕТ:    "+left+"  🟢  "+right+"\n       ПРОЙДИ КОЛЬЦО";
            case 17:return "РЫБАЛКА: "+left+"  🎣  "+right+"\n       ПОДСЕКАЙ ВОВРЕМЯ";
            case 18:return "ТЕТРИС:  "+left+"  ▣  "+right+"\n       ПОВОРОТ / СДВИГ";
            default:return "ОРБИТА:  "+left+"  ☄  "+right+"\n       УКЛОНЯЙСЯ ОТ АСТЕРОИДА";
        }
    }

    static PendingIntent action(Context c,String act,int code){
        Intent i=new Intent(c,GameReceiver.class);i.setAction(act);
        int flags=PendingIntent.FLAG_UPDATE_CURRENT;
        if(Build.VERSION.SDK_INT>=23)flags|=PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(c,code,i,flags);
    }

    static void show(Context c){
        ensureChannel(c);
        SharedPreferences p=c.getSharedPreferences(PREF,0);
        int game=p.getInt("game",0),score=p.getInt("score",0),lives=p.getInt("lives",3);
        int step=p.getInt("step",0),lane=p.getInt("lane",1),combo=p.getInt("combo",0);
        boolean active=p.getBoolean("active",false);
        int expected=expectedCommand(game,step);
        RemoteViews view=new RemoteViews(c.getPackageName(),R.layout.game_notification);
        view.setTextViewText(R.id.game_title,"ARCADE  •  "+NAMES[game]+(active?"":"  •  GAME OVER"));
        view.setTextViewText(R.id.game_stats,"Очки "+score+"   ♥ "+lives+"   Серия ×"+combo);
        view.setTextViewText(R.id.game_board,board(game,step,lane));
        view.setTextViewText(R.id.game_message,active?CHALLENGES[game][step%3]+"  ⏱ Быстрее = больше очков.":p.getString("message","Раунд завершён."));
        view.setTextViewText(R.id.btn_left,game==0||game==6||game==12?"↓ ПРИСЕСТЬ":"◀ ЛЕВО");
        view.setTextViewText(R.id.btn_action,game==0||game==6||game==12?"↑ ПРЫЖОК":game==4?"✦ ОГОНЬ":"● ДЕЙСТВИЕ");
        view.setTextViewText(R.id.btn_right,"ПРАВО ▶");
        view.setTextViewText(R.id.btn_reset,active?"■":"↻");
        view.setOnClickPendingIntent(R.id.btn_left,action(c,ACTION_LEFT,21));
        view.setOnClickPendingIntent(R.id.btn_action,action(c,ACTION_ACT,22));
        view.setOnClickPendingIntent(R.id.btn_right,action(c,ACTION_RIGHT,23));
        view.setOnClickPendingIntent(R.id.btn_reset,action(c,active?ACTION_END:ACTION_RESET,24));
        Intent open=new Intent(c,MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content=PendingIntent.getActivity(c,10,open,PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0));
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,CHANNEL):new Notification.Builder(c);
        Notification n=b.setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Arcade Notifications — "+NAMES[game])
            .setContentText(active?CHALLENGES[game][step%3]:"Раунд завершён · "+score+" очков")
            .setContentIntent(content).setCustomContentView(view).setCustomBigContentView(view)
            .setStyle(new Notification.DecoratedCustomViewStyle()).setOnlyAlertOnce(true)
            .setOngoing(active).build();
        ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID,n);
    }
}