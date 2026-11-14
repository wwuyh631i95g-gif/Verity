package app.notifydash;

import android.app.*;
import android.content.*;
import android.os.Build;
import android.widget.RemoteViews;
import java.util.Locale;

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
    static final String[] PROMPTS={
        "Шип впереди — нажми ДЕЙСТВИЕ, чтобы прыгнуть.",
        "Тапни ДЕЙСТВИЕ, чтобы взмахнуть крыльями.",
        "Влево/вправо — повернуть; действие — шаг вперёд.",
        "Двигай ракетку влево или вправо, действие — удар.",
        "Уклоняйся по сторонам и нажимай действие, чтобы стрелять.",
        "Удерживай мяч на поле: стороны двигают ракетку.",
        "Выбирай платформу сбоку и нажимай действие для прыжка.",
        "Цель мигает в одной из трёх нор — выбери сторону и ударь.",
        "Двигай плитки влево/вправо и нажимай действие для слияния.",
        "Выбери карту влево/вправо и открой её действием.",
        "Жми действие в такт — поймай музыкальный бит.",
        "Сверяйся с коридором и двигайся по клеткам.",
        "Меняй дорожку и перепрыгивай преграду действием.",
        "Балансируй влево/вправо и клади доску действием.",
        "Меняй глубину подлодки и сканируй путь.",
        "Выравнивай ракету боковыми кнопками, действие — тяга.",
        "Выбери нужный цвет и пройди вращающееся кольцо.",
        "Настрой леску влево/вправо, действие — подсечь.",
        "Сдвигай фигуру по сторонам и поворачивай действием.",
        "Уклоняйся от астероидов и включай щит действием."
    };

    static void ensureChannel(Context c){
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel ch=new NotificationChannel(CHANNEL,"Arcade games in notifications",NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("Интерактивные мини-игры в развёрнутом уведомлении");
            ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
    }
    static void start(Context c,int game){
        ensureChannel(c);
        int g=Math.max(0,Math.min(19,game));
        c.getSharedPreferences(PREF,0).edit().putBoolean("active",true).putInt("game",g).putInt("score",0).putInt("lives",3).putInt("step",0).putInt("lane",1).putString("message","Игра готова! "+PROMPTS[g]).apply();
        show(c);
    }
    @Override public void onReceive(Context c,Intent i){
        String a=i.getAction();
        if(ACTION_END.equals(a)){
            c.getSharedPreferences(PREF,0).edit().putBoolean("active",false).apply();
            ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(NOTIFICATION_ID);
            return;
        }
        if(ACTION_RESET.equals(a)){SharedPreferences p=c.getSharedPreferences(PREF,0);start(c,p.getInt("game",0));return;}
        SharedPreferences p=c.getSharedPreferences(PREF,0);
        if(!p.getBoolean("active",false))return;
        int game=p.getInt("game",0),step=p.getInt("step",0),score=p.getInt("score",0),lives=p.getInt("lives",3),lane=p.getInt("lane",1);
        int action=ACTION_LEFT.equals(a)?-1:ACTION_RIGHT.equals(a)?1:0;
        lane=Math.max(0,Math.min(2,lane+action));
        boolean good;
        if(game==7){int target=step%3;good=ACTION_ACT.equals(a)&&lane==target;}
        else if(game==10){good=ACTION_ACT.equals(a)&&(step%3==1||step%3==2);}
        else if(game==0||game==1||game==6||game==12){boolean danger=(step%4==0||step%4==1);good=!danger||ACTION_ACT.equals(a);}
        else if(game==8||game==9){good=ACTION_ACT.equals(a)||(step%3!=0);}
        else if(game==11){good=(lane!=(step/2)%3)||ACTION_ACT.equals(a);}
        else if(game==14||game==19){good=(lane!=step%3)||ACTION_ACT.equals(a);}
        else if(game==17){good=ACTION_ACT.equals(a)&&(step%3!=0);}
        else {good=ACTION_ACT.equals(a)||Math.floorMod(step+lane+game,4)!=0;}
        if(good){score+=1+(game==8&&step%5==0?2:0);p.edit().putString("message",successText(game,step)).apply();}
        else {lives--;p.edit().putString("message",missText(game)).apply();}
        step++;
        boolean active=lives>0;
        p.edit().putInt("score",score).putInt("lives",lives).putInt("step",step).putInt("lane",lane).putBoolean("active",active).apply();
        if(!active)p.edit().putString("message","Раунд завершён. Нажми СБРОС, чтобы сыграть ещё раз.").apply();
        else if(step%12==0)p.edit().putString("message","Новый этап! "+PROMPTS[game]).apply();
        show(c);
    }
    private static String successText(int game,int step){
        String[] s={"Прыжок! Препятствие пройдено.","Взмах! Ты пролетел между трубами.","Змейка движется — яблоко рядом.","Отличный отбив!","Попадание по астероиду!","Блок разбит — держи шар.","Платформа поймана!","Цель поймана!","Плитки объединены!","Пара найдена или новая карточка открыта.","Бит пойман!","Поворот выполнен.","Монета собрана!","Доска установлена.","Сканирование чистое.","Тяга стабилизирована.","Цвет совпал!","Поклёвка!","Фигура перемещена.","Манёвр удался!"};
        return s[game];
    }
    private static String missText(int game){
        String[] s={"Удар о препятствие — нужна другая команда.","Труба близко — попробуй чаще взмахивать.","Змейка задела стену.","Мяч прошёл мимо ракетки.","Астероид задел корабль.","Шар потерян — двигай ракетку.","Промах по платформе.","Мимо цели — выбери другую нору.","Плитка не сдвинулась.","Карточки не совпали.","Не попал в бит.","Тупик — смени направление.","Препятствие на дорожке.","Мост качнулся.","Подлодка задела мину.","Ракета потеряла баланс.","Цвет не совпал.","Рыба сорвалась.","Фигура застряла.","Касание астероида."};
        return s[game];
    }
    private static String board(int game,int step,int lane,int score){
        int q=step%4;
        switch(game){
            case 0:return "━━━━ "+(q==0?"▲":q==1?"▰":"·")+" ━━━━   ◆";
            case 1:return "  ●     ▥       ▥   🐤";
            case 2:return "○──●──●──●     🍎";
            case 3:return "│  •       │\n│     ▰    │";
            case 4:return "✦  ·  ☄  ·  ✦     🚀";
            case 5:return "▣▣▣ ▣▣▣\n   •\n  ▰";
            case 6:return "☁   ▱    ☁\n   ▱  🐸";
            case 7:return "◯   "+(lane==0?"🐹":"◯")+"   "+(lane==1?"🐹":"◯")+"   "+(lane==2?"🐹":"◯");
            case 8:return " [2]  ["+(2<<(step%4))+"]  ["+(4<<(step%3))+"]  [ ]";
            case 9:return "■   ★   ▲   ◆   ■   ?";
            case 10:return "♪    ♫   ▮    ♬    ♪";
            case 11:return "┌─┬─┬─┐\n│·│"+(lane==1?"●":" ")+"│·│  →";
            case 12:return "🪙  ·  ▲  ·  🪙  ·  🏃";
            case 13:return "════╤════╤════   "+(lane==1?"🪵":"·");
            case 14:return "≈≈  ⚓  ≈≈  "+(lane==1?"🚤":"·")+"  💣";
            case 15:return "   ↖   🚀   ↗\n      ▽";
            case 16:return "🔴  🟢  🔵   "+new String[]{"🔴","🟢","🔵"}[lane];
            case 17:return "🌊  🎣  🐟   "+(q==0?"!":"·");
            case 18:return " ▣▣   ▣\n  ▣   ▣▣";
            default:return "✦   ☄   ·   🛰   ✦";
        }
    }
    static PendingIntent action(Context c,String act,int code){
        Intent i=new Intent(c,GameReceiver.class);i.setAction(act);
        int flags=PendingIntent.FLAG_UPDATE_CURRENT;if(Build.VERSION.SDK_INT>=23)flags|=PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(c,code,i,flags);
    }
    static void show(Context c){
        ensureChannel(c);
        SharedPreferences p=c.getSharedPreferences(PREF,0);
        int game=p.getInt("game",0),score=p.getInt("score",0),lives=p.getInt("lives",3),step=p.getInt("step",0),lane=p.getInt("lane",1);
        boolean active=p.getBoolean("active",false);
        RemoteViews view=new RemoteViews(c.getPackageName(),R.layout.game_notification);
        view.setTextViewText(R.id.game_title,"ARCADE  •  "+NAMES[game]+(active?"":"  •  GAME OVER"));
        view.setTextViewText(R.id.game_stats,"Очки "+score+"   ♥ "+lives+"   Шаг "+step);
        view.setTextViewText(R.id.game_board,board(game,step,lane,score));
        view.setTextViewText(R.id.game_message,p.getString("message",PROMPTS[game]));
        view.setTextViewText(R.id.btn_left,"◀");
        view.setTextViewText(R.id.btn_action,game==0||game==6||game==12?"↑ ПРЫЖОК":game==4?"✦ ОГОНЬ":game==10?"♪ БИТ":"● ДЕЙСТВИЕ");
        view.setTextViewText(R.id.btn_right,"▶");
        view.setTextViewText(R.id.btn_reset,active?"■":"↻");
        view.setOnClickPendingIntent(R.id.btn_left,action(c,ACTION_LEFT,21));
        view.setOnClickPendingIntent(R.id.btn_action,action(c,ACTION_ACT,22));
        view.setOnClickPendingIntent(R.id.btn_right,action(c,ACTION_RIGHT,23));
        view.setOnClickPendingIntent(R.id.btn_reset,action(c,active?ACTION_END:ACTION_RESET,24));
        Intent open=new Intent(c,MainActivity.class);open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content=PendingIntent.getActivity(c,10,open,PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0));
        Notification n=new Notification.Builder(c,CHANNEL)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Arcade Notifications — "+NAMES[game])
            .setContentText("Разверни карточку: "+PROMPTS[game])
            .setContentIntent(content)
            .setCustomContentView(view)
            .setCustomBigContentView(view)
            .setStyle(new Notification.DecoratedCustomViewStyle())
            .setOnlyAlertOnce(true)
            .setOngoing(active)
            .build();
        ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID,n);
    }
}