package com.arkashstudio.apphub.ui.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.arkashstudio.apphub.R;
import com.arkashstudio.apphub.data.remote.dto.AppDto;
import com.arkashstudio.apphub.util.UrlUtil;
import com.bumptech.glide.Glide;

/**
 * Адаптер списка приложений в админ-панели.
 * Показывает: иконку, название, packageName, бейдж версии и статус публикации.
 */
public class AdminAppAdapter extends ListAdapter<AppDto, AdminAppAdapter.AppVH> {

    public interface OnAppClickListener {
        void onAppClick(long appId);
    }

    private final Context context;
    private final OnAppClickListener listener;

    public AdminAppAdapter(Context context, OnAppClickListener listener) {
        super(DIFF);
        this.context = context;
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<AppDto> DIFF =
            new DiffUtil.ItemCallback<AppDto>() {
                @Override
                public boolean areItemsTheSame(@NonNull AppDto o, @NonNull AppDto n) {
                    return o.id == n.id;
                }
                @Override
                public boolean areContentsTheSame(@NonNull AppDto o, @NonNull AppDto n) {
                    return o.id == n.id && eq(o.title, n.title)
                            && eq(o.latestVersionCode, n.latestVersionCode);
                }
                private static boolean eq(Object a, Object b) {
                    return a == null ? b == null : a.equals(b);
                }
            };

    @NonNull
    @Override
    public AppVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_app, parent, false);
        return new AppVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AppVH holder, int position) {
        AppDto app = getItem(position);
        holder.bind(app);
    }

    class AppVH extends RecyclerView.ViewHolder {
        private final ImageView icon;
        private final TextView title;
        private final TextView pkg;
        private final TextView version;
        private final TextView status;

        AppVH(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.admin_app_icon);
            title = itemView.findViewById(R.id.admin_app_title);
            pkg = itemView.findViewById(R.id.admin_app_package);
            version = itemView.findViewById(R.id.admin_app_version);
            status = itemView.findViewById(R.id.admin_app_status);
            itemView.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onAppClick(getItem(pos).id);
                }
            });
        }

        void bind(AppDto app) {
            title.setText(app.title);
            pkg.setText(app.packageName);

            if (app.latestVersionName != null) {
                version.setVisibility(View.VISIBLE);
                version.setText(context.getString(R.string.version_fmt, app.latestVersionName));
            } else {
                version.setVisibility(View.GONE);
            }

            // Статус публикации: опубликовано / нет версии.
            if (app.hasVersion()) {
                status.setText(R.string.status_published);
                status.setBackgroundResource(R.drawable.badge_success);
            } else {
                status.setText(R.string.status_no_version);
                status.setBackgroundResource(R.drawable.badge_warning);
            }

            if (app.iconUrl != null && !app.iconUrl.isEmpty()) {
                Glide.with(context)
                        .load(UrlUtil.mediaUrl(app.iconUrl))
                        .placeholder(R.drawable.placeholder_app_icon)
                        .into(icon);
            } else {
                icon.setImageResource(R.drawable.placeholder_app_icon);
            }
        }
    }
}
