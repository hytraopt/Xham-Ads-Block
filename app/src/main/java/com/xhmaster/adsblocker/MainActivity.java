package com.xhmaster.adsblocker;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private WebView webView;

    // Primary site
    private static final String HOME_URL = "https://xhamster.com";

    // List of domains to allow (basic whitelist approach)
    private static final Set<String> ALLOWED_DOMAINS = new HashSet<>();

    // List of known ad / tracker keywords to block
    private static final String[] AD_KEYWORDS = new String[]{
            "ad", "ads", "advert", "banner", "promo", "sponsor",
            "doubleclick", "googlesyndication", "googleadservices",
            "adservice", "adserver", "tracking", "analytics",
            "adnetwork", "adcontent", "adimg", "adtrack"
    };

    static {
        // Allow main site and important subdomains / CDNs
        ALLOWED_DOMAINS.add("xhamster.com");
        ALLOWED_DOMAINS.add("www.xhamster.com");
        ALLOWED_DOMAINS.add("m.xhamster.com");
        ALLOWED_DOMAINS.add("premium.xhamster.com");
        ALLOWED_DOMAINS.add("static.xhamster.com");
        ALLOWED_DOMAINS.add("thumb-p7.xhamster.com");
        ALLOWED_DOMAINS.add("thumb-p8.xhamster.com");
        // Add more if needed after testing
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        setupWebView();

        webView.loadUrl(HOME_URL);
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Enable cookies (for login etc.)
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (shouldBlockUrl(url)) {
                    // Block ad / tracker URL by returning true and not loading
                    return true;
                }
                // Allow normal navigation inside WebView
                view.loadUrl(url);
                return true;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (shouldBlockUrl(url)) {
                    // Return empty response to block ad/tracker request
                    return new WebResourceResponse("text/plain", "UTF-8",
                            new ByteArrayInputStream("".getBytes()));
                }
                return super.shouldInterceptRequest(view, request);
            }
        });

        // Optional: Open some links in external browser
        findViewById(R.id.btnOpenInBrowser).setOnClickListener(v -> {
            String url = webView.getUrl();
            if (url == null) url = HOME_URL;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
    }

    private boolean shouldBlockUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        String lower = url.toLowerCase();

        // Block data: and about: for safety (optional)
        if (lower.startsWith("data:") || lower.startsWith("about:")) {
            return false; // usually okay to allow
        }

        // Check ad keywords
        for (String kw : AD_KEYWORDS) {
            if (lower.contains(kw)) {
                // Extra check: if it's from allowed main domain and critical, you may skip
                // For now, we block aggressively
                return true;
            }
        }

        // Optional: strict whitelist mode (comment out if too restrictive)
        // return !isAllowedDomain(url);

        return false;
    }

    private boolean isAllowedDomain(String url) {
        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            if (host == null) return false;

            for (String allowed : ALLOWED_DOMAINS) {
                if (host.equals(allowed) || host.endsWith("." + allowed)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
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