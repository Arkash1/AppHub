package com.arkashstudio.apphub;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.WorkManager;

import com.arkashstudio.apphub.data.remote.ApiClient;
import com.arkashstudio.apphub.service.CatalogSyncWorker;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Application-класс AppHub. Точка инициализации глобального состояния.
 *
 * <p>Ответственности:
 * <ul>
 *   <li>Создание notification-канала для сервиса загрузки.</li>
 *   <li>Инициализация Retrofit-клиента (в фоновом потоке — чтение настроек синхронное).</li>
 *   <li>Планирование фоновой синхронизации каталога (WorkManager, раз в 15 мин).</li>
 * </ul>
 *
 * <p>Внимание: WorkManager инициализируется автоматически системой (через провайдер
 * в манифесте androidx.startup). Здесь только планируем периодическую задачу.
 */
public class AppHubApp extends Application {

    public static final String CHANNEL_DOWNLOAD = "channel_download";

    @Override
    public void onCreate() {
        super.onCreate();

        // 1) Notification-канал для foreground-сервиса загрузки.
        createDownloadChannel();

        // 2) Инициализация Retrofit в фоновом потоке (чтение настроек синхронное).
        Executors.newSingleThreadExecutor().execute(() -> ApiClient.init(getApplicationContext()));

        // 3) WorkManager — планирование фоновой задачи (инициализация автоматическая).
        scheduleCatalogSync();
    }

    private void createDownloadChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_DOWNLOAD,
                    "Загрузка приложений",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Прогресс скачивания APK");
            channel.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private void scheduleCatalogSync() {
        // PeriodicWorkRequest.Builder — Java API (Kotlin-билдер с reified generics не доступен в Java).
        androidx.work.PeriodicWorkRequest request =
                new androidx.work.PeriodicWorkRequest.Builder(
                        CatalogSyncWorker.class, 15, TimeUnit.MINUTES)
                        .setConstraints(new androidx.work.Constraints.Builder()
                                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                                .build())
                        .build();

        // WorkManager уже инициализирован системой — просто планируем задачу.
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "catalog_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                request);
    }
}
