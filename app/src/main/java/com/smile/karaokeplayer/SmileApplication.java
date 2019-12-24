package com.smile.karaokeplayer;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;

import androidx.multidex.MultiDexApplication;

import com.facebook.ads.AudienceNetworkAds;
import com.google.android.gms.ads.MobileAds;
import com.smile.smilelibraries.facebook_ads_util.FacebookInterstitialAds;
import com.smile.smilelibraries.google_admob_ads_util.GoogleAdMobInterstitial;
import com.smile.smilelibraries.showing_instertitial_ads_utility.ShowingInterstitialAdsUtil;
import com.smile.smilelibraries.utilities.ScreenUtil;

import java.util.LinkedHashMap;

public class SmileApplication extends MultiDexApplication {

    public static final String PrivacyPolicyUrl = "http://ec2-13-59-195-3.us-east-2.compute.amazonaws.com/PrivacyPolicy";
    public static final int FontSize_Scale_Type = ScreenUtil.FontSize_Pixel_Type;
    public static final int leftChannel = 0;
    public static final int rightChannel = 1;
    public static final int stereoChannel = 2;
    public static final String UseFilePickerString = "UseFilePicker";
    public static final String PlayerFragmentTag = "PlayerFragmentTag";
    public static final String CrudActionString = "CrudAction";
    public static final String AddActionString = "ADD";
    public static final String EditActionString = "EDIT";
    public static final String DeleteActionString = "DELETE";
    public static final String PlayActionString = "PLAY";

    public static String leftChannelString;
    public static String rightChannelString;
    public static String stereoChannelString;
    public static LinkedHashMap<Integer, String> audioChannelMap;
    public static LinkedHashMap<String, Integer> audioChannelReverseMap;

    public static Resources AppResources;
    public static Context AppContext;

    public static String googleAdMobBannerID = "";
    public static ShowingInterstitialAdsUtil InterstitialAd;

    private static FacebookInterstitialAds facebookAds;
    private static GoogleAdMobInterstitial googleInterstitialAd;

    @Override
    public void onCreate() {
        super.onCreate();

        AppResources = getResources();
        AppContext = getApplicationContext();

        leftChannelString = getString(R.string.leftChannelString);
        rightChannelString = getString(R.string.rightChannelString);
        stereoChannelString = getString(R.string.stereoChannelString);

        audioChannelMap = new LinkedHashMap<>();
        audioChannelMap.put(leftChannel, leftChannelString);
        audioChannelMap.put(rightChannel, rightChannelString);
        audioChannelMap.put(stereoChannel, stereoChannelString);

        audioChannelReverseMap = new LinkedHashMap<>();
        audioChannelReverseMap.put(leftChannelString, leftChannel);
        audioChannelReverseMap.put(rightChannelString, rightChannel);
        audioChannelReverseMap.put(stereoChannelString, stereoChannel);

        AudienceNetworkAds.initialize(this);
        String facebookPlacementID = new String("1712962715503258_1712963252169871");
        facebookAds = new FacebookInterstitialAds(AppContext, facebookPlacementID);

        // Google AdMob
        String googleAdMobAppID = getString(R.string.google_AdMobAppID);
        String googleAdMobInterstitialID = "ca-app-pub-8354869049759576/1418354889";
        googleAdMobBannerID = "ca-app-pub-8354869049759576/8267060571";
        MobileAds.initialize(AppContext, googleAdMobAppID);
        googleInterstitialAd = new GoogleAdMobInterstitial(AppContext, googleAdMobInterstitialID);

        InterstitialAd = new ShowingInterstitialAdsUtil(facebookAds, googleInterstitialAd);

        final Handler adHandler = new Handler(Looper.getMainLooper());
        final Runnable adRunnable = new Runnable() {
            @Override
            public void run() {
                adHandler.removeCallbacksAndMessages(null);
                googleInterstitialAd.loadAd(); // load first google ad
                facebookAds.loadAd();   // load first facebook ad
            }
        };
        adHandler.postDelayed(adRunnable, 1000);
    }
}
