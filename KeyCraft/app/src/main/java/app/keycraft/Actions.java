package app.keycraft;

public final class Actions {
    private Actions() {}

    public static final String VOLUME = "VOLUME";
    public static final String NONE = "NONE";
    public static final String FLASHLIGHT = "FLASHLIGHT";
    public static final String SCREENSHOT = "SCREENSHOT";
    public static final String CAMERA = "CAMERA";
    public static final String QUICK_SETTINGS = "QUICK_SETTINGS";
    public static final String NOTIFICATIONS = "NOTIFICATIONS";
    public static final String HOME = "HOME";
    public static final String BACK = "BACK";
    public static final String RECENTS = "RECENTS";
    public static final String LOCK = "LOCK";
    public static final String SPLIT = "SPLIT";
    public static final String SETTINGS = "SETTINGS";
    public static final String ASSISTANT = "ASSISTANT";
    public static final String SEARCH = "SEARCH";
    public static final String MEDIA_PLAY = "MEDIA_PLAY";
    public static final String MEDIA_NEXT = "MEDIA_NEXT";
    public static final String MEDIA_PREV = "MEDIA_PREV";
    public static final String OPEN_APP = "OPEN_APP";

    public static final String[] IDS = {
        VOLUME, NONE, FLASHLIGHT, SCREENSHOT, CAMERA, QUICK_SETTINGS,
        NOTIFICATIONS, HOME, BACK, RECENTS, LOCK, SPLIT, SETTINGS,
        ASSISTANT, SEARCH, MEDIA_PLAY, MEDIA_NEXT, MEDIA_PREV, OPEN_APP
    };

    public static final String[] LABELS = {
        "Обычная громкость",
        "Ничего не делать",
        "Включить / выключить фонарик",
        "Сделать скриншот",
        "Открыть камеру",
        "Открыть быстрые настройки",
        "Открыть уведомления",
        "На главный экран",
        "Назад",
        "Последние приложения",
        "Заблокировать экран",
        "Разделить экран",
        "Открыть настройки",
        "Запустить голосового помощника",
        "Открыть поиск",
        "Медиа: воспроизведение / пауза",
        "Медиа: следующий трек",
        "Медиа: предыдущий трек",
        "Открыть выбранное приложение"
    };

    public static String label(String id) {
        for (int i = 0; i < IDS.length; i++) {
            if (IDS[i].equals(id)) return LABELS[i];
        }
        return LABELS[1];
    }
}
