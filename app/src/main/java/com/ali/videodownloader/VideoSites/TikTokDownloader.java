package com.ali.videodownloader.VideoSites;

import android.content.Context;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ali.videodownloader.utils.BaseVideoDownloader;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class TikTokDownloader extends BaseVideoDownloader {

    public TikTokDownloader(ProgressBar progressBar, TextView statusTextView, View downloadButton, Context context, WebView webView) {
        super(progressBar, statusTextView, downloadButton, context, webView);
    }

    @Override
    public String getVideoUrl(String url) throws Exception {
        String apiUrl = "https://www.tikwm.com/api/?url=" + URLEncoder.encode(url, "UTF-8");
        Document doc = Jsoup.connect(apiUrl)
                .userAgent(getRandomUserAgent())
                .referrer("https://www.tiktok.com/")
                .ignoreContentType(true)
                .timeout(30000)
                .get();

        String json = doc.body().text();
        JSONObject jsonObject = new JSONObject(json);
        if (jsonObject.has("data")) {
            JSONObject data = jsonObject.getJSONObject("data");
            if (data.has("play")) {
                return data.getString("play");
            }
        }
        return null;
    }


    @Override
    protected String doInBackground(String... params) {
        String videoUrl = params[0];
        String cachedUrl = getCachedUrl(videoUrl);
        if (cachedUrl != null) {
            return downloadVideo(cachedUrl);
        }

        try {
            String downloadUrl = getVideoUrl(videoUrl);
            if (downloadUrl == null || downloadUrl.isEmpty()) {
                return "Video URL'si alınamadı";
            }

            cacheUrl(videoUrl, downloadUrl);
            return downloadVideo(downloadUrl);
        } catch (Exception e) {
            return "Xəta: " + e.getMessage();
        }
    }

    protected String downloadVideo(String downloadUrl) {
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;

        try {
            URL url = new URL(downloadUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            int fileLength = connection.getContentLength();
            if (fileLength <= 0) {
                return "Fayl uzunluğu sıfırdır";
            }

            input = connection.getInputStream();
            File outputFile = new File(context.getExternalFilesDir(null), "tiktok_video.mp4");
            output = new FileOutputStream(outputFile);

            byte[] buffer = new byte[4096];
            long total = 0;
            int count;
            int lastProgress = 0;

            while ((count = input.read(buffer)) != -1) {
                total += count;
                output.write(buffer, 0, count);

                if (fileLength > 0) {
                    int progress = (int) (total * 100 / fileLength);
                    if (progress != lastProgress) {
                        publishProgress(progress);
                        lastProgress = progress;
                    }
                }
            }

            return "Uğurla yükləndi: " + outputFile.getAbsolutePath();
        } catch (Exception e) {
            return "Yükləmə xətası: " + e.getMessage();
        } finally {
            try {
                if (output != null) output.close();
                if (input != null) input.close();
            } catch (IOException ignored) {}

            if (connection != null) connection.disconnect();
        }
    }


    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        progressBar.setIndeterminate(false); // dairə yox, konkret faiz
        progressBar.setProgress(values[0]);  // faiz göstər
    }




}
