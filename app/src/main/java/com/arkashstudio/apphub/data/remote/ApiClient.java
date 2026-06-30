package com.arkashstudio.apphub.data.remote;

import android.content.Context;

import com.arkashstudio.apphub.BuildConfig;
import com.arkashstudio.apphub.data.local.AdminCredentialsStore;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Фабрика Retrofit-клиента. Базовый URL определяется настройками (ручной ввод)
 * или дефолтным доменом из BuildConfig.
 *
 * <p>Клиент пересоздаётся при смене адреса сервера через {@link #rebuild(String)}.
 * Все запросы проходят через {@link AdminAuthInterceptor}.
 */
public class ApiClient {

    private static volatile ApiService instance;
    private static volatile String currentBaseUrl = "";
    private static volatile OkHttpClient sharedHttpClient;

    private ApiClient() { }

    /**
     * Инициализировать клиент из настроек. Вызывается из AppHubApp.onCreate().
     */
    public static void init(Context context) {
        AdminCredentialsStore credentials = new AdminCredentialsStore(context);
        com.arkashstudio.apphub.data.local.SettingsRepository settings =
                new com.arkashstudio.apphub.data.local.SettingsRepository(context);

        // Чтение адреса — блокирующее, в UI-потоке НЕ вызывать.
        // AppHubApp создаёт клиент в фоновом потоке (см. AppHubApp).
        String url = readUrlFromBackground(settings);
        build(url, credentials);
    }

    /**
     * Пересобрать клиент с новым базовым URL (вызывается из настроек при смене адреса).
     * @param baseUrl полный URL, напр. "https://apphub.example.ru"
     */
    public static synchronized void rebuild(String baseUrl, Context context) {
        AdminCredentialsStore credentials = new AdminCredentialsStore(context);
        build(baseUrl, credentials);
    }

    /**
     * Пересобрать клиент с тем же адресом (напр. после смены пароля — чтобы
     * перехватчик перечитал EncryptedSharedPreferences).
     */
    public static synchronized void rebuildSameUrl(Context context) {
        AdminCredentialsStore credentials = new AdminCredentialsStore(context);
        build(currentBaseUrl, credentials);
    }

    private static void build(String baseUrl, AdminCredentialsStore credentials) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = BuildConfig.DEFAULT_SERVER_URL;
        }
        // Гарантируем trailing slash для Retrofit.
        if (!baseUrl.endsWith("/")) baseUrl = baseUrl + "/";

        OkHttpClient client = buildOkHttp(credentials);
        sharedHttpClient = client;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        instance = retrofit.create(ApiService.class);
        currentBaseUrl = baseUrl;
    }

    private static OkHttpClient buildOkHttp(AdminCredentialsStore credentials) {
        OkHttpClient.Builder b = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .addInterceptor(new AdminAuthInterceptor(credentials));

        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
            b.addInterceptor(logging);
        }
        return b.build();
    }

    /**
     * Готовый ApiService. Может вернуть null, если init() ещё не отработал —
     * в этом случае вызов应在 вызове на UI-потоке быть обёрнут в проверку.
     */
    public static ApiService get() {
        if (instance == null) {
            throw new IllegalStateException("ApiClient не инициализирован. Вызовите ApiClient.init() в AppHubApp.");
        }
        return instance;
    }

    /** Возвращает null-safe ApiService (без исключения). */
    public static ApiService getOrNull() {
        return instance;
    }

    /** Текущий базовый URL. */
    public static String currentBaseUrl() {
        return currentBaseUrl;
    }

    /** OkHttp-клиент — нужен DownloadService для стриминга с прогрессом. */
    public static OkHttpClient httpClient() {
        return sharedHttpClient;
    }

    /** Асинхронное чтение адреса сервера — вызывать ИЗКЛЮЧИТЕЛЬНО в фоновом потоке. */
    private static String readUrlFromBackground(
            com.arkashstudio.apphub.data.local.SettingsRepository settings) {
        return settings.getServerUrlSync();
    }
}
