package com.ali.videodownloader.config;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.ali.videodownloader.R;
import com.bumptech.glide.Glide;

import java.io.File;
import java.util.List;

public class StatusAdapter extends RecyclerView.Adapter<StatusAdapter.StatusViewHolder> {

    private Context context;
    private List<File> mediaFiles;

    public StatusAdapter(Context context, List<File> mediaFiles) {
        this.context = context;
        this.mediaFiles = mediaFiles;
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

        if (file.getName().endsWith(".jpg")) {
            // Şəkil faylıdır
            holder.videoView.setVisibility(View.GONE);
            holder.imageView.setVisibility(View.VISIBLE);
            // Glide ilə şəkili yükləyirik
            Glide.with(context)
                    .load(file)
                    .into(holder.imageView);
        } else if (file.getName().endsWith(".mp4")) {
            // Video faylıdır
            holder.imageView.setVisibility(View.GONE);
            holder.videoView.setVisibility(View.VISIBLE);
            holder.videoView.setVideoURI(Uri.fromFile(file));
            holder.videoView.seekTo(1); // Videonun birinci frame-ni göstərmək üçün
            // İstəyə bağlı olaraq autoplay və ya controls əlavə edə bilərsiniz
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
