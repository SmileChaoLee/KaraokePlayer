package com.smile.u2bplayer

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

class U2bPlayerApp : SmileAppBase() {

    companion object {
        private const val TAG = "U2bPlayerApp"
    }

    override fun onCreate() {
        super.onCreate()
        LogUtil.i(TAG, "onCreate")
    }

    override fun initAds() {
        // for com.smile.youtubeplayer
        // adMobBannerID = "ca-app-pub-8354869049759576/1400953752"
        // adMobNativeID = "ca-app-pub-8354869049759576/6170064706"
        // for com.smile.u2bplayer
        adMobBannerID = "ca-app-pub-8354869049759576/7413877501"
        adMobNativeID = "ca-app-pub-8354869049759576/7222305817"
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
            adMobBannerID, "", 0
        )
    }

    override fun getInterstitial(): AdMobInterstitial? {
        // for com.smile.youtubeplayer
        // val adMobInterstitialID = "ca-app-pub-8354869049759576/7483146379"
        // for com.smile.u2bplayer
        // val adMobInterstitialID = "ca-app-pub-8354869049759576/5405649501"
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
