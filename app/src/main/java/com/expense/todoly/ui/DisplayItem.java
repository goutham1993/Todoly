package com.expense.todoly.ui;

import com.expense.todoly.data.entity.Todo;
import com.expense.todoly.data.model.CategoryWithCount;

public class DisplayItem {

    public static final int TYPE_CATEGORY_HEADER = 0;
    public static final int TYPE_TODO_ROW = 1;
    public static final int TYPE_COMPLETED_HEADER = 2;
    public static final int TYPE_COMPLETED_ROW = 3;
    public static final int TYPE_ADD_TASK = 4;

    public final int type;

    public CategoryWithCount category;
    public boolean expanded;

    public Todo todo;
    public String categoryColor;
    public String categoryName;

    public int completedCount;

    public long addCategoryId;

    private DisplayItem(int type) {
        this.type = type;
    }

    public static DisplayItem categoryHeader(CategoryWithCount category, boolean expanded) {
        DisplayItem item = new DisplayItem(TYPE_CATEGORY_HEADER);
        item.category = category;
        item.expanded = expanded;
        return item;
    }

    public static DisplayItem todoRow(Todo todo, String categoryColor, String categoryName) {
        DisplayItem item = new DisplayItem(TYPE_TODO_ROW);
        item.todo = todo;
        item.categoryColor = categoryColor;
        item.categoryName = categoryName;
        return item;
    }

    public static DisplayItem completedHeader(int completedCount, boolean expanded) {
        DisplayItem item = new DisplayItem(TYPE_COMPLETED_HEADER);
        item.completedCount = completedCount;
        item.expanded = expanded;
        return item;
    }

    public static DisplayItem completedRow(Todo todo, String categoryColor, String categoryName) {
        DisplayItem item = new DisplayItem(TYPE_COMPLETED_ROW);
        item.todo = todo;
        item.categoryColor = categoryColor;
        item.categoryName = categoryName;
        return item;
    }

    public static DisplayItem addTask(long categoryId, String categoryColor) {
        DisplayItem item = new DisplayItem(TYPE_ADD_TASK);
        item.addCategoryId = categoryId;
        item.categoryColor = categoryColor;
        return item;
    }

    public long stableId() {
        switch (type) {
            case TYPE_CATEGORY_HEADER:
                return 1_000_000_000L + category.id;
            case TYPE_TODO_ROW:
                return 2_000_000_000L + todo.id;
            case TYPE_COMPLETED_HEADER:
                return 3_000_000_000L;
            case TYPE_COMPLETED_ROW:
                return 4_000_000_000L + todo.id;
            case TYPE_ADD_TASK:
                return 5_000_000_000L + addCategoryId;
            default:
                return RecyclerViewNoId;
        }
    }

    private static final long RecyclerViewNoId = -1L;
}
