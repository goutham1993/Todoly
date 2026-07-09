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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Category.class, Todo.class}, version = 3, exportSchema = false)
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

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "todoly.db")
                            .addMigrations(MIGRATION_2_3)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
