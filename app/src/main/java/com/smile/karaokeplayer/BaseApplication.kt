package com.smile.karaokeplayer

import android.content.res.Configuration
import android.os.Handler
import androidx.multidex.MultiDexApplication
import com.smile.karaokeplayer.constants.CommonConstants
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener
import com.google.android.gms.ads.initialization.InitializationStatus
import com.smile.smilelibraries.google_admob_ads_util.GoogleAdMobInterstitial
import android.os.Looper
import android.util.Log
import com.smile.smilelibraries.showing_interstitial_ads_utility.ShowingInterstitialAdsUtil
import com.smile.smilelibraries.facebook_ads_util.FacebookInterstitialAds
import java.util.LinkedHashMap

abstract class BaseApplication : MultiDexApplication() {
    protected var facebookInterstitialID = ""
    @JvmField
    protected var googleAdMobAppID = ""
    @JvmField
    protected var googleAdMobInterstitialID = ""
    protected var testString = ""
    abstract fun setGoogleAdMobAndFacebookAudioNetwork()
    override fun onCreate() {
        super.onCreate()
        leftChannelString = getString(R.string.leftChannelString)
        rightChannelString = getString(R.string.rightChannelString)
        stereoChannelString = getString(R.string.stereoChannelString)
        audioChannelMap = LinkedHashMap()
        audioChannelMap!![CommonConstants.LeftChannel] = leftChannelString
        audioChannelMap!![CommonConstants.RightChannel] = rightChannelString
        audioChannelMap!![CommonConstants.StereoChannel] = stereoChannelString
        audioChannelReverseMap = LinkedHashMap()
        audioChannelReverseMap!![leftChannelString] = CommonConstants.LeftChannel
        audioChannelReverseMap!![rightChannelString] = CommonConstants.RightChannel
        audioChannelReverseMap!![stereoChannelString] = CommonConstants.StereoChannel

        // for debug mode and for facebook
        if (BuildConfig.DEBUG) {
            testString = "IMG_16_9_APP_INSTALL#"
        }
        setGoogleAdMobAndFacebookAudioNetwork()

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
        MobileAds.initialize(
            applicationContext,
            OnInitializationCompleteListener { initializationStatus: InitializationStatus? ->
                Log.d(TAG, "Google AdMob was initialized successfully. $initializationStatus")
            })
        googleInterstitialAd = GoogleAdMobInterstitial(applicationContext, googleAdMobInterstitialID)
        val adHandler = Handler(Looper.getMainLooper())
        val adRunnable = Runnable {
            adHandler.removeCallbacksAndMessages(null)
            if (googleInterstitialAd != null) {
                googleInterstitialAd!!.loadAd() // load first google ad
            }
            if (facebookAds != null) {
                facebookAds!!.loadAd() // load first facebook ad
            }
        }
        adHandler.postDelayed(adRunnable, 1000)
    }

    companion object {
        private const val TAG = "BaseApplication"
        var leftChannelString: String? = null
        var rightChannelString: String? = null
        var stereoChannelString: String? = null
        @JvmField
        var audioChannelMap: LinkedHashMap<Int, String?>? = null
        @JvmField
        var audioChannelReverseMap: LinkedHashMap<String?, Int>? = null
        @JvmField
        var InterstitialAd: ShowingInterstitialAdsUtil? = null
        @JvmField
        var facebookBannerID = ""
        @JvmField
        var googleAdMobBannerID = ""
        @JvmField
        var googleAdMobNativeID = ""
        @JvmField
        var AdProvider = ShowingInterstitialAdsUtil.FacebookAdProvider // default is Facebook Ad
        @JvmField
        var facebookAds: FacebookInterstitialAds? = null
        @JvmField
        var googleInterstitialAd: GoogleAdMobInterstitial? = null
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "Configuration changed")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "System is running low on memory")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.w(TAG, "onTrimMemory, level: $level")
    }
}