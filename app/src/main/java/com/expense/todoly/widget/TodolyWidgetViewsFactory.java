package com.expense.todoly.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.expense.todoly.R;
import com.expense.todoly.data.AppDatabase;
import com.expense.todoly.data.DayRollover;
import com.expense.todoly.data.dao.TodoDao;
import com.expense.todoly.data.entity.Todo;

import java.util.ArrayList;
import java.util.List;

public class TodolyWidgetViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context context;
    private final int appWidgetId;
    private List<Todo> todos = new ArrayList<>();

    TodolyWidgetViewsFactory(Context context, Intent intent) {
        this.context = context.getApplicationContext();
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onDataSetChanged() {
        TodoDao todoDao = AppDatabase.getInstance(context).todoDao();
        DayRollover.runIfNeeded(context, todoDao);
        List<Todo> loaded = todoDao.getTodayActiveSync();
        todos = loaded == null ? new ArrayList<>() : loaded;
    }

    @Override
    public void onDestroy() {
        todos.clear();
    }

    @Override
    public int getCount() {
        return todos.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (position < 0 || position >= todos.size()) {
            return null;
        }
        Todo todo = todos.get(position);
        boolean completing = WidgetCompletionTracker.isCompleting(todo.id);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_todo_item);
        views.setTextViewText(R.id.widget_item_title, todo.title);

        if (completing) {
            views.setImageViewResource(R.id.widget_checkbox, R.drawable.ic_widget_checkbox_checked);
            views.setInt(R.id.widget_item_title, "setPaintFlags",
                    Paint.ANTI_ALIAS_FLAG | Paint.STRIKE_THRU_TEXT_FLAG);
            views.setTextColor(R.id.widget_item_title,
                    context.getColor(R.color.widget_on_surface_variant));
            views.setFloat(R.id.widget_item_root, "setAlpha", 0.55f);
        } else {
            views.setImageViewResource(R.id.widget_checkbox, R.drawable.ic_widget_checkbox_unchecked);
            views.setInt(R.id.widget_item_title, "setPaintFlags", Paint.ANTI_ALIAS_FLAG);
            views.setTextColor(R.id.widget_item_title, context.getColor(R.color.widget_on_surface));
            views.setFloat(R.id.widget_item_root, "setAlpha", 1f);
        }

        Intent completeIntent = new Intent();
        completeIntent.putExtra(TodolyWidgetProvider.EXTRA_TODO_ID, todo.id);
        views.setOnClickFillInIntent(R.id.widget_checkbox, completeIntent);

        Intent openIntent = new Intent();
        openIntent.putExtra(TodolyWidgetProvider.EXTRA_OPEN_APP, true);
        views.setOnClickFillInIntent(R.id.widget_item_root, openIntent);
        views.setOnClickFillInIntent(R.id.widget_item_title, openIntent);

        return views;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        if (position < 0 || position >= todos.size()) {
            return position;
        }
        return todos.get(position).id;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
