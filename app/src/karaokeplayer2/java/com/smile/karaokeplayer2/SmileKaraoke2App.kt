package com.smile.karaokeplayer2

import android.app.Activity
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.google.android.ads.nativetemplates.TemplateView
import com.smile.karaoke.SmileAppBase
import com.smile.karaoke.utilities.LogUtil
import com.smile.nativetemplates_models.GoogleAdMobNativeTemplate
import com.smile.smilelibraries.google_ads_util.AdMobInterstitial
import com.smile.smilelibraries.show_banner_ads.SetBannerAdView

class SmileKaraoke2App : SmileAppBase() {

    companion object {
        private const val TAG = "SmileKaraoke2App"
        private const val NATIVE_AD_ID = ""
    }

    override fun onCreate() {
        super.onCreate()
        LogUtil.d(TAG, "onCreate")
    }

    override fun initAds() {
        // No ads in this version
        LogUtil.d(TAG, "initAds.do nothing")
    }

    override fun showBannerAd(activity: Activity?, bannerLayout: LinearLayout?): SetBannerAdView? {
        LogUtil.d(TAG, "showBannerAd.do nothing")
        return null // No ads in this version
    }

    override fun getInterstitial(): AdMobInterstitial? {
        return null // No ads in this version
    }

    override fun getNativeTemplate(activity: Activity?, nativeLayout: FrameLayout?,
                                  nativeAdView: TemplateView?)
            : GoogleAdMobNativeTemplate? {
        LogUtil.d(TAG, "getNativeTemplate.do nothing")
        return null // No ads in this version
    }
}
