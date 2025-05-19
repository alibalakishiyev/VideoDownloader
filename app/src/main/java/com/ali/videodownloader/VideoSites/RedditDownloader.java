package com.ali.videodownloader.VideoSites;

import android.content.Context;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ali.videodownloader.utils.BaseVideoDownloader;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URLEncoder;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RedditDownloader extends BaseVideoDownloader {

    public RedditDownloader(ProgressBar progressBar, TextView statusTextView,
                            View downloadButton, Context context, WebView webView) {
        super(progressBar, statusTextView, downloadButton, context, webView);
    }

    @Override
    protected String getVideoUrl(String url) throws Exception {
        try {
            // Use Reddit's official API
            String postId = url.split("/comments/")[1].split("/")[0];
            String apiUrl = "https://www.reddit.com/comments/" + postId + ".json";

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .header("User-Agent", "MyApp/1.0")
                    .build();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new Exception("Reddit API request failed: " + response.code());
            }

            JSONArray jsonArray = new JSONArray(response.body().string());
            JSONObject postData = jsonArray.getJSONObject(0)
                    .getJSONObject("data")
                    .getJSONArray("children")
                    .getJSONObject(0)
                    .getJSONObject("data");

            if (!postData.has("is_video") || !postData.getBoolean("is_video")) {
                throw new Exception("This post doesn't contain a video");
            }

            String videoUrl = postData.getJSONObject("media")
                    .getJSONObject("reddit_video")
                    .getString("fallback_url");

            return videoUrl.split("\\?")[0]; // Remove query parameters
        } catch (Exception e) {
            throw new Exception("Reddit download error: " + e.getMessage());
        }
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
            if (downloadUrl != null && !downloadUrl.isEmpty()) {
                cacheUrl(videoUrl, downloadUrl);
                return downloadVideo(downloadUrl);
            }
            return "Video URL'si alınamadı";
        } catch (Exception e) {
            return "Xəta: " + e.getMessage();
        }
    }
}