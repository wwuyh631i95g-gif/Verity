package app.notifydash;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;

public class MainActivity extends Activity {
    boolean pendingStart=false;
    TextView status;
    static final String[] NAMES={
        "Geometry Dash","Flappy Hop","Змейка","Мини-Понг","Космострел","Арканоид",
        "Doodle Jump","Ударь крота","2048","Память","Ритм-бит","Лабиринт",
        "Монетный забег","Мостостроитель","Подлодка","Посадка ракеты",
        "Color Switch","Рыбалка","Мини-Тетрис","Орбитальный рывок"
    };
    static final String[] DESCRIPTIONS={
        "Прыгай через шипы и блоки.","Подпрыгивай, чтобы пролететь между трубами.",
        "Поворачивай и веди змейку к еде.","Двигай ракетку и отбивай мяч.",
        "Уклоняйся и стреляй по метеорам.","Лови шарик и разбивай блоки.",
        "Прыгай по платформам, двигаясь в стороны.","Попади по появившейся цели.",
        "Сдвигай плитки и собирай большие числа.","Открывай карточки и ищи пары.",
        "Нажимай в такт музыкальному индикатору.","Выбирай направление в коротком лабиринте.",
        "Собирай монеты и не попадай под препятствия.","Балансируй и достраивай мост.",
        "Меняй глубину и обходи мины.","Управляй тягой и выравнивай ракету.",
        "Подбирай цвет перед прохождением кольца.","Лови момент, когда рыба клюнет.",
        "Двигай и поворачивай падающие блоки.","Уклоняйся от метеоров на орбите."
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(0xff131e32);
        getWindow().setNavigationBarColor(0xff131e32);
        GameReceiver.ensureChannel(this);

        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16,18,16,18);
        root.setBackgroundColor(0xff17233a);

        TextView title=new TextView(this);
        title.setText("ARCADE NOTIFICATIONS");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(null,1);
        root.addView(title);

        TextView desc=new TextView(this);
        desc.setText("20 маленьких игр прямо в развёрнутой карточке уведомления. Выбери игру ниже, затем открой шторку. Кнопки обновляют игровое поле, очки и состояние раунда.");
        desc.setTextColor(0xffd2dced);
        desc.setTextSize(14);
        desc.setPadding(0,7,0,12);
        root.addView(desc);

        Button stop=new Button(this);
        stop.setText("Убрать игровое уведомление");
        stop.setAllCaps(false);
        stop.setOnClickListener(v->{
            Intent i=new Intent(this,GameReceiver.class);i.setAction(GameReceiver.ACTION_END);sendBroadcast(i);
            status.setText("Игровое уведомление остановлено.");
        });
        root.addView(stop);

        status=new TextView(this);
        status.setText("Выбери любой режим. Никакие сторонние приложения не нужны.");
        status.setTextColor(0xff8fe0c2);
        status.setTextSize(12);
        status.setPadding(0,5,0,7);
        root.addView(status);

        ScrollView scroll=new ScrollView(this);
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);
        for(int i=0;i<NAMES.length;i+=2){
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(gameButton(i),new LinearLayout.LayoutParams(0,-2,1));
            if(i+1<NAMES.length)row.addView(gameButton(i+1),new LinearLayout.LayoutParams(0,-2,1));
            list.addView(row);
        }
        scroll.addView(list);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
    }

    private Button gameButton(int index){
        Button b=new Button(this);
        b.setAllCaps(false);b.setTextSize(11);b.setTextColor(Color.WHITE);
        b.setMinHeight(60);
        b.setText((index+1)+". "+NAMES[index]+"\n"+DESCRIPTIONS[index]);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff304866));
        b.setOnClickListener(v->startGame(index));
        return b;
    }

    private void startGame(int game){
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
            pendingStart=true;
            getPreferences(0).edit().putInt("pending_game",game).apply();
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},88);
            return;
        }
        GameReceiver.start(this,game);
        status.setText("Запущена игра: "+NAMES[game]+". Открой шторку уведомлений и разверни Arcade.");
    }
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){
        super.onRequestPermissionsResult(r,p,g);
        if(r==88&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED&&pendingStart){
            pendingStart=false;int game=getPreferences(0).getInt("pending_game",0);
            GameReceiver.start(this,game);
            status.setText("Игра запущена. Открой шторку уведомлений.");
        }else if(r==88){
            pendingStart=false;status.setText("Разрешение на уведомления не выдано — Android не покажет игровую карточку.");
        }
    }
}