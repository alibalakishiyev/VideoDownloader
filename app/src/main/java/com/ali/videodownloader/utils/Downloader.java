package com.ali.videodownloader.utils;

import static com.ali.videodownloader.config.PermissionDenied.checkStoragePermission;
import static com.ali.videodownloader.config.PermissionDenied.initializeDownloader;
import static com.ali.videodownloader.config.PermissionDenied.requestStoragePermission;

import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ali.videodownloader.R;
import com.ali.videodownloader.VideoSites.TikTokDownloader;
import com.ali.videodownloader.config.PermissionDenied;
import com.ali.videodownloader.config.StatusAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Downloader extends AppCompatActivity {
    private EditText urlEditText;
    private Button downloadButton;
    private ProgressBar progressBar;
    private TextView statusTextView;
    private WebView webView;
    private RecyclerView statusRecyclerView;
    private FrameLayout containerLayout;
    private String selectedPlatform;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloader);

        // Get selected platform from intent
        selectedPlatform = getIntent().getStringExtra("siteName");

        initializeViews();
        setupPlatformSpecificUI();
        pasteLinkFromClipboard();

        if (PermissionDenied.checkStoragePermission(this)) {
            initializeDownloader(this);
        } else {
            PermissionDenied.requestStoragePermission(this);
        }

        selectedPlatform = getIntent().getStringExtra("siteName");
        if (selectedPlatform == null) {
            Toast.makeText(this, "Platform not specified!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }


        setupDownloadButton();
    }

    private void initializeViews() {
        urlEditText = findViewById(R.id.urlEditText);
        downloadButton = findViewById(R.id.downloadButton);
        progressBar = findViewById(R.id.progressBar);
        statusTextView = findViewById(R.id.statusTextView);
        webView = findViewById(R.id.webBrowser);
        statusRecyclerView = findViewById(R.id.statusRecyclerView);
        containerLayout = findViewById(R.id.containerLayout);

        // Initially hide both containers
        webView.setVisibility(View.GONE);
        statusRecyclerView.setVisibility(View.GONE);
    }

    private void setupPlatformSpecificUI() {
        if (selectedPlatform.equals("WhatsApp Status")) {
            setupWhatsAppUI();
        } else {
            setupWebViewUI();
        }
    }

    private void setupWhatsAppUI() {
        // Hide URL input for WhatsApp Status
        urlEditText.setVisibility(View.GONE);
        statusRecyclerView.setVisibility(View.VISIBLE);
        containerLayout.setVisibility(View.VISIBLE);

        showWhatsAppStatuses();
    }

    private void setupWebViewUI() {
        // Show URL input for web platforms
        urlEditText.setVisibility(View.VISIBLE);
        webView.setVisibility(View.VISIBLE);
        containerLayout.setVisibility(View.VISIBLE);

        // Configure WebView
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
    }

    private void showWhatsAppStatuses() {
        File statusDir = new File(Environment.getExternalStorageDirectory() + "/WhatsApp/Media/.Statuses");
        if (statusDir.exists() && statusDir.isDirectory()) {
            File[] statusFiles = statusDir.listFiles();
            if (statusFiles != null && statusFiles.length > 0) {
                List<File> mediaList = new ArrayList<>();
                for (File file : statusFiles) {
                    if (file.getName().endsWith(".jpg") || file.getName().endsWith(".mp4")) {
                        mediaList.add(file);
                    }
                }

                statusRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                StatusAdapter adapter = new StatusAdapter(this, mediaList);
                statusRecyclerView.setAdapter(adapter);
            } else {
                Toast.makeText(this, "No statuses found", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "WhatsApp status folder not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupDownloadButton() {
        downloadButton.setOnClickListener(v -> {
            if ("WhatsApp Status".equals(selectedPlatform)) {
                setupWhatsAppUI();
            } else if (selectedPlatform != null) {
                setupWebViewUI();
            } else {
                Toast.makeText(this, "No platform selected", Toast.LENGTH_SHORT).show();
                finish(); // fəaliyyətin davam etməsinin qarşısını al
            }


            String videoUrl = urlEditText.getText().toString().trim();
            if (videoUrl.isEmpty()) {
                Toast.makeText(this, "Please enter video URL", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            statusTextView.setText("Starting download...");
            downloadButton.setEnabled(false);

            new Thread(() -> {
                try {
                    BaseVideoDownloader downloader = DownloaderFactory.createDownloader(
                            selectedPlatform,
                            progressBar,
                            statusTextView,
                            downloadButton,
                            this,
                            webView
                    );

                    String result = downloader.execute(videoUrl).get();

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        downloadButton.setEnabled(true);
                        handleDownloadResult(result);
                    });

                } catch (Exception e) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        downloadButton.setEnabled(true);
                        statusTextView.setText("Error: " + e.getMessage());
                        Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
        });
    }

    private void handleDownloadResult(String result) {
        if (result != null && result.startsWith("Error")) {
            statusTextView.setText(result);
            Toast.makeText(this, result, Toast.LENGTH_LONG).show();
        } else if (result != null) {
            statusTextView.setText(result);
            Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
        }
    }

    private void pasteLinkFromClipboard() {
        if (selectedPlatform.equals("WhatsApp Status")) return;

        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            ClipData clipData = clipboard.getPrimaryClip();
            if (clipData != null && clipData.getItemCount() > 0) {
                CharSequence pastedText = clipData.getItemAt(0).getText();
                if (pastedText != null && pastedText.toString().startsWith("http")) {
                    urlEditText.setText(pastedText.toString());
                }
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionDenied.getStoragePermissionCode()) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeDownloader(this);
            } else {
                PermissionDenied.showPermissionDeniedDialog(this);
            }
        }
    }
}
