package com.arkashstudio.apphub.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import com.arkashstudio.apphub.R;
import com.arkashstudio.apphub.ui.catalog.CatalogActivity;
import com.arkashstudio.apphub.ui.common.GradientTextView;

/**
 * Splash screen с анимированным неоновым логотипом AppHub.
 * Длительность ~1200мс, затем переход на главный экран.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION_MS = 1200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        GradientTextView logo = findViewById(R.id.splash_logo);
        android.view.View iconWrap = findViewById(R.id.splash_icon_wrap);

        // Анимация появления: scale + alpha (overshoot для «отпружинивания»).
        iconWrap.setScaleX(0.3f);
        iconWrap.setScaleY(0.3f);
        iconWrap.setAlpha(0f);
        ViewCompat.animate(iconWrap)
                .scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(600)
                .setInterpolator(new OvershootInterpolator(1.3f))
                .start();

        // Логотип появляется чуть позже с fade-in.
        logo.setAlpha(0f);
        logo.setTranslationY(40f);
        ViewCompat.animate(logo)
                .alpha(1f).translationY(0f)
                .setDuration(700)
                .setStartDelay(200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        // Лёгкое увеличение иконки после появления (эффект «дыхания»).
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            ViewCompat.animate(iconWrap)
                    .scaleX(1.08f).scaleY(1.08f)
                    .setDuration(800)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }, 700);

        // Переход на главный экран.
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, CatalogActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, SPLASH_DURATION_MS);
    }
}
