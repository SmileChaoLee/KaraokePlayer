package com.smile.karaokeplayer;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.multidex.MultiDexApplication;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.common.util.VisibleForTesting;
import com.smile.karaokeplayer.constants.CommonConstants;
import com.smile.smilelibraries.facebook_ads_util.FacebookInterstitial;
import com.smile.smilelibraries.google_ads_util.AdMobInterstitial;
import com.smile.smilelibraries.utilities.ScreenUtil;

import java.util.LinkedHashMap;

public abstract class BaseApplication extends MultiDexApplication {

    private static final String TAG = "BaseApplication";

    protected String facebookInterstitialID = "";
    protected String googleAdMobAppID = "";
    protected String googleAdMobInterstitialID = "";
    protected String testString = "";

    public static int FontSize_Scale_Type = ScreenUtil.FontSize_Pixel_Type;
    public static String leftChannelString;
    public static String rightChannelString;
    public static String stereoChannelString;
    public static LinkedHashMap<Integer, String> audioChannelMap;
    public static LinkedHashMap<String, Integer> audioChannelReverseMap;

    public static Resources AppResources;
    public static Context AppContext;
    public static String facebookBannerID = "";
    public static String googleAdMobBannerID = "";
    public static String googleAdMobNativeID = "";
    public FacebookInterstitial facebookInterstitial;
    public AdMobInterstitial adMobInterstitial;

    public abstract void setGoogleAdMobAndFacebookAudioNetwork();

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

        // for debug mode and for facebook
        if (BuildConfig.DEBUG) {
            testString = "IMG_16_9_APP_INSTALL#";
        }

        setGoogleAdMobAndFacebookAudioNetwork();

        /*
        switch (com.smile.karaokeplayer.BuildConfig.APPLICATION_ID) {
            case "com.smile.karaokeplayer":
                AudienceNetworkAds.initialize(this);
                facebookInterstitialID = "1712962715503258_1712963252169871";
                facebookInterstitialID = testString + facebookInterstitialID;
                facebookAds = new FacebookInterstitialAds(AppContext, facebookInterstitialID);
                facebookBannerID = testString + "1712962715503258_2019623008170559";
                googleAdMobAppID = "ca-app-pub-8354869049759576~5549171584";
                googleAdMobInterstitialID = "ca-app-pub-8354869049759576/1418354889";
                googleAdMobBannerID = "ca-app-pub-8354869049759576/8267060571";
                googleAdMobNativeID = "ca-app-pub-8354869049759576/7985456524";
                break;
            case "com.smile.videoplayer":
                // Token: EAAEN04aiEXUBAHBQwZBZB4gmWOueuRTEZCHMCAWOHZBB7hjavIuXgKELNvtfcIGCJV72zkohipkSZALG51WCXX6xbN3oUy84i8NrIvuc44RTfypgdcyOTnaPyM1W4JZBjQvDGuWsCRGmbusWaZCKmWt5iCkhQklOKeZC4Edx6FDIGTUUiHMKURzS
                // App ID: 633653050588487
                // facebookInterstitialID = "296677124739445_296678328072658";
                // facebookBannerID = testString + "296677124739445_296687284738429";
                googleAdMobAppID = "ca-app-pub-8354869049759576~5376732060";
                googleAdMobInterstitialID = "ca-app-pub-8354869049759576/7715939032";
                googleAdMobBannerID = "ca-app-pub-8354869049759576/2158051096";
                googleAdMobNativeID = "ca-app-pub-8354869049759576/6498242044";
                AdProvider = ShowingInterstitialAdsUtil.GoogleAdMobAdProvider;
                break;
        }
        */

        // facebook
        // only for com.smile.karaokeplayer
        // facebookInterstitialID = testString + facebookInterstitialID;
        // facebookAds = new FacebookInterstitialAds(AppContext, facebookInterstitialID);

        // google
        MobileAds.initialize(AppContext, initializationStatus -> Log.d(TAG, "Google AdMob was initialized successfully."));
        adMobInterstitial = new AdMobInterstitial(AppContext, googleAdMobInterstitialID);

        final Handler adHandler = new Handler(Looper.getMainLooper());
        @VisibleForTesting
        final Runnable adRunnable = () -> {
            adHandler.removeCallbacksAndMessages(null);
            if (adMobInterstitial != null) {
                adMobInterstitial.loadAd(); // load first google ad
            }
            if (facebookInterstitial != null) {
                facebookInterstitial.loadAd();   // load first facebook ad
            }
        };
        adHandler.postDelayed(adRunnable, 1000);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "Configuration changed");
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.w(TAG, "System is running low on memory");
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Log.w(TAG, "onTrimMemory, level: = " + level);
    }
}
