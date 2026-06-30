package com.arkashstudio.apphub.data.repo;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.arkashstudio.apphub.data.local.AppDatabase;
import com.arkashstudio.apphub.data.local.EntityMapper;
import com.arkashstudio.apphub.data.local.SettingsRepository;
import com.arkashstudio.apphub.data.local.dao.AppDao;
import com.arkashstudio.apphub.data.local.entity.AppEntity;
import com.arkashstudio.apphub.data.remote.ApiClient;
import com.arkashstudio.apphub.data.remote.ApiService;
import com.arkashstudio.apphub.data.remote.dto.ApiResponse;
import com.arkashstudio.apphub.data.remote.dto.AppDetailDto;
import com.arkashstudio.apphub.data.remote.dto.AppDto;
import com.arkashstudio.apphub.data.remote.dto.PageDto;
import com.arkashstudio.apphub.data.remote.dto.SystemInfoDto;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Репозиторий каталога приложений.
 *
 * <p>Single source of truth: UI всегда читает из Room ({@link #observeAll()}),
 * а сеть обновляет кэш в фоне. Так каталог работает офлайн.
 */
public class CatalogRepository {

    private static volatile CatalogRepository instance;

    private final AppDao dao;
    private final SettingsRepository settings;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private CatalogRepository(Context context) {
        this.dao = AppDatabase.get(context).appDao();
        this.settings = new SettingsRepository(context);
    }

    public static CatalogRepository get(Context context) {
        if (instance == null) {
            synchronized (CatalogRepository.class) {
                if (instance == null) {
                    instance = new CatalogRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    // ============ UI: наблюдение за кэшем ============

    /** Все приложения (с категорией или без). */
    public LiveData<List<AppEntity>> observeAll() {
        return dao.observeAll();
    }

    /** Приложения одной категории. */
    public LiveData<List<AppEntity>> observeByCategory(String category) {
        return dao.observeByCategory(category);
    }

    // ============ Сеть: обновление кэша ============

    /**
     * Результат обновления каталога с сервера.
     */
    public static class Result {
        public final boolean success;
        public final String error;
        public final int count;

        private Result(boolean success, String error, int count) {
            this.success = success;
            this.error = error;
            this.count = count;
        }

        public static Result ok(int count) { return new Result(true, null, count); }
        public static Result fail(String error) { return new Result(false, error, 0); }
    }

    public interface Callback { void onResult(Result result); }

    /**
     * Асинхронно обновить каталог с сервера. Сохраняет в Room, обновляет catalogVersion.
     */
    public void refresh(Callback callback) {
        io.execute(() -> {
            try {
                ApiService api = ApiClient.get();
                // Загружаем все страницы (для простоты — большую страницу).
                Call<ApiResponse<PageDto<AppDto>>> call = api.getCatalog(null, null, 0, 200);
                Response<ApiResponse<PageDto<AppDto>>> resp = call.execute();
                if (!resp.isSuccessful() || resp.body() == null || !resp.body().isSuccess()) {
                    String err = resp.body() != null ? resp.body().errorMessage() : ("HTTP " + resp.code());
                    notify(callback, Result.fail(err));
                    return;
                }
                List<AppDto> apps = resp.body().data.items();
                List<AppEntity> entities = EntityMapper.toEntities(apps);

                // Атомарно заменяем кэш.
                dao.clear();
                dao.upsertAll(entities);

                long cv = resp.body().catalogVersion;
                settings.setCatalogVersion(cv);

                notify(callback, Result.ok(entities.size()));
            } catch (Exception e) {
                notify(callback, Result.fail("Ошибка сети: " + e.getMessage()));
            }
        });
    }

    /**
     * Синхронная детальная страница (вызывать в фоне). null при ошибке.
     */
    public AppDetailDto getDetailSync(long id) {
        try {
            Response<ApiResponse<AppDetailDto>> resp = ApiClient.get().getAppDetail(id).execute();
            if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()) {
                return resp.body().data;
            }
        } catch (Exception ignored) { }
        return null;
    }

    /**
     * Проверить доступность сервера и версию каталога. null при недоступности.
     */
    public SystemInfoDto checkServerSync() {
        try {
            Response<ApiResponse<SystemInfoDto>> resp = ApiClient.get().getSystemInfo().execute();
            if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()) {
                return resp.body().data;
            }
        } catch (Exception ignored) { }
        return null;
    }

    /** Возвращает категории (синхронно, в фоне). */
    public List<String> getCategoriesSync() {
        try {
            Response<ApiResponse<List<String>>> resp = ApiClient.get().getCategories().execute();
            if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()) {
                return resp.body().data;
            }
        } catch (Exception ignored) { }
        return java.util.Collections.emptyList();
    }

    /** Сохранённая версия каталога. */
    public long cachedCatalogVersion() {
        return settings.getCatalogVersionSync();
    }

    private void notify(Callback cb, Result r) {
        if (cb != null) cb.onResult(r);
    }
}
