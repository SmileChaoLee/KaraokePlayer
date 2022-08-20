package com.smile.karaokeplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.smile.karaokeplayer.fragments.PlayerBaseViewFragment
import com.smile.karaokeplayer.fragments.TablayoutFragment
import com.smile.karaokeplayer.interfaces.FragmentInterface

private const val TAG : String = "BaseActivity"

abstract class BaseActivity : AppCompatActivity(), FragmentInterface {
    lateinit var playerFragment: PlayerBaseViewFragment
    private lateinit var tablayoutFragment : TablayoutFragment
    private lateinit var basePlayViewLayout : LinearLayout
    private lateinit var tablayoutViewLayout: LinearLayout
    private lateinit var baseReceiver : BroadcastReceiver
    // private var previousPlayParentView : ViewGroup? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)

        basePlayViewLayout = findViewById(R.id.basePlayViewLayout)
        tablayoutViewLayout = findViewById(R.id.tablayoutViewLayout)

        playerFragment = getFragment()
        tablayoutFragment = TablayoutFragment()
        supportFragmentManager.beginTransaction().apply {
            add(R.id.basePlayViewLayout, playerFragment)
            add(R.id.tablayoutViewLayout, tablayoutFragment)
            commit()
        }

        // setting BroadcastReceiver
        object: BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d(TAG, "intent?.action = ${intent?.action}")
                when (intent?.action) {
                    PlayerBaseViewFragment.Hide_PlayerView -> {
                        tablayoutViewLayout.visibility = View.VISIBLE
                        Log.d(TAG, "tablayoutViewLayout.visibility = View.VISIBLE")
                    }
                    PlayerBaseViewFragment.Show_PlayerView -> {
                        tablayoutViewLayout.visibility = View.GONE
                        Log.d(TAG, "tablayoutViewLayout.visibility = View.GONE")
                    }
                }
            }
        }.also { baseReceiver = it }

        LocalBroadcastManager.getInstance(this).apply {
            registerReceiver(baseReceiver, IntentFilter().apply {
                addAction(PlayerBaseViewFragment.Hide_PlayerView)
                addAction(PlayerBaseViewFragment.Show_PlayerView)
            })
        }
        //

        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG, "onBackPressedDispatcher.handleOnBackPressed")
                playerFragment.onBackPressed()
            }
        })

        findViewById<FrameLayout?>(R.id.activity_base_layout).apply {
            viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    // Layout has been finished
                    // hove to use removeGlobalOnLayoutListener() method after API 16 or is API 16
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    createViewDependingOnOrientation(resources.configuration.orientation)
                    /*  // using adding and removing view to implement
                    val view = playerFragment.view  // not null now
                    view?.let {
                        previousPlayParentView = view.parent as ViewGroup
                    }
                    */
                }
            })
        }
    }

    override fun onResume() {
        Log.d(TAG, "onResume() is called")
        super.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        Log.d(TAG, "onSaveInstanceState() is called")
        super.onSaveInstanceState(outState, outPersistentState)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        Log.d(TAG, "onConfigurationChanged() is called")
        createViewDependingOnOrientation(newConfig.orientation)
        super.onConfigurationChanged(newConfig)
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).apply {
            unregisterReceiver(baseReceiver)
        }
        super.onDestroy()
    }
/*
    private fun createViewDependingOnOrientation(orientation: Int) {
        val view = playerFragment.view
        Log.d(TAG, "createViewDependingOnOrientation.playerFragment.view = $view")
        view?.let {
            val currentParentView = it.parent as ViewGroup
            currentParentView.removeView(it)
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                Log.d(TAG, "createViewDependingOnOrientation.ORIENTATION_LANDSCAPE")
                basePlayViewLayout.addView(it)
                basePlayViewLayout.visibility = View.VISIBLE
                tablayoutViewLayout.visibility = View.INVISIBLE
            } else {
                Log.d(TAG, "createViewDependingOnOrientation.previousPlayParentView = $previousPlayParentView")
                previousPlayParentView?.addView(it)
                basePlayViewLayout.visibility = View.INVISIBLE
                tablayoutViewLayout.visibility = View.VISIBLE
            }
            previousPlayParentView = currentParentView
        }
    }
    */

    private fun createViewDependingOnOrientation(orientation : Int) {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.d(TAG, "createViewDependingOnOrientation.ORIENTATION_LANDSCAPE")
        } else {
            Log.d(TAG, "createViewDependingOnOrientation.ORIENTATION_PORTRAIT")
        }
        playerFragment.hidePlayerView()
    }
}