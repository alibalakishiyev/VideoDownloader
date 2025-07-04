package com.ali.videodownloader;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ali.videodownloader.VideoSites.FacebookDownloader;
import com.ali.videodownloader.VideoSites.InstagramDownloader;
import com.ali.videodownloader.VideoSites.RedditDownloader;
import com.ali.videodownloader.VideoSites.TikTokDownloader;
import com.ali.videodownloader.VideoSites.WhatsAppStatusDownloader;
import com.ali.videodownloader.VideoSites.YouTubeDownloader;
import com.ali.videodownloader.utils.Downloader;
import com.ali.videodownloader.utils.WebBrowser;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AlgoListener {

    private AdView mAdView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        ArrayList<Algo> arrayList = new ArrayList<>();
        arrayList.add(new Algo(R.drawable.tiktok, "TikTok"));
        arrayList.add(new Algo(R.drawable.youtube, "YouTube"));
        arrayList.add(new Algo(R.drawable.facebook62, "Facebook"));
        arrayList.add(new Algo(R.drawable.instagram, "Instagram"));
        arrayList.add(new Algo(R.drawable.whatsapp, "WhatsApp Status"));
        arrayList.add(new Algo(R.drawable.reddit, "Reddit"));
        arrayList.add(new Algo(R.drawable.pinterest, "Pinterest"));
        arrayList.add(new Algo(R.drawable.download1, "Rutor"));
        arrayList.add(new Algo(R.drawable.explorer62, "WebBrowser"));





        AlgoAdapter algoAdapter = new AlgoAdapter(arrayList, this);
        RecyclerView recyclerView = findViewById(R.id.main_recycler_view);
        recyclerView.setAdapter(algoAdapter);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        getOnBackPressedDispatcher().addCallback(this, callback);
    }




    @Override
    public void onAlgoSelected(Algo algo) {
        if ("WebBrowser".equals(algo.algoText)) {
            // WebBrowser seçildiğinde özel activity'yi başlat
            Intent intent = new Intent(this, WebBrowser.class);
            startActivity(intent);
        } else {
            // Diğer seçenekler için mevcut işlem
            Intent intent = new Intent(this, Downloader.class);
            intent.putExtra("siteName", algo.algoText);
            startActivity(intent);
        }
    }

    OnBackPressedCallback callback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(MainActivity.this);
            materialAlertDialogBuilder.setTitle(R.string.app_name);
            materialAlertDialogBuilder.setMessage("Are you sure want to exit the app?");
            materialAlertDialogBuilder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int i) {
                    dialog.dismiss();
                }
            });
            materialAlertDialogBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int i) {
                    finish();
                }
            });
            materialAlertDialogBuilder.show();
        }
    };

}

class AlgoAdapter extends RecyclerView.Adapter<AlgoViewHolder> {

    private List<Algo> algoList;
    private AlgoListener algoListener;

    public AlgoAdapter(List<Algo> algoList, AlgoListener listener) {
        this.algoList = algoList;
        this.algoListener = listener;
    }

    @NonNull
    @Override
    public AlgoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_icons, parent, false);
        return new AlgoViewHolder(view, algoListener);
    }

    @Override
    public void onBindViewHolder(@NonNull AlgoViewHolder holder, int position) {
        holder.bind(algoList.get(position));
    }

    @Override
    public int getItemCount() {
        return algoList.size();
    }
}

class AlgoViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private ImageView iconImageView;
    private TextView algoTextView;
    private AlgoListener algoListener;
    private Algo algo;

    public AlgoViewHolder(@NonNull View itemView, AlgoListener algoListener) {
        super(itemView);
        itemView.setOnClickListener(this);
        this.algoListener = algoListener;

        iconImageView = itemView.findViewById(R.id.iconImageView);
        algoTextView = itemView.findViewById(R.id.algoTextView);
    }

    public void bind(Algo algo) {
        this.algo = algo;
        iconImageView.setImageResource(algo.iconResourceId);
        algoTextView.setText(algo.algoText);
    }

    @Override
    public void onClick(View v) {
        if (algoListener != null) {
            algoListener.onAlgoSelected(algo);
        }
    }
}

class Algo {
    public int iconResourceId;
    public String algoText;

    public Algo(int iconResourceId, String algoText) {
        this.iconResourceId = iconResourceId;
        this.algoText = algoText;
    }
}


interface AlgoListener {
    void onAlgoSelected(Algo algo);
}
