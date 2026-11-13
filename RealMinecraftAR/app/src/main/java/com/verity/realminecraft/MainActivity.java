package com.verity.realminecraft;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("deprecation")
public class MainActivity extends Activity implements SurfaceHolder.Callback {
    private static final int CAMERA_REQUEST = 73;

    private SurfaceView cameraSurface;
    private SurfaceHolder cameraHolder;
    private WorldView worldView;
    private Camera camera;
    private boolean surfaceReady = false;
    private boolean cameraGranted = false;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(0xff101913);
        getWindow().setNavigationBarColor(0xff101913);

        FrameLayout root = new FrameLayout(this);
        cameraSurface = new SurfaceView(this);
        cameraSurface.setZOrderMediaOverlay(false);
        cameraHolder = cameraSurface.getHolder();
        cameraHolder.addCallback(this);
        root.addView(cameraSurface, new FrameLayout.LayoutParams(-1, -1));

        worldView = new WorldView(this);
        worldView.setCallbacks(new WorldView.Callbacks() {
            @Override public void editFriendName() { showFriendNameDialog(); }
            @Override public void clearFriends() {
                worldView.clearFriends();
                Toast.makeText(MainActivity.this, "Метки удалены", Toast.LENGTH_SHORT).show();
            }
            @Override public void retryCamera() {
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    cameraGranted = true;
                    worldView.setCameraState(true, "");
                    if (surfaceReady) startCamera();
                } else {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);
                }
            }
        });
        root.addView(worldView, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        cameraGranted = checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        worldView.setCameraState(cameraGranted, cameraGranted ? "" : "Для живого изображения нужна только камера");
        if (!cameraGranted) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);
        } else if (surfaceReady) {
            startCamera();
        }
    }

    private void showFriendNameDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Например: Alex");
        input.setText(worldView.getFriendName());
        new AlertDialog.Builder(this)
            .setTitle("Имя над меткой")
            .setMessage("После сохранения коснись места в кадре — появится табличка с именем.")
            .setView(input)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Сохранить", (dialog, which) -> {
                String value = input.getText().toString().trim();
                if (value.isEmpty()) value = "Друг";
                worldView.setFriendName(value);
                Toast.makeText(this, "Теперь коснись места для метки", Toast.LENGTH_SHORT).show();
            }).show();
    }

    @Override public void surfaceCreated(SurfaceHolder holder) {
        surfaceReady = true;
        if (cameraGranted) startCamera();
    }

    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (camera != null) {
            try {
                camera.stopPreview();
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (Exception error) {
                releaseCamera();
                worldView.setCameraState(false, "Камера перезапускается — нажми на значок камеры");
            }
        }
    }

    @Override public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceReady = false;
        releaseCamera();
    }

    private void startCamera() {
        if (!surfaceReady || camera != null || !cameraGranted) return;
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraGranted = false;
            worldView.setCameraState(false, "Разреши только доступ к камере");
            return;
        }
        try {
            int backCamera = 0;
            Camera.CameraInfo info = new Camera.CameraInfo();
            for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                Camera.getCameraInfo(i, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    backCamera = i;
                    break;
                }
            }
            camera = Camera.open(backCamera);
            Camera.Parameters params = camera.getParameters();

            List<Camera.Size> sizes = params.getSupportedPreviewSizes();
            if (sizes != null && !sizes.isEmpty()) {
                float target = (float) getResources().getDisplayMetrics().heightPixels /
                        Math.max(1, getResources().getDisplayMetrics().widthPixels);
                Camera.Size best = sizes.get(0);
                float bestScore = Float.MAX_VALUE;
                for (Camera.Size size : sizes) {
                    float ratio = (float) size.width / Math.max(1, size.height);
                    float score = Math.abs(ratio - target) + (size.width < 960 ? 0.12f : 0f);
                    if (score < bestScore) {
                        bestScore = score;
                        best = size;
                    }
                }
                params.setPreviewSize(best.width, best.height);
                float portraitHorizontal = params.getVerticalViewAngle();
                float portraitVertical = params.getHorizontalViewAngle();
                worldView.setCameraFieldOfView(portraitHorizontal, portraitVertical);
            }

            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes != null) {
                if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
            }
            camera.setParameters(params);
            camera.setDisplayOrientation(90);
            camera.setPreviewDisplay(cameraHolder);
            camera.startPreview();
            cameraGranted = true;
            worldView.setCameraState(true, "");
        } catch (Exception error) {
            releaseCamera();
            worldView.setCameraState(false, "Не удалось открыть камеру. Нажми здесь, чтобы повторить");
            Toast.makeText(this, "Камера занята или недоступна. Закрой другие приложения с камерой и повтори.", Toast.LENGTH_LONG).show();
        }
    }

    private void releaseCamera() {
        if (camera != null) {
            try { camera.stopPreview(); } catch (Exception ignored) {}
            try { camera.setPreviewCallback(null); } catch (Exception ignored) {}
            try { camera.release(); } catch (Exception ignored) {}
            camera = null;
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == CAMERA_REQUEST) {
            cameraGranted = results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED;
            if (cameraGranted) {
                worldView.setCameraState(true, "");
                if (surfaceReady) startCamera();
            } else {
                worldView.setCameraState(false, "Камера отключена. Нажми сюда, чтобы попробовать снова");
                Toast.makeText(this, "Без разрешения камеры приложение не может показать комнату.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (worldView != null) worldView.startSensors();
        if (surfaceReady && cameraGranted &&
                checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        }
    }

    @Override protected void onPause() {
        releaseCamera();
        if (worldView != null) worldView.stopSensors();
        super.onPause();
    }

    @Override protected void onDestroy() {
        releaseCamera();
        super.onDestroy();
    }

    static final class WorldView extends View implements SensorEventListener {
        interface Callbacks {
            void editFriendName();
            void clearFriends();
            void retryCamera();
        }

        private static final String[] BLOCKS = {
            "Трава","Земля","Камень","Булыжник","Доски","Дуб","Листва","Песок",
            "Песчаник","Гравий","Кирпичи","Стекло","Белая шерсть","Красная шерсть",
            "Синяя шерсть","Зелёная шерсть","Чёрная шерсть","Жёлтая шерсть","Обсидиан",
            "Бедрок","Алмазный","Золотой","Железный","Изумрудный","Редстоун",
            "Угольный","Светокамень","Морской фонарь","ТНТ","Книжная полка",
            "Верстак","Печь","Сундук","Адский камень","Незеритовый кирпич",
            "Песок душ","Лёд","Снег","Глина","Мох"
        };
        private static final int[] BLOCK_COLORS = {
            0xff65a94c,0xff98613d,0xff858585,0xff777777,0xffb98a50,0xff8a6038,0xff477b36,
            0xffe4d49a,0xffd9c28d,0xffa49e91,0xffa34d40,0xffa6dfe0,0xffe9e9e9,0xffc84343,
            0xff466cc3,0xff4d9149,0xff303039,0xffe5cc43,0xff29213b,0xff4b4b51,0xff43c6d4,
            0xffe4b83d,0xffaeb4b9,0xff38ad68,0xffa93636,0xff373737,0xffe6be54,0xffa7dfe3,
            0xffd74335,0xff895f3f,0xffa27b4c,0xff797777,0xff986d3e,0xff6b3533,0xff68504b,
            0xff80bfe8,0xffe9f2f6,0xffaaa5a0,0xffb6b8aa,0xff638a50
        };
        private static final String[] MOBS = {
            "Зомби","Скелет","Крипер","Эндермен","Паук",
            "Слизень","Свинья","Корова","Овца","Курица"
        };
        private static final int[] MOB_COLORS = {
            0xff4c9a55,0xffd9d5c8,0xff68b94d,0xff29242f,0xff514039,
            0xff70ca58,0xffe9a0aa,0xff8d6b4b,0xffe9e5d9,0xfff0eee5
        };
        private static final float CAMERA_HEIGHT = 1.45f;
        private static final float BLOCK_METERS = 0.42f;
        private static final float STEP_METERS = 0.68f;
        private static final float MAX_DISTANCE = 18f;

        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final List<WorldBlock> blocks = new ArrayList<>();
        private final List<WorldMob> mobs = new ArrayList<>();
        private final List<FriendTag> friends = new ArrayList<>();
        private final SensorManager sensorManager;
        private final Sensor rotationSensor;
        private final Sensor stepDetector;
        private final Sensor stepCounter;
        private final SharedPreferences prefs;
        private final float density;
        private final float[] rotationMatrix = new float[9];
        private final float[] remappedMatrix = new float[9];
        private final float[] accelerometerValues = new float[3];
        private final float[] magneticValues = new float[3];
        private boolean hasAccelerometer = false;
        private boolean hasMagnetic = false;
        private boolean sensorRegistered = false;
        private long lastStepCounter = -1;
        private long lastStepSave = 0;
        private long lastFrame = 0;
        private long lastHunger = System.currentTimeMillis();

        private Callbacks callbacks;
        private boolean cameraReady = false;
        private String cameraMessage = "";
        private int mode = 0;
        private int selectedBlock = 0;
        private int selectedMob = 0;
        private int selectedSlot = 0;
        private int catalogScroll = 0;
        private final int[] blockSlots = {0,1,2,3,4,5,6,7,8};
        private final int[] mobSlots = {0,1,2,3,4,5,6,7,8};
        private int distanceMeters = 3;
        private int health = 10;
        private int hunger = 10;
        private int xp = 0;
        private float playerX = 0f;
        private float playerZ = 0f;
        private float yaw = 0f;
        private float pitch = 0f;
        private float horizontalFov = 55f;
        private float verticalFov = 65f;
        private String friendName = "Друг";
        private float touchDownX, touchDownY;
        private long touchDownAt;
        private float focalX = 450f, focalY = 450f;
        private float cameraCenterY = 400f;

        WorldView(Context context) {
            super(context);
            density = getResources().getDisplayMetrics().density;
            sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
            stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            prefs = context.getSharedPreferences("blockworld_v3_world", Context.MODE_PRIVATE);
            p.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD));
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
            loadWorld();
        }

        void setCallbacks(Callbacks value) { callbacks = value; }
        void setCameraState(boolean ready, String message) {
            cameraReady = ready;
            cameraMessage = message == null ? "" : message;
            invalidate();
        }
        void setCameraFieldOfView(float h, float v) {
            if (h > 10f && h < 150f) horizontalFov = h;
            if (v > 10f && v < 150f) verticalFov = v;
            invalidate();
        }
        String getFriendName() { return friendName; }
        void setFriendName(String value) { friendName = value; saveWorld(); invalidate(); }
        void clearFriends() { friends.clear(); saveWorld(); invalidate(); }

        void startSensors() {
            if (sensorRegistered || sensorManager == null) return;
            if (rotationSensor != null) {
                sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
            } else {
                Sensor a = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                Sensor m = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
                if (a != null) sensorManager.registerListener(this, a, SensorManager.SENSOR_DELAY_GAME);
                if (m != null) sensorManager.registerListener(this, m, SensorManager.SENSOR_DELAY_GAME);
            }
            if (stepDetector != null) {
                sensorManager.registerListener(this, stepDetector, SensorManager.SENSOR_DELAY_NORMAL);
            } else if (stepCounter != null) {
                sensorManager.registerListener(this, stepCounter, SensorManager.SENSOR_DELAY_NORMAL);
            }
            sensorRegistered = true;
        }

        void stopSensors() {
            if (sensorManager != null && sensorRegistered) sensorManager.unregisterListener(this);
            sensorRegistered = false;
            saveWorld();
        }

        @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

        @Override public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                updateAnglesFromMatrix(rotationMatrix);
            } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                System.arraycopy(event.values, 0, accelerometerValues, 0, 3);
                hasAccelerometer = true;
                updateFallbackAngles();
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(event.values, 0, magneticValues, 0, 3);
                hasMagnetic = true;
                updateFallbackAngles();
            } else if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
                if (event.values.length > 0 && event.values[0] > 0f) movePlayerOneStep();
            } else if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
                long count = event.values.length > 0 ? (long) event.values[0] : 0L;
                if (lastStepCounter < 0) lastStepCounter = count;
                else {
                    long steps = Math.max(0, Math.min(20, count - lastStepCounter));
                    lastStepCounter = count;
                    for (int i = 0; i < steps; i++) movePlayerOneStep();
                }
            }
        }

        private void updateFallbackAngles() {
            if (!hasAccelerometer || !hasMagnetic) return;
            if (SensorManager.getRotationMatrix(rotationMatrix, null,
                    accelerometerValues, magneticValues)) {
                updateAnglesFromMatrix(rotationMatrix);
            }
        }

        private void updateAnglesFromMatrix(float[] matrix) {
            // The rear camera looks along the phone's -Z axis.
            float forwardEast = -matrix[2];
            float forwardNorth = -matrix[5];
            float forwardUp = -matrix[8];
            float horizontal = (float) Math.sqrt(forwardEast * forwardEast + forwardNorth * forwardNorth);
            float newYaw = (float) Math.atan2(forwardEast, forwardNorth);
            float newPitch = (float) Math.atan2(forwardUp, Math.max(0.001f, horizontal));
            yaw = smoothAngle(yaw, newYaw, 0.22f);
            pitch = pitch * 0.78f + newPitch * 0.22f;
            invalidate();
        }

        private float smoothAngle(float current, float target, float factor) {
            float delta = normalizeAngle(target - current);
            return normalizeAngle(current + delta * factor);
        }

        private float normalizeAngle(float angle) {
            while (angle > Math.PI) angle -= (float) (Math.PI * 2.0);
            while (angle < -Math.PI) angle += (float) (Math.PI * 2.0);
            return angle;
        }

        private void movePlayerOneStep() {
            playerX += (float) Math.sin(yaw) * STEP_METERS;
            playerZ += (float) Math.cos(yaw) * STEP_METERS;
            long now = System.currentTimeMillis();
            if (now - lastStepSave > 1800) {
                lastStepSave = now;
                saveWorld();
            }
            invalidate();
        }

        private void loadWorld() {
            playerX = prefs.getFloat("player_x", 0f);
            playerZ = prefs.getFloat("player_z", 0f);
            friendName = prefs.getString("friend_name", "Друг");
            try {
                JSONArray savedBlocks = new JSONArray(prefs.getString("blocks", "[]"));
                for (int i = 0; i < savedBlocks.length(); i++) {
                    JSONObject o = savedBlocks.getJSONObject(i);
                    blocks.add(new WorldBlock(o.getInt("t"), (float)o.getDouble("x"),
                            (float)o.getDouble("y"), (float)o.getDouble("z")));
                }
                JSONArray savedMobs = new JSONArray(prefs.getString("mobs", "[]"));
                for (int i = 0; i < savedMobs.length(); i++) {
                    JSONObject o = savedMobs.getJSONObject(i);
                    WorldMob m = new WorldMob(o.getInt("t"), (float)o.getDouble("x"),
                            (float)o.getDouble("z"));
                    m.hp = o.optInt("hp", 5);
                    mobs.add(m);
                }
                JSONArray savedFriends = new JSONArray(prefs.getString("friends", "[]"));
                for (int i = 0; i < savedFriends.length(); i++) {
                    JSONObject o = savedFriends.getJSONObject(i);
                    friends.add(new FriendTag(o.optString("n", "Друг"),
                            (float)o.getDouble("x"), (float)o.getDouble("y"),
                            (float)o.getDouble("z")));
                }
                xp = prefs.getInt("xp", 0);
            } catch (Exception ignored) {
                blocks.clear();
                mobs.clear();
                friends.clear();
            }
        }

        private void saveWorld() {
            try {
                JSONArray savedBlocks = new JSONArray();
                for (WorldBlock b : blocks) {
                    JSONObject o = new JSONObject();
                    o.put("t", b.type); o.put("x", b.x); o.put("y", b.y); o.put("z", b.z);
                    savedBlocks.put(o);
                }
                JSONArray savedMobs = new JSONArray();
                for (WorldMob m : mobs) {
                    JSONObject o = new JSONObject();
                    o.put("t", m.type); o.put("x", m.x); o.put("z", m.z); o.put("hp", m.hp);
                    savedMobs.put(o);
                }
                JSONArray savedFriends = new JSONArray();
                for (FriendTag f : friends) {
                    JSONObject o = new JSONObject();
                    o.put("n", f.name); o.put("x", f.x); o.put("y", f.y); o.put("z", f.z);
                    savedFriends.put(o);
                }
                prefs.edit()
                    .putFloat("player_x", playerX)
                    .putFloat("player_z", playerZ)
                    .putString("friend_name", friendName)
                    .putString("blocks", savedBlocks.toString())
                    .putString("mobs", savedMobs.toString())
                    .putString("friends", savedFriends.toString())
                    .putInt("xp", xp)
                    .apply();
            } catch (Exception ignored) {}
        }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float w = getWidth();
            float h = getHeight();
            focalX = w / (2f * (float)Math.tan(Math.toRadians(horizontalFov * 0.5f)));
            focalY = h / (2f * (float)Math.tan(Math.toRadians(verticalFov * 0.5f)));
            cameraCenterY = h * 0.49f;

            updateGame();
            drawWorld(c, w, h);
            drawHud(c, w, h);
            drawBottomPanel(c, w, h);

            if (!cameraReady) {
                drawLabel(c, "КАМЕРА НЕ ВКЛЮЧЕНА", w * 0.5f, h * 0.37f,
                        13*density, Color.WHITE, 0xdd7d3026);
                drawLabel(c, cameraMessage.isEmpty() ? "Коснись экрана, чтобы попробовать снова" : cameraMessage,
                        w * 0.5f, h * 0.37f + 35*density, 8.5f*density,
                        Color.WHITE, 0xdd17231b);
            }
            if (health <= 0) {
                drawLabel(c, "ИГРОК ВЫБЫЛ — КОСНИСЬ, ЧТОБЫ ВОССТАНОВИТЬСЯ",
                        w*0.5f, h*0.44f, 9*density, Color.WHITE, 0xdd8a2727);
            }
            postInvalidateDelayed(33);
        }

        private void updateGame() {
            long now = System.currentTimeMillis();
            float dt = lastFrame == 0 ? 0.033f : Math.min(0.10f, (now - lastFrame) / 1000f);
            lastFrame = now;
            if (now - lastHunger > 65000L && hunger > 0) {
                hunger--;
                lastHunger = now;
            }
            if (mobs.isEmpty() || health <= 0) return;
            for (WorldMob mob : mobs) {
                float dx = playerX - mob.x;
                float dz = playerZ - mob.z;
                float distance = (float)Math.sqrt(dx*dx + dz*dz);
                if (distance > 0.01f && distance > 0.72f) {
                    float speed = mob.type == 3 ? 0.72f : (mob.type == 4 ? 0.38f : 0.52f);
                    mob.x += dx / distance * speed * dt;
                    mob.z += dz / distance * speed * dt;
                } else if (distance <= 1.15f && mob.type <= 5 && now - mob.lastAttack > 2300L) {
                    mob.lastAttack = now;
                    health = Math.max(0, health - 1);
                    setStatus("⚔ " + MOBS[mob.type] + " рядом: −1 сердце");
                }
            }
        }

        private void drawWorld(Canvas c, float w, float h) {
            List<WorldBlock> orderedBlocks = new ArrayList<>(blocks);
            Collections.sort(orderedBlocks, (a,b) -> Float.compare(distanceToPlayer(b.x,b.z), distanceToPlayer(a.x,a.z)));
            for (WorldBlock block : orderedBlocks) {
                ScreenPoint point = project(block.x, block.y + BLOCK_METERS*0.5f, block.z, w);
                if (!point.visible || point.distance > MAX_DISTANCE) continue;
                float size = clamp(BLOCK_METERS * focalX / Math.max(0.35f, point.distance), 15*density, 130*density);
                block.lastScreenX = point.x;
                block.lastScreenY = point.y;
                block.lastScreenSize = size;
                drawIsoCube(c, point.x, point.y, size, BLOCK_COLORS[block.type]);
                if (size > 35*density) {
                    drawText(c, BLOCKS[block.type], point.x, point.y + size*0.78f,
                            7.5f*density, Color.WHITE, true);
                }
            }

            List<WorldMob> orderedMobs = new ArrayList<>(mobs);
            Collections.sort(orderedMobs, (a,b) -> Float.compare(distanceToPlayer(b.x,b.z), distanceToPlayer(a.x,a.z)));
            for (WorldMob mob : orderedMobs) {
                ScreenPoint base = project(mob.x, 0f, mob.z, w);
                if (!base.visible || base.distance > MAX_DISTANCE) continue;
                float ppm = clamp(focalY / Math.max(0.50f, base.distance), 22*density, 430*density);
                float mobHeight = mob.type == 3 ? 2.75f : (mob.type == 4 ? 0.72f : (mob.type == 5 ? 0.72f : (mob.type == 9 ? 0.82f : 1.82f)));
                float pxHeight = mobHeight * ppm;
                if (base.y < -pxHeight || base.y > h + 80*density) continue;
                mob.lastScreenX = base.x;
                mob.lastScreenBaseY = base.y;
                mob.lastScreenHeight = pxHeight;
                mob.lastScreenWidth = (mob.type == 4 ? 1.1f : 0.72f) * ppm;
                drawMob3D(c, mob, base.x, base.y, ppm);
                drawLabel(c, MOBS[mob.type] + "  " + mob.hp + "/5",
                        base.x, base.y - pxHeight - 14*density, 8*density,
                        Color.WHITE, 0xbb111811);
            }

            for (FriendTag friend : friends) {
                ScreenPoint point = project(friend.x, friend.y, friend.z, w);
                if (!point.visible || point.distance > MAX_DISTANCE) continue;
                p.setColor(0xffffd75c);
                c.drawCircle(point.x, point.y, 5*density, p);
                drawLabel(c, friend.name, point.x, point.y - 18*density,
                        10*density, Color.WHITE, 0xdd28372c);
            }
        }

        private ScreenPoint project(float worldX, float worldY, float worldZ, float width) {
            float dx = worldX - playerX;
            float dz = worldZ - playerZ;
            float distance = (float)Math.sqrt(dx*dx + dz*dz);
            if (distance < 0.08f) distance = 0.08f;
            float bearing = (float)Math.atan2(dx, dz);
            float relativeYaw = normalizeAngle(bearing - yaw);
            float maxAngle = (float)Math.toRadians(horizontalFov * 0.62f);
            if (Math.abs(relativeYaw) > maxAngle) return new ScreenPoint(-1000f,-1000f,distance,false);
            float screenX = width*0.5f + (float)Math.tan(relativeYaw)*focalX;
            float elevation = (float)Math.atan2(worldY - CAMERA_HEIGHT, distance) - pitch;
            float screenY = cameraCenterY - (float)Math.tan(elevation)*focalY;
            boolean visible = screenX > -100*density && screenX < width+100*density &&
                    screenY > -400*density && screenY < getHeight()+400*density;
            return new ScreenPoint(screenX,screenY,distance,visible);
        }

        private float distanceToPlayer(float x, float z) {
            float dx=x-playerX, dz=z-playerZ;
            return (float)Math.sqrt(dx*dx+dz*dz);
        }

        private void drawHud(Canvas c, float w, float h) {
            p.setColor(0xdd101913);
            c.drawRoundRect(10*density, 8*density, w-10*density, 72*density,
                    14*density, 14*density, p);
            drawText(c, "BLOCKWORLD AR  •  V3", 21*density, 25*density,
                    10*density, 0xffd4ead5, false);
            drawText(c, "Шаги двигают тебя по миру  •  Дистанция: " + distanceMeters + " м",
                    21*density, 43*density, 7.8f*density, 0xffd7e2d9, false);

            p.setTextSize(17*density);
            p.setColor(0xffff5656);
            p.setTextAlign(Paint.Align.LEFT);
            for (int i=0;i<5;i++) {
                String heart = i*2 < health ? "♥" : (i*2+1 < health ? "♥" : "♡");
                c.drawText(heart, 21*density+i*25*density, 65*density, p);
            }
            p.setTextSize(16*density);
            p.setColor(0xffffc36b);
            for (int i=0;i<5;i++) {
                String food = i*2 < hunger ? "◆" : (i*2+1 < hunger ? "◆" : "◇");
                c.drawText(food, w-145*density+i*24*density, 64*density, p);
            }
            drawText(c, "XP " + xp, w-20*density, 24*density,
                    8.5f*density, 0xffffdc78, true);

            // Small centre reticle for aiming.
            p.setColor(0xaaFFFFFF);
            p.setStrokeWidth(1.2f*density);
            float cx=w*0.5f, cy=h*0.48f;
            c.drawLine(cx-7*density,cy,cx-3*density,cy,p);
            c.drawLine(cx+3*density,cy,cx+7*density,cy,p);
            c.drawLine(cx,cy-7*density,cx,cy-3*density,p);
            c.drawLine(cx,cy+3*density,cx,cy+7*density,p);
        }

        private void drawBottomPanel(Canvas c, float w, float h) {
            float top = h - 226*density;
            p.setColor(0xef101913);
            c.drawRoundRect(7*density, top, w-7*density, h-7*density,
                    15*density,15*density,p);

            String[] tabs = {"БЛОКИ","МОБЫ","ДРУЗЬЯ"};
            float tabWidth = (w-26*density)/3f;
            for (int i=0;i<3;i++) {
                float x=13*density+i*tabWidth;
                p.setColor(mode==i ? 0xff3e8248 : 0xff29372e);
                c.drawRoundRect(x,top+7*density,x+tabWidth-4*density,top+39*density,
                        8*density,8*density,p);
                drawText(c,tabs[i],x+(tabWidth-4*density)*0.5f,top+28*density,
                        9*density,Color.WHITE,true);
            }

            float rowY=top+46*density;
            if (mode==2) {
                p.setColor(0xff34483a);
                c.drawRoundRect(15*density,rowY,w-15*density,rowY+48*density,
                        8*density,8*density,p);
                drawText(c,"＋ ИМЯ: "+friendName,w*0.34f,rowY+29*density,
                        9.5f*density,Color.WHITE,true);
                drawText(c,"ОЧИСТИТЬ",w*0.79f,rowY+29*density,
                        8.8f*density,0xffffc9aa,true);
                drawText(c,"Коснись места в кадре — поставь табличку",
                        w*0.5f,rowY+65*density,7.8f*density,0xffd4e1d6,true);
            } else {
                String[] list = mode==0 ? BLOCKS : MOBS;
                int count = Math.min(6,Math.max(1,(int)((w-32*density)/(56*density))));
                float cell=(w-26*density)/count;
                for (int i=0;i<count;i++) {
                    int index=catalogScroll+i;
                    if (index>=list.length) break;
                    float x=13*density+i*cell;
                    boolean selected = mode==0 ? index==selectedBlock : index==selectedMob;
                    p.setColor(selected ? 0xff537d4b : 0xff26362d);
                    c.drawRoundRect(x,rowY,x+cell-3*density,rowY+51*density,
                            6*density,6*density,p);
                    if (mode==0) drawMiniCube(c,x+cell*0.5f,rowY+20*density,17*density,BLOCK_COLORS[index]);
                    else drawMiniMob(c,x+cell*0.5f,rowY+26*density,0.28f*density,MOB_COLORS[index]);
                    String label=list[index].length()>8?list[index].substring(0,7)+"…":list[index];
                    drawText(c,label,x+cell*0.5f,rowY+44*density,
                            6.8f*density,0xfff4f6f2,true);
                }
                drawText(c,"‹",8*density,rowY+34*density,19*density,Color.WHITE,true);
                drawText(c,"›",w-8*density,rowY+34*density,19*density,Color.WHITE,true);
            }

            float hotY=top+105*density;
            float slot=(w-26*density)/9f;
            for (int i=0;i<9;i++) {
                float x=13*density+i*slot;
                p.setColor(i==selectedSlot ? 0xffd1bf68 : 0xff3b473f);
                c.drawRoundRect(x,hotY,x+slot-2*density,hotY+52*density,
                        5*density,5*density,p);
                if (mode==0) drawMiniCube(c,x+slot*0.5f,hotY+23*density,18*density,BLOCK_COLORS[blockSlots[i]]);
                else if (mode==1) drawMiniMob(c,x+slot*0.5f,hotY+26*density,0.25f*density,MOB_COLORS[mobSlots[i]]);
                else drawText(c,"★",x+slot*0.5f,hotY+32*density,14*density,0xffffd66c,true);
                drawText(c,""+(i+1),x+slot*0.5f,hotY+48*density,
                        6.5f*density,0xffe9eee9,true);
            }

            float controlY=top+166*density;
            p.setColor(0xff29372e);
            c.drawRoundRect(16*density,controlY,w-16*density,controlY+47*density,
                    9*density,9*density,p);
            drawText(c,"−",w*0.24f,controlY+31*density,22*density,Color.WHITE,true);
            drawText(c,"ДАЛЬНОСТЬ  "+distanceMeters+" м",w*0.5f,controlY+29*density,
                    9.3f*density,0xffffe8aa,true);
            drawText(c,"＋",w*0.76f,controlY+31*density,20*density,Color.WHITE,true);
        }

        private void drawMiniCube(Canvas c,float x,float y,float size,int color) {
            drawIsoCube(c,x,y,size,color);
        }

        private void drawMiniMob(Canvas c,float x,float y,float scale,int color) {
            float s=scale;
            drawVoxelBox(c,x,y-9*s,12*s,13*s,4*s,color);
            drawVoxelBox(c,x,y-20*s,10*s,10*s,4*s,color);
            p.setColor(0xff1e2520);
            c.drawRect(x-3*s,y-16*s,x-1*s,y-14*s,p);
            c.drawRect(x+1*s,y-16*s,x+3*s,y-14*s,p);
        }

        private void drawIsoCube(Canvas c,float x,float y,float size,int color) {
            float half=size*0.51f;
            float depth=size*0.28f;
            float top=y-size*0.60f;
            float mid=y-size*0.22f;
            float bottom=y+size*0.34f;
            Path upper=new Path();
            upper.moveTo(x-half,mid);
            upper.lineTo(x,top);
            upper.lineTo(x+half,mid);
            upper.lineTo(x,bottom-size*0.34f);
            upper.close();
            Path left=new Path();
            left.moveTo(x-half,mid);
            left.lineTo(x,bottom-size*0.34f);
            left.lineTo(x,bottom+depth*0.16f);
            left.lineTo(x-half,bottom-size*0.34f+depth*0.16f);
            left.close();
            Path right=new Path();
            right.moveTo(x,bottom-size*0.34f);
            right.lineTo(x+half,mid);
            right.lineTo(x+half,bottom-size*0.34f+depth*0.16f);
            right.lineTo(x,bottom+depth*0.16f);
            right.close();
            p.setColor(shade(color,0.68f)); c.drawPath(left,p);
            p.setColor(shade(color,0.84f)); c.drawPath(right,p);
            p.setColor(shade(color,1.18f)); c.drawPath(upper,p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Math.max(1f,density));
            p.setColor(0x77332b26);
            c.drawPath(left,p); c.drawPath(right,p); c.drawPath(upper,p);
            p.setStyle(Paint.Style.FILL);
        }

        private void drawMob3D(Canvas c,WorldMob mob,float x,float baseY,float ppm) {
            int type=mob.type;
            int body=MOB_COLORS[type];
            if (type==4) {
                float unit=clamp(ppm*0.52f,10*density,150*density);
                drawVoxelBox(c,x,baseY-unit*0.56f,unit*0.95f,unit*0.52f,unit*0.28f,body);
                for(int i=0;i<4;i++) {
                    float legX=x-unit*0.55f+i*unit*0.36f;
                    drawVoxelBox(c,legX,baseY-unit*0.25f,unit*0.18f,unit*0.5f,unit*0.12f,shade(body,0.72f));
                    drawVoxelBox(c,legX-unit*0.10f,baseY-unit*0.08f,unit*0.17f,unit*0.32f,unit*0.10f,shade(body,0.72f));
                }
                p.setColor(Color.RED);
                c.drawCircle(x-unit*0.19f,baseY-unit*0.64f,Math.max(2*density,unit*0.045f),p);
                c.drawCircle(x+unit*0.19f,baseY-unit*0.64f,Math.max(2*density,unit*0.045f),p);
                return;
            }

            float scale=ppm;
            float head= (type==3 ? 0.48f : (type==9 ? 0.28f : 0.43f))*scale;
            float bodyW=(type==3 ? 0.36f : (type==5 ? 0.55f : (type>=6 ? 0.65f : 0.52f)))*scale;
            float legH=(type>=6 ? 0.30f : (type==3 ? 0.82f : 0.52f))*scale;
            float torsoH=(type>=6 ? 0.56f : (type==3 ? 1.08f : 0.66f))*scale;
            float totalH=legH+torsoH+head*1.12f;
            float topY=baseY-totalH;
            int skin=(type==1||type==8||type==9)?0xffd8d4c8:body;
            int shirt=(type==0)?0xff2872a7:(type==1?0xffdedbd0:(type==3?0xff1d1b25:body));
            int pants=(type==0)?0xff384a91:(type==1?0xffc4beb1:(type==3?0xff22202c:shade(body,0.75f)));

            if (type==2) {
                // Creeper: square head, tall body, four short legs.
                drawVoxelBox(c,x-bodyW*0.31f,baseY-legH*0.85f,bodyW*0.62f,legH*0.85f,bodyW*0.35f,body);
                drawVoxelBox(c,x+bodyW*0.31f,baseY-legH*0.85f,bodyW*0.62f,legH*0.85f,bodyW*0.35f,body);
                drawVoxelBox(c,x-bodyW*0.31f,baseY-legH*0.85f,bodyW*0.62f,legH*0.85f,bodyW*0.35f,body);
                drawVoxelBox(c,x+bodyW*0.31f,baseY-legH*0.85f,bodyW*0.62f,legH*0.85f,bodyW*0.35f,body);
                drawVoxelBox(c,x,topY+head*0.88f,bodyW,torsoH,bodyW*0.57f,body);
                drawVoxelBox(c,x,topY,head*1.25f,head,head*0.55f,body);
                p.setColor(0xff17231a);
                c.drawRect(x-head*0.34f,topY+head*0.38f,x-head*0.08f,topY+head*0.60f,p);
                c.drawRect(x+head*0.08f,topY+head*0.38f,x+head*0.34f,topY+head*0.60f,p);
                c.drawRect(x-head*0.12f,topY+head*0.55f,x+head*0.12f,topY+head*0.83f,p);
            } else if (type==5 || type==6 || type==7 || type==8 || type==9) {
                // Slime or blocky farm animal.
                float animalY=baseY-legH;
                drawVoxelBox(c,x,animalY,bodyW*1.15f,torsoH*0.80f,bodyW*0.56f,body);
                drawVoxelBox(c,x+bodyW*0.48f,animalY+torsoH*0.04f,head*1.05f,head*1.05f,head*0.70f,skin);
                for(int i=0;i<4;i++) {
                    float legX=x-bodyW*0.42f+(i%2)*bodyW*0.82f;
                    float frontBack=(i<2?0.06f:0.42f);
                    drawVoxelBox(c,legX,baseY-legH*0.58f,bodyW*0.20f,legH*0.58f,bodyW*0.18f,shade(body,0.72f));
                }
                if(type==5) {
                    // Slime's cubic silhouette and bright eyes.
                    drawVoxelBox(c,x,baseY-torsoH*0.60f,bodyW*1.18f,torsoH*0.65f,bodyW*0.52f,0xaa70d66a);
                } else if(type==7) {
                    drawVoxelBox(c,x+bodyW*0.50f,animalY+head*0.05f,head*0.18f,head*0.45f,head*0.18f,0xffeee8d9);
                } else if(type==9) {
                    drawVoxelBox(c,x+bodyW*0.48f,animalY+head*0.70f,head*0.25f,head*0.18f,head*0.2f,0xffd94b42);
                }
                p.setColor(0xff26221f);
                c.drawCircle(x+bodyW*0.35f,animalY+head*0.37f,Math.max(2*density,head*0.055f),p);
                c.drawCircle(x+bodyW*0.65f,animalY+head*0.37f,Math.max(2*density,head*0.055f),p);
            } else {
                // Zombie, skeleton and Enderman.
                float legTop=baseY-legH;
                drawVoxelBox(c,x-bodyW*0.22f,legTop+legH*0.5f,bodyW*0.32f,legH,bodyW*0.28f,pants);
                drawVoxelBox(c,x+bodyW*0.22f,legTop+legH*0.5f,bodyW*0.32f,legH,bodyW*0.28f,pants);
                drawVoxelBox(c,x-bodyW*0.72f,topY+head*1.30f,bodyW*0.34f,torsoH*0.92f,bodyW*0.26f,shirt);
                drawVoxelBox(c,x+bodyW*0.72f,topY+head*1.30f,bodyW*0.34f,torsoH*0.92f,bodyW*0.26f,shirt);
                drawVoxelBox(c,x,topY+head*1.02f,bodyW,torsoH,bodyW*0.48f,shirt);
                drawVoxelBox(c,x,topY+head*0.45f,head,head,head*0.48f,skin);
                if(type==3) {
                    // Enderman's extra height and purple eyes.
                    p.setColor(0xffca63ff);
                    c.drawRect(x-head*0.33f,topY+head*0.38f,x-head*0.08f,topY+head*0.48f,p);
                    c.drawRect(x+head*0.08f,topY+head*0.38f,x+head*0.33f,topY+head*0.48f,p);
                } else {
                    p.setColor(0xff202020);
                    c.drawRect(x-head*0.30f,topY+head*0.35f,x-head*0.08f,topY+head*0.52f,p);
                    c.drawRect(x+head*0.08f,topY+head*0.35f,x+head*0.30f,topY+head*0.52f,p);
                }
            }

            if(mob.hp<5) {
                float barW=Math.max(24*density,bodyW*1.4f);
                p.setColor(0xcc211e1d);
                c.drawRoundRect(x-barW/2,topY-8*density,x+barW/2,topY-3*density,3*density,3*density,p);
                p.setColor(0xffef514c);
                c.drawRoundRect(x-barW/2,topY-8*density,x-barW/2+barW*mob.hp/5f,topY-3*density,3*density,3*density,p);
            }
        }

        private void drawVoxelBox(Canvas c,float x,float yTop,float width,float height,float depth,int color) {
            float left=x-width*0.5f;
            float right=x+width*0.5f;
            float side=depth*0.45f;
            Path sideFace=new Path();
            sideFace.moveTo(right,yTop);
            sideFace.lineTo(right+side,yTop-side*0.65f);
            sideFace.lineTo(right+side,yTop+height-side*0.65f);
            sideFace.lineTo(right,yTop+height);
            sideFace.close();
            Path topFace=new Path();
            topFace.moveTo(left,yTop);
            topFace.lineTo(right,yTop);
            topFace.lineTo(right+side,yTop-side*0.65f);
            topFace.lineTo(left+side,yTop-side*0.65f);
            topFace.close();
            p.setColor(shade(color,0.66f)); c.drawPath(sideFace,p);
            p.setColor(shade(color,1.16f)); c.drawPath(topFace,p);
            p.setColor(color); c.drawRect(left,yTop,right,yTop+height,p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Math.max(0.8f,density*0.65f));
            p.setColor(0x55302720);
            c.drawRect(left,yTop,right,yTop+height,p);
            p.setStyle(Paint.Style.FILL);
        }

        private void placeAt(float x,float y) {
            float angleH=(float)Math.atan((x-getWidth()*0.5f)/Math.max(1f,focalX));
            float angleV=(float)Math.atan((cameraCenterY-y)/Math.max(1f,focalY));
            float bearing=normalizeAngle(yaw+angleH);
            float dist=distanceMeters;
            float wx=playerX+(float)Math.sin(bearing)*dist;
            float wz=playerZ+(float)Math.cos(bearing)*dist;
            float elevation=pitch+angleV;
            float wy=CAMERA_HEIGHT+(float)Math.tan(elevation)*dist;
            wy=clamp(wy,0.04f,4.5f);

            if(mode==0) {
                WorldBlock hit=findBlockAt(x,y);
                if(hit!=null) {
                    blocks.remove(hit);
                    xp++;
                    setStatus("Блок разрушен  +1 XP");
                } else if(blocks.size()<180) {
                    blocks.add(new WorldBlock(selectedBlock,wx,wy,wz));
                    setStatus(BLOCKS[selectedBlock]+" поставлен на расстоянии "+distanceMeters+" м");
                } else setStatus("Достигнут лимит блоков");
            } else if(mode==1) {
                WorldMob hit=findMobAt(x,y);
                if(hit!=null) {
                    hit.hp--;
                    if(hit.hp<=0) {
                        mobs.remove(hit);
                        xp+=3;
                        setStatus(MOBS[hit.type]+" побеждён  +3 XP");
                    } else setStatus(MOBS[hit.type]+"  HP "+hit.hp+"/5");
                } else if(mobs.size()<30) {
                    mobs.add(new WorldMob(selectedMob,wx,wz));
                    setStatus(MOBS[selectedMob]+" появился и идёт к тебе");
                } else setStatus("Слишком много мобов рядом");
            } else {
                friends.add(new FriendTag(friendName,wx,wy,wz));
                setStatus("Метка «"+friendName+"» закреплена в твоём мире");
            }
            saveWorld();
            invalidate();
        }

        private WorldBlock findBlockAt(float x,float y) {
            WorldBlock best=null;float bestDist=Float.MAX_VALUE;
            for(WorldBlock b:blocks) {
                float r=Math.max(22*density,b.lastScreenSize*0.75f);
                float dx=x-b.lastScreenX,dy=y-b.lastScreenY;
                float d2=dx*dx+dy*dy;
                if(Math.abs(dx)<r && Math.abs(dy)<r && d2<bestDist) {best=b;bestDist=d2;}
            }
            return best;
        }

        private WorldMob findMobAt(float x,float y) {
            WorldMob best=null;float bestDist=Float.MAX_VALUE;
            for(WorldMob m:mobs) {
                float halfW=Math.max(22*density,m.lastScreenWidth*0.7f);
                float top=m.lastScreenBaseY-m.lastScreenHeight;
                if(x>=m.lastScreenX-halfW && x<=m.lastScreenX+halfW &&
                        y>=top && y<=m.lastScreenBaseY) {
                    float d2=(x-m.lastScreenX)*(x-m.lastScreenX)+
                            (y-(top+m.lastScreenBaseY)*0.5f)*(y-(top+m.lastScreenBaseY)*0.5f);
                    if(d2<bestDist){best=m;bestDist=d2;}
                }
            }
            return best;
        }

        @Override public boolean onTouchEvent(android.view.MotionEvent e) {
            float x=e.getX(),y=e.getY(),h=getHeight(),w=getWidth();
            float panelTop=h-226*density;
            if(e.getAction()==android.view.MotionEvent.ACTION_DOWN) {
                touchDownX=x;touchDownY=y;touchDownAt=System.currentTimeMillis();
                return true;
            }
            if(e.getAction()!=android.view.MotionEvent.ACTION_UP) return true;

            if(!cameraReady && y<panelTop && y>panelTop-100*density) {
                if(callbacks!=null) callbacks.retryCamera();
                return true;
            }

            if(y>=panelTop) {
                if(y<panelTop+44*density) {
                    mode=clampInt((int)((x-13*density)/((w-26*density)/3f)),0,2);
                    catalogScroll=0;
                    invalidate();return true;
                }
                if(mode==2 && y<panelTop+112*density) {
                    if(x<w*0.56f && callbacks!=null) callbacks.editFriendName();
                    else if(callbacks!=null) callbacks.clearFriends();
                    return true;
                }
                if(mode!=2 && y<panelTop+102*density) {
                    String[] list=mode==0?BLOCKS:MOBS;
                    int count=Math.min(6,Math.max(1,(int)((w-32*density)/(56*density))));
                    float cell=(w-26*density)/count;
                    if(x<17*density) catalogScroll=Math.max(0,catalogScroll-1);
                    else if(x>w-17*density) catalogScroll=Math.min(Math.max(0,list.length-count),catalogScroll+1);
                    else {
                        int index=catalogScroll+(int)((x-13*density)/cell);
                        if(index>=0&&index<list.length) {
                            if(mode==0){selectedBlock=index;blockSlots[0]=index;}
                            else{selectedMob=index;mobSlots[0]=index;}
                            selectedSlot=0;
                        }
                    }
                    invalidate();return true;
                }
                if(y>=panelTop+103*density && y<panelTop+162*density) {
                    int slot=clampInt((int)((x-13*density)/((w-26*density)/9f)),0,8);
                    selectedSlot=slot;
                    if(mode==0)selectedBlock=blockSlots[slot];
                    if(mode==1)selectedMob=mobSlots[slot];
                    invalidate();return true;
                }
                if(y>=panelTop+164*density) {
                    if(x<w*0.37f)distanceMeters=Math.max(1,distanceMeters-1);
                    else if(x>w*0.63f)distanceMeters=Math.min(8,distanceMeters+1);
                    invalidate();return true;
                }
                return true;
            }

            if(health<=0) {
                health=10;hunger=10;lastHunger=System.currentTimeMillis();
                setStatus("Игрок восстановлен");
                invalidate();return true;
            }
            if(!cameraReady) {
                if(callbacks!=null)callbacks.retryCamera();
                return true;
            }
            placeAt(x,y);
            return true;
        }

        private void setStatus(String message) {
            // Kept as the current HUD message. No toast spam during the game loop.
            statusMessage=message;
            invalidate();
        }
        private String statusMessage="Осмотри комнату и выбери блок";
        private float clamp(float value,float min,float max){return Math.max(min,Math.min(max,value));}
        private int clampInt(int value,int min,int max){return Math.max(min,Math.min(max,value));}
        private int shade(int color,float factor) {
            int r=Math.min(255,(int)(Color.red(color)*factor));
            int g=Math.min(255,(int)(Color.green(color)*factor));
            int b=Math.min(255,(int)(Color.blue(color)*factor));
            return Color.argb(Color.alpha(color),r,g,b);
        }
        private void drawText(Canvas c,String value,float x,float y,float size,int color,boolean center) {
            p.setTextSize(size);p.setColor(color);
            p.setTextAlign(center?Paint.Align.CENTER:Paint.Align.LEFT);
            c.drawText(value,x,y,p);
        }
        private void drawLabel(Canvas c,String value,float x,float y,float size,int textColor,int bg) {
            p.setTextSize(size);p.setTextAlign(Paint.Align.CENTER);
            float width=p.measureText(value)+14*density,height=size+11*density;
            p.setColor(bg);
            c.drawRoundRect(x-width/2,y-height/2,x+width/2,y+height/2,7*density,7*density,p);
            p.setColor(textColor);
            c.drawText(value,x,y+size*0.34f,p);
        }

        @Override protected void onFinishInflate() { super.onFinishInflate(); }

        static final class WorldBlock {
            final int type; final float x,y,z;
            float lastScreenX,lastScreenY,lastScreenSize;
            WorldBlock(int t,float px,float py,float pz){type=t;x=px;y=py;z=pz;}
        }
        static final class WorldMob {
            final int type; float x,z; int hp=5; long lastAttack=0;
            float lastScreenX,lastScreenBaseY,lastScreenHeight,lastScreenWidth;
            WorldMob(int t,float px,float pz){type=t;x=px;z=pz;}
        }
        static final class FriendTag {
            final String name; final float x,y,z;
            FriendTag(String n,float px,float py,float pz){name=n;x=px;y=py;z=pz;}
        }
        static final class ScreenPoint {
            final float x,y,distance; final boolean visible;
            ScreenPoint(float px,float py,float dist,boolean v){x=px;y=py;distance=dist;visible=v;}
        }
    }
}