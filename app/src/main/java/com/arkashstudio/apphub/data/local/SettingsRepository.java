package com.arkashstudio.apphub.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import com.arkashstudio.apphub.BuildConfig;

/**
 * Репозиторий настроек на обычных SharedPreferences (без DataStore/RxJava).
 *
 * <p>Хранит: адрес сервера (ручной ввод или дефолт) и последнюю известную
 * версию каталога.
 *
 * <p>Логика авто-подключения: если пользователь задал адрес вручную — он
 * приоритетнее дефолтного домена из BuildConfig.
 */
public class SettingsRepository {

    private static final String PREFS_NAME = "apphub_settings";
    private static final String KEY_SERVER_URL = "server_url";
    private static final String KEY_CATALOG_VERSION = "catalog_version";

    private static final String DEFAULT_URL = BuildConfig.DEFAULT_SERVER_URL;

    private final SharedPreferences prefs;

    public SettingsRepository(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Асинхронно сохранить адрес сервера (ручной ввод в настройках). */
    public void setServerUrl(String url) {
        prefs.edit().putString(KEY_SERVER_URL, normalizeUrl(url)).apply();
    }

    /** Сбросить адрес к дефолтному домену. */
    public void resetServerUrl() {
        prefs.edit().remove(KEY_SERVER_URL).apply();
    }

    /** Текущий адрес сервера. */
    public String getServerUrlSync() {
        String url = prefs.getString(KEY_SERVER_URL, null);
        return (url == null || url.isEmpty()) ? DEFAULT_URL : normalizeUrl(url);
    }

    /** Сохранить последнюю известную версию каталога. */
    public void setCatalogVersion(long version) {
        prefs.edit().putLong(KEY_CATALOG_VERSION, version).apply();
    }

    /** Последняя известная версия каталога или 0. */
    public long getCatalogVersionSync() {
        return prefs.getLong(KEY_CATALOG_VERSION, 0L);
    }

    /** Дефолтный домен (из BuildConfig) — для отображения в настройках. */
    public String getDefaultUrl() {
        return DEFAULT_URL;
    }

    /** Использовать ли дефолтный адрес (нет ручного ввода). */
    public boolean isUsingDefault() {
        String url = prefs.getString(KEY_SERVER_URL, null);
        return url == null || url.isEmpty();
    }

    /** Нормализация: гарантированный trailing слэш убран, есть схема. */
    private static String normalizeUrl(String url) {
        if (url == null || url.isBlank()) return DEFAULT_URL;
        String u = url.trim();
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = "https://" + u;
        }
        return u;
    }
}
