package com.arkashstudio.apphub.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Тело запроса "проверка обновлений": список установленных приложений.
 */
public class CheckUpdatesRequest {
    @SerializedName("apps")
    public List<InstalledApp> apps;

    public CheckUpdatesRequest(List<InstalledApp> apps) {
        this.apps = apps;
    }

    public static class InstalledApp {
        @SerializedName("packageName")
        public String packageName;
        @SerializedName("versionCode")
        public int versionCode;

        public InstalledApp(String packageName, int versionCode) {
            this.packageName = packageName;
            this.versionCode = versionCode;
        }
    }
}
