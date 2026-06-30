package com.arkashstudio.apphub.ui.common;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

import com.arkashstudio.apphub.R;

/**
 * Кнопка действия приложения, которая морфит capsule-кнопку в неоновый прогресс-бар.
 *
 * <p>Состояния (см. дизайн-спеку):
 * <ul>
 *   <li>{@link Mode#DOWNLOAD} — «Скачать» (неон-градиент).</li>
 *   <li>{@link Mode#INSTALL} — «Установить» (неон-градиент).</li>
 *   <li>{@link Mode#UPDATE} — «Обновить» (неон-градиент).</li>
 *   <li>{@link Mode#OPEN} — «Открыть» (зелёный).</li>
 *   <li>{@link Mode#DISABLED} — недоступно (серый, нет клика).</li>
 *   <li>{@link Mode#DOWNLOADING} — прогресс-бар + «Отмена».</li>
 * </ul>
 */
public class ActionButton extends FrameLayout {

    public enum Mode {
        DOWNLOAD, INSTALL, UPDATE, OPEN, DISABLED, DOWNLOADING
    }

    public interface OnActionListener {
        void onAction(Mode mode);
    }

    private AppCompatButton button;
    private ProgressBar progress;
    private TextView percentText;
    private AppCompatButton cancelButton;

    private Mode mode = Mode.DOWNLOAD;
    private int progressPercent = 0;
    private OnActionListener listener;

    @ColorInt private static final int NEON_START = Color.parseColor("#7C4DFF");
    @ColorInt private static final int NEON_END = Color.parseColor("#00E5FF");
    @ColorInt private static final int GREEN = Color.parseColor("#00E676");
    @ColorInt private static final int GRAY = Color.parseColor("#3A4060");

    public ActionButton(@NonNull Context context) {
        super(context);
        init(context);
    }

    public ActionButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ActionButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_action_button, this, true);
        button = findViewById(R.id.action_button);
        progress = findViewById(R.id.action_progress);
        percentText = findViewById(R.id.action_percent);
        cancelButton = findViewById(R.id.action_cancel);

        button.setOnClickListener(v -> {
            if (listener != null && mode != Mode.DISABLED && mode != Mode.DOWNLOADING) {
                listener.onAction(mode);
            }
        });
        cancelButton.setOnClickListener(v -> {
            if (listener != null) listener.onAction(Mode.DOWNLOADING); // отменить
        });

        applyMode();
    }

    public void setOnActionListener(OnActionListener listener) {
        this.listener = listener;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        applyMode();
    }

    public void setProgress(int percent) {
        progressPercent = Math.max(0, Math.min(100, percent));
        if (mode == Mode.DOWNLOADING) {
            progress.setProgress(progressPercent);
            percentText.setText(progressPercent + "%");
        }
    }

    public Mode getMode() {
        return mode;
    }

    private void applyMode() {
        boolean isDownloading = mode == Mode.DOWNLOADING;

        // Плавный fade между кнопкой и прогресс-баром.
        button.animate().alpha(isDownloading ? 0f : 1f).setDuration(180).start();
        progress.animate().alpha(isDownloading ? 1f : 0f).setDuration(180).start();
        percentText.animate().alpha(isDownloading ? 1f : 0f).setDuration(180).start();
        cancelButton.animate().alpha(isDownloading ? 1f : 0f).setDuration(180).start();

        button.setEnabled(mode != Mode.DISABLED && mode != Mode.DOWNLOADING);
        progress.setVisibility(isDownloading ? VISIBLE : INVISIBLE);
        percentText.setVisibility(isDownloading ? VISIBLE : INVISIBLE);
        cancelButton.setVisibility(isDownloading ? VISIBLE : INVISIBLE);
        cancelButton.setEnabled(isDownloading);

        if (!isDownloading) {
            button.setBackground(makeCapsule(mode));
            button.setTextColor(Color.WHITE);
            button.setText(labelFor(mode));
        } else {
            progress.setProgress(progressPercent);
            percentText.setText(progressPercent + "%");
        }
    }

    private String labelFor(Mode m) {
        switch (m) {
            case INSTALL: return getContext().getString(R.string.btn_install);
            case UPDATE: return getContext().getString(R.string.btn_update);
            case OPEN: return getContext().getString(R.string.btn_open);
            case DISABLED: return getContext().getString(R.string.btn_unavailable);
            case DOWNLOADING: return "";
            case DOWNLOAD:
            default: return getContext().getString(R.string.btn_download);
        }
    }

    /**
     * Capsule-фон с неон-градиентом (для DOWNLOAD/INSTALL/UPDATE) или заливкой
     * (OPEN=зелёный, DISABLED=серый).
     */
    private Drawable makeCapsule(Mode m) {
        GradientDrawable capsule = new GradientDrawable();
        capsule.setShape(GradientDrawable.RECTANGLE);
        capsule.setCornerRadius(dp(24));

        switch (m) {
            case OPEN:
                capsule.setColor(GREEN);
                break;
            case DISABLED:
                capsule.setColor(GRAY);
                break;
            case DOWNLOAD:
            case INSTALL:
            case UPDATE:
            default:
                capsule.setColors(new int[]{NEON_START, NEON_END});
                capsule.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
                break;
        }

        // Лёгкая тень-свечение под неоновыми кнопками.
        if (m == Mode.DOWNLOAD || m == Mode.INSTALL || m == Mode.UPDATE) {
            GradientDrawable glow = new GradientDrawable();
            glow.setShape(GradientDrawable.RECTANGLE);
            glow.setCornerRadius(dp(26));
            glow.setColor(adjustAlpha(NEON_START, 0.35f));
            LayerDrawable layered = new LayerDrawable(new Drawable[]{glow, capsule});
            layered.setLayerInset(1, dp(2), dp(2), dp(2), dp(2));
            return layered;
        }
        return capsule;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    @ColorInt
    private static int adjustAlpha(@ColorInt int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }
}
