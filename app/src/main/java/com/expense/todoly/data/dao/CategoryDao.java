package com.expense.todoly.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.expense.todoly.data.entity.Category;
import com.expense.todoly.data.model.CategoryWithCount;

import java.util.List;

@Dao
public interface CategoryDao {

    @Insert
    long insert(Category category);

    @Insert
    void insertAll(List<Category> categories);

    @Update
    void update(Category category);

    @Delete
    void delete(Category category);

    @Query("DELETE FROM categories WHERE id = :id")
    void deleteById(long id);

    @Query("DELETE FROM categories")
    void deleteAll();

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, createdAt ASC")
    LiveData<List<Category>> observeAll();

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, createdAt ASC")
    List<Category> getAllSync();

    @Query("SELECT COUNT(*) FROM categories")
    int count();

    @Query("SELECT c.id AS id, c.name AS name, c.colorHex AS colorHex, c.sortOrder AS sortOrder, "
            + "SUM(CASE WHEN t.isCompleted = 0 THEN 1 ELSE 0 END) AS activeCount, "
            + "COUNT(t.id) AS totalCount "
            + "FROM categories c LEFT JOIN todos t ON t.categoryId = c.id "
            + "GROUP BY c.id ORDER BY c.sortOrder ASC, c.createdAt ASC")
    LiveData<List<CategoryWithCount>> observeCategoriesWithCounts();
}
