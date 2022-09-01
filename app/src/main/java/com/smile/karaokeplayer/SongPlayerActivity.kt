package com.smile.karaokeplayer

import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.smile.karaokeplayer.fragments.PlayerBaseViewFragment

private const val TAG = "SongPlayerActivity"

abstract class SongPlayerActivity : AppCompatActivity(), PlayerBaseViewFragment.PlayBaseFragmentFunc {

    private lateinit var playerFragment: PlayerBaseViewFragment

    abstract fun getFragment() : PlayerBaseViewFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate() is called")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)

        playerFragment = getFragment()
        supportFragmentManager.beginTransaction().apply {
            add(R.id.basePlayViewLayout, playerFragment)
            commit()
        }

        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG, "onBackPressedDispatcher.handleOnBackPressed")
                playerFragment.onBackPressed()
            }
        })
    }

    override fun baseHidePlayerView() {
        // do nothing
    }

    override fun baseShowPlayerView() {
        // do nothing
    }
}