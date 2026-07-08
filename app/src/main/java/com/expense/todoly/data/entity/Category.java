package com.expense.todoly.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class Category {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String name;

    @NonNull
    public String colorHex;

    public int sortOrder;

    public long createdAt;

    public Category(@NonNull String name, @NonNull String colorHex, int sortOrder, long createdAt) {
        this.name = name;
        this.colorHex = colorHex;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
    }
}
