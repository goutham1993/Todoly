package com.expense.todoly.data;

import android.content.Context;

import com.expense.todoly.data.dao.TodoDao;

import java.util.Calendar;

public final class DayRollover {

    private DayRollover() {
    }

    public static int currentDayEpoch() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Runs once per calendar day on first app/widget touch: promotes tomorrow-only tasks
     * to today, then clears stale tomorrow flags. Dual-tagged tasks keep today=true.
     */
    public static boolean runIfNeeded(Context context, TodoDao todoDao) {
        int today = currentDayEpoch();
        int last = Prefs.getLastRolloverDay(context);
        if (last == today) {
            return false;
        }
        todoDao.promoteTomorrowOnlyToToday();
        todoDao.clearTomorrowFlags();
        Prefs.setLastRolloverDay(context, today);
        return true;
    }
}
