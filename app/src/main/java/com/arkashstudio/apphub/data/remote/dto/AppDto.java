package com.arkashstudio.apphub.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Краткое представление приложения (карточка каталога).
 * Поля последней версии имеют префикс latest* — нужны для кнопки действия.
 */
public class AppDto {
    @SerializedName("id") public long id;
    @SerializedName("packageName") public String packageName;
    @SerializedName("title") public String title;
    @SerializedName("developer") public String developer;
    @SerializedName("shortDesc") public String shortDesc;
    @SerializedName("category") public String category;
    @SerializedName("iconUrl") public String iconUrl;
    @SerializedName("rating") public double rating;
    @SerializedName("featured") public boolean featured;

    @SerializedName("latestVersionId") public Long latestVersionId;
    @SerializedName("latestVersionName") public String latestVersionName;
    @SerializedName("latestVersionCode") public Integer latestVersionCode;
    @SerializedName("latestMinSdk") public Integer latestMinSdk;
    @SerializedName("latestFileSize") public Long latestFileSize;
    @SerializedName("latestSha256") public String latestSha256;

    @SerializedName("updatedAt") public String updatedAt;

    /** Есть ли хотя бы одна версия (опубликовано). */
    public boolean hasVersion() {
        return latestVersionId != null && latestVersionCode != null;
    }
}
