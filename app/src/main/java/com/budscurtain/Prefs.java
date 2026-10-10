package com.budscurtain;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class Prefs {
    private static final String FILE = "buds_curtain";
    static SharedPreferences of(Context context) { return context.getSharedPreferences(FILE, Context.MODE_PRIVATE); }
    static String address(Context context) { return of(context).getString("address", ""); }
    static String name(Context context) { return of(context).getString("name", "Xiaomi Buds 5"); }
    static void choose(Context context, String name, String address) {
        of(context).edit().putString("name", name).putString("address", address).apply();
    }
    static void log(Context context, String message) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        SharedPreferences p = of(context);
        String old = p.getString("log", "");
        String next = time + "  " + message + "\n" + old;
        if (next.length() > 16000) next = next.substring(0, 16000);
        p.edit().putString("log", next).apply();
    }
}
