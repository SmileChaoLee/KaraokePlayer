package com.smile.karaoke.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.tabs.TabLayout
import com.smile.karaoke.R
import com.smile.karaoke.SmileAppBase
import com.smile.karaoke.utilities.LogUtil
import com.smile.karaoke.utilities.CommonUtil
import com.smile.smilelibraries.show_banner_ads.SetBannerAdView
import com.smile.smilelibraries.utilities.ScreenUtil

class TablayoutFragment : Fragment() {


    companion object {
        private const val TAG : String = "TablayoutFragment"
    }

    interface TabFragmentFunc {
        fun setTabs(activity: FragmentActivity?, tabLayout: TabLayout, containerId: Int)
        fun becomeVisible(tabLayout: TabLayout)
        fun becomeInVisible()
    }

    private var tabFragmentFunc: TabFragmentFunc? = null

    private var toastTextSize: Float = 0f
    private var bannerLayoutForTab: LinearLayout? = null
    private var myBannerAdView: SetBannerAdView? = null
    var playTabLayout: TabLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        LogUtil.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
        activity?.let {
            if (it is TabFragmentFunc) tabFragmentFunc = it
            LogUtil.d(TAG, "onCreate.tabFragmentFunc = $tabFragmentFunc")
        }
        LogUtil.d(TAG, "onCreate.finished")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        LogUtil.d(TAG, "onCreateView")
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tablayout,
            container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        LogUtil.d(TAG, "onViewCreated")
        bannerLayoutForTab = view.findViewById(R.id.bannerLayoutForTab)
        activity?.let {actIt ->
            val textFontSize = ScreenUtil.getPxTextFontSizeNeeded(actIt)
            toastTextSize = textFontSize * 0.7f
            showBannerAd()
        }
        CommonUtil.setVisible(bannerLayoutForTab, View.GONE)

        playTabLayout = view.findViewById(R.id.fragmentsTabLayout)
        playTabLayout?.let {
            tabFragmentFunc?.setTabs(activity, it, R.id.tablayout_container)
        }

        LogUtil.d(TAG, "onViewCreated.finished")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        LogUtil.d(TAG, "onConfigurationChanged")
        super.onConfigurationChanged(newConfig)
        showBannerAd()
        CommonUtil.setVisible(bannerLayoutForTab, View.GONE)
    }

    override fun onStart() {
        LogUtil.d(TAG, "onStart")
        super.onStart()
    }

    override fun onResume() {
        LogUtil.d(TAG, "onResume")
        super.onResume()
        myBannerAdView?.resume()
        CommonUtil.setVisible(bannerLayoutForTab, View.GONE)
    }

    override fun onPause() {
        LogUtil.d(TAG, "onPause")
        super.onPause()
        myBannerAdView?.pause()
        bannerLayoutForTab?.visibility = View.GONE
    }

    override fun onStop() {
        LogUtil.d(TAG, "onStop")
        super.onStop()
    }

    override fun onDestroy() {
        LogUtil.d(TAG, "onDestroy")
        myBannerAdView?.destroy()
        super.onDestroy()
    }

    fun becomeVisible() {
        LogUtil.d(TAG, "becomeVisible")
        playTabLayout?.let {
            tabFragmentFunc?.becomeVisible(it)
        }
    }

    fun becomeInVisible() {
        LogUtil.d(TAG, "becomeInVisible")
        tabFragmentFunc?.becomeInVisible()
    }

    private fun showBannerAd() {
        LogUtil.d(TAG, "showBannerAd")
        activity?.let { actIt ->
            myBannerAdView?.destroy()
            myBannerAdView = (actIt.application as SmileAppBase)
                .showBannerAd(actIt, bannerLayoutForTab)
            LogUtil.d(TAG, "showBannerAd.myBannerAdView = $myBannerAdView")
            myBannerAdView?.showBannerAdView() // AdMob first
        }
    }
}