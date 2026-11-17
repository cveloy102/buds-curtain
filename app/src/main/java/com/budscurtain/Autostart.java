package com.budscurtain;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Resumes monitoring after a reboot or an app update, when it was left enabled. */
public final class Autostart extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) resume(context, "перезагрузка");
        else if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) resume(context, "обновление приложения");
    }

    static void resume(Context context, String why) {
        if (MonitorService.instance != null || !Prefs.of(context).getBoolean("enabled", false)) return;
        try {
            context.startForegroundService(new Intent(context, MonitorService.class).setAction(MonitorService.RESUME));
            Prefs.log(context, "Наблюдение возобновлено: " + why);
        } catch (Exception ex) {
            Prefs.log(context, "Не удалось возобновить наблюдение (" + why + "): " + ex.getClass().getSimpleName());
        }
    }
}
