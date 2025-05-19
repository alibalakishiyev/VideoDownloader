package com.ali.videodownloader.utils;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public abstract class BaseVideoDownloader extends AsyncTask<String, Integer, String> {
    protected ProgressBar progressBar;
    protected TextView statusTextView;
    protected View downloadButton;
    protected Context context;
    protected File outputFile;
    protected WebView webView;
    protected static Map<String, String> urlCache = new HashMap<>();


    protected abstract String getVideoUrl(String url) throws Exception;


    public BaseVideoDownloader(ProgressBar progressBar, TextView statusTextView, View downloadButton, Context context, WebView webView) {
        this.progressBar = progressBar;
        this.statusTextView = statusTextView;
        this.downloadButton = downloadButton;
        this.context = context;
        this.webView = webView;
    }
    @Override
    protected void onPreExecute() {
        progressBar.setProgress(0);
        progressBar.setVisibility(View.VISIBLE);
        statusTextView.setText("Download starting...");
        downloadButton.setEnabled(false);
    }


    protected String downloadVideo(String downloadUrl) {
        File tempFile = null;
        try {
            URL url = new URL(downloadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", getRandomUserAgent());
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "Connection error: " + connection.getResponseCode();
            }

            // Create videos directory if it doesn't exist
            File videosDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), "Videos");
            if (!videosDir.exists() && !videosDir.mkdirs()) {
                return "Failed to create download directory";
            }

            // Create temp file during download
            String fileName = "video_" + System.currentTimeMillis() + ".mp4";
            tempFile = new File(videosDir, fileName + ".temp");
            outputFile = new File(videosDir, fileName);

            InputStream input = new BufferedInputStream(connection.getInputStream());
            OutputStream output = new FileOutputStream(tempFile);

            byte[] data = new byte[8192];
            long total = 0;
            int count;
            int fileLength = connection.getContentLength();

            while ((count = input.read(data)) != -1) {
                if (isCancelled()) {
                    input.close();
                    output.close();
                    tempFile.delete();
                    return "Download cancelled";
                }
                total += count;
                publishProgress((int) (total * 100 / fileLength));
                output.write(data, 0, count);
            }

            output.flush();
            output.close();
            input.close();

            // Validate the downloaded file
            if (!validateDownloadedFile(tempFile)) {
                tempFile.delete();
                return "Downloaded file is corrupted";
            }

            // Rename temp file to final file
            if (!tempFile.renameTo(outputFile)) {
                // If rename fails, try copy
                copyFile(tempFile, outputFile);
            }

            scanFile(outputFile);
            return "Download complete: " + outputFile.getAbsolutePath();

        } catch (Exception e) {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
            return "Error: " + e.getMessage();
        }
    }

    protected String getRandomUserAgent() {
        String[] agents = {
                "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X)",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36"
        };
        return agents[new Random().nextInt(agents.length)];
    }

    private void copyFile(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }

    private boolean validateDownloadedFile(File file) {
        if (!file.exists() || file.length() < 102400) { // At least 100KB
            return false;
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            // Check MP4 file signature (ftyp)
            if (raf.length() < 12) return false;

            raf.seek(4);
            byte[] ftyp = new byte[4];
            raf.read(ftyp);

            // Check for 'ftyp' signature
            if (ftyp[0] != 'f' || ftyp[1] != 't' || ftyp[2] != 'y' || ftyp[3] != 'p') {
                return false;
            }

            // Additional check for moov atom (indicates complete MP4)
            raf.seek(0);
            byte[] buffer = new byte[1024];
            int bytesRead = raf.read(buffer);

            // Search for 'moov' in the first 1KB
            for (int i = 0; i < bytesRead - 4; i++) {
                if (buffer[i] == 'm' && buffer[i+1] == 'o' && buffer[i+2] == 'o' && buffer[i+3] == 'v') {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            Log.e("FileValidation", "Error validating file", e);
            return false;
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        progressBar.setProgress(values[0]);
        statusTextView.setText("Downloading: " + values[0] + "%");
    }

    @Override
    protected void onPostExecute(String result) {
        progressBar.setVisibility(View.GONE);
        statusTextView.setText(result);
        downloadButton.setEnabled(true);
        Toast.makeText(context, result, Toast.LENGTH_LONG).show();
    }
    private void scanFile(File file) {
        MediaScannerConnection.scanFile(
                context,
                new String[]{file.getAbsolutePath()},
                null,
                (path, uri) -> Log.d("ScanFile", "Fayil Yuklendi: " + path)
        );
    }
    protected String getCachedUrl(String originalUrl) {
        if (urlCache.containsKey(originalUrl)) {
            return urlCache.get(originalUrl);
        }
        return null;
    }

    protected void cacheUrl(String originalUrl, String directUrl) {
        urlCache.put(originalUrl, directUrl);
    }
}