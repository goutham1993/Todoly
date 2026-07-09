package com.expense.todoly.widget;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Tracks todos mid-completion so the widget can show a strike-through before removal.
 */
public final class WidgetCompletionTracker {

    private static final Object LOCK = new Object();
    private static final Set<Long> COMPLETING = new HashSet<>();

    private WidgetCompletionTracker() {
    }

    public static void markCompleting(long todoId) {
        synchronized (LOCK) {
            COMPLETING.add(todoId);
        }
    }

    public static boolean isCompleting(long todoId) {
        synchronized (LOCK) {
            return COMPLETING.contains(todoId);
        }
    }

    public static void clear(long todoId) {
        synchronized (LOCK) {
            COMPLETING.remove(todoId);
        }
    }

    public static Set<Long> snapshot() {
        synchronized (LOCK) {
            return Collections.unmodifiableSet(new HashSet<>(COMPLETING));
        }
    }
}
