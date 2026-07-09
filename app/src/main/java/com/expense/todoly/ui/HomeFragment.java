package com.expense.todoly.ui;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.expense.todoly.R;
import com.expense.todoly.data.entity.Category;
import com.expense.todoly.data.entity.Todo;
import com.expense.todoly.ui.dialog.AddTodoBottomSheet;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import nl.dionsegijn.konfetti.core.Party;
import nl.dionsegijn.konfetti.core.PartyFactory;
import nl.dionsegijn.konfetti.core.Position;
import nl.dionsegijn.konfetti.core.emitter.Emitter;
import nl.dionsegijn.konfetti.core.emitter.EmitterConfig;
import nl.dionsegijn.konfetti.xml.KonfettiView;

public class HomeFragment extends Fragment {

    private TodoViewModel viewModel;
    private HomeAdapter adapter;
    private RecyclerView recycler;
    private View emptyState;
    private TextView pendingCount;
    private TextView completedCount;
    private Chip chipFilterImportant;
    private Chip chipFilterQuick;
    private Chip chipFilterWeekend;
    private Chip chipFilterWeekday;
    private Chip chipFilterTimesensitive;
    private Chip chipFilterToday;
    private Chip chipFilterTomorrow;
    private ItemTouchHelper touchHelper;
    private final List<Category> categories = new ArrayList<>();
    private final Random random = new Random();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(TodoViewModel.class);

        recycler = view.findViewById(R.id.recycler);
        emptyState = view.findViewById(R.id.emptyState);
        pendingCount = view.findViewById(R.id.pendingCount);
        completedCount = view.findViewById(R.id.completedCount);
        chipFilterImportant = view.findViewById(R.id.chipFilterImportant);
        chipFilterQuick = view.findViewById(R.id.chipFilterQuick);
        chipFilterWeekend = view.findViewById(R.id.chipFilterWeekend);
        chipFilterWeekday = view.findViewById(R.id.chipFilterWeekday);
        chipFilterTimesensitive = view.findViewById(R.id.chipFilterTimesensitive);
        chipFilterToday = view.findViewById(R.id.chipFilterToday);
        chipFilterTomorrow = view.findViewById(R.id.chipFilterTomorrow);

        adapter = new HomeAdapter(new HomeAdapter.Listener() {
            @Override
            public void onCategoryHeaderClick(long categoryId) {
                viewModel.toggleCategoryExpanded(categoryId);
            }

            @Override
            public void onCompletedHeaderClick() {
                viewModel.toggleCompletedExpanded();
            }

            @Override
            public void onCategoryLongClick(com.expense.todoly.data.model.CategoryWithCount category) {
                confirmDeleteCategory(category);
            }

            @Override
            public void onTodoChecked(DisplayItem item, boolean checked) {
                viewModel.toggleComplete(item.todo, checked);
            }

            @Override
            public void onTodoClick(DisplayItem item) {
                AddTodoBottomSheet.showEdit(requireContext(), viewModel,
                        new ArrayList<>(categories), item.todo);
            }

            @Override
            public void onTodoLongClick(DisplayItem item) {
                confirmDeleteTodo(item.todo);
            }

            @Override
            public void onAddTaskClick(long categoryId) {
                AddTodoBottomSheet.show(requireContext(), viewModel,
                        new ArrayList<>(categories), categoryId);
            }

            @Override
            public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
                if (touchHelper != null) {
                    touchHelper.startDrag(viewHolder);
                }
            }
        });

        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);
        RecyclerView.ItemAnimator animator = recycler.getItemAnimator();
        if (animator instanceof androidx.recyclerview.widget.SimpleItemAnimator) {
            ((androidx.recyclerview.widget.SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

        attachSwipe();
        setupFilters();

        viewModel.getCategories().observe(getViewLifecycleOwner(), list -> {
            categories.clear();
            if (list != null) categories.addAll(list);
        });

        viewModel.getDisplayItems().observe(getViewLifecycleOwner(), items -> {
            adapter.submit(items);
            emptyState.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.getPendingCount().observe(getViewLifecycleOwner(), count ->
                pendingCount.setText(String.valueOf(count == null ? 0 : count)));

        viewModel.getCompletedCount().observe(getViewLifecycleOwner(), count ->
                completedCount.setText(String.valueOf(count == null ? 0 : count)));

        viewModel.getCelebrationEvent().observe(getViewLifecycleOwner(), event -> {
            if (event == null) return;
            Integer streak = event.getIfNotHandled();
            if (streak != null) celebrate(streak);
        });
    }

    private void celebrate(int streak) {
        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_celebration, (ViewGroup) requireView(), false);

        String[] emojis = getResources().getStringArray(R.array.celebration_emojis);
        String[] messages = getResources().getStringArray(R.array.streak_messages);

        TextView emoji = content.findViewById(R.id.celebrationEmoji);
        TextView title = content.findViewById(R.id.celebrationTitle);
        TextView message = content.findViewById(R.id.celebrationMessage);
        View card = content.findViewById(R.id.celebrationCard);
        final KonfettiView konfetti = content.findViewById(R.id.celebrationKonfetti);

        if (emojis.length > 0) emoji.setText(emojis[random.nextInt(emojis.length)]);
        if (messages.length > 0) title.setText(messages[random.nextInt(messages.length)]);
        message.setText(getString(R.string.celebration_subtitle, streak));

        final androidx.appcompat.app.AlertDialog dialog =
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setView(content)
                        .setCancelable(true)
                        .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        }

        content.findViewById(R.id.celebrationButton).setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        card.setScaleX(0.7f);
        card.setScaleY(0.7f);
        card.setAlpha(0f);
        card.animate()
                .scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(320L)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.4f))
                .start();

        konfetti.post(() -> {
            EmitterConfig emitterConfig = new Emitter(300L, TimeUnit.MILLISECONDS).max(120);
            Party party = new PartyFactory(emitterConfig)
                    .spread(360)
                    .colors(Arrays.asList(0xFFFCE18A, 0xFFFF726D, 0xFFB48DEF, 0xFFF4306D, 0xFF2F85FF))
                    .setSpeedBetween(0f, 30f)
                    .position(new Position.Relative(0.5, 0.3))
                    .build();
            konfetti.start(party);
        });
    }

    private void setupFilters() {
        chipFilterImportant.setOnCheckedChangeListener((btn, checked) ->
                viewModel.setFilterImportant(checked));
        chipFilterQuick.setOnCheckedChangeListener((btn, checked) ->
                viewModel.setFilterQuick(checked));
        chipFilterWeekend.setOnCheckedChangeListener((btn, checked) -> {
            if (checked && chipFilterWeekday.isChecked()) chipFilterWeekday.setChecked(false);
            viewModel.setFilterWeekend(checked);
        });
        chipFilterWeekday.setOnCheckedChangeListener((btn, checked) -> {
            if (checked && chipFilterWeekend.isChecked()) chipFilterWeekend.setChecked(false);
            viewModel.setFilterWeekday(checked);
        });
        chipFilterTimesensitive.setOnCheckedChangeListener((btn, checked) ->
                viewModel.setFilterTimesensitive(checked));
        chipFilterToday.setOnCheckedChangeListener((btn, checked) ->
                viewModel.setFilterToday(checked));
        chipFilterTomorrow.setOnCheckedChangeListener((btn, checked) ->
                viewModel.setFilterTomorrow(checked));

        viewModel.getFilterImportant().observe(getViewLifecycleOwner(), enabled -> {
            boolean on = Boolean.TRUE.equals(enabled);
            if (chipFilterImportant.isChecked() != on) chipFilterImportant.setChecked(on);
        });
        viewModel.getFilterQuick().observe(getViewLifecycleOwner(), enabled -> {
            boolean on = Boolean.TRUE.equals(enabled);
            if (chipFilterQuick.isChecked() != on) chipFilterQuick.setChecked(on);
        });
        viewModel.getFilterWeekend().observe(getViewLifecycleOwner(), enabled -> {
            boolean on = Boolean.TRUE.equals(enabled);
            if (chipFilterWeekend.isChecked() != on) chipFilterWeekend.setChecked(on);
        });
        viewModel.getFilterWeekday().observe(getViewLifecycleOwner(), enabled -> {
            boolean on = Boolean.TRUE.equals(enabled);
            if (chipFilterWeekday.isChecked() != on) chipFilterWeekday.setChecked(on);
        });
        viewModel.getFilterTimesensitive().observe(getViewLifecycleOwner(), enabled -> {
            boolean on = Boolean.TRUE.equals(enabled);
            if (chipFilterTimesensitive.isChecked() != on) chipFilterTimesensitive.setChecked(on);
        });
        viewModel.getFilterToday().observe(getViewLifecycleOwner(), enabled -> {
            boolean on = Boolean.TRUE.equals(enabled);
            if (chipFilterToday.isChecked() != on) chipFilterToday.setChecked(on);
        });
        viewModel.getFilterTomorrow().observe(getViewLifecycleOwner(), enabled -> {
            boolean on = Boolean.TRUE.equals(enabled);
            if (chipFilterTomorrow.isChecked() != on) chipFilterTomorrow.setChecked(on);
        });
    }

    private void attachSwipe() {
        final Drawable doneIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_done);
        final Drawable deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete);
        final int completeColor = ContextCompat.getColor(requireContext(), R.color.success);
        final int deleteColor = ContextCompat.getColor(requireContext(), R.color.danger);
        final float radius = getResources().getDisplayMetrics().density * 16f;
        final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);

        touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            private boolean reordered = false;
            private long dragCategoryId = -1L;

            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }

            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) {
                DisplayItem from = adapter.itemAt(vh.getBindingAdapterPosition());
                DisplayItem to = adapter.itemAt(target.getBindingAdapterPosition());
                if (from == null || to == null || from.todo == null || to.todo == null) return false;
                if (from.type != DisplayItem.TYPE_TODO_ROW || to.type != DisplayItem.TYPE_TODO_ROW) {
                    return false;
                }
                if (from.todo.categoryId != to.todo.categoryId) return false;
                adapter.moveItem(vh.getBindingAdapterPosition(), target.getBindingAdapterPosition());
                reordered = true;
                dragCategoryId = from.todo.categoryId;
                return true;
            }

            @Override
            public boolean canDropOver(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder current,
                                       @NonNull RecyclerView.ViewHolder target) {
                DisplayItem from = adapter.itemAt(current.getBindingAdapterPosition());
                DisplayItem to = adapter.itemAt(target.getBindingAdapterPosition());
                return from != null && to != null && from.todo != null && to.todo != null
                        && to.type == DisplayItem.TYPE_TODO_ROW
                        && from.todo.categoryId == to.todo.categoryId;
            }

            @Override
            public void clearView(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
                super.clearView(rv, vh);
                if (reordered && dragCategoryId != -1L) {
                    viewModel.reorderTodos(adapter.orderedTodoIdsForCategory(dragCategoryId));
                }
                reordered = false;
                dragCategoryId = -1L;
            }

            @Override
            public int getMovementFlags(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
                int type = vh.getItemViewType();
                if (type == DisplayItem.TYPE_TODO_ROW) {
                    return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                            ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
                }
                if (type == DisplayItem.TYPE_COMPLETED_ROW) {
                    return makeMovementFlags(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
                }
                return 0;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
                int pos = vh.getBindingAdapterPosition();
                DisplayItem item = adapter.itemAt(pos);
                if (item == null || item.todo == null) {
                    adapter.notifyItemChanged(pos);
                    return;
                }
                final Todo todo = item.todo;
                if (direction == ItemTouchHelper.RIGHT) {
                    viewModel.toggleComplete(todo, !todo.isCompleted);
                } else {
                    viewModel.deleteTodo(todo);
                    Snackbar.make(requireView(), R.string.task_deleted, Snackbar.LENGTH_LONG)
                            .setAction(R.string.undo, v ->
                                    viewModel.addTodo(todo.categoryId, todo.title, todo.notes,
                                            todo.important, todo.quick, todo.weekend, todo.weekday,
                                            todo.timesensitive, todo.today, todo.tomorrow))
                            .show();
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv,
                                    @NonNull RecyclerView.ViewHolder vh, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                View v = vh.itemView;
                if (Math.abs(dX) > 0.5f) {
                    boolean swipingRight = dX > 0;
                    paint.setColor(swipingRight ? completeColor : deleteColor);
                    RectF bg = new RectF(v.getLeft() + dpToPx(16), v.getTop() + dpToPx(6),
                            v.getRight() - dpToPx(16), v.getBottom());
                    c.drawRoundRect(bg, radius, radius, paint);

                    Drawable icon = swipingRight ? doneIcon : deleteIcon;
                    if (icon != null) {
                        int size = dpToPx(24);
                        int cy = (int) (bg.top + (bg.height() - size) / 2f);
                        if (swipingRight) {
                            int left = (int) bg.left + dpToPx(20);
                            icon.setBounds(left, cy, left + size, cy + size);
                        } else {
                            int right = (int) bg.right - dpToPx(20);
                            icon.setBounds(right - size, cy, right, cy + size);
                        }
                        icon.draw(c);
                    }
                }
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive);
            }
        });
        touchHelper.attachToRecyclerView(recycler);
    }

    private void confirmDeleteTodo(final Todo todo) {
        if (todo == null) return;
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_todo)
                .setMessage(getString(R.string.delete_todo_message, todo.title))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    viewModel.deleteTodo(todo);
                    Snackbar.make(requireView(), R.string.task_deleted, Snackbar.LENGTH_LONG)
                            .setAction(R.string.undo, v ->
                                    viewModel.addTodo(todo.categoryId, todo.title, todo.notes,
                                            todo.important, todo.quick, todo.weekend, todo.weekday,
                                            todo.timesensitive, todo.today, todo.tomorrow))
                            .show();
                })
                .show();
    }

    private void confirmDeleteCategory(com.expense.todoly.data.model.CategoryWithCount category) {
        String message = category.totalCount > 0
                ? getString(R.string.delete_category_with_todos, category.name, category.totalCount)
                : getString(R.string.delete_category_empty, category.name);
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_category)
                .setMessage(message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, w) -> viewModel.deleteCategory(category.id))
                .show();
    }

    private int dpToPx(int dp) {
        return Math.round(getResources().getDisplayMetrics().density * dp);
    }
}
