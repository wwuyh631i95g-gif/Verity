package app.notifydash;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
    boolean pendingStart=false; TextView status;
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(0xff131e32);getWindow().setNavigationBarColor(0xff131e32);
        GameReceiver.ensureChannel(this);
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(22,28,22,20);root.setBackgroundColor(0xff17233a);
        TextView title=new TextView(this);title.setText("DASHBELL  ▸  игра в уведомлении");title.setTextColor(Color.WHITE);title.setTextSize(21);root.addView(title);
        TextView desc=new TextView(this);desc.setText("\nМини-платформер, который живёт в шторке уведомлений. Разверни уведомление: кнопкой «ПРЫЖОК» перепрыгивай ямы и шипы, кнопкой «НИЗКО» проходи под балками. Каждый ход открывает следующий участок трассы.");desc.setTextColor(0xffd2dced);desc.setTextSize(15);root.addView(desc);
        Button start=new Button(this);start.setText("Запустить игру в уведомлении");start.setAllCaps(false);start.setOnClickListener(v->startGame());root.addView(start);
        Button end=new Button(this);end.setText("Убрать игровое уведомление");end.setAllCaps(false);end.setOnClickListener(v->{stopService(new Intent(this,GameReceiver.class));((android.app.NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(GameReceiver.NOTIFICATION_ID);status.setText("Уведомление убрано.");});root.addView(end);
        status=new TextView(this);status.setText("\nПосле запуска открой шторку и разверни карточку DashBell. Внутри находятся настоящие кнопки управления.");status.setTextColor(0xff8fe0c2);status.setTextSize(13);root.addView(status);
        setContentView(root);
    }
    void startGame(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){pendingStart=true;requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},88);return;}
        GameReceiver.start(this);status.setText("Игра запущена. Открой и разверни уведомление DashBell.");}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==88&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED&&pendingStart){pendingStart=false;GameReceiver.start(this);status.setText("Игра запущена. Открой шторку уведомлений.");}else if(r==88){pendingStart=false;status.setText("Разрешение на уведомления не выдано — Android не покажет игровую карточку.");}}
}
