package com.smile.u2bplayer2

import android.app.Activity
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.google.android.ads.nativetemplates.TemplateView
import com.smile.karaoke.SmileAppBase
import com.smile.karaoke.utilities.LogUtil
import com.smile.nativetemplates_models.GoogleAdMobNativeTemplate
import com.smile.smilelibraries.google_ads_util.AdMobInterstitial
import com.smile.smilelibraries.show_banner_ads.SetBannerAdView

class U2bPlayer2App : SmileAppBase() {

    companion object {
        private const val TAG = "U2bPlayer2App"
    }

    override fun onCreate() {
        super.onCreate()
        LogUtil.i(TAG, "onCreate")
    }

    override fun initAds() {
        // No ads in this version
        LogUtil.d(TAG, "initAds.do nothing")
    }

    override fun showBannerAd(activity: Activity?, bannerLayout: LinearLayout?): SetBannerAdView? {
        LogUtil.d(TAG, "showBannerAd.do nothing")
        return null
    }

    override fun getInterstitial(): AdMobInterstitial? {
        // for com.smile.youtubeplayer
        // val adMobInterstitialID = "ca-app-pub-8354869049759576/7483146379"
        // for com.smile.u2bplayer
        return null
    }

    override fun getNativeTemplate(activity: Activity?, nativeLayout: FrameLayout?,
                                  nativeAdView: TemplateView?)
            : GoogleAdMobNativeTemplate? {
        LogUtil.i(TAG, "getNativeTemplate")
        return null
    }
}
