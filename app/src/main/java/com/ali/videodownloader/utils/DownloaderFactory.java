package com.ali.videodownloader.utils;

import android.content.Context;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ali.videodownloader.VideoSites.FacebookDownloader;
import com.ali.videodownloader.VideoSites.GenericDownloader;
import com.ali.videodownloader.VideoSites.InstagramDownloader;
import com.ali.videodownloader.VideoSites.PinterestDownloader;
import com.ali.videodownloader.VideoSites.RedditDownloader;
import com.ali.videodownloader.VideoSites.RutubeDownloader;
import com.ali.videodownloader.VideoSites.TikTokDownloader;
import com.ali.videodownloader.VideoSites.YouTubeDownloader;

public class DownloaderFactory {


    public static BaseVideoDownloader createDownloader(String platform,
                                                       ProgressBar progressBar,
                                                       TextView statusTextView,
                                                       View downloadButton,
                                                       Context context,
                                                       WebView webView) {
        switch (platform) {
            case "TikTok":
                return new TikTokDownloader(progressBar, statusTextView, downloadButton, context,webView);
            case "Facebook":
                return new FacebookDownloader(progressBar, statusTextView, downloadButton, context,webView);
            case "Reddit":
                return new RedditDownloader(progressBar, statusTextView, downloadButton, context,webView);
            case "Instagram":
                return new InstagramDownloader(progressBar, statusTextView, downloadButton, context,webView);
            case "YouTube":
                return new YouTubeDownloader(progressBar, statusTextView, downloadButton, context, webView);
            case "Pinterest":
                return new PinterestDownloader(progressBar, statusTextView, downloadButton, context, webView);
            case "Rutube":
                return new RutubeDownloader(progressBar, statusTextView, downloadButton, context, webView);
            default:
                return new GenericDownloader(progressBar, statusTextView, downloadButton, context,webView);
        }
    }
}

