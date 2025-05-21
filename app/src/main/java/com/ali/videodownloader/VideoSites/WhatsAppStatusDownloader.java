package com.ali.videodownloader.VideoSites;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ali.videodownloader.utils.BaseVideoDownloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WhatsAppStatusDownloader extends BaseVideoDownloader {

    private static final String STATUS_DIRECTORY = "/Android/media/com.whatsapp/WhatsApp/Media/.Statuses";
    private static final String SAVE_DIRECTORY = "/WhatsApp Statuses";

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
        // Əvvəlcə fayl sistemi ilə yoxlayır
        String result = downloadStatusesFromFiles();

        // Əgər status tapılmazsa MediaStore ilə yoxla
        if (result.equals("No statuses found") || result.equals("No video statuses found")) {
            result = downloadStatusesMediaStore();
        }

        return result;
    }

    private String downloadStatusesFromFiles() {
        try {
            File statusDir = new File(context.getExternalFilesDir(null).getParent() + STATUS_DIRECTORY);
            File saveDir = new File(context.getExternalFilesDir(null).getParent() + SAVE_DIRECTORY);

            if (!saveDir.exists() && !saveDir.mkdirs()) {
                return "Error creating save directory";
            }

            File[] statusFiles = statusDir.listFiles();
            if (statusFiles == null || statusFiles.length == 0) {
                return "No statuses found";
            }

            int downloadedCount = 0;
            for (File statusFile : statusFiles) {
                if (isVideoFile(statusFile)) {
                    String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                            .format(new Date(statusFile.lastModified()));

                    String destFileName = "WA_STATUS_" + timestamp + "_" + statusFile.getName();
                    File destFile = new File(saveDir, destFileName);

                    if (copyFile(statusFile, destFile)) {
                        downloadedCount++;
                    }
                }
            }

            return downloadedCount > 0 ? "Downloaded " + downloadedCount + " statuses" : "No video statuses found";

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private String downloadStatusesMediaStore() {
        try {
            Uri uri = MediaStore.Files.getContentUri("external");
            String[] projection = {MediaStore.Files.FileColumns.DATA};
            String selection = MediaStore.Files.FileColumns.DATA + " LIKE ?";
            String[] selectionArgs = new String[]{"%WhatsApp/Media/.Statuses%"};

            Cursor cursor = context.getContentResolver().query(
                    uri,
                    projection,
                    selection,
                    selectionArgs,
                    null
            );

            if (cursor == null) {
                return "Error accessing statuses";
            }

            File saveDir = new File(context.getExternalFilesDir(null).getParent() + SAVE_DIRECTORY);
            if (!saveDir.exists() && !saveDir.mkdirs()) {
                cursor.close();
                return "Error creating save directory";
            }

            int downloadedCount = 0;
            while (cursor.moveToNext()) {
                String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA));
                File sourceFile = new File(path);

                if (isVideoFile(sourceFile)) {
                    String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                            .format(new Date(sourceFile.lastModified()));

                    String destFileName = "WA_STATUS_" + timestamp + "_" + sourceFile.getName();
                    File destFile = new File(saveDir, destFileName);

                    if (copyFile(sourceFile, destFile)) {
                        downloadedCount++;
                    }
                }
            }
            cursor.close();

            return downloadedCount > 0 ? "Downloaded " + downloadedCount + " statuses" : "No video statuses found";

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
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
}
