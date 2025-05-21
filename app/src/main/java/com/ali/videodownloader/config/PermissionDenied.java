package com.ali.videodownloader.config;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


public class PermissionDenied  {

    private static final int STORAGE_PERMISSION_CODE = 100;
    private static final int MANAGE_STORAGE_CODE = 101;

    public static boolean checkStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            int write = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int read = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
            return write == PackageManager.PERMISSION_GRANTED &&
                    read == PackageManager.PERMISSION_GRANTED;
        }
    }

    public static void requestStoragePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", activity.getPackageName())));
                activity.startActivityForResult(intent, MANAGE_STORAGE_CODE);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                activity.startActivityForResult(intent, MANAGE_STORAGE_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                    STORAGE_PERMISSION_CODE
            );
        }
    }

    public static   void initializeDownloader(Activity activity) {
        Toast.makeText(activity, "Icaze Verildi", Toast.LENGTH_LONG).show();
    }


    public static void showPermissionDeniedDialog(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("İcazə Tələb Olunur")
                .setMessage("Bu funksiyanı işlətmək üçün yaddaş icazəsi lazımdır. Zəhmət olmasa ayarlardan verin.")
                .setPositiveButton("Ayarlar", (dialog, which) -> openAppSettings(activity))
                .setNegativeButton("İmtina", null)
                .show();
    }

    public static void openAppSettings(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivity(intent);
    }

    public static int getStoragePermissionCode() {
        return STORAGE_PERMISSION_CODE;
    }

    public static int getManageStorageCode() {
        return MANAGE_STORAGE_CODE;
    }


}