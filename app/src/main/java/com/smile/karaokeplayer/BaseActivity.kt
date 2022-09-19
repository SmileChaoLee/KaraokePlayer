package com.smile.karaokeplayer

import android.Manifest
import android.app.Activity
import android.provider.Settings
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.smile.karaokeplayer.fragments.MyFavoritesFragment
import com.smile.karaokeplayer.fragments.PlayerBaseViewFragment
import com.smile.karaokeplayer.fragments.TablayoutFragment
import com.smile.karaokeplayer.interfaces.PlaySongs
import com.smile.karaokeplayer.models.SongInfo
import com.smile.smilelibraries.utilities.ScreenUtil

private const val TAG : String = "BaseActivity"
private const val PERMISSION_WRITE_EXTERNAL_CODE = 0x11
private const val PlayerFragmentTag = "PlayerFragment"
private const val TablayoutFragmentTag = "TablayoutFragment"
abstract class BaseActivity : AppCompatActivity(), PlayerBaseViewFragment.PlayBaseFragmentFunc,
        PlaySongs, MyFavoritesFragment.PlayMyFavorites {

    private var permissionExternalStorage = false
    private var permissionManageExternalStorage = false

    private lateinit var playerFragment: PlayerBaseViewFragment
    private lateinit var basePlayViewLayout : LinearLayout
    private var tablayoutFragment : TablayoutFragment? = null
    private lateinit var tablayoutViewLayout: LinearLayout

    abstract fun getFragment() : PlayerBaseViewFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG,"onCreate() is called")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)

        permissionExternalStorage =
                (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!permissionExternalStorage) {
                val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_WRITE_EXTERNAL_CODE)
            }
            // MANAGE_EXTERNAL_STORAGE
            // requestManageExternalStoragePermission()
        } else {
            if (!permissionExternalStorage) {
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
            Log.d(TAG, "intent.extras is null")
            tablayoutFragment = TablayoutFragment()
        } else {
            Log.d(TAG, "intent.extras is not null")
        }
        supportFragmentManager.beginTransaction().apply {
            add(R.id.basePlayViewLayout, playerFragment, PlayerFragmentTag)
            tablayoutFragment?.let {
                add(R.id.tablayoutViewLayout, it, TablayoutFragmentTag)
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

    private fun requestManageExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Log.d(TAG, "Environment.isExternalStorageManager() = ${Environment.isExternalStorageManager()}")
            if (!Environment.isExternalStorageManager()) {
                permissionManageExternalStorage = false
                val launcher: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts
                        .StartActivityForResult()) { result: ActivityResult? ->
                    result?.run {
                        if (resultCode == Activity.RESULT_OK) {
                            if (Environment.isExternalStorageManager()) {
                                permissionManageExternalStorage = true
                            }
                        }
                        // still can run this app if permissionManageExternalStorage = false
                    }
                }
                try {
                    val uri = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
                    val mIntent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri)
                    launcher.launch(mIntent)
                } catch (ex: Exception) {
                    Log.d(TAG, "Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION Exception")
                    ex.message?.let {
                        Log.d(TAG, it)
                    }
                    val mIntent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    launcher.launch(mIntent)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_WRITE_EXTERNAL_CODE) {
            val rLen = grantResults.size
            permissionExternalStorage = rLen > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
        }
        if (!permissionExternalStorage) {
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

    // implementing interface PlaySongs
    override fun playSelectedSongList(songs: ArrayList<SongInfo>) {
        Log.d(TAG, "playSelectedSongList.songs.size = ${songs.size}")
        playerFragment.mPresenter.playSongList(songs)
        playerFragment.showPlayerView()
    }
    // Finish implementing interface PlaySongs

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