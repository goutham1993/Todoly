package com.expense.todoly;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.core.widget.NestedScrollView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Guards the add-task sheet layout so Cancel/Save stay pinned outside the scroll area
 * on short viewports (the regression that hid actions on compact phones).
 */
@RunWith(AndroidJUnit4.class)
public class AddTodoSheetLayoutTest {

    @Test
    public void actionsStayPinnedOutsideScrollOnShortViewport() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Context context = new ContextThemeWrapper(appContext, R.style.Theme_Todoly);
        float density = context.getResources().getDisplayMetrics().density;
        int width = (int) (360 * density);
        int height = (int) (480 * density);

        FrameLayout host = new FrameLayout(context);
        View sheet = LayoutInflater.from(context).inflate(R.layout.sheet_add_todo, host, false);
        host.addView(sheet, new FrameLayout.LayoutParams(width, height));

        host.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        host.layout(0, 0, width, height);

        NestedScrollView scroll = sheet.findViewById(R.id.sheetScroll);
        View actions = sheet.findViewById(R.id.sheetActions);
        View saveButton = sheet.findViewById(R.id.saveButton);
        View cancelButton = sheet.findViewById(R.id.cancelButton);
        View titleInput = sheet.findViewById(R.id.titleInput);
        View keepAdding = sheet.findViewById(R.id.keepAddingCheck);

        assertNotNull(scroll);
        assertNotNull(actions);
        assertNotNull(saveButton);
        assertNotNull(cancelButton);
        assertNotNull(titleInput);
        assertNotNull(keepAdding);

        // Form fields scroll; action row is a direct sibling of NestedScrollView.
        assertSame(sheet, actions.getParent());
        assertSame(sheet, scroll.getParent());
        assertTrue(isDescendant(scroll, titleInput));
        assertTrue(isDescendant(scroll, keepAdding));
        assertTrue(!isDescendant(scroll, actions));

        assertEquals(View.VISIBLE, saveButton.getVisibility());
        assertEquals(View.VISIBLE, cancelButton.getVisibility());
        assertTrue("Save should be laid out within the short viewport",
                saveButton.getBottom() <= height);
        assertTrue("Cancel should be laid out within the short viewport",
                cancelButton.getBottom() <= height);
        assertTrue("Actions should sit below the scroll region",
                actions.getTop() >= scroll.getBottom());
        assertTrue("Scroll region should receive remaining height",
                scroll.getHeight() > 0);
    }

    private static boolean isDescendant(View ancestor, View child) {
        View current = child;
        while (current != null) {
            if (current == ancestor) {
                return true;
            }
            if (!(current.getParent() instanceof View)) {
                return false;
            }
            current = (View) current.getParent();
        }
        return false;
    }
}
