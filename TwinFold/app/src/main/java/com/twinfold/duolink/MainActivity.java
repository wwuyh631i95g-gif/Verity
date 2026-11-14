package com.twinfold.duolink;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {
    private static final int PORT = 38471;
    private static final int RC_CAPTURE = 4101;
    private static final int RC_INSTALL_PERMISSION = 4102;
    private static final int BG = Color.rgb(8, 14, 27);
    private static final int CARD = Color.rgb(19, 29, 49);
    private static final int CARD_2 = Color.rgb(25, 39, 65);
    private static final int ACCENT = Color.rgb(100, 210, 255);
    private static final int TEXT = Color.rgb(238, 245, 255);
    private static final int MUTED = Color.rgb(153, 171, 197);

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newCachedThreadPool();
    private final Object writeLock = new Object();
    private final AtomicBoolean frameBusy = new AtomicBoolean(false);
    private volatile Socket peerSocket;
    private volatile PrintWriter peerWriter;
    private volatile BufferedReader peerReader;
    private volatile ServerSocket serverSocket;
    private volatile boolean paired = false;
    private volatile boolean allowRemoteLaunch = false;

    private String pairingPin = "";
    private String pendingOfferPackage = "";
    private String approvedPackage = "";
    private String pendingInstallPackage = "";
    private List<File> pendingInstallFiles = new ArrayList<>();
    private File incomingDir;
    private FileOutputStream incomingFile;
    private File incomingCurrentFile;
    private long incomingExpected = 0;
    private long incomingWritten = 0;
    private int incomingFileCount = 0;
    private int incomingFilesDone = 0;
    private int pageNumber = 0;
    private float downX = 0;

    private TextView statusText;
    private TextView localDeviceText;
    private TextView remoteDeviceText;
    private TextView connectionInfo;
    private TextView sharedNoteText;
    private TextView appCountText;
    private TextView pageText;
    private LinearLayout appGrid;
    private LinearLayout deck;
    private LinearLayout localPanel;
    private LinearLayout remotePanel;
    private EditText noteInput;
    private EditText searchInput;
    private Spinner deckModeSpinner;
    private Spinner remoteSideSpinner;
    private ImageView screenView;
    private SharedPreferences prefs;
    private final List<ApplicationInfo> allApps = new ArrayList<>();
    private final List<ApplicationInfo> filteredApps = new ArrayList<>();

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            byte[] jpeg = intent.getByteArrayExtra(ScreenShareService.EXTRA_FRAME);
            if (jpeg == null || jpeg.length == 0) return;
            if (paired) {
                final String encoded = Base64.encodeToString(jpeg, Base64.NO_WRAP);
                io.execute(() -> sendRaw("FRAME|" + encoded));
            }
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        prefs = getSharedPreferences("twinfold", MODE_PRIVATE);
        allowRemoteLaunch = prefs.getBoolean("remote_launch", false);
        buildInterface();
        loadAllApps();
        IntentFilter filter = new IntentFilter(ScreenShareService.ACTION_FRAME);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(screenReceiver, filter);
        }
    }

    private void buildInterface() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(17), dp(17), dp(17), dp(28));
        scroll.addView(page);
        setContentView(scroll);

        TextView brand = text("TWINFOLD", 28, TEXT, true);
        brand.setLetterSpacing(0.13f);
        page.addView(brand);
        TextView sub = text("DUAL DEVICE WORKSPACE  •  0.2", 10, ACCENT, true);
        sub.setLetterSpacing(0.1f);
        LinearLayout.LayoutParams subLp = params(-2, -2);
        subLp.topMargin = dp(4);
        page.addView(sub, subLp);

        statusText = text("●  НЕ ПОДКЛЮЧЕНО", 12, MUTED, true);
        LinearLayout.LayoutParams statusLp = params(-2, -2);
        statusLp.topMargin = dp(14);
        page.addView(statusText, statusLp);

        TextView intro = text("Два Android-телефона соединяются в общее пространство TwinFold. Можно переносить APK с подтверждением установки и передавать живую часть экрана второго устройства.", 13, TEXT, false);
        intro.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams introLp = params(-1, -2);
        introLp.topMargin = dp(8);
        page.addView(intro, introLp);

        LinearLayout connectCard = card();
        addTop(page, connectCard, 14);
        connectCard.addView(text("1  •  СОЕДИНЕНИЕ", 12, ACCENT, true));
        TextView connectHelp = text("Оба телефона подключите к одной Wi‑Fi сети. На первом создайте комнату, на втором введите показанные IP и код.", 12, MUTED, false);
        connectHelp.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams ch = params(-1, -2);
        ch.topMargin = dp(6);
        connectCard.addView(connectHelp, ch);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams btnLp = params(-1, -2);
        btnLp.topMargin = dp(10);
        connectCard.addView(buttons, btnLp);
        Button host = button("Создать комнату", ACCENT, Color.rgb(5, 22, 36));
        Button join = button("Подключиться", Color.rgb(53, 79, 115), TEXT);
        LinearLayout.LayoutParams h = new LinearLayout.LayoutParams(0, dp(45), 1f);
        h.rightMargin = dp(5);
        buttons.addView(host, h);
        LinearLayout.LayoutParams j = new LinearLayout.LayoutParams(0, dp(45), 1f);
        j.leftMargin = dp(5);
        buttons.addView(join, j);
        host.setOnClickListener(v -> startHosting());
        join.setOnClickListener(v -> showConnectDialog());

        connectionInfo = text("Комната пока не создана.", 12, TEXT, false);
        connectionInfo.setPadding(dp(10), dp(10), dp(10), dp(10));
        connectionInfo.setBackground(round(CARD_2, 11));
        LinearLayout.LayoutParams ci = params(-1, -2);
        ci.topMargin = dp(9);
        connectCard.addView(connectionInfo, ci);
        Button disconnect = button("Отключить устройства", Color.rgb(72, 48, 62), Color.rgb(255, 210, 220));
        LinearLayout.LayoutParams dc = params(-1, dp(40));
        dc.topMargin = dp(8);
        connectCard.addView(disconnect, dc);
        disconnect.setOnClickListener(v -> disconnectPeer());

        LinearLayout displayCard = card();
        addTop(page, displayCard, 12);
        displayCard.addView(text("2  •  КОМПОНОВКА ДВУХ ТЕЛЕФОНОВ", 12, ACCENT, true));
        TextView displayHelp = text("Выберите, как физически расположены телефоны: рядом или один над другим. Значки можно перетаскивать через границу.", 12, MUTED, false);
        LinearLayout.LayoutParams dh = params(-1, -2);
        dh.topMargin = dp(6);
        displayCard.addView(displayHelp, dh);
        deckModeSpinner = spinner(new String[]{"Слева направо", "Справа налево", "Сверху вниз", "Снизу вверх"});
        LinearLayout.LayoutParams ds = params(-1, dp(44));
        ds.topMargin = dp(8);
        displayCard.addView(deckModeSpinner, ds);
        deck = new LinearLayout(this);
        deck.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams deckLp = params(-1, -2);
        deckLp.topMargin = dp(8);
        displayCard.addView(deck, deckLp);
        localPanel = buildDevicePanel("ЭТОТ ТЕЛЕФОН", deviceName(), false);
        remotePanel = buildDevicePanel("ВТОРОЙ ТЕЛЕФОН", "Ожидает соединения", true);
        renderDeck();
        deckModeSpinner.setSelection(prefs.getInt("deck_mode", 0));
        deckModeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                prefs.edit().putInt("deck_mode", pos).apply();
                renderDeck();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });

        LinearLayout securityCard = card();
        addTop(page, securityCard, 12);
        securityCard.addView(text("3  •  РАЗРЕШЕНИЯ", 12, ACCENT, true));
        Switch launchSwitch = new Switch(this);
        launchSwitch.setText("Принимать запросы на открытие приложений");
        launchSwitch.setTextColor(TEXT);
        launchSwitch.setTextSize(13);
        launchSwitch.setChecked(allowRemoteLaunch);
        LinearLayout.LayoutParams slp = params(-1, -2);
        slp.topMargin = dp(5);
        securityCard.addView(launchSwitch, slp);
        launchSwitch.setOnCheckedChangeListener((v, checked) -> {
            allowRemoteLaunch = checked;
            prefs.edit().putBoolean("remote_launch", checked).apply();
        });
        TextView safe = text("Перенос APK всегда требует согласия получателя и системного подтверждения Android. TwinFold не устанавливает приложения скрытно.", 11, MUTED, false);
        safe.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams safeLp = params(-1, -2);
        safeLp.topMargin = dp(4);
        securityCard.addView(safe, safeLp);

        LinearLayout screenCard = card();
        addTop(page, screenCard, 12);
        screenCard.addView(text("4  •  ЖИВОЙ РАЗДЕЛЁННЫЙ ЭКРАН", 12, ACCENT, true));
        TextView screenHelp = text("Разрешите захват экрана Android. TwinFold передаст второму телефону выбранную половину текущего изображения. Откройте нужную игру или приложение после запуска трансляции.", 12, MUTED, false);
        screenHelp.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams sh = params(-1, -2);
        sh.topMargin = dp(6);
        screenCard.addView(screenHelp, sh);
        remoteSideSpinner = spinner(new String[]{"Второму телефону: правая часть", "Второму телефону: левая часть", "Второму телефону: нижняя часть", "Второму телефону: верхняя часть"});
        LinearLayout.LayoutParams rs = params(-1, dp(44));
        rs.topMargin = dp(8);
        screenCard.addView(remoteSideSpinner, rs);
        remoteSideSpinner.setSelection(prefs.getInt("remote_side", 0));
        remoteSideSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                prefs.edit().putInt("remote_side", pos).apply();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });
        Button startStream = button("Запустить трансляцию экрана", ACCENT, Color.rgb(5, 22, 36));
        LinearLayout.LayoutParams ss = params(-1, dp(43));
        ss.topMargin = dp(8);
        screenCard.addView(startStream, ss);
        startStream.setOnClickListener(v -> startScreenCapture());
        Button stopStream = button("Остановить трансляцию", CARD_2, TEXT);
        LinearLayout.LayoutParams st = params(-1, dp(40));
        st.topMargin = dp(6);
        screenCard.addView(stopStream, st);
        stopStream.setOnClickListener(v -> stopScreenCapture());
        screenView = new ImageView(this);
        screenView.setAdjustViewBounds(true);
        screenView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        screenView.setBackground(round(Color.BLACK, 12));
        LinearLayout.LayoutParams iv = params(-1, 220);
        iv.topMargin = dp(10);
        screenCard.addView(screenView, iv);
        screenView.setOnClickListener(v -> {
            if (ScreenViewerActivity.latestFrame != null) startActivity(new Intent(this, ScreenViewerActivity.class));
        });
        TextView tapView = text("Окно приёма экрана • нажмите для полного просмотра", 10, MUTED, false);
        tapView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tv = params(-1, -2);
        tv.topMargin = dp(5);
        screenCard.addView(tapView, tv);

        LinearLayout appsCard = card();
        addTop(page, appsCard, 12);
        appsCard.addView(text("5  •  ПОЛКА ВСЕХ ПРИЛОЖЕНИЙ", 12, ACCENT, true));
        TextView appHelp = text("Показываются все найденные установленные пакеты, включая системные. Нажмите значок — откроется приложение на этом телефоне. Зажмите и перетащите на карточку второго телефона — отправится APK-пакет на установку.", 12, MUTED, false);
        appHelp.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams ah = params(-1, -2);
        ah.topMargin = dp(6);
        appsCard.addView(appHelp, ah);
        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setTextSize(13);
        searchInput.setTextColor(TEXT);
        searchInput.setHintTextColor(MUTED);
        searchInput.setHint("Поиск приложения или пакета");
        searchInput.setInputType(InputType.TYPE_CLASS_TEXT);
        searchInput.setPadding(dp(10), dp(7), dp(10), dp(7));
        searchInput.setBackground(round(CARD_2, 11));
        LinearLayout.LayoutParams searchLp = params(-1, dp(42));
        searchLp.topMargin = dp(9);
        appsCard.addView(searchInput, searchLp);
        appCountText = text("Загрузка списка…", 11, MUTED, false);
        LinearLayout.LayoutParams cnt = params(-1, -2);
        cnt.topMargin = dp(8);
        appsCard.addView(appCountText, cnt);
        appGrid = new LinearLayout(this);
        appGrid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams gridLp = params(-1, -2);
        gridLp.topMargin = dp(8);
        appsCard.addView(appGrid, gridLp);
        pageText = text("Страница 1", 11, MUTED, false);
        pageText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams pg = params(-1, -2);
        pg.topMargin = dp(7);
        appsCard.addView(pageText, pg);
        Button refreshApps = button("Обновить список приложений", CARD_2, TEXT);
        LinearLayout.LayoutParams ra = params(-1, dp(39));
        ra.topMargin = dp(7);
        appsCard.addView(refreshApps, ra);
        refreshApps.setOnClickListener(v -> loadAllApps());
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int st,int c,int a) {}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count) { pageNumber=0; filterApps(); renderApps(); }
            @Override public void afterTextChanged(Editable e) {}
        });
        appGrid.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) { downX = event.getX(); return true; }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                float delta = event.getX() - downX;
                if (Math.abs(delta) > dp(45)) {
                    int pages = Math.max(1, (filteredApps.size() + 14) / 15);
                    pageNumber = Math.max(0, Math.min(pages - 1, pageNumber + (delta < 0 ? 1 : -1)));
                    renderApps();
                    sendRaw("PAGE|" + pageNumber);
                }
                return true;
            }
            return true;
        });

        LinearLayout notesCard = card();
        addTop(page, notesCard, 12);
        notesCard.addView(text("6  •  ОБЩАЯ ПАМЯТЬ", 12, ACCENT, true));
        noteInput = new EditText(this);
        noteInput.setMinLines(2);
        noteInput.setTextColor(TEXT);
        noteInput.setHintTextColor(MUTED);
        noteInput.setHint("Заметка для второго телефона");
        noteInput.setTextSize(13);
        noteInput.setPadding(dp(10), dp(8), dp(10), dp(8));
        noteInput.setBackground(round(CARD_2, 11));
        LinearLayout.LayoutParams ni = params(-1, -2);
        ni.topMargin = dp(8);
        notesCard.addView(noteInput, ni);
        Button sendNote = button("Отправить заметку", ACCENT, Color.rgb(5, 22, 36));
        LinearLayout.LayoutParams sn = params(-1, dp(42));
        sn.topMargin = dp(7);
        notesCard.addView(sendNote, sn);
        sendNote.setOnClickListener(v -> sendNote());
        sharedNoteText = text("Здесь появятся заметки.", 12, TEXT, false);
        sharedNoteText.setPadding(dp(10), dp(10), dp(10), dp(10));
        sharedNoteText.setBackground(round(CARD_2, 11));
        LinearLayout.LayoutParams nt = params(-1, -2);
        nt.topMargin = dp(7);
        notesCard.addView(sharedNoteText, nt);

        TextView foot = text("TWINFOLD 0.2  •  ANDROID 14 / HONOR FRIENDLY", 10, MUTED, false);
        foot.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams fl = params(-1, -2);
        fl.topMargin = dp(18);
        page.addView(foot, fl);
    }

    private void addTop(LinearLayout parent, View child, int margin) {
        LinearLayout.LayoutParams lp = params(-1, -2);
        lp.topMargin = dp(margin);
        parent.addView(child, lp);
    }

    private LinearLayout buildDevicePanel(String title, String sub, boolean remote) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(11), dp(12), dp(11), dp(12));
        panel.setGravity(Gravity.CENTER_VERTICAL);
        panel.setBackground(round(remote ? Color.rgb(26,47,69) : CARD, 15));
        panel.addView(text(title, 10, remote ? ACCENT : MUTED, true));
        TextView device = text(sub, 13, TEXT, true);
        if (remote) remoteDeviceText = device; else localDeviceText = device;
        LinearLayout.LayoutParams d = params(-1, -2);
        d.topMargin = dp(9);
        panel.addView(device, d);
        panel.addView(text(remote ? "＋  ПЕРЕНЕСТИ APK СЮДА" : "ЛОКАЛЬНЫЕ ПРИЛОЖЕНИЯ", 9, ACCENT, true), params(-1, -2));
        if (remote) {
            panel.setOnDragListener((v, e) -> {
                if (e.getAction() == android.view.DragEvent.ACTION_DRAG_STARTED)
                    return e.getClipDescription() != null && e.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN);
                if (e.getAction() == android.view.DragEvent.ACTION_DRAG_ENTERED) panel.setBackground(round(Color.rgb(35,78,100),15));
                if (e.getAction() == android.view.DragEvent.ACTION_DRAG_EXITED || e.getAction() == android.view.DragEvent.ACTION_DRAG_ENDED)
                    panel.setBackground(round(Color.rgb(26,47,69),15));
                if (e.getAction() == android.view.DragEvent.ACTION_DROP) {
                    panel.setBackground(round(Color.rgb(26,47,69),15));
                    CharSequence pkg = e.getClipData().getItemAt(0).getText();
                    if (pkg != null) offerAppTransfer(pkg.toString());
                }
                return true;
            });
        }
        return panel;
    }

    private void renderDeck() {
        if (deck == null) return;
        deck.removeAllViews();
        int mode = deckModeSpinner == null ? prefs.getInt("deck_mode",0) : deckModeSpinner.getSelectedItemPosition();
        boolean vertical = mode >= 2;
        boolean remoteFirst = mode == 1 || mode == 3;
        deck.setOrientation(vertical ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        View first = remoteFirst ? remotePanel : localPanel;
        View second = remoteFirst ? localPanel : remotePanel;
        LinearLayout.LayoutParams a = vertical ? new LinearLayout.LayoutParams(-1, dp(82)) : new LinearLayout.LayoutParams(0, dp(94), 1f);
        LinearLayout.LayoutParams b = vertical ? new LinearLayout.LayoutParams(-1, dp(82)) : new LinearLayout.LayoutParams(0, dp(94), 1f);
        if (vertical) a.bottomMargin = dp(6); else a.rightMargin = dp(6);
        if (vertical) b.topMargin = dp(6); else b.leftMargin = dp(6);
        deck.addView(first, a);
        deck.addView(second, b);
    }

    private LinearLayout card() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(13), dp(13), dp(13), dp(13));
        box.setBackground(round(CARD, 16));
        return box;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button button(String value, int color, int tc) {
        Button b = new Button(this);
        b.setText(value); b.setTextSize(12); b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); b.setTextColor(tc);
        b.setBackground(round(color, 12)); b.setPadding(dp(4),0,dp(4),0);
        return b;
    }

    private Spinner spinner(String[] values) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(a);
        return s;
    }

    private LinearLayout.LayoutParams params(int w, int h) {
        return new LinearLayout.LayoutParams(w < 0 ? w : dp(w), h < 0 ? h : dp(h));
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), Color.argb(28,180,215,255));
        return d;
    }

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    private String deviceName() {
        return (Build.MANUFACTURER + " " + Build.MODEL).trim();
    }

    private void loadAllApps() {
        io.execute(() -> {
            List<ApplicationInfo> list = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
            Collections.sort(list, (a,b) -> {
                String aa = String.valueOf(getPackageManager().getApplicationLabel(a));
                String bb = String.valueOf(getPackageManager().getApplicationLabel(b));
                return aa.compareToIgnoreCase(bb);
            });
            main.post(() -> {
                allApps.clear();
                for (ApplicationInfo info : list) if (!info.packageName.equals(getPackageName())) allApps.add(info);
                filterApps();
                renderApps();
            });
        });
    }

    private void filterApps() {
        filteredApps.clear();
        String query = searchInput == null ? "" : searchInput.getText().toString().trim().toLowerCase(Locale.ROOT);
        for (ApplicationInfo info : allApps) {
            String label = String.valueOf(getPackageManager().getApplicationLabel(info));
            if (query.isEmpty() || label.toLowerCase(Locale.ROOT).contains(query) || info.packageName.toLowerCase(Locale.ROOT).contains(query))
                filteredApps.add(info);
        }
        int max = Math.max(1, (filteredApps.size() + 14) / 15);
        pageNumber = Math.max(0, Math.min(pageNumber, max - 1));
        if (appCountText != null) appCountText.setText("Найдено приложений: " + filteredApps.size() + " • всего установлено: " + allApps.size());
    }

    private void renderApps() {
        if (appGrid == null) return;
        appGrid.removeAllViews();
        int pages = Math.max(1, (filteredApps.size() + 14) / 15);
        int start = pageNumber * 15;
        int end = Math.min(filteredApps.size(), start + 15);
        LinearLayout row = null;
        for (int i=start; i<end; i++) {
            if ((i-start) % 3 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rp = params(-1,-2);
                if (i > start) rp.topMargin = dp(6);
                appGrid.addView(row,rp);
            }
            ApplicationInfo info = filteredApps.get(i);
            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setPadding(dp(4),dp(7),dp(4),dp(7));
            tile.setBackground(round(CARD_2,12));
            ImageView icon = new ImageView(this);
            try { icon.setImageDrawable(getPackageManager().getApplicationIcon(info)); } catch(Exception ignored) {}
            tile.addView(icon,params(32,32));
            TextView name = text(String.valueOf(getPackageManager().getApplicationLabel(info)),10,TEXT,true);
            name.setGravity(Gravity.CENTER); name.setMaxLines(2);
            LinearLayout.LayoutParams nl = params(-1,dp(30)); nl.topMargin=dp(4);
            tile.addView(name,nl);
            String pkg = info.packageName;
            tile.setOnClickListener(v -> launchLocal(pkg));
            tile.setOnLongClickListener(v -> {
                ClipData data = ClipData.newPlainText("TwinFold package",pkg);
                tile.startDragAndDrop(data,new View.DragShadowBuilder(tile),null,0);
                Toast.makeText(this,"Перетащите значок на сторону второго телефона для передачи APK",Toast.LENGTH_SHORT).show();
                return true;
            });
            LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0,dp(76),1f);
            tl.leftMargin=dp(3); tl.rightMargin=dp(3);
            row.addView(tile,tl);
        }
        pageText.setText("Страница " + (pageNumber+1) + " / " + pages + "   •   листайте влево или вправо");
    }

    private void launchLocal(String pkg) {
        try {
            Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
            if (launch == null) {
                Toast.makeText(this,"У этого системного пакета нет отдельного ярлыка запуска.",Toast.LENGTH_SHORT).show();
                return;
            }
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launch);
        } catch(Exception e) {
            Toast.makeText(this,"Не удалось открыть это приложение на данном телефоне.",Toast.LENGTH_SHORT).show();
        }
    }

    private void startHosting() {
        disconnectPeer();
        pairingPin = String.format(Locale.US,"%06d",new SecureRandom().nextInt(1000000));
        io.execute(() -> {
            try {
                ServerSocket server = new ServerSocket(PORT);
                serverSocket = server;
                String ip = getLocalIp();
                main.post(() -> {
                    connectionInfo.setText("IP: " + ip + ":" + PORT + "\nКОД: " + pairingPin + "\nВведите данные на втором телефоне.");
                    statusText.setText("●  КОМНАТА ОТКРЫТА — ОЖИДАНИЕ");
                    statusText.setTextColor(ACCENT);
                });
                while (!server.isClosed()) {
                    Socket socket = server.accept();
                    if (paired) { socket.close(); continue; }
                    acceptPeer(socket,pairingPin);
                }
            } catch(Exception e) {
                main.post(() -> { statusText.setText("●  НЕ УДАЛОСЬ ОТКРЫТЬ КОМНАТУ"); statusText.setTextColor(Color.RED); });
            }
        });
    }

    private void acceptPeer(Socket socket,String expectedPin) {
        io.execute(() -> {
            try {
                socket.setSoTimeout(15000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(),true);
                String first = reader.readLine();
                if (first == null) { socket.close(); return; }
                String[] p = first.split("\\|",3);
                if (p.length != 3 || !p[0].equals("PAIR") || !p[1].equals(expectedPin)) {
                    writer.println("NO|PIN"); socket.close(); return;
                }
                String other = dec(p[2]);
                socket.setSoTimeout(0);
                writer.println("WELCOME|" + enc(deviceName()));
                attachPeer(socket,reader,writer,other);
            } catch(Exception e) { try { socket.close(); } catch(Exception ignored) {} }
        });
    }

    private void showConnectDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(14),dp(6),dp(14),0);
        EditText ip = new EditText(this);
        ip.setSingleLine(true); ip.setHint("IP первого телефона");
        EditText pin = new EditText(this);
        pin.setSingleLine(true); pin.setHint("Шестизначный код");
        pin.setInputType(InputType.TYPE_CLASS_NUMBER);
        form.addView(ip); form.addView(pin);
        new AlertDialog.Builder(this).setTitle("Подключить второй телефон")
                .setMessage("Введите IP и код комнаты с первого устройства.")
                .setView(form).setNegativeButton("Отмена",null)
                .setPositiveButton("Подключиться",(d,w)->connectTo(ip.getText().toString().trim(),pin.getText().toString().trim())).show();
    }

    private void connectTo(String ip,String pin) {
        if (ip.isEmpty() || pin.length()!=6) { Toast.makeText(this,"Проверьте IP и код.",Toast.LENGTH_LONG).show(); return; }
        statusText.setText("●  ПОДКЛЮЧЕНИЕ…");
        io.execute(() -> {
            Socket socket = new Socket();
            try {
                socket.connect(new java.net.InetSocketAddress(ip,PORT),7000);
                socket.setSoTimeout(15000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(),true);
                writer.println("PAIR|" + pin + "|" + enc(deviceName()));
                String answer = reader.readLine();
                if (answer==null || !answer.startsWith("WELCOME|")) {
                    socket.close();
                    main.post(()->{statusText.setText("●  КОД НЕ ПРИНЯТ");statusText.setTextColor(Color.RED);});
                    return;
                }
                String other=dec(answer.substring(8));
                socket.setSoTimeout(0);
                attachPeer(socket,reader,writer,other);
            } catch(Exception e) {
                try{socket.close();}catch(Exception ignored){}
                main.post(()->{statusText.setText("●  ОШИБКА ПОДКЛЮЧЕНИЯ");statusText.setTextColor(Color.RED);Toast.makeText(this,"Проверьте Wi‑Fi, IP и код.",Toast.LENGTH_LONG).show();});
            }
        });
    }

    private void attachPeer(Socket socket,BufferedReader reader,PrintWriter writer,String other) {
        peerSocket=socket; peerReader=reader; peerWriter=writer; paired=true;
        main.post(()->{
            statusText.setText("●  ДВА ТЕЛЕФОНА СОЕДИНЕНЫ");
            statusText.setTextColor(ACCENT);
            remoteDeviceText.setText(other);
            connectionInfo.setText("Соединено с " + other + "\nПеретаскивание значка отправляет запрос на передачу APK.");
        });
        io.execute(()->{
            try {
                String line;
                while((line=reader.readLine())!=null) handleMessage(line,other);
            } catch(Exception ignored) {
            } finally {
                if(peerSocket==socket) {
                    paired=false; peerSocket=null; peerWriter=null; peerReader=null;
                    main.post(()->{statusText.setText("●  СОЕДИНЕНИЕ ПРЕРВАНО");statusText.setTextColor(MUTED);remoteDeviceText.setText("Не подключён");});
                }
                try{socket.close();}catch(Exception ignored){}
            }
        });
    }

    private void handleMessage(String line,String sender) {
        String[] head=line.split("\\|",2);
        if(head.length<1)return;
        String type=head[0], payload=head.length>1?head[1]:"";
        if(type.equals("NOTE")) main.post(()->sharedNoteText.setText("От "+sender+":\n"+dec(payload)));
        else if(type.equals("PAGE")) {
            try { int p=Integer.parseInt(payload); main.post(()->{pageNumber=p;filterApps();renderApps();}); } catch(Exception ignored) {}
        } else if(type.equals("OPEN")) {
            String pkg=dec(payload); main.post(()->receiveLaunchRequest(pkg,sender));
        } else if(type.equals("APP_OFFER")) {
            String[] p=payload.split("\\|",2);
            if(p.length==2) main.post(()->receiveAppOffer(dec(p[0]),dec(p[1]),sender));
        } else if(type.equals("APP_ACCEPT")) {
            String pkg=dec(payload);
            io.execute(()->sendAppBundle(pkg));
        } else if(type.equals("APK_BEGIN")) {
            String[] p=payload.split("\\|",3);
            if(p.length==3) beginIncomingApk(dec(p[0]),dec(p[1]),Integer.parseInt(p[2]));
        } else if(type.equals("APK_FILE")) {
            String[] p=payload.split("\\|",2);
            if(p.length==2) beginIncomingFile(dec(p[0]),Long.parseLong(p[1]));
        } else if(type.equals("APK_CHUNK")) {
            writeIncomingChunk(payload);
        } else if(type.equals("APK_FILE_END")) {
            finishIncomingFile();
        } else if(type.equals("APK_END")) {
            finishIncomingBundle();
        } else if(type.equals("FRAME")) {
            byte[] bytes=Base64.decode(payload,Base64.DEFAULT);
            Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length);
            if(bitmap!=null) {
                ScreenViewerActivity.latestFrame=bitmap;
                main.post(()->{if(screenView!=null)screenView.setImageBitmap(bitmap);ScreenViewerActivity.updateIfVisible(bitmap);});
            }
        }
    }

    private void receiveLaunchRequest(String pkg,String sender) {
        if(!allowRemoteLaunch) {
            new AlertDialog.Builder(this).setTitle("Запрос от "+sender)
                    .setMessage("Запрос на открытие "+pkg+". Сначала включите разрешение на запросы.")
                    .setPositiveButton("Понятно",null).show();
            return;
        }
        new AlertDialog.Builder(this).setTitle("Открыть приложение?")
                .setMessage(sender+" просит запустить "+pkg+".")
                .setNegativeButton("Отклонить",null).setPositiveButton("Открыть",(d,w)->launchLocal(pkg)).show();
    }

    private void offerAppTransfer(String pkg) {
        if(!paired) { Toast.makeText(this,"Сначала соедините телефоны.",Toast.LENGTH_SHORT).show(); return; }
        try {
            ApplicationInfo info=getPackageManager().getApplicationInfo(pkg,0);
            String label=String.valueOf(getPackageManager().getApplicationLabel(info));
            sendRaw("APP_OFFER|"+enc(pkg)+"|"+enc(label));
            Toast.makeText(this,"Запрос передачи APK отправлен второму телефону.",Toast.LENGTH_LONG).show();
        } catch(Exception e) { Toast.makeText(this,"Не удалось найти приложение.",Toast.LENGTH_SHORT).show(); }
    }

    private void receiveAppOffer(String pkg,String label,String sender) {
        pendingOfferPackage=pkg;
        new AlertDialog.Builder(this).setTitle("Перенести приложение?")
                .setMessage(sender+" хочет отправить приложение «"+label+"» ("+pkg+").\n\nAPK будет принят только после согласия, а установка потребует подтверждения Android.")
                .setNegativeButton("Отклонить",(d,w)->pendingOfferPackage="")
                .setPositiveButton("Принять APK",(d,w)->{
                    approvedPackage=pkg;
                    sendRaw("APP_ACCEPT|"+enc(pkg));
                    Toast.makeText(this,"Ожидается APK-пакет…",Toast.LENGTH_SHORT).show();
                }).show();
    }

    private void sendAppBundle(String pkg) {
        try {
            PackageInfo pi=getPackageManager().getPackageInfo(pkg,0);
            List<File> files=new ArrayList<>();
            files.add(new File(pi.applicationInfo.sourceDir));
            if(pi.applicationInfo.splitSourceDirs!=null) for(String path:pi.applicationInfo.splitSourceDirs) files.add(new File(path));
            for(File f:files) if(!f.isFile() || !f.canRead()) throw new Exception("APK недоступен для чтения: "+f.getName());
            String label=String.valueOf(getPackageManager().getApplicationLabel(pi.applicationInfo));
            synchronized(writeLock) {
                PrintWriter w=peerWriter;
                if(w==null || !paired) return;
                w.println("APK_BEGIN|"+enc(pkg)+"|"+enc(label)+"|"+files.size());
                byte[] buffer=new byte[24576];
                for(int i=0;i<files.size();i++) {
                    File file=files.get(i);
                    String splitName = (pi.splitNames != null && i-1 < pi.splitNames.length) ? pi.splitNames[i-1] : String.valueOf(i);
                    String name=i==0?"base.apk":"split_"+splitName+".apk";
                    w.println("APK_FILE|"+enc(name)+"|"+file.length());
                    try(InputStream in=new FileInputStream(file)) {
                        int n;
                        while((n=in.read(buffer))!=-1) {
                            String chunk=Base64.encodeToString(java.util.Arrays.copyOf(buffer,n),Base64.NO_WRAP);
                            w.println("APK_CHUNK|"+chunk);
                        }
                    }
                    w.println("APK_FILE_END");
                }
                w.println("APK_END");
                if(w.checkError()) throw new Exception("Ошибка передачи по сети");
            }
            main.post(()->Toast.makeText(this,"APK приложения отправлен на второй телефон.",Toast.LENGTH_LONG).show());
        } catch(Exception e) {
            main.post(()->new AlertDialog.Builder(this).setTitle("Не удалось экспортировать APK")
                    .setMessage("Android не разрешил прочитать установочный пакет этого приложения. Для некоторых системных или защищённых приложений такое ограничение нормально.\n"+e.getMessage())
                    .setPositiveButton("Понятно",null).show());
        }
    }

    private void beginIncomingApk(String pkg,String label,int count) {
        if(!pkg.equals(approvedPackage) || count<1 || count>30) {
            sendRaw("APK_REJECT|"+enc(pkg));
            incomingDir=null; return;
        }
        try {
            incomingDir=new File(getCacheDir(),"incoming_"+System.currentTimeMillis());
            if(!incomingDir.mkdirs()) throw new Exception("Не удалось создать временную папку");
            incomingFileCount=count; incomingFilesDone=0;
            pendingInstallPackage=pkg; pendingInstallFiles=new ArrayList<>();
            main.post(()->connectionInfo.setText("Получение APK: "+label+"…"));
        } catch(Exception e) { incomingDir=null; }
    }

    private void beginIncomingFile(String name,long length) {
        if(incomingDir==null || length<1 || length>1500L*1024L*1024L) return;
        try {
            String safe=name.replaceAll("[^A-Za-z0-9._-]","");
            incomingCurrentFile=new File(incomingDir,safe);
            incomingFile=new FileOutputStream(incomingCurrentFile);
            incomingExpected=length; incomingWritten=0;
        } catch(Exception e) { incomingCurrentFile=null; incomingFile=null; }
    }

    private void writeIncomingChunk(String payload) {
        if(incomingFile==null) return;
        try {
            byte[] bytes=Base64.decode(payload,Base64.DEFAULT);
            incomingFile.write(bytes);
            incomingWritten+=bytes.length;
            if(incomingWritten>incomingExpected) { incomingFile.close(); incomingFile=null; }
        } catch(Exception e) {
            try{incomingFile.close();}catch(Exception ignored){}
            incomingFile=null;
        }
    }

    private void finishIncomingFile() {
        try {
            if(incomingFile!=null) {
                incomingFile.flush(); incomingFile.close();
                if(incomingWritten==incomingExpected && incomingCurrentFile!=null) pendingInstallFiles.add(incomingCurrentFile);
            }
        } catch(Exception ignored) {}
        incomingFile=null; incomingCurrentFile=null; incomingFilesDone++;
    }

    private void finishIncomingBundle() {
        File dir=incomingDir; incomingDir=null;
        if(dir==null || incomingFilesDone!=incomingFileCount || pendingInstallFiles.size()!=incomingFileCount || pendingInstallFiles.isEmpty()) {
            main.post(()->Toast.makeText(this,"Передача APK завершилась с ошибкой.",Toast.LENGTH_LONG).show());
            approvedPackage="";
            return;
        }
        main.post(()->{
            Toast.makeText(this,"APK получен. Проверяем возможность установки…",Toast.LENGTH_LONG).show();
            continueInstallReceived();
        });
    }

    private void continueInstallReceived() {
        if(pendingInstallFiles==null || pendingInstallFiles.isEmpty()) return;
        if(Build.VERSION.SDK_INT>=26 && !getPackageManager().canRequestPackageInstalls()) {
            Intent settings=new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName()));
            new AlertDialog.Builder(this).setTitle("Разрешить установку")
                    .setMessage("Для установки переданного APK Android попросит разрешить TwinFold устанавливать приложения. После возврата установка продолжится с системным подтверждением.")
                    .setPositiveButton("Открыть настройки",(d,w)->{try{startActivityForResult(settings,RC_INSTALL_PERMISSION);}catch(Exception ignored){}})
                    .setNegativeButton("Позже",null).show();
            return;
        }
        installReceivedPackage();
    }

    private void installReceivedPackage() {
        try {
            PackageInstaller installer=getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params=new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            if(Build.VERSION.SDK_INT>=31) params.setAppPackageName(pendingInstallPackage);
            int sessionId=installer.createSession(params);
            PackageInstaller.Session session=installer.openSession(sessionId);
            for(File file:pendingInstallFiles) {
                try(InputStream in=new FileInputStream(file);
                    java.io.OutputStream out=session.openWrite(file.getName(),0,file.length())) {
                    byte[] buffer=new byte[32768]; int n;
                    while((n=in.read(buffer))!=-1) out.write(buffer,0,n);
                    session.fsync(out);
                }
            }
            Intent callback=new Intent(this,InstallStatusReceiver.class);
            callback.setAction("com.twinfold.duolink.INSTALL_STATUS");
            PendingIntent pi=PendingIntent.getBroadcast(this,sessionId,callback,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_MUTABLE);
            session.commit(pi.getIntentSender());
            Toast.makeText(this,"Передано установщику Android. Подтвердите установку на системном экране.",Toast.LENGTH_LONG).show();
            approvedPackage="";
            pendingInstallFiles.clear();
        } catch(Exception e) {
            new AlertDialog.Builder(this).setTitle("Ошибка установки APK")
                    .setMessage("Установщик Android отклонил пакет. Возможно, не хватает split-частей, подпись приложения отличается или пакет несовместим.\n"+e.getMessage())
                    .setPositiveButton("Понятно",null).show();
        }
    }

    private void sendNote() {
        String note=noteInput.getText().toString().trim();
        if(note.isEmpty() || !paired) {Toast.makeText(this,"Введите заметку и соедините телефоны.",Toast.LENGTH_SHORT).show();return;}
        sendRaw("NOTE|"+enc(note)); sharedNoteText.setText("Отправлено: "+note); noteInput.setText("");
    }

    private void startScreenCapture() {
        if(!paired) {Toast.makeText(this,"Сначала соедините оба телефона.",Toast.LENGTH_SHORT).show();return;}
        MediaProjectionManager manager=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(manager.createScreenCaptureIntent(),RC_CAPTURE);
    }

    private void stopScreenCapture() {
        Intent stop=new Intent(this,ScreenShareService.class);
        stop.setAction(ScreenShareService.ACTION_STOP);
        startService(stop);
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==RC_CAPTURE && resultCode==RESULT_OK && data!=null) {
            Intent service=new Intent(this,ScreenShareService.class);
            service.setAction(ScreenShareService.ACTION_START);
            service.putExtra(ScreenShareService.EXTRA_RESULT_CODE,resultCode);
            service.putExtra(ScreenShareService.EXTRA_RESULT_DATA,data);
            service.putExtra(ScreenShareService.EXTRA_SIDE,remoteSideSpinner.getSelectedItemPosition());
            if(Build.VERSION.SDK_INT>=26) startForegroundService(service); else startService(service);
            Toast.makeText(this,"Захват экрана включён. Откройте игру или нужное приложение — выбранная половина появится у партнёра.",Toast.LENGTH_LONG).show();
        } else if(requestCode==RC_INSTALL_PERMISSION) {
            if(Build.VERSION.SDK_INT<26 || getPackageManager().canRequestPackageInstalls()) installReceivedPackage();
            else Toast.makeText(this,"Разрешение на установку не включено.",Toast.LENGTH_LONG).show();
        }
    }

    private void sendRaw(String message) {
        synchronized(writeLock) {
            PrintWriter writer=peerWriter;
            if(writer==null || !paired) return;
            writer.println(message);
            if(writer.checkError()) main.post(()->Toast.makeText(this,"Ошибка сетевой передачи.",Toast.LENGTH_SHORT).show());
        }
    }

    private void disconnectPeer() {
        paired=false;
        PrintWriter w=peerWriter; Socket s=peerSocket; ServerSocket server=serverSocket;
        peerWriter=null; peerReader=null; peerSocket=null; serverSocket=null;
        try{if(w!=null)w.close();}catch(Exception ignored){}
        try{if(s!=null)s.close();}catch(Exception ignored){}
        try{if(server!=null)server.close();}catch(Exception ignored){}
        if(statusText!=null) {
            statusText.setText("●  НЕ ПОДКЛЮЧЕНО"); statusText.setTextColor(MUTED);
            if(remoteDeviceText!=null)remoteDeviceText.setText("Ожидает соединения");
            if(connectionInfo!=null)connectionInfo.setText("Соединение закрыто.");
        }
    }

    private String getLocalIp() {
        try {
            Enumeration<NetworkInterface> nets=NetworkInterface.getNetworkInterfaces();
            while(nets.hasMoreElements()) {
                Enumeration<java.net.InetAddress> addresses=nets.nextElement().getInetAddresses();
                while(addresses.hasMoreElements()) {
                    java.net.InetAddress a=addresses.nextElement();
                    if(!a.isLoopbackAddress() && a instanceof Inet4Address) {
                        String ip=a.getHostAddress();
                        if(!ip.startsWith("169.254."))return ip;
                    }
                }
            }
        }catch(Exception ignored){}
        return "IP не найден";
    }

    private String enc(String value) {
        return Base64.encodeToString(value.getBytes(StandardCharsets.UTF_8),Base64.NO_WRAP);
    }
    private String dec(String value) {
        try{return new String(Base64.decode(value,Base64.DEFAULT),StandardCharsets.UTF_8);}catch(Exception e){return value;}
    }

    @Override protected void onDestroy() {
        try{unregisterReceiver(screenReceiver);}catch(Exception ignored){}
        disconnectPeer();
        try { stopService(new Intent(this, ScreenShareService.class)); } catch(Exception ignored) {}
        io.shutdownNow();
        super.onDestroy();
    }
}
