package com.expense.todoly.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public final class Prefs {

    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    private static final String PREFS_NAME = "todoly_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_LAST_ROLLOVER_DAY = "last_rollover_day";

    private Prefs() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static int getThemeMode(Context context) {
        return prefs(context).getInt(KEY_THEME_MODE, THEME_SYSTEM);
    }

    public static void setThemeMode(Context context, int themeMode) {
        prefs(context).edit().putInt(KEY_THEME_MODE, themeMode).apply();
    }

    public static int toNightMode(int themeMode) {
        switch (themeMode) {
            case THEME_LIGHT:
                return AppCompatDelegate.MODE_NIGHT_NO;
            case THEME_DARK:
                return AppCompatDelegate.MODE_NIGHT_YES;
            case THEME_SYSTEM:
            default:
                return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }

    public static void applyTheme(Context context) {
        AppCompatDelegate.setDefaultNightMode(toNightMode(getThemeMode(context)));
    }

    public static int getLastRolloverDay(Context context) {
        return prefs(context).getInt(KEY_LAST_ROLLOVER_DAY, 0);
    }

    public static void setLastRolloverDay(Context context, int dayEpoch) {
        prefs(context).edit().putInt(KEY_LAST_ROLLOVER_DAY, dayEpoch).apply();
    }
}
