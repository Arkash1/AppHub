package com.arkashstudio.apphub.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.arkashstudio.apphub.data.local.dao.AppDao;
import com.arkashstudio.apphub.data.local.entity.AppEntity;

/**
 * Room-база AppHub. Хранит офлайн-кэш каталога приложений.
 *
 * <p>Синглтон: создаётся один раз и переиспользуется во всём приложении.
 */
@Database(
        entities = {AppEntity.class},
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract AppDao appDao();

    public static AppDatabase get(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "apphub.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}
