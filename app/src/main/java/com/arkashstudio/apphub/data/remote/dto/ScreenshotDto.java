package com.arkashstudio.apphub.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Скриншот приложения. */
public class ScreenshotDto {
    @SerializedName("id") public long id;
    @SerializedName("url") public String url;
    @SerializedName("sortOrder") public int sortOrder;
}
