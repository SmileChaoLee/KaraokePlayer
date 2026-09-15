package com.smile.videoplayer

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

class SmileVideoApp : SmileAppBase() {

    companion object {
        private const val TAG = "SmileVideoApp"
    }

    override fun onCreate() {
        super.onCreate()
        LogUtil.i(TAG, "onCreate")
    }

    override fun initAds() {
        // AudienceNetworkAds.initialize(this)  // no need for mediation
        // Token: EAAEN04aiEXUBAHBQwZBZB4gmWOueuRTEZCHMCAWOHZBB7hjavIuXgKELNvtfcIGCJV72zkohipkSZALG51WCXX6xbN3oUy84i8NrIvuc44RTfypgdcyOTnaPyM1W4JZBjQvDGuWsCRGmbusWaZCKmWt5iCkhQklOKeZC4Edx6FDIGTUUiHMKURzS
        // App ID: 633653050588487
        // No facebook ads for video player
        // facebookInterstitialID = "296677124739445_296678328072658";
        val testString = if (BuildConfig.DEBUG) "IMG_16_9_APP_INSTALL#" else ""
        facebookBannerID = testString + "296677124739445_296687284738429";
        // googleAdMobAppID = "ca-app-pub-8354869049759576~5376732060"
        adMobBannerID = "ca-app-pub-8354869049759576/2158051096"
        adMobNativeID = "ca-app-pub-8354869049759576/6498242044"
        // google
        MobileAds.initialize(applicationContext
        ) { initializationStatus: InitializationStatus? ->
            LogUtil.i(TAG, "Google AdMob was initialized successfully.")
        }
        // for the chrome cast
    }

    override fun showBannerAd(activity: Activity?, bannerLayout: LinearLayout?): SetBannerAdView? {
        LogUtil.d(TAG, "showBannerAd")
        val act = activity ?: return null
        // var bannerDpWidth = (ScreenUtil.getScreenSize(act).x * 0.98).toFloat()
        // bannerDpWidth = ScreenUtil.pixelToDp(bannerDpWidth)
        return SetBannerAdView(act, null,
            bannerLayout,
            adMobBannerID, "", 0
        )
    }

    override fun getInterstitial(): AdMobInterstitial? {
        val adMobInterstitialID = "ca-app-pub-8354869049759576/7715939032"
        return AdMobInterstitial(applicationContext, adMobInterstitialID)
    }

    override fun getNativeTemplate(activity: Activity?, nativeLayout: FrameLayout?,
                                  nativeAdView: TemplateView?)
            : GoogleAdMobNativeTemplate? {
        LogUtil.i(TAG, "getNativeTemplate")
        return GoogleAdMobNativeTemplate(activity,
            nativeLayout,
            adMobNativeID,
            nativeAdView)
    }
}
