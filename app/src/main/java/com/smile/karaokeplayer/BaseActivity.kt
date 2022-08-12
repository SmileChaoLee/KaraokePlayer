package com.smile.karaokeplayer

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.smile.karaokeplayer.adapters.FragmentAdapter
import com.smile.karaokeplayer.fragments.MyFavoriteFragment
import com.smile.karaokeplayer.fragments.MyListFragment
import com.smile.karaokeplayer.fragments.PlayerBaseViewFragment

interface FragmentInterface {
    fun getFragment() : PlayerBaseViewFragment
}

private const val fragmentTag : String = "FragmentTag"
private const val TAG : String = "BaseActivity"

abstract class BaseActivity : AppCompatActivity(), FragmentInterface {
    private lateinit var playerFragment: PlayerBaseViewFragment
    private lateinit var basePlayViewFragmentLlayout : LinearLayout
    private lateinit var linearLayoutForTabLayout: LinearLayout
    private var isHide : Boolean = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)

        basePlayViewFragmentLlayout = findViewById(R.id.basePlayViewFragmentLlayout)
        playerFragment = getFragment()
        val fmManager = supportFragmentManager
        val ft = fmManager.beginTransaction()
        val curFragment = fmManager.findFragmentByTag(fragmentTag)
        if (curFragment != null) {
            ft.add(R.id.basePlayViewFragmentLlayout, playerFragment, fragmentTag)
        } else {
            ft.replace(R.id.basePlayViewFragmentLlayout, playerFragment, fragmentTag)
        }
        ft.commit()
        basePlayViewFragmentLlayout.visibility = View.GONE
        isHide = true

        linearLayoutForTabLayout = findViewById(R.id.linearLayoutForTabLayout)
        var playTablayout : TabLayout = findViewById(R.id.playTabLayout)
        val playViewPager2 : ViewPager2 = findViewById(R.id.playViewPager2)
        val fragmentStateAdapter = FragmentAdapter(supportFragmentManager, lifecycle)

        val listFragment = MyListFragment();
        fragmentStateAdapter.addFragment(listFragment, "My List")
        val favoriteFragment = MyFavoriteFragment()
        fragmentStateAdapter.addFragment(favoriteFragment, "My Favorites")

        playViewPager2.adapter = fragmentStateAdapter
        TabLayoutMediator(playTablayout, playViewPager2) { tab, position ->
            tab.text = fragmentStateAdapter.getTitle(position)
        }.attach()

        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG, "onBackPressedDispatcher.handleOnBackPressed")
                playerFragment.onBackPressed()

                if (isHide) {
                    basePlayViewFragmentLlayout.visibility = View.VISIBLE
                    linearLayoutForTabLayout.visibility = View.GONE
                } else {
                    basePlayViewFragmentLlayout.visibility = View.GONE
                    linearLayoutForTabLayout.visibility = View.VISIBLE
                }
                isHide = !isHide
            }
        })
    }
}