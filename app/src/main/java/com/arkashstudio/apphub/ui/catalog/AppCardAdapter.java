package com.arkashstudio.apphub.ui.catalog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.arkashstudio.apphub.R;
import com.arkashstudio.apphub.data.local.entity.AppEntity;
import com.arkashstudio.apphub.data.repo.DownloadRepository;
import com.arkashstudio.apphub.data.repo.DownloadState;
import com.arkashstudio.apphub.install.ApkInstaller;
import com.arkashstudio.apphub.ui.common.ActionButton;
import com.arkashstudio.apphub.ui.common.AppCardView;
import com.arkashstudio.apphub.util.FileUtil;
import com.arkashstudio.apphub.util.PackageUtils;
import com.arkashstudio.apphub.util.UrlUtil;

import java.io.File;

/**
 * Адаптер списка приложений каталога.
 *
 * <p>Ключевая логика — определение состояния {@link ActionButton.Mode} по данным
 * приложения, PackageManager и DownloadRepository (см. дизайн-спеку, раздел 3.2).
 */
public class AppCardAdapter extends ListAdapter<AppEntity, AppCardAdapter.CardVH> {

    public interface OnAppClickListener { void onAppClick(long appId); }

    public interface OnActionListener {
        void onDownload(AppEntity app);
        void onInstall(AppEntity app, File apkFile);
        void onOpen(AppEntity app);
        void onCancel(String packageName);
    }

    private final Context context;
    private final LifecycleOwner lifecycleOwner;
    private final DownloadRepository downloadRepo;
    private final OnAppClickListener clickListener;
    private final OnActionListener actionListener;

    public AppCardAdapter(Context context,
                          LifecycleOwner lifecycleOwner,
                          DownloadRepository downloadRepo,
                          OnAppClickListener clickListener,
                          OnActionListener actionListener) {
        super(DIFF);
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
        this.downloadRepo = downloadRepo;
        this.clickListener = clickListener;
        this.actionListener = actionListener;
    }

    private static final DiffUtil.ItemCallback<AppEntity> DIFF =
            new DiffUtil.ItemCallback<AppEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull AppEntity o, @NonNull AppEntity n) {
                    return o.id == n.id;
                }
                @Override
                public boolean areContentsTheSame(@NonNull AppEntity o, @NonNull AppEntity n) {
                    return o.id == n.id
                            && eq(o.latestVersionCode, n.latestVersionCode)
                            && eq(o.latestSha256, n.latestSha256)
                            && eq(o.title, n.title);
                }
                private boolean eq(Object a, Object b) {
                    return a == null ? b == null : a.equals(b);
                }
            };

    @NonNull
    @Override
    public CardVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        AppCardView view = (AppCardView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_card, parent, false);
        return new CardVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardVH holder, int position) {
        AppEntity app = getItem(position);
        holder.bind(app);
    }

    class CardVH extends RecyclerView.ViewHolder {
        private final AppCardView card;

        CardVH(@NonNull AppCardView itemView) {
            super(itemView);
            this.card = itemView;
        }

        void bind(AppEntity app) {
            card.bind(app);
            card.setOnCardClickListener(() -> {
                if (clickListener != null) clickListener.onAppClick(app.id);
            });

            // Иконка через Glide.
            bindIcon(app);

            // Кнопка действия.
            bindActionButton(app);
        }

        private void bindIcon(AppEntity app) {
            if (app.iconUrl != null && !app.iconUrl.isEmpty()) {
                String url = UrlUtil.mediaUrl(app.iconUrl);
                com.bumptech.glide.Glide.with(context)
                        .load(url)
                        .placeholder(R.drawable.placeholder_app_icon)
                        .error(R.drawable.placeholder_app_icon)
                        .into(card.getIconView());
            } else {
                card.getIconView().setImageResource(R.drawable.placeholder_app_icon);
            }
        }

        private void bindActionButton(AppEntity app) {
            ActionButton button = card.getActionButton();

            // Подписка на состояние загрузки.
            LiveData<DownloadState> state = downloadRepo.observe(app.packageName);
            state.observe(lifecycleOwner, ds -> applyState(app, button, ds));

            button.setOnActionListener(mode -> handleAction(app, mode));
        }

        private void applyState(AppEntity app, ActionButton button, DownloadState ds) {
            // Если идёт загрузка — показываем прогресс.
            if (ds != null && ds.status == DownloadState.Status.DOWNLOADING) {
                button.setMode(ActionButton.Mode.DOWNLOADING);
                button.setProgress(ds.progress);
                return;
            }
            // Иначе — определяем по данным приложения и PackageManager.
            ActionButton.Mode mode = resolveMode(app);
            button.setMode(mode);
        }

        private ActionButton.Mode resolveMode(AppEntity app) {
            if (!app.hasVersion()) {
                return ActionButton.Mode.DISABLED;
            }
            // Android-версия устройства.
            if (app.latestMinSdk != null && app.latestMinSdk > PackageUtils.deviceSdk()) {
                return ActionButton.Mode.DISABLED;
            }
            Long installedCode = PackageUtils.getInstalledVersionCode(context, app.packageName);
            if (installedCode == null) {
                // Не установлено → Скачать или Установить (если APK уже в кэше).
                boolean hasLocal = app.latestSha256 != null
                        && downloadRepo.isApkDownloaded(app.packageName, app.latestSha256);
                return hasLocal ? ActionButton.Mode.INSTALL : ActionButton.Mode.DOWNLOAD;
            }
            int latest = app.latestVersionCode != null ? app.latestVersionCode : 0;
            if (installedCode < latest) {
                return ActionButton.Mode.UPDATE;
            }
            return ActionButton.Mode.OPEN;
        }

        private void handleAction(AppEntity app, ActionButton.Mode mode) {
            switch (mode) {
                case DOWNLOAD:
                case UPDATE:
                    if (actionListener != null) actionListener.onDownload(app);
                    break;
                case INSTALL: {
                    File apk = FileUtil.apkFileBySha(context, app.latestSha256);
                    if (actionListener != null) actionListener.onInstall(app, apk);
                    break;
                }
                case OPEN:
                    if (actionListener != null) actionListener.onOpen(app);
                    break;
                case DOWNLOADING:
                    if (actionListener != null) actionListener.onCancel(app.packageName);
                    break;
                default:
                    break;
            }
        }
    }
}
