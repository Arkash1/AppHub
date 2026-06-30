package com.arkashstudio.apphub.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Сущность кэша каталога в Room. Хранит «карточку» приложения, включая поля
 * последней версии — чтобы UI работал офлайн.
 */
@Entity(tableName = "app")
public class AppEntity {
    @PrimaryKey
    public long id;

    public String packageName;
    public String title;
    public String developer;
    public String shortDesc;
    public String category;
    public String iconUrl;
    public double rating;
    public boolean featured;

    public Long latestVersionId;
    public String latestVersionName;
    public Integer latestVersionCode;
    public Integer latestMinSdk;
    public Long latestFileSize;
    public String latestSha256;

    public String updatedAt;

    public boolean hasVersion() {
        return latestVersionId != null && latestVersionCode != null;
    }
}
