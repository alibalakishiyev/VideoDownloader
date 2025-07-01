package com.ali.videodownloader.utils;

import static com.ali.videodownloader.config.PermissionDenied.initializeDownloader;

import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ali.videodownloader.MainActivity;
import com.ali.videodownloader.R;
import com.ali.videodownloader.VideoSites.InstagramDownloader;
import com.ali.videodownloader.VideoSites.PinterestDownloader;
import com.ali.videodownloader.VideoSites.TikTokDownloader;
import com.ali.videodownloader.VideoSites.YouTubeDownloader;
import com.ali.videodownloader.config.PermissionDenied;
import com.ali.videodownloader.config.StatusAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    private StatusAdapter adapter;  // adapter-i global elan et
    private AdView mAdView1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloader);

        initializeViews();

        selectedPlatform = getIntent().getStringExtra("siteName");
        if (selectedPlatform == null) {
            Toast.makeText(this, "Platform not specified!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupPlatformSpecificUI();
        pasteLinkFromClipboard();

        if (PermissionDenied.checkStoragePermission(this)) {
            initializeDownloader(this);
        } else {
            PermissionDenied.requestStoragePermission(this);
        }



        setupDownloadButton();

        mAdView1 = findViewById(R.id.adView1);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView1.loadAd(adRequest);

        getOnBackPressedDispatcher().addCallback(this, callback);
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
        containerLayout.setVisibility(View.GONE);
    }

    private void setupPlatformSpecificUI() {
        if (selectedPlatform.equals("WhatsApp Status")) {
            setupWhatsAppUI();
        } else if(selectedPlatform.equals("TikTok")){
            setupOtherAppUI();
        }
    }

    private void setupOtherAppUI(){
        statusRecyclerView.setVisibility(View.VISIBLE);
        webView.setVisibility(View.GONE);
        containerLayout.setVisibility(View.GONE);

    }

    private void setupWhatsAppUI() {
        // Hide URL input, show RecyclerView for WhatsApp Status
        urlEditText.setVisibility(View.GONE);
        statusRecyclerView.setVisibility(View.VISIBLE);
        webView.setVisibility(View.GONE);
        containerLayout.setVisibility(View.VISIBLE);

        showWhatsAppStatuses();
    }

    private void setupWebViewUI() {
        urlEditText.setVisibility(View.VISIBLE);
        statusRecyclerView.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        containerLayout.setVisibility(View.VISIBLE);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);

        // Configure WebViewClient to handle popups and downloads
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                // Intercept SaveFrom.net download links
                if (url.contains("savefrom.net/download") || url.contains("videodownload")) {
                    startDownload(url);
                    return true;
                }
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
                statusTextView.setText("Loading...");
                downloadButton.setEnabled(false);
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                statusTextView.setText("Page loaded - select quality and click download");
                downloadButton.setEnabled(true);
                super.onPageFinished(view, url);

                // Inject JavaScript to help with SaveFrom.net
                injectSaveFromHelper();
            }
        });

        // Enable popup windows
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog,
                                          boolean isUserGesture, Message resultMsg) {
                WebView newWebView = new WebView(Downloader.this);
                newWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        if (url.contains("savefrom.net/download")) {
                            startDownload(url);
                            return true;
                        }
                        return false;
                    }
                });

                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newWebView);
                resultMsg.sendToTarget();
                return true;
            }
        });

        // Enable downloads from WebView
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            startDownload(url);
        });
    }


    private void injectSaveFromHelper() {
        String jsCode = "javascript:(function() {" +
                "var downloadBtns = document.querySelectorAll('a[href*=\"download\"], button[onclick*=\"download\"]');" +
                "downloadBtns.forEach(function(btn) {" +
                "   btn.setAttribute('onclick', 'window.open(this.href); return false;');" +
                "});" +
                "})()";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(jsCode, null);
        } else {
            webView.loadUrl(jsCode);
        }
    }

    // Improved download method
    private void startDownload(String downloadUrl) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
            request.setTitle("Video Download");
            request.setDescription("Downloading from " + selectedPlatform);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);

            // Generate proper filename based on platform
            String fileName = selectedPlatform.toLowerCase() + "_" + System.currentTimeMillis();
            if (downloadUrl.contains(".mp4")) {
                fileName += ".mp4";
            } else if (downloadUrl.contains(".webm")) {
                fileName += ".webm";
            } else {
                fileName += ".mp4"; // default extension
            }

            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    fileName
            );

            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show();
                    statusTextView.setText("Download in progress...");
                    downloadButton.setEnabled(true);
                });
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Download service unavailable", Toast.LENGTH_SHORT).show();
                    statusTextView.setText("Download failed");
                    downloadButton.setEnabled(true);
                });
            }
        } catch (Exception e) {
            runOnUiThread(() -> {
                Toast.makeText(this, "Download error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                statusTextView.setText("Error: " + e.getMessage());
                downloadButton.setEnabled(true);
            });
        }
    }

    private void showWhatsAppStatuses() {
        // Check both old and new WhatsApp status directories
        File statusDir = new File(Environment.getExternalStorageDirectory() + "/WhatsApp/Media/.Statuses");
        File newStatusDir = new File(Environment.getExternalStorageDirectory() + "/Android/media/com.whatsapp/WhatsApp/Media/.Statuses");

        File targetDir = statusDir.exists() ? statusDir : newStatusDir;

        if (targetDir.exists() && targetDir.isDirectory()) {
            File[] statusFiles = targetDir.listFiles();
            if (statusFiles != null && statusFiles.length > 0) {
                List<File> mediaList = new ArrayList<>();
                for (File file : statusFiles) {
                    if (file.getName().endsWith(".jpg") || file.getName().endsWith(".mp4")) {
                        mediaList.add(file);
                    }
                }


                if (!mediaList.isEmpty()) {
                    statusRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
                    adapter = new StatusAdapter(this, mediaList);
                    statusRecyclerView.setAdapter(adapter);
                    return;
                }
            }
        }
        Toast.makeText(this, "No WhatsApp statuses found", Toast.LENGTH_SHORT).show();
    }




    private void setupDownloadButton() {
        downloadButton.setOnClickListener(v -> {
            String videoUrl = urlEditText.getText().toString().trim();

            if ("WhatsApp Status".equals(selectedPlatform)) {
                // WhatsApp Status seçilibsə, seçilmiş statusları yüklə
                if (adapter == null) {
                    Toast.makeText(this, "Status adapter hazır deyil", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<File> selectedFiles = adapter.getSelectedFiles();

                if (selectedFiles.isEmpty()) {
                    Toast.makeText(this, "Heç bir status seçilməyib", Toast.LENGTH_SHORT).show();
                    return;
                }

                for (File file : selectedFiles) {
                    Toast.makeText(this, "Fayil Secildi", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            if (videoUrl.isEmpty()) {
                Toast.makeText(this, "Please enter video URL", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setProgress(0);
            progressBar.setVisibility(View.VISIBLE);
            statusTextView.setText("Preparing download...");
            downloadButton.setEnabled(false);

            // Handle TikTok downloads
            if ("TikTok".equals(selectedPlatform)) {
                new Thread(() -> {
                    try {
                        TikTokDownloader tikTokDownloader = new TikTokDownloader(
                                progressBar,
                                statusTextView,
                                downloadButton,
                                Downloader.this,
                                webView
                        );

                        // Extract video URL from TikTok link
                        String downloadUrl = tikTokDownloader.getVideoUrl(videoUrl);

                        runOnUiThread(() -> {
                            if (downloadUrl != null && !downloadUrl.isEmpty()) {
                                startDownload(downloadUrl);
                                progressBar.setVisibility(View.GONE);
                            } else {
                                statusTextView.setText("Failed to extract TikTok video URL");
                                downloadButton.setEnabled(true);
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            statusTextView.setText("Error: " + e.getMessage());
                            Toast.makeText(Downloader.this, "Error processing TikTok video", Toast.LENGTH_LONG).show();
                            downloadButton.setEnabled(true);
                        });
                    }
                }).start();
                return;
            }

            // Handle Pinterest downloads
            if ("Pinterest".equals(selectedPlatform)) {
                new Thread(() -> {
                    try {
                        PinterestDownloader pinterestDownloader = new PinterestDownloader(
                                progressBar,
                                statusTextView,
                                downloadButton,
                                Downloader.this,
                                webView
                        );

                        // Extract video URL from Pinterest link
                        String downloadUrl = pinterestDownloader.getVideoUrl(videoUrl);

                        runOnUiThread(() -> {
                            if (downloadUrl != null && !downloadUrl.isEmpty()) {
                                startDownload(downloadUrl);
                                progressBar.setVisibility(View.GONE);

                            } else {
                                statusTextView.setText("Failed to extract Pinterest video URL");
                                downloadButton.setEnabled(true);
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            statusTextView.setText("Error: " + e.getMessage());
                            Toast.makeText(Downloader.this, "Error processing Pinterest video", Toast.LENGTH_LONG).show();
                            downloadButton.setEnabled(true);
                        });
                    }
                }).start();
                return;
            }


            // Platform yoxlaması
            if ("YouTube".equalsIgnoreCase(selectedPlatform) ||
                    "Instagram".equalsIgnoreCase(selectedPlatform) ||
                    "Facebook".equalsIgnoreCase(selectedPlatform)) {

                setupWebViewUI();

                YouTubeDownloader youTubeDownloader = new YouTubeDownloader(
                        progressBar,
                        statusTextView,
                        downloadButton,
                        this,
                        webView
                );
                InstagramDownloader instagramDownloader = new InstagramDownloader(
                        progressBar,
                        statusTextView,
                        downloadButton,
                        this,
                        webView
                );

                try {
                    youTubeDownloader.getVideoUrl(videoUrl);
                    instagramDownloader.getVideoUrl(videoUrl);
                } catch (Exception e) {
                    statusTextView.setText("Error: " + e.getMessage());
                    Toast.makeText(this, "Error processing video: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    downloadButton.setEnabled(true);
                }

            }

        });
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

    OnBackPressedCallback callback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(Downloader.this);
            materialAlertDialogBuilder.setTitle(R.string.app_name);
            materialAlertDialogBuilder.setMessage("Are you sure want to exit the app?");
            materialAlertDialogBuilder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int i) {
                    dialog.dismiss();
                }
            });
            materialAlertDialogBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int i) {
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    finish();
                }
            });
            materialAlertDialogBuilder.show();
        }
    };
}
