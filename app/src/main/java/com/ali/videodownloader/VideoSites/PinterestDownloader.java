package com.ali.videodownloader.VideoSites;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ali.videodownloader.utils.BaseVideoDownloader;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PinterestDownloader extends BaseVideoDownloader {


    public PinterestDownloader(ProgressBar progressBar, TextView statusTextView, View downloadButton, Context context, WebView webView) {
        super(progressBar, statusTextView, downloadButton, context,webView);
    }

    @Override
    protected String getVideoUrl(String url) throws Exception {
        try {
            // First ensure we have a proper Pinterest URL
            if (!url.contains("pinterest.com") && !url.contains("pin.it")) {
                url = "https://www.pinterest.com/pin/" + extractPinId(url);
            }

            // Use mobile user agent to get simpler page structure
            String mobileUserAgent = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36";

            Document doc = Jsoup.connect(url)
                    .userAgent(mobileUserAgent)
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .referrer("https://www.pinterest.com/")
                    .timeout(20000)
                    .get();

            // Method 1: Check for video in JSON data
            Element scriptElement = doc.select("script[data-test-id=video-snippet]").first();
            if (scriptElement != null) {
                String json = scriptElement.html();
                JSONObject jsonObject = new JSONObject(json);
                if (jsonObject.has("contentUrl")) {
                    String videoUrl = jsonObject.getString("contentUrl");
                    if (videoUrl.startsWith("http")) {
                        return videoUrl;
                    }
                }
            }

            // Method 2: Check for video in meta tags
            Element videoElement = doc.select("meta[property=og:video]").first();
            if (videoElement != null) {
                String videoUrl = videoElement.attr("content");
                if (videoUrl.startsWith("http")) {
                    return videoUrl;
                }
            }

            // Method 3: Find video in HTML5 video tag
            Element videoTag = doc.select("video").first();
            if (videoTag != null) {
                String videoUrl = videoTag.attr("src");
                if (videoUrl.startsWith("http")) {
                    return videoUrl;
                }

                // Check for source tags inside video
                Element sourceTag = videoTag.select("source").first();
                if (sourceTag != null) {
                    videoUrl = sourceTag.attr("src");
                    if (videoUrl.startsWith("http")) {
                        return videoUrl;
                    }
                }
            }

            // Method 4: Alternative API approach
            String pinId = extractPinId(url);
            if (pinId != null) {
                String apiUrl = "https://api.pinterest.com/v3/pidgets/pins/info/?pin_ids=" + pinId;

                Document apiDoc = Jsoup.connect(apiUrl)
                        .userAgent(mobileUserAgent)
                        .ignoreContentType(true)
                        .timeout(20000)
                        .get();

                String apiJson = apiDoc.body().text();
                JSONObject jsonResponse = new JSONObject(apiJson);
                if (jsonResponse.has("data")) {
                    JSONArray pins = jsonResponse.getJSONArray("data");
                    if (pins.length() > 0) {
                        JSONObject pin = pins.getJSONObject(0);
                        if (pin.has("videos")) {
                            JSONObject videos = pin.getJSONObject("videos");
                            if (videos.has("video_list")) {
                                JSONObject videoList = videos.getJSONObject("video_list");
                                if (videoList.has("V_720P")) {
                                    return videoList.getJSONObject("V_720P").getString("url");
                                } else if (videoList.length() > 0) {
                                    // Get first available video
                                    String firstKey = videoList.keys().next();
                                    return videoList.getJSONObject(firstKey).getString("url");
                                }
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            Log.e("Pinterest_Download", "Error getting URL: " + e.getMessage());
        }

        return null;
    }

    private String extractPinId(String url) {
        // Extract pin ID from various URL formats
        String pattern = "(?:pin\\.it|pinterest\\.com\\/pin)\\/([a-zA-Z0-9]+)";
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
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
