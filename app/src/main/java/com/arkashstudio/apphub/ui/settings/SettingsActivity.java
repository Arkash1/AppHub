package com.arkashstudio.apphub.ui.settings;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.arkashstudio.apphub.BuildConfig;
import com.arkashstudio.apphub.R;
import com.arkashstudio.apphub.data.repo.CatalogRepository;
import com.arkashstudio.apphub.ui.admin.AdminListActivity;

/**
 * Экран настроек.
 *
 * <p>Функционал:
 * <ul>
 *   <li>адрес сервера (текущий, ручной ввод, сброс к дефолтному);</li>
 *   <li>проверка обновлений каталога;</li>
 *   <li>о приложении;</li>
 *   <li><b>пасхалка</b>: 5 тапов по «developed by Arkash's Studio» → карточка
 *       «Режим администратора» → ввод пароля → вход в админ-панель.</li>
 * </ul>
 */
public class SettingsActivity extends AppCompatActivity {

    private static final int TAPS_TO_UNLOCK = 5;
    private static final long TAP_RESET_MS = 1500;

    private SettingsViewModel vm;

    private int tapCount = 0;
    private long lastTapTime = 0;
    private final Handler tapHandler = new Handler(Looper.getMainLooper());

    private TextView developedBy;
    private View adminRevealCard;
    private View adminLogoutRow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        vm = new ViewModelProvider(this).get(SettingsViewModel.class);

        bindConnectionSettings();
        bindCatalogSettings();
        bindAboutSettings();
        bindEasterEgg();
        observe();
        vm.loadServerUrl();
    }

    // ===== Подключение =====

    private void bindConnectionSettings() {
        TextView urlView = findViewById(R.id.settings_server_url);
        vm.serverUrl().observe(this, urlView::setText);

        findViewById(R.id.settings_set_url).setOnClickListener(v -> showUrlInputDialog());
        findViewById(R.id.settings_reset_url).setOnClickListener(v -> vm.resetServerUrl());
    }

    private void showUrlInputDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setHint("https://apphub.example.ru");
        input.setPadding(dp(16), dp(12), dp(16), dp(12));

        new AlertDialog.Builder(this, R.style.DialogTheme)
                .setTitle(R.string.set_server_url_title)
                .setView(input)
                .setPositiveButton(R.string.save, (d, w) -> {
                    String url = input.getText().toString().trim();
                    if (!url.isEmpty()) vm.setServerUrl(url);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ===== Каталог =====

    private void bindCatalogSettings() {
        findViewById(R.id.settings_check_updates).setOnClickListener(v -> {
            Toast.makeText(this, R.string.checking_updates, Toast.LENGTH_SHORT).show();
            new Thread(() -> {
                com.arkashstudio.apphub.data.remote.dto.SystemInfoDto info =
                        CatalogRepository.get(this).checkServerSync();
                runOnUiThread(() -> {
                    if (info == null) {
                        Toast.makeText(this, R.string.server_unavailable, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this,
                                getString(R.string.catalog_version) + info.catalogVersion,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        });

        // Очистка кэша загрузок.
        TextView cacheSizeView = findViewById(R.id.settings_cache_size);
        View clearCacheBtn = findViewById(R.id.settings_clear_cache);
        refreshCacheSize(cacheSizeView);
        clearCacheBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(this, R.style.DialogTheme)
                    .setTitle(R.string.clear_cache)
                    .setMessage(R.string.clear_cache_confirm)
                    .setPositiveButton(R.string.yes, (d, w) -> {
                        new Thread(() -> {
                            com.arkashstudio.apphub.util.FileUtil.clearDownloadsDir(this);
                            runOnUiThread(() -> {
                                refreshCacheSize(cacheSizeView);
                                Toast.makeText(this, R.string.cache_cleared, Toast.LENGTH_SHORT).show();
                            });
                        }).start();
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
        });
    }

    private void refreshCacheSize(TextView cacheSizeView) {
        new Thread(() -> {
            long size = com.arkashstudio.apphub.util.FileUtil.downloadsDirSize(this);
            String text = size > 0
                    ? getString(R.string.cache_size_fmt, com.arkashstudio.apphub.util.FileUtil.formatSize(size))
                    : getString(R.string.cache_empty);
            runOnUiThread(() -> cacheSizeView.setText(text));
        }).start();
    }

    // ===== О приложении =====

    private void bindAboutSettings() {
        TextView version = findViewById(R.id.settings_app_version);
        version.setText(getString(R.string.app_version_fmt, BuildConfig.APP_VERSION));
    }

    // ===== Пасхалка 5 тапов =====

    private void bindEasterEgg() {
        developedBy = findViewById(R.id.settings_developed_by);
        adminRevealCard = findViewById(R.id.settings_admin_reveal);
        adminLogoutRow = findViewById(R.id.settings_admin_logout);

        // Карточка скрыта по умолчанию.
        adminRevealCard.setVisibility(View.GONE);
        adminRevealCard.setAlpha(0f);
        adminRevealCard.setTranslationY(dp(60));

        developedBy.setOnClickListener(v -> handleDevelopedByTap());

        findViewById(R.id.settings_admin_enter).setOnClickListener(v -> showPasswordDialog());
        adminLogoutRow.setOnClickListener(v -> {
            new AlertDialog.Builder(this, R.style.DialogTheme)
                    .setTitle(R.string.admin_logout)
                    .setMessage(R.string.admin_logout_confirm)
                    .setPositiveButton(R.string.yes, (d, w) -> vm.logoutAdmin())
                    .setNegativeButton(R.string.no, null)
                    .show();
        });
    }

    private void handleDevelopedByTap() {
        long now = System.currentTimeMillis();
        // Сброс счётчика, если между тапами прошло слишком много времени.
        if (now - lastTapTime > TAP_RESET_MS) {
            tapCount = 0;
        }
        lastTapTime = now;
        tapCount++;

        // Лёгкая пульсация надписи при каждом тапе.
        developedBy.animate().scaleX(1.15f).scaleY(1.15f).setDuration(80)
                .withEndAction(() -> developedBy.animate().scaleX(1f).scaleY(1f).setDuration(80).start())
                .start();

        if (tapCount >= TAPS_TO_UNLOCK) {
            tapCount = 0;
            revealAdminCard();
        } else {
            // Таймер сброса, если пользователь не продолжит тапать.
            tapHandler.removeCallbacksAndMessages(null);
            tapHandler.postDelayed(() -> tapCount = 0, TAP_RESET_MS);
        }
    }

    /** Анимация появления карточки «Режим администратора». */
    private void revealAdminCard() {
        adminRevealCard.setVisibility(View.VISIBLE);

        ObjectAnimator translate = ObjectAnimator.ofFloat(adminRevealCard, "translationY", dp(60), 0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(adminRevealCard, "alpha", 0f, 1f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(adminRevealCard, "scaleX", 0.8f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(adminRevealCard, "scaleY", 0.8f, 1f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(translate, alpha, scaleX, scaleY);
        set.setDuration(420);
        set.setInterpolator(new OvershootInterpolator(1.2f));
        set.start();

        // Пульсация для привлечения внимания.
        developedBy.animate().alpha(0.5f).setDuration(200).withEndAction(() ->
                developedBy.animate().alpha(1f).setDuration(200).start()).start();
    }

    private void showPasswordDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(20), dp(8), dp(20), dp(8));
        container.addView(input);

        final AlertDialog dialog = new AlertDialog.Builder(this, R.style.DialogTheme)
                .setTitle(R.string.admin_password_title)
                .setView(container)
                .setPositiveButton(R.string.confirm, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.setOnShowListener(d -> {
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positive.setOnClickListener(v -> {
                String pwd = input.getText().toString();
                if (pwd.isEmpty()) return;
                vm.verifyAdminPassword(pwd);
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private void observe() {
        vm.adminAuthorized().observe(this, authorized -> {
            adminLogoutRow.setVisibility(Boolean.TRUE.equals(authorized) ? View.VISIBLE : View.GONE);
        });
        vm.message().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                if ("Вход выполнен".equals(msg)) {
                    startActivity(new Intent(this, AdminListActivity.class));
                }
            }
        });
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
