package com.ali.videodownloader.VideoSites;

import android.app.Activity;
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
import org.jsoup.nodes.Element;

import java.net.URLEncoder;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class FacebookDownloader extends BaseVideoDownloader {

    public FacebookDownloader(ProgressBar progressBar, TextView statusTextView, View downloadButton, Context context, WebView webView) {
        super(progressBar, statusTextView, downloadButton, context,webView);
        this.webView = webView;
    }

    @Override
    protected String getVideoUrl(String url) throws Exception {
        // Show savefrom.net in WebView instead of direct download
        final String saveFromUrl = "https://en.savefrom.net/#url=" + url;

        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webView.setVisibility(View.VISIBLE);
                webView.loadUrl(saveFromUrl);
            }
        });

        return null; // We're handling download through WebView
    }

    @Override
    protected String doInBackground(String... params) {
        // Don't do background download for YouTube
        return "SaveFrom.net saytı WebView-də açıldı.";
    }


}
