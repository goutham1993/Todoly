package com.expense.todoly.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.expense.todoly.R;
import com.expense.todoly.data.AppRepository;
import com.expense.todoly.data.Prefs;
import com.expense.todoly.data.entity.Category;
import com.expense.todoly.data.entity.Todo;
import com.expense.todoly.data.model.CategoryWithCount;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TodoViewModel extends AndroidViewModel {

    public enum ViewMode {GROUPED, LIST}

    public enum HomeTab {ALL, TODAY, WEEKEND}

    private final AppRepository repository;

    private final LiveData<List<CategoryWithCount>> categoriesWithCounts;
    private final LiveData<List<Category>> categories;
    private final LiveData<List<Todo>> activeTodos;
    private final LiveData<List<Todo>> completedTodos;
    private final LiveData<Integer> pendingCount;
    private final LiveData<Integer> completedCount;

    private final MutableLiveData<ViewMode> viewMode;
    private final MutableLiveData<HomeTab> homeTab;
    private final MutableLiveData<Boolean> reorderCategoriesMode = new MutableLiveData<>(false);
    private final MutableLiveData<Set<Long>> collapsedCategoryIds = new MutableLiveData<>(new HashSet<>());
    private final MutableLiveData<Set<String>> collapsedPlanSections = new MutableLiveData<>(new HashSet<>());
    private final MutableLiveData<Boolean> completedExpanded = new MutableLiveData<>(false);
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> filterImportant = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> filterQuick = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> filterWeekend = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> filterWeekday = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> filterTimesensitive = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> filterToday = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> filterTomorrow = new MutableLiveData<>(false);

    private final MediatorLiveData<List<DisplayItem>> displayItems = new MediatorLiveData<>();
    private final MediatorLiveData<Integer> displayPendingCount = new MediatorLiveData<>();
    private final MediatorLiveData<Integer> displayCompletedCount = new MediatorLiveData<>();

    private final MutableLiveData<Event<Integer>> celebrationEvent = new MutableLiveData<>();
    private int completionStreak = 0;
    private boolean initialCollapseApplied = false;

    public TodoViewModel(@NonNull Application application) {
        super(application);
        repository = new AppRepository(application);
        viewMode = new MutableLiveData<>(loadSavedViewMode(application));
        homeTab = new MutableLiveData<>(loadSavedHomeTab(application));
        categoriesWithCounts = repository.getCategoriesWithCounts();
        categories = repository.getCategories();
        activeTodos = repository.getActiveTodos();
        completedTodos = repository.getCompletedTodos();
        pendingCount = Transformations.map(activeTodos, list -> list == null ? 0 : list.size());
        completedCount = Transformations.map(completedTodos, list -> list == null ? 0 : list.size());

        displayItems.addSource(categoriesWithCounts, v -> {
            applyInitialCollapse(v);
            rebuild();
        });
        displayItems.addSource(activeTodos, v -> {
            rebuild();
            rebuildStats();
        });
        displayItems.addSource(completedTodos, v -> {
            rebuild();
            rebuildStats();
        });
        displayItems.addSource(viewMode, v -> rebuild());
        displayItems.addSource(homeTab, v -> {
            rebuild();
            rebuildStats();
        });
        displayItems.addSource(collapsedCategoryIds, v -> rebuild());
        displayItems.addSource(collapsedPlanSections, v -> rebuild());
        displayItems.addSource(completedExpanded, v -> rebuild());
        displayItems.addSource(searchQuery, v -> rebuild());
        displayItems.addSource(filterImportant, v -> rebuild());
        displayItems.addSource(filterQuick, v -> rebuild());
        displayItems.addSource(filterWeekend, v -> rebuild());
        displayItems.addSource(filterWeekday, v -> rebuild());
        displayItems.addSource(filterTimesensitive, v -> rebuild());
        displayItems.addSource(filterToday, v -> rebuild());
        displayItems.addSource(filterTomorrow, v -> rebuild());

        displayPendingCount.addSource(pendingCount, v -> rebuildStats());
        displayCompletedCount.addSource(completedCount, v -> rebuildStats());
        rebuildStats();
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
        return displayPendingCount;
    }

    public LiveData<Integer> getCompletedCount() {
        return displayCompletedCount;
    }

    public LiveData<ViewMode> getViewMode() {
        return viewMode;
    }

    public LiveData<HomeTab> getHomeTab() {
        return homeTab;
    }

    public LiveData<Boolean> getReorderCategoriesMode() {
        return reorderCategoriesMode;
    }

    public void toggleReorderCategoriesMode() {
        if (homeTab.getValue() != HomeTab.ALL) return;
        reorderCategoriesMode.setValue(!Boolean.TRUE.equals(reorderCategoriesMode.getValue()));
    }

    public void setReorderCategoriesMode(boolean enabled) {
        if (enabled && homeTab.getValue() != HomeTab.ALL) return;
        if (!Boolean.valueOf(enabled).equals(reorderCategoriesMode.getValue())) {
            reorderCategoriesMode.setValue(enabled);
        }
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

    public LiveData<Boolean> getFilterTimesensitive() {
        return filterTimesensitive;
    }

    public LiveData<Boolean> getFilterToday() {
        return filterToday;
    }

    public LiveData<Boolean> getFilterTomorrow() {
        return filterTomorrow;
    }

    private void applyInitialCollapse(List<CategoryWithCount> cats) {
        if (initialCollapseApplied || cats == null || cats.isEmpty()) return;
        initialCollapseApplied = true;
        Set<Long> collapsed = new HashSet<>();
        for (CategoryWithCount c : cats) {
            collapsed.add(c.id);
        }
        collapsedCategoryIds.setValue(collapsed);
    }

    private void rebuildStats() {
        HomeTab tab = homeTab.getValue();
        if (tab == null) tab = HomeTab.ALL;

        if (tab == HomeTab.ALL) {
            Integer pending = pendingCount.getValue();
            Integer completed = completedCount.getValue();
            displayPendingCount.setValue(pending == null ? 0 : pending);
            displayCompletedCount.setValue(completed == null ? 0 : completed);
            return;
        }

        List<Todo> active = activeTodos.getValue();
        List<Todo> completed = completedTodos.getValue();
        if (active == null) active = new ArrayList<>();
        if (completed == null) completed = new ArrayList<>();

        int pending = 0;
        for (Todo t : active) {
            if (matchesDayFlag(t, tab)) pending++;
        }
        int done = 0;
        for (Todo t : completed) {
            if (matchesDayFlag(t, tab)) done++;
        }
        displayPendingCount.setValue(pending);
        displayCompletedCount.setValue(done);
    }

    private static boolean matchesDayFlag(Todo todo, HomeTab tab) {
        if (tab == HomeTab.TODAY) return todo.today;
        if (tab == HomeTab.WEEKEND) return todo.weekend;
        return true;
    }

    private void rebuild() {
        List<CategoryWithCount> cats = categoriesWithCounts.getValue();
        List<Todo> active = activeTodos.getValue();
        List<Todo> completed = completedTodos.getValue();
        ViewMode mode = viewMode.getValue();
        HomeTab tab = homeTab.getValue();
        Set<Long> collapsed = collapsedCategoryIds.getValue();
        boolean compExpanded = Boolean.TRUE.equals(completedExpanded.getValue());

        if (cats == null) cats = new ArrayList<>();
        if (active == null) active = new ArrayList<>();
        if (completed == null) completed = new ArrayList<>();
        if (collapsed == null) collapsed = new HashSet<>();
        if (mode == null) mode = ViewMode.GROUPED;
        if (tab == null) tab = HomeTab.ALL;

        String query = searchQuery.getValue();
        if (query == null) query = "";

        if (tab == HomeTab.TODAY || tab == HomeTab.WEEKEND) {
            displayItems.setValue(buildPlanResults(cats, active, tab, query));
            return;
        }

        boolean fImportant = Boolean.TRUE.equals(filterImportant.getValue());
        boolean fQuick = Boolean.TRUE.equals(filterQuick.getValue());
        boolean fWeekend = Boolean.TRUE.equals(filterWeekend.getValue());
        boolean fWeekday = Boolean.TRUE.equals(filterWeekday.getValue());
        boolean fTimesensitive = Boolean.TRUE.equals(filterTimesensitive.getValue());
        boolean fToday = Boolean.TRUE.equals(filterToday.getValue());
        boolean fTomorrow = Boolean.TRUE.equals(filterTomorrow.getValue());

        if (!query.isEmpty() || fImportant || fQuick || fWeekend || fWeekday || fTimesensitive
                || fToday || fTomorrow) {
            displayItems.setValue(buildFilteredResults(cats, active, completed, query,
                    fImportant, fQuick, fWeekend, fWeekday, fTimesensitive, fToday, fTomorrow));
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

    private List<DisplayItem> buildPlanResults(List<CategoryWithCount> cats, List<Todo> active,
                                               HomeTab tab, String query) {
        List<DisplayItem> items = new ArrayList<>();
        Application app = getApplication();
        Set<Long> claimed = new HashSet<>();

        Set<String> collapsed = collapsedPlanSections.getValue();
        if (collapsed == null) collapsed = new HashSet<>();

        // Most specific first so each task appears in only one section.
        appendPlanSection(items, cats, active, query, claimed, collapsed, tab,
                DisplayItem.PLAN_SECTION_IMPORTANT_DUE,
                app.getString(R.string.plan_section_important_due),
                t -> matchesDayFlag(t, tab) && t.important && t.timesensitive);

        appendPlanSection(items, cats, active, query, claimed, collapsed, tab,
                DisplayItem.PLAN_SECTION_IMPORTANT,
                app.getString(R.string.plan_section_important),
                t -> matchesDayFlag(t, tab) && t.important);

        appendPlanSection(items, cats, active, query, claimed, collapsed, tab,
                DisplayItem.PLAN_SECTION_QUICK,
                app.getString(R.string.plan_section_quick),
                t -> matchesDayFlag(t, tab) && t.quick);

        String allTitle = tab == HomeTab.WEEKEND
                ? app.getString(R.string.plan_section_weekend)
                : app.getString(R.string.plan_section_today);
        appendPlanSection(items, cats, active, query, claimed, collapsed, tab,
                DisplayItem.PLAN_SECTION_ALL, allTitle,
                t -> matchesDayFlag(t, tab));

        return items;
    }

    private interface PlanMatcher {
        boolean matches(Todo todo);
    }

    private void appendPlanSection(List<DisplayItem> items, List<CategoryWithCount> cats,
                                   List<Todo> active, String query, Set<Long> claimed,
                                   Set<String> collapsed, HomeTab tab,
                                   int sectionId, String title, PlanMatcher matcher) {
        List<Todo> matched = new ArrayList<>();
        for (Todo t : active) {
            if (claimed.contains(t.id)) continue;
            if (matcher.matches(t) && matches(t, query)) {
                matched.add(t);
                claimed.add(t.id);
            }
        }
        if (matched.isEmpty()) return;

        boolean expanded = !collapsed.contains(planSectionKey(tab, sectionId));
        items.add(DisplayItem.planSectionHeader(sectionId, title, matched.size(), expanded));
        if (!expanded) return;

        for (Todo t : matched) {
            CategoryWithCount cat = findCategory(cats, t.categoryId);
            String color = cat != null ? cat.colorHex : "#9E9E9E";
            String name = cat != null ? cat.name : "";
            items.add(DisplayItem.todoRow(t, color, name, sectionId));
        }
    }

    private static String planSectionKey(HomeTab tab, int sectionId) {
        return tab.name() + ":" + sectionId;
    }

    public void togglePlanSection(int sectionId) {
        HomeTab tab = homeTab.getValue();
        if (tab != HomeTab.TODAY && tab != HomeTab.WEEKEND) return;
        Set<String> current = collapsedPlanSections.getValue();
        Set<String> collapsed = current == null ? new HashSet<>() : new HashSet<>(current);
        String key = planSectionKey(tab, sectionId);
        if (!collapsed.remove(key)) {
            collapsed.add(key);
        }
        collapsedPlanSections.setValue(collapsed);
    }

    private List<DisplayItem> buildFilteredResults(List<CategoryWithCount> cats, List<Todo> active,
                                                   List<Todo> completed, String query,
                                                   boolean fImportant, boolean fQuick,
                                                   boolean fWeekend, boolean fWeekday,
                                                   boolean fTimesensitive, boolean fToday,
                                                   boolean fTomorrow) {
        List<DisplayItem> items = new ArrayList<>();
        for (Todo t : active) {
            if (matches(t, query) && matchesAttributes(t, fImportant, fQuick, fWeekend, fWeekday,
                    fTimesensitive, fToday, fTomorrow)) {
                CategoryWithCount cat = findCategory(cats, t.categoryId);
                String color = cat != null ? cat.colorHex : "#9E9E9E";
                String name = cat != null ? cat.name : "";
                items.add(DisplayItem.todoRow(t, color, name));
            }
        }
        // Completed todos only appear for text search, not for attribute-only filtering.
        if (!query.isEmpty()) {
            for (Todo t : completed) {
                if (matches(t, query) && matchesAttributes(t, fImportant, fQuick, fWeekend, fWeekday,
                        fTimesensitive, fToday, fTomorrow)) {
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
                                      boolean fWeekend, boolean fWeekday,
                                      boolean fTimesensitive, boolean fToday,
                                      boolean fTomorrow) {
        if (fImportant && !todo.important) return false;
        if (fQuick && !todo.quick) return false;
        if (fWeekend && !todo.weekend) return false;
        if (fWeekday && !todo.weekday) return false;
        if (fTimesensitive && !todo.timesensitive) return false;
        if (fToday && !todo.today) return false;
        return !fTomorrow || todo.tomorrow;
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

    public void updateCategory(long id, String name, String colorHex) {
        repository.updateCategory(id, name, colorHex);
    }

    public void addTodo(long categoryId, String title, String notes, boolean important, boolean quick,
                        boolean weekend, boolean weekday, boolean timesensitive,
                        boolean today, boolean tomorrow) {
        repository.addTodo(categoryId, title, notes, important, quick, weekend, weekday,
                timesensitive, today, tomorrow);
    }

    public void updateTodo(Todo todo) {
        repository.updateTodo(todo);
    }

    public void reorderTodos(List<Long> orderedIds) {
        repository.reorderTodos(orderedIds);
    }

    public void reorderCategories(List<Long> orderedIds) {
        repository.reorderCategories(orderedIds);
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

    public void ensureCategoryExpanded(long categoryId) {
        Set<Long> collapsed = new HashSet<>(collapsedCategoryIds.getValue());
        if (collapsed.remove(categoryId)) {
            collapsedCategoryIds.setValue(collapsed);
        }
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

    public void setViewMode(ViewMode mode) {
        if (mode == null || mode == viewMode.getValue()) return;
        viewMode.setValue(mode);
        if (mode == ViewMode.LIST) {
            setReorderCategoriesMode(false);
        }
        Prefs.setViewMode(getApplication(),
                mode == ViewMode.LIST ? Prefs.VIEW_MODE_LIST : Prefs.VIEW_MODE_GROUPED);
    }

    public void setHomeTab(HomeTab tab) {
        if (tab == null || tab == homeTab.getValue()) return;
        if (tab != HomeTab.ALL) {
            setReorderCategoriesMode(false);
        }
        homeTab.setValue(tab);
        Prefs.setHomeTab(getApplication(), toPrefsHomeTab(tab));
    }

    private static ViewMode loadSavedViewMode(Application application) {
        return Prefs.VIEW_MODE_LIST.equals(Prefs.getViewMode(application))
                ? ViewMode.LIST
                : ViewMode.GROUPED;
    }

    private static HomeTab loadSavedHomeTab(Application application) {
        String saved = Prefs.getHomeTab(application);
        if (Prefs.HOME_TAB_TODAY.equals(saved)) return HomeTab.TODAY;
        if (Prefs.HOME_TAB_WEEKEND.equals(saved)) return HomeTab.WEEKEND;
        return HomeTab.ALL;
    }

    private static String toPrefsHomeTab(HomeTab tab) {
        if (tab == HomeTab.TODAY) return Prefs.HOME_TAB_TODAY;
        if (tab == HomeTab.WEEKEND) return Prefs.HOME_TAB_WEEKEND;
        return Prefs.HOME_TAB_ALL;
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

    public void setFilterTimesensitive(boolean enabled) {
        if (!Boolean.valueOf(enabled).equals(filterTimesensitive.getValue())) {
            filterTimesensitive.setValue(enabled);
        }
    }

    public void setFilterToday(boolean enabled) {
        if (!Boolean.valueOf(enabled).equals(filterToday.getValue())) {
            filterToday.setValue(enabled);
        }
    }

    public void setFilterTomorrow(boolean enabled) {
        if (!Boolean.valueOf(enabled).equals(filterTomorrow.getValue())) {
            filterTomorrow.setValue(enabled);
        }
    }
}
