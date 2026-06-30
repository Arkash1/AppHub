package com.arkashstudio.apphub.ui.catalog;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.arkashstudio.apphub.R;
import com.arkashstudio.apphub.data.local.entity.AppEntity;
import com.arkashstudio.apphub.data.repo.DownloadRepository;
import com.arkashstudio.apphub.install.ApkInstaller;
import com.arkashstudio.apphub.ui.detail.DetailActivity;
import com.arkashstudio.apphub.ui.settings.SettingsActivity;
import com.arkashstudio.apphub.util.PackageUtils;
import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.util.List;

/**
 * Главный экран — каталог приложений.
 *
 * <p>Содержит: тулбар с логотипом, вкладки категорий, список карточек с
 * {@link com.arkashstudio.apphub.ui.common.ActionButton}, pull-to-refresh,
 * FAB поиска/настроек, баннер статуса сервера.
 */
public class CatalogActivity extends AppCompatActivity {

    private CatalogViewModel vm;
    private AppCardAdapter adapter;
    private DownloadRepository downloadRepo;

    private SwipeRefreshLayout swipe;
    private View offlineBanner;
    private View emptyState;
    private TabLayout tabs;
    private TextView messageText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        vm = new ViewModelProvider(this).get(CatalogViewModel.class);
        downloadRepo = DownloadRepository.get(this);

        setupToolbar();
        setupTabs();
        setupList();
        setupSwipe();
        setupObservers();
        setupFabs();

        // Первичная загрузка.
        vm.refresh();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.catalog_toolbar);
        setSupportActionBar(toolbar);
        messageText = findViewById(R.id.catalog_message);
    }

    private void setupTabs() {
        tabs = findViewById(R.id.catalog_tabs);
        // Вкладка «Все» по умолчанию.
        tabs.addTab(tabs.newTab().setText(R.string.cat_all));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String cat = tab.getPosition() == 0 ? null : tab.getText().toString();
                vm.setCategory(cat);
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }
            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });

        // Подгрузить категории с сервера в фоне.
        new Thread(() -> {
            List<String> cats = vm.getCategoriesFromServer();
            runOnUiThread(() -> {
                for (String c : cats) {
                    tabs.addTab(tabs.newTab().setText(c));
                }
            });
        }).start();
    }

    private void setupList() {
        RecyclerView list = findViewById(R.id.catalog_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppCardAdapter(this, this, downloadRepo,
                this::openDetail,
                new AppCardAdapter.OnActionListener() {
                    @Override public void onDownload(AppEntity app) { downloadRepo.startDownload(app); }
                    @Override public void onInstall(AppEntity app, File apkFile) {
                        ApkInstaller.install(CatalogActivity.this, apkFile);
                    }
                    @Override public void onOpen(AppEntity app) {
                        PackageUtils.launchApp(CatalogActivity.this, app.packageName);
                    }
                    @Override public void onCancel(String packageName) {
                        downloadRepo.cancel(packageName);
                    }
                });
        list.setAdapter(adapter);
    }

    private void setupSwipe() {
        swipe = findViewById(R.id.catalog_swipe);
        swipe.setOnRefreshListener(() -> vm.refresh());
        swipe.setColorSchemeResources(R.color.primary_neon, R.color.accent_cyan);
    }

    private void setupObservers() {
        offlineBanner = findViewById(R.id.catalog_offline_banner);
        emptyState = findViewById(R.id.catalog_empty);

        vm.apps().observe(this, apps -> {
            adapter.submitList(apps);
            emptyState.setVisibility(apps == null || apps.isEmpty() ? View.VISIBLE : View.GONE);
        });
        vm.loading().observe(this, loading -> {
            swipe.setRefreshing(Boolean.TRUE.equals(loading));
        });
        vm.serverOnline().observe(this, online -> {
            offlineBanner.setVisibility(Boolean.TRUE.equals(online) ? View.GONE : View.VISIBLE);
        });
        vm.message().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                messageText.setText(msg);
                messageText.setVisibility(View.VISIBLE);
                messageText.postDelayed(() -> messageText.setVisibility(View.GONE), 3000);
            }
        });
    }

    private void setupFabs() {
        View settingsFab = findViewById(R.id.catalog_fab_settings);
        settingsFab.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
    }

    private void openDetail(long appId) {
        Intent i = new Intent(this, DetailActivity.class);
        i.putExtra(DetailActivity.EXTRA_APP_ID, appId);
        startActivity(i);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Перепроверяем состояние кнопок (напр. после установки из установщика).
        vm.checkServer();
        // Обновляем список, чтобы ActionButton перечитал установленные приложения.
        adapter.notifyDataSetChanged();
    }
}
