package com.arkashstudio.apphub.ui.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arkashstudio.apphub.R;
import com.arkashstudio.apphub.data.remote.dto.AppDto;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

/**
 * Админ-панель: список всех приложений (включая неопубликованные).
 * FAB "+" открывает экран создания нового приложения.
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
            startActivity(new android.content.Intent(this, AdminAppNewActivity.class));
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
        android.content.Intent i = new android.content.Intent(this, AdminAppEditActivity.class);
        i.putExtra(AdminAppEditActivity.EXTRA_APP_ID, appId);
        startActivity(i);
    }

    @Override
    protected void onResume() {
        super.onResume();
        View empty = findViewById(R.id.admin_list_empty);
        loadApps(empty);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
