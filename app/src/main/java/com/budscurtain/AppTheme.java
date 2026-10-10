package com.budscurtain;

import android.content.Context;
import android.content.res.Configuration;

final class AppTheme {
    static final String SYSTEM = "system";
    static final String LIGHT = "light";
    static final String DARK = "dark";

    static String choice(Context context) {
        String saved = Prefs.of(context).getString("theme", SYSTEM);
        return LIGHT.equals(saved) || DARK.equals(saved) ? saved : SYSTEM;
    }

    static void choose(Context context, String choice) {
        if (!SYSTEM.equals(choice) && !LIGHT.equals(choice) && !DARK.equals(choice)) return;
        Prefs.of(context).edit().putString("theme", choice).apply();
    }

    static boolean isDark(Context context) {
        return isDark(choice(context), context.getResources().getConfiguration());
    }

    static boolean isDark(String choice, Configuration configuration) {
        if (DARK.equals(choice)) return true;
        if (LIGHT.equals(choice)) return false;
        return (configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private AppTheme() {}
}
