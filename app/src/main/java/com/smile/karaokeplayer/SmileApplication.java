package com.smile.karaokeplayer;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.multidex.MultiDexApplication;

import com.facebook.ads.AudienceNetworkAds;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.cast.framework.CastState;
import com.smile.karaokeplayer.Constants.CommonConstants;
import com.smile.smilelibraries.facebook_ads_util.FacebookInterstitialAds;
import com.smile.smilelibraries.google_admob_ads_util.GoogleAdMobInterstitial;
import com.smile.smilelibraries.showing_instertitial_ads_utility.ShowingInterstitialAdsUtil;

import java.util.LinkedHashMap;

public class SmileApplication extends MultiDexApplication {

    private static final String TAG = new String("SmileApplication");

    public static String leftChannelString;
    public static String rightChannelString;
    public static String stereoChannelString;
    public static LinkedHashMap<Integer, String> audioChannelMap;
    public static LinkedHashMap<String, Integer> audioChannelReverseMap;

    public static Resources AppResources;
    public static Context AppContext;


    public static ShowingInterstitialAdsUtil InterstitialAd;
    public static String facebookBannerID = "";
    public static String googleAdMobBannerID = "";
    public static int AdProvider = ShowingInterstitialAdsUtil.FacebookAdProvider;    // default is Facebook Ad

    public static FacebookInterstitialAds facebookAds;
    public static GoogleAdMobInterstitial googleInterstitialAd;

    public static int currentCastState = CastState.NO_DEVICES_AVAILABLE;

    @Override
    public void onCreate() {
        super.onCreate();

        AppResources = getResources();
        AppContext = getApplicationContext();

        leftChannelString = getString(R.string.leftChannelString);
        rightChannelString = getString(R.string.rightChannelString);
        stereoChannelString = getString(R.string.stereoChannelString);

        audioChannelMap = new LinkedHashMap<>();
        audioChannelMap.put(CommonConstants.LeftChannel, leftChannelString);
        audioChannelMap.put(CommonConstants.RightChannel, rightChannelString);
        audioChannelMap.put(CommonConstants.StereoChannel, stereoChannelString);

        audioChannelReverseMap = new LinkedHashMap<>();
        audioChannelReverseMap.put(leftChannelString, CommonConstants.LeftChannel);
        audioChannelReverseMap.put(rightChannelString, CommonConstants.RightChannel);
        audioChannelReverseMap.put(stereoChannelString, CommonConstants.StereoChannel);

        AudienceNetworkAds.initialize(this);
        String facebookInterstitialID = new String("1712962715503258_1712963252169871");
        String testString = "";
        // for debug mode
        if (com.smile.karaokeplayer.BuildConfig.DEBUG) {
            testString = "IMG_16_9_APP_INSTALL#";
        }
        facebookInterstitialID = testString + facebookInterstitialID;
        //
        facebookAds = new FacebookInterstitialAds(AppContext, facebookInterstitialID);
        facebookBannerID = "1712962715503258_2019623008170559";

        // Google AdMob
        String googleAdMobAppID = getString(R.string.google_AdMobAppID);
        String googleAdMobInterstitialID = "ca-app-pub-8354869049759576/1418354889";
        googleAdMobBannerID = "ca-app-pub-8354869049759576/8267060571";
        MobileAds.initialize(AppContext, initializationStatus -> Log.d(TAG, "Google AdMob was initialized successfully."));

        googleInterstitialAd = new GoogleAdMobInterstitial(AppContext, googleAdMobInterstitialID);

        final Handler adHandler = new Handler(Looper.getMainLooper());
        final Runnable adRunnable = new Runnable() {
            @Override
            public void run() {
                adHandler.removeCallbacksAndMessages(null);
                if (googleInterstitialAd != null) {
                    googleInterstitialAd.loadAd(); // load first google ad
                }
                if (facebookAds != null) {
                    facebookAds.loadAd();   // load first facebook ad
                }
            }
        };
        adHandler.postDelayed(adRunnable, 1000);
    }
}
