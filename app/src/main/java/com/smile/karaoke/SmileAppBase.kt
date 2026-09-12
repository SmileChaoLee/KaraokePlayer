package com.smile.karaoke

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Color
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.multidex.MultiDexApplication
import com.google.android.ads.nativetemplates.TemplateView
import com.google.android.gms.cast.framework.CastContext
import com.smile.karaoke.constants.CommonConstants
import com.smile.karaoke.chromecast.InitCastContext
import com.smile.karaoke.utilities.LogUtil
import com.smile.nativetemplates_models.GoogleAdMobNativeTemplate
import com.smile.smilelibraries.google_ads_util.AdMobInterstitial
import com.smile.smilelibraries.show_banner_ads.SetBannerAdView

abstract class SmileAppBase : MultiDexApplication() {

    companion object {
        private const val TAG = "SmileAppBase"
        @JvmField
        val accentColor = Color.rgb(0xFF, 0x40, 0x81)
        @JvmField
        val transparentLightGray =
            Color.argb(0x33, 0xd5, 0xd5, 0xd5) //Color(0x33D5D5D5)
        @JvmField
        val transparent =
            Color.argb(0x0, 0x0, 0x0, 0x0) //Color(0x00000000)
        @JvmField
        val audioChannelMap = LinkedHashMap<Int, String>()
        @JvmField
        val audioChannelReverseMap = LinkedHashMap<String, Int>()
        var facebookBannerID = ""
        var adMobBannerID = ""
        var adMobNativeID = ""
    }

    var leftChannelString = ""
    var rightChannelString = ""
    var stereoChannelString = ""
    var castContext: CastContext? = null

    abstract fun initAds()
    abstract fun showBannerAd(activity: Activity?, bannerLayout: LinearLayout?)
    : SetBannerAdView?
    abstract fun getInterstitial(): AdMobInterstitial?
    abstract fun getNativeTemplate(activity: Activity?, nativeLayout: FrameLayout?,
                                   nativeAdView: TemplateView?)
    : GoogleAdMobNativeTemplate?

    override fun onCreate() {
        super.onCreate()
        LogUtil.d(TAG, "onCreate")
        leftChannelString = getString(R.string.leftChannelString)
        rightChannelString = getString(R.string.rightChannelString)
        stereoChannelString = getString(R.string.stereoChannelString)
        audioChannelMap[CommonConstants.LEFT_CHANNEL] = leftChannelString
        audioChannelMap[CommonConstants.RIGHT_CHANNEL] = rightChannelString
        audioChannelMap[CommonConstants.STEREO] = stereoChannelString
        audioChannelReverseMap[leftChannelString] = CommonConstants.LEFT_CHANNEL
        audioChannelReverseMap[rightChannelString] = CommonConstants.RIGHT_CHANNEL
        audioChannelReverseMap[stereoChannelString] = CommonConstants.STEREO

        initAds()

        castContext = InitCastContext.getInstance(this)
        LogUtil.d(TAG, "castContext = $castContext")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LogUtil.d(TAG, "Configuration changed")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        LogUtil.w(TAG, "System is running low on memory")
    }

    override fun onTerminate() {
        super.onTerminate()
        castContext = null
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        LogUtil.w(TAG, "onTrimMemory, level: = $level")
    }
}
