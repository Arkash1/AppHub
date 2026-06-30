package com.arkashstudio.apphub.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

/**
 * Помощники работы с PackageManager: проверка установки приложения и
 * получение его versionCode — основа логики кнопки «скачать/обновить/открыть».
 */
public final class PackageUtils {

    private PackageUtils() { }

    /**
     * Установлено ли приложение с данным packageName.
     */
    public static boolean isInstalled(Context context, String packageName) {
        return getInstalledVersionCode(context, packageName) != null;
    }

    /**
     * VersionCode установленного приложения или null, если оно не установлено.
     * Использует PackageInfoCompat.getLongVersionCode (корректно для API 28+).
     */
    public static Long getInstalledVersionCode(Context context, String packageName) {
        try {
            PackageInfo info = context.getPackageManager()
                    .getPackageInfo(packageName, 0);
            return androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(info);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    /**
     * Запустить установленное приложение (intent MAIN/LAUNCHER).
     * @return true, если запуск удался.
     */
    public static boolean launchApp(Context context, String packageName) {
        try {
            android.content.Intent intent = context.getPackageManager()
                    .getLaunchIntentForPackage(packageName);
            if (intent == null) return false;
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Текущий уровень SDK устройства. */
    public static int deviceSdk() {
        return Build.VERSION.SDK_INT;
    }
}
