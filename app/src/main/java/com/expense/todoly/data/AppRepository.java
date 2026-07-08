package com.expense.todoly.data;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.expense.todoly.data.dao.CategoryDao;
import com.expense.todoly.data.dao.TodoDao;
import com.expense.todoly.data.entity.Category;
import com.expense.todoly.data.entity.Todo;
import com.expense.todoly.data.model.CategoryWithCount;

import java.util.List;

public class AppRepository {

    private final CategoryDao categoryDao;
    private final TodoDao todoDao;

    private final LiveData<List<Category>> categories;
    private final LiveData<List<CategoryWithCount>> categoriesWithCounts;
    private final LiveData<List<Todo>> activeTodos;
    private final LiveData<List<Todo>> completedTodos;

    public AppRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        categoryDao = db.categoryDao();
        todoDao = db.todoDao();
        categories = categoryDao.observeAll();
        categoriesWithCounts = categoryDao.observeCategoriesWithCounts();
        activeTodos = todoDao.observeActive();
        completedTodos = todoDao.observeCompleted();
    }

    public LiveData<List<Category>> getCategories() {
        return categories;
    }

    public LiveData<List<CategoryWithCount>> getCategoriesWithCounts() {
        return categoriesWithCounts;
    }

    public LiveData<List<Todo>> getActiveTodos() {
        return activeTodos;
    }

    public LiveData<List<Todo>> getCompletedTodos() {
        return completedTodos;
    }

    public void addCategory(final String name, final String colorHex) {
        AppDatabase.IO_EXECUTOR.execute(() -> {
            List<Category> all = categoryDao.getAllSync();
            int order = all.size();
            categoryDao.insert(new Category(name, colorHex, order, System.currentTimeMillis()));
        });
    }

    public void addTodo(final long categoryId, final String title, final String notes,
                        final boolean important, final boolean quick) {
        AppDatabase.IO_EXECUTOR.execute(() -> {
            int order = todoDao.maxSortOrder(categoryId) + 1;
            Todo todo = new Todo(categoryId, title, notes, System.currentTimeMillis(), order);
            todo.important = important;
            todo.quick = quick;
            todoDao.insert(todo);
        });
    }

    public void reorderTodos(final List<Long> orderedIds) {
        AppDatabase.IO_EXECUTOR.execute(() -> {
            for (int i = 0; i < orderedIds.size(); i++) {
                todoDao.updateSortOrder(orderedIds.get(i), i);
            }
        });
    }

    public void setCompleted(final long todoId, final boolean completed) {
        AppDatabase.IO_EXECUTOR.execute(() ->
                todoDao.setCompleted(todoId, completed, completed ? System.currentTimeMillis() : 0L));
    }

    public void deleteTodo(final Todo todo) {
        AppDatabase.IO_EXECUTOR.execute(() -> todoDao.delete(todo));
    }

    public void deleteCategory(final Category category) {
        AppDatabase.IO_EXECUTOR.execute(() -> categoryDao.delete(category));
    }

    public void deleteCategoryById(final long id) {
        AppDatabase.IO_EXECUTOR.execute(() -> categoryDao.deleteById(id));
    }

    public void updateTodo(final Todo todo) {
        AppDatabase.IO_EXECUTOR.execute(() -> todoDao.update(todo));
    }
}
