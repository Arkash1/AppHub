package com.arkashstudio.apphub.util;

import android.content.Context;

import com.arkashstudio.apphub.BuildConfig;
import com.arkashstudio.apphub.data.remote.ApiClient;

/**
 * Помощник построения URL к медиа на сервере.
 *
 * <p>Сервер хранит пути относительно корня хранения ("icon/abc.png"). Клиент
 * превращает их в полный URL через базовый адрес + /api/v1/media/{type}/{file}.
 */
public final class UrlUtil {

    private UrlUtil() { }

    /**
     * Полный URL к медиа-файлу по относительному пути.
     * @param relativePath напр. "icon/abc.png" или "screenshot/x.jpg"
     */
    public static String mediaUrl(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) return "";
        String base = currentBaseUrl();
        // relativePath = "icon/abc.png" → "icon" и "abc.png"
        String[] parts = relativePath.split("/", 2);
        if (parts.length == 2) {
            return base + "/api/v1/media/" + parts[0] + "/" + parts[1];
        }
        return base + "/api/v1/media/" + relativePath;
    }

    /**
     * Проверить, является ли строка уже полным URL (http/https).
     */
    public static boolean isAbsoluteUrl(String url) {
        return url != null
                && (url.startsWith("http://") || url.startsWith("https://"));
    }

    /**
     * Текущий базовый URL (без trailing slash) или дефолтный домен.
     */
    private static String currentBaseUrl() {
        String base = ApiClient.currentBaseUrl();
        if (base == null || base.isEmpty()) {
            base = BuildConfig.DEFAULT_SERVER_URL;
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }
}
