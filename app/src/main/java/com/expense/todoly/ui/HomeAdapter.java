package com.expense.todoly.ui;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.expense.todoly.R;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.List;

public class HomeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onCategoryHeaderClick(long categoryId);

        void onCategoryLongClick(com.expense.todoly.data.model.CategoryWithCount category);

        void onCompletedHeaderClick();

        void onTodoChecked(DisplayItem item, boolean checked);

        void onTodoClick(DisplayItem item);

        void onTodoLongClick(DisplayItem item);

        void onAddTaskClick(long categoryId);

        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    private final List<DisplayItem> items = new ArrayList<>();
    private final Listener listener;

    public HomeAdapter(Listener listener) {
        this.listener = listener;
        setHasStableIds(true);
    }

    public void submit(List<DisplayItem> newItems) {
        final List<DisplayItem> old = new ArrayList<>(items);
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return old.size();
            }

            @Override
            public int getNewListSize() {
                return newItems.size();
            }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                return old.get(oldPos).stableId() == newItems.get(newPos).stableId();
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                return sameContent(old.get(oldPos), newItems.get(newPos));
            }
        });
        items.clear();
        items.addAll(newItems);
        result.dispatchUpdatesTo(this);
    }

    private boolean sameContent(DisplayItem a, DisplayItem b) {
        if (a.type != b.type) return false;
        switch (a.type) {
            case DisplayItem.TYPE_CATEGORY_HEADER:
                return a.expanded == b.expanded
                        && a.category.name.equals(b.category.name)
                        && a.category.colorHex.equals(b.category.colorHex)
                        && a.category.activeCount == b.category.activeCount
                        && a.category.totalCount == b.category.totalCount;
            case DisplayItem.TYPE_COMPLETED_HEADER:
                return a.expanded == b.expanded && a.completedCount == b.completedCount;
            case DisplayItem.TYPE_ADD_TASK:
                return a.addCategoryId == b.addCategoryId && a.categoryColor.equals(b.categoryColor);
            case DisplayItem.TYPE_TODO_ROW:
            case DisplayItem.TYPE_COMPLETED_ROW:
                return a.todo.title.equals(b.todo.title)
                        && a.todo.isCompleted == b.todo.isCompleted
                        && a.todo.important == b.todo.important
                        && a.todo.quick == b.todo.quick
                        && a.todo.weekend == b.todo.weekend
                        && a.todo.weekday == b.todo.weekday
                        && a.todo.timesensitive == b.todo.timesensitive
                        && a.categoryColor.equals(b.categoryColor)
                        && equalsNullable(a.categoryName, b.categoryName);
            default:
                return true;
        }
    }

    private boolean equalsNullable(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    public DisplayItem itemAt(int position) {
        if (position < 0 || position >= items.size()) return null;
        return items.get(position);
    }

    public void moveItem(int from, int to) {
        if (from < 0 || to < 0 || from >= items.size() || to >= items.size()) return;
        DisplayItem moved = items.remove(from);
        items.add(to, moved);
        notifyItemMoved(from, to);
    }

    public List<Long> orderedTodoIdsForCategory(long categoryId) {
        List<Long> ids = new ArrayList<>();
        for (DisplayItem item : items) {
            if (item.type == DisplayItem.TYPE_TODO_ROW && item.todo != null
                    && item.todo.categoryId == categoryId) {
                ids.add(item.todo.id);
            }
        }
        return ids;
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).stableId();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case DisplayItem.TYPE_CATEGORY_HEADER:
                return new CategoryHeaderVH(inflater.inflate(R.layout.item_category_header, parent, false));
            case DisplayItem.TYPE_COMPLETED_HEADER:
                return new CompletedHeaderVH(inflater.inflate(R.layout.item_completed_header, parent, false));
            case DisplayItem.TYPE_ADD_TASK:
                return new AddTaskVH(inflater.inflate(R.layout.item_add_task, parent, false));
            default:
                return new TodoVH(inflater.inflate(R.layout.item_todo, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DisplayItem item = items.get(position);
        if (holder instanceof CategoryHeaderVH) {
            ((CategoryHeaderVH) holder).bind(item);
        } else if (holder instanceof CompletedHeaderVH) {
            ((CompletedHeaderVH) holder).bind(item);
        } else if (holder instanceof AddTaskVH) {
            ((AddTaskVH) holder).bind(item);
        } else {
            ((TodoVH) holder).bind(item);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static int parseColor(String hex) {
        try {
            return Color.parseColor(hex);
        } catch (Exception e) {
            return Color.parseColor("#9E9E9E");
        }
    }

    class CategoryHeaderVH extends RecyclerView.ViewHolder {
        final View colorDot;
        final TextView name;
        final TextView badge;
        final ImageView chevron;

        CategoryHeaderVH(@NonNull View itemView) {
            super(itemView);
            colorDot = itemView.findViewById(R.id.colorDot);
            name = itemView.findViewById(R.id.categoryName);
            badge = itemView.findViewById(R.id.countBadge);
            chevron = itemView.findViewById(R.id.chevron);
        }

        void bind(DisplayItem item) {
            name.setText(item.category.name);
            colorDot.setBackgroundTintList(ColorStateList.valueOf(parseColor(item.category.colorHex)));
            badge.setText(item.category.activeCount + "/" + item.category.totalCount);
            chevron.setRotation(item.expanded ? 0f : -90f);
            itemView.setOnClickListener(v -> listener.onCategoryHeaderClick(item.category.id));
            itemView.setOnLongClickListener(v -> {
                listener.onCategoryLongClick(item.category);
                return true;
            });
        }
    }

    class CompletedHeaderVH extends RecyclerView.ViewHolder {
        final TextView badge;
        final ImageView chevron;

        CompletedHeaderVH(@NonNull View itemView) {
            super(itemView);
            badge = itemView.findViewById(R.id.countBadge);
            chevron = itemView.findViewById(R.id.chevron);
        }

        void bind(DisplayItem item) {
            badge.setText(String.valueOf(item.completedCount));
            chevron.setRotation(item.expanded ? 0f : -90f);
            itemView.setOnClickListener(v -> listener.onCompletedHeaderClick());
        }
    }

    class AddTaskVH extends RecyclerView.ViewHolder {
        final ImageView addIcon;

        AddTaskVH(@NonNull View itemView) {
            super(itemView);
            addIcon = itemView.findViewById(R.id.addIcon);
        }

        void bind(DisplayItem item) {
            addIcon.setImageTintList(ColorStateList.valueOf(parseColor(item.categoryColor)));
            itemView.setOnClickListener(v -> listener.onAddTaskClick(item.addCategoryId));
        }
    }

    class TodoVH extends RecyclerView.ViewHolder {
        final View colorStrip;
        final MaterialCheckBox checkbox;
        final TextView title;
        final TextView subtitle;
        final ImageView iconImportant;
        final ImageView iconQuick;
        final ImageView iconWeekend;
        final ImageView iconWeekday;
        final ImageView iconTimesensitive;
        final ImageView dragHandle;

        TodoVH(@NonNull View itemView) {
            super(itemView);
            colorStrip = itemView.findViewById(R.id.colorStrip);
            checkbox = itemView.findViewById(R.id.checkbox);
            title = itemView.findViewById(R.id.title);
            subtitle = itemView.findViewById(R.id.subtitle);
            iconImportant = itemView.findViewById(R.id.iconImportant);
            iconQuick = itemView.findViewById(R.id.iconQuick);
            iconWeekend = itemView.findViewById(R.id.iconWeekend);
            iconWeekday = itemView.findViewById(R.id.iconWeekday);
            iconTimesensitive = itemView.findViewById(R.id.iconTimesensitive);
            dragHandle = itemView.findViewById(R.id.dragHandle);
        }

        @SuppressLint("ClickableViewAccessibility")
        void bind(DisplayItem item) {
            title.setText(item.todo.title);
            colorStrip.setBackgroundTintList(ColorStateList.valueOf(parseColor(item.categoryColor)));

            iconImportant.setVisibility(item.todo.important ? View.VISIBLE : View.GONE);
            iconQuick.setVisibility(item.todo.quick ? View.VISIBLE : View.GONE);
            iconWeekend.setVisibility(item.todo.weekend ? View.VISIBLE : View.GONE);
            iconWeekday.setVisibility(item.todo.weekday ? View.VISIBLE : View.GONE);
            iconTimesensitive.setVisibility(item.todo.timesensitive ? View.VISIBLE : View.GONE);

            boolean reorderable = item.type == DisplayItem.TYPE_TODO_ROW;
            dragHandle.setVisibility(reorderable ? View.VISIBLE : View.GONE);
            if (reorderable) {
                dragHandle.setOnTouchListener((v, event) -> {
                    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                        listener.onStartDrag(this);
                    }
                    return false;
                });
            } else {
                dragHandle.setOnTouchListener(null);
            }

            if (item.categoryName != null && !item.categoryName.isEmpty()) {
                subtitle.setVisibility(View.VISIBLE);
                subtitle.setText(item.categoryName);
            } else {
                subtitle.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> listener.onTodoClick(item));
            itemView.setOnLongClickListener(v -> {
                listener.onTodoLongClick(item);
                return true;
            });

            checkbox.setOnCheckedChangeListener(null);
            checkbox.setChecked(item.todo.isCompleted);

            if (item.todo.isCompleted) {
                title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                itemView.setAlpha(0.55f);
            } else {
                title.setPaintFlags(title.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                itemView.setAlpha(1f);
            }

            checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked != item.todo.isCompleted) {
                    listener.onTodoChecked(item, isChecked);
                }
            });
        }
    }
}
