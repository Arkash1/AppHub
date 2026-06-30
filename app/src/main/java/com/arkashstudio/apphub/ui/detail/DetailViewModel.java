package com.arkashstudio.apphub.ui.detail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.arkashstudio.apphub.data.remote.dto.AppDetailDto;
import com.arkashstudio.apphub.data.repo.CatalogRepository;

/**
 * ViewModel страницы деталей приложения. Загружает детальную страницу с сервера.
 */
public class DetailViewModel extends AndroidViewModel {

    private final CatalogRepository repo;

    private final MutableLiveData<AppDetailDto> app = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public DetailViewModel(@NonNull Application application) {
        super(application);
        repo = CatalogRepository.get(application);
    }

    public LiveData<AppDetailDto> app() { return app; }
    public LiveData<Boolean> loading() { return loading; }
    public LiveData<String> error() { return error; }

    public void load(long appId) {
        loading.setValue(true);
        new Thread(() -> {
            AppDetailDto dto = repo.getDetailSync(appId);
            loading.postValue(false);
            if (dto != null) {
                app.postValue(dto);
            } else {
                error.postValue("Не удалось загрузить данные");
            }
        }).start();
    }
}
