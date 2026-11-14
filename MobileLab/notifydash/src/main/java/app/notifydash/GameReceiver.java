package app.notifydash;

import android.app.*;
import android.content.*;
import android.os.Build;
import android.widget.RemoteViews;

public class GameReceiver extends BroadcastReceiver {
    static final String CHANNEL="dashbell_game";
    static final int NOTIFICATION_ID=7138;
    static final String ACTION_START="app.notifydash.START", ACTION_JUMP="app.notifydash.JUMP", ACTION_DUCK="app.notifydash.DUCK", ACTION_RESET="app.notifydash.RESET", ACTION_END="app.notifydash.END";
    static final String PREF="dashbell_state";
    static void ensureChannel(Context c){if(Build.VERSION.SDK_INT>=26){NotificationChannel ch=new NotificationChannel(CHANNEL,"DashBell game",NotificationManager.IMPORTANCE_DEFAULT);ch.setDescription("Мини-игра в шторке уведомлений");((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(ch);}}
    static void start(Context c){ensureChannel(c);SharedPreferences p=c.getSharedPreferences(PREF,0);p.edit().putBoolean("active",true).putInt("score",0).putInt("lives",3).putInt("step",0).putString("message","Трасса началась!").apply();show(c);}
    @Override public void onReceive(Context c,Intent i){String a=i.getAction();SharedPreferences p=c.getSharedPreferences(PREF,0);if(ACTION_START.equals(a)||ACTION_RESET.equals(a)){start(c);return;}if(ACTION_END.equals(a)){((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(NOTIFICATION_ID);return;}
        if(!p.getBoolean("active",false))return;
        int step=p.getInt("step",0),score=p.getInt("score",0),lives=p.getInt("lives",3);int obstacle=step%8;boolean jump=ACTION_JUMP.equals(a),duck=ACTION_DUCK.equals(a);
        boolean hit=(obstacle==0||obstacle==1||obstacle==5)?!jump:(obstacle==2||obstacle==6)?!duck:false;
        String obstacleName=(obstacle==0?"яма":(obstacle==1||obstacle==5)?"шипы":(obstacle==2||obstacle==6)?"низкая балка":(obstacle==3)?"монетка":"ровная дорожка");
        if(hit){lives--;p.edit().putString("message","Столкновение: "+obstacleName+". Попробуй следующий участок!").apply();}
        else{score+=obstacle==3?2:1;p.edit().putString("message",obstacle==3?"Монетка! +2 очка":"Отлично! Пройдено: "+obstacleName).apply();}
        step++;boolean active=lives>0;p.edit().putInt("score",score).putInt("lives",lives).putInt("step",step).putBoolean("active",active).apply();
        if(!active)p.edit().putString("message","Забег завершён. Нажми «Сброс» для новой игры.").apply();
        else if(step%8==0)p.edit().putString("message","Уровень "+(step/8+1)+" открыт!").apply();
        show(c);
    }
    static PendingIntent action(Context c,String act,int code){Intent i=new Intent(c,GameReceiver.class);i.setAction(act);int flags=PendingIntent.FLAG_UPDATE_CURRENT;if(Build.VERSION.SDK_INT>=23)flags|=PendingIntent.FLAG_IMMUTABLE;return PendingIntent.getBroadcast(c,code,i,flags);}
    static void show(Context c){ensureChannel(c);SharedPreferences p=c.getSharedPreferences(PREF,0);int score=p.getInt("score",0),lives=p.getInt("lives",3),step=p.getInt("step",0);boolean active=p.getBoolean("active",false);
        RemoteViews view=new RemoteViews(c.getPackageName(),R.layout.game_notification);
        view.setTextViewText(R.id.game_title,"DASHBELL  •  "+(active?"ЗАБЕГ":"ИГРА ОСТАНОВЛЕНА"));
        view.setTextViewText(R.id.game_stats,"Очки: "+score+"     Жизни: "+lives+"     Участок: "+(step+1));
        view.setTextViewText(R.id.game_message,p.getString("message","Готов? Прыгай через препятствия!"));
        view.setTextViewText(R.id.btn_jump,"↑ ПРЫЖОК");view.setTextViewText(R.id.btn_duck,"↓ НИЗКО");view.setTextViewText(R.id.btn_reset,active?"■ СТОП":"↻ СБРОС");
        view.setOnClickPendingIntent(R.id.btn_jump,action(c,ACTION_JUMP,1));
        view.setOnClickPendingIntent(R.id.btn_duck,action(c,ACTION_DUCK,2));
        view.setOnClickPendingIntent(R.id.btn_reset,action(c,active?ACTION_END:ACTION_RESET,3));
        Intent open=new Intent(c,MainActivity.class);open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content=PendingIntent.getActivity(c,10,open,PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0));
        Notification n=new Notification.Builder(c,CHANNEL).setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("DashBell — игра").setContentText("Разверни уведомление и играй кнопками").setContentIntent(content).setCustomContentView(view).setCustomBigContentView(view).setStyle(new Notification.DecoratedCustomViewStyle()).setOnlyAlertOnce(true).setOngoing(active).build();
        ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID,n);
    }
}
