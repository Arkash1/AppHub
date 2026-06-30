package com.arkashstudio.apphub.ui.admin;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.arkashstudio.apphub.R;
import com.arkashstudio.apphub.data.remote.dto.AppDetailDto;
import com.arkashstudio.apphub.data.remote.dto.VersionDto;
import com.arkashstudio.apphub.util.FileUtil;
import com.arkashstudio.apphub.util.UrlUtil;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Админка: редактирование приложения.
 *
 * <p>Возможности:
 * <ul>
 *   <li>редактирование полей (title, description, иконка, баннер, категория и т.д.);</li>
 *   <li>загрузка нового APK (с notes);</li>
 *   <li>просмотр/удаление существующих версий;</li>
 *   <li>удаление приложения с подтверждением паролем.</li>
 * </ul>
 */
public class AdminAppEditActivity extends AppCompatActivity {

    public static final String EXTRA_APP_ID = "app_id";

    private static final String TYPE_ICON = "icon";
    private static final String TYPE_BANNER = "banner";
    private static final String TYPE_APK = "apk";

    private long appId;
    private AppDetailDto current;
    private String pickingType;

    private File newIconFile;
    private File newBannerFile;
    private File newApkFile;

    private ActivityResultLauncher<Intent> imagePicker;
    private ActivityResultLauncher<Intent> apkPicker;

    private TextView versionsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_app_edit);

        appId = getIntent().getLongExtra(EXTRA_APP_ID, -1);
        if (appId < 0) { finish(); return; }

        Toolbar toolbar = findViewById(R.id.admin_edit_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        versionsList = findViewById(R.id.admin_edit_versions);

        imagePicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> handlePickedImage(result.getResultCode(), result.getData()));

        apkPicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            newApkFile = copyUriToCache(uri, "apk", ".apk");
                            if (newApkFile != null) {
                                showReleaseNotesDialog();
                            }
                        }
                    }
                });

        // Кнопки.
        findViewById(R.id.admin_edit_pick_icon).setOnClickListener(v -> pickImage(TYPE_ICON));
        findViewById(R.id.admin_edit_pick_banner).setOnClickListener(v -> pickImage(TYPE_BANNER));
        findViewById(R.id.admin_edit_upload_apk).setOnClickListener(v -> pickApk());
        findViewById(R.id.admin_edit_save).setOnClickListener(v -> saveApp());
        findViewById(R.id.admin_edit_delete).setOnClickListener(v -> confirmDelete());

        loadApp();
    }

    private void loadApp() {
        findViewById(R.id.admin_edit_progress).setVisibility(View.VISIBLE);
        new Thread(() -> {
            AdminRepository repo = new AdminRepository();
            AdminRepository.Result<AppDetailDto> result = repo.getApp(appId);
            runOnUiThread(() -> {
                findViewById(R.id.admin_edit_progress).setVisibility(View.GONE);
                if (result.success && result.data != null) {
                    current = result.data;
                    bindApp(current);
                } else {
                    Toast.makeText(this, result.error, Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private void bindApp(AppDetailDto app) {
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(app.title);
        setText(R.id.admin_edit_package, app.packageName);
        setText(R.id.admin_edit_title, app.title);
        setText(R.id.admin_edit_developer, app.developer);
        setText(R.id.admin_edit_short_desc, app.shortDesc);
        setText(R.id.admin_edit_description, app.description);
        setText(R.id.admin_edit_category, app.category);
        if (app.rating > 0) setText(R.id.admin_edit_rating, String.valueOf(app.rating));
        ((MaterialCheckBox) findViewById(R.id.admin_edit_featured)).setChecked(app.featured);

        // Иконка/баннер.
        if (app.iconUrl != null) {
            Glide.with(this).load(UrlUtil.mediaUrl(app.iconUrl))
                    .placeholder(R.drawable.placeholder_app_icon)
                    .into((android.widget.ImageView) findViewById(R.id.admin_edit_icon_preview));
        }
        if (app.bannerUrl != null) {
            findViewById(R.id.admin_edit_banner_preview).setVisibility(View.VISIBLE);
            Glide.with(this).load(UrlUtil.mediaUrl(app.bannerUrl))
                    .into((android.widget.ImageView) findViewById(R.id.admin_edit_banner_preview));
        }

        // Версии.
        if (app.versions != null && !app.versions.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (VersionDto v : app.versions) {
                sb.append("v").append(v.versionName)
                        .append("  (").append(FileUtil.formatSize(v.fileSize)).append(")")
                        .append("  sha: ").append(v.sha256.substring(0, 8))
                        .append("…\n");
            }
            versionsList.setText(sb.toString().trim());
        } else {
            versionsList.setText(R.string.no_versions);
        }
    }

    // ===== Загрузка файлов =====

    private void pickImage(String type) {
        pickingType = type;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePicker.launch(Intent.createChooser(intent, getString(R.string.select_image)));
    }

    private void pickApk() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/vnd.android.package-archive");
        apkPicker.launch(intent);
    }

    private void handlePickedImage(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        File copied = copyUriToCache(uri, pickingType, ".img");
        if (copied == null) return;
        if (TYPE_ICON.equals(pickingType)) {
            newIconFile = copied;
            Toast.makeText(this, R.string.icon_selected, Toast.LENGTH_SHORT).show();
        } else {
            newBannerFile = copied;
            Toast.makeText(this, R.string.banner_selected, Toast.LENGTH_SHORT).show();
        }
    }

    private void showReleaseNotesDialog() {
        final EditText input = new EditText(this);
        input.setHint(R.string.release_notes_hint);
        input.setMinLines(3);
        LinearLayout container = new LinearLayout(this);
        container.setPadding(dp(20), dp(8), dp(20), dp(8));
        container.addView(input);

        new AlertDialog.Builder(this, R.style.DialogTheme)
                .setTitle(R.string.upload_apk)
                .setMessage(R.string.release_notes_prompt)
                .setView(container)
                .setPositiveButton(R.string.upload, (d, w) -> uploadApk(input.getText().toString()))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void uploadApk(String releaseNotes) {
        findViewById(R.id.admin_edit_progress).setVisibility(View.VISIBLE);
        new Thread(() -> {
            AdminRepository repo = new AdminRepository();
            AdminRepository.Result<VersionDto> result =
                    repo.addVersion(appId, newApkFile, releaseNotes);
            runOnUiThread(() -> {
                findViewById(R.id.admin_edit_progress).setVisibility(View.GONE);
                newApkFile = null;
                if (result.success) {
                    Toast.makeText(this, R.string.version_uploaded, Toast.LENGTH_SHORT).show();
                    loadApp();
                } else {
                    Toast.makeText(this, result.error, Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    // ===== Сохранение =====

    private void saveApp() {
        String pkg = text(R.id.admin_edit_package);
        String title = text(R.id.admin_edit_title);
        if (TextUtils.isEmpty(pkg) || TextUtils.isEmpty(title)) {
            Toast.makeText(this, R.string.err_required_fields, Toast.LENGTH_SHORT).show();
            return;
        }
        String json = buildJson();
        findViewById(R.id.admin_edit_progress).setVisibility(View.VISIBLE);
        new Thread(() -> {
            AdminRepository repo = new AdminRepository();
            AdminRepository.Result<com.arkashstudio.apphub.data.remote.dto.AppDto> result =
                    repo.updateApp(appId, json, newIconFile, newBannerFile);
            runOnUiThread(() -> {
                findViewById(R.id.admin_edit_progress).setVisibility(View.GONE);
                if (result.success) {
                    Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
                    newIconFile = null;
                    newBannerFile = null;
                    loadApp();
                } else {
                    Toast.makeText(this, result.error, Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private String buildJson() {
        String pkg = text(R.id.admin_edit_package);
        String title = text(R.id.admin_edit_title);
        String developer = text(R.id.admin_edit_developer);
        String shortDesc = text(R.id.admin_edit_short_desc);
        String description = text(R.id.admin_edit_description);
        String category = text(R.id.admin_edit_category);
        String ratingStr = text(R.id.admin_edit_rating);
        boolean featured = ((MaterialCheckBox) findViewById(R.id.admin_edit_featured)).isChecked();
        double rating = 0.0;
        try { if (!TextUtils.isEmpty(ratingStr)) rating = Double.parseDouble(ratingStr); }
        catch (Exception ignored) { }

        return "{" +
                "\"packageName\":\"" + esc(pkg) + "\"" +
                ",\"title\":\"" + esc(title) + "\"" +
                ",\"developer\":\"" + esc(developer) + "\"" +
                ",\"shortDesc\":\"" + esc(shortDesc) + "\"" +
                ",\"description\":\"" + esc(description) + "\"" +
                ",\"category\":\"" + esc(category) + "\"" +
                ",\"rating\":" + rating +
                ",\"featured\":" + featured +
                "}";
    }

    // ===== Удаление =====

    private void confirmDelete() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        LinearLayout container = new LinearLayout(this);
        container.setPadding(dp(20), dp(8), dp(20), dp(8));
        container.addView(input);

        new AlertDialog.Builder(this, R.style.DialogTheme)
                .setTitle(R.string.delete_app)
                .setMessage(R.string.delete_app_confirm)
                .setView(container)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    String pwd = input.getText().toString();
                    if (!pwd.isEmpty()) deleteApp(pwd);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteApp(String password) {
        findViewById(R.id.admin_edit_progress).setVisibility(View.VISIBLE);
        new Thread(() -> {
            AdminRepository repo = new AdminRepository();
            AdminRepository.Result<Void> result = repo.deleteApp(appId, password);
            runOnUiThread(() -> {
                findViewById(R.id.admin_edit_progress).setVisibility(View.GONE);
                if (result.success) {
                    Toast.makeText(this, R.string.app_deleted, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, result.error, Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    // ===== Helpers =====

    private String text(int viewId) {
        TextInputEditText et = findViewById(viewId);
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void setText(int viewId, String value) {
        TextInputEditText et = findViewById(viewId);
        if (et != null && value != null) et.setText(value);
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private File copyUriToCache(Uri uri, String prefix, String defaultExt) {
        try {
            String ext = defaultExt;
            List<String> segments = uri.getPathSegments();
            if (segments != null && !segments.isEmpty()) {
                String last = segments.get(segments.size() - 1);
                int dot = last.lastIndexOf('.');
                if (dot >= 0) ext = last.substring(dot);
            }
            File out = File.createTempFile("apphub_" + prefix + "_", ext, getCacheDir());
            try (InputStream is = getContentResolver().openInputStream(uri);
                 FileOutputStream fos = new FileOutputStream(out)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) > 0) fos.write(buf, 0, n);
            }
            return out;
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось загрузить файл: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            return null;
        }
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
