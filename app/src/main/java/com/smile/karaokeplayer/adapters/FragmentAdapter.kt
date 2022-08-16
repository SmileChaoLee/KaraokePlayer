package com.smile.karaokeplayer.adapters

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

private const val TAG : String = "FragmentAdapter"

class FragmentAdapter(fm : FragmentManager, lifeCycle : Lifecycle) :
    FragmentStateAdapter(fm, lifeCycle) {

    private var fragmentList : ArrayList<Fragment> = ArrayList()
    private var fragmentTags : ArrayList<String> = ArrayList()

    override fun getItemCount(): Int {
        Log.d(TAG, "getItemCount")
        return fragmentList.size
    }

    override fun createFragment(position: Int): Fragment {
        Log.d(TAG, "createFragment.position = $position")
        return fragmentList[position]
    }

    fun addFragment(fragment : Fragment, tag : String) {
        Log.d(TAG, "addFragment")
        fragmentList.add(fragment)
        fragmentTags.add(tag)
    }

    fun getTag(position : Int) : String{
        Log.d(TAG, "getTag.position = $position")
        return fragmentTags[position]
    }
}