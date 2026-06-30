package com.arkashstudio.apphub.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Версия приложения. */
public class VersionDto {
    @SerializedName("id") public long id;
    @SerializedName("appId") public long appId;
    @SerializedName("versionName") public String versionName;
    @SerializedName("versionCode") public int versionCode;
    @SerializedName("minSdk") public Integer minSdk;
    @SerializedName("targetSdk") public Integer targetSdk;
    @SerializedName("fileSize") public long fileSize;
    @SerializedName("sha256") public String sha256;
    @SerializedName("releaseNotes") public String releaseNotes;
    @SerializedName("createdAt") public String createdAt;
}
