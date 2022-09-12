package com.smile.karaokeplayer.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.smile.karaokeplayer.BaseApplication
import com.smile.karaokeplayer.R
import com.smile.karaokeplayer.adapters.FragmentAdapter
import com.smile.smilelibraries.show_banner_ads.SetBannerAdView

private const val TAG : String = "TablayoutFragment"

class TablayoutFragment : Fragment() {

    companion object {
        const val OpenFragmentTag : String = "OPEN_FILES"
        const val FavoriteFragmentTag : String = "MY_FAVORITES"
    }

    private lateinit var openFragment: OpenFileFragment
    private lateinit var favoriteFragment: MyFavoritesFragment
    private lateinit var bannerLayoutForTab: LinearLayout
    private var myBannerAdView: SetBannerAdView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_tablayout, container, false)

        bannerLayoutForTab = view.findViewById(R.id.bannerLayoutForTab)
        myBannerAdView = SetBannerAdView(
                activity,null, bannerLayoutForTab, BaseApplication.googleAdMobBannerID,
                BaseApplication.facebookBannerID)
        myBannerAdView?.showBannerAdView()

        resources.configuration.orientation.let {
            if (it == Configuration.ORIENTATION_LANDSCAPE) bannerLayoutForTab.visibility = View.GONE
            else bannerLayoutForTab.visibility = View.VISIBLE
        }

        val playTabLayout: TabLayout = view.findViewById(R.id.fragmentsTabLayout)
        val playViewPager2: ViewPager2 = view.findViewById(R.id.fragmentsViewPager2)

        activity?.let {
            val fragmentAdapter = FragmentAdapter(it.supportFragmentManager, lifecycle)
            openFragment = OpenFileFragment()
            fragmentAdapter.addFragment(openFragment, OpenFragmentTag)
            favoriteFragment = MyFavoritesFragment()
            fragmentAdapter.addFragment(favoriteFragment, FavoriteFragmentTag)
            val tabText = arrayOf(getString(R.string.open_files), getString(R.string.my_favorites))
            playViewPager2.adapter = fragmentAdapter
            Log.d(TAG, "TabLayoutMediator.attach()")
            TabLayoutMediator(playTabLayout, playViewPager2) { tab, position ->
                tab.text = tabText[position]
            }.attach()
        }
        return view
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        newConfig.orientation.let {
            if (it == Configuration.ORIENTATION_LANDSCAPE) bannerLayoutForTab.visibility = View.GONE
            else bannerLayoutForTab.visibility = View.VISIBLE
        }
        super.onConfigurationChanged(newConfig)
    }

    override fun onResume() {
        Log.d(TAG, "onResume() is called.")
        myBannerAdView?.resume()
        super.onResume()
    }

    override fun onPause() {
        Log.d(TAG, "onPause() is called.")
        myBannerAdView?.pause()
        super.onPause()
    }
    override fun onDestroy() {
        Log.d(TAG, "onDestroy() is called.")
        myBannerAdView?.destroy()
        super.onDestroy()
    }
}