package com.smile.karaokeplayer;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;

import androidx.multidex.MultiDexApplication;

import com.google.android.gms.ads.MobileAds;
import com.smile.smilelibraries.facebook_ads_util.FacebookInterstitialAds;
import com.smile.smilelibraries.google_admob_ads_util.GoogleAdMobInterstitial;
import com.smile.smilelibraries.showing_instertitial_ads_utility.ShowingInterstitialAdsUtil;
import com.smile.smilelibraries.utilities.ScreenUtil;

public class SmileApplication extends MultiDexApplication {

    public static final String PrivacyPolicyUrl = "http://ec2-13-59-195-3.us-east-2.compute.amazonaws.com/PrivacyPolicy";
    public static final int FontSize_Scale_Type = ScreenUtil.FontSize_Pixel_Type;
    public static final int leftChannel = 0;
    public static final int rightChannel = 1;
    public static final int stereoChannel = 2;

    public static Resources AppResources;
    public static Context AppContext;

    public static ShowingInterstitialAdsUtil InterstitialAd;
    private static FacebookInterstitialAds facebookAds;
    private static GoogleAdMobInterstitial googleInterstitialAd;

    @Override
    public void onCreate() {
        super.onCreate();

        AppResources = getResources();
        AppContext = getApplicationContext();

        String facebookPlacementID = new String("1712962715503258_1712963252169871");
        facebookAds = new FacebookInterstitialAds(AppContext, facebookPlacementID);
        facebookAds.loadAd();

        // Google AdMob
        String googleAdMobAppID = getString(R.string.google_AdMobAppID);
        String googleAdMobInterstitialID = "ca-app-pub-8354869049759576/1418354889";
        MobileAds.initialize(AppContext, googleAdMobAppID);
        googleInterstitialAd = new GoogleAdMobInterstitial(AppContext, googleAdMobInterstitialID);
        googleInterstitialAd.loadAd(); // load first ad

        InterstitialAd = new ShowingInterstitialAdsUtil(facebookAds, googleInterstitialAd);
    }
}
