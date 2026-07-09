package com.expense.todoly.ui;

/**
 * Wraps a value that represents a one-time action, so LiveData observers don't
 * re-trigger it on configuration changes (e.g. rotation).
 */
public class Event<T> {

    private final T content;
    private boolean handled = false;

    public Event(T content) {
        this.content = content;
    }

    /**
     * Returns the content the first time it is called, then null on subsequent calls.
     */
    public T getIfNotHandled() {
        if (handled) {
            return null;
        }
        handled = true;
        return content;
    }
}
