package com.arkashstudio.apphub.ui.catalog;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.arkashstudio.apphub.data.local.entity.AppEntity;
import com.arkashstudio.apphub.data.repo.CatalogRepository;
import com.arkashstudio.apphub.util.PackageUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel главного экрана (каталог).
 *
 * <p>UI наблюдает за {@link #apps()} (Room как single source of truth).
 * {@link #refresh()} обновляет кэш с сервера в фоне.
 *
 * <p>Режим «Мои приложения» ({@link #setShowMine(boolean)}): фильтрует кэш по
 * приложениям, установленным на устройстве через PackageManager.
 *
 * <p>Фильтрация выполняется реактивно через MediatorLiveData на последнем
 * известном значении кэша — БД-запросы на главном потоке НЕ выполняются.
 */
public class CatalogViewModel extends AndroidViewModel {

    private final CatalogRepository repo;

    /** Категория-фильтр (null = все). */
    private final MutableLiveData<String> category = new MutableLiveData<>(null);
    /** Режим «Мои приложения» — фильтр по установленным. */
    private final MutableLiveData<Boolean> mineMode = new MutableLiveData<>(false);
    /** Загрузка идёт. */
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    /** Сообщение (ошибка / статус). */
    private final MutableLiveData<String> message = new MutableLiveData<>();
    /** Сервер доступен. */
    private final MutableLiveData<Boolean> serverOnline = new MutableLiveData<>();

    /** MediatorLiveData: объединяет кэш Room + фильтры в один поток для UI. */
    private MediatorLiveData<List<AppEntity>> filteredApps;
    /** Последнее известное значение кэша (для пересчёта фильтров без БД-запроса). */
    private List<AppEntity> lastCache = new ArrayList<>();

    public CatalogViewModel(@NonNull Application application) {
        super(application);
        repo = CatalogRepository.get(application);
    }

    /**
     * Список приложений, реактивно меняющийся при смене категории или режима «Мои».
     * Слушает три источника: кэш Room + флаг mineMode + категория.
     * Никаких синхронных БД-запросов — фильтрация на lastCache.
     */
    public LiveData<List<AppEntity>> apps() {
        if (filteredApps == null) {
            filteredApps = new MediatorLiveData<>();
            filteredApps.addSource(repo.observeAll(), all -> {
                lastCache = (all != null) ? all : new ArrayList<>();
                filteredApps.setValue(applyFilters(lastCache));
            });
            filteredApps.addSource(mineMode, b -> filteredApps.setValue(applyFilters(lastCache)));
            filteredApps.addSource(category, c -> filteredApps.setValue(applyFilters(lastCache)));
        }
        return filteredApps;
    }

    /** Применяет фильтр категории и (опционально) режим «Мои». Без БД-запросов. */
    private List<AppEntity> applyFilters(List<AppEntity> all) {
        if (all == null) return new ArrayList<>();

        Boolean mine = mineMode.getValue();
        String cat = category.getValue();

        List<AppEntity> filtered = new ArrayList<>(all.size());

        // Режим «Мои» имеет приоритет — в нём категория игнорируется.
        if (Boolean.TRUE.equals(mine)) {
            for (AppEntity a : all) {
                if (a.packageName != null
                        && PackageUtils.isInstalled(getApplication(), a.packageName)) {
                    filtered.add(a);
                }
            }
            return filtered;
        }

        // Обычный режим — фильтр по категории.
        if (cat == null || cat.isEmpty()) {
            return all;
        }
        for (AppEntity a : all) {
            if (cat.equals(a.category)) {
                filtered.add(a);
            }
        }
        return filtered;
    }

    public LiveData<Boolean> loading() { return loading; }
    public LiveData<String> message() { return message; }
    public LiveData<Boolean> serverOnline() { return serverOnline; }

    /** Установить фильтр категории (null = все). Сбрасывает режим «Мои». */
    public void setCategory(String cat) {
        mineMode.setValue(false);
        category.setValue(cat);
    }

    /** Включить/выключить режим «Мои приложения». */
    public void setShowMine(boolean showMine) {
        if (showMine) category.setValue(null);
        mineMode.setValue(showMine);
    }

    public boolean isMineMode() {
        return Boolean.TRUE.equals(mineMode.getValue());
    }

    /** Текущая выбранная категория. */
    public String currentCategory() {
        return category.getValue();
    }

    /** Обновить каталог с сервера. */
    public void refresh() {
        loading.setValue(true);
        repo.refresh(result -> {
            loading.postValue(false);
            if (result.success) {
                message.postValue("Каталог обновлён");
                serverOnline.postValue(true);
            } else {
                message.postValue(result.error);
                serverOnline.postValue(false);
            }
        });
    }

    /** Проверить доступность сервера (без обновления каталога). */
    public void checkServer() {
        new Thread(() -> {
            boolean online = repo.checkServerSync() != null;
            serverOnline.postValue(online);
        }).start();
    }

    public void clearMessage() {
        message.setValue(null);
    }

    /** Получить список категорий с сервера (синхронно — вызывать в фоне). */
    public List<String> getCategoriesFromServer() {
        return repo.getCategoriesSync();
    }
}
