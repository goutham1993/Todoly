package com.expense.todoly.widget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;

import com.expense.todoly.R;
import com.expense.todoly.data.AppDatabase;

public final class WidgetHelper {

    private WidgetHelper() {
    }

    /** Rebinds list rows only — no database access, safe on the main thread. */
    public static void refreshListData(Context context) {
        Context app = context.getApplicationContext();
        AppWidgetManager manager = AppWidgetManager.getInstance(app);
        ComponentName provider = new ComponentName(app, TodolyWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(provider);
        for (int id : ids) {
            manager.notifyAppWidgetViewDataChanged(id, R.id.widget_list);
        }
    }

    /** Full widget refresh including task count — always runs off the main thread. */
    public static void refreshWidgets(Context context) {
        AppDatabase.IO_EXECUTOR.execute(() -> {
            Context app = context.getApplicationContext();
            AppWidgetManager manager = AppWidgetManager.getInstance(app);
            ComponentName provider = new ComponentName(app, TodolyWidgetProvider.class);
            int[] ids = manager.getAppWidgetIds(provider);
            if (ids.length == 0) {
                return;
            }
            for (int id : ids) {
                TodolyWidgetProvider.updateWidget(app, manager, id);
            }
        });
    }
}
