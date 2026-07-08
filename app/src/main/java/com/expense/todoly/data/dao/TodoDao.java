package com.expense.todoly.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.expense.todoly.data.entity.Todo;

import java.util.List;

@Dao
public interface TodoDao {

    @Insert
    long insert(Todo todo);

    @Update
    void update(Todo todo);

    @Update
    void updateAll(List<Todo> todos);

    @Delete
    void delete(Todo todo);

    @Query("UPDATE todos SET sortOrder = :sortOrder WHERE id = :id")
    void updateSortOrder(long id, int sortOrder);

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM todos WHERE categoryId = :categoryId")
    int maxSortOrder(long categoryId);

    @Query("SELECT * FROM todos ORDER BY categoryId ASC, sortOrder ASC, createdAt ASC")
    LiveData<List<Todo>> observeAll();

    @Query("SELECT * FROM todos WHERE isCompleted = 0 ORDER BY categoryId ASC, sortOrder ASC, createdAt ASC")
    LiveData<List<Todo>> observeActive();

    @Query("SELECT * FROM todos WHERE isCompleted = 1 ORDER BY completedAt DESC")
    LiveData<List<Todo>> observeCompleted();

    @Query("UPDATE todos SET isCompleted = :completed, completedAt = :completedAt WHERE id = :id")
    void setCompleted(long id, boolean completed, long completedAt);

    @Query("SELECT * FROM todos WHERE id = :id")
    Todo getById(long id);
}
