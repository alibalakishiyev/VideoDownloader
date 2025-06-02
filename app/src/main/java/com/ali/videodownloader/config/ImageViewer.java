package com.ali.videodownloader.config;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ali.videodownloader.R;
import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageViewer extends AppCompatActivity {

    private ImageView imageView;
    private ImageButton btnDownload;
    private String imagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        imageView = findViewById(R.id.imageView);
        btnDownload = findViewById(R.id.imgdownloadbtn);

        imagePath = getIntent().getStringExtra("mediaPath");
        Glide.with(this).load(new File(imagePath)).into(imageView);

        btnDownload.setOnClickListener(v -> downloadImage());
    }

    private void downloadImage() {
        try {
            File sourceFile = new File(imagePath);
            File destinationDir = new File(Environment.getExternalStorageDirectory(), "Download/DownloadedImages");

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

            Toast.makeText(this, "Şəkil qalereyaya yükləndi!", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Xəta baş verdi", Toast.LENGTH_SHORT).show();
        }
    }
}
