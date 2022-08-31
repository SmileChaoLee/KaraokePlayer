package com.smile.karaokeplayer

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.smile.karaokeplayer.fragments.MyFavoritesFragment
import com.smile.karaokeplayer.fragments.OpenFileFragment
import com.smile.karaokeplayer.fragments.PlayerBaseViewFragment
import com.smile.karaokeplayer.fragments.TablayoutFragment
import com.smile.karaokeplayer.models.SongInfo
import com.smile.smilelibraries.utilities.ScreenUtil

private const val TAG : String = "BaseActivity"

abstract class BaseActivity : AppCompatActivity(), PlayerBaseViewFragment.PlayBaseFragmentFunc,
        OpenFileFragment.PlayOpenFiles, MyFavoritesFragment.PlayMyFavorites {

    private val PERMISSION_REQUEST_CODE = 0x11
    private var hasPermissionForExternalStorage = false

    private lateinit var playerFragment: PlayerBaseViewFragment
    private lateinit var basePlayViewLayout : LinearLayout
    private var tablayoutFragment : TablayoutFragment? = null
    private lateinit var tablayoutViewLayout: LinearLayout

    abstract fun getFragment() : PlayerBaseViewFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG,"onCreate() is called")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)

        hasPermissionForExternalStorage =
                (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!hasPermissionForExternalStorage) {
                val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
            }
        } else {
            if (!hasPermissionForExternalStorage) {
                ScreenUtil.showToast(this, "Permission Denied", 60f,
                        ScreenUtil.FontSize_Pixel_Type,
                        Toast.LENGTH_LONG)
                finish()
            }
        }

        basePlayViewLayout = findViewById(R.id.basePlayViewLayout)
        tablayoutViewLayout = findViewById(R.id.tablayoutViewLayout)

        playerFragment = getFragment()

        if (intent.extras == null) {
            Log.d(TAG,"intent.extras is null")
            tablayoutFragment = TablayoutFragment()
        } else {
            Log.d(TAG,"intent.extras is not null")
        }
        supportFragmentManager.beginTransaction().apply {
            add(R.id.basePlayViewLayout, playerFragment)
            tablayoutFragment?.let {
                add(R.id.tablayoutViewLayout, it)
                tablayoutViewLayout.visibility = View.GONE
            }
            commit()
        }

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
                }
            })
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val rLen = grantResults.size
            hasPermissionForExternalStorage = rLen > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
        }
        if (!hasPermissionForExternalStorage) {
            ScreenUtil.showToast(this, "Permission Denied", 60f, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_LONG)
            finish() // exit the activity immediately
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
        super.onConfigurationChanged(newConfig)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy() is called.")
        super.onDestroy()
    }

    // implementing interface PlayerBaseViewFragment.PlayBaseFragmentFunc
    override fun baseHidePlayerView() {
        Log.d(TAG, "baseHidePlayerView() is called.")
        tablayoutViewLayout.visibility = View.VISIBLE
    }

    override fun baseShowPlayerView() {
        Log.d(TAG, "baseShowPlayerView() is called.")
        tablayoutViewLayout.visibility = View.GONE
    }
    // Finishes interface PlayerBaseViewFragment.PlayBaseFragmentFunc

    // implementing interface OpenFileFragment.PlayOpenFiles
    override fun playUriList(uris: ArrayList<Uri>) {
        Log.d(TAG, "playUriList.uris.size = ${uris.size}")
        playerFragment.mPresenter.playSelectedUrisFromStorage(uris)
    }
    // Finishes implementing interface OpenFileFragment.PlayOpenFiles

    // implementing interface MyListFragment.PlayMyList
    override fun playSongList(songs: ArrayList<SongInfo>) {
        Log.d(TAG, "playSongList.uris.size = ${songs.size}")
        playerFragment.mPresenter.playSongList(songs)
        playerFragment.showPlayerView()
    }

    private fun createViewDependingOnOrientation(orientation : Int) {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.d(TAG, "createViewDependingOnOrientation.ORIENTATION_LANDSCAPE")
        } else {
            Log.d(TAG, "createViewDependingOnOrientation.ORIENTATION_PORTRAIT")
        }
        if (intent.extras == null) {
            playerFragment.hidePlayerView()
        }
    }
}