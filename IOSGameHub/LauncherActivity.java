package org.touchhle.android;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LauncherActivity extends Activity {
    private static final int PICK_IPA = 4102;
    private static final int BG = Color.rgb(10, 13, 18);
    private static final int CARD = Color.rgb(23, 29, 38);
    private static final int MUTED = Color.rgb(166, 178, 194);

    private LinearLayout gamesList;
    private TextView status;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildScreen();
        refreshGames();
        handleIncoming(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncoming(intent);
    }

    private File appsDirectory() {
        File root = getExternalFilesDir(null);
        if (root == null) root = getFilesDir();
        File dir = new File(root, "touchHLE_apps");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(20), dp(18), dp(24));
        root.setBackgroundColor(BG);
        scroll.addView(root);

        root.addView(text("VERITY  /  CLASSIC iPHONE GAMES", 11, 0xFF79A9FF, true));

        TextView title = text("iOS GAME HUB", 29, Color.WHITE, true);
        title.setPadding(0, dp(7), 0, dp(4));
        root.addView(title);

        root.addView(text(
            "Импортируй файл .ipa, запускай совместимые старые iPhone-игры и создавай отдельные ярлыки на главном экране Android.",
            14, MUTED, false
        ));

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setPadding(dp(13), dp(13), dp(13), dp(13));
        controls.setBackground(cardBackground());
        LinearLayout.LayoutParams controlParams = new LinearLayout.LayoutParams(-1, -2);
        controlParams.topMargin = dp(16);
        root.addView(controls, controlParams);

        Button importButton = button("＋  ДОБАВИТЬ IPA");
        importButton.setOnClickListener(v -> pickIpa());
        controls.addView(importButton, new LinearLayout.LayoutParams(-1, dp(48)));

        Button pickerButton = button("ОТКРЫТЬ СПИСОК ЭМУЛЯТОРА");
        pickerButton.setOnClickListener(v -> startEmulator(null));
        LinearLayout.LayoutParams pickerParams = new LinearLayout.LayoutParams(-1, dp(44));
        pickerParams.topMargin = dp(9);
        controls.addView(pickerButton, pickerParams);

        TextView compatibility = text(
            "ВАЖНО О СОВМЕСТИМОСТИ\n" +
            "Работает только часть старых 32-битных игр для iPhone OS 2.x/3.x и iOS 4.0.x. " +
            "Файл .ipa должен быть расшифрованным. Современные игры и 64-битные приложения iOS " +
            "не поддерживаются. Это ограничение самого эмулятора, а не телефона.",
            12, 0xFFFFD18A, false
        );
        compatibility.setPadding(dp(12), dp(12), dp(12), dp(12));
        compatibility.setBackground(cardBackground());
        LinearLayout.LayoutParams compatParams = new LinearLayout.LayoutParams(-1, -2);
        compatParams.topMargin = dp(11);
        root.addView(compatibility, compatParams);

        status = text("", 12, MUTED, false);
        status.setPadding(0, dp(13), 0, dp(4));
        root.addView(status);

        TextView section = text("МОИ ИГРЫ", 13, 0xFF79A9FF, true);
        section.setPadding(0, dp(14), 0, dp(8));
        root.addView(section);

        gamesList = new LinearLayout(this);
        gamesList.setOrientation(LinearLayout.VERTICAL);
        root.addView(gamesList);

        TextView footer = text(
            "Эмулятор: touchHLE v0.3.0  •  Совместимость: appdb.touchhle.org",
            10, 0xFF7D899A, false
        );
        footer.setPadding(0, dp(18), 0, 0);
        root.addView(footer);

        setContentView(scroll);

        if (!hasArm64()) {
            status.setText("На устройстве не обнаружена поддержка ARM64. Эта сборка эмулятора требует 64-битную систему Android.");
            status.setTextColor(0xFFFF7777);
        } else {
            status.setText("ARM64 обнаружен • Android " + Build.VERSION.RELEASE +
                " • файлы выбираются через системный проводник");
        }
    }

    private void handleIncoming(Intent intent) {
        if (intent == null) return;
        Uri data = intent.getData();
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && data != null) {
            importUri(data);
        } else if (Intent.ACTION_SEND.equals(intent.getAction())) {
            Uri stream = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (stream != null) importUri(stream);
        }
    }

    private void pickIpa() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivityForResult(intent, PICK_IPA);
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось открыть выбор файлов: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IPA && resultCode == RESULT_OK && data != null && data.getData() != null) {
            importUri(data.getData());
        }
    }

    private void importUri(Uri uri) {
        status.setText("Копирование файла в библиотеку…");
        io.execute(() -> {
            String originalName = displayName(uri);
            if (originalName == null || !originalName.toLowerCase(Locale.ROOT).endsWith(".ipa")) {
                runOnUiThread(() -> {
                    status.setText("Нужен именно файл с расширением .ipa.");
                    Toast.makeText(this, "Выбери файл .ipa", Toast.LENGTH_LONG).show();
                });
                return;
            }

            String safeName = originalName.replaceAll("[^\\p{L}\\p{N}._() -]", "_").trim();
            if (safeName.isEmpty()) safeName = "iPhone-game.ipa";
            File target = uniqueFile(appsDirectory(), safeName);

            try (InputStream input = getContentResolver().openInputStream(uri);
                 FileOutputStream output = new FileOutputStream(target)) {
                if (input == null) throw new IllegalStateException("Проводник не открыл файл");
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
                output.flush();

                runOnUiThread(() -> {
                    status.setText("Добавлено: " + target.getName());
                    Toast.makeText(this, "Игра добавлена", Toast.LENGTH_SHORT).show();
                    refreshGames();
                });
            } catch (Exception e) {
                target.delete();
                runOnUiThread(() -> {
                    status.setText("Ошибка импорта: " + e.getMessage());
                    Toast.makeText(this, "Не удалось импортировать IPA", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private String displayName(Uri uri) {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (column >= 0) return cursor.getString(column);
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        String last = uri.getLastPathSegment();
        return last == null ? null : last;
    }

    private File uniqueFile(File dir, String name) {
        File result = new File(dir, name);
        if (!result.exists()) return result;

        int dot = name.toLowerCase(Locale.ROOT).endsWith(".ipa") ? name.length() - 4 : name.length();
        String base = name.substring(0, dot);
        for (int i = 2; i < 10000; i++) {
            result = new File(dir, base + " (" + i + ").ipa");
            if (!result.exists()) return result;
        }
        return new File(dir, System.currentTimeMillis() + ".ipa");
    }

    private void refreshGames() {
        if (gamesList == null) return;
        gamesList.removeAllViews();

        File[] games = appsDirectory().listFiles((dir, name) ->
            name.toLowerCase(Locale.ROOT).endsWith(".ipa")
        );

        if (games == null || games.length == 0) {
            TextView empty = text(
                "Пока нет игр. Нажми «Добавить IPA», чтобы импортировать файл с телефона.",
                13, MUTED, false
            );
            empty.setPadding(dp(13), dp(16), dp(13), dp(16));
            empty.setBackground(cardBackground());
            gamesList.addView(empty);
            return;
        }

        Arrays.sort(games, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        for (File game : games) addGameCard(game);
    }

    private void addGameCard(File game) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(13), dp(12), dp(13), dp(12));
        card.setBackground(cardBackground());

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(9);
        gamesList.addView(card, params);

        card.addView(text(game.getName(), 15, Color.WHITE, true));

        TextView size = text(formatSize(game.length()), 11, MUTED, false);
        size.setPadding(0, dp(3), 0, dp(10));
        card.addView(size);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        Button play = button("ИГРАТЬ");
        play.setOnClickListener(v -> startEmulator(game));
        row.addView(play, new LinearLayout.LayoutParams(0, dp(42), 1));

        Button pin = button("НА ГЛАВНЫЙ ЭКРАН");
        pin.setOnClickListener(v -> pinShortcut(game));
        LinearLayout.LayoutParams pinParams = new LinearLayout.LayoutParams(0, dp(42), 1);
        pinParams.leftMargin = dp(8);
        row.addView(pin, pinParams);

        card.addView(row);
    }

    private void startEmulator(File game) {
        if (!hasArm64()) {
            Toast.makeText(this, "Для эмулятора нужна 64-битная ARM64-система Android.", Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(this, MainActivity.class);
        if (game != null) intent.putExtra("ipa_path", game.getAbsolutePath());

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось запустить эмулятор: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void pinShortcut(File game) {
        if (Build.VERSION.SDK_INT < 26) {
            Toast.makeText(this, "Ярлыки поддерживаются на Android 8 и новее.", Toast.LENGTH_LONG).show();
            return;
        }

        ShortcutManager manager = getSystemService(ShortcutManager.class);
        if (manager == null || !manager.isRequestPinShortcutSupported()) {
            Toast.makeText(this, "Этот лаунчер не разрешает закреплять ярлыки.", Toast.LENGTH_LONG).show();
            return;
        }

        String id = "ipa_" + Integer.toHexString(game.getName().hashCode()) + "_" + game.length();
        String label = game.getName();
        if (label.toLowerCase(Locale.ROOT).endsWith(".ipa")) {
            label = label.substring(0, label.length() - 4);
        }
        if (label.length() > 24) label = label.substring(0, 21) + "…";

        Intent launch = new Intent(this, MainActivity.class);
        launch.setAction("app.verity.iosgamehub.RUN_" + id);
        launch.putExtra("ipa_path", game.getAbsolutePath());
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        ShortcutInfo shortcut = new ShortcutInfo.Builder(this, id)
            .setShortLabel(label)
            .setLongLabel("Запустить " + label + " через iOS Game Hub")
            .setIcon(Icon.createWithResource(this, R.drawable.ic_game_hub))
            .setIntent(launch)
            .build();

        try {
            manager.requestPinShortcut(shortcut, null);
            Toast.makeText(this, "Запрос ярлыка отправлен на главный экран.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось создать ярлык: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean hasArm64() {
        for (String abi : Build.SUPPORTED_ABIS) {
            if ("arm64-v8a".equals(abi)) return true;
        }
        return false;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024L) return bytes + " B";
        if (bytes < 1024L * 1024L) return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(11);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(buttonBackground());
        b.setPadding(dp(7), 0, dp(7), 0);
        return b;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private GradientDrawable cardBackground() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(CARD);
        d.setCornerRadius(dp(15));
        d.setStroke(dp(1), 0xFF354252);
        return d;
    }

    private GradientDrawable buttonBackground() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(0xFF2466C9);
        d.setCornerRadius(dp(11));
        return d;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdown();
    }
}
