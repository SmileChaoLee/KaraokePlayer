package com.smile.karaokeplayer

import android.content.res.Configuration
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentActivity
import com.smile.karaokeplayer.fragments.*
import com.smile.karaokeplayer.interfaces.FragmentInterface

private const val TAG : String = "BaseActivity"

abstract class BaseActivity : FragmentActivity(), FragmentInterface {
    lateinit var playerFragment: PlayerBaseViewFragment
    lateinit var tablayoutFragment : TablayoutFragment
    lateinit var basePlayViewLayout : LinearLayout
    lateinit var tablayoutViewLayout: LinearLayout
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

        basePlayViewLayout.visibility = View.VISIBLE
        tablayoutViewLayout.visibility = View.INVISIBLE

        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG, "onBackPressedDispatcher.handleOnBackPressed")
                playerFragment.onBackPressed()
            }
        })

        val activityBaseLayout : FrameLayout = findViewById(R.id.activity_base_layout)
        activityBaseLayout.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    // Layout has been finished
                    // hove to use removeGlobalOnLayoutListener() method after API 16 or is API 16
                    activityBaseLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
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
    }
}