package com.expense.todoly.ui.dialog;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.expense.todoly.R;
import com.expense.todoly.data.model.CategoryWithCount;
import com.expense.todoly.ui.TodoViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class AddCategoryDialog {

    private static final String[] PALETTE = {
            "#6C5CE7", "#00B894", "#0984E3", "#E17055",
            "#E84393", "#00CEC9", "#FDCB6E", "#D63031"
    };

    public static void show(Context context, TodoViewModel viewModel) {
        open(context, viewModel, null);
    }

    public static void showEdit(Context context, TodoViewModel viewModel, CategoryWithCount category) {
        open(context, viewModel, category);
    }

    private static void open(Context context, TodoViewModel viewModel, CategoryWithCount editing) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_add_category, null, false);
        TextView title = view.findViewById(R.id.dialogTitle);
        TextInputLayout nameLayout = view.findViewById(R.id.nameLayout);
        TextInputEditText nameInput = view.findViewById(R.id.nameInput);
        LinearLayout colorRow = view.findViewById(R.id.colorRow);

        boolean isEdit = editing != null;
        title.setText(isEdit ? R.string.edit_category : R.string.add_category);
        if (isEdit) {
            nameInput.setText(editing.name);
        }

        int initialColorIndex = isEdit ? paletteIndex(editing.colorHex) : 0;
        final String[] selected = {PALETTE[initialColorIndex]};
        final ImageView[] swatches = new ImageView[PALETTE.length];
        int size = dp(context, 44);
        int margin = dp(context, 6);

        for (int i = 0; i < PALETTE.length; i++) {
            final int index = i;
            ImageView swatch = new ImageView(context);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(margin, margin, margin, margin);
            swatch.setLayoutParams(lp);
            swatch.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_circle));
            swatch.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(PALETTE[i])));
            swatch.setPadding(dp(context, 10), dp(context, 10), dp(context, 10), dp(context, 10));
            swatch.setImageResource(R.drawable.ic_done);
            swatch.setImageTintList(ColorStateList.valueOf(Color.WHITE));
            swatch.setOnClickListener(v -> {
                selected[0] = PALETTE[index];
                updateSwatches(swatches, index);
            });
            swatches[i] = swatch;
            colorRow.addView(swatch);
        }
        updateSwatches(swatches, initialColorIndex);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String name = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
                    if (!name.isEmpty()) {
                        if (isEdit) {
                            viewModel.updateCategory(editing.id, name, selected[0]);
                        } else {
                            viewModel.addCategory(name, selected[0]);
                        }
                    }
                });
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
        nameInput.requestFocus();
    }

    private static int paletteIndex(String colorHex) {
        if (colorHex == null) return 0;
        String normalized = colorHex.trim().toUpperCase();
        for (int i = 0; i < PALETTE.length; i++) {
            if (PALETTE[i].equalsIgnoreCase(normalized)) {
                return i;
            }
        }
        return 0;
    }

    private static void updateSwatches(ImageView[] swatches, int selectedIndex) {
        for (int i = 0; i < swatches.length; i++) {
            swatches[i].setImageAlpha(i == selectedIndex ? 255 : 0);
            swatches[i].animate().scaleX(i == selectedIndex ? 1.12f : 1f)
                    .scaleY(i == selectedIndex ? 1.12f : 1f).setDuration(120).start();
        }
    }

    private static int dp(Context context, int value) {
        return Math.round(context.getResources().getDisplayMetrics().density * value);
    }
}
