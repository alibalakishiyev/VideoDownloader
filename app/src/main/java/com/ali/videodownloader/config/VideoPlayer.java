package com.ali.videodownloader.config;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ali.videodownloader.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class VideoPlayer extends AppCompatActivity {

    private VideoView videoView;
    private ImageButton downloadButton;
    private String mediaPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        videoView = findViewById(R.id.videoView);
        downloadButton = findViewById(R.id.downloadButton);

        mediaPath = getIntent().getStringExtra("mediaPath");

        videoView.setVideoURI(Uri.parse(mediaPath));
        videoView.start();

        downloadButton.setOnClickListener(v -> downloadVideo());
    }

    private void downloadVideo() {
        try {
            File sourceFile = new File(mediaPath);
            File destinationDir = new File(Environment.getExternalStorageDirectory(), "Download/DownloadedVideos");

            if (!destinationDir.exists()) {
                destinationDir.mkdirs();
            }

            File destinationFile = new File(destinationDir, sourceFile.getName());

            FileInputStream in = new FileInputStream(sourceFile);
            FileOutputStream out = new FileOutputStream(destinationFile);

            byte[] buffer = new byte[1024];
            int length;

            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            in.close();
            out.close();

            // Qalereyaya əlavə et
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(Uri.fromFile(destinationFile));
            sendBroadcast(mediaScanIntent);

            Toast.makeText(this, "Video qalereyaya yükləndi!", Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Xəta baş verdi", Toast.LENGTH_SHORT).show();
        }
    }

}

