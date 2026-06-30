package com.arkashstudio.apphub.ui.admin;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.arkashstudio.apphub.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Админка: создание нового приложения.
 *
 * <p>Поля: packageName (обязательный), title (обязательный), developer, shortDesc,
 * description, category, rating, featured. Иконка/баннер — опциональны через
 * image-picker.
 */
public class AdminAppNewActivity extends AppCompatActivity {

    private static final String TYPE_ICON = "icon";
    private static final String TYPE_BANNER = "banner";

    private String pickingType;

    private File iconFile;
    private File bannerFile;

    private ActivityResultLauncher<Intent> imagePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_app_new);

        Toolbar toolbar = findViewById(R.id.admin_new_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.admin_new_app);
        }

        // Регистрация image-picker.
        imagePicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            File copied = copyUriToCache(uri, pickingType);
                            if (TYPE_ICON.equals(pickingType)) {
                                iconFile = copied;
                                Toast.makeText(this, R.string.icon_selected, Toast.LENGTH_SHORT).show();
                            } else {
                                bannerFile = copied;
                                Toast.makeText(this, R.string.banner_selected, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });

        // Кнопки выбора иконки/баннера.
        findViewById(R.id.admin_new_pick_icon).setOnClickListener(v -> pickImage(TYPE_ICON));
        findViewById(R.id.admin_new_pick_banner).setOnClickListener(v -> pickImage(TYPE_BANNER));

        MaterialButton save = findViewById(R.id.admin_new_save);
        save.setOnClickListener(v -> saveApp());
    }

    private void pickImage(String type) {
        pickingType = type;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePicker.launch(Intent.createChooser(intent, getString(R.string.select_image)));
    }

    private void saveApp() {
        String pkg = text(R.id.admin_new_package);
        String title = text(R.id.admin_new_title);

        if (TextUtils.isEmpty(pkg) || TextUtils.isEmpty(title)) {
            Toast.makeText(this, R.string.err_required_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        // Сборка JSON (без медиа — они передаются отдельными частями).
        String json = buildJson(pkg, title);

        final View progress = findViewById(R.id.admin_new_progress);
        progress.setVisibility(View.VISIBLE);

        new Thread(() -> {
            AdminRepository repo = new AdminRepository();
            AdminRepository.Result<com.arkashstudio.apphub.data.remote.dto.AppDto> result =
                    repo.createApp(json, iconFile, bannerFile);
            runOnUiThread(() -> {
                progress.setVisibility(View.GONE);
                if (result.success) {
                    Toast.makeText(this, R.string.app_created, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, result.error, Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private String buildJson(String pkg, String title) {
        String developer = text(R.id.admin_new_developer);
        String shortDesc = text(R.id.admin_new_short_desc);
        String description = text(R.id.admin_new_description);
        String category = text(R.id.admin_new_category);
        String ratingStr = text(R.id.admin_new_rating);
        boolean featured = ((com.google.android.material.checkbox.MaterialCheckBox)
                findViewById(R.id.admin_new_featured)).isChecked();

        double rating = 0.0;
        try { if (!TextUtils.isEmpty(ratingStr)) rating = Double.parseDouble(ratingStr); }
        catch (Exception ignored) { }

        StringBuilder sb = new StringBuilder("{");
        sb.append("\"packageName\":\"").append(esc(pkg)).append("\"");
        sb.append(",\"title\":\"").append(esc(title)).append("\"");
        sb.append(",\"developer\":\"").append(esc(developer)).append("\"");
        sb.append(",\"shortDesc\":\"").append(esc(shortDesc)).append("\"");
        sb.append(",\"description\":\"").append(esc(description)).append("\"");
        sb.append(",\"category\":\"").append(esc(category)).append("\"");
        sb.append(",\"rating\":").append(rating);
        sb.append(",\"featured\":").append(featured);
        sb.append("}");
        return sb.toString();
    }

    private String text(int viewId) {
        TextInputEditText et = findViewById(viewId);
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private File copyUriToCache(Uri uri, String suffix) {
        try {
            String suffixExt = ".img";
            java.util.List<String> segments = uri.getPathSegments();
            if (segments != null && !segments.isEmpty()) {
                String last = segments.get(segments.size() - 1);
                int dot = last.lastIndexOf('.');
                if (dot >= 0) suffixExt = last.substring(dot);
            }
            File out = File.createTempFile("apphub_" + suffix + "_", suffixExt, getCacheDir());
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

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
