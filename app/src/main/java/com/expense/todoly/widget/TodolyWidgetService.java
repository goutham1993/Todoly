package com.expense.todoly.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class TodolyWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new TodolyWidgetViewsFactory(getApplicationContext(), intent);
    }
}
