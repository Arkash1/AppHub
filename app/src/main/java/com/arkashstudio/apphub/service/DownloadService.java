package com.arkashstudio.apphub.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.arkashstudio.apphub.AppHubApp;
import com.arkashstudio.apphub.R;
import com.arkashstudio.apphub.data.remote.ApiClient;
import com.arkashstudio.apphub.data.remote.ApiService;
import com.arkashstudio.apphub.data.repo.DownloadRepository;
import com.arkashstudio.apphub.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Foreground-сервис скачивания APK с OkHttp-streaming и прогрессом.
 *
 * <p>Одна активная загрузка на приложение (по packageName). Поддерживает:
 * <ul>
 *   <li>потоковое чтение с прогрессом (8 КБ чанков);</li>
 *   <li>отмену через флаг {@code cancelFlags};</li>
 *   <li>SHA-256 верификацию после завершения;</li>
 *   <li>уведомление с прогрессом (foreground).</li>
 * </ul>
 *
 * <p>Android 10+ требует foreground-сервис типа {@code dataSync} для фоновой загрузки.
 */
public class DownloadService extends Service {

    public static final String EXTRA_APP_ID = "app_id";
    public static final String EXTRA_PACKAGE = "package";
    public static final String EXTRA_VERSION_ID = "version_id";
    public static final String EXTRA_VERSION_CODE = "version_code";
    public static final String EXTRA_SHA256 = "sha256";
    public static final String EXTRA_FILE_SIZE = "file_size";

    public static final String ACTION_CANCEL = "com.arkashstudio.apphub.ACTION_CANCEL";
    public static final String EXTRA_CANCEL_PACKAGE = "cancel_package";

    private static final int NOTIF_ID_BASE = 7000;

    /** Флаги отмены по packageName. */
    private static final Map<String, AtomicBoolean> cancelFlags = new ConcurrentHashMap<>();

    private final Map<String, okhttp3.Call> activeCalls = new ConcurrentHashMap<>();

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelfIfIdle();
            return START_NOT_STICKY;
        }

        // Команда отмены.
        if (ACTION_CANCEL.equals(intent.getAction())) {
            String pkg = intent.getStringExtra(EXTRA_CANCEL_PACKAGE);
            cancelDownload(pkg);
            return START_NOT_STICKY;
        }

        final String pkg = intent.getStringExtra(EXTRA_PACKAGE);
        final long versionId = intent.getLongExtra(EXTRA_VERSION_ID, -1);
        final String sha = intent.getStringExtra(EXTRA_SHA256);
        final long size = intent.getLongExtra(EXTRA_FILE_SIZE, 0);

        startForeground(pkg);

        // Запуск загрузки в фоне.
        new Thread(() -> performDownload(pkg, versionId, sha, size)).start();

        return START_STICKY;
    }

    private void performDownload(String pkg, long versionId, String sha, long size) {
        DownloadRepository repo = DownloadRepository.get(this);
        AtomicBoolean cancelFlag = new AtomicBoolean(false);
        cancelFlags.put(pkg, cancelFlag);

        // Временный файл (для верификации SHA, прежде чем финализировать).
        File tempFile = new File(FileUtil.downloadsDir(this), pkg + ".apk.tmp");
        File finalFile = new File(FileUtil.downloadsDir(this), sha + ".apk");

        try {
            OkHttpClient client = ApiClient.httpClient();
            if (client == null) {
                // Fallback: создать простой клиент, если ApiClient ещё не готов.
                client = new OkHttpClient();
            }

            String url = ApiClient.currentBaseUrl()
                    + "api/v1/download/" + versionId;
            Request request = new Request.Builder().url(url).build();
            okhttp3.Call call = client.newCall(request);
            activeCalls.put(pkg, call);

            try (Response response = call.execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("HTTP " + response.code());
                }
                ResponseBody body = response.body();
                if (body == null) throw new IOException("Пустой ответ");

                long total = size > 0 ? size : body.contentLength();

                try (InputStream is = body.byteStream()) {
                    FileUtil.writeStream(is, tempFile, total, percent ->
                            repo.publishProgress(pkg, percent), cancelFlag);
                }
            }

            // SHA-256 верификация.
            String actualSha = FileUtil.sha256(tempFile);
            if (sha == null || sha.isEmpty()) {
                // Нет ожидаемого хэша — пропускаем проверку, но финализируем по фактическому хэшу.
                sha = actualSha;
            } else if (!sha.equalsIgnoreCase(actualSha)) {
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
                repo.publishFailed(pkg, "Контрольная сумма не совпала — файл повреждён");
                finishForeground(pkg);
                return;
            }

            // Переименование в финальный файл.
            if (finalFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                finalFile.delete();
            }
            if (!tempFile.renameTo(finalFile)) {
                throw new IOException("Не удалось сохранить файл");
            }

            repo.publishCompleted(pkg);

        } catch (Exception e) {
            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();
            if (cancelFlag.get()) {
                repo.publishCancelled(pkg);
            } else {
                repo.publishFailed(pkg, "Ошибка загрузки: " + e.getMessage());
            }
        } finally {
            cancelFlags.remove(pkg);
            activeCalls.remove(pkg);
            finishForeground(pkg);
        }
    }

    // ===== Foreground notification =====

    private void startForeground(String pkg) {
        Notification n = buildNotification(pkg, 0, true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(notificationId(pkg), n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(notificationId(pkg), n);
        }
    }

    private void finishForeground(String pkg) {
        // Если других активных загрузок нет — останавливаем foreground.
        if (activeCalls.isEmpty()) {
            stopForeground(true);
            stopSelf();
        }
    }

    private void stopSelfIfIdle() {
        if (activeCalls.isEmpty()) {
            stopSelf();
        }
    }

    private Notification buildNotification(String pkg, int progress, boolean indeterminate) {
        Intent cancelIntent = new Intent(this, DownloadService.class);
        cancelIntent.setAction(ACTION_CANCEL);
        cancelIntent.putExtra(EXTRA_CANCEL_PACKAGE, pkg);
        PendingIntent cancelPi = PendingIntent.getService(
                this, pkg.hashCode(), cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = "Загрузка приложения";
        return new NotificationCompat.Builder(this, AppHubApp.CHANNEL_DOWNLOAD)
                .setContentTitle(title)
                .setContentText(progress > 0 ? progress + "%" : "Подготовка…")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setProgress(100, progress, indeterminate || progress == 0)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .addAction(0, "Отмена", cancelPi)
                .build();
    }

    private int notificationId(String pkg) {
        return NOTIF_ID_BASE + (pkg == null ? 0 : Math.abs(pkg.hashCode()) % 1000);
    }

    // ===== Отмена =====

    private void cancelDownload(String pkg) {
        if (pkg == null) return;
        AtomicBoolean flag = cancelFlags.get(pkg);
        if (flag != null) {
            flag.set(true);
        }
        okhttp3.Call call = activeCalls.get(pkg);
        if (call != null && !call.isCanceled()) {
            call.cancel();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // ===== Статические точки входа =====

    public static void enqueue(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void cancel(Context context, String pkg) {
        Intent i = new Intent(context, DownloadService.class);
        i.setAction(ACTION_CANCEL);
        i.putExtra(EXTRA_CANCEL_PACKAGE, pkg);
        // Отмена обрабатывается синхронно — не требует foreground-старта,
        // но сервис должен быть запущен. Шлём обычный startService.
        context.startService(i);
    }
}
