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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RutubeDownloader extends BaseVideoDownloader {

    public RutubeDownloader(ProgressBar progressBar, TextView statusTextView, View downloadButton, Context context, WebView webView) {
        super(progressBar, statusTextView, downloadButton, context,webView);
    }

    @Override
    protected String getVideoUrl(String url) throws Exception {
        try {
            String videoId = extractRutubeVideoId(url);
            if (videoId == null) {
                return null;
            }

            // Use mobile API endpoint with HTTPS
            String apiUrl = "https://rutube.ru/api/play/options/" + videoId + "/?format=json";

            Log.d("RutubeDownloader", "Fetching video from API: " + apiUrl);

            Document doc = Jsoup.connect(apiUrl)
                    .userAgent("Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36")
                    .referrer("https://m.rutube.ru")
                    .ignoreContentType(true)
                    .timeout(20000)
                    .get();

            String json = doc.body().text();
            JSONObject jsonObject = new JSONObject(json);

            // Debug log the complete JSON response
            Log.d("RutubeDownloader", "API Response: " + jsonObject.toString());

            // First try to get MP4 URL
            if (jsonObject.has("video_balancer")) {
                JSONObject balancer = jsonObject.getJSONObject("video_balancer");

                if (balancer.has("mp4")) {
                    JSONObject mp4 = balancer.getJSONObject("mp4");
                    if (mp4.has("url")) {
                        String videoUrl = mp4.getString("url");
                        if (videoUrl.startsWith("//")) {
                            videoUrl = "https:" + videoUrl;
                        }
                        Log.d("RutubeDownloader", "Found MP4 URL: " + videoUrl);
                        return videoUrl;
                    }
                }

                // Then try m3u8
                if (balancer.has("m3u8")) {
                    String m3u8Url = balancer.getString("m3u8");
                    if (m3u8Url.startsWith("//")) {
                        m3u8Url = "https:" + m3u8Url;
                    }
                    Log.d("RutubeDownloader", "Found M3U8 URL: " + m3u8Url);
                    return m3u8Url;
                }
            }

            // Alternative approach - check for direct video URLs
            if (jsonObject.has("video")) {
                JSONObject video = jsonObject.getJSONObject("video");
                if (video.has("url")) {
                    String videoUrl = video.getString("url");
                    if (videoUrl.startsWith("//")) {
                        videoUrl = "https:" + videoUrl;
                    }
                    return videoUrl;
                }
            }

        } catch (Exception e) {
            Log.e("Rutube_Download", "Error getting URL", e);
        }

        return null;
    }

    private String extractRutubeVideoId(String url) {
        // Handle both regular and embed URLs
        String[] patterns = {
                "rutube\\.ru\\/video\\/([a-f0-9]{32})",
                "rutube\\.ru\\/play\\/embed\\/([a-f0-9]{32})",
                "rutube\\.ru\\/video\\/embed\\/([a-f0-9]{32})",
                "rutube\\.ru\\/video\\/([a-f0-9]{32})\\/"
        };

        for (String pattern : patterns) {
            Pattern compiledPattern = Pattern.compile(pattern);
            Matcher matcher = compiledPattern.matcher(url);
            if (matcher.find()) {
                return matcher.group(1);
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