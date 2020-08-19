package com.smile.karaokeplayer;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.multidex.MultiDexApplication;

import com.facebook.ads.AudienceNetworkAds;
import com.google.android.gms.ads.MobileAds;
import com.smile.karaokeplayer.Constants.CommonConstants;
import com.smile.smilelibraries.facebook_ads_util.FacebookInterstitialAds;
import com.smile.smilelibraries.google_admob_ads_util.GoogleAdMobInterstitial;
import com.smile.smilelibraries.showing_instertitial_ads_utility.ShowingInterstitialAdsUtil;

import java.util.LinkedHashMap;

public class SmileApplication extends MultiDexApplication {

    private static final String TAG = new String("SmileApplication");

    public static final int maxNumOfPlayedSongsBeforeAd = 3;  // show ad after playing 3 songs
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
    public static String googleAdMobNativeID = "";
    public static int AdProvider = ShowingInterstitialAdsUtil.FacebookAdProvider;    // default is Facebook Ad

    public static FacebookInterstitialAds facebookAds;
    public static GoogleAdMobInterstitial googleInterstitialAd;

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

        // String facebookInterstitialID = new String("1712962715503258_1712963252169871");
        // facebookBannerID = "1712962715503258_2019623008170559";
        // String googleAdMobInterstitialID = "ca-app-pub-8354869049759576/1418354889";
        // googleAdMobBannerID = "ca-app-pub-8354869049759576/8267060571";

        String facebookInterstitialID = "";
        String googleAdMobAppID = "";
        String googleAdMobInterstitialID = "";
        switch (BuildConfig.APPLICATION_ID) {
            case "com.smile.karaokeplayer":
                facebookInterstitialID = "1712962715503258_1712963252169871";
                facebookBannerID = "1712962715503258_2019623008170559";
                googleAdMobAppID = "ca-app-pub-8354869049759576~5549171584";
                googleAdMobInterstitialID = "ca-app-pub-8354869049759576/1418354889";
                googleAdMobBannerID = "ca-app-pub-8354869049759576/8267060571";
                googleAdMobNativeID = "ca-app-pub-8354869049759576/7985456524";
                Log.d(TAG, "BuildConfig.APPLICATION_ID = " + BuildConfig.APPLICATION_ID);
                break;
            case "com.smile.videoplayer":
                // Token: EAAEN04aiEXUBAHBQwZBZB4gmWOueuRTEZCHMCAWOHZBB7hjavIuXgKELNvtfcIGCJV72zkohipkSZALG51WCXX6xbN3oUy84i8NrIvuc44RTfypgdcyOTnaPyM1W4JZBjQvDGuWsCRGmbusWaZCKmWt5iCkhQklOKeZC4Edx6FDIGTUUiHMKURzS
                // App ID: 633653050588487
                facebookInterstitialID = "296677124739445_296678328072658";
                facebookBannerID = "296677124739445_296687284738429";
                googleAdMobAppID = "ca-app-pub-8354869049759576~5376732060";
                googleAdMobInterstitialID = "ca-app-pub-8354869049759576/7715939032";
                googleAdMobBannerID = "ca-app-pub-8354869049759576/2158051096";
                googleAdMobNativeID = "ca-app-pub-8354869049759576/6498242044";
                Log.d(TAG, "BuildConfig.APPLICATION_ID = " + BuildConfig.APPLICATION_ID);
                break;
            case "com.smile.musicplayer":
                // Token: EAAFARZAONXXYBAK09XFusSFgrK4un2ykcXGG3xjwlRaHZCUzr5ZAJIM6mz2ZC7DcKQAGWMFtinkg9p6BeV41YB7Ttr8m90YJu8mhUZCsJnmdffp8uNGiNZC73rErjkHZBS2tWn2mZBOCHf96AYlhZCqfki10UKI5uZBqDX8LQPwmqrEZBku5QLLEGHQ
                // App ID: 3006486769578153
                facebookInterstitialID = "352142549146998_352142985813621";
                facebookBannerID = "352142549146998_352146195813300";
                googleAdMobAppID = "ca-app-pub-8354869049759576~3279561070";
                googleAdMobInterstitialID = "ca-app-pub-8354869049759576/1888913198";
                googleAdMobBannerID = "ca-app-pub-8354869049759576/1966479405";
                googleAdMobNativeID = "ca-app-pub-8354869049759576/6835662705";
                Log.d(TAG, "BuildConfig.APPLICATION_ID = " + BuildConfig.APPLICATION_ID);
                break;
        }

        AudienceNetworkAds.initialize(this);
        String testString = "";
        // for debug mode
        if (BuildConfig.DEBUG) {
            testString = "IMG_16_9_APP_INSTALL#";
        }

        // facebook
        facebookInterstitialID = testString + facebookInterstitialID;
        facebookAds = new FacebookInterstitialAds(AppContext, facebookInterstitialID);

        // google
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
