package com.arkashstudio.apphub.ui.admin;

import android.content.Context;

import com.arkashstudio.apphub.data.remote.ApiClient;
import com.arkashstudio.apphub.data.remote.ApiService;
import com.arkashstudio.apphub.data.remote.dto.ApiResponse;
import com.arkashstudio.apphub.data.remote.dto.AppDetailDto;
import com.arkashstudio.apphub.data.remote.dto.AppDto;
import com.arkashstudio.apphub.data.remote.dto.ScreenshotDto;
import com.arkashstudio.apphub.data.remote.dto.VersionDto;

import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Синхронный репозиторий админских операций (используется из фоновых потоков).
 * Все вызовы идут через {@link ApiService} с заголовком X-Admin-Password,
 * который добавляет {@link com.arkashstudio.apphub.data.remote.AdminAuthInterceptor}.
 */
public class AdminRepository {

    private final ApiService api;

    public AdminRepository() {
        this.api = ApiClient.get();
    }

    // ===== Приложения =====

    public Result<List<AppDto>> listApps() {
        return exec(api.adminListApps());
    }

    public Result<AppDetailDto> getApp(long id) {
        return exec(api.adminGetApp(id));
    }

    public Result<AppDto> createApp(String json, java.io.File icon, java.io.File banner) {
        RequestBody data = jsonPart(json);
        MultipartBody.Part iconPart = filePart(icon, "icon");
        MultipartBody.Part bannerPart = filePart(banner, "banner");
        return exec(api.adminCreateApp(data, iconPart, bannerPart));
    }

    public Result<AppDto> updateApp(long id, String json, java.io.File icon, java.io.File banner) {
        RequestBody data = jsonPart(json);
        MultipartBody.Part iconPart = filePart(icon, "icon");
        MultipartBody.Part bannerPart = filePart(banner, "banner");
        return exec(api.adminUpdateApp(id, data, iconPart, bannerPart));
    }

    public Result<Void> deleteApp(long id, String password) {
        return exec(api.adminDeleteApp(id, new ApiService.PasswordBody(password)));
    }

    // ===== Версии =====

    public Result<VersionDto> addVersion(long appId, java.io.File apk, String releaseNotes) {
        MultipartBody.Part apkPart = filePart(apk, "apk");
        // releaseNotes может быть null/пустым — передаём пустой JSON,
        // т.к. Retrofit @Part("data") не принимает null.
        String notes = releaseNotes == null ? "" : releaseNotes;
        String dataJson = "{\"releaseNotes\":\"" + escape(notes) + "\"}";
        RequestBody data = jsonPart(dataJson);
        return exec(api.adminAddVersion(appId, apkPart, data));
    }

    public Result<List<ScreenshotDto>> addScreenshots(long appId, List<java.io.File> files) {
        List<MultipartBody.Part> parts = new java.util.ArrayList<>();
        for (java.io.File f : files) {
            parts.add(MultipartBody.Part.createFormData("screenshots", f.getName(),
                    RequestBody.create(f, guessMediaType(f))));
        }
        return exec(api.adminAddScreenshots(appId, parts));
    }

    public Result<Void> deleteScreenshot(long appId, long screenshotId) {
        return exec(api.adminDeleteScreenshot(appId, screenshotId));
    }

    // ===== Пароль =====

    public Result<Void> changePassword(String oldPwd, String newPwd) {
        return exec(api.adminChangePassword(new ApiService.PasswordBody(oldPwd, newPwd)));
    }

    // ===== Helpers =====

    private RequestBody jsonPart(String json) {
        return RequestBody.create(json, MediaType.parse("application/json"));
    }

    private MultipartBody.Part filePart(java.io.File file, String name) {
        if (file == null || !file.exists()) {
            // Пустая часть — сервер проигнорирует null файлы.
            return MultipartBody.Part.createFormData(name, "",
                    RequestBody.create(new byte[0], null));
        }
        return MultipartBody.Part.createFormData(name, file.getName(),
                RequestBody.create(file, guessMediaType(file)));
    }

    private MediaType guessMediaType(java.io.File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".apk")) return MediaType.parse("application/vnd.android.package-archive");
        if (name.endsWith(".png")) return MediaType.parse("image/png");
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return MediaType.parse("image/jpeg");
        if (name.endsWith(".webp")) return MediaType.parse("image/webp");
        return MediaType.parse("application/octet-stream");
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    /** Обёртка выполнения Retrofit-вызова в синхронный Result. */
    private <T> Result<T> exec(Call<ApiResponse<T>> call) {
        try {
            Response<ApiResponse<T>> resp = call.execute();
            if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()) {
                return Result.ok(resp.body().data);
            }
            String err = resp.body() != null ? resp.body().errorMessage()
                    : ("HTTP " + resp.code());
            return Result.fail(err);
        } catch (Exception e) {
            return Result.fail("Ошибка сети: " + e.getMessage());
        }
    }

    /** Результат операции. */
    public static class Result<T> {
        public final boolean success;
        public final T data;
        public final String error;
        private Result(boolean ok, T data, String error) {
            this.success = ok; this.data = data; this.error = error;
        }
        public static <T> Result<T> ok(T data) { return new Result<>(true, data, null); }
        public static <T> Result<T> fail(String error) { return new Result<>(false, null, error); }
    }
}
