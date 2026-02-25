// ═══════════════════════════════════════════════════════════
//  MainActivity.java — MIND VAULT Brain Puzzle Game
//  Android WebView + AdMob Integration
//  Publisher: Nex Genix Business School
// ═══════════════════════════════════════════════════════════

package com.nexgenix.mindvault;

import android.os.Bundle;
import android.webkit.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.ads.*;
import com.google.android.gms.ads.rewarded.*;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private RewardedAd rewardedAd;

    // ══════════════════════════════════════════════════════
    //  YOUR REAL ADMOB IDs
    // ══════════════════════════════════════════════════════
    private static final String APP_ID          = "ca-app-pub-2619727838493895~9979077428";
    private static final String BANNER_UNIT_ID  = "ca-app-pub-2619727838493895/2299897957";

    private static final String REWARDED_UNIT_ID = "ca-app-pub-2619727838493895/5179011032"; // ✅ Real Rewarded unit

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Initialize AdMob SDK
        MobileAds.initialize(this, initializationStatus -> {});

        // 2. Setup WebView
        webView = findViewById(R.id.webView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setDomStorageEnabled(true);      // needed for localStorage (game saves)
        settings.setAllowFileAccessFromFileURLs(true);

        // 3. Inject JavaScript Bridge (JS calls Android.showRewardedAd())
        webView.addJavascriptInterface(new AdMobBridge(), "Android");

        // 4. Load Mind Vault
        // Option A — local asset (faster, works offline):
        webView.loadUrl("file:///android_asset/index.html");
        // Option B — hosted on GitHub Pages (easier to update):
        // webView.loadUrl("https://nexgenixcreativesolutions.github.io/mind-vault/");

        // 5. Pre-load rewarded ad so it's ready instantly
        loadRewardedAd();

        // 6. Setup banner ad
        setupBanner();
    }

    // ── Banner Ad Setup ──────────────────────────────────────
    private void setupBanner() {
        AdView bannerView = findViewById(R.id.adView);
        if (bannerView != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            bannerView.loadAd(adRequest);
        }
    }

    // ── Load / pre-cache a Rewarded Ad ──────────────────────
    private void loadRewardedAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, REWARDED_UNIT_ID, adRequest,
            new RewardedAdLoadCallback() {
                @Override
                public void onAdLoaded(RewardedAd ad) {
                    rewardedAd = ad;
                }
                @Override
                public void onAdFailedToLoad(LoadAdError error) {
                    rewardedAd = null;
                    // Retry loading after a short delay
                    webView.postDelayed(() -> loadRewardedAd(), 5000);
                }
            });
    }

    // ── JavaScript Bridge ────────────────────────────────────
    private class AdMobBridge {

        // Called from JS: Android.showRewardedAd()
        // Used for: unlock bonus world, +30 seconds on fail
        @JavascriptInterface
        public void showRewardedAd() {
            runOnUiThread(() -> {
                if (rewardedAd != null) {
                    rewardedAd.show(MainActivity.this, rewardItem -> {
                        // Reward earned — notify the game
                        webView.post(() ->
                            webView.evaluateJavascript("window.onAdRewarded()", null)
                        );
                        // Pre-load next ad immediately
                        rewardedAd = null;
                        loadRewardedAd();
                    });
                } else {
                    // Ad not ready yet — fall back to simulated ad in JS
                    webView.post(() ->
                        webView.evaluateJavascript("adFinished()", null)
                    );
                    loadRewardedAd();
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AdView bannerView = findViewById(R.id.adView);
        if (bannerView != null) bannerView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        AdView bannerView = findViewById(R.id.adView);
        if (bannerView != null) bannerView.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AdView bannerView = findViewById(R.id.adView);
        if (bannerView != null) bannerView.destroy();
    }
}

// ═══════════════════════════════════════════════════════════
//  res/layout/activity_main.xml
// ═══════════════════════════════════════════════════════════
/*
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <WebView
        android:id="@+id/webView"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"/>

    <com.google.android.gms.ads.AdView
        xmlns:ads="http://schemas.android.com/apk/res-auto"
        android:id="@+id/adView"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom|center_horizontal"
        ads:adSize="BANNER"
        ads:adUnitId="ca-app-pub-2619727838493895/2299897957"/>

</LinearLayout>
*/

// ═══════════════════════════════════════════════════════════
//  AndroidManifest.xml — add inside <application> tag
// ═══════════════════════════════════════════════════════════
/*
<uses-permission android:name="android.permission.INTERNET"/>

<application ...>

    <!-- YOUR REAL AdMob App ID -->
    <meta-data
        android:name="com.google.android.gms.ads.APPLICATION_ID"
        android:value="ca-app-pub-2619727838493895~9979077428"/>

    <activity android:name=".MainActivity" android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.MAIN"/>
            <category android:name="android.intent.category.LAUNCHER"/>
        </intent-filter>
    </activity>

</application>
*/

// ═══════════════════════════════════════════════════════════
//  build.gradle (app level)
// ═══════════════════════════════════════════════════════════
/*
dependencies {
    implementation 'com.google.android.gms:play-services-ads:23.0.0'
}
*/

// ═══════════════════════════════════════════════════════════
//  NEXT STEP: Create your Rewarded Ad unit
//  1. Go to admob.google.com
//  2. Apps → Mind Vault → Ad units → Add ad unit
//  3. Choose "Rewarded"
//  4. Name it "Mind Vault Rewarded"
//  5. Copy the unit ID (ca-app-pub-2619727838493895/XXXXXXXXXX)
//  6. Replace REWARDED_UNIT_ID at the top of this file
// ═══════════════════════════════════════════════════════════
