package com.arkashstudio.apphub.install;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.content.FileProvider;

import java.io.File;

/**
 * Запуск системного установщика Android для скачанного APK.
 *
 * <p>Использует {@link FileProvider} для безопасной отдачи файла во внешнее
 * приложение-установщик. Требует разрешение {@code REQUEST_INSTALL_PACKAGES}
 * (прописано в манифесте).
 *
 * <p>На Android 8+ пользователь должен однократно разрешить установку из
 * неизвестных источников для AppHub — при первом запуске установщика система
 * покажет диалог. После разрешения установка проходит без подтверждений.
 */
public final class ApkInstaller {

    private ApkInstaller() { }

    /**
     * Запустить установку APK.
     *
     * @return true, если intent отправлен; false — если нет разрешения на установку.
     */
    public static boolean install(Context context, File apkFile) {
        if (apkFile == null || !apkFile.exists()) return false;

        // На Android 8+ проверяем разрешение на установку из неизвестных источников.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !context.getPackageManager().canRequestPackageInstalls()) {
            // Открываем настройки, чтобы пользователь разрешил.
            openUnknownSourcesSettings(context);
            return false;
        }

        Uri uri = FileProvider.getUriForFile(context,
                context.getPackageName() + ".fileprovider", apkFile);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Открыть системные настройки «Установка из неизвестных источников» для AppHub. */
    public static void openUnknownSourcesSettings(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Uri uri = Uri.parse("package:" + context.getPackageName());
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(intent);
            } catch (Exception ignored) { }
        }
    }

    /** Есть ли разрешение на установку из неизвестных источников. */
    public static boolean canInstall(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return context.getPackageManager().canRequestPackageInstalls();
        }
        return true;
    }
}
