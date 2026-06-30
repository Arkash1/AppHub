package com.arkashstudio.apphub.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

/**
 * Безопасное хранение админ-пароля в EncryptedSharedPreferences.
 *
 * <p>Ключ шифрования хранится в Android Keystore (MasterKey), сами данные —
 * в файле настроек, зашифрованные AES-256. После успешной верификации пароль
 * сохраняется здесь, чтобы AdminAuthInterceptor добавлял его в каждый админ-запрос.
 *
 * <p>EncryptedSharedPreferences доступен только на API 23+. AppHub требует API 29+
 * (Android 10), так что ограничение соблюдается.
 */
public class AdminCredentialsStore {

    private static final String FILE_NAME = "apphub_admin_secrets";
    private static final String KEY_PASSWORD = "admin_password";

    private final SharedPreferences prefs;

    public AdminCredentialsStore(Context context) {
        this.prefs = createEncryptedPrefs(context.getApplicationContext());
    }

    /** Сохранить пароль (только после успешной верификации на сервере). */
    public void savePassword(String password) {
        prefs.edit().putString(KEY_PASSWORD, password).apply();
    }

    /** Текущий сохранённый пароль или null, если админ не авторизован. */
    public String getPassword() {
        return prefs.getString(KEY_PASSWORD, null);
    }

    /** Выйти из режима администратора: очистить пароль. */
    public void clear() {
        prefs.edit().remove(KEY_PASSWORD).apply();
    }

    /** Авторизован ли администратор (есть сохранённый пароль). */
    public boolean isAuthorized() {
        String p = getPassword();
        return p != null && !p.isEmpty();
    }

    private static SharedPreferences createEncryptedPrefs(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            return EncryptedSharedPreferences.create(
                    context,
                    FILE_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            // На случай ошибки шифрования (напр. сбой Keystore) — fallback на обычные
            // prefs, чтобы приложение не падало. Пароль будет храниться в открытом виде,
            // но приложение продолжит работать (лучше, чем краш).
            return context.getSharedPreferences(FILE_NAME + "_plain", Context.MODE_PRIVATE);
        }
    }
}
