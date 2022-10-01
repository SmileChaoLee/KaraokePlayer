package com.smile.karaokeplayer.fragments

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.smile.karaokeplayer.BaseApplication
import com.smile.karaokeplayer.R
import com.smile.smilelibraries.show_banner_ads.SetBannerAdView
import com.smile.smilelibraries.utilities.ScreenUtil

private const val TAG : String = "TablayoutFragment"

class TablayoutFragment : Fragment() {

    companion object {
        const val OpenFragmentTag : String = "OPEN_FILES"
        const val FavoriteFragmentTag : String = "MY_FAVORITES"
    }

    // private lateinit var fragmentAdapter: FragmentAdapter
    private val openFragment = OpenFileFragment()
    private val favoriteFragment = MyFavoritesFragment()
    private lateinit var bannerLayoutForTab: LinearLayout
    private var myBannerAdView: SetBannerAdView? = null
    private lateinit var playTabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate() is called.")
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView()")

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_tablayout, container, false)

        bannerLayoutForTab = view.findViewById(R.id.bannerLayoutForTab)
        setMyBannerAdView(activity as Activity)

        resources.configuration.orientation.let {
            if (it == Configuration.ORIENTATION_LANDSCAPE) bannerLayoutForTab.visibility = View.GONE
            else bannerLayoutForTab.visibility = View.VISIBLE
        }

        playTabLayout = view.findViewById(R.id.fragmentsTabLayout)
        val tabText = arrayOf(getString(R.string.open_files), getString(R.string.my_favorites))
        playTabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    when (it.position) {
                        0-> {
                            Log.d(TAG, "OnTabSelectedListener.onTabSelected.position = 0")
                            activity?.supportFragmentManager?.beginTransaction()?.apply {
                                replace(R.id.tablayout_container, openFragment, OpenFragmentTag)
                                commit()
                            }
                        }
                        1-> {
                            Log.d(TAG, "OnTabSelectedListener.onTabSelected.position = 1")
                            activity?.supportFragmentManager?.beginTransaction()?.apply {
                                replace(R.id.tablayout_container, favoriteFragment, FavoriteFragmentTag)
                                commit()
                            }
                        }
                        else->{
                            Log.d(TAG, "OnTabSelectedListener.onTabSelected.others")
                        }
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                Log.d(TAG, "OnTabSelectedListener.onTabUnselected")
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                Log.d(TAG, "OnTabSelectedListener.onTabReselected")
            }

        })

        playTabLayout.addTab(playTabLayout.newTab().setText(tabText[0]), true)
        playTabLayout.addTab(playTabLayout.newTab().setText(tabText[1]))
        /*
        val playViewPager2: ViewPager2 = view.findViewById(R.id.fragmentsViewPager2)
        playViewPager2.adapter = fragmentAdapter
        Log.d(TAG, "TabLayoutMediator.attach()")
        TabLayoutMediator(playTabLayout, playViewPager2) { tab, position ->
            tab.text = tabText[position]
        }.attach()
        */

        return view
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        Log.d(TAG, "onConfigurationChanged()")
        super.onConfigurationChanged(newConfig)
        setMyBannerAdView(activity as Activity)
        newConfig.orientation.let {
            if (it == Configuration.ORIENTATION_LANDSCAPE) bannerLayoutForTab.visibility = View.GONE
            else bannerLayoutForTab.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        Log.d(TAG, "onResume()")
        super.onResume()
        myBannerAdView?.resume()
        resources.configuration.orientation.let {
            if (it == Configuration.ORIENTATION_LANDSCAPE) bannerLayoutForTab.visibility = View.GONE
            else bannerLayoutForTab.visibility = View.VISIBLE
        }
    }

    override fun onPause() {
        Log.d(TAG, "onPause()")
        super.onPause()
        myBannerAdView?.pause()
        bannerLayoutForTab.visibility = View.GONE
    }
    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        myBannerAdView?.destroy()
        super.onDestroy()
    }

    private fun setMyBannerAdView(activity: Activity) {
        val screen : Point = ScreenUtil.getScreenSize(activity)
        val adaptiveBannerDpWidth = ScreenUtil.pixelToDp(activity, screen.x)
        Log.d(TAG, "setMyBannerAdView().adaptiveBannerDpWidth = $adaptiveBannerDpWidth")
        myBannerAdView?.destroy()
        myBannerAdView = SetBannerAdView(
                activity,null, bannerLayoutForTab, BaseApplication.googleAdMobBannerID,
                BaseApplication.facebookBannerID, adaptiveBannerDpWidth)
        myBannerAdView?.showBannerAdView()
    }

    fun switchToOpenFileFragment() {
        Log.d(TAG, "switchToOpenFileFragment()")
        playTabLayout.selectTab(playTabLayout.getTabAt(0))
    }
}