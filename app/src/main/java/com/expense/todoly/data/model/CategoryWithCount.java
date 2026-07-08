package com.expense.todoly.data.model;

import androidx.annotation.NonNull;

public class CategoryWithCount {

    public long id;

    @NonNull
    public String name;

    @NonNull
    public String colorHex;

    public int sortOrder;

    public int activeCount;

    public int totalCount;
}
