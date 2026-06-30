package com.arkashstudio.apphub.ui.settings;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.arkashstudio.apphub.data.local.AdminCredentialsStore;
import com.arkashstudio.apphub.data.local.SettingsRepository;
import com.arkashstudio.apphub.data.remote.ApiClient;
import com.arkashstudio.apphub.data.remote.ApiService;
import com.arkashstudio.apphub.data.remote.dto.ApiResponse;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

/**
 * ViewModel экрана настроек. Обрабатывает: смену адреса сервера, проверку пароля
 * администратора, выход из режима админа.
 */
public class SettingsViewModel extends AndroidViewModel {

    private final SettingsRepository settings;
    private final AdminCredentialsStore credentials;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private final MutableLiveData<Boolean> adminAuthorized = new MutableLiveData<>();
    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<String> serverUrl = new MutableLiveData<>();

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        settings = new SettingsRepository(application);
        credentials = new AdminCredentialsStore(application);
        adminAuthorized.setValue(credentials.isAuthorized());
    }

    public LiveData<Boolean> adminAuthorized() { return adminAuthorized; }
    public LiveData<String> message() { return message; }
    public LiveData<String> serverUrl() { return serverUrl; }

    /** Текущий адрес сервера (вызывать в фоне). */
    public void loadServerUrl() {
        io.execute(() -> {
            String url = settings.getServerUrlSync();
            boolean usingDefault = settings.isUsingDefault();
            serverUrl.postValue(usingDefault ? url + " (по умолчанию)" : url);
        });
    }

    /** Сохранить новый адрес сервера и пересоздать клиент. */
    public void setServerUrl(String url) {
        io.execute(() -> {
            settings.setServerUrl(url);
            ApiClient.rebuild(url, getApplication());
            serverUrl.postValue(url);
            message.postValue("Адрес сервера обновлён");
        });
    }

    /** Сбросить адрес к дефолтному домену. */
    public void resetServerUrl() {
        io.execute(() -> {
            settings.resetServerUrl();
            String def = settings.getDefaultUrl();
            ApiClient.rebuild(def, getApplication());
            serverUrl.postValue(def + " (по умолчанию)");
            message.postValue("Сброшено к адресу по умолчанию");
        });
    }

    /** Проверить пароль администратора на сервере. */
    public void verifyAdminPassword(String password) {
        io.execute(() -> {
            try {
                ApiService api = ApiClient.get();
                Call<ApiResponse<Void>> call = api.adminVerify(
                        new ApiService.PasswordBody(password));
                Response<ApiResponse<Void>> resp = call.execute();
                if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()) {
                    // Сохраняем пароль для дальнейших админ-запросов.
                    credentials.savePassword(password);
                    adminAuthorized.postValue(true);
                    message.postValue("Вход выполнен");
                } else {
                    String err = resp.body() != null ? resp.body().errorMessage()
                            : ("Ошибка " + resp.code());
                    adminAuthorized.postValue(false);
                    message.postValue(err);
                }
            } catch (Exception e) {
                adminAuthorized.postValue(false);
                message.postValue("Ошибка сети: " + e.getMessage());
            }
        });
    }

    /** Выйти из режима администратора. */
    public void logoutAdmin() {
        credentials.clear();
        adminAuthorized.postValue(false);
        message.postValue("Вы вышли из режима администратора");
    }
}
