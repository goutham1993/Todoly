package com.expense.todoly;

import android.app.Application;

import com.expense.todoly.data.Prefs;

public class TodolyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Prefs.applyTheme(this);
    }
}
