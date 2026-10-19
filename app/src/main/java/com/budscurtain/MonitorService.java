package com.budscurtain;

import android.annotation.SuppressLint;
import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.bluetooth.le.ScanFilter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.os.ParcelUuid;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.PathInterpolator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Arrays;
import java.util.Objects;

public final class MonitorService extends Service {
    static final String START = "com.budscurtain.START";
    static final String STOP = "com.budscurtain.STOP";
    static final String DEMO = "com.budscurtain.DEMO";
    static final String THEME_CHANGED = "com.budscurtain.THEME_CHANGED";
    private static final String CHANNEL = "monitor";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, Long> logged = new HashMap<>();
    private final Map<String, String> lastPayload = new HashMap<>();
    private BluetoothLeScanner scanner;
    private ScanCallback callback;
    private int scanFailures;
    private long lastScanStartedAt;
    private long lastScanResultAt;
    private int unconfirmedScans;
    private final Runnable retryScan = this::startBleScan;
    private final Runnable recoverSilentScan = () -> {
        if (this.monitoring && SystemClock.elapsedRealtime() -
                Math.max(lastScanStartedAt, lastScanResultAt) > 30000) {
            Prefs.log(this, "Нет BLE-сигналов при подключении; восстанавливаю сканирование");
            unconfirmedScans = 0;
            startBleScan();
        }
    };
    private final Runnable verifyScan = () -> {
        if (!this.monitoring || callback == null || lastScanResultAt >= lastScanStartedAt) return;
        // HyperOS can reject a background start without calling onScanFailed.
        // Retry briefly, then wait for a new connection or the app to be opened.
        if (++unconfirmedScans < 3) scheduleScanRetry("Нет BLE-пакетов после запуска", 10000);
        else Prefs.log(this, "BLE пока не подтвердил работу; ожидаю подключение или открытие приложения");
    };
    private WindowManager windows;
    private CurtainCard card;
    private WindowManager.LayoutParams cardParams;
    private boolean cardAttached;
    private long animationEpoch;
    private static final PathInterpolator ENTER_CURVE = new PathInterpolator(0.18f, 0.0f, 0.0f, 1.0f);
    private static final PathInterpolator EXIT_CURVE = new PathInterpolator(0.42f, 0.0f, 1.0f, 1.0f);
    private boolean receiverRegistered;
    private boolean monitoring;
    private boolean visible;
    private final CurtainState state = new CurtainState();
    private final BudsDockEvidence dock = new BudsDockEvidence();
    private boolean demo;
    private long lastBatteryAt;
    private String lastBatteryNote = "";
    private long lastBatteryLogAt;
    private boolean leftCharging, rightCharging, caseCharging;
    private BluetoothDevice selected;
    private Integer leftBattery, rightBattery, caseBattery;
    private static final ParcelUuid FAST_PAIR = ParcelUuid.fromString("0000FE2C-0000-1000-8000-00805F9B34FB");
    private final Runnable expiry = new Runnable() {
        @Override public void run() {
            long now = SystemClock.elapsedRealtime();
            if (!demo && visible && (!state.wantsCard(now) ||
                ("Xiaomi Buds 5".equalsIgnoreCase(Prefs.name(MonitorService.this)) && !dock.bothDocked(now))))
                hideCard("Сигнал кейса закончился");
            if (lastBatteryAt > 0 && now - lastBatteryAt > CurtainState.STALE_MS) clearBattery();
            if (monitoring) handler.postDelayed(this, 1000);
        }
    };
    private final BroadcastReceiver connectionReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_ON.equals(intent.getAction()) || Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
                handler.removeCallbacks(recoverSilentScan);
                handler.postDelayed(recoverSilentScan, 2000);
                return;
            }
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                int adapterState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (adapterState == BluetoothAdapter.STATE_TURNING_OFF || adapterState == BluetoothAdapter.STATE_OFF) {
                    state.disconnected(SystemClock.elapsedRealtime());
                    if (!demo) hideCard("Bluetooth выключен");
                    clearBattery();
                    scheduleScanRetry("Bluetooth выключен; ожидаю включения", 30000);
                } else if (adapterState == BluetoothAdapter.STATE_ON && monitoring) {
                    Prefs.log(context, "Bluetooth включён; восстанавливаю BLE-сканирование");
                    scanFailures = 0;
                    unconfirmedScans = 0;
                    handler.removeCallbacks(retryScan);
                    handler.postDelayed(retryScan, Math.max(0, lastScanStartedAt + 6000 - SystemClock.elapsedRealtime()));
                }
                return;
            }
            BluetoothDevice device = Build.VERSION.SDK_INT >= 33
                ? intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class)
                : intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device == null || !isSelected(device.getAddress())) return;
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(intent.getAction())) {
                state.connected();
                Prefs.log(context, "Bluetooth подключён; ожидаю сигнал кейса");
                // A Bluetooth stack restart can silently discard the scanner registration.
                // Re-register on a real connection after a long silence, without polling.
                // During profile negotiation HyperOS temporarily rejects background starts.
                handler.removeCallbacks(recoverSilentScan);
                handler.postDelayed(recoverSilentScan, 2000);
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(intent.getAction())) {
                Prefs.log(context, "Отключение: " + safeName(device));
                state.disconnected(SystemClock.elapsedRealtime());
                if (!demo) hideCard("Bluetooth отключился");
                clearBattery();
            }
        }
    };
    private final BroadcastReceiver themeReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (card != null) card.applyTheme(AppTheme.isDark(MonitorService.this));
        }
    };

    @SuppressLint("UnspecifiedRegisterReceiverFlag") // Flags overload exists only on API 33+.
    @Override public void onCreate() {
        super.onCreate();
        windows = (WindowManager)getSystemService(WINDOW_SERVICE);
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Наблюдение Buds 5", NotificationManager.IMPORTANCE_LOW));
        if (Build.VERSION.SDK_INT >= 33)
            registerReceiver(themeReceiver, new IntentFilter(THEME_CHANGED), Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(themeReceiver, new IntentFilter(THEME_CHANGED));
    }

    @Override public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (card != null) card.applyTheme(AppTheme.isDark(AppTheme.choice(this), configuration));
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? START : intent.getAction();
        if (STOP.equals(action)) { Prefs.of(this).edit().putBoolean("enabled", false).apply(); stopSelf(); return START_NOT_STICKY; }
        startForeground(7, notification());
        if (DEMO.equals(action)) {
            handler.removeCallbacks(stopAfterDemo);
            demo = true;
            showCard("Демонстрация оформления");
            handler.removeCallbacks(endDemo);
            long demoDuration = intent == null ? 5000 : intent.getLongExtra("demo_duration_ms", 5000);
            handler.postDelayed(endDemo, Math.max(1000, Math.min(demoDuration, 15000)));
            return monitoring ? START_STICKY : START_NOT_STICKY;
        }
        if (!monitoring) begin();
        else {
            unconfirmedScans = 0;
            recoverSilentScan.run();
        }
        return monitoring ? START_STICKY : START_NOT_STICKY;
    }

    private final Runnable stopAfterDemo = () -> {
        if (!demo && !monitoring) stopSelf();
    };
    private final Runnable endDemo = () -> {
        demo = false;
        hideCard("Демонстрация завершена");
        if (!monitoring) handler.postDelayed(stopAfterDemo, 400);
    };

    private Notification notification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("Xiaomi Buds 5 Overlay работает")
            .setContentText("Наблюдение за Xiaomi Buds 5")
            .setContentIntent(pi).setOngoing(true).build();
    }

    private void begin() {
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Prefs.log(this, "Нет разрешений Bluetooth"); stopSelf(); return;
        }
        BluetoothManager manager = getSystemService(BluetoothManager.class);
        BluetoothAdapter adapter = manager == null ? null : manager.getAdapter();
        if (adapter == null) { Prefs.log(this, "Bluetooth недоступен"); stopSelf(); return; }
        String wanted = Prefs.address(this);
        if (wanted.isEmpty()) { Prefs.log(this, "Наушники не выбраны"); stopSelf(); return; }
        try { selected = adapter.getRemoteDevice(wanted); } catch (IllegalArgumentException ex) { Prefs.log(this, "Неверный адрес устройства"); stopSelf(); return; }
        monitoring = true;
        if (card == null) card = new CurtainCard(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(connectionReceiver, filter, Context.RECEIVER_EXPORTED);
        else registerReceiver(connectionReceiver, filter);
        receiverRegistered = true;
        startBleScan();
        Prefs.of(this).edit().putBoolean("active", true).apply();
        handler.postDelayed(expiry, 1000);
    }

    @SuppressLint("MissingPermission") // begin() checks permission; stop/start are guarded against revocation.
    private void startBleScan() {
        if (!monitoring) return;
        handler.removeCallbacks(retryScan);
        handler.removeCallbacks(verifyScan);
        if (scanner != null && callback != null) {
            try { scanner.stopScan(callback); } catch (Exception ignored) {}
        }
        callback = null;
        BluetoothManager manager = getSystemService(BluetoothManager.class);
        BluetoothAdapter adapter = manager == null ? null : manager.getAdapter();
        scanner = adapter == null || !adapter.isEnabled() ? null : adapter.getBluetoothLeScanner();
        if (scanner == null) { scheduleScanRetry("Bluetooth-сканер недоступен", 30000); return; }
        callback = new ScanCallback() {
            @Override public void onScanResult(int callbackType, ScanResult result) {
                handler.post(() -> {
                    if (callback != this) return;
                    scanFailures = 0;
                    unconfirmedScans = 0;
                    lastScanResultAt = SystemClock.elapsedRealtime();
                    handler.removeCallbacks(verifyScan);
                    if (!Prefs.of(MonitorService.this).getBoolean("scan_confirmed", false))
                        Prefs.of(MonitorService.this).edit().putBoolean("scan_confirmed", true).apply();
                    observe(result);
                });
            }
            @Override public void onScanFailed(int errorCode) {
                handler.post(() -> {
                    if (callback == this) scheduleScanRetry("Ошибка BLE сканирования: " + errorCode,
                        errorCode == ScanCallback.SCAN_FAILED_SCANNING_TOO_FREQUENTLY ? 60000 : 0);
                });
            }
        };
        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build();
        try {
            lastScanStartedAt = SystemClock.elapsedRealtime();
            scanner.startScan(Prefs.of(this).getBoolean("diagnostic", false)
                ? Arrays.asList(new ScanFilter.Builder().build()) : Arrays.asList(
                new ScanFilter.Builder().setDeviceAddress(Prefs.address(this)).build(),
                new ScanFilter.Builder().setServiceData(FAST_PAIR, new byte[0]).build()), settings, callback);
            Prefs.of(this).edit().putBoolean("scan_active", true).putBoolean("scan_confirmed", false).apply();
            handler.postDelayed(verifyScan, 15000);
            Prefs.log(this, "Фильтр BLE запущен в экономном режиме");
        } catch (Exception ex) {
            scheduleScanRetry("Не удалось запустить BLE: " + ex.getClass().getSimpleName(), 0);
        }
    }

    @SuppressLint("MissingPermission") // stopScan is best effort when permission changes.
    private void scheduleScanRetry(String reason, long minimumDelay) {
        if (!monitoring) return;
        handler.removeCallbacks(verifyScan);
        if (scanner != null && callback != null) {
            try { scanner.stopScan(callback); } catch (Exception ignored) {}
        }
        callback = null;
        scanner = null;
        Prefs.of(this).edit().putBoolean("scan_active", false).putBoolean("scan_confirmed", false).apply();
        long backoff = Math.min(60000, 5000L << Math.min(scanFailures++, 3));
        long delay = Math.max(minimumDelay, backoff);
        Prefs.log(this, reason + "; повтор через " + delay / 1000 + " с");
        handler.removeCallbacks(retryScan);
        handler.postDelayed(retryScan, delay);
    }

    private void observe(ScanResult result) {
        if (!monitoring || result.getDevice() == null) return;
        ScanRecord record = result.getScanRecord();
        String address = result.getDevice().getAddress();
        boolean match = isSelected(address);
        boolean diagnostic = Prefs.of(this).getBoolean("diagnostic", false);
        if (!match && !(diagnostic && result.getRssi() >= -85)) return;
        long now = SystemClock.elapsedRealtime();
        if (diagnostic) {
            String name = record == null ? null : record.getDeviceName();
            if (name == null) name = safeName(result.getDevice());
            String payload = packet(record);
            Long previous = logged.get(address);
            if (previous == null || now - previous > 10000 || !payload.equals(lastPayload.get(address))) {
                logged.put(address, now);
                lastPayload.put(address, payload);
                Prefs.log(this, (match ? "Buds " : "BLE ") + name + " " + address + " RSSI " + result.getRssi() + " " + payload);
                if (logged.size() > 100) { logged.clear(); lastPayload.clear(); }
            }
        }
        if (!match) return;
        boolean buds5 = "Xiaomi Buds 5".equalsIgnoreCase(Prefs.name(this));
        FastPairBattery battery = FastPairBattery.parse(record == null ? null : record.getServiceData(FAST_PAIR), buds5);
        if (battery == null) return;
        long timestamp = result.getTimestampNanos() / 1000000;
        // A partial hide can alternate with a partial show while the case is open.
        // The complete hide packet still closes the card immediately.
        boolean partialHide = buds5 && !battery.show && battery.cradle != null &&
            (battery.left == null || battery.right == null) &&
            (battery.leftCharging || battery.rightCharging);
        if (partialHide) {
            if (!state.acceptPartial(timestamp, now)) return;
        } else if (!state.accept(battery.show, timestamp, now)) return;
        Integer oldLeft = leftBattery, oldRight = rightBattery, oldCase = caseBattery;
        boolean oldLeftCharging = leftCharging, oldRightCharging = rightCharging, oldCaseCharging = caseCharging;
        if (buds5) {
            if (!battery.show && !partialHide) dock.reset();
            else dock.observe(battery, now);
            leftBattery = dock.left; rightBattery = dock.right; caseBattery = dock.cradle;
            leftCharging = dock.leftCharging; rightCharging = dock.rightCharging; caseCharging = dock.caseCharging;
        } else {
            leftBattery = battery.left; rightBattery = battery.right; caseBattery = battery.cradle;
            leftCharging = battery.leftCharging; rightCharging = battery.rightCharging; caseCharging = battery.caseCharging;
        }
        boolean levelsChanged = !Objects.equals(oldLeft, leftBattery) || !Objects.equals(oldRight, rightBattery) ||
            !Objects.equals(oldCase, caseBattery) || oldLeftCharging != leftCharging ||
            oldRightCharging != rightCharging || oldCaseCharging != caseCharging;
        lastBatteryAt = now;
        String note = "Fast Pair: " + (battery.show ? "показать" : "скрыть") +
            " L=" + battery.left + " R=" + battery.right + " C=" + battery.cradle +
            " зарядка=" + battery.leftCharging + "/" + battery.rightCharging + "/" + battery.caseCharging;
        boolean changed = !note.equals(lastBatteryNote);
        if (changed && now - lastBatteryLogAt >= 2000) {
            Prefs.log(this, note + " RSSI=" + result.getRssi());
            lastBatteryLogAt = now;
        }
        lastBatteryNote = note;
        long lastSaved = Prefs.of(this).getLong("last_signal", 0);
        if (System.currentTimeMillis() - lastSaved >= 1000) {
            Prefs.of(this).edit().putLong("last_signal", System.currentTimeMillis())
                .putString("last_levels", "Левый " + percent(leftBattery) + " · правый " + percent(rightBattery) + " · кейс " + percent(caseBattery))
                .putBoolean("case_show", battery.show).apply();
        }
        if (demo) return;
        if (buds5 && !dock.bothDocked(now)) {
            hideCard("Нет подтверждения, что оба наушника в кейсе");
            return;
        }
        if (!state.wantsCard(now)) { hideCard("Fast Pair: скрыть"); return; }
        if (!getSystemService(PowerManager.class).isInteractive()) return;
        if (!visible) showCard("Наушники рядом");
        else if (card != null && levelsChanged) updateCard("Кейс открыт · рядом");
    }

    private boolean isSelected(String address) {
        String selectedAddress = Prefs.address(this);
        if (selectedAddress.isEmpty()) return false;
        return selectedAddress.equalsIgnoreCase(address);
    }
    private String safeName(BluetoothDevice device) {
        try { String n = device.getName(); return n == null ? "без имени" : n; }
        catch (SecurityException ex) { return "без имени"; }
    }
    private static String percent(Integer value) { return value == null ? "—" : value + "%"; }
    private void clearBattery() {
        leftBattery = rightBattery = caseBattery = null;
        leftCharging = rightCharging = caseCharging = false;
        lastBatteryAt = 0;
        lastBatteryNote = "";
        dock.reset();
    }
    private void updateCard(String message) {
        card.setLevels(leftBattery, rightBattery, caseBattery, leftCharging, rightCharging, caseCharging, message);
    }
    private String packet(ScanRecord record) {
        if (record == null) return "без данных";
        byte[] bytes = record.getBytes();
        if (bytes == null) return "без данных";
        StringBuilder out = new StringBuilder("RAW:");
        for (int i = 0; i < Math.min(bytes.length, 128); i++) out.append(String.format(Locale.US, "%02X", bytes[i] & 0xff));
        return out.toString();
    }

    private void showCard(String message) {
        if (!Settings.canDrawOverlays(this)) { Prefs.log(this, "Окно не показано: нет разрешения поверх приложений"); return; }
        if (!attachCard()) return;
        if ((cardParams.flags & WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) != 0) {
            cardParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            cardParams.alpha = 1f;
            try { windows.updateViewLayout(card, cardParams); }
            catch (Exception ex) { Prefs.log(this, "Ошибка overlay: " + ex.getClass().getSimpleName()); return; }
        }
        if (demo && leftBattery == null && rightBattery == null && caseBattery == null)
            card.setLevels(100, 100, 64, true, true, false, "Пример отображения");
        else updateCard(demo ? "Пример отображения" : "Кейс открыт · рядом");
        long epoch = ++animationEpoch;
        card.animate().cancel();
        visible = true;
        card.postOnAnimation(() -> {
            if (cardAttached && visible && animationEpoch == epoch) {
                card.animate().withEndAction(null).translationY(0f)
                    .setInterpolator(ENTER_CURVE).setDuration(420).withLayer().start();
            }
        });
        Prefs.log(this, "Карточка показана: " + message);
    }

    private boolean attachCard() {
        if (cardAttached) return true;
        if (card == null) card = new CurtainCard(this);
        cardParams = new WindowManager.LayoutParams(getResources().getDisplayMetrics().widthPixels - dp(32), -2,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT);
        cardParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        cardParams.y = dp(25);
        cardParams.preferredRefreshRate = 120f;
        card.setOnTouchListener(new View.OnTouchListener() {
            float startRawY;
            float startTranslation;
            boolean tracking;
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        if (!visible) return true;
                        ++animationEpoch;
                        v.animate().cancel();
                        startRawY = e.getRawY();
                        startTranslation = v.getTranslationY();
                        tracking = true;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (tracking && visible) {
                            float distance = e.getRawY() - startRawY;
                            float hidden = v.getHeight() + dp(35);
                            v.setTranslationY(Math.max(0f, Math.min(hidden, startTranslation + distance)));
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (tracking && visible) {
                            float distance = e.getRawY() - startRawY;
                            tracking = false;
                            if (distance >= Math.min(dp(96), v.getHeight() * 0.3f)) {
                                state.dismiss();
                                hideCard("Скрыто жестом до следующего открытия");
                            } else snapCardBack();
                        }
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        if (tracking && visible) snapCardBack();
                        tracking = false;
                        return true;
                    default:
                        return true;
                }
            }
        });
        card.measure(View.MeasureSpec.makeMeasureSpec(cardParams.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        card.setTranslationY(card.getMeasuredHeight() + dp(35));
        card.setAlpha(1f);
        try { windows.addView(card, cardParams); cardAttached = true; return true; }
        catch (Exception ex) { card = null; cardParams = null; Prefs.log(this, "Ошибка overlay: " + ex.getClass().getSimpleName()); return false; }
    }
    private void snapCardBack() {
        if (card == null || !visible) return;
        ++animationEpoch;
        card.animate().cancel();
        card.animate().withEndAction(null).translationY(0f)
            .setInterpolator(ENTER_CURVE).setDuration(240).withLayer().start();
    }
    private void hideCard(String why) {
        if (card == null || !visible) return;
        visible = false;
        CurtainCard closing = card;
        long epoch = ++animationEpoch;
        closing.animate().cancel();
        float hidden = closing.getHeight() + dp(35);
        long duration = Math.max(150, Math.min(320, (long)(320 * Math.max(0f, hidden - closing.getTranslationY()) / hidden)));
        closing.animate().translationY(hidden)
            .setInterpolator(EXIT_CURVE).setDuration(duration).withLayer().withEndAction(() -> {
                if (card == closing && !visible && cardAttached && animationEpoch == epoch) {
                    try { windows.removeViewImmediate(closing); } catch (Exception ignored) {}
                    cardAttached = false;
                    cardParams = null;
                }
            }).start();
        Prefs.log(this, "Карточка скрыта: " + why);
    }
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }

    @SuppressLint("MissingPermission") // stopScan is best effort during teardown.
    @Override public void onDestroy() {
        monitoring = false; handler.removeCallbacksAndMessages(null);
        if (scanner != null && callback != null) try { scanner.stopScan(callback); } catch (Exception ignored) {}
        if (receiverRegistered) try { unregisterReceiver(connectionReceiver); } catch (Exception ignored) {}
        try { unregisterReceiver(themeReceiver); } catch (Exception ignored) {}
        if (card != null) card.animate().cancel();
        if (cardAttached) try { windows.removeView(card); } catch (Exception ignored) {}
        cardAttached = false; cardParams = null; card = null;
        Prefs.of(this).edit().putBoolean("active", false).putBoolean("scan_active", false).apply();
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
