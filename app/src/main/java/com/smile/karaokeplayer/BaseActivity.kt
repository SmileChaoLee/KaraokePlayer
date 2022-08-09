package com.smile.karaokeplayer

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

interface FragmentInterface {
    fun getFragment() : Fragment
    fun onBackPressed()
}

private const val fragmentTag : String = "FragmentTag"
private const val TAG : String = "BaseActivity"

abstract class BaseActivity : AppCompatActivity(), FragmentInterface {
    protected lateinit var playerFragment: Fragment
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)
        playerFragment = getFragment()
        val fmManager = supportFragmentManager
        val ft = fmManager.beginTransaction()
        val curFragment = fmManager.findFragmentByTag(fragmentTag)
        if (curFragment != null) {
            ft.add(R.id.activity_base_layout, playerFragment, fragmentTag)
        } else {
            ft.replace(R.id.activity_base_layout, playerFragment, fragmentTag)
        }
        ft.commit()
    }

    override fun onBackPressed() {
        Log.d(TAG, "onBackPressed() is called")
        onBackPressed()
    }
}