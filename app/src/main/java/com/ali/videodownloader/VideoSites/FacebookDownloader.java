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
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FacebookDownloader extends BaseVideoDownloader {

    private static final String[] API_ENDPOINTS = {
            "https://fbdownloader.net/api/video?url=",
            "https://getfbstuff.com/api/video?url=",
            "https://snapsave.app/api/get-video?url="
    };

    private static final String MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.45 Mobile Safari/537.36";

    public FacebookDownloader(ProgressBar progressBar, TextView statusTextView,
                              View downloadButton, Context context, WebView webView) {
        super(progressBar, statusTextView, downloadButton, context, webView);
    }

    @Override
    protected String getVideoUrl(String url) throws Exception {
        // Step 1: Convert share link to watch link if needed
        String normalizedUrl = normalizeFacebookUrl(url);

        // Step 2: Try all available APIs
        for (String apiEndpoint : API_ENDPOINTS) {
            try {
                String apiUrl = apiEndpoint + URLEncoder.encode(normalizedUrl, "UTF-8");
                String videoUrl = tryApiDownload(apiUrl);
                if (videoUrl != null) {
                    return videoUrl;
                }
            } catch (Exception e) {
                // Continue to next API if this one fails
            }
        }

        // Step 3: If APIs fail, try direct mobile parsing with multiple methods
        String mobileUrl = normalizedUrl.replace("www.facebook", "m.facebook")
                .replace("facebook", "m.facebook");

        // Method 1: Try standard meta tag extraction
        try {
            String videoUrl = extractFromMobilePage(mobileUrl);
            if (videoUrl != null) {
                return videoUrl;
            }
        } catch (Exception e) {
            // Continue to next method
        }

        // Method 2: Try alternative parsing
        try {
            String videoUrl = extractWithAlternativeMethod(mobileUrl);
            if (videoUrl != null) {
                return videoUrl;
            }
        } catch (Exception e) {
            // Continue to next method
        }

        // Method 3: Try regex pattern matching
        try {
            String videoUrl = extractWithRegex(mobileUrl);
            if (videoUrl != null) {
                return videoUrl;
            }
        } catch (Exception e) {
            // All methods failed
        }

        throw new Exception("Video URL'si tapılmadı. Səbəblər:\n" +
                "1. Video gizli ola bilər\n" +
                "2. Facebook strukturunu dəyişib\n" +
                "3. API servisləri bloklanıb\n\n" +
                "Ətraflı məlumat üçün daha sadə bir video ilə cəhd edin.");
    }

    private String normalizeFacebookUrl(String url) {
        if (url.contains("/share/v/")) {
            String videoId = url.substring(url.lastIndexOf('/') + 1);
            return "https://www.facebook.com/watch/?v=" + videoId.replace("/", "");
        }
        return url;
    }

    private String tryApiDownload(String apiUrl) throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(apiUrl)
                .header("User-Agent", MOBILE_USER_AGENT)
                .build();

        Response response = client.newCall(request).execute();
        String responseBody = response.body().string();
        JSONObject json = new JSONObject(responseBody);

        if (json.has("links")) {
            JSONObject links = json.getJSONObject("links");
            if (links.has("HD")) {
                return links.getString("HD");
            }
            if (links.has("Download High Quality")) {
                return links.getString("Download High Quality");
            }
        }

        if (json.has("url")) {
            return json.getString("url");
        }

        return null;
    }

    private String extractFromMobilePage(String mobileUrl) throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(mobileUrl)
                .header("User-Agent", MOBILE_USER_AGENT)
                .build();

        Response response = client.newCall(request).execute();
        String html = response.body().string();
        Document doc = Jsoup.parse(html);

        // Try meta tag first
        Element meta = doc.select("meta[property=og:video]").first();
        if (meta != null) {
            String videoUrl = meta.attr("content");
            if (!videoUrl.isEmpty()) {
                return videoUrl;
            }
        }

        // Try video tag
        Elements videos = doc.select("video");
        for (Element video : videos) {
            String src = video.attr("src");
            if (!src.isEmpty()) {
                return src;
            }
        }

        return null;
    }

    private String extractWithAlternativeMethod(String mobileUrl) throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(mobileUrl)
                .header("User-Agent", MOBILE_USER_AGENT)
                .header("Accept-Language", "en-US,en;q=0.9")
                .build();

        Response response = client.newCall(request).execute();
        String html = response.body().string();

        // Look for data-store attribute
        Pattern pattern = Pattern.compile("data-store=\\\"{.*?\\\"src\\\":\\\"(.*?)\\\"");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1).replace("\\/", "/");
        }

        return null;
    }

    private String extractWithRegex(String mobileUrl) throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(mobileUrl)
                .header("User-Agent", MOBILE_USER_AGENT)
                .build();

        Response response = client.newCall(request).execute();
        String html = response.body().string();

        // Try to find HD source
        Pattern pattern = Pattern.compile("\"hd_src\":\"(.*?)\"");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1).replace("\\/", "/");
        }

        // Try to find SD source
        pattern = Pattern.compile("\"sd_src\":\"(.*?)\"");
        matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1).replace("\\/", "/");
        }

        return null;
    }

    @Override
    protected String doInBackground(String... params) {
        String videoUrl = params[0];
        try {
            String downloadUrl = getVideoUrl(videoUrl);
            if (downloadUrl == null || downloadUrl.isEmpty()) {
                return "Video URL'si alınamadı";
            }
            return downloadVideo(downloadUrl);
        } catch (Exception e) {
            return "Xəta: " + e.getMessage();
        }
    }
}