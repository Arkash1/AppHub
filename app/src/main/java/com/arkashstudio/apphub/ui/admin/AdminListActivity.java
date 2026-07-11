package com.arkashstudio.apphub.ui.admin;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arkashstudio.apphub.R;
import com.arkashstudio.apphub.data.local.AdminCredentialsStore;
import com.arkashstudio.apphub.data.remote.ApiClient;
import com.arkashstudio.apphub.data.remote.dto.AppDto;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

/**
 * Админ-панель: список всех приложений (включая неопубликованные).
 * FAB "+" открывает экран создания нового приложения.
 * В меню тулбара — смена пароля и выход из режима админа.
 */
public class AdminListActivity extends AppCompatActivity {

    private AdminAppAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_list);

        Toolbar toolbar = findViewById(R.id.admin_list_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.admin_panel_title);
        }

        RecyclerView list = findViewById(R.id.admin_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminAppAdapter(this, this::openApp);
        list.setAdapter(adapter);

        View empty = findViewById(R.id.admin_list_empty);

        FloatingActionButton fab = findViewById(R.id.admin_fab_add);
        fab.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminAppNewActivity.class));
        });

        loadApps(empty);
    }

    private void loadApps(View empty) {
        new Thread(() -> {
            AdminRepository repo = new AdminRepository();
            AdminRepository.Result<List<AppDto>> result = repo.listApps();
            runOnUiThread(() -> {
                if (result.success && result.data != null) {
                    adapter.submitList(result.data);
                    empty.setVisibility(result.data.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    Toast.makeText(this, result.error, Toast.LENGTH_LONG).show();
                    empty.setVisibility(View.VISIBLE);
                }
            });
        }).start();
    }

    private void openApp(long appId) {
        Intent i = new Intent(this, AdminAppEditActivity.class);
        i.putExtra(AdminAppEditActivity.EXTRA_APP_ID, appId);
        startActivity(i);
    }

    // ===== Меню тулбара =====

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_admin, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        if (id == R.id.action_change_password) {
            showChangePasswordDialog();
            return true;
        }
        if (id == R.id.action_logout) {
            showLogoutConfirm();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showChangePasswordDialog() {
        final EditText oldInput = new EditText(this);
        oldInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        oldInput.setHint(R.string.old_password);

        final EditText newInput = new EditText(this);
        newInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        newInput.setHint(R.string.new_password);

        final EditText confirmInput = new EditText(this);
        confirmInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        confirmInput.setHint(R.string.confirm_password);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(20), dp(8), dp(20), dp(8));
        container.addView(oldInput);
        container.addView(newInput);
        container.addView(confirmInput);

        final AlertDialog dialog = new AlertDialog.Builder(this, R.style.DialogTheme)
                .setTitle(R.string.change_password_title)
                .setView(container)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String oldPwd = oldInput.getText().toString();
                String newPwd = newInput.getText().toString();
                String confirm = confirmInput.getText().toString();

                if (oldPwd.isEmpty() || newPwd.isEmpty()) {
                    Toast.makeText(this, R.string.err_required_fields, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (newPwd.length() < 4) {
                    Toast.makeText(this, R.string.err_password_short, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!newPwd.equals(confirm)) {
                    Toast.makeText(this, R.string.err_password_mismatch, Toast.LENGTH_SHORT).show();
                    return;
                }

                changePassword(oldPwd, newPwd);
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private void changePassword(String oldPwd, String newPwd) {
        new Thread(() -> {
            AdminRepository repo = new AdminRepository();
            AdminRepository.Result<Void> result = repo.changePassword(oldPwd, newPwd);
            runOnUiThread(() -> {
                if (result.success) {
                    // Обновляем сохранённый пароль в EncryptedSharedPreferences.
                    new AdminCredentialsStore(this).savePassword(newPwd);
                    // Пересоздаём ApiClient, чтобы перехватчик перечитал пароль.
                    ApiClient.rebuildSameUrl(this);
                    Toast.makeText(this, R.string.password_changed, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, result.error, Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private void showLogoutConfirm() {
        new AlertDialog.Builder(this, R.style.DialogTheme)
                .setTitle(R.string.admin_logout)
                .setMessage(R.string.admin_logout_confirm)
                .setPositiveButton(R.string.yes, (d, w) -> {
                    new AdminCredentialsStore(this).clear();
                    ApiClient.rebuildSameUrl(this);
                    Toast.makeText(this, R.string.logged_out, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        View empty = findViewById(R.id.admin_list_empty);
        loadApps(empty);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
