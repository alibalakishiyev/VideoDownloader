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
import android.webkit.WebSettings;
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
                        if (result.startsWith("Error")) {
                            statusTextView.setText(result);
                            Toast.makeText(this, result, Toast.LENGTH_LONG).show();
                        } else {
                            statusTextView.setText("Download completed!");
                            Toast.makeText(this, "Video downloaded successfully", Toast.LENGTH_SHORT).show();
                        }
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

        // Enhanced WebView settings
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportMultipleWindows(false);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
        webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 10) Mobile");
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient() {
            private boolean isDownloadStarted = false;

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.contains("savefrom.net") || url.contains("youtube.com") || url.contains("instagram.com")) {
                    view.loadUrl(url);
                    return true;
                }
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                isDownloadStarted = false;
                progressBar.setVisibility(View.VISIBLE);
                statusTextView.setText("Preparing download...");
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!isDownloadStarted) {
                    injectFinalDownloadScript(view);
                }
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
                if (isValidVideoUrl(url) && !isDownloadStarted) {
                    isDownloadStarted = true;
                    startVideoDownload(url, webView.getSettings().getUserAgentString());
                }
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            if (isValidVideoUrl(url)) {
                startVideoDownload(url, userAgent);
            }
        });
    }


    private void injectFinalDownloadScript(WebView view) {
        String jsCode = "javascript:(function() {" +
                "try {" +
                // Check if download button exists
                "   var downloadBtn = document.querySelector('a[href*=\"download\"], button[onclick*=\"download\"]');" +
                "   if (downloadBtn) {" +
                // If download button found, click it
                "       downloadBtn.click();" +
                "   } else {" +
                // Otherwise look for direct video links
                "       var videoLinks = document.querySelectorAll('a[href*=\"videoplayback\"], a[href*=\"video_redirect\"]');" +
                "       if (videoLinks.length > 0) {" +
                "           window.location.href = videoLinks[0].href;" +
                "       }" +
                "   }" +
                "} catch(e) { console.error('Final download error:', e); }" +
                "})();";

        view.evaluateJavascript(jsCode, null);
    }

    private void startVideoDownload(String url, String userAgent) {
        try {
            // Create unique filename with quality indicator
            String fileName = "video_720p_" + System.currentTimeMillis() + ".mp4";

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType("video/mp4");
            request.addRequestHeader("User-Agent", userAgent);
            request.setTitle(fileName);
            request.setDescription("Downloading HD video...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);

            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (dm != null) {
                long downloadId = dm.enqueue(request);
                monitorDownload(downloadId);
                runOnUiThread(() -> {
                    Toast.makeText(Downloader.this, "Downloading 720p video...", Toast.LENGTH_LONG).show();
                    progressBar.setVisibility(View.VISIBLE);
                });
            }
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(Downloader.this,
                    "Download error: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    @SuppressWarnings("Range")
    private void monitorDownload(long downloadId) {
        new Thread(() -> {
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

            boolean downloading = true;
            while (downloading) {
                try (Cursor cursor = manager.query(query)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                        int bytesDownloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        int bytesTotal = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                        // Update progress
                        int progress = bytesTotal > 0 ? (int) ((bytesDownloaded * 100L) / bytesTotal) : 0;
                        runOnUiThread(() -> {
                            progressBar.setProgress(progress);
                            statusTextView.setText("Downloading: " + progress + "%");
                        });

                        // Check if download completed
                        if (status == DownloadManager.STATUS_SUCCESSFUL ||
                                status == DownloadManager.STATUS_FAILED) {
                            downloading = false;
                        }
                    }
                }
                try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
            }
        }).start();
    }

    private boolean isValidVideoUrl(String url) {
        if (url == null || url.isEmpty()) return false;

        String lowerUrl = url.toLowerCase();
        return lowerUrl.matches(".*\\.(mp4|webm|mkv|mov|avi|flv|3gp|m4v)$") ||
                lowerUrl.contains("googlevideo.com") ||
                lowerUrl.contains("videoplayback") ||
                lowerUrl.contains("video_redirect") ||
                lowerUrl.contains("cdninstagram.com") ||
                lowerUrl.contains("video") ||
                lowerUrl.contains("720p") ||
                lowerUrl.contains("high.quality");
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