package com.smile.karaokeplayer.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class FragmentAdapter(fm : FragmentManager, lifeCycle : Lifecycle) :
    FragmentStateAdapter(fm, lifeCycle) {

    private var fragmentList : ArrayList<Fragment> = ArrayList()
    private var fragmentTitles : ArrayList<String> = ArrayList()

    override fun getItemCount(): Int {
        return fragmentList.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragmentList[position]
    }

    fun addFragment(fragment : Fragment, title : String) {
        fragmentList.add(fragment)
        fragmentTitles.add(title)
    }

    fun getTitle(position : Int) : String{
        return fragmentTitles[position]
    }
}