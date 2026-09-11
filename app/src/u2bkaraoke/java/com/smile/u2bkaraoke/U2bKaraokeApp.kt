package com.smile.u2bkaraoke

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
import com.smile.u2bkaraoke.dagger.interfaces.DaggerU2bKaOkComponent

class U2bKaraokeApp : SmileAppBase() {
    companion object {
        private const val TAG = "U2bKaraokeApp"
        val appCompBuilder = DaggerU2bKaOkComponent.builder()!!
        val appComponent = appCompBuilder.build()
    }

    override fun onCreate() {
        LogUtil.d(TAG, "onCreate")
        super.onCreate()
    }

    override fun initAds() {
        adMobBannerID = "ca-app-pub-8354869049759576/6589441492"
        adMobNativeID = "ca-app-pub-8354869049759576/6259620074"
        // google
        MobileAds.initialize(applicationContext) {
                initializationStatus: InitializationStatus? ->
            LogUtil.i(TAG, "Google AdMob was initialized successfully.")
        }
    }

    override fun showBannerAd(activity: Activity?, bannerLayout: LinearLayout?): SetBannerAdView? {
        LogUtil.d(TAG, "showBannerAd")
        val act = activity ?: return null
        // var bannerDpWidth = (ScreenUtil.getScreenSize(act).x * 0.98).toFloat()
        // bannerDpWidth = ScreenUtil.pixelToDp(bannerDpWidth)
        return SetBannerAdView(act, null,
            bannerLayout,
            adMobBannerID, facebookBannerID, 0
        )
    }

    override fun getInterstitial(): AdMobInterstitial? {
        // val adMobInterstitialID = "ca-app-pub-8354869049759576/1573096143"
        // return AdMobInterstitial(applicationContext, adMobInterstitialID)
        return null
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