package com.arkashstudio.apphub.util;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Файловые утилиты: запись стрима в файл, расчёт SHA-256, форматирование размеров,
 * каталог для скачиваемых APK.
 */
public final class FileUtil {

    private FileUtil() { }

    /**
     * Записать байты из InputStream в файл (полная перезапись) с прогрессом.
     *
     * @param totalSize  ожидаемый размер (для прогресса) или -1, если неизвестен.
     * @param progress   callback(0..100), может быть null.
     * @param cancelFlag флаг отмены — если станет true, операция прерывается IOException.
     */
    public static void writeStream(InputStream input, File target, long totalSize,
                                   ProgressCallback progress, AtomicBoolean cancelFlag)
            throws IOException {

        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }

        long written = 0;
        byte[] buffer = new byte[8192];
        int lastReported = -1;

        try (RandomAccessFile out = new RandomAccessFile(target, "rw")) {
            out.setLength(0);
            int read;
            while ((read = input.read(buffer)) != -1) {
                if (cancelFlag != null && cancelFlag.get()) {
                    throw new IOException("download cancelled");
                }
                out.write(buffer, 0, read);
                written += read;
                if (progress != null && totalSize > 0) {
                    int percent = (int) ((written * 100) / totalSize);
                    if (percent != lastReported && percent <= 100) {
                        lastReported = percent;
                        progress.onProgress(percent);
                    }
                }
            }
        }
        if (progress != null) {
            progress.onProgress(100);
        }
    }

    /** SHA-256 файла в виде 64 hex-символов нижнего регистра. */
    public static String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream is = Files.newInputStream(file.toPath())) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        byte[] hash = digest.digest();
        StringBuilder sb = new StringBuilder(64);
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /** Форматирование размера в человекочитаемый вид: "12.3 МБ". */
    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " Б";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.US, "%.1f КБ", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format(Locale.US, "%.1f МБ", mb);
        double gb = mb / 1024.0;
        return String.format(Locale.US, "%.2f ГБ", gb);
    }

    /** Каталог для скачиваемых APK (внутренний кэш приложения). */
    public static File downloadsDir(Context context) {
        File dir = new File(context.getCacheDir(), "downloads");
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
    }

    /** Путь к конкретному скачанному APK по SHA-256. */
    public static File apkFileBySha(Context context, String sha256) {
        return new File(downloadsDir(context), sha256 + ".apk");
    }

    /** Callback прогресса (0..100). */
    public interface ProgressCallback {
        void onProgress(int percent);
    }
}
