package com.arkashstudio.apphub.ui.search;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arkashstudio.apphub.R;
import com.arkashstudio.apphub.data.local.entity.AppEntity;
import com.arkashstudio.apphub.data.remote.ApiClient;
import com.arkashstudio.apphub.data.remote.ApiService;
import com.arkashstudio.apphub.data.remote.dto.AppDto;
import com.arkashstudio.apphub.data.remote.dto.PageDto;
import com.arkashstudio.apphub.data.repo.DownloadRepository;
import com.arkashstudio.apphub.data.local.EntityMapper;
import com.arkashstudio.apphub.ui.catalog.AppCardAdapter;
import com.arkashstudio.apphub.ui.detail.DetailActivity;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Экран поиска приложений. Поиск идёт на сервере (через ?query=).
 * Использует дебаунс 400мс, чтобы не спамить запросами при каждом символе.
 */
public class SearchActivity extends AppCompatActivity {

    private static final long DEBOUNCE_MS = 400;

    private AppCardAdapter adapter;
    private EditText searchField;
    private View emptyState;
    private View progress;

    private final java.util.concurrent.ExecutorService exec =
            java.util.concurrent.Executors.newSingleThreadExecutor();
    private final AtomicBoolean queryInFlight = new AtomicBoolean(false);
    private String pendingQuery = null;
    private final android.os.Handler uiHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    private final Runnable debouncedSearch = this::runSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        Toolbar toolbar = findViewById(R.id.search_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.search_title);
        }

        searchField = findViewById(R.id.search_field);
        emptyState = findViewById(R.id.search_empty);
        progress = findViewById(R.id.search_progress);

        RecyclerView list = findViewById(R.id.search_results);
        list.setLayoutManager(new LinearLayoutManager(this));
        DownloadRepository downloadRepo = DownloadRepository.get(this);
        adapter = new AppCardAdapter(this, this, downloadRepo,
                this::openDetail,
                new AppCardAdapter.OnActionListener() {
                    @Override public void onDownload(AppEntity app) { downloadRepo.startDownload(app); }
                    @Override public void onInstall(AppEntity app, java.io.File apkFile) {
                        com.arkashstudio.apphub.install.ApkInstaller.install(SearchActivity.this, apkFile);
                    }
                    @Override public void onOpen(AppEntity app) {
                        com.arkashstudio.apphub.util.PackageUtils.launchApp(SearchActivity.this, app.packageName);
                    }
                    @Override public void onCancel(String packageName) { downloadRepo.cancel(packageName); }
                });
        list.setAdapter(adapter);

        searchField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                uiHandler.removeCallbacks(debouncedSearch);
                String q = s.toString().trim();
                if (q.isEmpty()) {
                    adapter.submitList(java.util.Collections.emptyList());
                    emptyState.setVisibility(View.GONE);
                    return;
                }
                uiHandler.postDelayed(debouncedSearch, DEBOUNCE_MS);
            }
        });

        searchField.requestFocus();
    }

    private void runSearch() {
        final String query = searchField.getText().toString().trim();
        if (query.isEmpty()) return;

        // Если запрос уже летит — запомним и выполним после.
        if (queryInFlight.get()) {
            pendingQuery = query;
            return;
        }
        executeQuery(query);
    }

    private void executeQuery(String query) {
        queryInFlight.set(true);
        runOnUiThread(() -> {
            progress.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        });

        exec.execute(() -> {
            try {
                ApiService api = ApiClient.get();
                Call<com.arkashstudio.apphub.data.remote.dto.ApiResponse<PageDto<AppDto>>> call =
                        api.getCatalog(null, query, 0, 50);
                Response<com.arkashstudio.apphub.data.remote.dto.ApiResponse<PageDto<AppDto>>> resp =
                        call.execute();

                List<AppEntity> results;
                boolean ok = false;
                if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()) {
                    List<AppDto> dtos = resp.body().data.items();
                    results = EntityMapper.toEntities(dtos);
                    ok = true;
                } else {
                    results = java.util.Collections.emptyList();
                }

                final List<AppEntity> finalResults = results;
                final boolean finalOk = ok;
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    adapter.submitList(finalResults);
                    // Показываем «ничего не найдено» только если запрос был успешным
                    // и результатов нет (иначе это может быть ошибка сети).
                    emptyState.setVisibility(
                            finalOk && finalResults.isEmpty() ? View.VISIBLE : View.GONE);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    adapter.submitList(java.util.Collections.emptyList());
                });
            } finally {
                queryInFlight.set(false);
                // Если за время полёта накопился новый запрос — выполним.
                if (pendingQuery != null && !pendingQuery.equals(query)) {
                    String next = pendingQuery;
                    pendingQuery = null;
                    executeQuery(next);
                } else {
                    pendingQuery = null;
                }
            }
        });
    }

    private void openDetail(long appId) {
        Intent i = new Intent(this, DetailActivity.class);
        i.putExtra(DetailActivity.EXTRA_APP_ID, appId);
        startActivity(i);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
