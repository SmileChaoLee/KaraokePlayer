package com.smile.karaokeplayer

import android.app.Activity
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.google.android.ads.nativetemplates.TemplateView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus
import com.smile.karaoke.SmileAppBase
import com.smile.karaoke.utilities.LogUtil
import com.smile.nativetemplates_models.GoogleAdMobNativeTemplate
import com.smile.smilelibraries.google_ads_util.AdMobInterstitial
import com.smile.smilelibraries.show_banner_ads.SetBannerAdView
import com.smile.smilelibraries.utilities.ScreenUtil
import com.smile.karaoke.BuildConfig

class SmileKaraokeApp : SmileAppBase() {

    override fun onCreate() {
        super.onCreate()
        LogUtil.i(TAG, "onCreate")
    }

    override fun initAds() {
        // AudienceNetworkAds.initialize(this)  // no need for mediation
        // facebookInterstitialID = "1712962715503258_1712963252169871";
        // facebookInterstitialID = testString + facebookInterstitialID;
        // facebookInterstitial = new FacebookInterstitial(appContext,
        //         facebookInterstitialID);
        // for debug mode and for facebook
        val testString = if (BuildConfig.DEBUG) "IMG_16_9_APP_INSTALL#" else ""
        facebookBannerID = testString + "1712962715503258_2019623008170559"
        // googleAdMobAppID = "ca-app-pub-8354869049759576~5549171584"
        adMobBannerID = "ca-app-pub-8354869049759576/8267060571"
        adMobNativeID = "ca-app-pub-8354869049759576/7985456524"
        // google
        MobileAds.initialize(applicationContext
        ) { initializationStatus: InitializationStatus? ->
            LogUtil.d(TAG, "Google AdMob was initialized successfully.")
        }
        // for the chrome cast
    }

    override fun showBannerAd(activity: Activity?, bannerLayout: LinearLayout?): SetBannerAdView? {
        LogUtil.d(TAG, "showBannerAd")
        val act = activity ?: return null
        // var bannerDpWidth = (ScreenUtil.getScreenSize(act).x * 0.98).toFloat()
        // bannerDpWidth = ScreenUtil.pixelToDp(bannerDpWidth)
        return SetBannerAdView(
            act, null,
            bannerLayout,
            adMobBannerID, facebookBannerID, 0
        )
    }

    override fun getInterstitial(): AdMobInterstitial {
        val adMobInterstitialID = "ca-app-pub-8354869049759576/1418354889"
        return AdMobInterstitial(applicationContext, adMobInterstitialID)
    }

    override fun getNativeTemplate(activity: Activity?, nativeLayout: FrameLayout?,
                                   nativeAdView: TemplateView?)
    : GoogleAdMobNativeTemplate {
        LogUtil.d(TAG, "getNativeTemplate")
        return GoogleAdMobNativeTemplate(
            activity,
            nativeLayout,
            adMobNativeID,
            nativeAdView
        )
    }

    companion object {
        private const val TAG = "SmileKaraokeApp"
    }
}