package com.expense.todoly.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.RemoteViews;

import com.expense.todoly.MainActivity;
import com.expense.todoly.R;
import com.expense.todoly.data.AppDatabase;
import com.expense.todoly.data.DayRollover;
import com.expense.todoly.data.dao.TodoDao;

public class TodolyWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_WIDGET_ITEM =
            "com.expense.todoly.widget.ACTION_WIDGET_ITEM";
    public static final String EXTRA_TODO_ID = "extra_todo_id";
    public static final String EXTRA_OPEN_APP = "extra_open_app";

    private static final long COMPLETE_ANIMATION_MS = 450L;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final PendingResult pendingResult = goAsync();
        AppDatabase.IO_EXECUTOR.execute(() -> {
            try {
                runRollover(context);
                for (int appWidgetId : appWidgetIds) {
                    updateWidget(context, appWidgetManager, appWidgetId);
                }
            } finally {
                pendingResult.finish();
            }
        });
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_WIDGET_ITEM.equals(intent.getAction())) {
            if (intent.getBooleanExtra(EXTRA_OPEN_APP, false)) {
                Intent open = new Intent(context, MainActivity.class);
                open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(open);
                return;
            }
            long todoId = intent.getLongExtra(EXTRA_TODO_ID, -1L);
            if (todoId != -1L) {
                handleComplete(context, todoId);
                return;
            }
        }
        super.onReceive(context, intent);
    }

    private void handleComplete(Context context, long todoId) {
        if (WidgetCompletionTracker.isCompleting(todoId)) {
            return;
        }
        WidgetCompletionTracker.markCompleting(todoId);
        WidgetHelper.refreshListData(context);

        MAIN.postDelayed(() -> AppDatabase.IO_EXECUTOR.execute(() -> {
            TodoDao todoDao = AppDatabase.getInstance(context).todoDao();
            todoDao.setCompleted(todoId, true, System.currentTimeMillis());
            WidgetCompletionTracker.clear(todoId);
            WidgetHelper.refreshWidgets(context);
        }), COMPLETE_ANIMATION_MS);
    }

    /**
     * Must be called from a background thread — queries the database for the task count.
     */
    static void updateWidget(Context context, AppWidgetManager manager, int appWidgetId) {
        TodoDao todoDao = AppDatabase.getInstance(context).todoDao();
        int count = todoDao.countTodayActive();

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_todo_list);
        views.setTextViewText(R.id.widget_count, String.valueOf(count));

        Intent serviceIntent = new Intent(context, TodolyWidgetService.class);
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));
        views.setRemoteAdapter(R.id.widget_list, serviceIntent);
        views.setEmptyView(R.id.widget_list, R.id.widget_empty);

        Intent templateIntent = new Intent(context, TodolyWidgetProvider.class);
        templateIntent.setAction(ACTION_WIDGET_ITEM);
        PendingIntent templatePendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                templateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        views.setPendingIntentTemplate(R.id.widget_list, templatePendingIntent);

        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openAppPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_header, openAppPendingIntent);

        manager.updateAppWidget(appWidgetId, views);
        manager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list);
    }

    private static void runRollover(Context context) {
        TodoDao todoDao = AppDatabase.getInstance(context).todoDao();
        DayRollover.runIfNeeded(context, todoDao);
    }

    public static void updateAll(Context context) {
        Context app = context.getApplicationContext();
        AppWidgetManager manager = AppWidgetManager.getInstance(app);
        ComponentName provider = new ComponentName(app, TodolyWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(provider);
        if (ids.length == 0) {
            return;
        }
        AppDatabase.IO_EXECUTOR.execute(() -> {
            runRollover(app);
            for (int id : ids) {
                updateWidget(app, manager, id);
            }
        });
    }
}
