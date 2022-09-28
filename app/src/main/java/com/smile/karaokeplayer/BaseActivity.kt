package com.smile.karaokeplayer

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.*
import android.provider.Settings
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.smile.karaokeplayer.constants.PlayerConstants
import com.smile.karaokeplayer.fragments.PlayerBaseViewFragment
import com.smile.karaokeplayer.fragments.TablayoutFragment
import com.smile.karaokeplayer.interfaces.PlayMyFavorites
import com.smile.karaokeplayer.interfaces.PlaySongs
import com.smile.karaokeplayer.models.PlayingParameters
import com.smile.karaokeplayer.models.SongInfo
import com.smile.smilelibraries.utilities.ScreenUtil

private const val TAG : String = "BaseActivity"
private const val PERMISSION_WRITE_EXTERNAL_CODE = 0x11
private const val PlayerFragmentTag = "PlayerFragment"
private const val TablayoutFragmentTag = "TablayoutFragment"
private const val HasPlayedSingleState = "HasPlayedSingle"
private const val PlayDataState = "PlayData"
private const val CallingComponentState = "CallingComponentName"
abstract class BaseActivity : AppCompatActivity(), PlayerBaseViewFragment.PlayBaseFragmentFunc,
        PlaySongs, PlayMyFavorites {

    private var permissionExternalStorage = false
    private var permissionManageExternalStorage = false

    private var playerFragment: PlayerBaseViewFragment? = null
    private lateinit var basePlayViewLayout : LinearLayout
    private var tablayoutFragment : TablayoutFragment? = null
    private lateinit var tablayoutViewLayout : LinearLayout
    // the declaration of baseReceiver must be lateinit var.
    // Not var and BroadcastReceiver? = null
    private lateinit var baseReceiver: BroadcastReceiver
    private lateinit var callingIntent : Intent
    private var hasPlayedSingle : Boolean = false
    private var callingComponentName : ComponentName? = null
    private var playData = Bundle()

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

        callingIntent = intent
        if (savedInstanceState == null) {
            playerFragment = getFragment()
            if (callingIntent.extras == null) {
                Log.d(TAG, "callingIntent.extras is null")
                tablayoutFragment = TablayoutFragment()
            } else {
                Log.d(TAG, "callingIntent.extras is not null")
            }
        } else {
            hasPlayedSingle = savedInstanceState.getBoolean(HasPlayedSingleState, false)

            callingComponentName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                savedInstanceState.getParcelable(CallingComponentState, ComponentName::class.java)
            else savedInstanceState.getParcelable(CallingComponentState)

            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                savedInstanceState.getParcelable(PlayDataState, Bundle::class.java)
            else savedInstanceState.getParcelable(PlayDataState))?.also {
                playData = it
            }

            playerFragment = supportFragmentManager.findFragmentByTag(PlayerFragmentTag) as PlayerBaseViewFragment
            Log.d(TAG, "savedInstanceState is not null.playerFragment = $playerFragment")
            tablayoutFragment = supportFragmentManager.findFragmentByTag(TablayoutFragmentTag) as TablayoutFragment?
            Log.d(TAG, "savedInstanceState is not null.tablayoutFragment = $tablayoutFragment")
        }

        supportFragmentManager.beginTransaction().apply {
            var isReplaced = false
            tablayoutFragment?.let {
                if (!it.isInLayout) {
                    Log.d(TAG, "tablayoutFragment.isInLayout() = false")
                    replace(R.id.tablayoutViewLayout, it, TablayoutFragmentTag)
                    tablayoutViewLayout.visibility = View.VISIBLE
                    isReplaced = true
                }
            }
            playerFragment?.let {
                if (!it.isInLayout) {
                    Log.d(TAG, "playerFragment.isInLayout() = false")
                    replace(R.id.basePlayViewLayout, it, PlayerFragmentTag)
                    isReplaced = true
                }
            }
            if (isReplaced) commit()
        }

        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d(TAG, "BroadcastReceiver.onReceive")
                intent?.action?.let {
                    if (it == PlayerConstants.PlaySingleSongAction) {
                        Log.d(TAG, "onReceive.PlaySingleSongAction")
                        onReceiveFunc(it, intent, null)
                        hasPlayedSingle = true
                    }
                }
            }
        }.also { baseReceiver = it }

        LocalBroadcastManager.getInstance(this).apply {
            Log.d(TAG, "LocalBroadcastManager.registerReceiver")
            registerReceiver(baseReceiver, IntentFilter().apply {
                addAction(PlayerConstants.PlaySingleSongAction)
                addAction(PlayerConstants.BackToBaseActivity)
            })
        }

        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG, "onBackPressedDispatcher.handleOnBackPressed")
                playerFragment?.onBackPressed()
            }
        })

        findViewById<FrameLayout?>(R.id.activity_base_layout).apply {
            viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    // Layout has been finished
                    // hove to use removeGlobalOnLayoutListener() method after API 16 or is API 16
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    createViewDependingOnOrientation(resources.configuration.orientation, savedInstanceState)
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
        outState.putBoolean(HasPlayedSingleState, hasPlayedSingle)
        outState.putParcelable(CallingComponentState, callingComponentName)
        outState.putParcelable(PlayDataState, playData)
        super.onSaveInstanceState(outState, outPersistentState)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        Log.d(TAG, "onConfigurationChanged() is called")
        super.onConfigurationChanged(newConfig)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy() is called.")
        LocalBroadcastManager.getInstance(this).apply {
            unregisterReceiver(baseReceiver)
        }
        super.onDestroy()
    }

    private fun onReceiveFunc(action : String, intent : Intent?, pData : Bundle?) {
        Log.d(TAG, "onReceiveFunc()")
        playerFragment?.run {
            mPresenter.let{
                it.initializeVariables(pData, intent)
                it.playSongPlayedBeforeActivityCreated()
                setMainMenu()
                if (action == PlayerConstants.PlaySingleSongAction) {
                    showPlayerView()
                } else {
                    // PlayerConstants.BackToBaseActivity
                    if (it.playingParam.isPlayerViewVisible) showPlayerView() else hidePlayerView()
                    Log.d(TAG, "onReceiveFunc().currentPlaybackState = ${it.playingParam.currentPlaybackState}")
                }
            }
            showSupportToolbarAndAudioController()
        }
        Intent().apply {
            Log.d(TAG, "onReceiveFunc.componentName = $componentName")
            component = componentName
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(this)
        }
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

    // Implement interface PlayerBaseViewFragment.PlayBaseFragmentFunc
    override fun returnToPrevious(isSingleSong : Boolean) {
        if (isSingleSong) {
            playerFragment?.mPresenter?.pausePlay()
            callingComponentName?.let {
                Intent().apply {
                    component = it
                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    startActivity(this)
                }
                return
            }
        }
        val handlerClose = Handler(Looper.getMainLooper())
        val timeDelay = 300
        handlerClose.postDelayed({
            // exit application
            finish()
        }, timeDelay.toLong())
    }
    // Finishes interface PlayerBaseViewFragment.PlayBaseFragmentFunc

    // implementing interface PlayMyFavorites
    override fun onSavePlayingState(compName : ComponentName?) {
        Log.d(TAG, "onSavePlayingState.compName = $compName")
        callingComponentName = compName
        playerFragment?.let {
            playData.clear()
            it.onSaveInstanceState(playData)
        }
    }

    override fun restorePlayingState() {
        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            playData.getParcelable(PlayerConstants.PlayingParamState, PlayingParameters::class.java)
        else playData.getParcelable(PlayerConstants.PlayingParamState))?.apply {
            Log.d(TAG, "restorePlayingState.currentPlaybackState = $currentPlaybackState")
            Log.d(TAG, "restorePlayingState.currentAudioPosition = $currentAudioPosition")
        }

        if (hasPlayedSingle) {
            onReceiveFunc(PlayerConstants.BackToBaseActivity, null, playData)
            callingComponentName = null
        }
        hasPlayedSingle = false
    }
    // Finishes implementing interface PlayMyFavorites

    // implementing interface PlaySongs
    override fun playSelectedSongList(songs: ArrayList<SongInfo>) {
        Log.d(TAG, "playSelectedSongList.songs.size = ${songs.size}")
        playerFragment?.mPresenter?.playSongList(songs)
        playerFragment?.showPlayerView()
    }
    // Finish implementing interface PlaySongs

    private fun createViewDependingOnOrientation(orientation : Int, savedInstanceState : Bundle?) {
        if (callingIntent.extras == null) {
            playerFragment?.hidePlayerView()
        }
    }
}