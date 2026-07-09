package com.expense.todoly;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.expense.todoly.data.entity.Category;
import com.expense.todoly.databinding.ActivityMainBinding;
import com.expense.todoly.ui.TodoViewModel;
import com.expense.todoly.ui.dialog.AddCategoryDialog;
import com.expense.todoly.ui.dialog.AddTodoBottomSheet;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private TodoViewModel viewModel;

    private boolean fabOpen = false;
    private boolean allExpanded = true;
    private final List<Category> categories = new ArrayList<>();

    private MenuItem expandItem;
    private MenuItem viewItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }

        viewModel = new ViewModelProvider(this).get(TodoViewModel.class);

        viewModel.getCategories().observe(this, list -> {
            categories.clear();
            if (list != null) categories.addAll(list);
        });

        viewModel.getViewMode().observe(this, mode -> updateViewIcon(mode));

        binding.fab.setOnClickListener(v -> toggleFab());
        binding.scrim.setOnClickListener(v -> closeFab());

        binding.fabAddCategory.setOnClickListener(v -> {
            closeFab();
            AddCategoryDialog.show(this, viewModel);
        });
        binding.fabAddTodo.setOnClickListener(v -> {
            closeFab();
            AddTodoBottomSheet.show(this, viewModel, new ArrayList<>(categories));
        });
    }

    private void toggleFab() {
        if (fabOpen) {
            closeFab();
        } else {
            openFab();
        }
    }

    private void openFab() {
        fabOpen = true;
        binding.scrim.setVisibility(View.VISIBLE);
        binding.scrim.setAlpha(0f);
        binding.scrim.animate().alpha(1f).setDuration(180).start();

        binding.speedDial.setVisibility(View.VISIBLE);
        binding.speedDial.setAlpha(0f);
        binding.speedDial.setTranslationY(dp(24));
        binding.speedDial.animate().alpha(1f).translationY(0f).setDuration(200).start();

        binding.fab.animate().rotation(45f).setDuration(200).start();
    }

    private void closeFab() {
        if (!fabOpen) return;
        fabOpen = false;
        binding.scrim.animate().alpha(0f).setDuration(160)
                .withEndAction(() -> binding.scrim.setVisibility(View.GONE)).start();

        binding.speedDial.animate().alpha(0f).translationY(dp(24)).setDuration(160)
                .withEndAction(() -> binding.speedDial.setVisibility(View.GONE)).start();

        binding.fab.animate().rotation(0f).setDuration(200).start();
    }

    @Override
    public void onBackPressed() {
        if (fabOpen) {
            closeFab();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        expandItem = menu.findItem(R.id.action_toggle_expand);
        viewItem = menu.findItem(R.id.action_toggle_view);
        setupSearch(menu.findItem(R.id.action_search));
        updateExpandIcon();
        updateViewIcon(viewModel.getViewMode().getValue());
        return true;
    }

    private void setupSearch(MenuItem searchItem) {
        androidx.appcompat.widget.SearchView searchView =
                (androidx.appcompat.widget.SearchView) searchItem.getActionView();
        searchView.setQueryHint(getString(R.string.search_hint));
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                viewModel.setSearchQuery(newText);
                return true;
            }
        });
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                viewModel.setSearchQuery("");
                return true;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_toggle_expand) {
            if (allExpanded) {
                viewModel.collapseAll();
                allExpanded = false;
            } else {
                viewModel.expandAll();
                allExpanded = true;
            }
            updateExpandIcon();
            return true;
        } else if (id == R.id.action_toggle_view) {
            viewModel.toggleViewMode();
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateExpandIcon() {
        if (expandItem == null) return;
        expandItem.setIcon(allExpanded ? R.drawable.ic_collapse_all : R.drawable.ic_expand_all);
        expandItem.setTitle(allExpanded ? R.string.collapse_all : R.string.expand_all);
    }

    private void updateViewIcon(TodoViewModel.ViewMode mode) {
        if (viewItem == null || mode == null) return;
        boolean list = mode == TodoViewModel.ViewMode.LIST;
        viewItem.setIcon(list ? R.drawable.ic_view_grouped : R.drawable.ic_view_list);
        boolean expandVisible = mode == TodoViewModel.ViewMode.GROUPED;
        if (expandItem != null) expandItem.setVisible(expandVisible);
    }

    private float dp(int value) {
        return getResources().getDisplayMetrics().density * value;
    }
}
