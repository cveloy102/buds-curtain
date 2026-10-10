package com.budscurtain;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class MainActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView deviceText, stateText, logText;
    private ScrollView scroll;
    private boolean dark;
    private String renderedChoice;
    private String renderedLog;
    private boolean running;
    private final Runnable refresh = new Runnable() {
        @Override public void run() { update(); handler.postDelayed(this, 1500); }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
        requestBluetooth();
    }

    private void buildUi() {
        dark = AppTheme.isDark(this);
        renderedChoice = AppTheme.choice(this);
        int screen = dark ? 0xff101719 : 0xfff6f7f4;
        int ink = dark ? 0xfff4f5f2 : 0xff17232d;
        int muted = dark ? 0xffa9bcb3 : 0xff71808a;
        int accent = dark ? 0xff8bd5ad : 0xff45977a;
        getWindow().setStatusBarColor(screen);
        getWindow().setNavigationBarColor(screen);
        getWindow().getDecorView().setSystemUiVisibility(dark ? 0 :
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(screen);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(34), dp(20), dp(32));
        scroll.addView(page);
        setContentView(scroll);

        TextView heading = label("Xiaomi Buds 5", 29, ink, true);
        page.addView(heading);
        TextView sub = label("Карточка заряда · 0.6.6", 14, muted, false);
        addSpace(page, sub, 5);

        deviceText = label("", 15, ink, false);
        addSpace(page, deviceText, 24);

        TextView appearance = label("Внешний вид", 18, ink, true);
        addSpace(page, appearance, 0);
        TextView appearanceHelp = label("Тема приложения и карточки", 12, muted, false);
        addSpace(page, appearanceHelp, 5);
        addThemeOption(page, "Как в системе", "Меняется вместе с темой телефона", AppTheme.SYSTEM, ink, muted, accent);
        addThemeOption(page, "Светлая", "Светлая карточка всегда", AppTheme.LIGHT, ink, muted, accent);
        addThemeOption(page, "Тёмная", "Графитовая карточка всегда", AppTheme.DARK, ink, muted, accent);


        TextView controls = label("Управление", 18, ink, true);
        addSpace(page, controls, 26);
        button(page, "1. Разрешить Bluetooth", this::requestBluetooth);
        button(page, "2. Выбрать наушники", this::chooseDevice);
        button(page, "3. Разрешить показ поверх окон", this::requestOverlay);
        button(page, "4. Запустить наблюдение", this::startMonitor);
        button(page, "Остановить наблюдение", this::stopMonitor);
        button(page, "Показать карточку для проверки дизайна", this::showDemo);
        button(page, "Включить/выключить расширенную диагностику", this::toggleDiagnostic);

        stateText = label("", 13, accent, false);
        addSpace(page, stateText, 20);
        TextView logTitle = label("Локальный журнал Bluetooth", 18, ink, true);
        addSpace(page, logTitle, 18);
        TextView logHelp = label("Заряд приходит автоматически при открытии кейса. Для проверки оставь оба наушника внутри. Журнал хранится только на телефоне.", 12, muted, false);
        addSpace(page, logHelp, 7);
        button(page, "Копировать журнал", this::copyLog);
        button(page, "Очистить журнал", () -> { Prefs.of(this).edit().remove("log").apply(); update(); });
        logText = label("", 11, muted, false);
        logText.setTextIsSelectable(true);
        addSpace(page, logText, 10);
        renderedLog = null;
        if (running) update();
    }

    private void addThemeOption(LinearLayout page, String title, String explanation, String value,
                                int ink, int muted, int accent) {
        boolean selected = value.equals(renderedChoice);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(10), dp(12), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(selected ? (dark ? 0xff344844 : 0xffe5efe9) : (dark ? 0xff1c2528 : 0xffffffff));
        bg.setCornerRadius(dp(15));
        bg.setStroke(dp(1), selected ? accent : (dark ? 0xff354043 : 0xffe5e9e5));
        row.setBackground(bg);
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        words.addView(label(title, 14, ink, selected));
        TextView help = label(explanation, 11, muted, false);
        LinearLayout.LayoutParams helpParams = new LinearLayout.LayoutParams(-1, -2);
        helpParams.topMargin = dp(3);
        words.addView(help, helpParams);
        row.addView(words, new LinearLayout.LayoutParams(0, -2, 1));
        RadioButton radio = new RadioButton(this);
        radio.setButtonTintList(ColorStateList.valueOf(accent));
        radio.setChecked(selected);
        row.addView(radio);
        View.OnClickListener choose = v -> chooseTheme(value);
        row.setOnClickListener(choose);
        radio.setOnClickListener(choose);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(8);
        page.addView(row, params);
    }

    private void chooseTheme(String choice) {
        if (choice.equals(AppTheme.choice(this))) return;
        int y = scroll.getScrollY();
        AppTheme.choose(this, choice);
        sendBroadcast(new Intent(MonitorService.THEME_CHANGED).setPackage(getPackageName()));
        buildUi();
        scroll.post(() -> scroll.scrollTo(0, y));
    }

    @Override protected void onResume() {
        super.onResume(); running = true;
        if (dark != AppTheme.isDark(this) || !AppTheme.choice(this).equals(renderedChoice)) buildUi();
        selectKnownBuds();
        if (Prefs.of(this).getBoolean("enabled", false) && hasBluetooth() && Settings.canDrawOverlays(this) && !Prefs.address(this).isEmpty()) startMonitor();
        handler.post(refresh);
    }
    @Override protected void onPause() { running = false; handler.removeCallbacks(refresh); super.onPause(); }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        selectKnownBuds();
        update();
    }

    private void update() {
        if (!running) return;
        String chosen = Prefs.address(this).isEmpty() ? "Устройство не выбрано" : Prefs.name(this) + "\n" + Prefs.address(this);
        deviceText.setText(chosen);
        boolean overlay = Settings.canDrawOverlays(this);
        boolean active = Prefs.of(this).getBoolean("active", false);
        boolean scanning = Prefs.of(this).getBoolean("scan_active", active);
        long seen = Prefs.of(this).getLong("last_signal", 0);
        long seconds = Math.max(0, (System.currentTimeMillis() - seen) / 1000);
        String signal = seen == 0 ? "Открой кейс рядом с телефоном" :
            (seconds <= 8 && active ? "Свежий сигнал: " : "Последние данные (" + seconds + " с назад): ") + Prefs.of(this).getString("last_levels", "—");
        stateText.setText("Bluetooth: " + (hasBluetooth() ? "разрешён" : "нужно разрешение") +
            "  ·  поверх окон: " + (overlay ? "разрешено" : "не разрешено") +
            "\nНаблюдение: " + (!active ? "остановлено" : scanning ? "запущено" : "восстанавливает Bluetooth") +
            "  ·  диагностика: " + (Prefs.of(this).getBoolean("diagnostic", false) ? "включена" : "выключена") + "\n" + signal);
        String log = Prefs.of(this).getString("log", "Пока нет событий");
        if (!log.equals(renderedLog)) {
            renderedLog = log;
            logText.setText(log.length() <= 5000 ? log : log.substring(0, 5000));
        }
    }

    private boolean hasBluetooth() {
        return checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }
    private void requestBluetooth() {
        ArrayList<String> needed = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.BLUETOOTH_SCAN);
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.BLUETOOTH_CONNECT);
        if (!needed.isEmpty()) requestPermissions(needed.toArray(new String[0]), 1);
        else toast("Разрешения Bluetooth уже выданы");
    }
    private void requestOverlay() {
        if (Settings.canDrawOverlays(this)) { toast("Показ поверх окон уже разрешён"); return; }
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }
    private void selectKnownBuds() {
        if (!hasBluetooth() || !Prefs.address(this).isEmpty()) return;
        BluetoothAdapter adapter = getSystemService(BluetoothManager.class).getAdapter();
        if (adapter == null || !adapter.isEnabled()) return;
        BluetoothDevice found = null;
        for (BluetoothDevice device : adapter.getBondedDevices()) {
            if ("Xiaomi Buds 5".equalsIgnoreCase(device.getName())) {
                if (found != null) return;
                found = device;
            }
        }
        if (found != null) { Prefs.choose(this, found.getName(), found.getAddress()); Prefs.log(this, "Выбраны сопряжённые Xiaomi Buds 5"); }
    }
    private void chooseDevice() {
        if (!hasBluetooth()) { requestBluetooth(); return; }
        BluetoothManager manager = getSystemService(BluetoothManager.class);
        BluetoothAdapter adapter = manager == null ? null : manager.getAdapter();
        if (adapter == null || !adapter.isEnabled()) { toast("Включи Bluetooth"); return; }
        try {
            Set<BluetoothDevice> bonded = adapter.getBondedDevices();
            List<BluetoothDevice> devices = new ArrayList<>(bonded);
            devices.sort(Comparator.comparing(d -> d.getName() == null ? "" : d.getName()));
            if (devices.isEmpty()) { toast("Сначала сопряги Buds 5 в настройках Bluetooth"); return; }
            String[] titles = new String[devices.size()];
            for (int i = 0; i < devices.size(); i++) titles[i] = (devices.get(i).getName() == null ? "Без имени" : devices.get(i).getName()) + "  ·  " + devices.get(i).getAddress();
        new AlertDialog.Builder(this, dark ? android.R.style.Theme_Material_Dialog_Alert : android.R.style.Theme_Material_Light_Dialog_Alert).setTitle("Выбери свои Buds 5").setItems(titles, (dialog, index) -> {
                BluetoothDevice d = devices.get(index);
                if (Prefs.of(this).getBoolean("active", false)) stopMonitor();
                Prefs.choose(this, d.getName() == null ? "Buds 5" : d.getName(), d.getAddress());
                Prefs.of(this).edit().remove("last_signal").remove("last_levels").apply();
                Prefs.log(this, "Выбрано устройство " + Prefs.name(this) + " " + d.getAddress());
                update();
            }).show();
        } catch (SecurityException ex) { toast("Нет разрешения на Bluetooth"); }
    }
    private void startMonitor() {
        if (!hasBluetooth()) { requestBluetooth(); return; }
        if (Prefs.address(this).isEmpty()) { chooseDevice(); return; }
        if (!Settings.canDrawOverlays(this)) { requestOverlay(); return; }
        try {
            Intent intent = new Intent(this, MonitorService.class).setAction(MonitorService.START);
            startForegroundService(intent);
            Prefs.of(this).edit().putBoolean("enabled", true).apply();
            toast("Наблюдение запущено"); update();
        } catch (Exception ex) { Prefs.log(this, "Ошибка запуска: " + ex.getClass().getSimpleName()); toast("Не удалось запустить службу"); }
    }
    private void stopMonitor() {
        startService(new Intent(this, MonitorService.class).setAction(MonitorService.STOP));
        Prefs.of(this).edit().putBoolean("enabled", false).putBoolean("active", false).apply(); update();
    }
    private void showDemo() {
        if (!hasBluetooth()) { requestBluetooth(); return; }
        if (!Settings.canDrawOverlays(this)) { requestOverlay(); return; }
        Intent intent = new Intent(this, MonitorService.class).setAction(MonitorService.DEMO);
        startForegroundService(intent);
    }
    private void toggleDiagnostic() {
        boolean next = !Prefs.of(this).getBoolean("diagnostic", false);
        Prefs.of(this).edit().putBoolean("diagnostic", next).apply();
        Prefs.log(this, "Расширенная диагностика " + (next ? "включена" : "выключена"));
        toast(next ? "Запись близких BLE сигналов включена" : "Диагностика выключена");
        update();
    }
    private void copyLog() {
        String log = Prefs.of(this).getString("log", "");
        ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Xiaomi Buds 5 log", log));
        toast("Журнал скопирован");
    }
    private void button(LinearLayout page, String title, Runnable action) {
        Button b = new Button(this);
        b.setText(title); b.setTextColor(dark ? 0xfff1f8f3 : 0xff24453a); b.setAllCaps(false); b.setTextSize(14);
        GradientDrawable bg = new GradientDrawable(); bg.setColor(dark ? 0xff23332f : 0xffe2ebe5); bg.setCornerRadius(dp(13));
        b.setBackground(bg); b.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(50)); p.topMargin = dp(9);
        page.addView(b, p);
    }
    private TextView label(String content, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(content); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(null, 1); return t;
    }
    private void addSpace(LinearLayout page, View child, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.topMargin = dp(top); page.addView(child, p);
    }
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }
}
