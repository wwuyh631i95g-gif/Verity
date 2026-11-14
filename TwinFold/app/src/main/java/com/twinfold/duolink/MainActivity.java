package com.twinfold.duolink;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int PORT = 38471;
    private static final int BG = Color.rgb(8, 14, 27);
    private static final int CARD = Color.rgb(19, 29, 49);
    private static final int CARD_2 = Color.rgb(25, 39, 65);
    private static final int ACCENT = Color.rgb(100, 210, 255);
    private static final int TEXT = Color.rgb(238, 245, 255);
    private static final int MUTED = Color.rgb(153, 171, 197);

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newCachedThreadPool();
    private volatile Socket peerSocket;
    private volatile PrintWriter peerWriter;
    private volatile ServerSocket serverSocket;
    private volatile boolean paired = false;
    private volatile boolean allowRemoteLaunch = false;

    private String pairingPin = "";
    private TextView statusText;
    private TextView localDeviceText;
    private TextView remoteDeviceText;
    private TextView connectionInfo;
    private TextView sharedNoteText;
    private LinearLayout appGrid;
    private EditText noteInput;
    private Switch remoteLaunchSwitch;
    private SharedPreferences prefs;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        prefs = getSharedPreferences("twinfold", MODE_PRIVATE);
        allowRemoteLaunch = prefs.getBoolean("remote_launch", false);
        buildInterface();
        refreshApps();
    }

    private void buildInterface() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(18), dp(18), dp(30));
        scroll.addView(page);
        setContentView(scroll);

        TextView brand = text("TWINFOLD", 27, TEXT, true);
        brand.setLetterSpacing(0.12f);
        page.addView(brand);

        TextView tagline = text("TWO PHONES  /  ONE SHARED SPACE", 11, ACCENT, true);
        tagline.setLetterSpacing(0.09f);
        LinearLayout.LayoutParams tagLp = params(-2, -2);
        tagLp.topMargin = dp(4);
        page.addView(tagline, tagLp);

        statusText = text("●  НЕ ПОДКЛЮЧЕНО", 12, MUTED, true);
        LinearLayout.LayoutParams statusLp = params(-2, -2);
        statusLp.topMargin = dp(16);
        page.addView(statusText, statusLp);

        TextView intro = text("Общий экран внутри TwinFold: соедините два телефона по одной Wi‑Fi сети, чтобы передавать заметки и отправлять друг другу запросы на запуск приложений.", 14, TEXT, false);
        intro.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams introLp = params(-1, -2);
        introLp.topMargin = dp(10);
        page.addView(intro, introLp);

        LinearLayout deck = new LinearLayout(this);
        deck.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams deckLp = params(-1, -2);
        deckLp.topMargin = dp(18);
        page.addView(deck, deckLp);

        LinearLayout localPanel = buildDevicePanel("ЭТОТ ТЕЛЕФОН", "Локальная сторона", false);
        LinearLayout remotePanel = buildDevicePanel("ВТОРОЙ ТЕЛЕФОН", "Перетащите сюда значок", true);
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, dp(150), 1f);
        half.rightMargin = dp(6);
        deck.addView(localPanel, half);
        LinearLayout.LayoutParams half2 = new LinearLayout.LayoutParams(0, dp(150), 1f);
        half2.leftMargin = dp(6);
        deck.addView(remotePanel, half2);

        LinearLayout connectCard = card();
        LinearLayout.LayoutParams connectLp = params(-1, -2);
        connectLp.topMargin = dp(18);
        page.addView(connectCard, connectLp);

        connectCard.addView(text("СОЕДИНЕНИЕ", 12, ACCENT, true));
        TextView connectHelp = text("Оба телефона должны быть в одной Wi‑Fi сети. На первом нажмите «Создать комнату», на втором — «Подключиться».", 13, MUTED, false);
        connectHelp.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams helpLp = params(-1, -2);
        helpLp.topMargin = dp(8);
        connectCard.addView(connectHelp, helpLp);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams buttonsLp = params(-1, -2);
        buttonsLp.topMargin = dp(12);
        connectCard.addView(buttons, buttonsLp);

        Button host = button("Создать комнату", ACCENT, Color.rgb(5, 22, 36));
        Button join = button("Подключиться", Color.rgb(53, 79, 115), TEXT);
        LinearLayout.LayoutParams b1 = new LinearLayout.LayoutParams(0, dp(46), 1f);
        b1.rightMargin = dp(5);
        buttons.addView(host, b1);
        LinearLayout.LayoutParams b2 = new LinearLayout.LayoutParams(0, dp(46), 1f);
        b2.leftMargin = dp(5);
        buttons.addView(join, b2);

        host.setOnClickListener(v -> startHosting());
        join.setOnClickListener(v -> showConnectDialog());

        connectionInfo = text("Комната пока не создана.", 13, TEXT, false);
        connectionInfo.setPadding(dp(11), dp(10), dp(11), dp(10));
        connectionInfo.setBackground(round(CARD_2, 12));
        LinearLayout.LayoutParams infoLp = params(-1, -2);
        infoLp.topMargin = dp(10);
        connectCard.addView(connectionInfo, infoLp);

        Button disconnect = button("Отключить устройства", Color.rgb(72, 48, 62), Color.rgb(255, 210, 220));
        LinearLayout.LayoutParams disLp = params(-1, dp(42));
        disLp.topMargin = dp(9);
        connectCard.addView(disconnect, disLp);
        disconnect.setOnClickListener(v -> disconnectPeer());

        LinearLayout permissionsCard = card();
        LinearLayout.LayoutParams permLp = params(-1, -2);
        permLp.topMargin = dp(14);
        page.addView(permissionsCard, permLp);
        permissionsCard.addView(text("УПРАВЛЕНИЕ НА ВТОРОМ ТЕЛЕФОНЕ", 12, ACCENT, true));
        remoteLaunchSwitch = new Switch(this);
        remoteLaunchSwitch.setText("Разрешать запросы на запуск приложений");
        remoteLaunchSwitch.setTextColor(TEXT);
        remoteLaunchSwitch.setTextSize(13);
        remoteLaunchSwitch.setChecked(allowRemoteLaunch);
        LinearLayout.LayoutParams switchLp = params(-1, -2);
        switchLp.topMargin = dp(7);
        permissionsCard.addView(remoteLaunchSwitch, switchLp);
        remoteLaunchSwitch.setOnCheckedChangeListener((buttonView, checked) -> {
            allowRemoteLaunch = checked;
            prefs.edit().putBoolean("remote_launch", checked).apply();
            Toast.makeText(this, checked ? "Запросы включены. Каждый запуск всё равно нужно подтвердить." : "Удалённые запросы выключены.", Toast.LENGTH_SHORT).show();
        });
        TextView privacy = text("На принимающем телефоне каждый удалённый запуск отдельно подтверждается владельцем. Скрытого управления и невидимой трансляции здесь нет.", 12, MUTED, false);
        privacy.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams privacyLp = params(-1, -2);
        privacyLp.topMargin = dp(4);
        permissionsCard.addView(privacy, privacyLp);

        LinearLayout appsCard = card();
        LinearLayout.LayoutParams appsLp = params(-1, -2);
        appsLp.topMargin = dp(14);
        page.addView(appsCard, appsLp);
        appsCard.addView(text("ПОЛКА ПРИЛОЖЕНИЙ", 12, ACCENT, true));
        TextView appsHelp = text("Нажмите значок, чтобы открыть приложение здесь. Зажмите значок и перетащите на правую половину, чтобы отправить запрос на запуск на втором телефоне.", 12, MUTED, false);
        appsHelp.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams appsHelpLp = params(-1, -2);
        appsHelpLp.topMargin = dp(7);
        appsCard.addView(appsHelp, appsHelpLp);

        appGrid = new LinearLayout(this);
        appGrid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams gridLp = params(-1, -2);
        gridLp.topMargin = dp(13);
        appsCard.addView(appGrid, gridLp);

        Button refresh = button("Обновить список приложений", CARD_2, TEXT);
        LinearLayout.LayoutParams refreshLp = params(-1, dp(40));
        refreshLp.topMargin = dp(8);
        appsCard.addView(refresh, refreshLp);
        refresh.setOnClickListener(v -> refreshApps());

        LinearLayout noteCard = card();
        LinearLayout.LayoutParams noteLp = params(-1, -2);
        noteLp.topMargin = dp(14);
        page.addView(noteCard, noteLp);
        noteCard.addView(text("ОБЩАЯ ПАМЯТЬ", 12, ACCENT, true));
        TextView noteHelp = text("Отправьте короткую заметку — она появится в TwinFold на обоих телефонах.", 12, MUTED, false);
        LinearLayout.LayoutParams nh = params(-1, -2);
        nh.topMargin = dp(6);
        noteCard.addView(noteHelp, nh);

        noteInput = new EditText(this);
        noteInput.setSingleLine(false);
        noteInput.setMinLines(2);
        noteInput.setTextSize(14);
        noteInput.setTextColor(TEXT);
        noteInput.setHintTextColor(MUTED);
        noteInput.setHint("Напишите что-нибудь для второго телефона…");
        noteInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        noteInput.setBackground(round(CARD_2, 12));
        LinearLayout.LayoutParams noteInputLp = params(-1, -2);
        noteInputLp.topMargin = dp(10);
        noteCard.addView(noteInput, noteInputLp);

        Button sendNote = button("Отправить на второй телефон", ACCENT, Color.rgb(5, 22, 36));
        LinearLayout.LayoutParams sendLp = params(-1, dp(44));
        sendLp.topMargin = dp(8);
        noteCard.addView(sendNote, sendLp);
        sendNote.setOnClickListener(v -> sendNote());

        sharedNoteText = text("Здесь появятся полученные заметки.", 13, TEXT, false);
        sharedNoteText.setPadding(dp(11), dp(10), dp(11), dp(10));
        sharedNoteText.setBackground(round(CARD_2, 12));
        LinearLayout.LayoutParams sharedLp = params(-1, -2);
        sharedLp.topMargin = dp(8);
        noteCard.addView(sharedNoteText, sharedLp);

        TextView footer = text("TWINFOLD 0.1  •  ЛОКАЛЬНАЯ ТЕСТОВАЯ СБОРКА", 10, MUTED, false);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footLp = params(-1, -2);
        footLp.topMargin = dp(20);
        page.addView(footer, footLp);
    }

    private LinearLayout buildDevicePanel(String title, String sub, boolean remote) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(11), dp(12), dp(11), dp(10));
        panel.setBackground(round(remote ? Color.rgb(26, 47, 69) : CARD, 18));
        panel.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = text(title, 10, remote ? ACCENT : MUTED, true);
        panel.addView(label);
        TextView device = text(sub, 14, TEXT, true);
        LinearLayout.LayoutParams dlp = params(-1, -2);
        dlp.topMargin = dp(12);
        panel.addView(device, dlp);
        if (remote) {
            TextView drop = text("＋  ПЕРЕНЕСТИ СЮДА", 10, ACCENT, true);
            LinearLayout.LayoutParams dl = params(-1, -2);
            dl.topMargin = dp(10);
            panel.addView(drop, dl);
            panel.setOnDragListener((v, event) -> {
                switch (event.getAction()) {
                    case android.view.DragEvent.ACTION_DRAG_STARTED:
                        return event.getClipDescription() != null &&
                                event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN);
                    case android.view.DragEvent.ACTION_DRAG_ENTERED:
                        panel.setBackground(round(Color.rgb(35, 78, 100), 18));
                        return true;
                    case android.view.DragEvent.ACTION_DRAG_EXITED:
                        panel.setBackground(round(Color.rgb(26, 47, 69), 18));
                        return true;
                    case android.view.DragEvent.ACTION_DROP:
                        panel.setBackground(round(Color.rgb(26, 47, 69), 18));
                        CharSequence value = event.getClipData().getItemAt(0).getText();
                        if (value != null) requestRemoteLaunch(value.toString());
                        return true;
                    case android.view.DragEvent.ACTION_DRAG_ENDED:
                        panel.setBackground(round(Color.rgb(26, 47, 69), 18));
                        return true;
                }
                return true;
            });
        }
        return panel;
    }

    private LinearLayout card() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14), dp(14), dp(14), dp(14));
        box.setBackground(round(CARD, 17));
        return box;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button button(String value, int color, int textColor) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextSize(12);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(textColor);
        b.setBackground(round(color, 13));
        b.setPadding(dp(5), 0, dp(5), 0);
        return b;
    }

    private LinearLayout.LayoutParams params(int w, int h) {
        return new LinearLayout.LayoutParams(w < 0 ? w : dp(w), h < 0 ? h : dp(h));
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), Color.argb(28, 180, 215, 255));
        return d;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void refreshApps() {
        if (appGrid == null) return;
        appGrid.removeAllViews();
        Intent query = new Intent(Intent.ACTION_MAIN, null);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> found = getPackageManager().queryIntentActivities(query, 0);
        Collections.sort(found, new Comparator<ResolveInfo>() {
            @Override public int compare(ResolveInfo a, ResolveInfo b) {
                return String.valueOf(a.loadLabel(getPackageManager())).compareToIgnoreCase(
                        String.valueOf(b.loadLabel(getPackageManager())));
            }
        });

        List<ResolveInfo> apps = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ResolveInfo info : found) {
            String pkg = info.activityInfo.packageName;
            if (!pkg.equals(getPackageName()) && seen.add(pkg)) apps.add(info);
            if (apps.size() >= 18) break;
        }

        if (apps.isEmpty()) {
            appGrid.addView(text("Не удалось получить список ярлыков.", 13, MUTED, false));
            return;
        }

        LinearLayout row = null;
        for (int i = 0; i < apps.size(); i++) {
            if (i % 3 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rp = params(-1, -2);
                if (i > 0) rp.topMargin = dp(7);
                appGrid.addView(row, rp);
            }
            ResolveInfo info = apps.get(i);
            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setPadding(dp(5), dp(9), dp(5), dp(9));
            tile.setBackground(round(CARD_2, 13));
            ImageView icon = new ImageView(this);
            try { icon.setImageDrawable(info.loadIcon(getPackageManager())); } catch (Exception ignored) {}
            LinearLayout.LayoutParams ilp = params(34, 34);
            tile.addView(icon, ilp);
            TextView name = text(String.valueOf(info.loadLabel(getPackageManager())), 10, TEXT, true);
            name.setGravity(Gravity.CENTER);
            name.setMaxLines(2);
            LinearLayout.LayoutParams nlp = params(-1, 30);
            nlp.topMargin = dp(5);
            tile.addView(name, nlp);

            String packageName = info.activityInfo.packageName;
            tile.setOnClickListener(v -> launchLocal(packageName));
            tile.setOnLongClickListener(v -> {
                ClipData data = ClipData.newPlainText("TwinFold app package", packageName);
                View.DragShadowBuilder shadow = new View.DragShadowBuilder(tile);
                tile.startDragAndDrop(data, shadow, null, 0);
                Toast.makeText(this, "Перетащите на карточку второго телефона", Toast.LENGTH_SHORT).show();
                return true;
            });

            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, dp(85), 1f);
            tlp.leftMargin = dp(3);
            tlp.rightMargin = dp(3);
            row.addView(tile, tlp);
        }
    }

    private void launchLocal(String packageName) {
        try {
            Intent launch = getPackageManager().getLaunchIntentForPackage(packageName);
            if (launch == null) {
                Toast.makeText(this, "У приложения нет доступного ярлыка запуска.", Toast.LENGTH_SHORT).show();
                return;
            }
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launch);
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось открыть приложение.", Toast.LENGTH_SHORT).show();
        }
    }

    private void startHosting() {
        disconnectPeer();
        pairingPin = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        io.execute(() -> {
            try {
                ServerSocket server = new ServerSocket(PORT);
                serverSocket = server;
                String ip = getLocalIp();
                main.post(() -> {
                    connectionInfo.setText("АДРЕС: " + ip + ":" + PORT + "\nКОД: " + pairingPin +
                            "\nВведите эти данные на втором телефоне.");
                    statusText.setText("●  КОМНАТА ОТКРЫТА — ОЖИДАНИЕ");
                    statusText.setTextColor(ACCENT);
                });
                while (!server.isClosed()) {
                    Socket socket = server.accept();
                    if (paired) {
                        try { socket.close(); } catch (Exception ignored) {}
                        continue;
                    }
                    acceptPeer(socket, pairingPin);
                }
            } catch (Exception e) {
                main.post(() -> {
                    if (!paired) {
                        statusText.setText("●  НЕ УДАЛОСЬ ОТКРЫТЬ КОМНАТУ");
                        statusText.setTextColor(Color.rgb(255, 145, 145));
                    }
                });
            }
        });
    }

    private void acceptPeer(Socket socket, String expectedPin) {
        io.execute(() -> {
            try {
                socket.setSoTimeout(15000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                String first = reader.readLine();
                if (first == null) { socket.close(); return; }
                String[] parts = first.split("\\|", 3);
                if (parts.length != 3 || !parts[0].equals("PAIR") || !parts[1].equals(expectedPin)) {
                    writer.println("NO|PIN");
                    socket.close();
                    return;
                }
                String remoteName = dec(parts[2]);
                socket.setSoTimeout(0);
                writer.println("WELCOME|" + enc(deviceName()));
                attachPeer(socket, reader, writer, remoteName);
            } catch (Exception e) {
                try { socket.close(); } catch (Exception ignored) {}
            }
        });
    }

    private void showConnectDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(18), dp(8), dp(18), 0);
        EditText ip = new EditText(this);
        ip.setSingleLine(true);
        ip.setHint("IP второго телефона, например 192.168.1.5");
        ip.setInputType(InputType.TYPE_CLASS_TEXT);
        EditText pin = new EditText(this);
        pin.setSingleLine(true);
        pin.setHint("6-значный код комнаты");
        pin.setInputType(InputType.TYPE_CLASS_NUMBER);
        form.addView(ip);
        form.addView(pin);
        new AlertDialog.Builder(this)
                .setTitle("Подключить второй телефон")
                .setMessage("Введите адрес и код, которые показаны на первом телефоне.")
                .setView(form)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Соединить", (d, w) -> connectTo(ip.getText().toString().trim(), pin.getText().toString().trim()))
                .show();
    }

    private void connectTo(String ip, String pin) {
        if (ip.isEmpty() || pin.length() != 6) {
            Toast.makeText(this, "Проверьте IP-адрес и шестизначный код.", Toast.LENGTH_LONG).show();
            return;
        }
        main.post(() -> {
            statusText.setText("●  ПОДКЛЮЧЕНИЕ…");
            statusText.setTextColor(ACCENT);
        });
        io.execute(() -> {
            Socket socket = new Socket();
            try {
                socket.connect(new java.net.InetSocketAddress(ip, PORT), 7000);
                socket.setSoTimeout(15000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                writer.println("PAIR|" + pin + "|" + enc(deviceName()));
                String answer = reader.readLine();
                if (answer == null || !answer.startsWith("WELCOME|")) {
                    socket.close();
                    main.post(() -> Toast.makeText(this, "Код не принят или комната недоступна.", Toast.LENGTH_LONG).show());
                    main.post(() -> {
                        statusText.setText("●  НЕ ПОДКЛЮЧЕНО");
                        statusText.setTextColor(MUTED);
                    });
                    return;
                }
                String remoteName = dec(answer.substring("WELCOME|".length()));
                socket.setSoTimeout(0);
                attachPeer(socket, reader, writer, remoteName);
            } catch (Exception e) {
                try { socket.close(); } catch (Exception ignored) {}
                main.post(() -> {
                    statusText.setText("●  ОШИБКА ПОДКЛЮЧЕНИЯ");
                    statusText.setTextColor(Color.rgb(255, 145, 145));
                    Toast.makeText(this, "Не удалось соединиться. Проверьте Wi‑Fi, IP и код.", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void attachPeer(Socket socket, BufferedReader reader, PrintWriter writer, String remoteName) {
        peerSocket = socket;
        peerWriter = writer;
        paired = true;
        main.post(() -> {
            statusText.setText("●  ДВА ТЕЛЕФОНА СОЕДИНЕНЫ");
            statusText.setTextColor(ACCENT);
            remoteDeviceText.setText(remoteName);
            connectionInfo.setText("Связь активна с устройством: " + remoteName +
                    "\nПеретаскивайте значки приложений на правую карточку.");
        });
        io.execute(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) handleMessage(line, remoteName);
            } catch (Exception ignored) {
            } finally {
                if (peerSocket == socket) {
                    paired = false;
                    peerSocket = null;
                    peerWriter = null;
                    main.post(() -> {
                        statusText.setText("●  СОЕДИНЕНИЕ ПРЕРВАНО");
                        statusText.setTextColor(MUTED);
                        remoteDeviceText.setText("Не подключён");
                    });
                }
                try { socket.close(); } catch (Exception ignored) {}
            }
        });
    }

    private void handleMessage(String line, String sender) {
        int split = line.indexOf('|');
        if (split <= 0) return;
        String type = line.substring(0, split);
        String payload = line.substring(split + 1);
        if (type.equals("NOTE")) {
            String note = dec(payload);
            main.post(() -> sharedNoteText.setText("От " + sender + ":\n" + note));
        } else if (type.equals("OPEN")) {
            String pkg = dec(payload);
            main.post(() -> receiveLaunchRequest(pkg, sender));
        } else if (type.equals("PING")) {
            sendRaw("PONG|" + enc(deviceName()));
        }
    }

    private void receiveLaunchRequest(String packageName, String sender) {
        if (!allowRemoteLaunch) {
            new AlertDialog.Builder(this)
                    .setTitle("Запрос от " + sender)
                    .setMessage("Второй телефон просит открыть приложение " + packageName +
                            ". Сначала включите разрешение запросов в TwinFold, если хотите принимать их.")
                    .setPositiveButton("Понятно", null)
                    .show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Открыть приложение?")
                .setMessage(sender + " просит запустить приложение:\n" + packageName +
                        "\n\nПриложение откроется только после вашего подтверждения.")
                .setNegativeButton("Отклонить", null)
                .setPositiveButton("Открыть", (d, w) -> launchLocal(packageName))
                .show();
    }

    private void requestRemoteLaunch(String packageName) {
        if (!paired || peerWriter == null) {
            Toast.makeText(this, "Сначала соедините второй телефон.", Toast.LENGTH_SHORT).show();
            return;
        }
        sendRaw("OPEN|" + enc(packageName));
        Toast.makeText(this, "Запрос отправлен. На втором телефоне нужно подтвердить запуск.", Toast.LENGTH_LONG).show();
    }

    private void sendNote() {
        String note = noteInput.getText().toString().trim();
        if (note.isEmpty()) {
            Toast.makeText(this, "Сначала напишите заметку.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!paired) {
            Toast.makeText(this, "Сначала соедините второй телефон.", Toast.LENGTH_SHORT).show();
            return;
        }
        sharedNoteText.setText("Отправлено: " + note);
        sendRaw("NOTE|" + enc(note));
        noteInput.setText("");
    }

    private void sendRaw(String message) {
        PrintWriter writer = peerWriter;
        if (writer == null || !paired) return;
        io.execute(() -> {
            writer.println(message);
            if (writer.checkError()) main.post(() -> Toast.makeText(this, "Ошибка отправки.", Toast.LENGTH_SHORT).show());
        });
    }

    private void disconnectPeer() {
        paired = false;
        PrintWriter writer = peerWriter;
        Socket socket = peerSocket;
        peerWriter = null;
        peerSocket = null;
        if (writer != null) try { writer.close(); } catch (Exception ignored) {}
        if (socket != null) try { socket.close(); } catch (Exception ignored) {}
        ServerSocket server = serverSocket;
        serverSocket = null;
        if (server != null) try { server.close(); } catch (Exception ignored) {}
        if (statusText != null) main.post(() -> {
            statusText.setText("●  НЕ ПОДКЛЮЧЕНО");
            statusText.setTextColor(MUTED);
            remoteDeviceText.setText("Не подключён");
            connectionInfo.setText("Соединение закрыто.");
        });
    }

    private String getLocalIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface network = interfaces.nextElement();
                Enumeration<java.net.InetAddress> addresses = network.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress() && address instanceof Inet4Address) {
                        String value = address.getHostAddress();
                        if (!value.startsWith("169.254.")) return value;
                    }
                }
            }
        } catch (Exception ignored) {}
        return "IP не определён — включите Wi‑Fi";
    }

    private String deviceName() {
        String brand = android.os.Build.MANUFACTURER;
        String model = android.os.Build.MODEL;
        return (brand + " " + model).trim();
    }

    private String enc(String value) {
        return Base64.encodeToString(value.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
    }

    private String dec(String value) {
        try {
            return new String(Base64.decode(value, Base64.DEFAULT), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    @Override
    protected void onDestroy() {
        disconnectPeer();
        io.shutdownNow();
        super.onDestroy();
    }
}
