package com.arkashstudio.apphub.ui.common;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arkashstudio.apphub.R;
import com.arkashstudio.apphub.data.local.entity.AppEntity;
import com.arkashstudio.apphub.util.FileUtil;
import com.arkashstudio.apphub.util.UrlUtil;

/**
 * Карточка приложения в каталоге. Биндит {@link AppEntity} в UI: иконку, название,
 * разработчика, рейтинг, размер, и {@link ActionButton} с актуальным состоянием.
 */
public class AppCardView extends FrameLayout {

    private ImageView icon;
    private TextView title;
    private TextView developer;
    private TextView meta;
    private TextView shortDesc;
    private RatingBar rating;
    private ActionButton actionButton;

    public AppCardView(@NonNull Context context) {
        super(context);
        init();
    }

    public AppCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.item_app_card, this, true);
        icon = findViewById(R.id.card_icon);
        title = findViewById(R.id.card_title);
        developer = findViewById(R.id.card_developer);
        meta = findViewById(R.id.card_meta);
        shortDesc = findViewById(R.id.card_short_desc);
        rating = findViewById(R.id.card_rating);
        actionButton = findViewById(R.id.card_action);
    }

    public ActionButton getActionButton() {
        return actionButton;
    }

    public ImageView getIconView() {
        return icon;
    }

    /** Слушатель клика по карточке (переход на детали). */
    public void setOnCardClickListener(Runnable r) {
        View root = findViewById(R.id.card_root);
        if (root != null) root.setOnClickListener(v -> {
            if (r != null) r.run();
        });
    }

    /** Установить данные приложения (без состояния кнопки — оно биндится отдельно). */
    public void bind(AppEntity app) {
        title.setText(app.title);
        developer.setText(app.developer != null ? app.developer : "");
        rating.setRating((float) app.rating);
        shortDesc.setText(app.shortDesc != null ? app.shortDesc : "");
        shortDesc.setVisibility(app.shortDesc != null && !app.shortDesc.isEmpty() ? VISIBLE : GONE);
        String size = app.latestFileSize != null ? FileUtil.formatSize(app.latestFileSize) : "";
        String version = app.latestVersionName != null ? app.latestVersionName : "";
        meta.setText(android.text.TextUtils.join("  •  ",
                java.util.Arrays.asList(version, size).stream()
                        .filter(s -> s != null && !s.isEmpty())
                        .toArray(String[]::new)));
    }
}
