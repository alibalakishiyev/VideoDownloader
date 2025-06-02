package com.ali.videodownloader.VideoSites;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ali.videodownloader.utils.BaseVideoDownloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WhatsAppStatusDownloader extends BaseVideoDownloader {

    private static final String STATUS_DIRECTORY = "/Android/media/com.whatsapp/WhatsApp/Media/.Statuses";
    private static final String SAVE_DIRECTORY = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/WhatsApp Statuses";
    private File selectedStatusFile;

    public WhatsAppStatusDownloader(ProgressBar progressBar, TextView statusTextView,
                                    View downloadButton, Context context, WebView webView) {
        super(progressBar, statusTextView, downloadButton, context, webView);
    }

    @Override
    protected String getVideoUrl(String url) throws Exception {
        // WhatsApp statusları lokal fayllardır, URL emalına ehtiyac yoxdur
        return null;
    }

    @Override
    protected String doInBackground(String... params) {
        if (selectedStatusFile == null) {
            return "No status selected";
        }

        return saveStatusToGallery(selectedStatusFile);
    }

    public void setSelectedStatusFile(File statusFile) {
        this.selectedStatusFile = statusFile;
    }

    private String saveStatusToGallery(File statusFile) {
        try {
            File saveDir = new File(SAVE_DIRECTORY);
            if (!saveDir.exists() && !saveDir.mkdirs()) {
                return "Error creating save directory";
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(new Date(statusFile.lastModified()));

            String destFileName = "WA_STATUS_" + timestamp + "_" + statusFile.getName();
            File destFile = new File(saveDir, destFileName);

            if (copyFile(statusFile, destFile)) {
                // Mediascan işlədərək qalereyada görünməsini təmin et
                scanMediaFile(destFile);
                return "Status saved to gallery";
            } else {
                return "Failed to save status";
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private void scanMediaFile(File file) {
        MediaScannerConnection.scanFile(
                context,
                new String[]{file.getAbsolutePath()},
                new String[]{getMimeType(file)},
                (path, uri) -> {
                    // Fayl skan edildikdən sonra
                    ((Activity) context).runOnUiThread(() -> {
                        Toast.makeText(context, "Status saved to gallery", Toast.LENGTH_SHORT).show();

                        // Qalereyanı yenilə
                        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                Uri.fromFile(file)));
                    });

                });
    }

    private String getMimeType(File file) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(file.getAbsolutePath());
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
    }

    private boolean isVideoFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".3gp");
    }

    private boolean copyFile(File source, File dest) throws IOException {
        try (FileChannel sourceChannel = new FileInputStream(source).getChannel();
             FileChannel destChannel = new FileOutputStream(dest).getChannel()) {
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
            return true;
        }
    }

    // Status fayllarını tapmaq üçün metod
    public File[] getStatusFiles() {
        File statusDir = new File(Environment.getExternalStorageDirectory() + STATUS_DIRECTORY);
        if (!statusDir.exists()) {
            // Yeni WhatsApp qovluq strukturuna bax
            statusDir = new File(Environment.getExternalStorageDirectory() +
                    "/Android/media/com.whatsapp/WhatsApp/Media/.Statuses");
        }

        if (statusDir.exists() && statusDir.isDirectory()) {
            return statusDir.listFiles(file ->
                    file.getName().endsWith(".mp4") ||
                            file.getName().endsWith(".3gp") ||
                            file.getName().endsWith(".jpg") ||
                            file.getName().endsWith(".jpeg"));
        }
        return new File[0];
    }
}