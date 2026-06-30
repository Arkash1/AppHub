package com.arkashstudio.apphub.data.remote;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Перехватчик, добавляющий заголовок X-Admin-Password ко всем запросам
 * на /api/v1/admin/** (кроме /verify — там пароль в теле).
 *
 * <p>Пароль берётся из {@link com.arkashstudio.apphub.data.local.AdminCredentialsStore},
 * который хранит его в EncryptedSharedPreferences. Если пароль отсутствует —
 * заголовок не добавляется (запрос всё равно пройдёт для публичных эндпоинтов,
 * а админский вернёт 401 — это нормально для неавторизованного состояния).
 */
public class AdminAuthInterceptor implements Interceptor {

    private final com.arkashstudio.apphub.data.local.AdminCredentialsStore credentials;

    public AdminAuthInterceptor(com.arkashstudio.apphub.data.local.AdminCredentialsStore credentials) {
        this.credentials = credentials;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();
        String path = original.url().encodedPath();

        // Только админские пути (кроме /verify, где пароль проверяется из тела).
        if (path != null && path.startsWith("/api/v1/admin/") && !path.endsWith("/verify")) {
            String password = credentials.getPassword();
            if (password != null && !password.isEmpty()) {
                Request withAuth = original.newBuilder()
                        .header("X-Admin-Password", password)
                        .build();
                return chain.proceed(withAuth);
            }
        }
        return chain.proceed(original);
    }
}
