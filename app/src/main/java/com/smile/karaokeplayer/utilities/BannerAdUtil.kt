package com.smile.karaokeplayer.utilities

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Point
import android.util.Log
import android.widget.LinearLayout
import com.smile.karaokeplayer.BaseApplication
import com.smile.smilelibraries.show_banner_ads.SetBannerAdView
import com.smile.smilelibraries.utilities.ScreenUtil

private const val TAG : String = "BannerAdUtil"

object BannerAdUtil {
    fun getBannerAdView(activity: Activity, comLayout : LinearLayout?, layout : LinearLayout, orientation: Int)
            : SetBannerAdView {
        val screen : Point = ScreenUtil.getScreenSize(activity)
        val adaptiveBannerDpWidth = if (orientation == Configuration.ORIENTATION_PORTRAIT)
            ScreenUtil.pixelToDp(activity, screen.x)
        else ScreenUtil.pixelToDp(activity, screen.x) * 0.7
        Log.d(TAG, "setMyBannerAdView().adaptiveBannerDpWidth = $adaptiveBannerDpWidth")
        return SetBannerAdView(activity,comLayout, layout, BaseApplication.googleAdMobBannerID,
                BaseApplication.facebookBannerID, adaptiveBannerDpWidth.toInt())
    }
}