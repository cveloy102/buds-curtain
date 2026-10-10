package com.budscurtain;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** ADB-only entry point for repeatable animation checks in debug builds. */
public final class DebugTriggerActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        String theme = getIntent().getStringExtra("theme");
        if (theme != null) {
            AppTheme.choose(this, theme);
            sendBroadcast(new Intent(MonitorService.THEME_CHANGED).setPackage(getPackageName()));
        }
        if (getIntent().getBooleanExtra("demo", true)) {
            startForegroundService(new Intent(this, MonitorService.class).setAction(MonitorService.DEMO)
                .putExtra("demo_duration_ms", 15000L));
        }
        finish();
    }
}
