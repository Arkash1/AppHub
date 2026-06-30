package com.arkashstudio.apphub.data.repo;

/**
 * Состояние загрузки конкретного приложения.
 * Используется {@link com.arkashstudio.apphub.ui.common.ActionButton} для
 * определения внешнего вида и надписи кнопки.
 */
public class DownloadState {

    public enum Status {
        /** Не загружено. */
        IDLE,
        /** Идёт скачивание. */
        DOWNLOADING,
        /** Скачано, ожидает установки. */
        DOWNLOADED,
        /** Загрузка не удалась (ошибка/повреждён файл). */
        FAILED,
        /** Отменено пользователем. */
        CANCELLED
    }

    public final String packageName;
    public final Status status;
    public final int progress;        // 0..100 (актуально для DOWNLOADING)
    public final String errorMessage; // актуально для FAILED

    private DownloadState(String packageName, Status status, int progress, String errorMessage) {
        this.packageName = packageName;
        this.status = status;
        this.progress = progress;
        this.errorMessage = errorMessage;
    }

    public static DownloadState idle(String pkg) {
        return new DownloadState(pkg, Status.IDLE, 0, null);
    }

    public static DownloadState downloading(String pkg, int progress) {
        return new DownloadState(pkg, Status.DOWNLOADING, progress, null);
    }

    public static DownloadState downloaded(String pkg) {
        return new DownloadState(pkg, Status.DOWNLOADED, 100, null);
    }

    public static DownloadState failed(String pkg, String error) {
        return new DownloadState(pkg, Status.FAILED, 0, error);
    }

    public static DownloadState cancelled(String pkg) {
        return new DownloadState(pkg, Status.CANCELLED, 0, null);
    }

    @Override
    public String toString() {
        return "DownloadState{" + packageName + ": " + status + " " + progress + "%}";
    }
}
