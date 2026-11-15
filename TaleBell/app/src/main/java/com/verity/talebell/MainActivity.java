package com.verity.talebell;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.InputType;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    public static final String EXTRA_ALARM = "story_alarm_started";
    private static final String PREFS = "tale_settings";
    private static final int COLOR_BG = Color.rgb(13, 16, 36);
    private static final int COLOR_CARD = Color.rgb(25, 29, 55);
    private static final int COLOR_ACCENT = Color.rgb(169, 155, 255);
    private static final int COLOR_TEXT = Color.rgb(247, 247, 255);
    private static final int COLOR_MUTED = Color.rgb(177, 183, 211);

    private EditText apiKeyInput;
    private EditText modelInput;
    private EditText ideaInput;
    private Spinner genreSpinner;
    private SeekBar durationBar;
    private TextView durationLabel;
    private TextView statusLabel;
    private TextView storyLabel;
    private TextView alarmLabel;
    private CheckBox dailyCheck;
    private Button createButton;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private boolean creating = false;
    private String pendingSpeech = null;
    private String currentStory = "";
    private int alarmHour = 8;
    private int alarmMinute = 0;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(COLOR_BG);
        getWindow().setNavigationBarColor(COLOR_BG);
        buildInterface();
        loadSettings();
        initSpeech();
        if (getIntent() != null && getIntent().getBooleanExtra(EXTRA_ALARM, false)) startAlarmStory();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.getBooleanExtra(EXTRA_ALARM, false)) startAlarmStory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences(AlarmScheduler.PREFS, MODE_PRIVATE);
        if (prefs.getBoolean("pending", false) && AlarmScheduler.canSchedule(this)) {
            AlarmScheduler.schedule(this, prefs.getInt("hour", alarmHour),
                    prefs.getInt("minute", alarmMinute), prefs.getBoolean("daily", false));
            updateAlarmLabel();
            setStatus("Будильник сохранён.");
        }
    }

    private void buildInterface() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(COLOR_BG);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(22), dp(20), dp(30));
        scroll.addView(page);

        TextView title = text("СКАЗОЧНИК", 29, COLOR_TEXT, true);
        title.setLetterSpacing(0.07f);
        page.addView(title);
        TextView subtitle = text("Придумай историю. Устрой себе аудиосказку.", 14, COLOR_MUTED, false);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
        subParams.topMargin = dp(5);
        page.addView(subtitle, subParams);

        LinearLayout aiCard = card();
        addSectionTitle(aiCard, "Нейросеть");
        addCaption(aiCard, "OpenAI API-ключ");
        apiKeyInput = edit("Вставь свой API-ключ", true);
        aiCard.addView(apiKeyInput, matchWrap());
        addCaption(aiCard, "Модель");
        modelInput = edit("gpt-6-luna", true);
        modelInput.setInputType(InputType.TYPE_CLASS_TEXT);
        aiCard.addView(modelInput, matchWrap());
        Button saveKey = button("Сохранить ключ на телефоне", false);
        aiCard.addView(saveKey, fullButton());
        Button clearKey = button("Удалить сохранённый ключ", false);
        aiCard.addView(clearKey, fullButton());
        TextView safeNote = text("Ключ шифруется и хранится только на этом устройстве. Не отправляй его другим людям.", 12, COLOR_MUTED, false);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(-1, -2);
        noteParams.topMargin = dp(7);
        aiCard.addView(safeNote, noteParams);
        page.addView(aiCard, cardParams());

        LinearLayout storyCard = card();
        addSectionTitle(storyCard, "Новое произведение");
        addCaption(storyCard, "Жанр");
        String[] genres = new String[]{
                "Сказка", "Стихотворение", "Приключение", "Фантастика",
                "Юмор", "Басня", "Детектив без жестокости", "Спокойная история перед сном",
                "Пусть нейросеть выберет жанр"
        };
        genreSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, genres);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genreSpinner.setAdapter(adapter);
        storyCard.addView(genreSpinner, matchWrap());

        addCaption(storyCard, "Тема или идея (необязательно)");
        ideaInput = edit("Например: кот нашёл дверь в облачный город", false);
        ideaInput.setMinLines(2);
        ideaInput.setGravity(Gravity.TOP | Gravity.START);
        storyCard.addView(ideaInput, matchWrap());

        addCaption(storyCard, "Длительность озвучки");
        durationBar = new SeekBar(this);
        durationBar.setMax(9);
        durationBar.setProgress(4);
        if (Build.VERSION.SDK_INT >= 21) {
            durationBar.setProgressTintList(ColorStateList.valueOf(COLOR_ACCENT));
            durationBar.setThumbTintList(ColorStateList.valueOf(COLOR_ACCENT));
        }
        storyCard.addView(durationBar, matchWrap());
        durationLabel = text("5 минут", 14, COLOR_ACCENT, true);
        storyCard.addView(durationLabel);
        durationBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int mins = progress + 1;
                durationLabel.setText(mins + (mins == 1 ? " минута" : mins < 5 ? " минуты" : " минут"));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        createButton = button("Сочинить и озвучить", true);
        LinearLayout.LayoutParams createParams = fullButton();
        createParams.topMargin = dp(17);
        storyCard.addView(createButton, createParams);
        createButton.setOnClickListener(v -> createAndSpeak(false));

        statusLabel = text("Готово к созданию истории.", 13, COLOR_MUTED, false);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(12);
        storyCard.addView(statusLabel, statusParams);
        page.addView(storyCard, cardParams());

        LinearLayout resultCard = card();
        addSectionTitle(resultCard, "Только текст произведения");
        storyLabel = text("Здесь появится готовая сказка или стихотворение.", 15, COLOR_TEXT, false);
        storyLabel.setTextIsSelectable(true);
        resultCard.addView(storyLabel, matchWrap());
        LinearLayout playbackRow = new LinearLayout(this);
        playbackRow.setOrientation(LinearLayout.HORIZONTAL);
        Button replay = button("Повторить", false);
        Button stop = button("Стоп", false);
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, dp(48), 1);
        half.rightMargin = dp(5);
        playbackRow.addView(replay, half);
        LinearLayout.LayoutParams half2 = new LinearLayout.LayoutParams(0, dp(48), 1);
        half2.leftMargin = dp(5);
        playbackRow.addView(stop, half2);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.topMargin = dp(13);
        resultCard.addView(playbackRow, rowParams);
        replay.setOnClickListener(v -> {
            if (!currentStory.isEmpty()) speak(currentStory);
        });
        stop.setOnClickListener(v -> {
            if (tts != null) tts.stop();
            setStatus("Озвучка остановлена.");
        });
        page.addView(resultCard, cardParams());

        LinearLayout alarmCard = card();
        addSectionTitle(alarmCard, "Будильник сказок");
        addCaption(alarmCard, "Выбери время — приложение напомнит и попробует открыть экран сказки автоматически.");
        alarmLabel = text("Будильник не настроен.", 14, COLOR_ACCENT, true);
        alarmCard.addView(alarmLabel, matchWrap());
        Button chooseTime = button("Выбрать время", false);
        alarmCard.addView(chooseTime, fullButton());
        dailyCheck = new CheckBox(this);
        dailyCheck.setText("Повторять каждый день");
        dailyCheck.setTextColor(COLOR_TEXT);
        alarmCard.addView(dailyCheck, matchWrap());
        Button setAlarm = button("Включить будильник", true);
        alarmCard.addView(setAlarm, fullButton());
        Button cancelAlarm = button("Отключить будильник", false);
        alarmCard.addView(cancelAlarm, fullButton());
        Button fullScreenPermission = button("Разрешить полноэкранный запуск", false);
        alarmCard.addView(fullScreenPermission, fullButton());

        chooseTime.setOnClickListener(v -> new TimePickerDialog(this,
                (TimePicker view, int hour, int minute) -> {
                    alarmHour = hour;
                    alarmMinute = minute;
                    updateAlarmLabel();
                }, alarmHour, alarmMinute, true).show());
        setAlarm.setOnClickListener(v -> enableAlarm());
        cancelAlarm.setOnClickListener(v -> {
            AlarmScheduler.cancel(this);
            updateAlarmLabel();
            setStatus("Будильник отключён.");
        });
        fullScreenPermission.setOnClickListener(v -> requestFullScreenPermission());
        page.addView(alarmCard, cardParams());

        TextView footer = text("Озвучка выполняется голосом синтеза речи Android. Для генерации текста нужен интернет и действующий API-ключ.", 12, COLOR_MUTED, false);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(-1, -2);
        footerParams.topMargin = dp(5);
        page.addView(footer, footerParams);

        setContentView(scroll);
        saveKey.setOnClickListener(v -> saveApiKey());
        clearKey.setOnClickListener(v -> {
            SecureKeyStore.clear(this);
            apiKeyInput.setText("");
            setStatus("Сохранённый ключ удалён.");
        });
    }

    private void loadSettings() {
        String storedKey = SecureKeyStore.load(this);
        if (!storedKey.isEmpty()) apiKeyInput.setText(storedKey);
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        modelInput.setText(prefs.getString("model", "gpt-6-luna"));
        ideaInput.setText(prefs.getString("idea", ""));
        genreSpinner.setSelection(Math.max(0, Math.min(genreSpinner.getCount() - 1, prefs.getInt("genre", 0))));
        durationBar.setProgress(Math.max(0, Math.min(9, prefs.getInt("duration", 5) - 1)));
        SharedPreferences alarmPrefs = getSharedPreferences(AlarmScheduler.PREFS, MODE_PRIVATE);
        alarmHour = alarmPrefs.getInt("hour", 8);
        alarmMinute = alarmPrefs.getInt("minute", 0);
        dailyCheck.setChecked(alarmPrefs.getBoolean("daily", false));
        updateAlarmLabel();
    }

    private void saveSettings() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString("model", modelInput.getText().toString().trim())
                .putString("idea", ideaInput.getText().toString())
                .putInt("genre", genreSpinner.getSelectedItemPosition())
                .putInt("duration", durationBar.getProgress() + 1).apply();
    }

    private void saveApiKey() {
        try {
            String key = apiKeyInput.getText().toString().trim();
            SecureKeyStore.save(this, key);
            setStatus(key.isEmpty() ? "Ключ очищен." : "Ключ сохранён в защищённом хранилище телефона.");
        } catch (Exception error) {
            setStatus("Не удалось сохранить ключ: " + error.getMessage());
        }
    }

    private void initSpeech() {
        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) {
                int language = tts.setLanguage(new Locale("ru", "RU"));
                tts.setSpeechRate(0.94f);
                ttsReady = language != TextToSpeech.LANG_MISSING_DATA
                        && language != TextToSpeech.LANG_NOT_SUPPORTED;
                if (!ttsReady) {
                    tts.setLanguage(Locale.getDefault());
                    ttsReady = true;
                    setStatus("Русский голос не найден. Используется голос по умолчанию Android.");
                }
                if (pendingSpeech != null) {
                    String value = pendingSpeech;
                    pendingSpeech = null;
                    speak(value);
                }
            } else {
                setStatus("Не удалось запустить синтез речи Android. Проверь голосовые данные в настройках телефона.");
            }
        });
        if (tts != null) {
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String utteranceId) {
                    runOnUiThread(() -> setStatus("Идёт озвучка произведения…"));
                }
                @Override public void onDone(String utteranceId) {
                    if (utteranceId != null && utteranceId.endsWith("_last")) {
                        runOnUiThread(() -> setStatus("Произведение полностью озвучено."));
                    }
                }
                @Override public void onError(String utteranceId) {
                    runOnUiThread(() -> setStatus("Не удалось озвучить часть текста."));
                }
            });
        }
    }

    private void startAlarmStory() {
        if (apiKeyInput == null) return;
        setStatus("Будильник сработал. Готовлю новое произведение…");
        new android.os.Handler().postDelayed(() -> createAndSpeak(true), 500);
    }

    private void createAndSpeak(boolean fromAlarm) {
        if (creating) return;
        String key = apiKeyInput.getText().toString().trim();
        if (key.isEmpty()) {
            setStatus("Сначала вставь и сохрани API-ключ в разделе «Нейросеть».");
            if (fromAlarm) Toast.makeText(this, "Добавь API-ключ, чтобы будильник мог создать сказку.", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            SecureKeyStore.save(this, key);
        } catch (Exception error) {
            setStatus("Не удалось сохранить ключ на устройстве.");
            return;
        }
        saveSettings();

        String model = modelInput.getText().toString().trim();
        if (model.isEmpty()) model = "gpt-6-luna";
        String genre = String.valueOf(genreSpinner.getSelectedItem());
        String idea = ideaInput.getText().toString();
        int minutes = durationBar.getProgress() + 1;

        creating = true;
        createButton.setEnabled(false);
        createButton.setText("Создаю произведение…");
        setStatus("Нейросеть сочиняет. Это может занять некоторое время.");
        final String selectedModel = model;

        new Thread(() -> {
            try {
                String generated = TaleApi.generate(key, selectedModel, genre, minutes, idea);
                runOnUiThread(() -> {
                    currentStory = generated;
                    storyLabel.setText(generated);
                    creating = false;
                    createButton.setEnabled(true);
                    createButton.setText("Сочинить и озвучить");
                    speak(generated);
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    creating = false;
                    createButton.setEnabled(true);
                    createButton.setText("Сочинить и озвучить");
                    String message = error.getMessage() == null ? "Неизвестная ошибка." : error.getMessage();
                    setStatus("Не получилось создать текст: " + message);
                });
            }
        }, "TaleBell-AI").start();
    }

    private void speak(String text) {
        if (text == null || text.trim().isEmpty()) return;
        if (!ttsReady || tts == null) {
            pendingSpeech = text;
            setStatus("Текст готов. Подготавливаю голос Android…");
            return;
        }
        tts.stop();
        List<String> chunks = splitForSpeech(text, 3000);
        for (int i = 0; i < chunks.size(); i++) {
            String id = i == chunks.size() - 1 ? "talebell_last" : "talebell_" + i;
            tts.speak(chunks.get(i), i == 0 ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD, null, id);
        }
    }

    private List<String> splitForSpeech(String text, int maxChars) {
        List<String> chunks = new ArrayList<>();
        String remaining = text.trim();
        while (remaining.length() > maxChars) {
            int cut = Math.max(remaining.lastIndexOf('.', maxChars),
                    Math.max(remaining.lastIndexOf('!', maxChars),
                            Math.max(remaining.lastIndexOf('?', maxChars), remaining.lastIndexOf('\n', maxChars))));
            if (cut < maxChars / 2) cut = remaining.lastIndexOf(' ', maxChars);
            if (cut <= 0) cut = maxChars;
            chunks.add(remaining.substring(0, Math.min(remaining.length(), cut + 1)).trim());
            remaining = remaining.substring(Math.min(remaining.length(), cut + 1)).trim();
        }
        if (!remaining.isEmpty()) chunks.add(remaining);
        return chunks;
    }

    private void enableAlarm() {
        SharedPreferences prefs = getSharedPreferences(AlarmScheduler.PREFS, MODE_PRIVATE);
        prefs.edit().putInt("hour", alarmHour).putInt("minute", alarmMinute)
                .putBoolean("daily", dailyCheck.isChecked()).putBoolean("pending", true).apply();

        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2101);
        }
        if (!AlarmScheduler.canSchedule(this)) {
            try {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                setStatus("Разреши точные будильники для приложения, затем вернись сюда.");
            } catch (Exception error) {
                setStatus("Открой настройки приложения и разреши точные будильники.");
            }
            return;
        }
        AlarmScheduler.schedule(this, alarmHour, alarmMinute, dailyCheck.isChecked());
        updateAlarmLabel();
        setStatus("Будильник включён.");
        requestFullScreenPermission();
    }

    private void requestFullScreenPermission() {
        if (Build.VERSION.SDK_INT >= 34) {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (!manager.canUseFullScreenIntent()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                            Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                    setStatus("Разреши полноэкранные уведомления, чтобы будильник мог открывать экран сказки.");
                    return;
                } catch (Exception ignored) { }
            }
        }
        setStatus("Полноэкранный запуск разрешён или не требуется на этой версии Android.");
    }

    private void updateAlarmLabel() {
        if (alarmLabel == null) return;
        SharedPreferences prefs = getSharedPreferences(AlarmScheduler.PREFS, MODE_PRIVATE);
        boolean enabled = prefs.getBoolean("enabled", false);
        if (!enabled) {
            alarmLabel.setText(String.format(Locale.getDefault(), "Выбрано: %02d:%02d · будильник выключен", alarmHour, alarmMinute));
        } else {
            String repeat = prefs.getBoolean("daily", false) ? "каждый день" : "один раз";
            alarmLabel.setText(String.format(Locale.getDefault(), "Включён на %02d:%02d · %s", alarmHour, alarmMinute, repeat));
        }
    }

    private void setStatus(String value) {
        if (statusLabel != null) statusLabel.setText(value);
    }

    private LinearLayout card() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable background = new GradientDrawable();
        background.setColor(COLOR_CARD);
        background.setCornerRadius(dp(19));
        box.setBackground(background);
        return box;
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(17);
        return params;
    }

    private void addSectionTitle(LinearLayout parent, String title) {
        TextView label = text(title, 18, COLOR_TEXT, true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(9);
        parent.addView(label, params);
    }

    private void addCaption(LinearLayout parent, String caption) {
        TextView label = text(caption, 13, COLOR_MUTED, false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(9);
        params.bottomMargin = dp(5);
        parent.addView(label, params);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private EditText edit(String hint, boolean singleLine) {
        EditText input = new EditText(this);
        input.setSingleLine(singleLine);
        input.setTextSize(15);
        input.setTextColor(COLOR_TEXT);
        input.setHintTextColor(Color.rgb(125, 132, 163));
        input.setHint(hint);
        input.setPadding(dp(12), dp(10), dp(12), dp(10));
        input.setBackground(roundDrawable(Color.rgb(35, 40, 70), dp(11)));
        if (singleLine) input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        else input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        return input;
    }

    private Button button(String value, boolean primary) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(primary ? Color.rgb(18, 19, 40) : COLOR_TEXT);
        button.setBackground(roundDrawable(primary ? COLOR_ACCENT : Color.rgb(48, 53, 88), dp(13)));
        return button;
    }

    private GradientDrawable roundDrawable(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private LinearLayout.LayoutParams fullButton() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(48));
        params.topMargin = dp(8);
        return params;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        super.onDestroy();
    }
}
