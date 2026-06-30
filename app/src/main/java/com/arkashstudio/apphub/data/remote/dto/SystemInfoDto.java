package com.arkashstudio.apphub.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Системная информация сервера. */
public class SystemInfoDto {
    @SerializedName("name") public String name;
    @SerializedName("version") public String version;
    @SerializedName("catalogVersion") public long catalogVersion;
}
