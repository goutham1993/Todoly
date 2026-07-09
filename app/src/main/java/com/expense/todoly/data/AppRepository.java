package com.expense.todoly.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.expense.todoly.data.dao.CategoryDao;
import com.expense.todoly.data.dao.TodoDao;
import com.expense.todoly.data.entity.Category;
import com.expense.todoly.data.entity.Todo;
import com.expense.todoly.data.model.CategoryWithCount;

import java.util.List;

public class AppRepository {

    private final AppDatabase db;
    private final CategoryDao categoryDao;
    private final TodoDao todoDao;

    private final LiveData<List<Category>> categories;
    private final LiveData<List<CategoryWithCount>> categoriesWithCounts;
    private final LiveData<List<Todo>> activeTodos;
    private final LiveData<List<Todo>> completedTodos;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AppRepository(Context context) {
        db = AppDatabase.getInstance(context);
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
                        final boolean important, final boolean quick,
                        final boolean weekend, final boolean weekday,
                        final boolean timesensitive) {
        AppDatabase.IO_EXECUTOR.execute(() -> {
            int order = todoDao.maxSortOrder(categoryId) + 1;
            Todo todo = new Todo(categoryId, title, notes, System.currentTimeMillis(), order);
            todo.important = important;
            todo.quick = quick;
            todo.weekend = weekend;
            todo.weekday = weekday;
            todo.timesensitive = timesensitive;
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

    public interface ExportCallback {
        void onResult(List<Category> categories, List<Todo> todos);

        void onError(Exception e);
    }

    public interface ImportCallback {
        void onResult(int categoryCount, int todoCount);

        void onError(Exception e);
    }

    public void exportAll(final ExportCallback callback) {
        AppDatabase.IO_EXECUTOR.execute(() -> {
            try {
                final List<Category> allCategories = categoryDao.getAllSync();
                final List<Todo> allTodos = todoDao.getAllSync();
                mainHandler.post(() -> callback.onResult(allCategories, allTodos));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void importReplace(final List<Category> importedCategories,
                              final List<Todo> importedTodos,
                              final ImportCallback callback) {
        AppDatabase.IO_EXECUTOR.execute(() -> {
            try {
                db.replaceAll(importedCategories, importedTodos);
                final int categoryCount = importedCategories == null ? 0 : importedCategories.size();
                final int todoCount = importedTodos == null ? 0 : importedTodos.size();
                mainHandler.post(() -> callback.onResult(categoryCount, todoCount));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }
}
