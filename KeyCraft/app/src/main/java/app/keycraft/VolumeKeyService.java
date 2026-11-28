package app.keycraft;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

public class VolumeKeyService extends AccessibilityService {
    public static final String ACTION_REFRESH = "app.keycraft.REFRESH_BUTTON_MAPPINGS";
    private static final String PREFS = "keycraft_settings";
    private static final long DOUBLE_TAP_MS = 330L;
    private static final long LONG_PRESS_MS = 560L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final PressState volumeUp = new PressState();
    private final PressState volumeDown = new PressState();

    private AudioManager audioManager;
    private CameraManager cameraManager;
    private String torchCameraId;
    private boolean torchOn;
    private BroadcastReceiver refreshReceiver;
    private boolean receiverRegistered;

    private static class PressState {
        boolean pressed;
        boolean longTriggered;
        boolean secondTap;
        Runnable pendingClick;
        Runnable longPress;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        refreshReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                refreshKeyFilter();
            }
        };
        IntentFilter filter = new IntentFilter(ACTION_REFRESH);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(refreshReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(refreshReceiver, filter);
        }
        receiverRegistered = true;
        refreshKeyFilter();
    }

    private void refreshKeyFilter() {
        if (getServiceInfo() == null) return;
        AccessibilityServiceInfo info = getServiceInfo();
        info.flags = hasCustomMapping(KeyEvent.KEYCODE_VOLUME_UP)
            || hasCustomMapping(KeyEvent.KEYCODE_VOLUME_DOWN)
            ? AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS : 0;
        setServiceInfo(info);
    }

    private boolean hasCustomMapping(int keyCode) {
        String prefix = keyCode == KeyEvent.KEYCODE_VOLUME_UP ? "up_" : "down_";
        return isCustom(readAction(prefix + "click"))
            || isCustom(readAction(prefix + "double"))
            || isCustom(readAction(prefix + "long"));
    }

    private boolean isCustom(String action) {
        return !Actions.VOLUME.equals(action) && !Actions.NONE.equals(action);
    }

    private String readAction(String key) {
        return getSharedPreferences(PREFS, MODE_PRIVATE)
            .getString(key, key.endsWith("_click") ? Actions.VOLUME : Actions.NONE);
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();
        if (code != KeyEvent.KEYCODE_VOLUME_UP && code != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return false;
        }

        if (!hasCustomMapping(code)) return false;

        PressState state = code == KeyEvent.KEYCODE_VOLUME_UP ? volumeUp : volumeDown;
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.getRepeatCount() == 0) onButtonDown(state, code);
            return true;
        }
        if (event.getAction() == KeyEvent.ACTION_UP) {
            onButtonUp(state, code);
            return true;
        }
        return true;
    }

    private void onButtonDown(PressState state, int code) {
        if (state.pendingClick != null) {
            handler.removeCallbacks(state.pendingClick);
            state.pendingClick = null;
            state.secondTap = true;
        } else {
            state.secondTap = false;
        }

        state.pressed = true;
        state.longTriggered = false;
        if (state.longPress != null) handler.removeCallbacks(state.longPress);
        state.longPress = () -> {
            if (!state.pressed) return;
            state.longTriggered = true;
            if (state.pendingClick != null) {
                handler.removeCallbacks(state.pendingClick);
                state.pendingClick = null;
            }
            state.secondTap = false;
            performMappedAction(code, "long");
        };
        handler.postDelayed(state.longPress, LONG_PRESS_MS);
    }

    private void onButtonUp(PressState state, int code) {
        state.pressed = false;
        if (state.longPress != null) {
            handler.removeCallbacks(state.longPress);
            state.longPress = null;
        }

        if (state.longTriggered) {
            state.longTriggered = false;
            state.secondTap = false;
            return;
        }

        if (state.secondTap) {
            state.secondTap = false;
            String doubleAction = readAction(prefix(code) + "double");
            if (Actions.NONE.equals(doubleAction)) {
                performMappedAction(code, "click");
                performMappedAction(code, "click");
            } else {
                performAction(doubleAction, code);
            }
            return;
        }

        Runnable click = () -> {
            state.pendingClick = null;
            performMappedAction(code, "click");
        };
        state.pendingClick = click;
        handler.postDelayed(click, DOUBLE_TAP_MS);
    }

    private String prefix(int code) {
        return code == KeyEvent.KEYCODE_VOLUME_UP ? "up_" : "down_";
    }

    private void performMappedAction(int code, String gesture) {
        performAction(readAction(prefix(code) + gesture), code);
    }

    private void performAction(String action, int keyCode) {
        if (action == null || Actions.NONE.equals(action)) return;

        if (Actions.VOLUME.equals(action)) {
            if (audioManager != null) {
                int direction = keyCode == KeyEvent.KEYCODE_VOLUME_UP
                    ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER;
                audioManager.adjustVolume(direction, AudioManager.FLAG_SHOW_UI);
            }
            return;
        }

        switch (action) {
            case Actions.FLASHLIGHT:
                toggleFlashlight();
                break;
            case Actions.SCREENSHOT:
                if (Build.VERSION.SDK_INT >= 28) {
                    reportGlobalAction(performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT),
                        "Не удалось сделать скриншот");
                } else {
                    toast("Скриншот этим действием поддерживается на Android 9 и новее");
                }
                break;
            case Actions.CAMERA:
                launchCamera();
                break;
            case Actions.QUICK_SETTINGS:
                reportGlobalAction(performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS),
                    "Быстрые настройки недоступны");
                break;
            case Actions.NOTIFICATIONS:
                reportGlobalAction(performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS),
                    "Панель уведомлений недоступна");
                break;
            case Actions.HOME:
                performGlobalAction(GLOBAL_ACTION_HOME);
                break;
            case Actions.BACK:
                performGlobalAction(GLOBAL_ACTION_BACK);
                break;
            case Actions.RECENTS:
                performGlobalAction(GLOBAL_ACTION_RECENTS);
                break;
            case Actions.LOCK:
                if (Build.VERSION.SDK_INT >= 28) {
                    reportGlobalAction(performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN),
                        "Блокировка экрана недоступна");
                } else {
                    toast("Блокировка экрана требует Android 9 или новее");
                }
                break;
            case Actions.SPLIT:
                reportGlobalAction(performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN),
                    "Разделение экрана не поддерживается текущим режимом");
                break;
            case Actions.SETTINGS:
                launchIntent(new Intent(android.provider.Settings.ACTION_SETTINGS));
                break;
            case Actions.ASSISTANT:
                launchAssistant();
                break;
            case Actions.SEARCH:
                launchSearch();
                break;
            case Actions.MEDIA_PLAY:
                dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                break;
            case Actions.MEDIA_NEXT:
                dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT);
                break;
            case Actions.MEDIA_PREV:
                dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                break;
            case Actions.OPEN_APP:
                launchChosenApp();
                break;
            default:
                toast("Неизвестное действие");
                break;
        }
    }

    private void toggleFlashlight() {
        if (Build.VERSION.SDK_INT >= 23 &&
            checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            toast("Сначала разреши KeyCraft доступ к камере для фонарика");
            Intent open = new Intent(this, MainActivity.class);
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            open.putExtra(MainActivity.EXTRA_REQUEST_CAMERA, true);
            startActivity(open);
            return;
        }
        if (cameraManager == null) {
            toast("Фонарик недоступен на этом устройстве");
            return;
        }
        try {
            if (torchCameraId == null) {
                for (String id : cameraManager.getCameraIdList()) {
                    CameraCharacteristics c = cameraManager.getCameraCharacteristics(id);
                    Boolean flash = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    if (Boolean.TRUE.equals(flash)) {
                        torchCameraId = id;
                        break;
                    }
                }
            }
            if (torchCameraId == null) {
                toast("На камере не найден фонарик");
                return;
            }
            torchOn = !torchOn;
            cameraManager.setTorchMode(torchCameraId, torchOn);
        } catch (CameraAccessException | IllegalArgumentException e) {
            torchOn = false;
            toast("Не удалось переключить фонарик");
        } catch (SecurityException e) {
            toast("Нет разрешения на использование фонарика");
        }
    }

    private void launchCamera() {
        Intent camera = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
        camera.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (!launchIntent(camera)) {
            Intent fallback = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            launchIntent(fallback);
        }
    }

    private void launchAssistant() {
        Intent assist = new Intent(Intent.ACTION_ASSIST);
        assist.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (!launchIntent(assist)) {
            Intent voice = new Intent(Intent.ACTION_VOICE_COMMAND);
            launchIntent(voice);
        }
    }

    private void launchSearch() {
        Intent search = new Intent(Intent.ACTION_WEB_SEARCH);
        search.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (!launchIntent(search)) {
            Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"));
            launchIntent(browser);
        }
    }

    private void launchChosenApp() {
        String packageName = getSharedPreferences(PREFS, MODE_PRIVATE)
            .getString("target_package", "");
        if (packageName == null || packageName.isEmpty()) {
            toast("Сначала выбери приложение в KeyCraft");
            return;
        }
        PackageManager pm = getPackageManager();
        Intent launch = pm.getLaunchIntentForPackage(packageName);
        if (launch == null) {
            toast("Выбранное приложение больше не установлено");
            return;
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        launchIntent(launch);
    }

    private boolean launchIntent(Intent intent) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (intent.resolveActivity(getPackageManager()) == null) return false;
            startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void dispatchMediaKey(int keyCode) {
        if (audioManager == null) return;
        try {
            audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
            audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
        } catch (Exception e) {
            toast("Медиаплеер не принял команду");
        }
    }

    private void reportGlobalAction(boolean success, String failure) {
        if (!success) toast(failure);
    }

    private void toast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Intentionally unused: KeyCraft does not inspect screen content.
    }

    @Override
    public void onInterrupt() {
        // Nothing to stop.
    }

    @Override
    public void onDestroy() {
        if (receiverRegistered && refreshReceiver != null) {
            try {
                unregisterReceiver(refreshReceiver);
            } catch (Exception ignored) {
            }
        }
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
