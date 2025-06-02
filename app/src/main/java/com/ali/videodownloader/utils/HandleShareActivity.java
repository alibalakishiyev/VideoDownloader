package com.ali.videodownloader.utils;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ali.videodownloader.MainActivity;
import com.ali.videodownloader.VideoSites.FacebookDownloader;
import com.ali.videodownloader.VideoSites.InstagramDownloader;
import com.ali.videodownloader.VideoSites.PinterestDownloader;
import com.ali.videodownloader.VideoSites.RedditDownloader;
import com.ali.videodownloader.VideoSites.TikTokDownloader;
import com.ali.videodownloader.VideoSites.YouTubeDownloader;

public class HandleShareActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        try {
            String action = intent.getAction();
            String type = intent.getType();

            if (Intent.ACTION_SEND.equals(action) && type != null && "text/plain".equals(type)) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null && !sharedText.isEmpty()) {
                    processSharedText(sharedText);
                    return;
                }
            }
            openMainActivity();
        } catch (Exception e) {
            e.printStackTrace();
            openMainActivityWithError();
        }
    }


    private void processSharedText(String text) {
        try {
            String url = text.toLowerCase().trim();

            // URL formatını düzəlt
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }


            if (url.contains("youtube.com") || url.contains("youtu.be")) {
                openDownloader(YouTubeDownloader.class, url);
            } else if (url.contains("instagram.com")) {
                openDownloader(InstagramDownloader.class, url);
            } else if (url.contains("facebook.com")) {
                openDownloader(FacebookDownloader.class, url);
            } else if (url.contains("tiktok.com")) {
                openDownloader(TikTokDownloader.class, url);
            } else if (url.contains("pinterest.com")) {
                openDownloader(PinterestDownloader.class, url);
            } else if (url.contains("reddit.com")) {
                openDownloader(RedditDownloader.class, url);
            } else {
                openMainActivity();
                Toast.makeText(this, "Unsupported link - opened main app", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            openMainActivity();
            Toast.makeText(this, "Error processing link", Toast.LENGTH_SHORT).show();
        }
    }

    private void openDownloader(Class<?> activityClass, String url) {
        try {
            Intent intent = new Intent(this, activityClass);
            intent.putExtra("videoURL", url);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
            openMainActivityWithError();
        }
    }

    private void openMainActivity() {
        try {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openMainActivityWithError() {
        Toast.makeText(this, "Xəta baş verdi, tətbiq açılır...", Toast.LENGTH_LONG).show();
        openMainActivity();
    }

    private void openMainActivityWithMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        openMainActivity();
    }


}