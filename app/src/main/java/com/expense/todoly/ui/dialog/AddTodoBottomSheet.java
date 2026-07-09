package com.expense.todoly.ui.dialog;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.expense.todoly.R;
import com.expense.todoly.data.entity.Category;
import com.expense.todoly.data.entity.Todo;
import com.expense.todoly.ui.TodoViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class AddTodoBottomSheet {

    public static void show(Context context, TodoViewModel viewModel, List<Category> categories) {
        show(context, viewModel, categories, -1L);
    }

    public static void show(Context context, TodoViewModel viewModel, List<Category> categories,
                            long preselectedCategoryId) {
        open(context, viewModel, categories, preselectedCategoryId, null);
    }

    public static void showEdit(Context context, TodoViewModel viewModel, List<Category> categories,
                                Todo editing) {
        open(context, viewModel, categories, editing == null ? -1L : editing.categoryId, editing);
    }

    private static void open(Context context, TodoViewModel viewModel, List<Category> categories,
                             long preselectedCategoryId, Todo editing) {
        if (categories == null || categories.isEmpty()) {
            Toast.makeText(context, R.string.need_category_first, Toast.LENGTH_SHORT).show();
            AddCategoryDialog.show(context, viewModel);
            return;
        }

        View view = LayoutInflater.from(context).inflate(R.layout.sheet_add_todo, null, false);
        TextView sheetTitle = view.findViewById(R.id.sheetTitle);
        TextInputEditText titleInput = view.findViewById(R.id.titleInput);
        TextInputEditText notesInput = view.findViewById(R.id.notesInput);
        ChipGroup chipGroup = view.findViewById(R.id.categoryChips);
        Chip chipImportant = view.findViewById(R.id.chipImportant);
        Chip chipQuick = view.findViewById(R.id.chipQuick);
        Chip chipWeekend = view.findViewById(R.id.chipWeekend);
        Chip chipWeekday = view.findViewById(R.id.chipWeekday);
        Chip chipTimesensitive = view.findViewById(R.id.chipTimesensitive);
        MaterialButton saveButton = view.findViewById(R.id.saveButton);
        MaterialButton cancelButton = view.findViewById(R.id.cancelButton);
        MaterialCheckBox keepAddingCheck = view.findViewById(R.id.keepAddingCheck);

        final boolean isEdit = editing != null;
        sheetTitle.setText(isEdit ? R.string.edit_todo : R.string.add_todo);
        keepAddingCheck.setVisibility(isEdit ? View.GONE : View.VISIBLE);

        if (isEdit) {
            titleInput.setText(editing.title);
            notesInput.setText(editing.notes);
            chipImportant.setChecked(editing.important);
            chipQuick.setChecked(editing.quick);
            chipWeekend.setChecked(editing.weekend);
            chipWeekday.setChecked(editing.weekday);
            chipTimesensitive.setChecked(editing.timesensitive);
        }

        int preselectedChipId = View.NO_ID;
        for (Category category : categories) {
            Chip chip = new Chip(context);
            chip.setText(category.name);
            chip.setCheckable(true);
            chip.setId(View.generateViewId());
            chip.setTag(category.id);
            int color = parseColor(category.colorHex);
            chip.setChipBackgroundColor(new ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                    new int[]{color, adjustAlpha(color, 0.14f)}));
            chip.setChipStrokeWidth(0f);
            chip.setTextColor(new ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                    new int[]{Color.WHITE, context.getColor(R.color.on_surface)}));
            chipGroup.addView(chip);
            if (category.id == preselectedCategoryId) {
                preselectedChipId = chip.getId();
            }
        }
        if (preselectedChipId != View.NO_ID) {
            chipGroup.check(preselectedChipId);
        } else if (chipGroup.getChildCount() > 0) {
            chipGroup.check(chipGroup.getChildAt(0).getId());
        }

        final BottomSheetDialog dialog = new BottomSheetDialog(context);
        dialog.setContentView(view);

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        saveButton.setOnClickListener(v -> {
            String title = titleInput.getText() == null ? "" : titleInput.getText().toString().trim();
            if (title.isEmpty()) {
                titleInput.setError(context.getString(R.string.todo_title_hint));
                return;
            }
            int checkedId = chipGroup.getCheckedChipId();
            if (checkedId == View.NO_ID) {
                Toast.makeText(context, R.string.select_category, Toast.LENGTH_SHORT).show();
                return;
            }
            Chip checked = view.findViewById(checkedId);
            long categoryId = (long) checked.getTag();
            String notesText = notesInput.getText() == null ? "" : notesInput.getText().toString().trim();
            String notes = notesText.isEmpty() ? null : notesText;
            boolean important = chipImportant.isChecked();
            boolean quick = chipQuick.isChecked();
            boolean weekend = chipWeekend.isChecked();
            boolean weekday = chipWeekday.isChecked();
            boolean timesensitive = chipTimesensitive.isChecked();

            if (isEdit) {
                editing.title = title;
                editing.notes = notes;
                editing.categoryId = categoryId;
                editing.important = important;
                editing.quick = quick;
                editing.weekend = weekend;
                editing.weekday = weekday;
                editing.timesensitive = timesensitive;
                viewModel.updateTodo(editing);
                dialog.dismiss();
                return;
            }

            viewModel.addTodo(categoryId, title, notes, important, quick, weekend, weekday, timesensitive);
            if (keepAddingCheck.isChecked()) {
                titleInput.setText("");
                titleInput.setError(null);
                notesInput.setText("");
                chipImportant.setChecked(false);
                chipQuick.setChecked(false);
                chipWeekend.setChecked(false);
                chipWeekday.setChecked(false);
                chipTimesensitive.setChecked(false);
                titleInput.requestFocus();
            } else {
                dialog.dismiss();
            }
        });

        dialog.show();
        titleInput.requestFocus();
    }

    private static int parseColor(String hex) {
        try {
            return Color.parseColor(hex);
        } catch (Exception e) {
            return Color.parseColor("#9E9E9E");
        }
    }

    private static int adjustAlpha(int color, float factor) {
        int alpha = Math.round(255 * factor);
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }
}
