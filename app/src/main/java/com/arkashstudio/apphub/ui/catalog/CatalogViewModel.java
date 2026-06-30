package com.arkashstudio.apphub.ui.catalog;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.arkashstudio.apphub.data.local.entity.AppEntity;
import com.arkashstudio.apphub.data.repo.CatalogRepository;

import java.util.List;

/**
 * ViewModel главного экрана (каталог).
 *
 * <p>UI наблюдает за {@link #apps()} (Room как single source of truth).
 * {@link #refresh()} обновляет кэш с сервера в фоне.
 */
public class CatalogViewModel extends AndroidViewModel {

    private final CatalogRepository repo;

    /** Категория-фильтр (null = все). */
    private final MutableLiveData<String> category = new MutableLiveData<>(null);
    /** Загрузка идёт. */
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    /** Сообщение (ошибка / статус). */
    private final MutableLiveData<String> message = new MutableLiveData<>();
    /** Сервер доступен. */
    private final MutableLiveData<Boolean> serverOnline = new MutableLiveData<>();

    public CatalogViewModel(@NonNull Application application) {
        super(application);
        repo = CatalogRepository.get(application);
    }

    /** Список приложений, реактивно меняющийся при смене категории. */
    public LiveData<List<AppEntity>> apps() {
        return Transformations.switchMap(category, cat -> {
            if (cat == null || cat.isEmpty()) {
                return repo.observeAll();
            } else {
                return repo.observeByCategory(cat);
            }
        });
    }

    public LiveData<Boolean> loading() { return loading; }
    public LiveData<String> message() { return message; }
    public LiveData<Boolean> serverOnline() { return serverOnline; }

    /** Установить фильтр категории (null = все). */
    public void setCategory(String cat) {
        category.setValue(cat);
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
