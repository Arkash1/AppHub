package com.arkashstudio.apphub.data.repo;

import android.content.Context;
import android.content.Intent;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.arkashstudio.apphub.data.local.entity.AppEntity;
import com.arkashstudio.apphub.service.DownloadService;
import com.arkashstudio.apphub.util.FileUtil;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Репозиторий загрузок. Хранит in-memory состояние каждой загрузки по packageName
 * и делегирует само скачивание {@link DownloadService}.
 *
 * <p>UI подписывается на {@link #observe(String)} и обновляет кнопку.
 */
public class DownloadRepository {

    private static volatile DownloadRepository instance;

    private final Context context;
    private final Map<String, MutableLiveData<DownloadState>> states = new ConcurrentHashMap<>();

    private DownloadRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public static DownloadRepository get(Context context) {
        if (instance == null) {
            synchronized (DownloadRepository.class) {
                if (instance == null) {
                    instance = new DownloadRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    /**
     * Подписаться на состояние загрузки приложения. Возвращает LiveData, которое
     * обновляется сервисом при изменении прогресса/статуса.
     */
    public LiveData<DownloadState> observe(String packageName) {
        MutableLiveData<DownloadState> live = states.get(packageName);
        if (live == null) {
            live = new MutableLiveData<>(DownloadState.idle(packageName));
            // Если APK уже скачан ранее — покажем DOWNLOADED сразу.
            states.put(packageName, live);
        }
        return live;
    }

    /** Текущее состояние (для синхронных проверок). */
    public DownloadState snapshot(String packageName) {
        MutableLiveData<DownloadState> live = states.get(packageName);
        return live != null && live.getValue() != null
                ? live.getValue()
                : DownloadState.idle(packageName);
    }

    /** Запустить скачивание приложения. */
    public void startDownload(AppEntity app) {
        // Инициализируем состояние.
        MutableLiveData<DownloadState> live = getOrCreate(packageName(app));
        live.postValue(DownloadState.downloading(packageName(app), 0));

        Intent intent = new Intent(context, DownloadService.class);
        intent.putExtra(DownloadService.EXTRA_APP_ID, app.id);
        intent.putExtra(DownloadService.EXTRA_PACKAGE, app.packageName);
        intent.putExtra(DownloadService.EXTRA_VERSION_ID, app.latestVersionId);
        intent.putExtra(DownloadService.EXTRA_VERSION_CODE, app.latestVersionCode == null ? 0 : app.latestVersionCode);
        intent.putExtra(DownloadService.EXTRA_SHA256, app.latestSha256);
        intent.putExtra(DownloadService.EXTRA_FILE_SIZE, app.latestFileSize == null ? 0L : app.latestFileSize);
        DownloadService.enqueue(context, intent);
    }

    /** Отменить загрузку. */
    public void cancel(String packageName) {
        DownloadService.cancel(context, packageName);
    }

    // ===== Методы, вызываемые сервисом для обновления состояния =====

    /** Вызывается сервисом при изменении прогресса. */
    public void publishProgress(String packageName, int percent) {
        getOrCreate(packageName).postValue(DownloadState.downloading(packageName, percent));
    }

    /** Вызывается сервисом при успешном завершении. */
    public void publishCompleted(String packageName) {
        getOrCreate(packageName).postValue(DownloadState.downloaded(packageName));
    }

    /** Вызывается сервисом при ошибке/отмене. */
    public void publishFailed(String packageName, String error) {
        getOrCreate(packageName).postValue(DownloadState.failed(packageName, error));
    }

    /** Вызывается сервисом при отмене. */
    public void publishCancelled(String packageName) {
        getOrCreate(packageName).postValue(DownloadState.cancelled(packageName));
    }

    /** Есть ли уже скачанный (и не повреждённый) APK для приложения. */
    public boolean isApkDownloaded(String packageName, String sha256) {
        if (sha256 == null) return false;
        File f = FileUtil.apkFileBySha(context, sha256);
        return f.exists() && f.length() > 0;
    }

    /** Сбросить состояние приложения к IDLE (напр. после установки). */
    public void reset(String packageName) {
        getOrCreate(packageName).postValue(DownloadState.idle(packageName));
    }

    private MutableLiveData<DownloadState> getOrCreate(String packageName) {
        return states.computeIfAbsent(packageName, pkg -> {
            MutableLiveData<DownloadState> live = new MutableLiveData<>();
            live.setValue(DownloadState.idle(pkg));
            return live;
        });
    }

    private static String packageName(AppEntity app) {
        return app != null && app.packageName != null ? app.packageName : "";
    }
}
