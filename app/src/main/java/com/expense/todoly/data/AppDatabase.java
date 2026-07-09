package com.expense.todoly.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.expense.todoly.data.dao.CategoryDao;
import com.expense.todoly.data.dao.TodoDao;
import com.expense.todoly.data.entity.Category;
import com.expense.todoly.data.entity.Todo;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Category.class, Todo.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract CategoryDao categoryDao();

    public abstract TodoDao todoDao();

    private static volatile AppDatabase INSTANCE;

    public static final ExecutorService IO_EXECUTOR = Executors.newFixedThreadPool(4);

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE todos ADD COLUMN weekend INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE todos ADD COLUMN weekday INTEGER NOT NULL DEFAULT 0");
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE todos ADD COLUMN timesensitive INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "todoly.db")
                            .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Atomically wipes all existing data and loads the provided categories and todos,
     * preserving their original primary-key ids so that {@code Todo.categoryId} foreign
     * keys remain valid after a full replace import.
     */
    public void replaceAll(final List<Category> categories, final List<Todo> todos) {
        runInTransaction(() -> {
            todoDao().deleteAll();
            categoryDao().deleteAll();
            if (categories != null && !categories.isEmpty()) {
                categoryDao().insertAll(categories);
            }
            if (todos != null && !todos.isEmpty()) {
                todoDao().insertAll(todos);
            }
        });
    }
}
