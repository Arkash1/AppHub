package com.arkashstudio.apphub.service;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.arkashstudio.apphub.data.local.SettingsRepository;
import com.arkashstudio.apphub.data.remote.ApiClient;
import com.arkashstudio.apphub.data.remote.ApiService;
import com.arkashstudio.apphub.data.remote.dto.ApiResponse;
import com.arkashstudio.apphub.data.remote.dto.SystemInfoDto;

import retrofit2.Response;

/**
 * Периодическая фоновая задача (WorkManager): проверяет, изменилась ли версия
 * каталога на сервере. Если да — ничего не делает сама (лишь обеспечивает, что
 * при следующем запуске приложения refresh подхватит свежие данные).
 *
 * <p>Период: 15 минут (минимальный для PeriodicWorkRequest).
 */
public class CatalogSyncWorker extends Worker {

    public CatalogSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            ApiService api = ApiClient.getOrNull();
            if (api == null) {
                return Result.success(); // клиент ещё не инициализирован
            }
            Response<ApiResponse<SystemInfoDto>> resp = api.getSystemInfo().execute();
            if (!resp.isSuccessful() || resp.body() == null || !resp.body().isSuccess()) {
                return Result.retry();
            }
            long serverVersion = resp.body().data.catalogVersion;

            SettingsRepository settings = new SettingsRepository(getApplicationContext());
            long cachedVersion = settings.getCatalogVersionSync();

            // Если версия изменилась — просто запоминаем флаг «нужно обновить»,
            // сбрасывая кэшированную версию (UI сам инициирует refresh при следующем старте).
            if (serverVersion > cachedVersion) {
                settings.setCatalogVersion(serverVersion);
            }
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
