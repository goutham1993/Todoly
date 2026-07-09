package com.expense.todoly;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import com.expense.todoly.data.AppDatabase;
import com.expense.todoly.data.AppRepository;
import com.expense.todoly.data.BackupManager;
import com.expense.todoly.data.Prefs;
import com.expense.todoly.data.entity.Category;
import com.expense.todoly.data.entity.Todo;
import com.expense.todoly.databinding.ActivitySettingsBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private enum ExportFormat {JSON, CSV}

    private ActivitySettingsBinding binding;
    private AppRepository repository;

    private ExportFormat pendingExportFormat;

    private ActivityResultLauncher<Uri> pickFolderLauncher;
    private ActivityResultLauncher<String[]> pickFileLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new AppRepository(getApplicationContext());

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        setupTheme();
        setupAbout();
        setupDataActions();
        registerLaunchers();
    }

    private void setupTheme() {
        int mode = Prefs.getThemeMode(this);
        int checkedId;
        if (mode == Prefs.THEME_LIGHT) {
            checkedId = R.id.themeLight;
        } else if (mode == Prefs.THEME_DARK) {
            checkedId = R.id.themeDark;
        } else {
            checkedId = R.id.themeSystem;
        }
        binding.themeToggleGroup.check(checkedId);

        binding.themeToggleGroup.addOnButtonCheckedListener((group, checkedButtonId, isChecked) -> {
            if (!isChecked) return;
            int newMode;
            if (checkedButtonId == R.id.themeLight) {
                newMode = Prefs.THEME_LIGHT;
            } else if (checkedButtonId == R.id.themeDark) {
                newMode = Prefs.THEME_DARK;
            } else {
                newMode = Prefs.THEME_SYSTEM;
            }
            if (newMode == Prefs.getThemeMode(this)) return;
            Prefs.setThemeMode(this, newMode);
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(Prefs.toNightMode(newMode));
        });
    }

    private void setupAbout() {
        String version = "1.0";
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        TextView versionView = binding.aboutVersion;
        versionView.setText(getString(R.string.about_version, version));
    }

    private void setupDataActions() {
        binding.rowExportJson.setOnClickListener(v -> startExport(ExportFormat.JSON));
        binding.rowExportCsv.setOnClickListener(v -> startExport(ExportFormat.CSV));
        binding.rowImport.setOnClickListener(v -> pickFileLauncher.launch(
                new String[]{"application/json", "application/zip", "application/octet-stream"}));
    }

    private void registerLaunchers() {
        pickFolderLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocumentTree(),
                treeUri -> {
                    if (treeUri == null || pendingExportFormat == null) return;
                    getContentResolver().takePersistableUriPermission(
                            treeUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                    | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    exportTo(treeUri, pendingExportFormat);
                    pendingExportFormat = null;
                });

        pickFileLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri == null) return;
                    confirmAndImport(uri);
                });
    }

    private void startExport(ExportFormat format) {
        pendingExportFormat = format;
        pickFolderLauncher.launch(null);
    }

    private void exportTo(Uri treeUri, ExportFormat format) {
        repository.exportAll(new AppRepository.ExportCallback() {
            @Override
            public void onResult(List<Category> categories, List<Todo> todos) {
                AppDatabase.IO_EXECUTOR.execute(() -> writeExport(treeUri, format, categories, todos));
            }

            @Override
            public void onError(Exception e) {
                toast(getString(R.string.export_failed, String.valueOf(e.getMessage())));
            }
        });
    }

    private void writeExport(Uri treeUri, ExportFormat format,
                             List<Category> categories, List<Todo> todos) {
        try {
            DocumentFile dir = DocumentFile.fromTreeUri(this, treeUri);
            if (dir == null || !dir.canWrite()) {
                runOnUiThread(() -> toast(getString(R.string.export_failed, "cannot write to folder")));
                return;
            }
            String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
            String mime = format == ExportFormat.JSON ? "application/json" : "application/zip";
            String name = "todoly-backup-" + stamp + (format == ExportFormat.JSON ? ".json" : ".zip");

            DocumentFile file = dir.createFile(mime, name);
            if (file == null) {
                runOnUiThread(() -> toast(getString(R.string.export_failed, "could not create file")));
                return;
            }
            try (OutputStream out = getContentResolver().openOutputStream(file.getUri())) {
                if (out == null) throw new Exception("null output stream");
                if (format == ExportFormat.JSON) {
                    BackupManager.writeJson(out, categories, todos);
                } else {
                    BackupManager.writeCsvZip(out, categories, todos);
                }
            }
            runOnUiThread(() -> toast(getString(R.string.export_success)));
        } catch (Exception e) {
            runOnUiThread(() -> toast(getString(R.string.export_failed, String.valueOf(e.getMessage()))));
        }
    }

    private void confirmAndImport(Uri uri) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.import_confirm_title)
                .setMessage(R.string.import_confirm_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.import_confirm_action, (d, w) -> runImport(uri))
                .show();
    }

    private void runImport(Uri uri) {
        AppDatabase.IO_EXECUTOR.execute(() -> {
            try {
                byte[] bytes = readAllBytes(uri);
                BackupManager.BackupData data;
                if (isZip(bytes)) {
                    data = BackupManager.readCsvZip(new ByteArrayInputStream(bytes));
                } else {
                    data = BackupManager.readJson(new ByteArrayInputStream(bytes));
                }
                repository.importReplace(data.categories, data.todos, new AppRepository.ImportCallback() {
                    @Override
                    public void onResult(int categoryCount, int todoCount) {
                        toast(getString(R.string.import_success, categoryCount, todoCount));
                    }

                    @Override
                    public void onError(Exception e) {
                        toast(getString(R.string.import_failed, String.valueOf(e.getMessage())));
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> toast(getString(R.string.import_failed, String.valueOf(e.getMessage()))));
            }
        });
    }

    private boolean isZip(byte[] bytes) {
        return bytes.length >= 2 && bytes[0] == 'P' && bytes[1] == 'K';
    }

    private byte[] readAllBytes(Uri uri) throws Exception {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new Exception("null input stream");
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return buffer.toByteArray();
        }
    }

    private void toast(String message) {
        if (isFinishing() || isDestroyed()) return;
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
