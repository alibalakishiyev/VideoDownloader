package com.ali.videodownloader.VideoSites;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ali.videodownloader.utils.BaseVideoDownloader;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenericDownloader extends BaseVideoDownloader {


    public GenericDownloader(ProgressBar progressBar, TextView statusTextView, View downloadButton, Context context, WebView webView) {
        super(progressBar, statusTextView, downloadButton, context,webView);
    }

    @Override
    protected String getVideoUrl(String url) throws Exception {

        // Then try the original generic approach
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(getRandomUserAgent())
                    .get();

            Element videoElement = doc.select("meta[property=og:video]").first();
            if (videoElement != null) {
                return videoElement.attr("content");
            }
        } catch (Exception e) {
            Log.e("Generic", "Failed to get from meta tags", e);
        }

        // Fallback to savefrom.net API
        String apiUrl = "https://api.savefrom.net/api/convert?url=" + URLEncoder.encode(url, "UTF-8");
        Document doc = Jsoup.connect(apiUrl)
                .userAgent(getRandomUserAgent())
                .ignoreContentType(true)
                .timeout(30000)
                .get();

        String json = doc.body().text();
        JSONObject jsonObject = new JSONObject(json);
        if (jsonObject.has("url")) {
            return jsonObject.getString("url");
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
