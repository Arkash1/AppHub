package com.arkashstudio.apphub.data.remote;

import com.arkashstudio.apphub.data.remote.dto.ApiResponse;
import com.arkashstudio.apphub.data.remote.dto.AppDetailDto;
import com.arkashstudio.apphub.data.remote.dto.AppDto;
import com.arkashstudio.apphub.data.remote.dto.CheckUpdatesRequest;
import com.arkashstudio.apphub.data.remote.dto.PageDto;
import com.arkashstudio.apphub.data.remote.dto.ScreenshotDto;
import com.arkashstudio.apphub.data.remote.dto.SystemInfoDto;
import com.arkashstudio.apphub.data.remote.dto.VersionDto;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Streaming;

/**
 * Retrofit-интерфейс REST API AppHub.
 * Базовый URL задаётся в ApiClient (из настроек или дефолтный домен).
 */
public interface ApiService {

    // ============= Системные =============

    @GET("api/v1/system/info")
    Call<ApiResponse<SystemInfoDto>> getSystemInfo();

    // ============= Каталог =============

    @GET("api/v1/catalog")
    Call<ApiResponse<PageDto<AppDto>>> getCatalog(
            @Query("category") String category,
            @Query("query") String query,
            @Query("page") int page,
            @Query("size") int size);

    @GET("api/v1/catalog/featured")
    Call<ApiResponse<List<AppDto>>> getFeatured();

    @GET("api/v1/catalog/categories")
    Call<ApiResponse<List<String>>> getCategories();

    @GET("api/v1/catalog/{id}")
    Call<ApiResponse<AppDetailDto>> getAppDetail(@Path("id") long id);

    @POST("api/v1/catalog/check-updates")
    Call<ApiResponse<List<VersionDto>>> checkUpdates(@Body CheckUpdatesRequest request);

    // ============= Скачивание APK =============

    @Streaming
    @GET("api/v1/download/{versionId}")
    Call<ResponseBody> downloadApk(@Path("versionId") long versionId);

    // ============= Админка =============

    @POST("api/v1/admin/verify")
    Call<ApiResponse<Void>> adminVerify(@Body PasswordBody body);

    @GET("api/v1/admin/apps")
    Call<ApiResponse<List<AppDto>>> adminListApps();

    @GET("api/v1/admin/apps/{id}")
    Call<ApiResponse<AppDetailDto>> adminGetApp(@Path("id") long id);

    @Multipart
    @POST("api/v1/admin/apps")
    Call<ApiResponse<AppDto>> adminCreateApp(
            @Part("data") RequestBody data,
            @Part MultipartBody.Part icon,
            @Part MultipartBody.Part banner);

    @Multipart
    @PUT("api/v1/admin/apps/{id}")
    Call<ApiResponse<AppDto>> adminUpdateApp(
            @Path("id") long id,
            @Part("data") RequestBody data,
            @Part MultipartBody.Part icon,
            @Part MultipartBody.Part banner);

    @DELETE("api/v1/admin/apps/{id}")
    Call<ApiResponse<Void>> adminDeleteApp(@Path("id") long id, @Body PasswordBody body);

    @Multipart
    @POST("api/v1/admin/apps/{id}/version")
    Call<ApiResponse<VersionDto>> adminAddVersion(
            @Path("id") long id,
            @Part MultipartBody.Part apk,
            @Part("data") okhttp3.RequestBody data);

    @Multipart
    @POST("api/v1/admin/apps/{id}/screenshots")
    Call<ApiResponse<List<ScreenshotDto>>> adminAddScreenshots(
            @Path("id") long id,
            @Part List<MultipartBody.Part> screenshots);

    @DELETE("api/v1/admin/apps/{id}/screenshots/{screenshotId}")
    Call<ApiResponse<Void>> adminDeleteScreenshot(
            @Path("id") long id,
            @Path("screenshotId") long screenshotId);

    @PUT("api/v1/admin/password")
    Call<ApiResponse<Void>> adminChangePassword(@Body PasswordBody body);

    /** Тело запроса верификации/смены пароля. */
    class PasswordBody {
        public String password;
        public String newPassword;

        public PasswordBody(String password) {
            this.password = password;
        }

        public PasswordBody(String password, String newPassword) {
            this.password = password;
            this.newPassword = newPassword;
        }
    }
}
