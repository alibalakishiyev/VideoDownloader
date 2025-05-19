// Downloader.java
package com.ali.videodownloader.utils;

import android.Manifest;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.ali.videodownloader.R;


public class Downloader extends AppCompatActivity {

    private Spinner platformSpinner;
    private EditText urlEditText;
    private Button downloadButton;
    private ProgressBar progressBar;
    private TextView statusTextView;
    private WebView webView;


    private String selectedPlatform = "TikTok";
    private static final int STORAGE_PERMISSION_CODE = 100;
    private static final int MANAGE_STORAGE_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloader);

        initializeViews();
        setupWebView();
        // Tətbiq açılan kimi clipboard-dakı linki EditText-ə yazırıq
        pasteLinkFromClipboard();




        if (checkStoragePermission()) {
            initializeDownloader();
        } else {
            requestStoragePermission();
        }



        // Also modify the downloadButton click listener to reset the first load flag:
        downloadButton.setOnClickListener(v -> {
            String videoUrl = urlEditText.getText().toString().trim();
            if (videoUrl.isEmpty()) {
                Toast.makeText(this, "Please enter video URL", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!checkStoragePermission()) {
                requestStoragePermission();
                return;
            }

            if (selectedPlatform.equals("YouTube") || selectedPlatform.equals("Instagram")) {
                // Reset WebView state
                webView.clearCache(true);
                webView.clearHistory();

                // For YouTube/Instagram, use savefrom.net in WebView
                String saveFromUrl = "https://en.savefrom.net/#url=" + videoUrl;
                webView.setVisibility(View.VISIBLE);
                webView.loadUrl(saveFromUrl);
            } else {
                // For other platforms, use normal downloader
                BaseVideoDownloader downloader = DownloaderFactory.createDownloader(
                        selectedPlatform,
                        progressBar,
                        statusTextView,
                        downloadButton,
                        this,
                        webView
                );
                downloader.execute(videoUrl);
            }
        });


    }


    private void initializeViews() {
        platformSpinner = findViewById(R.id.platformSpinner);
        urlEditText = findViewById(R.id.urlEditText);
        downloadButton = findViewById(R.id.downloadButton);
        progressBar = findViewById(R.id.progressBar);
        statusTextView = findViewById(R.id.statusTextView);
        webView = findViewById(R.id.webBrowser);

        // Add platform selection listener
        platformSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedPlatform = parent.getItemAtPosition(position).toString();
                if (!selectedPlatform.equals("YouTube")) {
                    webView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedPlatform = "TikTok";
            }
        });
    }




    // In your Downloader class, modify the setupWebView method:

    private void setupWebView() {
        webView = findViewById(R.id.webBrowser);

        webView.setWebViewClient(new WebViewClient() {
            private boolean isFirstLoad = true;
            private String originalUrl;

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(0);
                statusTextView.setText("Loading...");

                // Save the original URL on first load
                if (isFirstLoad) {
                    originalUrl = url;
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);

                // If this is the first load and we detect an error page or ad
                if (isFirstLoad && (url.contains("error") || url.contains("ad"))) {
                    // Reload the original URL
                    webView.postDelayed(() -> webView.loadUrl(originalUrl), 1000);
                    isFirstLoad = false;
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                // Handle error by reloading
                if (isFirstLoad && originalUrl != null) {
                    webView.postDelayed(() -> webView.loadUrl(originalUrl), 1000);
                    isFirstLoad = false;
                }
            }
        });

        // Add WebChromeClient to track loading progress
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                progressBar.setProgress(newProgress);
                statusTextView.setText("Loading: " + newProgress + "%");
            }
        });

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.setVisibility(View.GONE);

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimeType);
            request.addRequestHeader("User-Agent", userAgent);
            request.setDescription("Video yüklənir...");
            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                    URLUtil.guessFileName(url, contentDisposition, mimeType));

            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            long downloadId = dm.enqueue(request);
            monitorDownloadProgress(downloadId);

            Toast.makeText(getApplicationContext(), "Yükləmə başladı...", Toast.LENGTH_LONG).show();
        });
    }



    private void pasteLinkFromClipboard() {
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

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            int write = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            return write == PackageManager.PERMISSION_GRANTED &&
                    read == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", getPackageName())));
                startActivityForResult(intent, MANAGE_STORAGE_CODE);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, MANAGE_STORAGE_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                    STORAGE_PERMISSION_CODE
            );
        }
    }

    private void monitorDownloadProgress(long downloadId) {
        new Thread(() -> {
            boolean downloading = true;
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

            while (downloading) {
                Cursor cursor = manager.query(query);
                if (cursor != null && cursor.moveToFirst()) {
                    int bytesDownloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    int bytesTotal = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                    if (bytesTotal > 0) {
                        int progress = (int) ((bytesDownloaded * 100L) / bytesTotal);
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.VISIBLE);
                            progressBar.setProgress(progress);
                            statusTextView.setText("Yükləmə: " + progress + "%");
                        });
                    }

                    int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                        downloading = false;
                    }

                    cursor.close();
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            runOnUiThread(() -> {
                progressBar.setProgress(100);
                statusTextView.setText("Yükləmə tamamlandı");
            });
        }).start();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeDownloader();
            } else {
                showPermissionDeniedDialog();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MANAGE_STORAGE_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    initializeDownloader();
                } else {
                    showPermissionDeniedDialog();
                }
            }
        }
    }

    private void initializeDownloader() {
        Toast.makeText(this, "Depolama izni verildi", Toast.LENGTH_SHORT).show();
    }

    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("İzin Gerekli")
                .setMessage("Uygulamanın çalışması için depolama izni gereklidir. Lütfen ayarlardan izin verin.")
                .setPositiveButton("Ayarlara Git", (dialog, which) -> openAppSettings())
                .setNegativeButton("İptal", null)
                .show();
    }



    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }
}