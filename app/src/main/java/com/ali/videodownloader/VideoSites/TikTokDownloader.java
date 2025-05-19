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

import java.net.URLEncoder;

public class TikTokDownloader extends BaseVideoDownloader {

    public TikTokDownloader(ProgressBar progressBar, TextView statusTextView, View downloadButton, Context context, WebView webView) {
        super(progressBar, statusTextView, downloadButton, context, webView);
    }

    @Override
    protected String getVideoUrl(String url) throws Exception {
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


}
