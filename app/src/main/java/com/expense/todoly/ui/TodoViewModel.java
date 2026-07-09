package com.expense.todoly.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.expense.todoly.data.AppRepository;
import com.expense.todoly.data.entity.Category;
import com.expense.todoly.data.entity.Todo;
import com.expense.todoly.data.model.CategoryWithCount;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TodoViewModel extends AndroidViewModel {

    public enum ViewMode {GROUPED, LIST}

    private final AppRepository repository;

    private final LiveData<List<CategoryWithCount>> categoriesWithCounts;
    private final LiveData<List<Category>> categories;
    private final LiveData<List<Todo>> activeTodos;
    private final LiveData<List<Todo>> completedTodos;
    private final LiveData<Integer> pendingCount;
    private final LiveData<Integer> completedCount;

    private final MutableLiveData<ViewMode> viewMode = new MutableLiveData<>(ViewMode.GROUPED);
    private final MutableLiveData<Set<Long>> collapsedCategoryIds = new MutableLiveData<>(new HashSet<>());
    private final MutableLiveData<Boolean> completedExpanded = new MutableLiveData<>(false);
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> filterImportant = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> filterQuick = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> filterWeekend = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> filterWeekday = new MutableLiveData<>(false);

    private final MediatorLiveData<List<DisplayItem>> displayItems = new MediatorLiveData<>();

    private final MutableLiveData<Event<Integer>> celebrationEvent = new MutableLiveData<>();
    private int completionStreak = 0;

    public TodoViewModel(@NonNull Application application) {
        super(application);
        repository = new AppRepository(application);
        categoriesWithCounts = repository.getCategoriesWithCounts();
        categories = repository.getCategories();
        activeTodos = repository.getActiveTodos();
        completedTodos = repository.getCompletedTodos();
        pendingCount = Transformations.map(activeTodos, list -> list == null ? 0 : list.size());
        completedCount = Transformations.map(completedTodos, list -> list == null ? 0 : list.size());

        displayItems.addSource(categoriesWithCounts, v -> rebuild());
        displayItems.addSource(activeTodos, v -> rebuild());
        displayItems.addSource(completedTodos, v -> rebuild());
        displayItems.addSource(viewMode, v -> rebuild());
        displayItems.addSource(collapsedCategoryIds, v -> rebuild());
        displayItems.addSource(completedExpanded, v -> rebuild());
        displayItems.addSource(searchQuery, v -> rebuild());
        displayItems.addSource(filterImportant, v -> rebuild());
        displayItems.addSource(filterQuick, v -> rebuild());
        displayItems.addSource(filterWeekend, v -> rebuild());
        displayItems.addSource(filterWeekday, v -> rebuild());
    }

    public LiveData<List<DisplayItem>> getDisplayItems() {
        return displayItems;
    }

    public LiveData<Event<Integer>> getCelebrationEvent() {
        return celebrationEvent;
    }

    public LiveData<List<Category>> getCategories() {
        return categories;
    }

    public LiveData<Integer> getPendingCount() {
        return pendingCount;
    }

    public LiveData<Integer> getCompletedCount() {
        return completedCount;
    }

    public LiveData<ViewMode> getViewMode() {
        return viewMode;
    }

    public LiveData<Boolean> getFilterImportant() {
        return filterImportant;
    }

    public LiveData<Boolean> getFilterQuick() {
        return filterQuick;
    }

    public LiveData<Boolean> getFilterWeekend() {
        return filterWeekend;
    }

    public LiveData<Boolean> getFilterWeekday() {
        return filterWeekday;
    }

    private void rebuild() {
        List<CategoryWithCount> cats = categoriesWithCounts.getValue();
        List<Todo> active = activeTodos.getValue();
        List<Todo> completed = completedTodos.getValue();
        ViewMode mode = viewMode.getValue();
        Set<Long> collapsed = collapsedCategoryIds.getValue();
        boolean compExpanded = Boolean.TRUE.equals(completedExpanded.getValue());

        if (cats == null) cats = new ArrayList<>();
        if (active == null) active = new ArrayList<>();
        if (completed == null) completed = new ArrayList<>();
        if (collapsed == null) collapsed = new HashSet<>();
        if (mode == null) mode = ViewMode.GROUPED;

        String query = searchQuery.getValue();
        if (query == null) query = "";
        boolean fImportant = Boolean.TRUE.equals(filterImportant.getValue());
        boolean fQuick = Boolean.TRUE.equals(filterQuick.getValue());
        boolean fWeekend = Boolean.TRUE.equals(filterWeekend.getValue());
        boolean fWeekday = Boolean.TRUE.equals(filterWeekday.getValue());

        if (!query.isEmpty() || fImportant || fQuick || fWeekend || fWeekday) {
            displayItems.setValue(buildFilteredResults(cats, active, completed, query,
                    fImportant, fQuick, fWeekend, fWeekday));
            return;
        }

        List<DisplayItem> items = new ArrayList<>();

        if (mode == ViewMode.GROUPED) {
            for (CategoryWithCount cat : cats) {
                boolean expanded = !collapsed.contains(cat.id);
                items.add(DisplayItem.categoryHeader(cat, expanded));
                if (expanded) {
                    for (Todo t : active) {
                        if (t.categoryId == cat.id) {
                            items.add(DisplayItem.todoRow(t, cat.colorHex, null));
                        }
                    }
                    items.add(DisplayItem.addTask(cat.id, cat.colorHex));
                }
            }
        } else {
            for (Todo t : active) {
                CategoryWithCount cat = findCategory(cats, t.categoryId);
                String color = cat != null ? cat.colorHex : "#9E9E9E";
                String name = cat != null ? cat.name : "";
                items.add(DisplayItem.todoRow(t, color, name));
            }
        }

        if (!completed.isEmpty()) {
            items.add(DisplayItem.completedHeader(completed.size(), compExpanded));
            if (compExpanded) {
                for (Todo t : completed) {
                    CategoryWithCount cat = findCategory(cats, t.categoryId);
                    String color = cat != null ? cat.colorHex : "#9E9E9E";
                    String name = cat != null ? cat.name : "";
                    items.add(DisplayItem.completedRow(t, color, name));
                }
            }
        }

        displayItems.setValue(items);
    }

    private List<DisplayItem> buildFilteredResults(List<CategoryWithCount> cats, List<Todo> active,
                                                   List<Todo> completed, String query,
                                                   boolean fImportant, boolean fQuick,
                                                   boolean fWeekend, boolean fWeekday) {
        List<DisplayItem> items = new ArrayList<>();
        for (Todo t : active) {
            if (matches(t, query) && matchesAttributes(t, fImportant, fQuick, fWeekend, fWeekday)) {
                CategoryWithCount cat = findCategory(cats, t.categoryId);
                String color = cat != null ? cat.colorHex : "#9E9E9E";
                String name = cat != null ? cat.name : "";
                items.add(DisplayItem.todoRow(t, color, name));
            }
        }
        // Completed todos only appear for text search, not for attribute-only filtering.
        if (!query.isEmpty()) {
            for (Todo t : completed) {
                if (matches(t, query) && matchesAttributes(t, fImportant, fQuick, fWeekend, fWeekday)) {
                    CategoryWithCount cat = findCategory(cats, t.categoryId);
                    String color = cat != null ? cat.colorHex : "#9E9E9E";
                    String name = cat != null ? cat.name : "";
                    items.add(DisplayItem.completedRow(t, color, name));
                }
            }
        }
        return items;
    }

    private boolean matches(Todo todo, String query) {
        if (query.isEmpty()) return true;
        if (todo.title != null && todo.title.toLowerCase().contains(query)) return true;
        return todo.notes != null && todo.notes.toLowerCase().contains(query);
    }

    private boolean matchesAttributes(Todo todo, boolean fImportant, boolean fQuick,
                                      boolean fWeekend, boolean fWeekday) {
        if (fImportant && !todo.important) return false;
        if (fQuick && !todo.quick) return false;
        if (fWeekend && !todo.weekend) return false;
        return !fWeekday || todo.weekday;
    }

    private CategoryWithCount findCategory(List<CategoryWithCount> cats, long id) {
        for (CategoryWithCount c : cats) {
            if (c.id == id) return c;
        }
        return null;
    }

    public void addCategory(String name, String colorHex) {
        repository.addCategory(name, colorHex);
    }

    public void addTodo(long categoryId, String title, String notes, boolean important, boolean quick,
                        boolean weekend, boolean weekday) {
        repository.addTodo(categoryId, title, notes, important, quick, weekend, weekday);
    }

    public void updateTodo(Todo todo) {
        repository.updateTodo(todo);
    }

    public void reorderTodos(List<Long> orderedIds) {
        repository.reorderTodos(orderedIds);
    }

    public void toggleComplete(Todo todo, boolean completed) {
        repository.setCompleted(todo.id, completed);
        if (completed) {
            completionStreak++;
            if (completionStreak % 3 == 0) {
                celebrationEvent.setValue(new Event<>(completionStreak));
            }
        } else {
            completionStreak = 0;
        }
    }

    public void deleteTodo(Todo todo) {
        repository.deleteTodo(todo);
    }

    public void deleteCategory(long categoryId) {
        repository.deleteCategoryById(categoryId);
    }

    public void toggleCategoryExpanded(long categoryId) {
        Set<Long> collapsed = new HashSet<>(collapsedCategoryIds.getValue());
        if (collapsed.contains(categoryId)) {
            collapsed.remove(categoryId);
        } else {
            collapsed.add(categoryId);
        }
        collapsedCategoryIds.setValue(collapsed);
    }

    public void toggleCompletedExpanded() {
        completedExpanded.setValue(!Boolean.TRUE.equals(completedExpanded.getValue()));
    }

    public void expandAll() {
        collapsedCategoryIds.setValue(new HashSet<>());
    }

    public void collapseAll() {
        Set<Long> collapsed = new HashSet<>();
        List<CategoryWithCount> cats = categoriesWithCounts.getValue();
        if (cats != null) {
            for (CategoryWithCount c : cats) {
                collapsed.add(c.id);
            }
        }
        collapsedCategoryIds.setValue(collapsed);
    }

    public void toggleViewMode() {
        viewMode.setValue(viewMode.getValue() == ViewMode.LIST ? ViewMode.GROUPED : ViewMode.LIST);
    }

    public void setSearchQuery(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase();
        String current = searchQuery.getValue();
        if (!normalized.equals(current)) {
            searchQuery.setValue(normalized);
        }
    }

    public void setFilterImportant(boolean enabled) {
        if (!Boolean.valueOf(enabled).equals(filterImportant.getValue())) {
            filterImportant.setValue(enabled);
        }
    }

    public void setFilterQuick(boolean enabled) {
        if (!Boolean.valueOf(enabled).equals(filterQuick.getValue())) {
            filterQuick.setValue(enabled);
        }
    }

    public void setFilterWeekend(boolean enabled) {
        if (!Boolean.valueOf(enabled).equals(filterWeekend.getValue())) {
            filterWeekend.setValue(enabled);
        }
    }

    public void setFilterWeekday(boolean enabled) {
        if (!Boolean.valueOf(enabled).equals(filterWeekday.getValue())) {
            filterWeekday.setValue(enabled);
        }
    }
}
