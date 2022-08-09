package com.smile.karaokeplayer

import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

interface FragmentInterface {
    fun getFragment() : PlayerBaseViewFragment
}

private const val fragmentTag : String = "FragmentTag"
private const val TAG : String = "BaseActivity"

abstract class BaseActivity : AppCompatActivity(), FragmentInterface {
    private lateinit var playerFragment: PlayerBaseViewFragment
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

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG, "Back Key is Pressed()")
                playerFragment.onBackPressed()
            }
        })
    }
}