package com.ali.videodownloader.utils;

import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ali.videodownloader.R;

public class WebBrowser extends AppCompatActivity {

    private WebView webView;
    private EditText urlEditText;
    private Button goButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_browser);

        // View'ları bağla
        webView = findViewById(R.id.webView);
        urlEditText = findViewById(R.id.urlEditText);
        goButton = findViewById(R.id.goButton);

        // WebView ayarları
        setupWebView();

        // Varsayılan olarak YouTube'u yükle
        loadUrl("https://www.youtube.com");

        // Go butonu click listener
        goButton.setOnClickListener(v -> loadUrlFromInput());

        // Klavyeden GO tuşu ile gönderme
        urlEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                loadUrlFromInput();
                return true;
            }
            return false;
        });
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                urlEditText.setText(url); // Gezilen URL'yi EditText'te göster
                return false;
            }
        });
    }

    private void loadUrlFromInput() {
        String url = urlEditText.getText().toString().trim();

        if (!url.isEmpty()) {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            loadUrl(url);
        } else {
            Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUrl(String url) {
        webView.loadUrl(url);
        urlEditText.setText(url);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}