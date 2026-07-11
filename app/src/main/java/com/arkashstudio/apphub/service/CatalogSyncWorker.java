package com.arkashstudio.apphub.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.arkashstudio.apphub.AppHubApp;
import com.arkashstudio.apphub.R;
import com.arkashstudio.apphub.data.local.SettingsRepository;
import com.arkashstudio.apphub.data.remote.ApiClient;
import com.arkashstudio.apphub.data.remote.ApiService;
import com.arkashstudio.apphub.data.remote.dto.ApiResponse;
import com.arkashstudio.apphub.data.remote.dto.CheckUpdatesRequest;
import com.arkashstudio.apphub.data.remote.dto.SystemInfoDto;
import com.arkashstudio.apphub.data.remote.dto.VersionDto;
import com.arkashstudio.apphub.ui.catalog.CatalogActivity;
import com.arkashstudio.apphub.util.PackageUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Периодическая фоновая задача (WorkManager).
 *
 * <p>Алгоритм:
 * <ol>
 *   <li>Запрашивает /system/info — текущую версию каталога на сервере.</li>
 *   <li>Если версия изменилась с последнего раза — собирает список установленных
 *       приложений и вызывает /catalog/check-updates.</li>
 *   <li>Если сервер сообщает об устаревших приложениях — показывает локальное
 *       уведомление «Доступно обновление».</li>
 * </ol>
 *
 * <p>Период: 15 минут (минимальный для PeriodicWorkRequest).
 */
public class CatalogSyncWorker extends Worker {

    private static final String CHANNEL_UPDATES = "channel_updates";
    private static final int NOTIF_ID = 9001;
    private static final String KEY_LAST_NOTIFIED_VERSION = "last_notified_catalog_version";

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

            // 1) Проверяем версию каталога.
            Response<ApiResponse<SystemInfoDto>> infoResp = api.getSystemInfo().execute();
            if (!infoResp.isSuccessful() || infoResp.body() == null || !infoResp.body().isSuccess()) {
                return Result.retry();
            }
            long serverVersion = infoResp.body().data.catalogVersion;

            SettingsRepository settings = new SettingsRepository(getApplicationContext());
            long cachedVersion = settings.getCatalogVersionSync();

            // Если каталог не изменился — уведомлять не о чем.
            if (serverVersion <= cachedVersion) {
                return Result.success();
            }

            // 2) Каталог изменился — проверяем устаревшие приложения.
            List<VersionDto> outdated = checkOutdated(api);

            // Запоминаем новую версию (UI подхватит при следующем старте).
            settings.setCatalogVersion(serverVersion);

            // 3) Если есть устаревшие — показываем уведомление (но не чаще, чем
            //    один раз на каждое изменение каталога).
            long lastNotified = getApplicationContext()
                    .getSharedPreferences("apphub_worker", Context.MODE_PRIVATE)
                    .getLong(KEY_LAST_NOTIFIED_VERSION, 0L);

            if (outdated != null && !outdated.isEmpty() && serverVersion != lastNotified) {
                showUpdateNotification(outdated.size());
                getApplicationContext()
                        .getSharedPreferences("apphub_worker", Context.MODE_PRIVATE)
                        .edit()
                        .putLong(KEY_LAST_NOTIFIED_VERSION, serverVersion)
                        .apply();
            }

            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }

    /** Собирает список установленных приложений и спрашивает сервер, какие устарели. */
    private List<VersionDto> checkOutdated(ApiService api) {
        try {
            // Получаем все приложения из кэша (packageNames).
            com.arkashstudio.apphub.data.local.AppDatabase db =
                    com.arkashstudio.apphub.data.local.AppDatabase.get(getApplicationContext());
            List<com.arkashstudio.apphub.data.local.entity.AppEntity> all = db.appDao().getAll();

            List<CheckUpdatesRequest.InstalledApp> installed = new ArrayList<>();
            for (com.arkashstudio.apphub.data.local.entity.AppEntity app : all) {
                Long code = PackageUtils.getInstalledVersionCode(getApplicationContext(), app.packageName);
                if (code != null && app.packageName != null) {
                    installed.add(new CheckUpdatesRequest.InstalledApp(app.packageName, code.intValue()));
                }
            }

            if (installed.isEmpty()) {
                return java.util.Collections.emptyList();
            }

            Call<ApiResponse<List<VersionDto>>> call =
                    api.checkUpdates(new CheckUpdatesRequest(installed));
            Response<ApiResponse<List<VersionDto>>> resp = call.execute();
            if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()) {
                return resp.body().data;
            }
        } catch (Exception e) {
            // Тихо игнорируем — это фоновая задача, не стоит беспокоить пользователя.
        }
        return java.util.Collections.emptyList();
    }

    /** Показать уведомление о доступных обновлениях. */
    private void showUpdateNotification(int count) {
        Context ctx = getApplicationContext();
        ensureChannel(ctx);

        Intent intent = new Intent(ctx, CatalogActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = ctx.getString(R.string.notif_updates_title);
        String text = ctx.getResources().getQuantityString(
                R.plurals.notif_updates_text, count, count);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_UPDATES)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        android.app.NotificationManager nm =
                (android.app.NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIF_ID, builder.build());
        }
    }

    private void ensureChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_UPDATES,
                    ctx.getString(R.string.notif_channel_updates),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(ctx.getString(R.string.notif_channel_updates_desc));
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm != null && nm.getNotificationChannel(CHANNEL_UPDATES) == null) {
                nm.createNotificationChannel(channel);
            }
        }
    }
}
