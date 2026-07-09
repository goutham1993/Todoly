package com.expense.todoly;

import android.app.Application;

import com.expense.todoly.data.AppDatabase;
import com.expense.todoly.data.AppRepository;
import com.expense.todoly.data.DayRollover;
import com.expense.todoly.data.Prefs;
import com.expense.todoly.data.dao.TodoDao;

public class TodolyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Prefs.applyTheme(this);
        AppDatabase.IO_EXECUTOR.execute(() -> {
            TodoDao todoDao = AppDatabase.getInstance(this).todoDao();
            DayRollover.runIfNeeded(this, todoDao);
        });
    }
}
