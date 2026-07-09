package com.expense.todoly.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "todos",
        foreignKeys = @ForeignKey(
                entity = Category.class,
                parentColumns = "id",
                childColumns = "categoryId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("categoryId")}
)
public class Todo {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long categoryId;

    @NonNull
    public String title;

    public String notes;

    @ColumnInfo(name = "isCompleted")
    public boolean isCompleted;

    public boolean important;

    public boolean quick;

    public boolean weekend;

    public boolean weekday;

    public long createdAt;

    public long completedAt;

    public int sortOrder;

    public Todo(long categoryId, @NonNull String title, String notes, long createdAt, int sortOrder) {
        this.categoryId = categoryId;
        this.title = title;
        this.notes = notes;
        this.isCompleted = false;
        this.important = false;
        this.quick = false;
        this.weekend = false;
        this.weekday = false;
        this.createdAt = createdAt;
        this.completedAt = 0L;
        this.sortOrder = sortOrder;
    }
}
