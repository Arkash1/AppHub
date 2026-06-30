package com.arkashstudio.apphub.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Полное представление приложения (страница деталей). */
public class AppDetailDto {
    @SerializedName("id") public long id;
    @SerializedName("packageName") public String packageName;
    @SerializedName("title") public String title;
    @SerializedName("developer") public String developer;
    @SerializedName("shortDesc") public String shortDesc;
    @SerializedName("description") public String description;
    @SerializedName("category") public String category;
    @SerializedName("iconUrl") public String iconUrl;
    @SerializedName("bannerUrl") public String bannerUrl;
    @SerializedName("rating") public double rating;
    @SerializedName("featured") public boolean featured;

    @SerializedName("versions") public List<VersionDto> versions;
    @SerializedName("screenshots") public List<ScreenshotDto> screenshots;

    @SerializedName("createdAt") public String createdAt;
    @SerializedName("updatedAt") public String updatedAt;

    /** Последняя версия (первая в отсортированном списке). */
    public VersionDto latestVersion() {
        if (versions == null || versions.isEmpty()) return null;
        return versions.get(0);
    }
}
