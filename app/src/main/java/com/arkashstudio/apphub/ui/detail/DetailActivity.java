package com.arkashstudio.apphub.ui.detail;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arkashstudio.apphub.R;
import com.arkashstudio.apphub.data.local.entity.AppEntity;
import com.arkashstudio.apphub.data.remote.dto.AppDetailDto;
import com.arkashstudio.apphub.data.remote.dto.ScreenshotDto;
import com.arkashstudio.apphub.data.remote.dto.VersionDto;
import com.arkashstudio.apphub.data.repo.DownloadRepository;
import com.arkashstudio.apphub.install.ApkInstaller;
import com.arkashstudio.apphub.ui.common.ActionButton;
import com.arkashstudio.apphub.util.FileUtil;
import com.arkashstudio.apphub.util.PackageUtils;
import com.arkashstudio.apphub.util.UrlUtil;
import com.bumptech.glide.Glide;

import java.io.File;

/**
 * Экран деталей приложения: баннер, иконка, описание, скриншоты,
 * «что нового», кнопка действия.
 */
public class DetailActivity extends AppCompatActivity {

    public static final String EXTRA_APP_ID = "app_id";

    private DetailViewModel vm;
    private DownloadRepository downloadRepo;
    private ActionButton actionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        vm = new ViewModelProvider(this).get(DetailViewModel.class);
        downloadRepo = DownloadRepository.get(this);

        actionButton = findViewById(R.id.detail_action);

        long appId = getIntent().getLongExtra(EXTRA_APP_ID, -1);
        if (appId < 0) { finish(); return; }

        setupObservers();
        vm.load(appId);
    }

    private void setupObservers() {
        vm.app().observe(this, this::bindApp);
        vm.loading().observe(this, loading -> {
            findViewById(R.id.detail_progress).setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
        });
    }

    private void bindApp(AppDetailDto app) {
        if (app == null) return;

        ((TextView) findViewById(R.id.detail_title)).setText(app.title);
        ((TextView) findViewById(R.id.detail_developer)).setText(app.developer);
        ((RatingBar) findViewById(R.id.detail_rating)).setRating((float) app.rating);

        // Иконка.
        ImageView icon = findViewById(R.id.detail_icon);
        if (app.iconUrl != null) {
            Glide.with(this).load(UrlUtil.mediaUrl(app.iconUrl))
                    .placeholder(R.drawable.placeholder_app_icon)
                    .into(icon);
        }

        // Баннер.
        ImageView banner = findViewById(R.id.detail_banner);
        if (app.bannerUrl != null) {
            banner.setVisibility(View.VISIBLE);
            Glide.with(this).load(UrlUtil.mediaUrl(app.bannerUrl)).into(banner);
        } else {
            banner.setVisibility(View.GONE);
        }

        // Описание.
        TextView desc = findViewById(R.id.detail_description);
        desc.setText(app.description != null ? app.description : "");

        // Что нового (последняя версия).
        TextView whatsNew = findViewById(R.id.detail_whats_new);
        if (app.versions != null && !app.versions.isEmpty()) {
            VersionDto latest = app.versions.get(0);
            String notes = latest.releaseNotes;
            whatsNew.setText(notes != null && !notes.isEmpty()
                    ? ("Версия " + latest.versionName + "\n" + notes)
                    : "Версия " + latest.versionName);
        } else {
            whatsNew.setText(R.string.no_versions);
        }

        // Информация о версии.
        bindInfo(app);

        // Скриншоты.
        bindScreenshots(app);

        // Кнопка действия.
        bindAction(app);
    }

    private void bindInfo(AppDetailDto app) {
        LinearLayout info = findViewById(R.id.detail_info);
        info.removeAllViews();
        if (app.versions == null || app.versions.isEmpty()) return;
        VersionDto v = app.versions.get(0);
        addInfoRow(info, "Версия", v.versionName);
        addInfoRow(info, "Размер", FileUtil.formatSize(v.fileSize));
        if (v.minSdk != null) addInfoRow(info, "Мин. Android", "API " + v.minSdk);
        if (app.packageName != null) addInfoRow(info, "Пакет", app.packageName);
    }

    private void addInfoRow(LinearLayout parent, String key, String value) {
        View row = getLayoutInflater().inflate(R.layout.item_info_row, parent, false);
        ((TextView) row.findViewById(R.id.info_key)).setText(key);
        ((TextView) row.findViewById(R.id.info_value)).setText(value);
        parent.addView(row);
    }

    private void bindScreenshots(AppDetailDto app) {
        RecyclerView recycler = findViewById(R.id.detail_screenshots);
        if (app.screenshots == null || app.screenshots.isEmpty()) {
            recycler.setVisibility(View.GONE);
            return;
        }
        recycler.setVisibility(View.VISIBLE);
        recycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recycler.setAdapter(new ScreenshotAdapter(app.screenshots));
    }

    private void bindAction(AppDetailDto app) {
        // Преобразуем DTO в сущность-подобный объект для логики кнопки.
        AppEntity entity = new AppEntity();
        entity.id = app.id;
        entity.packageName = app.packageName;
        entity.latestVersionId = app.versions != null && !app.versions.isEmpty() ? app.versions.get(0).id : null;
        entity.latestVersionCode = app.versions != null && !app.versions.isEmpty() ? app.versions.get(0).versionCode : null;
        entity.latestSha256 = app.versions != null && !app.versions.isEmpty() ? app.versions.get(0).sha256 : null;
        entity.latestMinSdk = app.versions != null && !app.versions.isEmpty() ? app.versions.get(0).minSdk : null;

        ActionButton.Mode mode = resolveMode(entity);
        actionButton.setMode(mode);

        actionButton.setOnActionListener(m -> {
            switch (m) {
                case DOWNLOAD:
                case UPDATE:
                    downloadRepo.startDownload(entity);
                    break;
                case INSTALL: {
                    File apk = FileUtil.apkFileBySha(this, entity.latestSha256);
                    ApkInstaller.install(this, apk);
                    break;
                }
                case OPEN:
                    PackageUtils.launchApp(this, entity.packageName);
                    break;
                case DOWNLOADING:
                    downloadRepo.cancel(entity.packageName);
                    break;
                default:
                    break;
            }
        });

        // Подписка на состояние загрузки.
        downloadRepo.observe(entity.packageName).observe(this, ds -> {
            if (ds != null && ds.status == com.arkashstudio.apphub.data.repo.DownloadState.Status.DOWNLOADING) {
                actionButton.setMode(ActionButton.Mode.DOWNLOADING);
                actionButton.setProgress(ds.progress);
            } else {
                actionButton.setMode(resolveMode(entity));
            }
        });
    }

    private ActionButton.Mode resolveMode(AppEntity app) {
        if (app.latestVersionCode == null) return ActionButton.Mode.DISABLED;
        if (app.latestMinSdk != null && app.latestMinSdk > PackageUtils.deviceSdk()) {
            return ActionButton.Mode.DISABLED;
        }
        Long installedCode = PackageUtils.getInstalledVersionCode(this, app.packageName);
        if (installedCode == null) {
            boolean hasLocal = app.latestSha256 != null
                    && downloadRepo.isApkDownloaded(app.packageName, app.latestSha256);
            return hasLocal ? ActionButton.Mode.INSTALL : ActionButton.Mode.DOWNLOAD;
        }
        int latest = app.latestVersionCode != null ? app.latestVersionCode : 0;
        if (installedCode < latest) return ActionButton.Mode.UPDATE;
        return ActionButton.Mode.OPEN;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Перепроверить состояние кнопки (напр. после установки).
        AppDetailDto app = vm.app().getValue();
        if (app != null) bindAction(app);
    }

    /** Адаптер галереи скриншотов. */
    private static class ScreenshotAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final java.util.List<ScreenshotDto> items;

        ScreenshotAdapter(java.util.List<ScreenshotDto> items) { this.items = items; }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView iv = new ImageView(parent.getContext());
            int w = (int) (parent.getResources().getDisplayMetrics().widthPixels * 0.6);
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(w,
                    (int) (w * 16f / 9f));
            int margin = (int) (8 * parent.getResources().getDisplayMetrics().density);
            lp.setMargins(margin, margin, margin, margin);
            iv.setLayoutParams(lp);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new RecyclerView.ViewHolder(iv) { };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ImageView iv = (ImageView) holder.itemView;
            ScreenshotDto s = items.get(position);
            Glide.with(iv).load(UrlUtil.mediaUrl(s.url))
                    .placeholder(R.drawable.placeholder_app_icon)
                    .into(iv);
        }

        @Override
        public int getItemCount() { return items.size(); }
    }
}
