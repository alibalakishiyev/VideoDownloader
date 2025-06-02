package com.ali.videodownloader.config;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.ali.videodownloader.R;
import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StatusAdapter extends RecyclerView.Adapter<StatusAdapter.StatusViewHolder> {

    private Context context;
    private List<File> mediaFiles;
    private List<File> selectedFiles = new ArrayList<>();  // Seçilmiş fayllar siyahısı

    public StatusAdapter(Context context, List<File> mediaFiles) {
        this.context = context;
        this.mediaFiles = mediaFiles;
    }

    public List<File> getSelectedFiles() {
        return selectedFiles;
    }

    @NonNull
    @Override
    public StatusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.status_item, parent, false);
        return new StatusViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StatusViewHolder holder, int position) {
        File file = mediaFiles.get(position);

        if (selectedFiles.contains(file)) {
            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.pressed_color));
        } else {
            holder.itemView.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
        }

        if (file.getName().endsWith(".jpg")) {
            holder.videoView.setVisibility(View.GONE);
            holder.imageView.setVisibility(View.VISIBLE);
            Glide.with(context).load(file).into(holder.imageView);
        } else if (file.getName().endsWith(".mp4")) {
            holder.videoView.setVisibility(View.GONE);
            holder.imageView.setVisibility(View.VISIBLE);

            Glide.with(context)
                    .load(file)
                    .thumbnail(0.1f)
                    .into(holder.imageView);
        }

        holder.itemView.setOnClickListener(v -> {
            if (selectedFiles.contains(file)) {
                selectedFiles.remove(file);
                holder.itemView.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
            } else {
                selectedFiles.add(file);
                holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.pressed_color));
            }
        });

        holder.imageView.setOnClickListener(v -> openMediaViewer(file));
    }


    private void openMediaViewer(File file) {
        if (file.getName().endsWith(".mp4")) {
            // Video baxmaq üçün yeni aktivitiyə göndər
            Intent intent = new Intent(context, VideoPlayer.class);
            intent.putExtra("mediaPath", file.getAbsolutePath());
            context.startActivity(intent);
        } else if (file.getName().endsWith(".jpg")) {
            // Şəkil baxmaq üçün yeni aktivitiyə göndər
            Intent intent = new Intent(context, ImageViewer.class);
            intent.putExtra("mediaPath", file.getAbsolutePath());
            context.startActivity(intent);
        }
    }


    @Override
    public int getItemCount() {
        return mediaFiles.size();
    }

    static class StatusViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        VideoView videoView;

        public StatusViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.statusImageView);
            videoView = itemView.findViewById(R.id.statusVideoView);
        }
    }
}

