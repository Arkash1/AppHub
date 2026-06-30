package com.arkashstudio.apphub.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.arkashstudio.apphub.data.local.entity.AppEntity;

import java.util.List;

@Dao
public interface AppDao {

    @Query("SELECT * FROM app ORDER BY featured DESC, title COLLATE NOCASE ASC")
    LiveData<List<AppEntity>> observeAll();

    @Query("SELECT * FROM app WHERE category = :category ORDER BY featured DESC, title COLLATE NOCASE ASC")
    LiveData<List<AppEntity>> observeByCategory(String category);

    @Query("SELECT * FROM app WHERE packageName = :pkg LIMIT 1")
    AppEntity findByPackage(String pkg);

    @Query("SELECT * FROM app")
    List<AppEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<AppEntity> apps);

    @Query("DELETE FROM app")
    void clear();

    @Query("SELECT COUNT(*) FROM app")
    int count();
}
